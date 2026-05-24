package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.dto.CreateOrderRequest;
import com.pbl3.project.pbl3_project.entity.Order;
import com.pbl3.project.pbl3_project.entity.PaymentMethod;
import com.pbl3.project.pbl3_project.entity.Product;
import com.pbl3.project.pbl3_project.entity.Promotion;
import com.pbl3.project.pbl3_project.entity.PromotionDiscountType;
import com.pbl3.project.pbl3_project.entity.PromotionScope;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServicePromotionPricingTest {

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
    void createOrderStoresPromotionSnapshotsAndTotals() {
        User actor = new User();
        actor.setId(1L);
        actor.setUsername("staff");
        actor.setFullName("Staff User");
        actor.setRole(Role.STAFF);
        actor.setEnabled(true);

        Product product = new Product();
        product.setId(11L);
        product.setName("Oat Milk");
        product.setPrice(new BigDecimal("100000"));
        product.setImportPrice(new BigDecimal("60000"));
        product.setQuantity(20);

        Promotion productPromotion = new Promotion();
        productPromotion.setId(501L);
        productPromotion.setName("Milk 20%");
        productPromotion.setScope(PromotionScope.PRODUCT);
        productPromotion.setDiscountType(PromotionDiscountType.PERCENT);
        productPromotion.setDiscountValue(new BigDecimal("20"));

        Promotion orderPromotion = new Promotion();
        orderPromotion.setId(601L);
        orderPromotion.setName("Order 10%");
        orderPromotion.setScope(PromotionScope.ORDER);
        orderPromotion.setDiscountType(PromotionDiscountType.PERCENT);
        orderPromotion.setDiscountValue(new BigDecimal("10"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(productRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(product));
        when(promotionService.previewBestProductPricing(anyCollection(), any())).thenReturn(Map.of(
            11L,
            new PromotionService.ProductPricingPreview(
                product,
                productPromotion,
                new BigDecimal("100000"),
                new BigDecimal("80000"),
                new BigDecimal("20000")
            )
        ));
        when(promotionService.resolveEligibleOrderPromotion(eq(601L), any(), any())).thenReturn(orderPromotion);
        when(promotionService.computeDiscountAmount(eq(orderPromotion), any())).thenReturn(new BigDecimal("16000"));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setId(77L);
            return saved;
        });

        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(1L);
        request.setPaymentMethod(PaymentMethod.CASH);
        request.setSelectedOrderPromotionId(601L);
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setProductId(11L);
        item.setQuantity(2);
        request.setItems(new ArrayList<>(java.util.List.of(item)));

        Order saved = orderService.createOrder(request);

        assertEquals(MoneySupport.normalize(new BigDecimal("200000")), saved.getGrossSubtotal());
        assertEquals(MoneySupport.normalize(new BigDecimal("56000")), saved.getDiscountTotal());
        assertEquals(MoneySupport.normalize(new BigDecimal("16000")), MoneySupport.normalize(saved.getOrderLevelDiscountTotal()));
        assertEquals(MoneySupport.normalize(new BigDecimal("144000")), saved.getTotalPrice());
        assertEquals(Long.valueOf(601L), saved.getAppliedOrderPromotionIdSnapshot());
        assertEquals("Order 10%", saved.getAppliedOrderPromotionNameSnapshot());

        var savedItem = saved.getOrderItems().get(0);
        assertEquals(MoneySupport.normalize(new BigDecimal("100000")), MoneySupport.normalize(savedItem.getOriginalUnitPrice()));
        assertEquals(MoneySupport.normalize(new BigDecimal("80000")), MoneySupport.normalize(savedItem.getPrice()));
        assertEquals(MoneySupport.normalize(new BigDecimal("40000")), MoneySupport.normalize(savedItem.getLinePromotionDiscountAmount()));
        assertEquals(MoneySupport.normalize(new BigDecimal("16000")), MoneySupport.normalize(savedItem.getOrderLevelDiscountAllocatedAmount()));
        assertEquals(Long.valueOf(501L), savedItem.getAppliedProductPromotionIdSnapshot());
        assertEquals("Milk 20%", savedItem.getAppliedProductPromotionNameSnapshot());
        verify(inventoryLedgerService).ensureBaseline(product);
    }
}
