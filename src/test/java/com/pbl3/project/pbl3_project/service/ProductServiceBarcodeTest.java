package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.entity.Product;
import com.pbl3.project.pbl3_project.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceBarcodeTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryTransactionService transactionService;
    @Mock
    private InventoryLedgerService inventoryLedgerService;
    @Mock
    private AuthorizationService authorizationService;
    @Mock
    private OperationalAuditLogService operationalAuditLogService;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(
            productRepository,
            transactionService,
            inventoryLedgerService,
            authorizationService,
            operationalAuditLogService
        );
    }

    @Test
    void saveProductRejectsDuplicateNonEmptyBarcodeAfterTrim() {
        Product product = product("Coffee", " SKU-1 ", " 8938505974191 ");
        when(productRepository.existsByBarcode("8938505974191")).thenReturn(true);

        assertThatThrownBy(() -> productService.saveProduct(product))
            .isInstanceOf(ValidationException.class)
            .hasMessage("Barcode already exists: 8938505974191");

        assertThat(product.getSku()).isEqualTo("SKU-1");
        assertThat(product.getBarcode()).isEqualTo("8938505974191");
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void saveProductAllowsMultipleNullBarcodes() {
        Product product = product("Coffee", " SKU-1 ", "   ");
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product saved = productService.saveProduct(product);

        assertThat(saved.getSku()).isEqualTo("SKU-1");
        assertThat(saved.getBarcode()).isNull();
        verify(productRepository, never()).existsByBarcode(any());
        verify(productRepository).save(product);
    }

    @Test
    void saveProductTrimsBarcodeBeforeSaving() {
        Product product = product("Coffee", "SKU-1", " 8938505974191 ");
        when(productRepository.existsByBarcode("8938505974191")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product saved = productService.saveProduct(product);

        assertThat(saved.getBarcode()).isEqualTo("8938505974191");
        verify(productRepository).existsByBarcode("8938505974191");
        verify(productRepository).save(product);
    }

    private Product product(String name, String sku, String barcode) {
        Product product = new Product();
        product.setName(name);
        product.setSku(sku);
        product.setBarcode(barcode);
        product.setPrice(BigDecimal.valueOf(12_000));
        product.setImportPrice(BigDecimal.valueOf(8_000));
        product.setQuantity(10);
        product.setMinStockLevel(2);
        return product;
    }
}
