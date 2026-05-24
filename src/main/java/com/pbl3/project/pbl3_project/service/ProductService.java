package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.entity.InventoryTransactionType;
import com.pbl3.project.pbl3_project.entity.OperationalAuditAction;
import com.pbl3.project.pbl3_project.entity.OperationalSubjectType;
import com.pbl3.project.pbl3_project.entity.Product;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.repository.ProductRepository;
import jakarta.persistence.criteria.JoinType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class ProductService {
    public record CatalogFilterOptions(Set<String> brandNames, BigDecimal maxPrice, int maxQuantity) {
    }

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
        return searchProductCatalog(categoryId, false, search, selectedBrands, minPrice, maxPrice, minQuantity, maxQuantity, pageable);
    }

    public Page<Product> searchProductCatalog(
        Long categoryId,
        boolean lowStockOnly,
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
            lowStockOnly
                ? Sort.by(Sort.Direction.ASC, "quantity").and(Sort.by(Sort.Direction.ASC, "name"))
                : Sort.by(Sort.Direction.ASC, "name"),
            java.util.Set.of("id", "sku", "name", "category.name", "brand.name", "price", "importPrice", "quantity", "minStockLevel")
        );
        return productRepository.findAll(
            buildProductCatalogSpec(categoryId, lowStockOnly, search, selectedBrands, minPrice, maxPrice, minQuantity, maxQuantity),
            sanitizedPageable
        );
    }

    public Page<Product> searchLowStockProducts(
        String search,
        Set<String> selectedBrands,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Integer minQuantity,
        Integer maxQuantity,
        Pageable pageable
    ) {
        return searchProductCatalog(null, true, search, selectedBrands, minPrice, maxPrice, minQuantity, maxQuantity, pageable);
    }

    private Specification<Product> buildProductCatalogSpec(
        Long categoryId,
        boolean lowStockOnly,
        String search,
        Set<String> selectedBrands,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Integer minQuantity,
        Integer maxQuantity
    ) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            var brandJoin = root.join("brand", JoinType.LEFT);
            var categoryJoin = root.join("category", JoinType.LEFT);

            predicates.add(cb.isFalse(root.get("isDeleted")));
            if (categoryId != null) {
                predicates.add(cb.equal(categoryJoin.get("id"), categoryId));
            }
            if (lowStockOnly) {
                predicates.add(cb.lessThanOrEqualTo(root.get("quantity"), root.get("minStockLevel")));
            }

            String normalizedSearch = search == null ? null : search.trim().toLowerCase();
            if (normalizedSearch != null && !normalizedSearch.isEmpty()) {
                String likeValue = "%" + normalizedSearch + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(cb.coalesce(root.get("name"), "")), likeValue),
                    cb.like(cb.lower(cb.coalesce(root.get("sku"), "")), likeValue),
                    cb.like(cb.lower(cb.coalesce(root.get("barcode"), "")), likeValue),
                    cb.like(cb.lower(cb.coalesce(brandJoin.get("name"), "")), likeValue),
                    cb.like(cb.lower(cb.coalesce(categoryJoin.get("name"), "")), likeValue)
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
    }

    public Set<String> getBrandNamesByCategory(Long categoryId) {
        return new LinkedHashSet<>(productRepository.findDistinctBrandNamesByCategoryId(categoryId));
    }

    public Set<String> getLowStockBrandNames() {
        return getBrandNamesForCatalog(null, true);
    }

    public Set<String> getBrandNamesForCatalog(Long categoryId, boolean lowStockOnly) {
        return productRepository.findCatalogBrandNames(categoryId, lowStockOnly).stream()
            .filter(name -> name != null && !name.isBlank())
            .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    public CatalogFilterOptions getCatalogFilterOptions(Long categoryId, boolean lowStockOnly) {
        Set<String> brandNames = getBrandNamesForCatalog(categoryId, lowStockOnly);
        BigDecimal maxPrice = productRepository.findCatalogMaxPrice(categoryId, lowStockOnly);
        Integer maxQuantity = productRepository.findCatalogMaxQuantity(categoryId, lowStockOnly);
        return new CatalogFilterOptions(
            brandNames,
            maxPrice != null ? maxPrice : BigDecimal.ZERO,
            maxQuantity != null ? maxQuantity : 0
        );
    }

    public BigDecimal getMaxPriceByCategory(Long categoryId) {
        return productRepository.findMaxPriceByCategoryId(categoryId);
    }

    public BigDecimal getMaxPriceForLowStock() {
        return getMaxPriceForCatalog(null, true);
    }

    public BigDecimal getMaxPriceForCatalog(Long categoryId, boolean lowStockOnly) {
        BigDecimal maxPrice = productRepository.findCatalogMaxPrice(categoryId, lowStockOnly);
        return maxPrice != null ? maxPrice : BigDecimal.ZERO;
    }

    public int getMaxQuantityByCategory(Long categoryId) {
        Integer maxQuantity = productRepository.findMaxQuantityByCategoryId(categoryId);
        return maxQuantity != null ? maxQuantity : 0;
    }

    public int getMaxQuantityForLowStock() {
        return getMaxQuantityForCatalog(null, true);
    }

    public int getMaxQuantityForCatalog(Long categoryId, boolean lowStockOnly) {
        Integer maxQuantity = productRepository.findCatalogMaxQuantity(categoryId, lowStockOnly);
        return maxQuantity != null ? maxQuantity : 0;
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    @Transactional
    public Product saveProduct(Product product) {
        normalizeProductIdentifiers(product);
        if (product.getId() == null) {
            try {
                validateUniqueBarcode(product);
                Product saved = productRepository.save(product);
                inventoryLedgerService.ensureBaseline(saved);
                operationalAuditLogService.recordChange(
                    null,
                    OperationalAuditAction.PRODUCT_CREATED,
                    OperationalSubjectType.PRODUCT,
                    saved.getId(),
                    saved.getName(),
                    "Product created",
                    null,
                    productSnapshot(saved)
                );
                return saved;
            } catch (DataIntegrityViolationException ex) {
                throw new ValidationException("Product SKU or barcode already exists");
            }
        }
        return saveProduct(product, null, "System product update");
    }

    @Transactional
    public Product saveProduct(Product product, User user, String notes) {
        if (user != null) {
            authorizationService.requireProductWrite(user);
        }
        try {
            normalizeProductIdentifiers(product);
            validateUniqueBarcode(product);
            if (product.getId() == null) {
                Product saved = productRepository.save(product);
                inventoryLedgerService.ensureBaseline(saved);
                operationalAuditLogService.recordChange(
                    user,
                    OperationalAuditAction.PRODUCT_CREATED,
                    OperationalSubjectType.PRODUCT,
                    saved.getId(),
                    saved.getName(),
                    normalizeNotes(notes, "Product created"),
                    null,
                    productSnapshot(saved)
                );
                return saved;
            }

            Product current = productRepository.findById(product.getId())
                .orElseThrow(() -> new ValidationException("Product not found: " + product.getId()));
            Map<String, Object> beforeSnapshot = productSnapshot(current);

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

            operationalAuditLogService.recordChange(
                user,
                OperationalAuditAction.PRODUCT_UPDATED,
                OperationalSubjectType.PRODUCT,
                current.getId(),
                current.getName(),
                normalizeNotes(notes, "Product updated"),
                beforeSnapshot,
                productSnapshot(current)
            );
            return current;
        } catch (OptimisticLockingFailureException ex) {
            throw new ConcurrencyConflictException("Data changed, reload and try again");
        } catch (DataIntegrityViolationException ex) {
            throw new ValidationException("Product SKU or barcode already exists");
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
                Map<String, Object> beforeSnapshot = productSnapshot(p);
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
                operationalAuditLogService.recordChange(
                    user,
                    OperationalAuditAction.PRODUCT_DELETED,
                    OperationalSubjectType.PRODUCT,
                    p.getId(),
                    p.getName(),
                    "Product deleted",
                    beforeSnapshot,
                    productSnapshot(p)
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

    private Map<String, Object> productSnapshot(Product product) {
        if (product == null) {
            return null;
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", product.getId());
        snapshot.put("name", product.getName());
        snapshot.put("description", product.getDescription());
        snapshot.put("sku", product.getSku());
        snapshot.put("barcode", product.getBarcode());
        snapshot.put("price", MoneySupport.normalize(product.getPrice()));
        snapshot.put("importPrice", MoneySupport.normalize(product.getImportPrice()));
        snapshot.put("quantity", product.getQuantity());
        snapshot.put("minStockLevel", product.getMinStockLevel());
        snapshot.put("deleted", product.isDeleted());
        snapshot.put("category", relatedSnapshot(product.getCategory()));
        snapshot.put("brand", relatedSnapshot(product.getBrand()));
        snapshot.put("origin", relatedSnapshot(product.getOrigin()));
        snapshot.put("unit", relatedSnapshot(product.getUnit()));
        snapshot.put("version", product.getVersion());
        return snapshot;
    }

    private Map<String, Object> relatedSnapshot(Object entity) {
        if (entity == null) {
            return null;
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        if (entity instanceof com.pbl3.project.pbl3_project.entity.Category category) {
            snapshot.put("id", category.getId());
            snapshot.put("name", category.getName());
        } else if (entity instanceof com.pbl3.project.pbl3_project.entity.Brand brand) {
            snapshot.put("id", brand.getId());
            snapshot.put("name", brand.getName());
        } else if (entity instanceof com.pbl3.project.pbl3_project.entity.Origin origin) {
            snapshot.put("id", origin.getId());
            snapshot.put("name", origin.getName());
        } else if (entity instanceof com.pbl3.project.pbl3_project.entity.Unit unit) {
            snapshot.put("id", unit.getId());
            snapshot.put("name", unit.getName());
        }
        return snapshot;
    }

    private String normalizeNotes(String notes, String fallback) {
        if (notes == null || notes.trim().isEmpty()) {
            return fallback;
        }
        return notes.trim();
    }

    private void normalizeProductIdentifiers(Product product) {
        if (product == null) {
            throw new ValidationException("Product is required");
        }
        product.setSku(blankToNull(product.getSku()));
        product.setBarcode(blankToNull(product.getBarcode()));
    }

    private void validateUniqueBarcode(Product product) {
        String barcode = product.getBarcode();
        if (barcode == null) {
            return;
        }
        boolean duplicate = product.getId() == null
            ? productRepository.existsByBarcode(barcode)
            : productRepository.existsByBarcodeAndIdNot(barcode, product.getId());
        if (duplicate) {
            throw new ValidationException("Barcode already exists: " + barcode);
        }
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
