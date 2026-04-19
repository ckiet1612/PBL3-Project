package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.entity.Order;
import com.pbl3.project.pbl3_project.entity.OrderStatus;
import com.pbl3.project.pbl3_project.entity.ReturnRefundScope;
import com.pbl3.project.pbl3_project.entity.Role;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.repository.CustomerRepository;
import com.pbl3.project.pbl3_project.repository.OrderRepository;
import com.pbl3.project.pbl3_project.repository.ProductRepository;
import com.pbl3.project.pbl3_project.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceReturnRefundScopeTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PromotionService promotionService;
    @Mock
    private InventoryTransactionService inventoryTransactionService;
    @Mock
    private InventoryLedgerService inventoryLedgerService;
    @Mock
    private AuthorizationService authorizationService;
    @Mock
    private OperationalAuditLogService operationalAuditLogService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
            orderRepository,
            customerRepository,
            productRepository,
            userRepository,
            promotionService,
            inventoryTransactionService,
            inventoryLedgerService,
            authorizationService,
            operationalAuditLogService
        );
    }

    @Test
    void resolveReturnRefundStatusesUsesProcessedDefaultWhenScopeMissing() {
        Set<OrderStatus> statuses = orderService.resolveReturnRefundStatuses(null, null);

        assertEquals(
            EnumSet.of(OrderStatus.PARTIALLY_RETURNED, OrderStatus.RETURNED, OrderStatus.CANCELED),
            statuses
        );
    }

    @Test
    void resolveReturnRefundStatusesIntersectsExplicitStatusesWithScope() {
        Set<OrderStatus> statuses = orderService.resolveReturnRefundStatuses(
            ReturnRefundScope.ELIGIBLE_ONLY,
            new LinkedHashSet<>(java.util.List.of(OrderStatus.COMPLETED, OrderStatus.RETURNED, OrderStatus.PARTIALLY_RETURNED))
        );

        assertEquals(
            EnumSet.of(OrderStatus.COMPLETED, OrderStatus.PARTIALLY_RETURNED),
            statuses
        );
    }

    @Test
    void searchReturnRefundOrdersReturnsEmptyPageWhenStatusIntersectionIsEmpty() {
        User actor = new User();
        actor.setId(8L);
        actor.setRole(Role.MANAGER);
        actor.setEnabled(true);

        Page<Order> page = orderService.searchReturnRefundOrders(
            actor,
            ReturnRefundScope.ELIGIBLE_ONLY,
            "",
            null,
            null,
            Set.of(),
            Set.of(),
            Set.of(OrderStatus.CANCELED),
            null,
            null,
            PageRequest.of(0, 20)
        );

        assertTrue(page.isEmpty());
        verify(authorizationService).requireReturnsRefundsAccess(actor);
        verify(orderRepository, never()).findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void searchReturnRefundOrdersDelegatesToOrderSearchForMatchingScopeStatuses() {
        User actor = new User();
        actor.setId(5L);
        actor.setRole(Role.ADMIN);
        actor.setEnabled(true);

        PageRequest pageable = PageRequest.of(0, 20);
        Page<Order> expectedPage = Page.empty(pageable);
        when(orderRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class))).thenReturn(expectedPage);

        Page<Order> actualPage = orderService.searchReturnRefundOrders(
            actor,
            ReturnRefundScope.PROCESSED,
            "refund",
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31),
            Set.of(5L),
            Set.of(),
            Set.of(OrderStatus.CANCELED, OrderStatus.RETURNED),
            BigDecimal.ZERO,
            null,
            pageable
        );

        assertEquals(expectedPage, actualPage);
        verify(authorizationService).requireReturnsRefundsAccess(actor);
        verify(authorizationService).requireOrderHistoryAccess(actor);
        verify(orderRepository).findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class));
    }
}
