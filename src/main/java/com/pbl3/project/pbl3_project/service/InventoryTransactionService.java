package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.entity.InventoryTransaction;
import com.pbl3.project.pbl3_project.entity.InventoryTransactionType;
import com.pbl3.project.pbl3_project.entity.ImportOrder;
import com.pbl3.project.pbl3_project.entity.Order;
import com.pbl3.project.pbl3_project.entity.Product;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.repository.InventoryTransactionRepository;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class InventoryTransactionService {

    private final InventoryTransactionRepository transactionRepository;
    private final AuthorizationService authorizationService;

    public InventoryTransactionService(InventoryTransactionRepository transactionRepository, AuthorizationService authorizationService) {
        this.transactionRepository = transactionRepository;
        this.authorizationService = authorizationService;
    }

    public InventoryTransaction recordTransaction(
        Product product,
        Integer quantityChange,
        InventoryTransactionType type,
        Order order,
        ImportOrder importOrder,
        User user,
        String notes,
        BigDecimal unitCostSnapshot,
        BigDecimal inventoryValueChange
    ) {
        int normalizedQuantityChange = quantityChange != null ? quantityChange : 0;
        BigDecimal normalizedInventoryValueChange = MoneySupport.normalize(inventoryValueChange);
        if (normalizedQuantityChange == 0 && MoneySupport.isZero(normalizedInventoryValueChange)) {
            return null;
        }

        Long referenceId = order != null
            ? order.getId()
            : importOrder != null
                ? importOrder.getId()
                : null;

        InventoryTransaction tx = new InventoryTransaction(
            product,
            normalizedQuantityChange,
            type,
            referenceId,
            order,
            importOrder,
            user,
            notes,
            MoneySupport.normalize(unitCostSnapshot),
            normalizedInventoryValueChange
        );
        return transactionRepository.save(tx);
    }

    public List<InventoryTransaction> getTransactionsByProduct(Long productId) {
        return transactionRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    public List<InventoryTransaction> getAllTransactions() {
        return transactionRepository.findAllByOrderByCreatedAtDesc();
    }

    public Page<InventoryTransaction> searchTransactions(
        User viewer,
        String search,
        LocalDate startDate,
        LocalDate endDate,
        Set<String> selectedUsers,
        Set<String> selectedTypes,
        Double minAbsoluteQuantity,
        Double maxAbsoluteQuantity,
        Pageable pageable
    ) {
        if (viewer != null) {
            authorizationService.requireAuditLogAccess(viewer);
        }
        Pageable sanitizedPageable = PageSortSupport.sanitize(
            pageable,
            Sort.by(Sort.Direction.DESC, "createdAt"),
            java.util.Set.of("createdAt", "transactionType", "product.name", "quantityChange", "user.username")
        );
        Specification<InventoryTransaction> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            var productJoin = root.join("product", JoinType.LEFT);
            var userJoin = root.join("user", JoinType.LEFT);

            String normalizedSearch = search == null ? null : search.trim().toLowerCase();
            if (normalizedSearch != null && !normalizedSearch.isEmpty()) {
                String likeValue = "%" + normalizedSearch + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(cb.coalesce(productJoin.get("name"), "")), likeValue),
                    cb.like(cb.lower(root.get("transactionType").as(String.class)), likeValue),
                    cb.like(cb.lower(cb.coalesce(userJoin.get("username"), "")), likeValue),
                    cb.like(cb.lower(cb.coalesce(root.get("notes"), "")), likeValue)
                ));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay()));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate.atTime(23, 59, 59)));
            }

            if (selectedUsers != null && !selectedUsers.isEmpty()) {
                predicates.add(userJoin.get("username").in(selectedUsers));
            }
            if (selectedTypes != null && !selectedTypes.isEmpty()) {
                List<InventoryTransactionType> transactionTypes = selectedTypes.stream()
                    .map(this::parseTransactionType)
                    .filter(java.util.Objects::nonNull)
                    .toList();
                if (!transactionTypes.isEmpty()) {
                    predicates.add(root.get("transactionType").in(transactionTypes));
                }
            }

            jakarta.persistence.criteria.Expression<Integer> absQty = cb.abs(root.get("quantityChange"));
            if (minAbsoluteQuantity != null) {
                predicates.add(cb.greaterThanOrEqualTo(absQty, minAbsoluteQuantity.intValue()));
            }
            if (maxAbsoluteQuantity != null) {
                predicates.add(cb.lessThanOrEqualTo(absQty, maxAbsoluteQuantity.intValue()));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return transactionRepository.findAll(
            spec,
            sanitizedPageable
        );
    }

    public Page<InventoryTransaction> searchTransactions(
        String search,
        LocalDate startDate,
        LocalDate endDate,
        Set<String> selectedUsers,
        Set<String> selectedTypes,
        Double minAbsoluteQuantity,
        Double maxAbsoluteQuantity,
        Pageable pageable
    ) {
        return searchTransactions(null, search, startDate, endDate, selectedUsers, selectedTypes, minAbsoluteQuantity, maxAbsoluteQuantity, pageable);
    }

    public List<String> getTransactionUsernames(User viewer) {
        authorizationService.requireAuditLogAccess(viewer);
        return transactionRepository.findDistinctUsernames();
    }

    public List<String> getTransactionUsernames() {
        return transactionRepository.findDistinctUsernames();
    }

    public double getTransactionMaxAbsoluteQuantity(User viewer) {
        authorizationService.requireAuditLogAccess(viewer);
        return transactionRepository.findMaxAbsoluteQuantityChange();
    }

    public double getTransactionMaxAbsoluteQuantity() {
        return transactionRepository.findMaxAbsoluteQuantityChange();
    }

    private InventoryTransactionType parseTransactionType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return InventoryTransactionType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
