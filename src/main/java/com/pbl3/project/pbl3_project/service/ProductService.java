package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.entity.InventoryTransactionType;
import com.pbl3.project.pbl3_project.entity.OperationalAuditAction;
import com.pbl3.project.pbl3_project.entity.OperationalSubjectType;
import com.pbl3.project.pbl3_project.entity.Product;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.repository.ProductRepository;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryTransactionService transactionService;
    private final InventoryLedgerService inventoryLedgerService;
    private final AuthorizationService authorizationService;
    private final OperationalAuditLogService operationalAuditLogService;

    public ProductService(
        ProductRepository productRepository,
        InventoryTransactionService transactionService,
        InventoryLedgerService inventoryLedgerService,
        AuthorizationService authorizationService,
        OperationalAuditLogService operationalAuditLogService
    ) {
        this.productRepository = productRepository;
        this.transactionService = transactionService;
        this.inventoryLedgerService = inventoryLedgerService;
        this.authorizationService = authorizationService;
        this.operationalAuditLogService = operationalAuditLogService;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAllByIsDeletedFalse();
    }

    public Page<Product> searchProducts(
        Long categoryId,
        String search,
        Set<String> selectedBrands,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Integer minQuantity,
        Integer maxQuantity,
        Pageable pageable
    ) {
        Pageable sanitizedPageable = PageSortSupport.sanitize(
            pageable,
            Sort.by(Sort.Direction.ASC, "name"),
            java.util.Set.of("id", "sku", "name", "brand.name", "price", "quantity")
        );
        Specification<Product> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            var brandJoin = root.join("brand", JoinType.LEFT);

            predicates.add(cb.isFalse(root.get("isDeleted")));
            predicates.add(cb.equal(root.get("category").get("id"), categoryId));

            String normalizedSearch = search == null ? null : search.trim().toLowerCase();
            if (normalizedSearch != null && !normalizedSearch.isEmpty()) {
                String likeValue = "%" + normalizedSearch + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(cb.coalesce(root.get("name"), "")), likeValue),
                    cb.like(cb.lower(cb.coalesce(root.get("sku"), "")), likeValue),
                    cb.like(cb.lower(cb.coalesce(brandJoin.get("name"), "")), likeValue)
                ));
            }

            if (selectedBrands != null && !selectedBrands.isEmpty()) {
                predicates.add(brandJoin.get("name").in(selectedBrands));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            if (minQuantity != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("quantity"), minQuantity));
            }
            if (maxQuantity != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("quantity"), maxQuantity));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return productRepository.findAll(
            spec,
            sanitizedPageable
        );
    }

    public Set<String> getBrandNamesByCategory(Long categoryId) {
        return new LinkedHashSet<>(productRepository.findDistinctBrandNamesByCategoryId(categoryId));
    }

    public BigDecimal getMaxPriceByCategory(Long categoryId) {
        return productRepository.findMaxPriceByCategoryId(categoryId);
    }

    public int getMaxQuantityByCategory(Long categoryId) {
        Integer maxQuantity = productRepository.findMaxQuantityByCategoryId(categoryId);
        return maxQuantity != null ? maxQuantity : 0;
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    @Transactional
    public Product saveProduct(Product product) {
        if (product.getId() == null) {
            Product saved = productRepository.save(product);
            inventoryLedgerService.ensureBaseline(saved);
            operationalAuditLogService.record(
                null,
                OperationalAuditAction.PRODUCT_CREATED,
                OperationalSubjectType.PRODUCT,
                saved.getId(),
                saved.getName(),
                "Product created"
            );
            return saved;
        }
        return saveProduct(product, null, "System product update");
    }

    @Transactional
    public Product saveProduct(Product product, User user, String notes) {
        if (user != null) {
            authorizationService.requireProductWrite(user);
        }
        try {
            if (product.getId() == null) {
                Product saved = productRepository.save(product);
                inventoryLedgerService.ensureBaseline(saved);
                operationalAuditLogService.record(
                    user,
                    OperationalAuditAction.PRODUCT_CREATED,
                    OperationalSubjectType.PRODUCT,
                    saved.getId(),
                    saved.getName(),
                    normalizeNotes(notes, "Product created")
                );
                return saved;
            }

            Product current = productRepository.findById(product.getId())
                .orElseThrow(() -> new ValidationException("Product not found: " + product.getId()));

            int oldQty = safeInt(current.getQuantity());
            BigDecimal oldAverageCost = MoneySupport.normalize(current.getImportPrice());
            int targetQty = safeInt(product.getQuantity());
            BigDecimal targetAverageCost = MoneySupport.normalize(product.getImportPrice());

            current.setName(product.getName());
            current.setDescription(product.getDescription());
            current.setPrice(product.getPrice());
            current.setImageUrl(product.getImageUrl());
            current.setSku(product.getSku());
            current.setBarcode(product.getBarcode());
            current.setBrand(product.getBrand());
            current.setOrigin(product.getOrigin());
            current.setUnit(product.getUnit());
            current.setCategory(product.getCategory());
            current.setDeleted(product.isDeleted());
            current.setMinStockLevel(product.getMinStockLevel());
            productRepository.save(current);

            inventoryLedgerService.ensureBaseline(current);

            int quantityDelta = targetQty - oldQty;
            if (quantityDelta != 0) {
                transactionService.recordTransaction(
                    current,
                    quantityDelta,
                    InventoryTransactionType.MANUAL_ADJUST,
                    null,
                    null,
                    user,
                    normalizeNotes(notes, "Manual quantity adjustment"),
                    oldAverageCost,
                    MoneySupport.multiply(oldAverageCost, quantityDelta)
                );
            }

            int resultingQuantity = oldQty + quantityDelta;
            if (MoneySupport.differs(targetAverageCost, oldAverageCost) && resultingQuantity > 0) {
                transactionService.recordTransaction(
                    current,
                    0,
                    InventoryTransactionType.REVALUE,
                    null,
                    null,
                    user,
                    normalizeNotes(notes, "Inventory cost revaluation"),
                    targetAverageCost,
                    MoneySupport.multiply(MoneySupport.subtract(targetAverageCost, oldAverageCost), resultingQuantity)
                );
            }

            if (quantityDelta != 0 || MoneySupport.differs(targetAverageCost, oldAverageCost)) {
                inventoryLedgerService.recomputeProductState(current);
            } else {
                productRepository.saveAndFlush(current);
            }

            operationalAuditLogService.record(
                user,
                OperationalAuditAction.PRODUCT_UPDATED,
                OperationalSubjectType.PRODUCT,
                current.getId(),
                current.getName(),
                normalizeNotes(notes, "Product updated")
            );
            return current;
        } catch (OptimisticLockingFailureException ex) {
            throw new ConcurrencyConflictException("Data changed, reload and try again");
        }
    }

    @Transactional
    public void deleteProduct(Long id) {
        deleteProduct(id, null);
    }

    @Transactional
    public void deleteProduct(Long id, User user) {
        if (user != null) {
            authorizationService.requireProductDelete(user);
        }
        try {
            Optional<Product> product = productRepository.findById(id);
            if (product.isPresent()) {
                Product p = product.get();
                inventoryLedgerService.ensureBaseline(p);

                int currentQuantity = safeInt(p.getQuantity());
                BigDecimal currentAverageCost = MoneySupport.normalize(p.getImportPrice());
                if (currentQuantity > 0) {
                    transactionService.recordTransaction(
                        p,
                        -currentQuantity,
                        InventoryTransactionType.DELETE,
                        null,
                        null,
                        user,
                        "Product deleted",
                        currentAverageCost,
                        MoneySupport.multiply(currentAverageCost, -currentQuantity)
                    );
                    inventoryLedgerService.recomputeProductState(p);
                }

                p.setDeleted(true);
                productRepository.saveAndFlush(p);
                operationalAuditLogService.record(
                    user,
                    OperationalAuditAction.PRODUCT_DELETED,
                    OperationalSubjectType.PRODUCT,
                    p.getId(),
                    p.getName(),
                    "Product deleted"
                );
            }
        } catch (OptimisticLockingFailureException ex) {
            throw new ConcurrencyConflictException("Data changed, reload and try again");
        }
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private BigDecimal safeDouble(BigDecimal value) {
        return MoneySupport.normalize(value);
    }

    private String normalizeNotes(String notes, String fallback) {
        if (notes == null || notes.trim().isEmpty()) {
            return fallback;
        }
        return notes.trim();
    }
}
