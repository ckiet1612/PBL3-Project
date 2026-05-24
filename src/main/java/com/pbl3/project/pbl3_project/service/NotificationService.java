package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.dto.report.ActionCenterItem;
import com.pbl3.project.pbl3_project.dto.report.ActionCenterSnapshot;
import com.pbl3.project.pbl3_project.dto.report.InsightSeverity;
import com.pbl3.project.pbl3_project.entity.Notification;
import com.pbl3.project.pbl3_project.entity.NotificationActionTarget;
import com.pbl3.project.pbl3_project.entity.NotificationCategory;
import com.pbl3.project.pbl3_project.entity.NotificationSeverity;
import com.pbl3.project.pbl3_project.entity.NotificationType;
import com.pbl3.project.pbl3_project.entity.NotificationUserState;
import com.pbl3.project.pbl3_project.entity.Product;
import com.pbl3.project.pbl3_project.entity.Promotion;
import com.pbl3.project.pbl3_project.entity.QrPayment;
import com.pbl3.project.pbl3_project.entity.QrPaymentStatus;
import com.pbl3.project.pbl3_project.entity.Role;
import com.pbl3.project.pbl3_project.entity.SalesShift;
import com.pbl3.project.pbl3_project.entity.SalesShiftStatus;
import com.pbl3.project.pbl3_project.entity.StocktakeSession;
import com.pbl3.project.pbl3_project.entity.StocktakeSessionStatus;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.repository.NotificationRepository;
import com.pbl3.project.pbl3_project.repository.NotificationUserStateRepository;
import com.pbl3.project.pbl3_project.repository.ProductRepository;
import com.pbl3.project.pbl3_project.repository.PromotionRepository;
import com.pbl3.project.pbl3_project.repository.QrPaymentRepository;
import com.pbl3.project.pbl3_project.repository.SalesShiftRepository;
import com.pbl3.project.pbl3_project.repository.StocktakeSessionRepository;
import com.pbl3.project.pbl3_project.repository.UserRepository;
import com.pbl3.project.pbl3_project.service.SePaySettingsService.SePaySettings;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class NotificationService {
    private static final Duration OPEN_SHIFT_WARNING_AGE = Duration.ofHours(8);
    private static final Duration PROMOTION_EXPIRING_WINDOW = Duration.ofDays(3);
    private static final Set<NotificationType> GENERATED_SYSTEM_TYPES = EnumSet.of(
        NotificationType.LOW_STOCK,
        NotificationType.OUT_OF_STOCK,
        NotificationType.OPEN_SHIFT,
        NotificationType.QR_PAYMENT_ISSUE,
        NotificationType.SEPAY_CONFIG,
        NotificationType.PROMOTION_EXPIRING,
        NotificationType.PROMOTION_EXPIRED,
        NotificationType.STOCKTAKE_OPEN,
        NotificationType.REORDER_SUGGESTION
    );

    private final NotificationRepository notificationRepository;
    private final NotificationUserStateRepository stateRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final SalesShiftRepository salesShiftRepository;
    private final QrPaymentRepository qrPaymentRepository;
    private final PromotionRepository promotionRepository;
    private final StocktakeSessionRepository stocktakeSessionRepository;
    private final OperationalInsightService operationalInsightService;
    private final SePaySettingsService sePaySettingsService;
    private final AuthorizationService authorizationService;
    private final TransactionTemplate refreshTransactionTemplate;

    public NotificationService(
        NotificationRepository notificationRepository,
        NotificationUserStateRepository stateRepository,
        UserRepository userRepository,
        ProductRepository productRepository,
        SalesShiftRepository salesShiftRepository,
        QrPaymentRepository qrPaymentRepository,
        PromotionRepository promotionRepository,
        StocktakeSessionRepository stocktakeSessionRepository,
        OperationalInsightService operationalInsightService,
        SePaySettingsService sePaySettingsService,
        AuthorizationService authorizationService,
        PlatformTransactionManager transactionManager
    ) {
        this.notificationRepository = notificationRepository;
        this.stateRepository = stateRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.salesShiftRepository = salesShiftRepository;
        this.qrPaymentRepository = qrPaymentRepository;
        this.promotionRepository = promotionRepository;
        this.stocktakeSessionRepository = stocktakeSessionRepository;
        this.operationalInsightService = operationalInsightService;
        this.sePaySettingsService = sePaySettingsService;
        this.authorizationService = authorizationService;
        this.refreshTransactionTemplate = new TransactionTemplate(transactionManager);
        this.refreshTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public synchronized void refreshSystemNotifications() {
        refreshSystemNotifications(false);
    }

    public synchronized void refreshSystemNotifications(boolean includeReorderInsights) {
        refreshTransactionTemplate.executeWithoutResult(status -> refreshSystemNotificationsInTransaction(includeReorderInsights));
    }

    private void refreshSystemNotificationsInTransaction(boolean includeReorderInsights) {
        LocalDateTime now = LocalDateTime.now();
        Set<String> activeEventKeys = new LinkedHashSet<>();
        List<User> managers = managementRecipients();

        refreshInventoryNotifications(now, activeEventKeys, managers);
        refreshOpenShiftNotifications(now, activeEventKeys, managers);
        refreshQrPaymentNotifications(now, activeEventKeys, managers);
        refreshSePayConfigNotification(now, activeEventKeys, managers);
        refreshPromotionNotifications(now, activeEventKeys, managers);
        refreshStocktakeNotifications(now, activeEventKeys, managers);
        if (includeReorderInsights) {
            refreshReorderNotifications(now, activeEventKeys, managers);
        }
        resolveInactiveSystemNotifications(now, activeEventKeys);
    }

    @Transactional(readOnly = true)
    public List<NotificationView> listForUser(User user, NotificationFilter filter) {
        requireEnabledUser(user);
        LocalDateTime now = LocalDateTime.now();
        NotificationFilter effectiveFilter = filter == null ? NotificationFilter.ALL : filter;
        return stateRepository.findVisibleForUser(user.getId(), now, false).stream()
            .map(this::toView)
            .filter(view -> matchesFilter(view, effectiveFilter))
            .sorted(viewComparator())
            .toList();
    }

    @Transactional(readOnly = true)
    public long countUnread(User user) {
        if (user == null || user.getId() == null || !user.isEnabled()) {
            return 0L;
        }
        return stateRepository.countUnreadForUser(user.getId(), LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public Optional<NotificationView> findVisibleForUser(User user, Long notificationId) {
        requireEnabledUser(user);
        if (notificationId == null) {
            return Optional.empty();
        }
        LocalDateTime now = LocalDateTime.now();
        return stateRepository.findByNotificationIdAndUserId(notificationId, user.getId())
            .filter(state -> state.getDismissedAt() == null)
            .filter(state -> {
                Notification notification = state.getNotification();
                return notification.getResolvedAt() == null
                    && (notification.getExpiresAt() == null || notification.getExpiresAt().isAfter(now));
            })
            .map(this::toView);
    }

    public long refreshAndCountUnread(User user) {
        if (user == null || user.getId() == null || !user.isEnabled()) {
            return 0L;
        }
        refreshSystemNotifications();
        return countUnread(user);
    }

    @Transactional
    public NotificationView markRead(User user, Long notificationId) {
        NotificationUserState state = requireVisibleState(user, notificationId);
        if (state.getReadAt() == null) {
            state.setReadAt(LocalDateTime.now());
        }
        return toView(stateRepository.save(state));
    }

    @Transactional
    public NotificationView dismiss(User user, Long notificationId) {
        NotificationUserState state = requireVisibleState(user, notificationId);
        LocalDateTime now = LocalDateTime.now();
        if (state.getReadAt() == null) {
            state.setReadAt(now);
        }
        state.setDismissedAt(now);
        return toView(stateRepository.save(state));
    }

    @Transactional
    public NotificationView completeTask(User user, Long notificationId) {
        NotificationUserState state = requireVisibleState(user, notificationId);
        if (state.getNotification().getCategory() != NotificationCategory.TASK) {
            throw new ValidationException("Only task notifications can be completed");
        }
        LocalDateTime now = LocalDateTime.now();
        if (state.getReadAt() == null) {
            state.setReadAt(now);
        }
        state.setCompletedAt(now);
        return toView(stateRepository.save(state));
    }

    @Transactional
    public NotificationView createTask(User actor, CreateTaskRequest request) {
        requireTaskManager(actor);
        CreateTaskRequest normalized = normalizeTaskRequest(request);
        List<User> recipients = resolveTaskRecipients(normalized);
        if (recipients.isEmpty()) {
            throw new ValidationException("Select at least one task recipient");
        }

        Notification notification = new Notification();
        notification.setCategory(NotificationCategory.TASK);
        notification.setType(NotificationType.TASK);
        notification.setSeverity(normalized.severity());
        notification.setTitle(normalized.title());
        notification.setMessage(normalized.message());
        notification.setActionTarget(normalized.actionTarget());
        notification.setActionPayloadJson(normalized.actionPayloadJson());
        notification.setCreatedBy(actor);
        notification.setExpiresAt(normalized.expiresAt());
        Notification savedNotification = notificationRepository.save(notification);
        syncRecipients(savedNotification, recipients);
        return stateRepository.findByNotificationIdAndUserId(savedNotification.getId(), recipients.get(0).getId())
            .map(this::toView)
            .orElseGet(() -> new NotificationView(
                savedNotification.getId(),
                savedNotification.getCategory(),
                savedNotification.getType(),
                savedNotification.getSeverity(),
                savedNotification.getTitle(),
                savedNotification.getMessage(),
                savedNotification.getActionTarget(),
                savedNotification.getActionPayloadJson(),
                savedNotification.getCreatedBy() != null ? displayName(savedNotification.getCreatedBy()) : "",
                savedNotification.getCreatedAt(),
                null,
                null,
                null,
                savedNotification.getSourceType(),
                savedNotification.getSourceId()
            ));
    }

    @Transactional(readOnly = true)
    public List<RecipientOption> listAssignableUsers(User actor) {
        requireTaskManager(actor);
        return userRepository.findAll().stream()
            .filter(User::isEnabled)
            .sorted(Comparator
                .comparing((User candidate) -> roleRank(candidate.getRole()))
                .thenComparing(this::displayName, String.CASE_INSENSITIVE_ORDER))
            .map(candidate -> new RecipientOption(candidate.getId(), displayName(candidate), candidate.getRole()))
            .toList();
    }

    private void refreshInventoryNotifications(LocalDateTime now, Set<String> activeEventKeys, List<User> managers) {
        for (Product product : productRepository.findLowStockProducts()) {
            int quantity = product.getQuantity() != null ? product.getQuantity() : 0;
            NotificationType type = quantity <= 0 ? NotificationType.OUT_OF_STOCK : NotificationType.LOW_STOCK;
            String key = "product-stock:" + product.getId() + ":" + type;
            activeEventKeys.add(key);
            String title = quantity <= 0 ? "Out of stock: " + product.getName() : "Low stock: " + product.getName();
            String message = product.getName() + " has " + quantity + " units on hand. Minimum stock is "
                + (product.getMinStockLevel() != null ? product.getMinStockLevel() : 0) + ".";
            upsertSystemNotification(
                key,
                type,
                quantity <= 0 ? NotificationSeverity.CRITICAL : NotificationSeverity.WARNING,
                title,
                message,
                "PRODUCT",
                product.getId(),
                NotificationActionTarget.PRODUCTS,
                null,
                managers
            );
        }
    }

    private void refreshOpenShiftNotifications(LocalDateTime now, Set<String> activeEventKeys, List<User> managers) {
        LocalDateTime openedBefore = now.minus(OPEN_SHIFT_WARNING_AGE);
        for (SalesShift shift : salesShiftRepository.findAllByStatusAndOpenedAtBeforeWithUser(SalesShiftStatus.OPEN, openedBefore)) {
            if (shift.getOpenedBy() == null || shift.getOpenedAt() == null) {
                continue;
            }
            String key = "open-shift:" + shift.getId();
            activeEventKeys.add(key);
            List<User> recipients = withUser(managers, shift.getOpenedBy());
            upsertSystemNotification(
                key,
                NotificationType.OPEN_SHIFT,
                NotificationSeverity.WARNING,
                "Open shift needs review",
                "Shift #" + shift.getId() + " opened by " + displayName(shift.getOpenedBy()) + " has been open for more than 8 hours.",
                "SALES_SHIFT",
                shift.getId(),
                NotificationActionTarget.REPORTS_SHIFTS,
                null,
                recipients
            );
        }
    }

    private void refreshQrPaymentNotifications(LocalDateTime now, Set<String> activeEventKeys, List<User> managers) {
        for (QrPayment payment : qrPaymentRepository.findAllByStatusWithUser(QrPaymentStatus.PAID_ORDER_FAILED)) {
            String key = "qr-payment:" + payment.getId() + ":paid-order-failed";
            activeEventKeys.add(key);
            List<User> recipients = withUser(managers, payment.getUser());
            upsertSystemNotification(
                key,
                NotificationType.QR_PAYMENT_ISSUE,
                NotificationSeverity.CRITICAL,
                "QR payment needs manual handling",
                "QR payment #" + payment.getId() + " was paid but order creation failed. Review and reconcile manually.",
                "QR_PAYMENT",
                payment.getId(),
                NotificationActionTarget.SALES_POS,
                null,
                recipients
            );
        }
    }

    private void refreshSePayConfigNotification(LocalDateTime now, Set<String> activeEventKeys, List<User> managers) {
        SePaySettings settings = sePaySettingsService.resolveEffectiveSettings();
        if (settings.enabled() && settings.configured()) {
            return;
        }
        String key = "sepay-config:incomplete";
        activeEventKeys.add(key);
        String message = settings.enabled()
            ? "SePay is enabled but API token, bank, or account number is missing."
            : "SePay QR checkout is disabled. Enable it in Settings before using QR payments.";
        upsertSystemNotification(
            key,
            NotificationType.SEPAY_CONFIG,
            NotificationSeverity.INFO,
            "QR payment setup incomplete",
            message,
            "SETTINGS",
            null,
            NotificationActionTarget.SETTINGS_QR,
            null,
            managers
        );
    }

    private void refreshPromotionNotifications(LocalDateTime now, Set<String> activeEventKeys, List<User> managers) {
        LocalDateTime expiringBefore = now.plus(PROMOTION_EXPIRING_WINDOW);
        for (Promotion promotion : promotionRepository.findEnabledEndingBefore(expiringBefore)) {
            boolean expired = promotion.getEndsAt().isBefore(now);
            boolean expiringSoon = !expired && !promotion.getEndsAt().isAfter(expiringBefore);
            if (!expired && !expiringSoon) {
                continue;
            }
            NotificationType type = expired ? NotificationType.PROMOTION_EXPIRED : NotificationType.PROMOTION_EXPIRING;
            String key = "promotion:" + promotion.getId() + ":" + type;
            activeEventKeys.add(key);
            upsertSystemNotification(
                key,
                type,
                expired ? NotificationSeverity.WARNING : NotificationSeverity.INFO,
                expired ? "Promotion expired: " + promotion.getName() : "Promotion ending soon: " + promotion.getName(),
                expired
                    ? promotion.getName() + " has ended but is still enabled."
                    : promotion.getName() + " ends within 3 days.",
                "PROMOTION",
                promotion.getId(),
                NotificationActionTarget.PROMOTIONS,
                null,
                managers
            );
        }
    }

    private void refreshStocktakeNotifications(LocalDateTime now, Set<String> activeEventKeys, List<User> managers) {
        for (StocktakeSession session : stocktakeSessionRepository.findAllByStatus(StocktakeSessionStatus.OPEN)) {
            String key = "stocktake-open:" + session.getId();
            activeEventKeys.add(key);
            List<User> recipients = withUser(managers, session.getCreatedBy());
            upsertSystemNotification(
                key,
                NotificationType.STOCKTAKE_OPEN,
                NotificationSeverity.INFO,
                "Open stocktake session",
                "Stocktake #" + session.getId() + " is still open and has not been applied.",
                "STOCKTAKE",
                session.getId(),
                NotificationActionTarget.STOCKTAKE,
                null,
                recipients
            );
        }
    }

    private void refreshReorderNotifications(LocalDateTime now, Set<String> activeEventKeys, List<User> managers) {
        try {
            ActionCenterSnapshot snapshot = operationalInsightService.getDashboardInsights(null).actionCenter();
            if (snapshot == null || snapshot.items() == null) {
                return;
            }
            for (ActionCenterItem item : snapshot.items()) {
                if (item.productId() == null || item.suggestedQuantity() == null || item.suggestedQuantity() <= 0) {
                    continue;
                }
                String key = "reorder:" + item.productId();
                activeEventKeys.add(key);
                upsertSystemNotification(
                    key,
                    NotificationType.REORDER_SUGGESTION,
                    toSeverity(item.severity()),
                    item.title(),
                    item.description(),
                    "PRODUCT",
                    item.productId(),
                    NotificationActionTarget.PRODUCTS,
                    "{\"suggestedQuantity\":" + item.suggestedQuantity() + "}",
                    managers
                );
            }
        } catch (RuntimeException ex) {
            // Insight notifications should not block the bell or the notifications page.
        }
    }

    private Notification upsertSystemNotification(
        String eventKey,
        NotificationType type,
        NotificationSeverity severity,
        String title,
        String message,
        String sourceType,
        Long sourceId,
        NotificationActionTarget actionTarget,
        String actionPayloadJson,
        Collection<User> recipients
    ) {
        Notification notification = notificationRepository.findByEventKey(eventKey).orElseGet(Notification::new);
        boolean reopeningResolvedNotification = notification.getId() != null && notification.getResolvedAt() != null;
        notification.setCategory(NotificationCategory.SYSTEM);
        notification.setType(type);
        notification.setSeverity(severity);
        notification.setTitle(trimTo(title, 180));
        notification.setMessage(blankToDefault(message, title));
        notification.setSourceType(sourceType);
        notification.setSourceId(sourceId);
        notification.setEventKey(eventKey);
        notification.setActionTarget(actionTarget);
        notification.setActionPayloadJson(actionPayloadJson);
        notification.setResolvedAt(null);
        notification = notificationRepository.save(notification);
        if (reopeningResolvedNotification) {
            resetUserStates(notification);
        }
        syncRecipients(notification, recipients);
        return notification;
    }

    private void resetUserStates(Notification notification) {
        for (NotificationUserState state : stateRepository.findAllByNotificationId(notification.getId())) {
            state.setReadAt(null);
            state.setDismissedAt(null);
            state.setCompletedAt(null);
            stateRepository.save(state);
        }
    }

    private void resolveInactiveSystemNotifications(LocalDateTime now, Set<String> activeEventKeys) {
        for (Notification notification : notificationRepository.findAllByCategoryAndTypeInAndResolvedAtIsNull(
            NotificationCategory.SYSTEM,
            GENERATED_SYSTEM_TYPES
        )) {
            if (notification.getEventKey() == null || activeEventKeys.contains(notification.getEventKey())) {
                continue;
            }
            notification.setResolvedAt(now);
            notificationRepository.save(notification);
        }
    }

    private void syncRecipients(Notification notification, Collection<User> recipients) {
        if (notification == null || notification.getId() == null || recipients == null) {
            return;
        }
        for (User recipient : uniqueEnabledUsers(recipients)) {
            if (recipient.getId() == null || stateRepository.existsByNotificationIdAndUserId(notification.getId(), recipient.getId())) {
                continue;
            }
            NotificationUserState state = new NotificationUserState();
            state.setNotification(notification);
            state.setUser(recipient);
            stateRepository.save(state);
        }
    }

    private NotificationUserState requireVisibleState(User user, Long notificationId) {
        requireEnabledUser(user);
        if (notificationId == null) {
            throw new ValidationException("Notification is required");
        }
        NotificationUserState state = stateRepository.findByNotificationIdAndUserId(notificationId, user.getId())
            .orElseThrow(() -> new AuthorizationException("You are not allowed to access this notification"));
        Notification notification = state.getNotification();
        LocalDateTime now = LocalDateTime.now();
        if (notification.getResolvedAt() != null || (notification.getExpiresAt() != null && !notification.getExpiresAt().isAfter(now))) {
            throw new ValidationException("Notification is no longer active");
        }
        return state;
    }

    private CreateTaskRequest normalizeTaskRequest(CreateTaskRequest request) {
        if (request == null) {
            throw new ValidationException("Task details are required");
        }
        String title = normalize(request.title());
        String message = normalize(request.message());
        if (title.isBlank()) {
            throw new ValidationException("Task title is required");
        }
        if (message.isBlank()) {
            message = title;
        }
        return new CreateTaskRequest(
            trimTo(title, 180),
            message,
            request.recipientUserIds() == null ? Set.of() : request.recipientUserIds(),
            request.recipientRoles() == null ? Set.of() : request.recipientRoles(),
            request.severity() == null ? NotificationSeverity.INFO : request.severity(),
            request.expiresAt(),
            request.actionTarget(),
            normalize(request.actionPayloadJson()).isBlank() ? null : normalize(request.actionPayloadJson())
        );
    }

    private List<User> resolveTaskRecipients(CreateTaskRequest request) {
        Set<Long> requestedUserIds = request.recipientUserIds().stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Role> requestedRoles = request.recipientRoles().stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        return userRepository.findAll().stream()
            .filter(User::isEnabled)
            .filter(user -> requestedUserIds.contains(user.getId()) || requestedRoles.contains(user.getRole()))
            .sorted(Comparator.comparing(this::displayName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    private List<User> managementRecipients() {
        return userRepository.findAll().stream()
            .filter(User::isEnabled)
            .filter(user -> user.getRole() == Role.ADMIN || user.getRole() == Role.MANAGER)
            .sorted(Comparator.comparing(this::displayName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    private List<User> withUser(Collection<User> baseRecipients, User extraRecipient) {
        List<User> recipients = new ArrayList<>();
        if (baseRecipients != null) {
            recipients.addAll(baseRecipients);
        }
        if (extraRecipient != null) {
            recipients.add(extraRecipient);
        }
        return uniqueEnabledUsers(recipients);
    }

    private List<User> uniqueEnabledUsers(Collection<User> users) {
        Map<Long, User> unique = new LinkedHashMap<>();
        for (User user : users == null ? List.<User>of() : users) {
            if (user == null || user.getId() == null || !user.isEnabled()) {
                continue;
            }
            unique.putIfAbsent(user.getId(), user);
        }
        return new ArrayList<>(unique.values());
    }

    private NotificationView toView(NotificationUserState state) {
        Notification notification = state.getNotification();
        return new NotificationView(
            notification.getId(),
            notification.getCategory(),
            notification.getType(),
            notification.getSeverity(),
            notification.getTitle(),
            notification.getMessage(),
            notification.getActionTarget(),
            notification.getActionPayloadJson(),
            notification.getCreatedBy() != null ? displayName(notification.getCreatedBy()) : "",
            notification.getCreatedAt(),
            state.getReadAt(),
            state.getDismissedAt(),
            state.getCompletedAt(),
            notification.getSourceType(),
            notification.getSourceId()
        );
    }

    private boolean matchesFilter(NotificationView view, NotificationFilter filter) {
        return switch (filter) {
            case ALL -> true;
            case UNREAD -> view.readAt() == null;
            case CRITICAL -> view.severity() == NotificationSeverity.CRITICAL;
            case TASKS -> view.category() == NotificationCategory.TASK;
        };
    }

    private Comparator<NotificationView> viewComparator() {
        return Comparator
            .comparing((NotificationView view) -> view.readAt() == null ? 0 : 1)
            .thenComparing(view -> severityRank(view.severity()))
            .thenComparing(NotificationView::createdAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private int severityRank(NotificationSeverity severity) {
        if (severity == NotificationSeverity.CRITICAL) {
            return 0;
        }
        if (severity == NotificationSeverity.WARNING) {
            return 1;
        }
        return 2;
    }

    private int roleRank(Role role) {
        if (role == Role.ADMIN) {
            return 0;
        }
        if (role == Role.MANAGER) {
            return 1;
        }
        return 2;
    }

    private NotificationSeverity toSeverity(InsightSeverity severity) {
        if (severity == InsightSeverity.CRITICAL) {
            return NotificationSeverity.CRITICAL;
        }
        if (severity == InsightSeverity.WARNING) {
            return NotificationSeverity.WARNING;
        }
        return NotificationSeverity.INFO;
    }

    private void requireEnabledUser(User user) {
        if (user == null || user.getId() == null || !user.isEnabled()) {
            throw new AuthorizationException("You are not allowed to access Notifications");
        }
    }

    private void requireTaskManager(User actor) {
        if (!authorizationService.hasAnyRole(actor, Role.ADMIN, Role.MANAGER)) {
            throw new AuthorizationException("Only managers and admins can create notification tasks");
        }
    }

    private String displayName(User user) {
        if (user == null) {
            return "System";
        }
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return "User #" + user.getId();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String blankToDefault(String value, String defaultValue) {
        String normalized = normalize(value);
        return normalized.isBlank() ? normalize(defaultValue) : normalized;
    }

    private String trimTo(String value, int maxLength) {
        String normalized = normalize(value);
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    public enum NotificationFilter {
        ALL,
        UNREAD,
        CRITICAL,
        TASKS
    }

    public record NotificationView(
        Long id,
        NotificationCategory category,
        NotificationType type,
        NotificationSeverity severity,
        String title,
        String message,
        NotificationActionTarget actionTarget,
        String actionPayloadJson,
        String createdByLabel,
        LocalDateTime createdAt,
        LocalDateTime readAt,
        LocalDateTime dismissedAt,
        LocalDateTime completedAt,
        String sourceType,
        Long sourceId
    ) {
        public boolean unread() {
            return readAt == null;
        }

        public boolean completed() {
            return completedAt != null;
        }
    }

    public record CreateTaskRequest(
        String title,
        String message,
        Set<Long> recipientUserIds,
        Set<Role> recipientRoles,
        NotificationSeverity severity,
        LocalDateTime expiresAt,
        NotificationActionTarget actionTarget,
        String actionPayloadJson
    ) {
    }

    public record RecipientOption(Long id, String label, Role role) {
        public String displayLabel() {
            String roleLabel = role == null ? "User" : role.name().substring(0, 1) + role.name().substring(1).toLowerCase(Locale.ROOT);
            return label + " (" + roleLabel + ")";
        }
    }
}
