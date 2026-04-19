package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.entity.Category;
import com.pbl3.project.pbl3_project.entity.InventoryTransaction;
import com.pbl3.project.pbl3_project.entity.InventoryTransactionType;
import com.pbl3.project.pbl3_project.entity.OperationalAuditAction;
import com.pbl3.project.pbl3_project.entity.OperationalSubjectType;
import com.pbl3.project.pbl3_project.entity.Product;
import com.pbl3.project.pbl3_project.entity.StocktakeItem;
import com.pbl3.project.pbl3_project.entity.StocktakeScopeType;
import com.pbl3.project.pbl3_project.entity.StocktakeSession;
import com.pbl3.project.pbl3_project.entity.StocktakeSessionStatus;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.repository.InventoryTransactionRepository;
import com.pbl3.project.pbl3_project.repository.ProductRepository;
import com.pbl3.project.pbl3_project.repository.StocktakeSessionRepository;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class StocktakeService {

    private final StocktakeSessionRepository sessionRepository;
    private final ProductRepository productRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final InventoryTransactionService inventoryTransactionService;
    private final InventoryLedgerService inventoryLedgerService;
    private final OperationalAuditLogService operationalAuditLogService;
    private final AuthorizationService authorizationService;

    public StocktakeService(
        StocktakeSessionRepository sessionRepository,
        ProductRepository productRepository,
        InventoryTransactionRepository transactionRepository,
        InventoryTransactionService inventoryTransactionService,
        InventoryLedgerService inventoryLedgerService,
        OperationalAuditLogService operationalAuditLogService,
        AuthorizationService authorizationService
    ) {
        this.sessionRepository = sessionRepository;
        this.productRepository = productRepository;
        this.transactionRepository = transactionRepository;
        this.inventoryTransactionService = inventoryTransactionService;
        this.inventoryLedgerService = inventoryLedgerService;
        this.operationalAuditLogService = operationalAuditLogService;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public StocktakeSession createSession(User actor, StocktakeScopeType scopeType, Category category, String notes) {
        requireStocktakeAccess(actor);
        if (scopeType == null) {
            throw new ValidationException("Stocktake scope is required");
        }
        if (scopeType == StocktakeScopeType.CATEGORY && (category == null || category.getId() == null)) {
            throw new ValidationException("Category is required for category stocktake");
        }

        List<Product> products = scopeType == StocktakeScopeType.ALL_PRODUCTS
            ? productRepository.findAllByIsDeletedFalse()
            : productRepository.findAll().stream()
                .filter(product -> !product.isDeleted())
                .filter(product -> product.getCategory() != null && category.getId().equals(product.getCategory().getId()))
                .toList();
        if (products.isEmpty()) {
            throw new ValidationException("No products available for stocktake scope");
        }

        StocktakeSession session = new StocktakeSession();
        session.setCreatedBy(actor);
        session.setScopeType(scopeType);
        session.setCategory(scopeType == StocktakeScopeType.CATEGORY ? category : null);
        session.setStatus(StocktakeSessionStatus.OPEN);
        session.setNotes(notes != null && !notes.isBlank() ? notes.trim() : null);

        List<StocktakeItem> items = new ArrayList<>();
        for (Product product : products) {
            inventoryLedgerService.ensureBaseline(product);
            StocktakeItem item = new StocktakeItem();
            item.setSession(session);
            item.setProduct(product);
            item.setSystemQuantity(product.getQuantity() != null ? product.getQuantity() : 0);
            item.setUnitCostSnapshot(MoneySupport.normalize(product.getImportPrice()));
            item.setCountedQuantity(product.getQuantity() != null ? product.getQuantity() : 0);
            items.add(item);
        }
        session.setItems(items);
        StocktakeSession saved = sessionRepository.save(session);
        operationalAuditLogService.record(
            actor,
            OperationalAuditAction.STOCKTAKE_CREATED,
            OperationalSubjectType.STOCKTAKE_SESSION,
            saved.getId(),
            "Stocktake #" + saved.getId(),
            buildSessionDetails(saved)
        );
        return saved;
    }

    @Transactional
    public StocktakeSession applySession(User actor, Long sessionId) {
        requireStocktakeAccess(actor);
        StocktakeSession session = sessionRepository.findByIdWithItems(sessionId)
            .orElseThrow(() -> new ValidationException("Stocktake session not found"));
        if (session.getStatus() != StocktakeSessionStatus.OPEN) {
            throw new ValidationException("Only open stocktake sessions can be applied");
        }

        List<Product> affectedProducts = new ArrayList<>();
        for (StocktakeItem item : session.getItems()) {
            Product product = item.getProduct();
            if (product == null || product.getId() == null) {
                continue;
            }
            if (transactionRepository.existsByProductIdAndCreatedAtAfter(product.getId(), session.getCreatedAt())) {
                throw new StaleStocktakeSessionException("Inventory changed after session start");
            }
            int counted = item.getCountedQuantity() != null ? item.getCountedQuantity() : 0;
            int system = item.getSystemQuantity() != null ? item.getSystemQuantity() : 0;
            int variance = counted - system;
            if (variance != 0) {
                inventoryTransactionService.recordTransaction(
                    product,
                    variance,
                    InventoryTransactionType.STOCKTAKE_ADJUST,
                    null,
                    null,
                    actor,
                    buildStocktakeItemNotes(session, item, variance),
                    item.getUnitCostSnapshot(),
                    MoneySupport.multiply(item.getUnitCostSnapshot(), variance)
                );
                affectedProducts.add(product);
            }
        }

        inventoryLedgerService.recomputeProducts(affectedProducts);
        session.setStatus(StocktakeSessionStatus.APPLIED);
        session.setAppliedAt(java.time.LocalDateTime.now());
        StocktakeSession saved = sessionRepository.save(session);
        operationalAuditLogService.record(
            actor,
            OperationalAuditAction.STOCKTAKE_APPLIED,
            OperationalSubjectType.STOCKTAKE_SESSION,
            saved.getId(),
            "Stocktake #" + saved.getId(),
            buildSessionDetails(saved)
        );
        return saved;
    }

    @Transactional
    public StocktakeSession cancelSession(User actor, Long sessionId, String notes) {
        requireStocktakeAccess(actor);
        StocktakeSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new ValidationException("Stocktake session not found"));
        if (session.getStatus() != StocktakeSessionStatus.OPEN) {
            throw new ValidationException("Only open stocktake sessions can be canceled");
        }
        session.setStatus(StocktakeSessionStatus.CANCELED);
        if (notes != null && !notes.isBlank()) {
            session.setNotes(notes.trim());
        }
        StocktakeSession saved = sessionRepository.save(session);
        operationalAuditLogService.record(
            actor,
            OperationalAuditAction.STOCKTAKE_CANCELED,
            OperationalSubjectType.STOCKTAKE_SESSION,
            saved.getId(),
            "Stocktake #" + saved.getId(),
            buildSessionDetails(saved)
        );
        return saved;
    }

    @Transactional
    public StocktakeSession updateSessionItems(User actor, Long sessionId, String notes, List<StocktakeItemUpdate> updates) {
        requireStocktakeAccess(actor);
        StocktakeSession session = sessionRepository.findByIdWithItems(sessionId)
            .orElseThrow(() -> new ValidationException("Stocktake session not found"));
        if (session.getStatus() != StocktakeSessionStatus.OPEN) {
            throw new ValidationException("Only open stocktake sessions can be updated");
        }
        java.util.Map<Long, StocktakeItem> itemById = session.getItems().stream()
            .filter(item -> item.getId() != null)
            .collect(java.util.stream.Collectors.toMap(StocktakeItem::getId, item -> item));
        if (updates != null) {
            for (StocktakeItemUpdate update : updates) {
                if (update == null || update.itemId() == null) {
                    continue;
                }
                StocktakeItem item = itemById.get(update.itemId());
                if (item == null) {
                    throw new ValidationException("Stocktake item not found");
                }
                int countedQuantity = update.countedQuantity() != null ? update.countedQuantity() : 0;
                if (countedQuantity < 0) {
                    throw new ValidationException("Counted quantity cannot be negative");
                }
                item.setCountedQuantity(countedQuantity);
                item.setNotes(update.notes() == null || update.notes().isBlank() ? null : update.notes().trim());
            }
        }
        session.setNotes(notes == null || notes.isBlank() ? null : notes.trim());
        return sessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public StocktakeSession getSessionWithItems(User actor, Long sessionId) {
        requireStocktakeAccess(actor);
        return sessionRepository.findByIdWithItems(sessionId)
            .orElseThrow(() -> new ValidationException("Stocktake session not found"));
    }

    @Transactional(readOnly = true)
    public Page<StocktakeSession> searchSessions(
        User actor,
        String search,
        LocalDate startDate,
        LocalDate endDate,
        Set<StocktakeSessionStatus> statuses,
        Set<StocktakeScopeType> scopeTypes,
        Pageable pageable
    ) {
        requireStocktakeAccess(actor);
        Pageable sanitizedPageable = PageSortSupport.sanitize(
            pageable,
            Sort.by(Sort.Direction.DESC, "createdAt"),
            Set.of("createdAt", "status", "scopeType", "createdBy.username")
        );
        Specification<StocktakeSession> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            var userJoin = root.join("createdBy", JoinType.LEFT);
            var categoryJoin = root.join("category", JoinType.LEFT);
            String normalizedSearch = search == null ? null : search.trim().toLowerCase();
            if (normalizedSearch != null && !normalizedSearch.isEmpty()) {
                String likeValue = "%" + normalizedSearch + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(cb.coalesce(userJoin.get("username"), "")), likeValue),
                    cb.like(cb.lower(cb.coalesce(categoryJoin.get("name"), "")), likeValue),
                    cb.like(cb.lower(root.get("status").as(String.class)), likeValue),
                    cb.like(cb.lower(root.get("scopeType").as(String.class)), likeValue),
                    cb.like(cb.lower(cb.coalesce(root.get("notes"), "")), likeValue)
                ));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay()));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate.atTime(23, 59, 59)));
            }
            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("status").in(statuses));
            }
            if (scopeTypes != null && !scopeTypes.isEmpty()) {
                predicates.add(root.get("scopeType").in(scopeTypes));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return sessionRepository.findAll(spec, sanitizedPageable);
    }

    private void requireStocktakeAccess(User actor) {
        authorizationService.requireStocktakeAccess(actor);
    }

    private String buildSessionDetails(StocktakeSession session) {
        String scope = session.getScopeType() == StocktakeScopeType.CATEGORY && session.getCategory() != null
            ? "Category: " + session.getCategory().getName()
            : "All products";
        return scope + (session.getNotes() != null && !session.getNotes().isBlank() ? " | " + session.getNotes() : "");
    }

    private String buildStocktakeItemNotes(StocktakeSession session, StocktakeItem item, int variance) {
        String base = "Stocktake #" + session.getId() + " variance " + variance;
        if (item.getNotes() == null || item.getNotes().isBlank()) {
            return base;
        }
        return base + ": " + item.getNotes().trim();
    }

    public record StocktakeItemUpdate(Long itemId, Integer countedQuantity, String notes) {
    }
}
