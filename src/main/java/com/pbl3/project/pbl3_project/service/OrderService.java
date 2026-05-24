package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.dto.CreateOrderRequest;
import com.pbl3.project.pbl3_project.dto.IdLabelOption;
import com.pbl3.project.pbl3_project.entity.Customer;
import com.pbl3.project.pbl3_project.entity.InventoryTransactionType;
import com.pbl3.project.pbl3_project.entity.OperationalAuditAction;
import com.pbl3.project.pbl3_project.entity.OperationalSubjectType;
import com.pbl3.project.pbl3_project.entity.Order;
import com.pbl3.project.pbl3_project.entity.OrderItem;
import com.pbl3.project.pbl3_project.entity.OrderStatus;
import com.pbl3.project.pbl3_project.entity.PaymentMethod;
import com.pbl3.project.pbl3_project.entity.Product;
import com.pbl3.project.pbl3_project.entity.Promotion;
import com.pbl3.project.pbl3_project.entity.ReturnRefundScope;
import com.pbl3.project.pbl3_project.entity.SalesShift;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.repository.CustomerRepository;
import com.pbl3.project.pbl3_project.repository.OrderRepository;
import com.pbl3.project.pbl3_project.repository.ProductRepository;
import com.pbl3.project.pbl3_project.repository.UserRepository;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PromotionService promotionService;
    private final InventoryTransactionService transactionService;
    private final InventoryLedgerService inventoryLedgerService;
    private final AuthorizationService authorizationService;
    private final OperationalAuditLogService operationalAuditLogService;
    private final SalesShiftService salesShiftService;

    @org.springframework.beans.factory.annotation.Autowired
    public OrderService(
        OrderRepository orderRepository,
        CustomerRepository customerRepository,
        ProductRepository productRepository,
        UserRepository userRepository,
        PromotionService promotionService,
        InventoryTransactionService transactionService,
        InventoryLedgerService inventoryLedgerService,
        AuthorizationService authorizationService,
        OperationalAuditLogService operationalAuditLogService,
        SalesShiftService salesShiftService
    ) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.promotionService = promotionService;
        this.transactionService = transactionService;
        this.inventoryLedgerService = inventoryLedgerService;
        this.authorizationService = authorizationService;
        this.operationalAuditLogService = operationalAuditLogService;
        this.salesShiftService = salesShiftService;
    }

    public OrderService(
        OrderRepository orderRepository,
        CustomerRepository customerRepository,
        ProductRepository productRepository,
        UserRepository userRepository,
        PromotionService promotionService,
        InventoryTransactionService transactionService,
        InventoryLedgerService inventoryLedgerService,
        AuthorizationService authorizationService,
        OperationalAuditLogService operationalAuditLogService
    ) {
        this(
            orderRepository,
            customerRepository,
            productRepository,
            userRepository,
            promotionService,
            transactionService,
            inventoryLedgerService,
            authorizationService,
            operationalAuditLogService,
            null
        );
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        try {
            if (request.getItems() == null || request.getItems().isEmpty()) {
                throw new ValidationException("Add at least one item");
            }
            User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ValidationException("User not found"));
            authorizationService.requireSalesAccess(user);
            SalesShift activeShift = salesShiftService != null
                ? salesShiftService.requireOpenShiftForSale(user)
                : null;

            Order order = new Order();
            order.setUser(user);
            order.setSalesShift(activeShift);
            Customer customer = resolveActiveCustomer(request.getCustomerId());
            if (customer != null) {
                order.setCustomer(customer);
                populateOrderCustomerSnapshot(order, customer);
            }
            LocalDateTime createdAt = LocalDateTime.now();
            order.setCreatedAt(createdAt);
            order.setOrderItems(new ArrayList<>());
            order.setPaymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.CASH);
            order.setTotalPrice(MoneySupport.ZERO);
            order.setGrossSubtotal(MoneySupport.ZERO);
            order.setDiscountTotal(MoneySupport.ZERO);
            order.setOrderLevelDiscountTotal(MoneySupport.ZERO);
            order.setAppliedOrderPromotionIdSnapshot(null);
            order.setAppliedOrderPromotionNameSnapshot(null);
            order.setRefundedAmount(MoneySupport.ZERO);
            order.setStatus(OrderStatus.COMPLETED);
            order.setStatusNote(null);
            order.setCreatedByNameSnapshot(resolveUserDisplayName(user));

            java.util.Map<Long, Integer> requestedQuantitiesByProductId = aggregateRequestedQuantities(request.getItems());
            java.util.Map<Long, Product> productById = new java.util.LinkedHashMap<>();
            for (Map.Entry<Long, Integer> entry : requestedQuantitiesByProductId.entrySet()) {
                Product product = productRepository.findByIdForUpdate(entry.getKey())
                    .orElseThrow(() -> new ValidationException("Product not found: " + entry.getKey()));
                if (product.isDeleted()) {
                    throw new ValidationException("Product is no longer available: " + product.getName());
                }
                if (safeInt(product.getQuantity()) < entry.getValue()) {
                    throw new ValidationException("Not enough stock for product: " + product.getName());
                }
                inventoryLedgerService.ensureBaseline(product);
                productById.putIfAbsent(product.getId(), product);
            }

            java.util.Map<Long, PromotionService.ProductPricingPreview> pricingByProductId =
                promotionService.previewBestProductPricing(productById.values(), createdAt);

            BigDecimal grossSubtotal = MoneySupport.ZERO;
            BigDecimal productDiscountTotal = MoneySupport.ZERO;
            BigDecimal subtotalAfterProductDiscount = MoneySupport.ZERO;
            Set<Product> affectedProducts = new HashSet<>();

            for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
                Product product = productById.get(itemRequest.getProductId());
                if (product == null) {
                    throw new ValidationException("Product not found: " + itemRequest.getProductId());
                }
                PromotionService.ProductPricingPreview pricingPreview = pricingByProductId.getOrDefault(
                    product.getId(),
                    new PromotionService.ProductPricingPreview(
                        product,
                        null,
                        MoneySupport.normalize(product.getPrice()),
                        MoneySupport.normalize(product.getPrice()),
                        MoneySupport.ZERO
                    )
                );

                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setProduct(product);
                orderItem.setQuantity(itemRequest.getQuantity());
                orderItem.setOriginalUnitPrice(pricingPreview.originalUnitPrice());
                orderItem.setPrice(pricingPreview.discountedUnitPrice());
                orderItem.setLinePromotionDiscountAmount(MoneySupport.multiply(pricingPreview.unitDiscountAmount(), itemRequest.getQuantity()));
                orderItem.setOrderLevelDiscountAllocatedAmount(MoneySupport.ZERO);
                if (pricingPreview.promotion() != null) {
                    orderItem.setAppliedProductPromotionIdSnapshot(pricingPreview.promotion().getId());
                    orderItem.setAppliedProductPromotionNameSnapshot(pricingPreview.promotion().getName());
                } else {
                    orderItem.setAppliedProductPromotionIdSnapshot(null);
                    orderItem.setAppliedProductPromotionNameSnapshot(null);
                }
                orderItem.setCostAtSale(MoneySupport.normalize(product.getImportPrice()));
                orderItem.setReturnedQuantity(0);
                populateOrderItemSnapshot(orderItem, product);

                order.getOrderItems().add(orderItem);
                grossSubtotal = MoneySupport.add(grossSubtotal, MoneySupport.multiply(pricingPreview.originalUnitPrice(), itemRequest.getQuantity()));
                productDiscountTotal = MoneySupport.add(productDiscountTotal, orderItem.getLinePromotionDiscountAmountSnapshot());
                subtotalAfterProductDiscount = MoneySupport.add(subtotalAfterProductDiscount, orderItem.getLineSubtotalBeforeOrderDiscount());
                affectedProducts.add(product);
            }

            Promotion selectedOrderPromotion = promotionService.resolveEligibleOrderPromotion(
                request.getSelectedOrderPromotionId(),
                subtotalAfterProductDiscount,
                createdAt
            );
            BigDecimal orderLevelDiscountTotal = selectedOrderPromotion != null
                ? promotionService.computeDiscountAmount(selectedOrderPromotion, subtotalAfterProductDiscount)
                : MoneySupport.ZERO;

            if (MoneySupport.isPositive(orderLevelDiscountTotal) && !order.getOrderItems().isEmpty() && MoneySupport.isPositive(subtotalAfterProductDiscount)) {
                BigDecimal allocatedSoFar = MoneySupport.ZERO;
                for (int i = 0; i < order.getOrderItems().size(); i++) {
                    OrderItem item = order.getOrderItems().get(i);
                    BigDecimal allocated;
                    if (i == order.getOrderItems().size() - 1) {
                        allocated = MoneySupport.subtract(orderLevelDiscountTotal, allocatedSoFar);
                    } else {
                        allocated = item.getLineSubtotalBeforeOrderDiscount()
                            .multiply(orderLevelDiscountTotal)
                            .divide(subtotalAfterProductDiscount, MoneySupport.MONEY_SCALE, MoneySupport.MONEY_ROUNDING);
                        allocatedSoFar = MoneySupport.add(allocatedSoFar, allocated);
                    }
                    item.setOrderLevelDiscountAllocatedAmount(allocated.compareTo(MoneySupport.ZERO) < 0 ? MoneySupport.ZERO : allocated);
                }
            }

            BigDecimal totalDiscount = MoneySupport.add(productDiscountTotal, orderLevelDiscountTotal);
            order.setGrossSubtotal(grossSubtotal);
            order.setOrderLevelDiscountTotal(orderLevelDiscountTotal);
            order.setDiscountTotal(totalDiscount);
            if (selectedOrderPromotion != null) {
                order.setAppliedOrderPromotionIdSnapshot(selectedOrderPromotion.getId());
                order.setAppliedOrderPromotionNameSnapshot(selectedOrderPromotion.getName());
            }
            order.setTotalPrice(MoneySupport.subtract(grossSubtotal, totalDiscount));
            Order savedOrder = orderRepository.save(order);

            for (OrderItem item : savedOrder.getOrderItems()) {
                BigDecimal unitCost = MoneySupport.normalize(item.getCostAtSale());
                int quantity = safeInt(item.getQuantity());
                transactionService.recordTransaction(
                    item.getProduct(),
                    -quantity,
                    InventoryTransactionType.SALE,
                    savedOrder,
                    null,
                    user,
                    "Sale Order #" + savedOrder.getId(),
                    unitCost,
                    MoneySupport.multiply(unitCost, -quantity)
                );
            }

            inventoryLedgerService.recomputeProducts(affectedProducts);
            operationalAuditLogService.record(
                user,
                OperationalAuditAction.ORDER_CREATED,
                OperationalSubjectType.ORDER,
                savedOrder.getId(),
                "Order #" + savedOrder.getId(),
                "Order created"
            );
            return savedOrder;
        } catch (OptimisticLockingFailureException | PessimisticLockingFailureException ex) {
            throw new ConcurrencyConflictException("Data changed, reload and try again");
        }
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }

    @Transactional(readOnly = true)
    public Page<Order> searchOrders(
        User viewer,
        String search,
        LocalDate startDate,
        LocalDate endDate,
        Set<Long> selectedUsers,
        Set<PaymentMethod> selectedMethods,
        Set<OrderStatus> selectedStatuses,
        BigDecimal minTotalPrice,
        BigDecimal maxTotalPrice,
        Pageable pageable
    ) {
        if (viewer != null) {
            authorizationService.requireOrderHistoryAccess(viewer);
        }
        Pageable sanitizedPageable = PageSortSupport.sanitize(
            pageable,
            Sort.by(Sort.Direction.DESC, "createdAt"),
            Set.of("id", "createdAt", "totalPrice", "grossSubtotal", "user.id", "status", "customerNameSnapshot", "createdByNameSnapshot", "refundedAmount")
        );
        Specification<Order> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            var userJoin = root.join("user", JoinType.LEFT);

            if (viewer != null && !authorizationService.canViewAllOrders(viewer)) {
                predicates.add(cb.equal(userJoin.get("id"), viewer.getId()));
            }

            String normalizedSearch = search == null ? null : search.trim().toLowerCase();
            if (normalizedSearch != null && !normalizedSearch.isEmpty()) {
                String likeValue = "%" + normalizedSearch + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(cb.function("str", String.class, root.get("id"))), likeValue),
                    cb.like(cb.lower(cb.coalesce(root.get("createdByNameSnapshot"), "")), likeValue),
                    cb.like(cb.lower(cb.coalesce(root.get("customerNameSnapshot"), "")), likeValue),
                    cb.like(cb.lower(cb.coalesce(root.get("customerPhoneSnapshot"), "")), likeValue),
                    cb.like(cb.lower(cb.coalesce(userJoin.get("username"), "")), likeValue),
                    cb.like(cb.lower(cb.function("str", String.class, root.get("createdAt"))), likeValue),
                    cb.like(cb.lower(root.get("paymentMethod").as(String.class)), likeValue),
                    cb.like(cb.lower(root.get("status").as(String.class)), likeValue)
                ));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay()));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate.atTime(23, 59, 59)));
            }

            if (selectedUsers != null && !selectedUsers.isEmpty()) {
                predicates.add(userJoin.get("id").in(selectedUsers));
            }
            if (selectedMethods != null && !selectedMethods.isEmpty()) {
                predicates.add(root.get("paymentMethod").in(selectedMethods));
            }
            if (selectedStatuses != null && !selectedStatuses.isEmpty()) {
                predicates.add(root.get("status").in(selectedStatuses));
            }
            if (minTotalPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("totalPrice"), minTotalPrice));
            }
            if (maxTotalPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("totalPrice"), maxTotalPrice));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return orderRepository.findAll(spec, sanitizedPageable);
    }

    @Transactional(readOnly = true)
    public Page<Order> searchOrders(
        String search,
        LocalDate startDate,
        LocalDate endDate,
        Set<Long> selectedUsers,
        Set<PaymentMethod> selectedMethods,
        Set<OrderStatus> selectedStatuses,
        BigDecimal minTotalPrice,
        BigDecimal maxTotalPrice,
        Pageable pageable
    ) {
        return searchOrders(null, search, startDate, endDate, selectedUsers, selectedMethods, selectedStatuses, minTotalPrice, maxTotalPrice, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Order> searchReturnRefundOrders(
        User viewer,
        ReturnRefundScope scope,
        String search,
        LocalDate startDate,
        LocalDate endDate,
        Set<Long> selectedUsers,
        Set<PaymentMethod> selectedMethods,
        Set<OrderStatus> selectedStatuses,
        BigDecimal minTotalPrice,
        BigDecimal maxTotalPrice,
        Pageable pageable
    ) {
        if (viewer != null) {
            authorizationService.requireReturnsRefundsAccess(viewer);
        }
        Set<OrderStatus> scopedStatuses = resolveReturnRefundStatuses(scope, selectedStatuses);
        if (scopedStatuses.isEmpty()) {
            Pageable emptyPageable = pageable != null ? pageable : Pageable.unpaged();
            return Page.empty(emptyPageable);
        }
        return searchOrders(
            viewer,
            search,
            startDate,
            endDate,
            selectedUsers,
            selectedMethods,
            scopedStatuses,
            minTotalPrice,
            maxTotalPrice,
            pageable
        );
    }

    Set<OrderStatus> resolveReturnRefundStatuses(ReturnRefundScope scope, Set<OrderStatus> selectedStatuses) {
        Set<OrderStatus> allowedStatuses = (scope != null ? scope : ReturnRefundScope.PROCESSED).getStatuses();
        if (selectedStatuses == null || selectedStatuses.isEmpty()) {
            return allowedStatuses;
        }

        java.util.EnumSet<OrderStatus> filteredStatuses = java.util.EnumSet.noneOf(OrderStatus.class);
        for (OrderStatus status : selectedStatuses) {
            if (status != null && allowedStatuses.contains(status)) {
                filteredStatuses.add(status);
            }
        }
        return filteredStatuses;
    }

    @Transactional(readOnly = true)
    public List<IdLabelOption> getOrderCreatorOptions(User viewer) {
        authorizationService.requireOrderHistoryAccess(viewer);
        if (!authorizationService.canViewAllOrders(viewer)) {
            return orderRepository.findDistinctCreatorOptionsByUserId(viewer.getId()).stream()
                .sorted(java.util.Comparator
                    .comparing(IdLabelOption::label, java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(IdLabelOption::id, java.util.Comparator.nullsLast(Long::compareTo)))
                .map(option -> new IdLabelOption(option.id(), option.label() + " #" + option.id()))
                .toList();
        }
        return orderRepository.findDistinctCreatorOptions().stream()
            .sorted(java.util.Comparator
                .comparing(IdLabelOption::label, java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(IdLabelOption::id, java.util.Comparator.nullsLast(Long::compareTo)))
            .map(option -> new IdLabelOption(option.id(), option.label() + " #" + option.id()))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<IdLabelOption> getOrderCreatorOptions() {
        return orderRepository.findDistinctCreatorOptions().stream()
            .sorted(java.util.Comparator
                .comparing(IdLabelOption::label, java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(IdLabelOption::id, java.util.Comparator.nullsLast(Long::compareTo)))
            .map(option -> new IdLabelOption(option.id(), option.label() + " #" + option.id()))
            .toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal getOrderMaxTotalPrice(User viewer) {
        authorizationService.requireOrderHistoryAccess(viewer);
        if (!authorizationService.canViewAllOrders(viewer)) {
            return MoneySupport.normalize(orderRepository.findMaxTotalPriceByUserId(viewer.getId()));
        }
        return MoneySupport.normalize(orderRepository.findMaxTotalPrice());
    }

    @Transactional(readOnly = true)
    public BigDecimal getOrderMaxTotalPrice() {
        return MoneySupport.normalize(orderRepository.findMaxTotalPrice());
    }

    @Transactional(readOnly = true)
    public Order getOrderWithItems(Long id) {
        return orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ValidationException("Order not found: " + id));
    }

    @Transactional(readOnly = true)
    public Order getOrderWithItems(Long id, User viewer) {
        authorizationService.requireOrderHistoryAccess(viewer);
        Order order = getOrderWithItems(id);
        authorizationService.requireManageOrder(viewer, order);
        return order;
    }

    @Transactional
    public Order cancelOrder(Long orderId, Long userId, String reason) {
        try {
            if (reason == null || reason.trim().isEmpty()) {
                throw new ValidationException("Cancellation reason is required");
            }

            User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("User not found"));
            Order order = getOrderWithItems(orderId);
            authorizationService.requireManageOrder(user, order);

            OrderStatus status = order.getStatus() != null ? order.getStatus() : OrderStatus.COMPLETED;
            if (status == OrderStatus.CANCELED) {
                throw new ValidationException("Order is already canceled");
            }
            if (status == OrderStatus.PARTIALLY_RETURNED || status == OrderStatus.RETURNED) {
                throw new ValidationException("Returned orders cannot be canceled");
            }

            Set<Product> affectedProducts = new HashSet<>();
            for (OrderItem item : order.getOrderItems()) {
                Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new ValidationException("Product not found: " + item.getProduct().getId()));
                int restoreQty = safeInt(item.getQuantity());
                BigDecimal unitCost = MoneySupport.normalize(item.getCostAtSale());

                transactionService.recordTransaction(
                    product,
                    restoreQty,
                    InventoryTransactionType.CANCEL_SALE,
                    order,
                    null,
                    user,
                    "Canceled Order #" + order.getId() + ": " + reason.trim(),
                    unitCost,
                    MoneySupport.multiply(unitCost, restoreQty)
                );
                affectedProducts.add(product);
            }

            inventoryLedgerService.recomputeProducts(affectedProducts);
            order.setStatus(OrderStatus.CANCELED);
            order.setRefundedAmount(order.getTotalPrice() != null ? order.getTotalPrice() : MoneySupport.ZERO);
            order.setStatusNote(reason.trim());
            Order saved = orderRepository.save(order);
            if (salesShiftService != null) {
                salesShiftService.recordRefundEvent(user, saved, saved.getTotalPrice(), reason.trim());
            }
            operationalAuditLogService.record(
                user,
                OperationalAuditAction.ORDER_CANCELED,
                OperationalSubjectType.ORDER,
                saved.getId(),
                "Order #" + saved.getId(),
                reason.trim()
            );
            return saved;
        } catch (OptimisticLockingFailureException ex) {
            throw new ConcurrencyConflictException("Data changed, reload and try again");
        }
    }

    @Transactional
    public Order returnOrderItems(Long orderId, Long userId, Map<Long, Integer> returnQuantities, String reason) {
        try {
            if (reason == null || reason.trim().isEmpty()) {
                throw new ValidationException("Return reason is required");
            }
            if (returnQuantities == null || returnQuantities.isEmpty()) {
                throw new ValidationException("Select at least one item to return");
            }

            User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("User not found"));
            Order order = getOrderWithItems(orderId);
            authorizationService.requireManageOrder(user, order);

            OrderStatus currentStatus = order.getStatus() != null ? order.getStatus() : OrderStatus.COMPLETED;
            if (currentStatus == OrderStatus.CANCELED) {
                throw new ValidationException("Canceled orders cannot be returned");
            }
            if (currentStatus == OrderStatus.RETURNED) {
                throw new ValidationException("Order is already fully returned");
            }

            BigDecimal refundedAmount = MoneySupport.normalize(order.getRefundedAmount());
            BigDecimal refundDeltaTotal = MoneySupport.ZERO;
            boolean changed = false;
            Set<Product> affectedProducts = new HashSet<>();

            for (OrderItem item : order.getOrderItems()) {
                Integer requestedQty = returnQuantities.get(item.getId());
                if (requestedQty == null || requestedQty <= 0) {
                    continue;
                }

                int returnableQty = item.getReturnableQuantity();
                if (requestedQty > returnableQty) {
                    throw new ValidationException("Return quantity exceeds available quantity for " + item.getProductDisplayName());
                }

                Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new ValidationException("Product not found: " + item.getProduct().getId()));
                BigDecimal unitCost = MoneySupport.normalize(item.getCostAtSale());

                int currentReturnedQty = safeInt(item.getReturnedQuantity());
                int updatedReturnedQty = currentReturnedQty + requestedQty;
                BigDecimal refundBefore = item.calculateRefundForReturnedQuantity(currentReturnedQty);
                BigDecimal refundAfter = item.calculateRefundForReturnedQuantity(updatedReturnedQty);

                item.setReturnedQuantity(updatedReturnedQty);
                BigDecimal refundDelta = MoneySupport.subtract(refundAfter, refundBefore);
                refundedAmount = MoneySupport.add(refundedAmount, refundDelta);
                refundDeltaTotal = MoneySupport.add(refundDeltaTotal, refundDelta);
                changed = true;

                transactionService.recordTransaction(
                    product,
                    requestedQty,
                    InventoryTransactionType.RETURN,
                    order,
                    null,
                    user,
                    "Returned from Order #" + order.getId() + ": " + reason.trim(),
                    unitCost,
                    MoneySupport.multiply(unitCost, requestedQty)
                );
                affectedProducts.add(product);
            }

            if (!changed) {
                throw new ValidationException("No return quantity was provided");
            }

            inventoryLedgerService.recomputeProducts(affectedProducts);
            order.setRefundedAmount(refundedAmount);
            order.setStatusNote(reason.trim());

            boolean fullyReturned = order.getOrderItems().stream()
                .allMatch(item -> item.getReturnableQuantity() == 0);
            order.setStatus(fullyReturned ? OrderStatus.RETURNED : OrderStatus.PARTIALLY_RETURNED);

            Order saved = orderRepository.save(order);
            if (salesShiftService != null && MoneySupport.isPositive(refundDeltaTotal)) {
                salesShiftService.recordRefundEvent(user, saved, refundDeltaTotal, reason.trim());
            }
            operationalAuditLogService.record(
                user,
                OperationalAuditAction.ORDER_RETURNED,
                OperationalSubjectType.ORDER,
                saved.getId(),
                "Order #" + saved.getId(),
                reason.trim()
            );
            return saved;
        } catch (OptimisticLockingFailureException ex) {
            throw new ConcurrencyConflictException("Data changed, reload and try again");
        }
    }

    private void populateOrderItemSnapshot(OrderItem orderItem, Product product) {
        orderItem.setProductNameSnapshot(product.getName());
        orderItem.setSkuSnapshot(product.getSku());
        orderItem.setBarcodeSnapshot(product.getBarcode());
        orderItem.setCategoryNameSnapshot(
            product.getCategory() != null ? product.getCategory().getName() : "Uncategorized"
        );
        orderItem.setBrandNameSnapshot(product.getBrand() != null ? product.getBrand().getName() : null);
        orderItem.setOriginNameSnapshot(product.getOrigin() != null ? product.getOrigin().getName() : null);
        orderItem.setUnitNameSnapshot(product.getUnit() != null ? product.getUnit().getName() : null);
    }

    private void populateOrderCustomerSnapshot(Order order, Customer customer) {
        if (order == null || customer == null) {
            return;
        }
        order.setCustomerNameSnapshot(customer.getFullName());
        order.setCustomerPhoneSnapshot(customer.getPhone());
    }

    private Customer resolveActiveCustomer(Long customerId) {
        if (customerId == null) {
            return null;
        }
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ValidationException("Customer not found"));
        if (!customer.isEnabled()) {
            throw new ValidationException("Selected customer is disabled");
        }
        return customer;
    }

    private java.util.Map<Long, Integer> aggregateRequestedQuantities(
        java.util.List<CreateOrderRequest.OrderItemRequest> itemRequests
    ) {
        java.util.Map<Long, Integer> requestedQuantitiesByProductId = new java.util.TreeMap<>();
        for (CreateOrderRequest.OrderItemRequest itemRequest : itemRequests) {
            if (itemRequest == null || itemRequest.getProductId() == null) {
                throw new ValidationException("Product is required for each item");
            }
            int quantity = safeInt(itemRequest.getQuantity());
            if (quantity <= 0) {
                throw new ValidationException("Item quantity must be greater than zero");
            }
            requestedQuantitiesByProductId.merge(itemRequest.getProductId(), quantity, (left, right) -> {
                try {
                    return Math.addExact(left, right);
                } catch (ArithmeticException ex) {
                    throw new ValidationException("Item quantity is too large");
                }
            });
        }
        return requestedQuantitiesByProductId;
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

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }
}
