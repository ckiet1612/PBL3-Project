package com.pbl3.project.pbl3_project.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbl3.project.pbl3_project.dto.CreateOrderRequest;
import com.pbl3.project.pbl3_project.dto.payment.QrPaymentCreateRequest;
import com.pbl3.project.pbl3_project.dto.payment.QrPaymentStatusDto;
import com.pbl3.project.pbl3_project.dto.payment.SePayWebhookPayload;
import com.pbl3.project.pbl3_project.entity.Customer;
import com.pbl3.project.pbl3_project.entity.Order;
import com.pbl3.project.pbl3_project.entity.PaymentMethod;
import com.pbl3.project.pbl3_project.entity.Product;
import com.pbl3.project.pbl3_project.entity.Promotion;
import com.pbl3.project.pbl3_project.entity.QrPayment;
import com.pbl3.project.pbl3_project.entity.QrPaymentStatus;
import com.pbl3.project.pbl3_project.entity.Role;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.repository.CustomerRepository;
import com.pbl3.project.pbl3_project.repository.OrderRepository;
import com.pbl3.project.pbl3_project.repository.ProductRepository;
import com.pbl3.project.pbl3_project.repository.QrPaymentRepository;
import com.pbl3.project.pbl3_project.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class QrPaymentService {
    private static final long ORDER_CODE_BASE = 900_000_000L;

    private final QrPaymentRepository qrPaymentRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final AuthorizationService authorizationService;
    private final SalesShiftService salesShiftService;
    private final PromotionService promotionService;
    private final QrPaymentGateway paymentGateway;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final String publicBaseUrl;

    private record FinalizeReservation(Order existingOrder, String cartSnapshotJson) {
    }

    public QrPaymentService(
        QrPaymentRepository qrPaymentRepository,
        UserRepository userRepository,
        CustomerRepository customerRepository,
        ProductRepository productRepository,
        OrderRepository orderRepository,
        OrderService orderService,
        AuthorizationService authorizationService,
        SalesShiftService salesShiftService,
        PromotionService promotionService,
        QrPaymentGateway paymentGateway,
        ObjectMapper objectMapper,
        TransactionTemplate transactionTemplate,
        @Value("${app.public-base-url:${APP_PUBLIC_BASE_URL:http://localhost:8080}}") String publicBaseUrl
    ) {
        this.qrPaymentRepository = qrPaymentRepository;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.authorizationService = authorizationService;
        this.salesShiftService = salesShiftService;
        this.promotionService = promotionService;
        this.paymentGateway = paymentGateway;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
        this.publicBaseUrl = trimTrailingSlash(publicBaseUrl == null || publicBaseUrl.isBlank() ? "http://localhost:8080" : publicBaseUrl);
    }

    public QrPaymentStatusDto createPayment(QrPaymentCreateRequest request) {
        validateCreateRequest(request);
        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new ValidationException("User not found"));
        authorizationService.requireSalesAccess(user);
        salesShiftService.requireOpenShiftForSale(user);

        Customer customer = request.getCustomerId() == null
            ? null
            : customerRepository.findById(request.getCustomerId()).orElseThrow(() -> new ValidationException("Customer not found"));
        BigDecimal amount = calculateExpectedAmount(request);
        if (request.getAmount() != null && MoneySupport.differs(request.getAmount(), amount)) {
            throw new ValidationException("Cart total changed. Refresh checkout before generating QR.");
        }
        LocalDateTime now = LocalDateTime.now();

        QrPayment payment = new QrPayment();
        payment.setStatus(QrPaymentStatus.PENDING);
        payment.setAmount(amount);
        payment.setUser(user);
        payment.setCustomer(customer);
        payment.setPromotionId(request.getSelectedOrderPromotionId());
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);
        payment.setExpiresAt(now.plusSeconds(paymentGateway.paymentExpirySeconds()));
        payment.setCartSnapshotJson(writeSnapshot(request));
        payment = qrPaymentRepository.saveAndFlush(payment);

        payment.setOrderCode(ORDER_CODE_BASE + payment.getId());
        qrPaymentRepository.saveAndFlush(payment);

        try {
            QrPaymentGateway.QrPaymentLink link = paymentGateway.createPaymentLink(new QrPaymentGateway.QrPaymentRequest(
                payment.getOrderCode(),
                amount,
                buildPaymentDescription(payment.getOrderCode()),
                publicBaseUrl + "/api/qr-payments/cancelled",
                publicBaseUrl + "/api/qr-payments/return"
            ));
            payment.setPaymentLinkId(link.paymentLinkId());
            payment.setQrCode(link.qrCode());
            payment.setCheckoutUrl(link.checkoutUrl());
            payment.setUpdatedAt(LocalDateTime.now());
            return QrPaymentStatusDto.from(qrPaymentRepository.save(payment));
        } catch (RuntimeException ex) {
            payment.setStatus(QrPaymentStatus.CANCELLED);
            payment.setFailureReason(ex.getMessage());
            payment.setUpdatedAt(LocalDateTime.now());
            qrPaymentRepository.save(payment);
            throw ex;
        }
    }

    public QrPaymentStatusDto getPaymentStatus(Long paymentId) {
        return QrPaymentStatusDto.from(updateExpiredIfNeeded(findPayment(paymentId)));
    }

    public QrPaymentStatusDto getPaymentStatus(User actor, Long paymentId) {
        QrPayment payment = updateExpiredIfNeeded(findPayment(paymentId));
        requirePaymentAccess(actor, payment);
        return QrPaymentStatusDto.from(payment);
    }

    @Transactional
    public QrPaymentStatusDto refreshPaymentStatus(Long paymentId) {
        QrPayment payment = updateExpiredIfNeeded(findPaymentForUpdate(paymentId));
        if (payment.getStatus() != QrPaymentStatus.PENDING) {
            return QrPaymentStatusDto.from(payment);
        }
        QrPaymentGateway.QrPaymentProviderStatus providerStatus = paymentGateway.getPaymentStatus(payment.getOrderCode());
        applyProviderStatus(payment, providerStatus.status(), providerStatus.amountPaid());
        return QrPaymentStatusDto.from(qrPaymentRepository.save(payment));
    }

    @Transactional
    public QrPaymentStatusDto refreshPaymentStatus(User actor, Long paymentId) {
        QrPayment payment = updateExpiredIfNeeded(findPaymentForUpdate(paymentId));
        requirePaymentAccess(actor, payment);
        if (payment.getStatus() != QrPaymentStatus.PENDING) {
            return QrPaymentStatusDto.from(payment);
        }
        QrPaymentGateway.QrPaymentProviderStatus providerStatus = paymentGateway.getPaymentStatus(payment.getOrderCode());
        applyProviderStatus(payment, providerStatus.status(), providerStatus.amountPaid());
        return QrPaymentStatusDto.from(qrPaymentRepository.save(payment));
    }

    @Transactional
    public QrPaymentStatusDto cancelPayment(Long paymentId) {
        QrPayment payment = findPaymentForUpdate(paymentId);
        if (payment.getStatus() != QrPaymentStatus.PENDING) {
            return QrPaymentStatusDto.from(payment);
        }
        paymentGateway.cancelPayment(payment.getOrderCode(), "Cashier cancelled QR payment");
        payment.setStatus(QrPaymentStatus.CANCELLED);
        payment.setUpdatedAt(LocalDateTime.now());
        return QrPaymentStatusDto.from(qrPaymentRepository.save(payment));
    }

    @Transactional
    public QrPaymentStatusDto cancelPayment(User actor, Long paymentId) {
        QrPayment payment = findPaymentForUpdate(paymentId);
        requirePaymentAccess(actor, payment);
        if (payment.getStatus() != QrPaymentStatus.PENDING) {
            return QrPaymentStatusDto.from(payment);
        }
        paymentGateway.cancelPayment(payment.getOrderCode(), "Cashier cancelled QR payment");
        payment.setStatus(QrPaymentStatus.CANCELLED);
        payment.setUpdatedAt(LocalDateTime.now());
        return QrPaymentStatusDto.from(qrPaymentRepository.save(payment));
    }

    @Transactional
    public QrPaymentStatusDto handleSePayWebhook(SePayWebhookPayload payload, String authorizationHeader, String rawBody) {
        if (!paymentGateway.verifyWebhook(payload, authorizationHeader, rawBody)) {
            throw new QrPaymentException("Invalid SePay webhook authentication");
        }
        QrPayment payment = payload.orderCode() != null
            ? qrPaymentRepository.findByOrderCodeForUpdate(payload.orderCode()).orElse(null)
            : null;
        if (payment == null && payload.paymentLinkId() != null) {
            payment = qrPaymentRepository.findByPaymentLinkIdForUpdate(payload.paymentLinkId()).orElse(null);
        }
        if (payment == null) {
            throw new QrPaymentException("QR payment not found for webhook");
        }
        if (payload.paymentLinkId() != null && (payment.getPaymentLinkId() == null || payment.getPaymentLinkId().isBlank())) {
            payment.setPaymentLinkId(payload.paymentLinkId());
        }
        if (payload.isIncoming()) {
            markPaid(payment, payload.amount());
        }
        return QrPaymentStatusDto.from(qrPaymentRepository.save(payment));
    }

    public Order finalizePaidPayment(Long paymentId) {
        FinalizeReservation reservation = reservePaidPaymentForOrderCreation(paymentId);
        if (reservation.existingOrder() != null) {
            return reservation.existingOrder();
        }

        try {
            CreateOrderRequest orderRequest = objectMapper.readValue(reservation.cartSnapshotJson(), CreateOrderRequest.class);
            orderRequest.setPaymentMethod(PaymentMethod.QR);
            Order order = orderService.createOrder(orderRequest);
            markPaymentOrderCreated(paymentId, order);
            return order;
        } catch (Exception ex) {
            markPaymentOrderFailed(paymentId, ex);
            throw new QrPaymentException("Payment was received, but order creation failed: " + ex.getMessage(), ex);
        }
    }

    private FinalizeReservation reservePaidPaymentForOrderCreation(Long paymentId) {
        return transactionTemplate.execute(status -> {
            QrPayment payment = findPaymentForUpdate(paymentId);
            if (payment.getStatus() == QrPaymentStatus.ORDER_CREATED && payment.getCreatedOrder() != null) {
                return new FinalizeReservation(payment.getCreatedOrder(), null);
            }
            if (payment.getStatus() == QrPaymentStatus.ORDER_CREATING) {
                throw new QrPaymentException("QR payment is already being finalized");
            }
            if (payment.getStatus() != QrPaymentStatus.PAID) {
                throw new QrPaymentException("QR payment is not paid yet");
            }
            payment.setStatus(QrPaymentStatus.ORDER_CREATING);
            payment.setUpdatedAt(LocalDateTime.now());
            qrPaymentRepository.saveAndFlush(payment);
            return new FinalizeReservation(null, payment.getCartSnapshotJson());
        });
    }

    private void markPaymentOrderCreated(Long paymentId, Order order) {
        transactionTemplate.executeWithoutResult(status -> {
            QrPayment payment = findPaymentForUpdate(paymentId);
            if (payment.getStatus() == QrPaymentStatus.ORDER_CREATED && payment.getCreatedOrder() != null) {
                return;
            }
            payment.setCreatedOrder(orderRepository.getReferenceById(order.getId()));
            payment.setStatus(QrPaymentStatus.ORDER_CREATED);
            payment.setFailureReason(null);
            payment.setUpdatedAt(LocalDateTime.now());
            qrPaymentRepository.save(payment);
        });
    }

    private void markPaymentOrderFailed(Long paymentId, Exception ex) {
        transactionTemplate.executeWithoutResult(status -> {
            QrPayment payment = findPaymentForUpdate(paymentId);
            payment.setStatus(QrPaymentStatus.PAID_ORDER_FAILED);
            payment.setFailureReason(ex.getMessage());
            payment.setUpdatedAt(LocalDateTime.now());
            qrPaymentRepository.save(payment);
        });
    }

    private void validateCreateRequest(QrPaymentCreateRequest request) {
        if (request == null) {
            throw new ValidationException("QR payment request is required");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ValidationException("Add at least one item");
        }
        for (CreateOrderRequest.OrderItemRequest item : request.getItems()) {
            if (item == null || item.getProductId() == null) {
                throw new ValidationException("Product is required");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new ValidationException("Quantity must be greater than zero");
            }
        }
    }

    private BigDecimal calculateExpectedAmount(QrPaymentCreateRequest request) {
        LocalDateTime now = LocalDateTime.now();
        java.util.Map<Long, Product> productById = new java.util.LinkedHashMap<>();
        for (CreateOrderRequest.OrderItemRequest item : request.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                .orElseThrow(() -> new ValidationException("Product not found: " + item.getProductId()));
            if (product.getQuantity() == null || product.getQuantity() < item.getQuantity()) {
                throw new ValidationException("Not enough stock for product: " + product.getName());
            }
            productById.putIfAbsent(product.getId(), product);
        }

        java.util.Map<Long, PromotionService.ProductPricingPreview> pricingByProductId =
            promotionService.previewBestProductPricing(productById.values(), now);
        BigDecimal subtotalAfterProductDiscount = MoneySupport.ZERO;
        for (CreateOrderRequest.OrderItemRequest item : request.getItems()) {
            Product product = productById.get(item.getProductId());
            PromotionService.ProductPricingPreview pricing = pricingByProductId.getOrDefault(
                product.getId(),
                new PromotionService.ProductPricingPreview(
                    product,
                    null,
                    MoneySupport.normalize(product.getPrice()),
                    MoneySupport.normalize(product.getPrice()),
                    MoneySupport.ZERO
                )
            );
            subtotalAfterProductDiscount = MoneySupport.add(
                subtotalAfterProductDiscount,
                MoneySupport.multiply(pricing.discountedUnitPrice(), item.getQuantity())
            );
        }

        Promotion selectedOrderPromotion = promotionService.resolveEligibleOrderPromotion(
            request.getSelectedOrderPromotionId(),
            subtotalAfterProductDiscount,
            now
        );
        BigDecimal orderDiscount = selectedOrderPromotion != null
            ? promotionService.computeDiscountAmount(selectedOrderPromotion, subtotalAfterProductDiscount)
            : MoneySupport.ZERO;
        BigDecimal expectedAmount = MoneySupport.subtract(subtotalAfterProductDiscount, orderDiscount);
        if (!MoneySupport.isPositive(expectedAmount)) {
            throw new ValidationException("QR payment amount must be greater than zero");
        }
        return expectedAmount;
    }

    private String writeSnapshot(QrPaymentCreateRequest request) {
        try {
            CreateOrderRequest snapshot = new CreateOrderRequest();
            snapshot.setUserId(request.getUserId());
            snapshot.setCustomerId(request.getCustomerId());
            snapshot.setSelectedOrderPromotionId(request.getSelectedOrderPromotionId());
            snapshot.setPaymentMethod(PaymentMethod.QR);
            snapshot.setItems(new ArrayList<>(copyItems(request.getItems())));
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception ex) {
            throw new QrPaymentException("Could not store QR payment cart snapshot", ex);
        }
    }

    private List<CreateOrderRequest.OrderItemRequest> copyItems(List<CreateOrderRequest.OrderItemRequest> sourceItems) {
        return sourceItems.stream().map(source -> {
            CreateOrderRequest.OrderItemRequest target = new CreateOrderRequest.OrderItemRequest();
            target.setProductId(source.getProductId());
            target.setQuantity(source.getQuantity());
            return target;
        }).toList();
    }

    private void applyProviderStatus(QrPayment payment, String status, BigDecimal amountPaid) {
        String normalized = status == null ? "" : status.trim().toUpperCase(java.util.Locale.ROOT);
        if ("PAID".equals(normalized) || "00".equals(normalized)) {
            markPaid(payment, amountPaid);
        } else if ("CANCELLED".equals(normalized) || "CANCELED".equals(normalized)) {
            payment.setStatus(QrPaymentStatus.CANCELLED);
            payment.setUpdatedAt(LocalDateTime.now());
        } else {
            updateExpiredIfNeeded(payment);
        }
    }

    private void markPaid(QrPayment payment, BigDecimal paidAmount) {
        if (payment.getStatus() == QrPaymentStatus.ORDER_CREATED
            || payment.getStatus() == QrPaymentStatus.ORDER_CREATING
            || payment.getStatus() == QrPaymentStatus.PAID_ORDER_FAILED) {
            return;
        }
        if (MoneySupport.normalize(paidAmount).compareTo(payment.getAmount()) < 0) {
            throw new QrPaymentException("Paid QR amount is lower than expected");
        }
        payment.setStatus(QrPaymentStatus.PAID);
        payment.setPaidAt(payment.getPaidAt() != null ? payment.getPaidAt() : LocalDateTime.now());
        payment.setFailureReason(null);
        payment.setUpdatedAt(LocalDateTime.now());
    }

    private QrPayment updateExpiredIfNeeded(QrPayment payment) {
        if (payment.getStatus() == QrPaymentStatus.PENDING
            && payment.getExpiresAt() != null
            && LocalDateTime.now().isAfter(payment.getExpiresAt())) {
            payment.setStatus(QrPaymentStatus.EXPIRED);
            payment.setUpdatedAt(LocalDateTime.now());
            return qrPaymentRepository.save(payment);
        }
        return payment;
    }

    private QrPayment findPayment(Long paymentId) {
        return qrPaymentRepository.findById(paymentId)
            .orElseThrow(() -> new QrPaymentException("QR payment not found"));
    }

    private QrPayment findPaymentForUpdate(Long paymentId) {
        return qrPaymentRepository.findByIdForUpdate(paymentId)
            .orElseThrow(() -> new QrPaymentException("QR payment not found"));
    }

    private void requirePaymentAccess(User actor, QrPayment payment) {
        authorizationService.requireSalesAccess(actor);
        if (payment == null) {
            throw new QrPaymentException("QR payment not found");
        }
        if (authorizationService.hasAnyRole(actor, Role.ADMIN, Role.MANAGER)) {
            return;
        }
        Long actorId = actor != null ? actor.getId() : null;
        Long ownerId = payment.getUser() != null ? payment.getUser().getId() : null;
        if (actorId == null || ownerId == null || !actorId.equals(ownerId)) {
            throw new AuthorizationException("You are not allowed to access this QR payment");
        }
    }

    private String buildPaymentDescription(Long orderCode) {
        return SePayClient.buildPaymentCode(orderCode);
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().replaceAll("/+$", "");
    }
}
