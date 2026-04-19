package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.dto.IdLabelOption;
import com.pbl3.project.pbl3_project.entity.Expense;
import com.pbl3.project.pbl3_project.entity.ExpenseCategory;
import com.pbl3.project.pbl3_project.entity.OperationalAuditAction;
import com.pbl3.project.pbl3_project.entity.OperationalSubjectType;
import com.pbl3.project.pbl3_project.entity.PaymentMethod;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.repository.ExpenseRepository;
import com.pbl3.project.pbl3_project.repository.UserRepository;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final AuthorizationService authorizationService;
    private final OperationalAuditLogService operationalAuditLogService;

    public ExpenseService(
        ExpenseRepository expenseRepository,
        UserRepository userRepository,
        AuthorizationService authorizationService,
        OperationalAuditLogService operationalAuditLogService
    ) {
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
        this.operationalAuditLogService = operationalAuditLogService;
    }

    @Transactional(readOnly = true)
    public Page<Expense> searchExpenses(
        User viewer,
        String search,
        LocalDate startDate,
        LocalDate endDate,
        Set<ExpenseCategory> selectedCategories,
        Set<PaymentMethod> selectedMethods,
        Set<Long> selectedCreators,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        Pageable pageable
    ) {
        if (viewer != null) {
            authorizationService.requireExpensesAccess(viewer);
        }
        Pageable sanitizedPageable = PageSortSupport.sanitize(
            pageable,
            Sort.by(Sort.Direction.DESC, "spentOn").and(Sort.by(Sort.Direction.DESC, "createdAt")),
            Set.of("id", "spentOn", "category", "title", "amount", "paymentMethod", "createdAt", "createdBy.fullName", "createdBy.username")
        );

        Specification<Expense> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            var createdByJoin = root.join("createdBy", JoinType.LEFT);

            String normalizedSearch = search == null ? null : search.trim().toLowerCase();
            if (normalizedSearch != null && !normalizedSearch.isEmpty()) {
                String likeValue = "%" + normalizedSearch + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(cb.function("str", String.class, root.get("id"))), likeValue),
                    cb.like(cb.lower(cb.coalesce(root.get("title"), "")), likeValue),
                    cb.like(cb.lower(cb.coalesce(root.get("note"), "")), likeValue),
                    cb.like(cb.lower(root.get("category").as(String.class)), likeValue),
                    cb.like(cb.lower(cb.coalesce(createdByJoin.get("fullName"), "")), likeValue),
                    cb.like(cb.lower(cb.coalesce(createdByJoin.get("username"), "")), likeValue)
                ));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("spentOn"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("spentOn"), endDate));
            }
            if (selectedCategories != null && !selectedCategories.isEmpty()) {
                predicates.add(root.get("category").in(selectedCategories));
            }
            if (selectedMethods != null && !selectedMethods.isEmpty()) {
                predicates.add(root.get("paymentMethod").in(selectedMethods));
            }
            if (selectedCreators != null && !selectedCreators.isEmpty()) {
                predicates.add(createdByJoin.get("id").in(selectedCreators));
            }
            if (minAmount != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), MoneySupport.normalize(minAmount)));
            }
            if (maxAmount != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), MoneySupport.normalize(maxAmount)));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return expenseRepository.findAll(spec, sanitizedPageable);
    }

    @Transactional(readOnly = true)
    public Expense getExpense(Long expenseId, User viewer) {
        authorizationService.requireExpensesAccess(viewer);
        return expenseRepository.findById(expenseId)
            .orElseThrow(() -> new ValidationException("Expense not found: " + expenseId));
    }

    @Transactional
    public Expense createExpense(
        User actor,
        LocalDate spentOn,
        ExpenseCategory category,
        String title,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        String note
    ) {
        User persistedActor = resolveWriter(actor);
        Expense expense = new Expense();
        applyExpenseFields(expense, spentOn, category, title, amount, paymentMethod, note);
        expense.setCreatedBy(persistedActor);
        Expense savedExpense = expenseRepository.save(expense);
        operationalAuditLogService.record(
            persistedActor,
            OperationalAuditAction.EXPENSE_CREATED,
            OperationalSubjectType.EXPENSE,
            savedExpense.getId(),
            buildSubjectLabel(savedExpense),
            "Expense created"
        );
        return savedExpense;
    }

    @Transactional
    public Expense updateExpense(
        User actor,
        Long expenseId,
        LocalDate spentOn,
        ExpenseCategory category,
        String title,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        String note
    ) {
        User persistedActor = resolveWriter(actor);
        Expense expense = expenseRepository.findById(expenseId)
            .orElseThrow(() -> new ValidationException("Expense not found: " + expenseId));
        applyExpenseFields(expense, spentOn, category, title, amount, paymentMethod, note);
        Expense savedExpense = expenseRepository.save(expense);
        operationalAuditLogService.record(
            persistedActor,
            OperationalAuditAction.EXPENSE_UPDATED,
            OperationalSubjectType.EXPENSE,
            savedExpense.getId(),
            buildSubjectLabel(savedExpense),
            "Expense updated"
        );
        return savedExpense;
    }

    @Transactional
    public void deleteExpense(User actor, Long expenseId) {
        User persistedActor = resolveWriter(actor);
        Expense expense = expenseRepository.findById(expenseId)
            .orElseThrow(() -> new ValidationException("Expense not found: " + expenseId));
        String subjectLabel = buildSubjectLabel(expense);
        expenseRepository.delete(expense);
        operationalAuditLogService.record(
            persistedActor,
            OperationalAuditAction.EXPENSE_DELETED,
            OperationalSubjectType.EXPENSE,
            expenseId,
            subjectLabel,
            "Expense deleted"
        );
    }

    @Transactional(readOnly = true)
    public List<IdLabelOption> getExpenseCreatorOptions(User viewer) {
        authorizationService.requireExpensesAccess(viewer);
        return expenseRepository.findDistinctCreatorOptions().stream()
            .sorted(java.util.Comparator
                .comparing(IdLabelOption::label, java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(IdLabelOption::id, java.util.Comparator.nullsLast(Long::compareTo)))
            .toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal getExpenseMaxAmount(User viewer) {
        authorizationService.requireExpensesAccess(viewer);
        return MoneySupport.normalize(expenseRepository.findMaxAmount());
    }

    private User resolveWriter(User actor) {
        authorizationService.requireExpenseWrite(actor);
        if (actor == null || actor.getId() == null) {
            throw new ValidationException("User not found");
        }
        return userRepository.findById(actor.getId())
            .orElseThrow(() -> new ValidationException("User not found"));
    }

    private void applyExpenseFields(
        Expense expense,
        LocalDate spentOn,
        ExpenseCategory category,
        String title,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        String note
    ) {
        String normalizedTitle = title != null ? title.trim() : null;
        String normalizedNote = note != null ? note.trim() : null;
        BigDecimal normalizedAmount = amount != null ? MoneySupport.normalize(amount) : MoneySupport.ZERO;

        if (spentOn == null) {
            throw new ValidationException("Spent on date is required");
        }
        if (category == null) {
            throw new ValidationException("Expense category is required");
        }
        if (normalizedTitle == null || normalizedTitle.isBlank()) {
            throw new ValidationException("Expense title is required");
        }
        if (!MoneySupport.isPositive(normalizedAmount)) {
            throw new ValidationException("Expense amount must be greater than 0");
        }

        expense.setSpentOn(spentOn);
        expense.setCategory(category);
        expense.setTitle(normalizedTitle);
        expense.setAmount(normalizedAmount);
        expense.setPaymentMethod(paymentMethod != null ? paymentMethod : PaymentMethod.CASH);
        expense.setNote(normalizedNote == null || normalizedNote.isBlank() ? null : normalizedNote);
    }

    private String buildSubjectLabel(Expense expense) {
        if (expense == null) {
            return "Expense";
        }
        String title = expense.getTitle() != null && !expense.getTitle().isBlank() ? expense.getTitle() : "Expense";
        return expense.getId() != null ? title + " (#" + expense.getId() + ")" : title;
    }
}
