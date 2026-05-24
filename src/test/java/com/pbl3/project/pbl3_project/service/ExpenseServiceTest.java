package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.entity.Expense;
import com.pbl3.project.pbl3_project.entity.ExpenseCategory;
import com.pbl3.project.pbl3_project.entity.OperationalAuditAction;
import com.pbl3.project.pbl3_project.entity.OperationalSubjectType;
import com.pbl3.project.pbl3_project.entity.PaymentMethod;
import com.pbl3.project.pbl3_project.entity.Role;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.repository.ExpenseRepository;
import com.pbl3.project.pbl3_project.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OperationalAuditLogService operationalAuditLogService;

    private ExpenseService expenseService;

    @BeforeEach
    void setUp() {
        expenseService = new ExpenseService(
            expenseRepository,
            userRepository,
            new AuthorizationService(),
            operationalAuditLogService
        );
    }

    @Test
    void createExpensePersistsNormalizedExpenseAndWritesAudit() {
        User actor = user(1L, Role.MANAGER);
        when(userRepository.findById(actor.getId())).thenReturn(Optional.of(actor));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
            Expense saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        Expense saved = expenseService.createExpense(
            actor,
            LocalDate.of(2026, 4, 18),
            ExpenseCategory.RENT,
            "  Main Store Rent  ",
            new BigDecimal("12500000"),
            PaymentMethod.QR,
            "  April invoice  "
        );

        assertEquals(99L, saved.getId());
        assertEquals("Main Store Rent", saved.getTitle());
        assertEquals(MoneySupport.normalize(new BigDecimal("12500000")), saved.getAmount());
        assertEquals(PaymentMethod.QR, saved.getPaymentMethod());
        assertEquals("April invoice", saved.getNote());
        assertEquals(actor, saved.getCreatedBy());

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(captor.capture());
        assertEquals(ExpenseCategory.RENT, captor.getValue().getCategory());
        verify(operationalAuditLogService).record(
            eq(actor),
            eq(OperationalAuditAction.EXPENSE_CREATED),
            eq(OperationalSubjectType.EXPENSE),
            eq(99L),
            eq("Main Store Rent (#99)"),
            eq("Expense created")
        );
    }

    @Test
    void deleteExpenseRemovesExpenseAndWritesAudit() {
        User actor = user(2L, Role.ADMIN);
        Expense expense = new Expense();
        expense.setId(41L);
        expense.setTitle("Utility Bill");

        when(userRepository.findById(actor.getId())).thenReturn(Optional.of(actor));
        when(expenseRepository.findById(expense.getId())).thenReturn(Optional.of(expense));

        expenseService.deleteExpense(actor, expense.getId());

        verify(expenseRepository).delete(expense);
        verify(operationalAuditLogService).record(
            eq(actor),
            eq(OperationalAuditAction.EXPENSE_DELETED),
            eq(OperationalSubjectType.EXPENSE),
            eq(41L),
            eq("Utility Bill (#41)"),
            eq("Expense deleted")
        );
    }

    @Test
    void createExpenseRejectsStaffAccess() {
        User actor = user(3L, Role.STAFF);

        AuthorizationException ex = assertThrows(
            AuthorizationException.class,
            () -> expenseService.createExpense(
                actor,
                LocalDate.of(2026, 4, 18),
                ExpenseCategory.SOFTWARE,
                "POS subscription",
                new BigDecimal("150000"),
                PaymentMethod.CARD,
                null
            )
        );

        assertTrue(ex.getMessage().contains("modify expenses"));
    }

    @Test
    void createExpenseRejectsNonPositiveAmount() {
        User actor = user(4L, Role.MANAGER);
        when(userRepository.findById(actor.getId())).thenReturn(Optional.of(actor));

        ValidationException ex = assertThrows(
            ValidationException.class,
            () -> expenseService.createExpense(
                actor,
                LocalDate.of(2026, 4, 18),
                ExpenseCategory.OTHER,
                "Coffee",
                BigDecimal.ZERO,
                PaymentMethod.CASH,
                null
            )
        );

        assertEquals("Expense amount must be greater than 0", ex.getMessage());
    }

    private User user(Long id, Role role) {
        return new User(id, "user" + id, "secret", "User " + id, role, true);
    }
}
