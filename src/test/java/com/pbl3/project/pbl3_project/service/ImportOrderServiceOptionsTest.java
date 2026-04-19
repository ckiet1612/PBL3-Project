package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.dto.IdLabelOption;
import com.pbl3.project.pbl3_project.repository.ImportOrderRepository;
import com.pbl3.project.pbl3_project.repository.ProductRepository;
import com.pbl3.project.pbl3_project.repository.SupplierRepository;
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
class ImportOrderServiceOptionsTest {

    @Mock
    private ImportOrderRepository importOrderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private InventoryTransactionService inventoryTransactionService;
    @Mock
    private InventoryLedgerService inventoryLedgerService;
    @Mock
    private AuthorizationService authorizationService;
    @Mock
    private OperationalAuditLogService operationalAuditLogService;

    private ImportOrderService importOrderService;

    @BeforeEach
    void setUp() {
        importOrderService = new ImportOrderService(
            importOrderRepository,
            productRepository,
            userRepository,
            supplierRepository,
            inventoryTransactionService,
            inventoryLedgerService,
            authorizationService,
            operationalAuditLogService
        );
    }

    @Test
    void supplierOptionsKeepDistinctIdsEvenWhenLabelsMatch() {
        when(importOrderRepository.findDistinctSupplierOptions()).thenReturn(List.of(
            new IdLabelOption(10L, "Vina"),
            new IdLabelOption(11L, "Vina")
        ));

        List<IdLabelOption> options = importOrderService.getImportSupplierOptions();

        assertEquals(2, options.size());
        assertEquals(10L, options.get(0).id());
        assertEquals("Vina #10", options.get(0).label());
        assertEquals(11L, options.get(1).id());
        assertEquals("Vina #11", options.get(1).label());
    }
}
