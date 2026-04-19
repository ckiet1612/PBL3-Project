package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.dto.report.ActionCenterType;
import com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow;
import com.pbl3.project.pbl3_project.dto.report.OperationalInsightBundle;
import com.pbl3.project.pbl3_project.entity.Category;
import com.pbl3.project.pbl3_project.entity.ImportOrder;
import com.pbl3.project.pbl3_project.entity.ImportOrderItem;
import com.pbl3.project.pbl3_project.entity.ImportOrderStatus;
import com.pbl3.project.pbl3_project.entity.InventoryTransaction;
import com.pbl3.project.pbl3_project.entity.Order;
import com.pbl3.project.pbl3_project.entity.OrderItem;
import com.pbl3.project.pbl3_project.entity.OrderStatus;
import com.pbl3.project.pbl3_project.entity.PaymentMethod;
import com.pbl3.project.pbl3_project.entity.Product;
import com.pbl3.project.pbl3_project.entity.Role;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.repository.ImportOrderItemRepository;
import com.pbl3.project.pbl3_project.repository.InventoryTransactionRepository;
import com.pbl3.project.pbl3_project.repository.OrderItemRepository;
import com.pbl3.project.pbl3_project.repository.OrderRepository;
import com.pbl3.project.pbl3_project.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationalInsightServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;
    @Mock
    private ImportOrderItemRepository importOrderItemRepository;

    private OperationalInsightService operationalInsightService;

    @BeforeEach
    void setUp() {
        operationalInsightService = new OperationalInsightService(
            orderRepository,
            orderItemRepository,
            productRepository,
            inventoryTransactionRepository,
            importOrderItemRepository,
            new AuthorizationService()
        );
    }

    @Test
    void dashboardInsightsForStaffReturnOnlyWhatChanged() {
        User staff = user(10L, Role.STAFF);
        User creator = user(11L, Role.STAFF);

        Order yesterdayOrder = order(
            1L,
            LocalDate.now().minusDays(1).atTime(10, 0),
            new BigDecimal("200"),
            OrderStatus.COMPLETED,
            creator
        );
        Order todayOrder = order(
            2L,
            LocalDate.now().atTime(10, 0),
            new BigDecimal("100"),
            OrderStatus.COMPLETED,
            creator
        );

        when(orderRepository.findAllWithinDateRangeAndUserId(any(), any(), eq(staff.getId())))
            .thenReturn(List.of(yesterdayOrder, todayOrder));
        when(orderItemRepository.findAllWithOrderAndProductWithinDateRangeAndUserId(any(), any(), eq(staff.getId())))
            .thenReturn(List.of());

        OperationalInsightBundle bundle = operationalInsightService.getDashboardInsights(staff);

        assertNotNull(bundle.whatChanged());
        assertNull(bundle.actionCenter());
        assertNull(bundle.reorder());
        verifyNoInteractions(productRepository, inventoryTransactionRepository, importOrderItemRepository);
    }

    @Test
    void dashboardInsightsForManagerIncludeReorderAndActionCenterSignals() {
        User manager = user(20L, Role.MANAGER);
        Category dairy = new Category(1L, "Dairy");
        Product milk = product(100L, "Milk Box", dairy, 4, 10, new BigDecimal("4000"));

        Order yesterdayOrder = order(
            3L,
            LocalDate.now().minusDays(1).atTime(11, 0),
            new BigDecimal("560"),
            OrderStatus.COMPLETED,
            manager
        );
        Order todayOrder = order(
            4L,
            LocalDate.now().atTime(11, 0),
            new BigDecimal("280"),
            OrderStatus.COMPLETED,
            manager
        );

        OrderItem todayItem = orderItem(todayOrder, milk, 28, new BigDecimal("10"));
        InventoryTransaction lastInbound = inboundTransaction(milk, LocalDate.now().minusDays(40).atStartOfDay(), 24);
        ImportOrderItem latestImport = latestImportItem(milk, LocalDate.now().minusDays(20).atStartOfDay(), "ACME Supplier", new BigDecimal("3500"));

        when(orderRepository.findAllWithinDateRange(any(), any()))
            .thenReturn(List.of(yesterdayOrder, todayOrder));
        when(orderItemRepository.findAllWithOrderAndProductWithinDateRange(any(), any()))
            .thenReturn(List.of(todayItem));
        when(productRepository.findAllActiveWithCategory())
            .thenReturn(List.of(milk));
        when(inventoryTransactionRepository.findAllWithProductOrderByCreatedAtDesc())
            .thenReturn(List.of(lastInbound));
        when(importOrderItemRepository.findCompletedImportSnapshotsForProducts(anyCollection(), eq(ImportOrderStatus.COMPLETED)))
            .thenReturn(List.of(latestImport));

        OperationalInsightBundle bundle = operationalInsightService.getDashboardInsights(manager);

        assertNotNull(bundle.whatChanged());
        assertNotNull(bundle.actionCenter());
        assertNotNull(bundle.reorder());
        assertEquals(1, bundle.reorder().rows().size());

        ExplainableReorderRow reorderRow = bundle.reorder().rows().get(0);
        assertEquals(milk.getId(), reorderRow.productId());
        assertEquals(24, reorderRow.suggestedReorderQty());
        assertEquals("ACME Supplier", reorderRow.latestSupplierName());
        assertTrue(reorderRow.coverageKnown());

        assertTrue(bundle.actionCenter().items().stream()
            .anyMatch(item -> item.type() == ActionCenterType.REORDER_NOW
                && milk.getId().equals(item.productId())
                && Integer.valueOf(24).equals(item.suggestedQuantity())));
        assertTrue(bundle.actionCenter().items().stream()
            .anyMatch(item -> item.type() == ActionCenterType.AGED_STOCK));
        assertTrue(bundle.actionCenter().items().stream()
            .anyMatch(item -> item.type() == ActionCenterType.REVENUE_DROP));
    }

    @Test
    void dashboardInsightsIncludeCancelSpikeActionItemWhenCanceledOrdersJump() {
        User manager = user(21L, Role.MANAGER);

        Order baselineOrder = order(
            10L,
            LocalDate.now().minusDays(1).atTime(9, 0),
            new BigDecimal("100"),
            OrderStatus.COMPLETED,
            manager
        );
        Order currentCanceledA = order(
            11L,
            LocalDate.now().atTime(9, 0),
            new BigDecimal("100"),
            OrderStatus.CANCELED,
            manager
        );
        Order currentCanceledB = order(
            12L,
            LocalDate.now().atTime(10, 0),
            new BigDecimal("80"),
            OrderStatus.CANCELED,
            manager
        );
        Order currentCompleted = order(
            13L,
            LocalDate.now().atTime(11, 0),
            new BigDecimal("120"),
            OrderStatus.COMPLETED,
            manager
        );

        when(orderRepository.findAllWithinDateRange(any(), any()))
            .thenReturn(List.of(baselineOrder, currentCanceledA, currentCanceledB, currentCompleted));
        when(orderItemRepository.findAllWithOrderAndProductWithinDateRange(any(), any()))
            .thenReturn(List.of());
        when(productRepository.findAllActiveWithCategory())
            .thenReturn(List.of());

        OperationalInsightBundle bundle = operationalInsightService.getDashboardInsights(manager);

        assertNotNull(bundle.actionCenter());
        assertTrue(bundle.actionCenter().items().stream()
            .anyMatch(item -> item.type() == ActionCenterType.CANCEL_SPIKE));
        assertTrue(bundle.whatChanged().insights().stream()
            .anyMatch(insight -> insight.headline().contains("Cancel rate up")));
    }

    @Test
    void operationalReportInsightsUsePreviousAdjacentRangeAsBaseline() {
        LocalDate reportStart = LocalDate.now().minusDays(2);
        LocalDate reportEnd = LocalDate.now();
        LocalDate baselineStart = reportStart.minusDays(3);
        LocalDate baselineEnd = reportStart.minusDays(1);

        User manager = user(22L, Role.MANAGER);
        Order baselineOrder = order(
            20L,
            baselineStart.plusDays(1).atTime(10, 0),
            new BigDecimal("600"),
            OrderStatus.COMPLETED,
            manager
        );
        Order currentOrder = order(
            21L,
            reportStart.plusDays(1).atTime(10, 0),
            new BigDecimal("300"),
            OrderStatus.COMPLETED,
            manager
        );

        when(orderRepository.findAllWithinDateRange(any(), any()))
            .thenReturn(List.of(baselineOrder, currentOrder));
        when(orderItemRepository.findAllWithOrderAndProductWithinDateRange(any(), any()))
            .thenReturn(List.of());
        when(productRepository.findAllActiveWithCategory())
            .thenReturn(List.of());

        OperationalInsightBundle bundle = operationalInsightService.getOperationalReportInsights(reportStart, reportEnd);

        assertNotNull(bundle.whatChanged());
        assertEquals(baselineStart + " to " + baselineEnd, bundle.whatChanged().baselineRangeLabel());
        assertTrue(bundle.whatChanged().insights().stream()
            .anyMatch(insight -> insight.headline().contains("vs " + baselineStart + " to " + baselineEnd)));
        assertTrue(bundle.actionCenter().items().stream()
            .anyMatch(item -> item.type() == ActionCenterType.REVENUE_DROP
                && item.title().contains("vs " + baselineStart + " to " + baselineEnd)));
    }

    private User user(Long id, Role role) {
        return new User(id, role.name().toLowerCase(), "secret", role.name(), role, true);
    }

    private Product product(Long id, String name, Category category, int quantity, int minStock, BigDecimal importPrice) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setCategory(category);
        product.setQuantity(quantity);
        product.setMinStockLevel(minStock);
        product.setPrice(BigDecimal.TEN);
        product.setImportPrice(importPrice);
        product.setDeleted(false);
        return product;
    }

    private Order order(Long id, LocalDateTime createdAt, BigDecimal totalPrice, OrderStatus status, User user) {
        Order order = new Order();
        order.setId(id);
        order.setCreatedAt(createdAt);
        order.setTotalPrice(totalPrice);
        order.setRefundedAmount(BigDecimal.ZERO);
        order.setStatus(status);
        order.setUser(user);
        order.setPaymentMethod(PaymentMethod.CASH);
        return order;
    }

    private OrderItem orderItem(Order order, Product product, int quantity, BigDecimal price) {
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setReturnedQuantity(0);
        item.setPrice(price);
        return item;
    }

    private InventoryTransaction inboundTransaction(Product product, LocalDateTime createdAt, int quantityChange) {
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setProduct(product);
        transaction.setQuantityChange(quantityChange);
        transaction.setCreatedAt(createdAt);
        return transaction;
    }

    private ImportOrderItem latestImportItem(Product product, LocalDateTime createdAt, String supplierName, BigDecimal importPrice) {
        ImportOrder importOrder = new ImportOrder();
        importOrder.setCreatedAt(createdAt);
        importOrder.setStatus(ImportOrderStatus.COMPLETED);
        importOrder.setSupplierNameSnapshot(supplierName);

        ImportOrderItem item = new ImportOrderItem();
        item.setImportOrder(importOrder);
        item.setProduct(product);
        item.setQuantity(12);
        item.setImportPrice(importPrice);
        return item;
    }
}
