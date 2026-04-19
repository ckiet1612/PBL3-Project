package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.dto.report.OperationalInsightBundle;
import com.pbl3.project.pbl3_project.entity.Order;
import com.pbl3.project.pbl3_project.entity.OrderItem;
import com.pbl3.project.pbl3_project.entity.OrderStatus;
import com.pbl3.project.pbl3_project.entity.Product;
import com.pbl3.project.pbl3_project.entity.Promotion;
import com.pbl3.project.pbl3_project.entity.PromotionDiscountType;
import com.pbl3.project.pbl3_project.entity.PromotionScope;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServicePromotionIntegrationTest {

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
    void getOperationalReportDataIncludesPromotionImpactAndNetRevenueAfterDiscounts() {
        LocalDate startDate = LocalDate.of(2026, 4, 1);
        LocalDate endDate = LocalDate.of(2026, 4, 30);

        Order order = new Order();
        order.setId(10L);
        order.setCreatedAt(LocalDateTime.of(2026, 4, 10, 10, 0));
        order.setStatus(OrderStatus.COMPLETED);
        order.setGrossSubtotal(new BigDecimal("100000"));
        order.setDiscountTotal(new BigDecimal("25000"));
        order.setOrderLevelDiscountTotal(new BigDecimal("5000"));
        order.setAppliedOrderPromotionIdSnapshot(700L);
        order.setAppliedOrderPromotionNameSnapshot("Order 5%");
        order.setTotalPrice(new BigDecimal("75000"));
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
        item.setQuantity(1);
        item.setReturnedQuantity(0);
        item.setOriginalUnitPrice(new BigDecimal("100000"));
        item.setPrice(new BigDecimal("80000"));
        item.setLinePromotionDiscountAmount(new BigDecimal("20000"));
        item.setOrderLevelDiscountAllocatedAmount(new BigDecimal("5000"));
        item.setAppliedProductPromotionIdSnapshot(701L);
        item.setAppliedProductPromotionNameSnapshot("Notebook 20%");
        item.setCostAtSale(new BigDecimal("40000"));
        item.setProductNameSnapshot("Notebook");
        item.setCategoryNameSnapshot("Stationery");

        Promotion activeProductPromotion = new Promotion();
        activeProductPromotion.setId(701L);
        activeProductPromotion.setName("Notebook 20%");
        activeProductPromotion.setScope(PromotionScope.PRODUCT);
        activeProductPromotion.setDiscountType(PromotionDiscountType.PERCENT);
        activeProductPromotion.setDiscountValue(new BigDecimal("20"));
        activeProductPromotion.setEnabled(true);

        Promotion activeOrderPromotion = new Promotion();
        activeOrderPromotion.setId(700L);
        activeOrderPromotion.setName("Order 5%");
        activeOrderPromotion.setScope(PromotionScope.ORDER);
        activeOrderPromotion.setDiscountType(PromotionDiscountType.PERCENT);
        activeOrderPromotion.setDiscountValue(new BigDecimal("5"));
        activeOrderPromotion.setEnabled(true);

        when(orderRepository.findAllWithinDateRange(any(), any())).thenReturn(List.of(order));
        when(orderItemRepository.findAllWithOrderAndProductWithinDateRange(any(), any())).thenReturn(List.of(item));
        when(productRepository.findAllActiveWithCategory()).thenReturn(List.of(product));
        when(inventoryTransactionRepository.findAllWithProductOrderByCreatedAtDesc()).thenReturn(List.of());
        when(expenseRepository.sumAmountBetween(startDate, endDate)).thenReturn(BigDecimal.ZERO);
        when(expenseRepository.findCategorySummariesBetween(startDate, endDate)).thenReturn(List.of());
        when(promotionRepository.findAllActiveAt(any())).thenReturn(List.of(activeProductPromotion, activeOrderPromotion));
        when(operationalInsightService.getOperationalReportInsights(startDate, endDate))
            .thenReturn(new OperationalInsightBundle(null, null, null));

        var report = reportService.getOperationalReportData(startDate, endDate);

        assertEquals(MoneySupport.normalize(new BigDecimal("75000")), report.summary().netRevenue());
        assertEquals(MoneySupport.normalize(new BigDecimal("25000")), report.promotionReport().totalDiscount());
        assertEquals(1L, report.promotionReport().promotedOrderCount());
        assertEquals(2, report.promotionReport().topPromotions().size());
        assertEquals("Notebook 20%", report.promotionReport().topPromotions().get(0).promotionName());
        assertEquals(MoneySupport.normalize(new BigDecimal("20000")), report.promotionReport().topPromotions().get(0).totalDiscount());
        assertEquals(2, report.promotionReport().activePromotions().size());
    }
}
