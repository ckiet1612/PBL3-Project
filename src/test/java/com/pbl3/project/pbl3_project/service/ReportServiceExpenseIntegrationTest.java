package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.dto.report.ExpenseCategorySummaryRow;
import com.pbl3.project.pbl3_project.dto.report.OperationalInsightBundle;
import com.pbl3.project.pbl3_project.entity.ExpenseCategory;
import com.pbl3.project.pbl3_project.entity.Order;
import com.pbl3.project.pbl3_project.entity.OrderItem;
import com.pbl3.project.pbl3_project.entity.OrderStatus;
import com.pbl3.project.pbl3_project.entity.Product;
import com.pbl3.project.pbl3_project.entity.Role;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.repository.ExpenseRepository;
import com.pbl3.project.pbl3_project.repository.InventoryTransactionRepository;
import com.pbl3.project.pbl3_project.repository.OrderItemRepository;
import com.pbl3.project.pbl3_project.repository.OrderRepository;
import com.pbl3.project.pbl3_project.repository.ProductRepository;
import com.pbl3.project.pbl3_project.repository.PromotionRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceExpenseIntegrationTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private PromotionRepository promotionRepository;
    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;
    @Mock
    private AuthorizationService authorizationService;
    @Mock
    private OperationalInsightService operationalInsightService;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(
            orderRepository,
            orderItemRepository,
            productRepository,
            promotionRepository,
            expenseRepository,
            inventoryTransactionRepository,
            authorizationService,
            operationalInsightService
        );
    }

    @Test
    void getOperationalReportDataSeparatesGrossProfitAndNetProfitAfterExpenses() {
        LocalDate startDate = LocalDate.of(2026, 4, 1);
        LocalDate endDate = LocalDate.of(2026, 4, 30);

        Order order = new Order();
        order.setId(10L);
        order.setCreatedAt(LocalDateTime.of(2026, 4, 10, 10, 0));
        order.setStatus(OrderStatus.COMPLETED);
        order.setTotalPrice(new BigDecimal("1000000"));
        order.setRefundedAmount(BigDecimal.ZERO);

        Product product = new Product();
        product.setId(50L);
        product.setName("Notebook");
        product.setPrice(new BigDecimal("100000"));
        product.setImportPrice(new BigDecimal("40000"));
        product.setQuantity(30);
        product.setMinStockLevel(5);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(10);
        item.setReturnedQuantity(0);
        item.setPrice(new BigDecimal("100000"));
        item.setCostAtSale(new BigDecimal("40000"));
        item.setProductNameSnapshot("Notebook");
        item.setCategoryNameSnapshot("Stationery");

        when(orderRepository.findAllWithinDateRange(any(), any())).thenReturn(List.of(order));
        when(orderItemRepository.findAllWithOrderAndProductWithinDateRange(any(), any())).thenReturn(List.of(item));
        when(productRepository.findAllActiveWithCategory()).thenReturn(List.of(product));
        when(inventoryTransactionRepository.findAllWithProductOrderByCreatedAtDesc()).thenReturn(List.of());
        when(expenseRepository.sumAmountBetween(startDate, endDate)).thenReturn(new BigDecimal("250000"));
        when(expenseRepository.findCategorySummariesBetween(startDate, endDate)).thenReturn(List.of(
            new ExpenseCategorySummaryRow(ExpenseCategory.RENT, new BigDecimal("200000"), 1L),
            new ExpenseCategorySummaryRow(ExpenseCategory.UTILITIES, new BigDecimal("50000"), 1L)
        ));
        when(promotionRepository.findAllActiveAt(any())).thenReturn(List.of());
        when(operationalInsightService.getOperationalReportInsights(startDate, endDate))
            .thenReturn(new OperationalInsightBundle(null, null, null));

        var report = reportService.getOperationalReportData(startDate, endDate);

        assertEquals(MoneySupport.normalize(new BigDecimal("1000000")), report.summary().netRevenue());
        assertEquals(MoneySupport.normalize(new BigDecimal("400000")), report.summary().estimatedCost());
        assertEquals(MoneySupport.normalize(new BigDecimal("600000")), report.summary().grossProfit());
        assertEquals(MoneySupport.normalize(new BigDecimal("250000")), report.summary().operatingExpenses());
        assertEquals(MoneySupport.normalize(new BigDecimal("350000")), report.summary().netProfit());
        assertEquals(2, report.expenseCategorySummaries().size());
        assertEquals(ExpenseCategory.RENT, report.expenseCategorySummaries().get(0).category());
    }

    @Test
    void getDashboardOverviewDataHidesExpenseKpiForStaffButShowsForManager() {
        User manager = new User(1L, "manager", "secret", "Manager", Role.MANAGER, true);
        User staff = new User(2L, "staff", "secret", "Staff", Role.STAFF, true);
        LocalDate today = LocalDate.now();

        when(operationalInsightService.getDashboardInsights(any())).thenReturn(new OperationalInsightBundle(null, null, null));
        when(expenseRepository.sumAmountOn(any())).thenAnswer(invocation -> {
            LocalDate date = invocation.getArgument(0);
            if (today.equals(date)) {
                return new BigDecimal("300000");
            }
            if (today.minusDays(1).equals(date)) {
                return new BigDecimal("120000");
            }
            return BigDecimal.ZERO;
        });
        when(authorizationService.canViewAllOrders(manager)).thenReturn(true);
        when(authorizationService.canViewAllOrders(staff)).thenReturn(false);
        when(orderRepository.findAllWithinDateRangeAndUserId(any(), any(), any())).thenReturn(List.of());
        when(orderItemRepository.findAllWithOrderAndProductWithinDateRangeAndUserId(any(), any(), any())).thenReturn(List.of());
        when(orderRepository.sumRevenueBetween(any(), any())).thenReturn(BigDecimal.ZERO);
        when(orderRepository.countOrdersBetween(any(), any())).thenReturn(0L);
        when(productRepository.findAllActiveWithCategory()).thenReturn(List.of());
        when(productRepository.findLowStockProducts()).thenReturn(List.of());
        when(inventoryTransactionRepository.findAllWithProductOrderByCreatedAtDesc()).thenReturn(List.of());
        when(orderItemRepository.findAllWithOrderAndProduct()).thenReturn(List.of());
        when(orderRepository.findAll()).thenReturn(List.of());

        var managerOverview = reportService.getDashboardOverviewData(manager);
        var staffOverview = reportService.getDashboardOverviewData(staff);

        assertEquals(MoneySupport.normalize(new BigDecimal("300000")), managerOverview.todayExpenses());
        assertEquals(MoneySupport.normalize(new BigDecimal("180000")), managerOverview.expenseDeltaVsYesterday());
        assertEquals(MoneySupport.ZERO, staffOverview.todayExpenses());
        assertEquals(MoneySupport.ZERO, staffOverview.expenseDeltaVsYesterday());
    }
}
