package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.dto.IdLabelOption;
import com.pbl3.project.pbl3_project.repository.CustomerRepository;
import com.pbl3.project.pbl3_project.repository.OrderRepository;
import com.pbl3.project.pbl3_project.repository.ProductRepository;
import com.pbl3.project.pbl3_project.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceOptionsTest {

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
    void creatorOptionsKeepDistinctIdsEvenWhenLabelsMatch() {
        when(orderRepository.findDistinctCreatorOptions()).thenReturn(List.of(
            new IdLabelOption(1L, "Alex"),
            new IdLabelOption(2L, "Alex")
        ));

        List<IdLabelOption> options = orderService.getOrderCreatorOptions();

        assertEquals(2, options.size());
        assertEquals(1L, options.get(0).id());
        assertEquals("Alex #1", options.get(0).label());
        assertEquals(2L, options.get(1).id());
        assertEquals("Alex #2", options.get(1).label());
    }
}
