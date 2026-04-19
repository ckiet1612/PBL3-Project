package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.entity.ImportOrder;
import com.pbl3.project.pbl3_project.entity.ImportOrderItem;
import com.pbl3.project.pbl3_project.entity.InventoryPositionBaseline;
import com.pbl3.project.pbl3_project.entity.InventoryTransaction;
import com.pbl3.project.pbl3_project.entity.InventoryTransactionType;
import com.pbl3.project.pbl3_project.entity.Product;
import com.pbl3.project.pbl3_project.repository.InventoryPositionBaselineRepository;
import com.pbl3.project.pbl3_project.repository.InventoryTransactionRepository;
import com.pbl3.project.pbl3_project.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class InventoryLedgerService {

    private final InventoryPositionBaselineRepository baselineRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final ProductRepository productRepository;

    public InventoryLedgerService(
        InventoryPositionBaselineRepository baselineRepository,
        InventoryTransactionRepository transactionRepository,
        ProductRepository productRepository
    ) {
        this.baselineRepository = baselineRepository;
        this.transactionRepository = transactionRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public void ensureBaselinesForAllProducts() {
        for (Product product : productRepository.findAll()) {
            ensureBaseline(product);
        }
    }

    @Transactional
    public InventoryPositionBaseline ensureBaseline(Product product) {
        if (product == null || product.getId() == null) {
            throw new ValidationException("Cannot create inventory baseline for unsaved product");
        }
        return baselineRepository.findByProductId(product.getId())
            .orElseGet(() -> createBaseline(product));
    }

    @Transactional
    public InventoryComputation recomputeProductState(Product product) {
        InventoryPositionBaseline baseline = ensureBaseline(product);
        InventoryComputation computation = replayProduct(product, baseline, Set.of());
        applyToProduct(product, computation);
        productRepository.saveAndFlush(product);
        return computation;
    }

    @Transactional
    public void recomputeProducts(Collection<Product> products) {
        if (products == null) {
            return;
        }
        Set<Long> seenProductIds = new HashSet<>();
        for (Product product : products) {
            if (product == null || product.getId() == null || !seenProductIds.add(product.getId())) {
                continue;
            }
            recomputeProductState(product);
        }
    }

    @Transactional(readOnly = true)
    public void validateCancelableImportReplay(ImportOrder importOrder) {
        if (importOrder == null || importOrder.getItems() == null || importOrder.getItems().isEmpty()) {
            return;
        }

        List<InventoryTransaction> importTransactions = transactionRepository
            .findByImportOrderIdAndTransactionTypeOrderByCreatedAtAscIdAsc(importOrder.getId(), InventoryTransactionType.IMPORT);
        if (importTransactions.isEmpty()) {
            throw new UnsafeLegacyOperationException("Legacy import cannot be canceled safely");
        }

        for (ImportOrderItem item : importOrder.getItems()) {
            Product product = item.getProduct();
            if (product == null || product.getId() == null) {
                continue;
            }

            InventoryPositionBaseline baseline = baselineRepository.findByProductId(product.getId())
                .orElseThrow(() -> new ValidationException("Missing inventory baseline for product: " + safeProductName(product)));

            if (importOrder.getCreatedAt() == null || importOrder.getCreatedAt().isBefore(baseline.getBaselineAt())) {
                throw new UnsafeLegacyOperationException("Legacy import cannot be canceled safely");
            }

            Set<Long> excludedTransactionIds = importTransactions.stream()
                .filter(tx -> tx.getProduct() != null && product.getId().equals(tx.getProduct().getId()))
                .map(InventoryTransaction::getId)
                .collect(java.util.stream.Collectors.toSet());

            if (excludedTransactionIds.isEmpty()) {
                throw new UnsafeLegacyOperationException("Legacy import cannot be canceled safely");
            }

            replayProduct(product, baseline, excludedTransactionIds);
        }
    }

    private InventoryPositionBaseline createBaseline(Product product) {
        InventoryPositionBaseline baseline = new InventoryPositionBaseline();
        int quantity = safeQuantity(product.getQuantity());
        BigDecimal averageCost = MoneySupport.normalize(product.getImportPrice());
        baseline.setProduct(product);
        baseline.setBaselineAt(LocalDateTime.now());
        baseline.setQuantity(quantity);
        baseline.setAverageCost(averageCost);
        baseline.setInventoryValue(MoneySupport.multiply(averageCost, quantity));
        return baselineRepository.save(baseline);
    }

    private InventoryComputation replayProduct(
        Product product,
        InventoryPositionBaseline baseline,
        Set<Long> excludedTransactionIds
    ) {
        int quantity = safeQuantity(baseline.getQuantity());
        BigDecimal inventoryValue = MoneySupport.normalize(baseline.getInventoryValue());
        List<InventoryTransaction> transactions = transactionRepository
            .findByProductIdAndCreatedAtGreaterThanEqualOrderByCreatedAtAscIdAsc(product.getId(), baseline.getBaselineAt());

        for (InventoryTransaction transaction : transactions) {
            if (excludedTransactionIds != null && excludedTransactionIds.contains(transaction.getId())) {
                continue;
            }
            quantity += safeQuantity(transaction.getQuantityChange());
            inventoryValue = MoneySupport.add(inventoryValue, resolveInventoryValueChange(transaction));
            if (quantity < 0) {
                throw new ValidationException("Inventory replay would result in negative stock for product: " + safeProductName(product));
            }
        }

        BigDecimal normalizedInventoryValue = quantity == 0 ? MoneySupport.ZERO : inventoryValue;
        BigDecimal averageCost = quantity > 0
            ? MoneySupport.divide(normalizedInventoryValue, quantity)
            : MoneySupport.ZERO;
        return new InventoryComputation(quantity, normalizedInventoryValue, averageCost);
    }

    private void applyToProduct(Product product, InventoryComputation computation) {
        product.setQuantity(computation.quantity());
        product.setImportPrice(computation.averageCost());
    }

    private BigDecimal resolveInventoryValueChange(InventoryTransaction transaction) {
        if (transaction.getInventoryValueChange() != null) {
            return MoneySupport.normalize(transaction.getInventoryValueChange());
        }
        BigDecimal unitCost = MoneySupport.normalize(transaction.getUnitCostSnapshot());
        return MoneySupport.multiply(unitCost, safeQuantity(transaction.getQuantityChange()));
    }

    private int safeQuantity(Integer value) {
        return value != null ? value : 0;
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return MoneySupport.normalize(value);
    }

    private String safeProductName(Product product) {
        return product != null && product.getName() != null ? product.getName() : "Unknown";
    }

    public record InventoryComputation(int quantity, BigDecimal inventoryValue, BigDecimal averageCost) {
    }
}
