package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.dto.CreateImportOrderRequest;
import com.pbl3.project.pbl3_project.dto.IdLabelOption;
import com.pbl3.project.pbl3_project.entity.ImportOrder;
import com.pbl3.project.pbl3_project.entity.ImportOrderItem;
import com.pbl3.project.pbl3_project.entity.ImportOrderStatus;
import com.pbl3.project.pbl3_project.entity.InventoryTransactionType;
import com.pbl3.project.pbl3_project.entity.OperationalAuditAction;
import com.pbl3.project.pbl3_project.entity.OperationalSubjectType;
import com.pbl3.project.pbl3_project.entity.Product;
import com.pbl3.project.pbl3_project.entity.Supplier;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.repository.ImportOrderRepository;
import com.pbl3.project.pbl3_project.repository.ProductRepository;
import com.pbl3.project.pbl3_project.repository.SupplierRepository;
import com.pbl3.project.pbl3_project.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ImportOrderService {
    private final ImportOrderRepository importOrderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryTransactionService transactionService;
    private final InventoryLedgerService inventoryLedgerService;
    private final AuthorizationService authorizationService;
    private final OperationalAuditLogService operationalAuditLogService;

    public ImportOrderService(
        ImportOrderRepository importOrderRepository,
        ProductRepository productRepository,
        UserRepository userRepository,
        SupplierRepository supplierRepository,
        InventoryTransactionService transactionService,
        InventoryLedgerService inventoryLedgerService,
        AuthorizationService authorizationService,
        OperationalAuditLogService operationalAuditLogService
    ) {
        this.importOrderRepository = importOrderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.supplierRepository = supplierRepository;
        this.transactionService = transactionService;
        this.inventoryLedgerService = inventoryLedgerService;
        this.authorizationService = authorizationService;
        this.operationalAuditLogService = operationalAuditLogService;
    }

    @Transactional
    public ImportOrder createImportOrder(CreateImportOrderRequest request) {
        try {
            User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ValidationException("User not found: " + request.getUserId()));
            authorizationService.requireImportGoodsAccess(user);

            Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ValidationException("Supplier not found: " + request.getSupplierId()));

            ImportOrder order = new ImportOrder();
            order.setUser(user);
            order.setSupplier(supplier);
            order.setNotes(request.getNotes());
            order.setCreatedAt(LocalDateTime.now());
            order.setItems(new ArrayList<>());
            order.setStatus(ImportOrderStatus.COMPLETED);
            order.setStatusNote(null);
            order.setCreatedByNameSnapshot(resolveUserDisplayName(user));
            order.setSupplierNameSnapshot(resolveSupplierDisplayName(supplier));

            BigDecimal total = MoneySupport.ZERO;
            List<Product> affectedProducts = new ArrayList<>();

            for (CreateImportOrderRequest.ImportOrderItemRequest itemReq : request.getItems()) {
                Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ValidationException("Product not found: " + itemReq.getProductId()));
                inventoryLedgerService.ensureBaseline(product);

                ImportOrderItem orderItem = new ImportOrderItem();
                orderItem.setImportOrder(order);
                orderItem.setProduct(product);
                orderItem.setQuantity(itemReq.getQuantity());
                orderItem.setImportPrice(MoneySupport.normalize(itemReq.getImportPrice()));
                populateImportOrderItemSnapshot(orderItem, product);

                order.getItems().add(orderItem);
                total = MoneySupport.add(total, MoneySupport.multiply(orderItem.getImportPrice(), itemReq.getQuantity()));
                affectedProducts.add(product);
            }

            order.setTotalCost(total);
            ImportOrder savedOrder = importOrderRepository.save(order);

            for (ImportOrderItem item : savedOrder.getItems()) {
                BigDecimal unitCost = MoneySupport.normalize(item.getImportPrice());
                int quantity = safeInt(item.getQuantity());
                transactionService.recordTransaction(
                    item.getProduct(),
                    quantity,
                    InventoryTransactionType.IMPORT,
                    null,
                    savedOrder,
                    user,
                    "Import Order #" + savedOrder.getId(),
                    unitCost,
                    MoneySupport.multiply(unitCost, quantity)
                );
            }

            inventoryLedgerService.recomputeProducts(affectedProducts);
            operationalAuditLogService.record(
                user,
                OperationalAuditAction.IMPORT_CREATED,
                OperationalSubjectType.IMPORT_ORDER,
                savedOrder.getId(),
                "Import #" + savedOrder.getId(),
                "Import order created"
            );
            return savedOrder;
        } catch (OptimisticLockingFailureException ex) {
            throw new ConcurrencyConflictException("Data changed, reload and try again");
        }
    }

    public List<ImportOrder> getAllImportOrders() {
        return importOrderRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }

    @Transactional(readOnly = true)
    public Page<ImportOrder> searchImportOrders(
        User viewer,
        String search,
        LocalDate startDate,
        LocalDate endDate,
        Set<Long> selectedSuppliers,
        Set<ImportOrderStatus> selectedStatuses,
        BigDecimal minTotalCost,
        BigDecimal maxTotalCost,
        Pageable pageable
    ) {
        if (viewer != null) {
            authorizationService.requireImportGoodsAccess(viewer);
        }
        Pageable sanitizedPageable = PageSortSupport.sanitize(
            pageable,
            Sort.by(Sort.Direction.DESC, "createdAt"),
            Set.of("id", "supplier.id", "createdAt", "totalCost", "status")
        );
        Specification<ImportOrder> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            var supplierJoin = root.join("supplier", jakarta.persistence.criteria.JoinType.LEFT);

            String normalizedSearch = search == null ? null : search.trim().toLowerCase();
            if (normalizedSearch != null && !normalizedSearch.isEmpty()) {
                String likeValue = "%" + normalizedSearch + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(cb.function("str", String.class, root.get("id"))), likeValue),
                    cb.like(cb.lower(cb.coalesce(root.get("supplierNameSnapshot"), "")), likeValue),
                    cb.like(cb.lower(cb.coalesce(supplierJoin.get("name"), "")), likeValue),
                    cb.like(cb.lower(cb.function("str", String.class, root.get("createdAt"))), likeValue),
                    cb.like(cb.lower(root.get("status").as(String.class)), likeValue)
                ));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay()));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate.atTime(23, 59, 59)));
            }

            if (selectedSuppliers != null && !selectedSuppliers.isEmpty()) {
                predicates.add(supplierJoin.get("id").in(selectedSuppliers));
            }
            if (selectedStatuses != null && !selectedStatuses.isEmpty()) {
                predicates.add(root.get("status").in(selectedStatuses));
            }
            if (minTotalCost != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("totalCost"), minTotalCost));
            }
            if (maxTotalCost != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("totalCost"), maxTotalCost));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return importOrderRepository.findAll(spec, sanitizedPageable);
    }

    @Transactional(readOnly = true)
    public Page<ImportOrder> searchImportOrders(
        String search,
        LocalDate startDate,
        LocalDate endDate,
        Set<Long> selectedSuppliers,
        Set<ImportOrderStatus> selectedStatuses,
        BigDecimal minTotalCost,
        BigDecimal maxTotalCost,
        Pageable pageable
    ) {
        return searchImportOrders(null, search, startDate, endDate, selectedSuppliers, selectedStatuses, minTotalCost, maxTotalCost, pageable);
    }

    @Transactional(readOnly = true)
    public List<IdLabelOption> getImportSupplierOptions(User viewer) {
        authorizationService.requireImportGoodsAccess(viewer);
        return importOrderRepository.findDistinctSupplierOptions().stream()
            .sorted(java.util.Comparator
                .comparing(IdLabelOption::label, java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(IdLabelOption::id, java.util.Comparator.nullsLast(Long::compareTo)))
            .map(option -> new IdLabelOption(option.id(), option.label() + " #" + option.id()))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<IdLabelOption> getImportSupplierOptions() {
        return importOrderRepository.findDistinctSupplierOptions().stream()
            .sorted(java.util.Comparator
                .comparing(IdLabelOption::label, java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(IdLabelOption::id, java.util.Comparator.nullsLast(Long::compareTo)))
            .map(option -> new IdLabelOption(option.id(), option.label() + " #" + option.id()))
            .toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal getImportMaxTotalCost(User viewer) {
        authorizationService.requireImportGoodsAccess(viewer);
        return MoneySupport.normalize(importOrderRepository.findMaxTotalCost());
    }

    @Transactional(readOnly = true)
    public BigDecimal getImportMaxTotalCost() {
        return MoneySupport.normalize(importOrderRepository.findMaxTotalCost());
    }

    @Transactional(readOnly = true)
    public ImportOrder getImportOrderWithItems(Long id) {
        return importOrderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ValidationException("Import Order not found: " + id));
    }

    @Transactional(readOnly = true)
    public ImportOrder getImportOrderWithItems(Long id, User viewer) {
        authorizationService.requireImportGoodsAccess(viewer);
        return getImportOrderWithItems(id);
    }

    @Transactional
    public ImportOrder cancelImportOrder(Long importOrderId, Long userId, String reason) {
        try {
            if (reason == null || reason.trim().isEmpty()) {
                throw new ValidationException("Cancellation reason is required");
            }

            User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("User not found"));
            authorizationService.requireImportGoodsAccess(user);
            ImportOrder order = getImportOrderWithItems(importOrderId);

            ImportOrderStatus status = order.getStatus() != null ? order.getStatus() : ImportOrderStatus.COMPLETED;
            if (status == ImportOrderStatus.CANCELED) {
                throw new ValidationException("Import order is already canceled");
            }

            inventoryLedgerService.validateCancelableImportReplay(order);

            List<Product> affectedProducts = new ArrayList<>();
            for (ImportOrderItem item : order.getItems()) {
                Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new ValidationException("Product not found: " + item.getProduct().getId()));
                int removeQty = safeInt(item.getQuantity());
                BigDecimal removeCost = MoneySupport.normalize(item.getImportPrice());

                transactionService.recordTransaction(
                    product,
                    -removeQty,
                    InventoryTransactionType.CANCEL_IMPORT,
                    null,
                    order,
                    user,
                    "Canceled Import Order #" + order.getId() + ": " + reason.trim(),
                    removeCost,
                    MoneySupport.multiply(removeCost, -removeQty)
                );
                affectedProducts.add(product);
            }

            inventoryLedgerService.recomputeProducts(affectedProducts);
            order.setStatus(ImportOrderStatus.CANCELED);
            order.setStatusNote(reason.trim());
            ImportOrder saved = importOrderRepository.save(order);
            operationalAuditLogService.record(
                user,
                OperationalAuditAction.IMPORT_CANCELED,
                OperationalSubjectType.IMPORT_ORDER,
                saved.getId(),
                "Import #" + saved.getId(),
                reason.trim()
            );
            return saved;
        } catch (OptimisticLockingFailureException ex) {
            throw new ConcurrencyConflictException("Data changed, reload and try again");
        }
    }

    private void populateImportOrderItemSnapshot(ImportOrderItem item, Product product) {
        item.setProductNameSnapshot(product.getName());
        item.setSkuSnapshot(product.getSku());
        item.setBarcodeSnapshot(product.getBarcode());
        item.setCategoryNameSnapshot(
            product.getCategory() != null ? product.getCategory().getName() : "Uncategorized"
        );
        item.setBrandNameSnapshot(product.getBrand() != null ? product.getBrand().getName() : null);
        item.setOriginNameSnapshot(product.getOrigin() != null ? product.getOrigin().getName() : null);
        item.setUnitNameSnapshot(product.getUnit() != null ? product.getUnit().getName() : null);
    }

    private String resolveUserDisplayName(User user) {
        if (user == null) {
            return "System";
        }
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return "System";
    }

    private String resolveSupplierDisplayName(Supplier supplier) {
        if (supplier == null) {
            return "-";
        }
        if (supplier.getName() != null && !supplier.getName().isBlank()) {
            return supplier.getName();
        }
        return "-";
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }
}
