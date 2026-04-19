package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.entity.InventoryTransactionType;
import com.pbl3.project.pbl3_project.entity.Product;
import com.pbl3.project.pbl3_project.entity.Role;
import com.pbl3.project.pbl3_project.entity.StocktakeItem;
import com.pbl3.project.pbl3_project.entity.StocktakeSession;
import com.pbl3.project.pbl3_project.entity.StocktakeSessionStatus;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.repository.InventoryTransactionRepository;
import com.pbl3.project.pbl3_project.repository.ProductRepository;
import com.pbl3.project.pbl3_project.repository.StocktakeSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StocktakeServiceTest {

    @Mock
    private StocktakeSessionRepository sessionRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryTransactionRepository transactionRepository;
    @Mock
    private InventoryTransactionService inventoryTransactionService;
    @Mock
    private InventoryLedgerService inventoryLedgerService;
    @Mock
    private OperationalAuditLogService operationalAuditLogService;
    @Mock
    private AuthorizationService authorizationService;

    private StocktakeService stocktakeService;

    @BeforeEach
    void setUp() {
        stocktakeService = new StocktakeService(
            sessionRepository,
            productRepository,
            transactionRepository,
            inventoryTransactionService,
            inventoryLedgerService,
            operationalAuditLogService,
            authorizationService
        );
        doNothing().when(authorizationService).requireStocktakeAccess(any());
    }

    @Test
    void updateSessionItemsRejectsNegativeCountedQuantity() {
        StocktakeSession session = openSessionWithSingleItem();
        when(sessionRepository.findByIdWithItems(1L)).thenReturn(Optional.of(session));

        assertThrows(
            ValidationException.class,
            () -> stocktakeService.updateSessionItems(
                actor(),
                1L,
                "notes",
                List.of(new StocktakeService.StocktakeItemUpdate(11L, -1, "bad"))
            )
        );
    }

    @Test
    void applySessionRejectsStaleSessionWhenInventoryChangedAfterStart() {
        StocktakeSession session = openSessionWithSingleItem();
        when(sessionRepository.findByIdWithItems(1L)).thenReturn(Optional.of(session));
        when(transactionRepository.existsByProductIdAndCreatedAtAfter(10L, session.getCreatedAt())).thenReturn(true);

        assertThrows(StaleStocktakeSessionException.class, () -> stocktakeService.applySession(actor(), 1L));
        verify(inventoryTransactionService, never()).recordTransaction(any(), any(), any(InventoryTransactionType.class), any(), any(), any(), any(), any(), any());
    }

    @Test
    void updateSessionItemsPersistsDraftValues() {
        StocktakeSession session = openSessionWithSingleItem();
        when(sessionRepository.findByIdWithItems(1L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(StocktakeSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StocktakeSession updated = stocktakeService.updateSessionItems(
            actor(),
            1L,
            "cycle count",
            List.of(new StocktakeService.StocktakeItemUpdate(11L, 8, "recounted"))
        );

        assertEquals("cycle count", updated.getNotes());
        assertEquals(8, updated.getItems().getFirst().getCountedQuantity());
        assertEquals("recounted", updated.getItems().getFirst().getNotes());

        ArgumentCaptor<StocktakeSession> captor = ArgumentCaptor.forClass(StocktakeSession.class);
        verify(sessionRepository).save(captor.capture());
        assertEquals("cycle count", captor.getValue().getNotes());
    }

    private StocktakeSession openSessionWithSingleItem() {
        Product product = new Product();
        product.setId(10L);
        product.setName("Laptop");
        product.setImportPrice(new BigDecimal("100.00"));
        product.setQuantity(5);

        StocktakeItem item = new StocktakeItem();
        item.setId(11L);
        item.setProduct(product);
        item.setSystemQuantity(5);
        item.setCountedQuantity(5);
        item.setUnitCostSnapshot(new BigDecimal("100.00"));

        StocktakeSession session = new StocktakeSession();
        session.setId(1L);
        session.setStatus(StocktakeSessionStatus.OPEN);
        session.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        session.setItems(List.of(item));
        item.setSession(session);
        return session;
    }

    private User actor() {
        User user = new User();
        user.setId(1L);
        user.setUsername("manager");
        user.setRole(Role.MANAGER);
        user.setEnabled(true);
        return user;
    }
}
