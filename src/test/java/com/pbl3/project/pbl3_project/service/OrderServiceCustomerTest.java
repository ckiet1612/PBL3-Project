package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.dto.CreateOrderRequest;
import com.pbl3.project.pbl3_project.entity.Customer;
import com.pbl3.project.pbl3_project.entity.Order;
import com.pbl3.project.pbl3_project.entity.PaymentMethod;
import com.pbl3.project.pbl3_project.entity.Product;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceCustomerTest {

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
    void createOrderStoresCustomerRelationAndSnapshots() {
        User actor = new User();
        actor.setId(1L);
        actor.setUsername("staff");
        actor.setFullName("Staff User");
        actor.setRole(Role.STAFF);
        actor.setEnabled(true);

        Customer customer = new Customer();
        customer.setId(7L);
        customer.setFullName("Alice Nguyen");
        customer.setPhone("0901234567");
        customer.setEnabled(true);

        Product product = new Product();
        product.setId(11L);
        product.setName("Keyboard");
        product.setPrice(new BigDecimal("150000.00"));
        product.setImportPrice(new BigDecimal("100000.00"));
        product.setQuantity(10);

        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(customerRepository.findById(7L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(11L)).thenReturn(Optional.of(product));
        when(promotionService.previewBestProductPricing(org.mockito.ArgumentMatchers.anyCollection(), any())).thenReturn(java.util.Map.of());
        when(promotionService.resolveEligibleOrderPromotion(any(), any(), any())).thenReturn(null);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(99L);
            return order;
        });

        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(1L);
        request.setCustomerId(7L);
        request.setPaymentMethod(PaymentMethod.CASH);
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setProductId(11L);
        item.setQuantity(2);
        request.setItems(new ArrayList<>(java.util.List.of(item)));

        Order saved = orderService.createOrder(request);

        assertNotNull(saved.getCustomer());
        assertEquals(7L, saved.getCustomer().getId());
        assertEquals("Alice Nguyen", saved.getCustomerNameSnapshot());
        assertEquals("0901234567", saved.getCustomerPhoneSnapshot());
        assertEquals("Alice Nguyen", saved.getCustomerDisplayName());
        assertEquals("0901234567", saved.getCustomerPhoneDisplay());
        assertEquals(new BigDecimal("300000.00"), saved.getTotalPrice());
        verify(inventoryLedgerService).ensureBaseline(product);
    }

    @Test
    void createOrderAllowsGuestCheckoutWhenCustomerIdIsMissing() {
        User actor = new User();
        actor.setId(1L);
        actor.setUsername("staff");
        actor.setFullName("Staff User");
        actor.setRole(Role.STAFF);
        actor.setEnabled(true);

        Product product = new Product();
        product.setId(11L);
        product.setName("Keyboard");
        product.setPrice(new BigDecimal("150000.00"));
        product.setImportPrice(new BigDecimal("100000.00"));
        product.setQuantity(10);

        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(productRepository.findById(11L)).thenReturn(Optional.of(product));
        when(promotionService.previewBestProductPricing(org.mockito.ArgumentMatchers.anyCollection(), any())).thenReturn(java.util.Map.of());
        when(promotionService.resolveEligibleOrderPromotion(any(), any(), any())).thenReturn(null);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(1L);
        request.setPaymentMethod(PaymentMethod.CASH);
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setProductId(11L);
        item.setQuantity(1);
        request.setItems(new ArrayList<>(java.util.List.of(item)));

        Order saved = orderService.createOrder(request);

        assertNull(saved.getCustomer());
        assertNull(saved.getCustomerNameSnapshot());
        assertNull(saved.getCustomerPhoneSnapshot());
        assertEquals("Guest", saved.getCustomerDisplayName());
    }

    @Test
    void createOrderRejectsDisabledCustomer() {
        User actor = new User();
        actor.setId(1L);
        actor.setUsername("staff");
        actor.setRole(Role.STAFF);
        actor.setEnabled(true);

        Customer customer = new Customer();
        customer.setId(7L);
        customer.setFullName("Alice Nguyen");
        customer.setPhone("0901234567");
        customer.setEnabled(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(customerRepository.findById(7L)).thenReturn(Optional.of(customer));

        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(1L);
        request.setCustomerId(7L);
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setProductId(11L);
        item.setQuantity(1);
        request.setItems(new ArrayList<>(java.util.List.of(item)));

        ValidationException ex = assertThrows(ValidationException.class, () -> orderService.createOrder(request));
        assertEquals("Selected customer is disabled", ex.getMessage());
    }
}
