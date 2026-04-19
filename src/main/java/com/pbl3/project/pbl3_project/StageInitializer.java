package com.pbl3.project.pbl3_project;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.animation.TranslateTransition;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import javafx.stage.Popup;
import com.pbl3.project.pbl3_project.service.*;
import com.pbl3.project.pbl3_project.StageReadyEvent;
import com.pbl3.project.pbl3_project.dto.CustomerOrderAggregate;

import java.math.BigDecimal;

@Component
public class StageInitializer implements ApplicationListener<StageReadyEvent> {

    private final AuthService authService;
    private final ProductService productService;
    private final com.pbl3.project.pbl3_project.repository.CategoryRepository categoryRepository; // Keep for legacy if needed, or replace usage
    private final OrderService orderService;
    private final ReportService reportService;
    private final ToastService toastService;
    private final AuthorizationService authorizationService;
    private final UserAccountService userAccountService;
    private final AccountAuditLogService accountAuditLogService;
    private final OperationalAuditLogService operationalAuditLogService;
    private final CustomerService customerService;

    private static final javafx.animation.Interpolator SPRING_BOUNCE = new javafx.animation.Interpolator() {
        @Override
        protected double curve(double t) {
            double tension = 0.4;
            t -= 1.0;
            return t * t * ((tension + 1) * t + tension) + 1.0;
        }
    };
    private static final String PRIMARY_HEX = "#1d7df2";
    private static final String PRIMARY_HOVER_HEX = "#176fd8";
    private static final String SUCCESS_HEX = "#22c55e";
    private static final String DANGER_HEX = "#ef4444";
    private static final String PRIMARY_BAR_FILL = "-app-primary";
    private static final String SUCCESS_BAR_FILL = "-app-success";
    private static final String DANGER_BAR_FILL = "-app-danger";
    private static final String TEXT_MUTED_HEX = "#78909C";
    private static final String SURFACE_HEX = "#FFFFFF";
    private static final String BORDER_HEX = "#CFD8DC";
    private static final String SLIDING_MENU_FONT_FAMILY = "Be Vietnam Pro";
    private static final double SLIDING_MENU_FONT_SIZE = 15;
    private static final double SLIDING_MENU_HORIZONTAL_PADDING = 24;
    private static final double SLIDING_MENU_WIDTH_BUFFER = 8;
    private static final java.time.format.DateTimeFormatter DISPLAY_DATE_FORMATTER =
        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final java.time.format.DateTimeFormatter FILE_DATE_FORMATTER =
        java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final java.time.format.DateTimeFormatter DISPLAY_DATE_TIME_FORMATTER =
        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final java.time.format.DateTimeFormatter DISPLAY_DATE_TIME_SECONDS_FORMATTER =
        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final javafx.animation.Interpolator SLIDING_MENU_INTERPOLATOR =
        javafx.animation.Interpolator.SPLINE(0.22, 0.82, 0.2, 1.0);
    private static final javafx.scene.paint.Color PRIMARY_COLOR = javafx.scene.paint.Color.web(PRIMARY_HEX);
    private static final javafx.scene.paint.Color PRIMARY_HOVER_COLOR = javafx.scene.paint.Color.web(PRIMARY_HOVER_HEX);
    private static final javafx.scene.paint.Color SUCCESS_COLOR = javafx.scene.paint.Color.web(SUCCESS_HEX);
    private static final javafx.scene.paint.Color DANGER_COLOR = javafx.scene.paint.Color.web(DANGER_HEX);
    private static final javafx.scene.paint.Color TEXT_MUTED_COLOR = javafx.scene.paint.Color.web(TEXT_MUTED_HEX);
    private static final javafx.scene.paint.Color SURFACE_COLOR = javafx.scene.paint.Color.web(SURFACE_HEX);
    private static final javafx.scene.paint.Color BORDER_COLOR = javafx.scene.paint.Color.web(BORDER_HEX);
    private static final double MAIN_WINDOW_DEFAULT_WIDTH = 1360;
    private static final double MAIN_WINDOW_DEFAULT_HEIGHT = 860;
    private static final String MANUAL_SORT_HANDLER_ATTACHED_KEY = "manualSortHandlerAttached";
    private static final String SORT_HEADER_BASE_TEXT_KEY = "sortHeaderBaseText";
    private static final String SORT_HEADER_LABEL_KEY = "sortHeaderLabel";
    private static final String SORT_HEADER_TRIANGLE_KEY = "sortHeaderTriangle";
    private static final String COLUMN_REORDER_GUARD_KEY = "columnReorderGuardInstalled";
    private static final String DEFAULT_SEARCH_PROMPT = "Search";
    private static final double STANDARD_TABLE_PAGE_SPACING = 12;
    private static final Insets STANDARD_TABLE_PAGE_PADDING = new Insets(20);
    private static final Insets STANDARD_TABLE_STATUS_PADDING = new Insets(8, 0, 0, 0);
    private static final String SIDEBAR_COLLAPSE_APPLIER_KEY = "sidebarCollapseApplier";
    private static final java.util.List<String> UI_ROOT_STYLE_CLASSES = java.util.List.of(
        "ui-accent-blue",
        "ui-accent-emerald",
        "ui-accent-amber",
        "ui-density-compact",
        "ui-reduced-motion"
    );
    private final CategoryService categoryService;
    private final BrandService brandService;
    private final SupplierService supplierService;
    private final OriginService originService;
    private final UnitService unitService;
    private final ImportOrderService importOrderService;
    private final ExpenseService expenseService;
    private final PromotionService promotionService;
    private final ReceiptService receiptService;
    private final UserUiPreferencesService userUiPreferencesService;
    private final InventoryTransactionService transactionService;
    private final StocktakeService stocktakeService;
    private final javafx.beans.property.DoubleProperty currentSidebarWidth = new javafx.beans.property.SimpleDoubleProperty(220.0);
    private final java.util.Map<String, TableSortState> sessionSortStates = new java.util.HashMap<>();
    private ImportOrderPrefill pendingImportOrderPrefill;

    private enum ReportFocusTarget {
        SUMMARY,
        ACTION_CENTER,
        WHAT_CHANGED,
        REORDER,
        REVENUE,
        ORDERS,
        CANCELED_ORDERS,
        PAYMENT_METHOD_SHARE,
        TOP_SELLING,
        CATEGORY_STOCK,
        AGING_STOCK
    }

    private record ReportSectionsBundle(
        java.util.List<javafx.scene.Node> nodes,
        java.util.Map<ReportFocusTarget, javafx.scene.Node> anchors
    ) {
    }

    private record ImportOrderPrefill(Long productId, int quantity) {
    }

    private record CheckoutSelection(
        com.pbl3.project.pbl3_project.entity.PaymentMethod paymentMethod,
        boolean printReceipt,
        Long selectedOrderPromotionId
    ) {
    }

    private record SortCriterion(String uiKey, javafx.scene.control.TableColumn.SortType direction) {
    }

    private static final class TableSortState {
        private final java.util.List<SortCriterion> defaultCriteria;
        private final java.util.List<SortCriterion> criteria = new java.util.ArrayList<>();

        private TableSortState(java.util.List<SortCriterion> defaultCriteria) {
            this.defaultCriteria = copyCriteria(defaultCriteria);
            resetToDefault();
        }

        private java.util.List<SortCriterion> snapshot() {
            return copyCriteria(criteria);
        }

        private void replace(java.util.List<SortCriterion> nextCriteria) {
            criteria.clear();
            criteria.addAll(copyCriteria(nextCriteria));
        }

        private void resetToDefault() {
            replace(defaultCriteria);
        }

        private void clear() {
            criteria.clear();
        }

        private boolean isEmpty() {
            return criteria.isEmpty();
        }
    }

    public StageInitializer(AuthService authService,
                            ProductService productService,
                            com.pbl3.project.pbl3_project.repository.CategoryRepository categoryRepository,
                            OrderService orderService,
                            ReportService reportService,
                            ToastService toastService,
                            AuthorizationService authorizationService,
                            UserAccountService userAccountService,
                            AccountAuditLogService accountAuditLogService,
                            OperationalAuditLogService operationalAuditLogService,
                            CustomerService customerService,
                            CategoryService categoryService,
                            BrandService brandService,
                            SupplierService supplierService,
                            OriginService originService,
                            UnitService unitService,
                            ImportOrderService importOrderService,
                            ExpenseService expenseService,
                            PromotionService promotionService,
                            ReceiptService receiptService,
                            UserUiPreferencesService userUiPreferencesService,
                            InventoryTransactionService transactionService,
                            StocktakeService stocktakeService) {
        this.authService = authService;
        this.productService = productService;
        this.categoryRepository = categoryRepository;
        this.orderService = orderService;
        this.reportService = reportService;
        this.toastService = toastService;
        this.authorizationService = authorizationService;
        this.userAccountService = userAccountService;
        this.accountAuditLogService = accountAuditLogService;
        this.operationalAuditLogService = operationalAuditLogService;
        this.customerService = customerService;
        this.categoryService = categoryService;
        this.brandService = brandService;
        this.supplierService = supplierService;
        this.originService = originService;
        this.unitService = unitService;
        this.importOrderService = importOrderService;
        this.expenseService = expenseService;
        this.promotionService = promotionService;
        this.receiptService = receiptService;
        this.userUiPreferencesService = userUiPreferencesService;
        this.transactionService = transactionService;
        this.stocktakeService = stocktakeService;
    }

    private boolean ensureAuthorized(Runnable action) {
        try {
            action.run();
            return true;
        } catch (AuthorizationException ex) {
            toastService.showError(ex.getMessage());
            return false;
        }
    }

    private void showUserFacingError(Throwable throwable) {
        toastService.showError(resolveUserFacingMessage(throwable));
    }

    private String resolveUserFacingMessage(Throwable throwable) {
        Throwable cause = unwrapCause(throwable);
        if (cause instanceof ConcurrencyConflictException) {
            return "Data changed, reload and try again";
        }
        if (cause instanceof StaleStocktakeSessionException) {
            return "Inventory changed after session start";
        }
        if (cause instanceof UnsafeLegacyOperationException) {
            return "Legacy import cannot be canceled safely";
        }
        if (cause instanceof ValidationException || cause instanceof AuthorizationException) {
            return cause.getMessage();
        }
        return cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()
            ? cause.getMessage()
            : "Operation failed";
    }

    private Throwable unwrapCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private String formatRoleLabel(com.pbl3.project.pbl3_project.entity.Role role) {
        if (role == null) {
            return "-";
        }
        return switch (role) {
            case ADMIN -> "Admin";
            case MANAGER -> "Manager";
            case STAFF -> "Staff";
        };
    }

    private String formatExpenseCategoryLabel(com.pbl3.project.pbl3_project.entity.ExpenseCategory category) {
        return humanizeEnumToken(category != null ? category.name() : null);
    }

    private String formatPromotionScopeLabel(com.pbl3.project.pbl3_project.entity.PromotionScope scope) {
        return scope == com.pbl3.project.pbl3_project.entity.PromotionScope.PRODUCT ? "Product" : "Order";
    }

    private String formatPromotionDiscountTypeLabel(com.pbl3.project.pbl3_project.entity.PromotionDiscountType discountType) {
        return discountType == com.pbl3.project.pbl3_project.entity.PromotionDiscountType.PERCENT ? "Percent" : "Fixed Amount";
    }

    private String formatPromotionLifecycleStatusLabel(com.pbl3.project.pbl3_project.entity.PromotionLifecycleStatus status) {
        return humanizeEnumToken(status != null ? status.name() : null);
    }

    private String formatPromotionTargetLabel(com.pbl3.project.pbl3_project.entity.Promotion promotion) {
        if (promotion == null) {
            return "-";
        }
        if (promotion.getScope() == com.pbl3.project.pbl3_project.entity.PromotionScope.PRODUCT) {
            return promotion.getTargetProduct() != null ? promotion.getTargetProduct().getName() : "Specific Product";
        }
        if (promotion.getMinOrderTotal() != null) {
            return "Min order " + formatVnd(promotion.getMinOrderTotal());
        }
        return "Any order";
    }

    private String formatPromotionScheduleLabel(com.pbl3.project.pbl3_project.entity.Promotion promotion) {
        if (promotion == null) {
            return "-";
        }
        String start = formatDateTime(promotion.getStartsAt());
        String end = formatDateTime(promotion.getEndsAt());
        if (promotion.getStartsAt() == null && promotion.getEndsAt() == null) {
            return "Always active";
        }
        if (promotion.getStartsAt() == null) {
            return "Until " + end;
        }
        if (promotion.getEndsAt() == null) {
            return "From " + start;
        }
        return start + " - " + end;
    }

    private String formatPromotionDiscountLabel(com.pbl3.project.pbl3_project.entity.Promotion promotion) {
        if (promotion == null) {
            return "-";
        }
        if (promotion.getDiscountType() == com.pbl3.project.pbl3_project.entity.PromotionDiscountType.PERCENT) {
            return MoneySupport.normalize(promotion.getDiscountValue()).stripTrailingZeros().toPlainString() + "%";
        }
        return formatVnd(promotion.getDiscountValue());
    }

    private BigDecimal calculatePosCartSubtotal(
        java.util.List<com.pbl3.project.pbl3_project.dto.CreateOrderRequest.OrderItemRequest> cartItems,
        java.util.List<com.pbl3.project.pbl3_project.entity.Product> allProducts,
        java.util.Map<Long, PromotionService.ProductPricingPreview> pricingByProductId
    ) {
        if (cartItems == null || cartItems.isEmpty()) {
            return MoneySupport.ZERO;
        }
        java.util.Map<Long, com.pbl3.project.pbl3_project.entity.Product> productsById = new java.util.LinkedHashMap<>();
        if (allProducts != null) {
            for (com.pbl3.project.pbl3_project.entity.Product product : allProducts) {
                if (product != null && product.getId() != null) {
                    productsById.put(product.getId(), product);
                }
            }
        }

        BigDecimal subtotal = MoneySupport.ZERO;
        for (com.pbl3.project.pbl3_project.dto.CreateOrderRequest.OrderItemRequest item : cartItems) {
            if (item == null || item.getProductId() == null) {
                continue;
            }
            com.pbl3.project.pbl3_project.entity.Product product = productsById.get(item.getProductId());
            if (product == null) {
                continue;
            }
            PromotionService.ProductPricingPreview preview = pricingByProductId != null
                ? pricingByProductId.get(item.getProductId())
                : null;
            BigDecimal unitPrice = preview != null ? preview.discountedUnitPrice() : product.getPrice();
            subtotal = MoneySupport.add(subtotal, MoneySupport.multiply(unitPrice, item.getQuantity()));
        }
        return subtotal;
    }

    private String humanizeEnumToken(String token) {
        if (token == null || token.isBlank()) {
            return "Unknown";
        }
        String normalized = token.toLowerCase().replace('_', ' ');
        StringBuilder builder = new StringBuilder();
        for (String part : normalized.split("\\s+")) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.isEmpty() ? token : builder.toString();
    }

    private String formatUserStatus(boolean enabled) {
        return enabled ? "Active" : "Disabled";
    }

    private String formatCustomerStatus(boolean enabled) {
        return enabled ? "Active" : "Disabled";
    }

    private void syncSessionUser(com.pbl3.project.pbl3_project.entity.User sessionUser, com.pbl3.project.pbl3_project.entity.User updatedUser) {
        if (sessionUser == null || updatedUser == null || sessionUser.getId() == null || updatedUser.getId() == null) {
            return;
        }
        if (!sessionUser.getId().equals(updatedUser.getId())) {
            return;
        }
        sessionUser.setUsername(updatedUser.getUsername());
        sessionUser.setFullName(updatedUser.getFullName());
        sessionUser.setRole(updatedUser.getRole());
        sessionUser.setEnabled(updatedUser.isEnabled());
        sessionUser.setPassword(updatedUser.getPassword());
    }

    private void setCenterContentCache(javafx.scene.layout.BorderPane root, boolean enabled) {
        if (root == null) {
            return;
        }
        javafx.scene.Node center = root.getCenter();
        if (center == null) {
            return;
        }
        center.setCache(enabled);
        center.setCacheHint(enabled ? javafx.scene.CacheHint.SPEED : javafx.scene.CacheHint.DEFAULT);
    }

    private void enableScrollPerfCache(javafx.scene.Node node) {
        if (node == null) {
            return;
        }
        node.setCache(true);
        node.setCacheHint(javafx.scene.CacheHint.SPEED);
    }

    private void applyApplicationStyles(Scene scene) {
        if (scene == null) {
            return;
        }
        String stylesheet = getClass().getResource("/application.css").toExternalForm();
        if (!scene.getStylesheets().contains(stylesheet)) {
            scene.getStylesheets().add(stylesheet);
        }
    }

    private javafx.scene.layout.BorderPane resolveMainLayout(Scene scene) {
        if (scene == null) {
            return null;
        }
        javafx.scene.Parent currentRoot = scene.getRoot();
        if (currentRoot instanceof javafx.scene.layout.BorderPane borderPane
            && "MAIN_LAYOUT".equals(borderPane.getUserData())) {
            return borderPane;
        }
        if (currentRoot instanceof javafx.scene.layout.StackPane stackPane
            && !stackPane.getChildren().isEmpty()
            && stackPane.getChildren().get(0) instanceof javafx.scene.layout.BorderPane borderPane
            && "MAIN_LAYOUT".equals(borderPane.getUserData())) {
            return borderPane;
        }
        return null;
    }

    private void applyCurrentUserUiPreferences(
        Stage stage,
        com.pbl3.project.pbl3_project.entity.User user,
        boolean applySidebarPreference
    ) {
        if (stage == null || stage.getScene() == null || user == null) {
            return;
        }
        javafx.scene.layout.BorderPane root = resolveMainLayout(stage.getScene());
        if (root == null) {
            return;
        }
        applyUserUiPreferences(root, userUiPreferencesService.getPreferences(user), applySidebarPreference);
    }

    @SuppressWarnings("unchecked")
    private void applyUserUiPreferences(
        javafx.scene.layout.BorderPane root,
        com.pbl3.project.pbl3_project.entity.UserUiPreferences preferences,
        boolean applySidebarPreference
    ) {
        if (root == null || preferences == null) {
            return;
        }

        root.getStyleClass().removeAll(UI_ROOT_STYLE_CLASSES);
        root.getStyleClass().add(preferences.getAccentPreset().getRootStyleClass());

        String densityStyleClass = preferences.getDensityMode().getRootStyleClass();
        if (densityStyleClass != null && !densityStyleClass.isBlank()) {
            root.getStyleClass().add(densityStyleClass);
        }
        if (preferences.isReducedMotion()) {
            root.getStyleClass().add("ui-reduced-motion");
        }

        if (!applySidebarPreference) {
            return;
        }

        Object applier = root.getProperties().get(SIDEBAR_COLLAPSE_APPLIER_KEY);
        if (applier instanceof java.util.function.BiConsumer<?, ?> rawApplier) {
            ((java.util.function.BiConsumer<Boolean, Boolean>) rawApplier)
                .accept(preferences.isSidebarCollapsedByDefault(), !preferences.isReducedMotion());
        }
    }

    private boolean isReducedMotionEnabled(javafx.scene.Node node) {
        javafx.scene.Node current = node;
        while (current != null) {
            if (current.getStyleClass().contains("ui-reduced-motion")) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private boolean isReducedMotionEnabledForUser(com.pbl3.project.pbl3_project.entity.User user) {
        if (user == null) {
            return false;
        }
        return userUiPreferencesService.getPreferences(user).isReducedMotion();
    }

    // --- Reusable UI Components ---
    public static class RangeSlider extends javafx.scene.layout.Pane {
        public final javafx.beans.property.DoubleProperty minVal = new javafx.beans.property.SimpleDoubleProperty(0);
        public final javafx.beans.property.DoubleProperty maxVal = new javafx.beans.property.SimpleDoubleProperty(1);
        
        public RangeSlider(double minBound, double maxBound, double initialMin, double initialMax, double sliderWidth) {
            setPrefHeight(24);
            setPrefWidth(sliderWidth); // explicitly set prefWidth
            javafx.scene.shape.Rectangle bgTrack = new javafx.scene.shape.Rectangle(0, 10, sliderWidth, 4);
            bgTrack.setFill(BORDER_COLOR);
            bgTrack.setArcWidth(4); bgTrack.setArcHeight(4);
            
            javafx.scene.shape.Rectangle activeTrack = new javafx.scene.shape.Rectangle(0, 10, sliderWidth, 4);
            activeTrack.setFill(PRIMARY_COLOR);
            activeTrack.setArcWidth(4); activeTrack.setArcHeight(4);
            
            javafx.scene.shape.Circle minThumb = new javafx.scene.shape.Circle(8, SURFACE_COLOR);
            minThumb.setStroke(BORDER_COLOR); minThumb.setStrokeWidth(1);
            minThumb.setCenterY(12); minThumb.setCursor(javafx.scene.Cursor.HAND);
            minThumb.setStyle("-fx-effect: dropshadow(three-pass-box, -app-shadow, 3, 0, 0, 1);");
            
            javafx.scene.shape.Circle maxThumb = new javafx.scene.shape.Circle(8, SURFACE_COLOR);
            maxThumb.setStroke(BORDER_COLOR); maxThumb.setStrokeWidth(1);
            maxThumb.setCenterY(12); maxThumb.setCursor(javafx.scene.Cursor.HAND);
            maxThumb.setStyle("-fx-effect: dropshadow(three-pass-box, -app-shadow, 3, 0, 0, 1);");

            getChildren().addAll(bgTrack, activeTrack, minThumb, maxThumb);

            Runnable updateLayout = () -> {
                double range = maxBound - minBound;
                if (range == 0) return;
                double minX = ((minVal.get() - minBound) / range) * sliderWidth;
                double maxX = ((maxVal.get() - minBound) / range) * sliderWidth;
                minThumb.setCenterX(minX);
                maxThumb.setCenterX(maxX);
                activeTrack.setX(minX);
                activeTrack.setWidth(maxX - minX);
            };

            minVal.addListener((obs, ov, nv) -> updateLayout.run());
            maxVal.addListener((obs, ov, nv) -> updateLayout.run());

            minVal.set(initialMin); maxVal.set(initialMax);

            javafx.event.EventHandler<javafx.scene.input.MouseEvent> dragMin = e -> {
                double newX = Math.max(0, Math.min(e.getX(), maxThumb.getCenterX() - 12));
                minVal.set(minBound + (newX / sliderWidth) * (maxBound - minBound));
            };
            minThumb.setOnMouseDragged(dragMin);
            minThumb.setOnMousePressed(dragMin);

            javafx.event.EventHandler<javafx.scene.input.MouseEvent> dragMax = e -> {
                double newX = Math.max(minThumb.getCenterX() + 12, Math.min(e.getX(), sliderWidth));
                maxVal.set(minBound + (newX / sliderWidth) * (maxBound - minBound));
            };
            maxThumb.setOnMouseDragged(dragMax);
            maxThumb.setOnMousePressed(dragMax);
        }
    }

    private static final class FilterDisclosureSection {
        private final javafx.scene.layout.VBox root;
        private final javafx.scene.layout.Region contentRegion;
        private final javafx.scene.layout.StackPane contentWrapper;
        private final javafx.scene.shape.Polygon arrow;
        private final javafx.animation.Timeline animationTimeline = new javafx.animation.Timeline();
        private boolean expanded;

        private FilterDisclosureSection(javafx.scene.control.CheckBox allCheckBox, javafx.scene.Node contentNode) {
            this.contentRegion = contentNode instanceof javafx.scene.layout.Region region
                ? region
                : new javafx.scene.layout.StackPane(contentNode);
            this.arrow = new javafx.scene.shape.Polygon(0.0, 0.0, 10.0, 0.0, 5.0, 6.0);
            this.arrow.setFill(TEXT_MUTED_COLOR);

            javafx.scene.layout.StackPane arrowWrap = new javafx.scene.layout.StackPane(arrow);
            arrowWrap.setMinSize(12, 12);
            arrowWrap.setPrefSize(12, 12);
            arrowWrap.setMaxSize(12, 12);

            javafx.scene.control.Button toggleButton = new javafx.scene.control.Button();
            toggleButton.getStyleClass().setAll("filter-disclosure-button");
            toggleButton.setGraphic(arrowWrap);
            toggleButton.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
            toggleButton.setFocusTraversable(false);
            toggleButton.setOnAction(e -> setExpanded(!expanded, true));

            javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(6, allCheckBox, toggleButton);
            header.setAlignment(Pos.CENTER_LEFT);

            this.contentRegion.setMaxWidth(Double.MAX_VALUE);
            this.contentWrapper = new javafx.scene.layout.StackPane(this.contentRegion);
            this.contentWrapper.setMaxWidth(Double.MAX_VALUE);
            this.contentWrapper.setMinHeight(0);

            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
            clip.widthProperty().bind(contentWrapper.widthProperty());
            clip.heightProperty().bind(contentWrapper.heightProperty());
            this.contentWrapper.setClip(clip);

            this.root = new javafx.scene.layout.VBox(4, header, contentWrapper);
            this.root.setFillWidth(true);
            setExpanded(false, false);
        }

        private javafx.scene.Node getNode() {
            return root;
        }

        private void setExpanded(boolean expanded) {
            setExpanded(expanded, true);
        }

        private void setExpanded(boolean expanded, boolean animate) {
            this.expanded = expanded;
            double targetRotation = expanded ? 180.0 : 0.0;
            animationTimeline.stop();

            if (!animate) {
                double targetHeight = expanded ? computeExpandedHeight() : 0.0;
                contentWrapper.setManaged(expanded);
                contentWrapper.setVisible(expanded);
                contentWrapper.setOpacity(expanded ? 1.0 : 0.0);
                contentWrapper.setPrefHeight(targetHeight);
                contentWrapper.setMaxHeight(targetHeight);
                arrow.setRotate(targetRotation);
                return;
            }

            if (expanded) {
                double targetHeight = computeExpandedHeight();
                contentWrapper.setManaged(true);
                contentWrapper.setVisible(true);
                if (contentWrapper.getPrefHeight() <= 0.0) {
                    contentWrapper.setPrefHeight(0.0);
                    contentWrapper.setMaxHeight(0.0);
                    contentWrapper.setOpacity(0.0);
                }

                animationTimeline.getKeyFrames().setAll(
                    new javafx.animation.KeyFrame(
                        Duration.millis(180),
                        new javafx.animation.KeyValue(arrow.rotateProperty(), targetRotation, javafx.animation.Interpolator.EASE_BOTH),
                        new javafx.animation.KeyValue(contentWrapper.prefHeightProperty(), targetHeight, javafx.animation.Interpolator.EASE_BOTH),
                        new javafx.animation.KeyValue(contentWrapper.maxHeightProperty(), targetHeight, javafx.animation.Interpolator.EASE_BOTH),
                        new javafx.animation.KeyValue(contentWrapper.opacityProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH)
                    )
                );
                animationTimeline.setOnFinished(e -> {
                    contentWrapper.setPrefHeight(targetHeight);
                    contentWrapper.setMaxHeight(targetHeight);
                });
            } else {
                double startHeight = contentWrapper.getHeight() > 0.0 ? contentWrapper.getHeight() : computeExpandedHeight();
                contentWrapper.setManaged(true);
                contentWrapper.setVisible(true);
                contentWrapper.setPrefHeight(startHeight);
                contentWrapper.setMaxHeight(startHeight);

                animationTimeline.getKeyFrames().setAll(
                    new javafx.animation.KeyFrame(
                        Duration.millis(180),
                        new javafx.animation.KeyValue(arrow.rotateProperty(), targetRotation, javafx.animation.Interpolator.EASE_BOTH),
                        new javafx.animation.KeyValue(contentWrapper.prefHeightProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH),
                        new javafx.animation.KeyValue(contentWrapper.maxHeightProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH),
                        new javafx.animation.KeyValue(contentWrapper.opacityProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH)
                    )
                );
                animationTimeline.setOnFinished(e -> {
                    contentWrapper.setManaged(false);
                    contentWrapper.setVisible(false);
                });
            }

            animationTimeline.playFromStart();
        }

        private double computeExpandedHeight() {
            double prefHeight = contentRegion.prefHeight(-1);
            double maxHeight = contentRegion.getMaxHeight();
            if (Double.isFinite(maxHeight) && maxHeight > 0.0) {
                prefHeight = maxHeight;
            }
            return Math.max(0.0, prefHeight);
        }
    }

    /** Helper to add scrollable month/year picker to a DatePicker */
    private static void customizeDatePicker(javafx.scene.control.DatePicker dp) {
        dp.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if (!isShowing) return;
            javafx.application.Platform.runLater(() -> {
                javafx.scene.control.skin.DatePickerSkin skin = (javafx.scene.control.skin.DatePickerSkin) dp.getSkin();
                if (skin == null) return;
                javafx.scene.Node popup = skin.getPopupContent();
                if (popup == null) return;

                java.util.Set<javafx.scene.Node> spinners = popup.lookupAll(".spinner");
                int idx = 0;
                for (javafx.scene.Node spinner : spinners) {
                    if (!(spinner instanceof javafx.scene.layout.HBox hbox)) continue;
                    Label lbl = null;
                    for (javafx.scene.Node child : hbox.getChildren()) {
                        if (child instanceof Label l) { lbl = l; break; }
                    }
                    if (lbl == null) continue;

                    final boolean isMonth = (idx == 0);
                    idx++;
                    final Label clickLabel = lbl;

                    if (clickLabel.getUserData() != null && "customized".equals(clickLabel.getUserData())) continue;
                    clickLabel.setUserData("customized");

                    // Add up/down triangle arrows as graphic on the label
                    javafx.scene.shape.Polygon upArrow = new javafx.scene.shape.Polygon(0, 4, 3.5, 0, 7, 4);
                    upArrow.setFill(TEXT_MUTED_COLOR);
                    javafx.scene.shape.Polygon downArrow = new javafx.scene.shape.Polygon(0, 0, 3.5, 4, 7, 0);
                    downArrow.setFill(TEXT_MUTED_COLOR);
                    VBox arrowBox = new VBox(1, upArrow, downArrow);
                    arrowBox.setAlignment(Pos.CENTER);

                    clickLabel.setGraphic(arrowBox);
                    clickLabel.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
                    clickLabel.setCursor(javafx.scene.Cursor.HAND);

                    // Click handler for showing scrollable picker
                    javafx.event.EventHandler<javafx.scene.input.MouseEvent> showPicker = me -> {
                        javafx.stage.Popup selectorPopup = new javafx.stage.Popup();
                        selectorPopup.setAutoHide(true);

                        VBox listBox = new VBox(2);
                        listBox.setPadding(new Insets(6));
                        listBox.setStyle("-fx-background-color: -app-surface; -fx-background-radius: 8; -fx-border-color: -app-border; -fx-border-radius: 8; -fx-effect: dropshadow(three-pass-box, -app-shadow, 8, 0, 0, 3);");

                        javafx.scene.control.ScrollPane sp = new javafx.scene.control.ScrollPane(listBox);
                        sp.setFitToWidth(true);
                        sp.setStyle("-fx-background: -app-surface; -fx-background-color: transparent; -fx-border-color: transparent;");
                        sp.setPrefViewportHeight(200);
                        sp.setPrefWidth(120);

                        if (isMonth) {
                            String[] months = {"Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6",
                                "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"};
                            java.time.LocalDate current = dp.getValue() != null ? dp.getValue() : java.time.LocalDate.now();
                            for (int i = 0; i < months.length; i++) {
                                final int monthIdx = i + 1;
                                Label item = new Label(months[i]);
                                item.setPrefWidth(100);
                                item.setPadding(new Insets(6, 10, 6, 10));
                                item.setCursor(javafx.scene.Cursor.HAND);
                                boolean selected = (current.getMonthValue() == monthIdx);
                                item.setStyle(selected
                                    ? "-fx-background-color: -app-primary; -fx-text-fill: -app-surface; -fx-background-radius: 6; -fx-font-size: 13px; -fx-font-weight: bold;"
                                    : "-fx-background-color: transparent; -fx-text-fill: -app-text-primary; -fx-background-radius: 6; -fx-font-size: 13px;");
                                item.setOnMouseEntered(e -> {
                                    if (current.getMonthValue() != monthIdx) item.setStyle("-fx-background-color: -app-primary-soft; -fx-text-fill: -app-primary; -fx-background-radius: 6; -fx-font-size: 13px;");
                                });
                                item.setOnMouseExited(e -> {
                                    boolean sel = (current.getMonthValue() == monthIdx);
                                    item.setStyle(sel
                                        ? "-fx-background-color: -app-primary; -fx-text-fill: -app-surface; -fx-background-radius: 6; -fx-font-size: 13px; -fx-font-weight: bold;"
                                        : "-fx-background-color: transparent; -fx-text-fill: -app-text-primary; -fx-background-radius: 6; -fx-font-size: 13px;");
                                });
                                item.setOnMouseClicked(e -> {
                                    java.time.LocalDate cur = dp.getValue() != null ? dp.getValue() : java.time.LocalDate.now();
                                    int maxDay = java.time.YearMonth.of(cur.getYear(), monthIdx).lengthOfMonth();
                                    int day = Math.min(cur.getDayOfMonth(), maxDay);
                                    dp.setValue(java.time.LocalDate.of(cur.getYear(), monthIdx, day));
                                    selectorPopup.hide();
                                });
                                listBox.getChildren().add(item);
                            }
                        } else {
                            int currentYear = dp.getValue() != null ? dp.getValue().getYear() : java.time.LocalDate.now().getYear();
                            int startYear = currentYear - 50;
                            int endYear = currentYear + 10;
                            int scrollToIdx = 0;
                            java.util.List<Label> items = new java.util.ArrayList<>();
                            for (int y = startYear; y <= endYear; y++) {
                                final int yr = y;
                                Label item = new Label(String.valueOf(yr));
                                item.setPrefWidth(100);
                                item.setPadding(new Insets(6, 10, 6, 10));
                                item.setCursor(javafx.scene.Cursor.HAND);
                                boolean selected = (yr == currentYear);
                                item.setStyle(selected
                                    ? "-fx-background-color: -app-primary; -fx-text-fill: -app-surface; -fx-background-radius: 6; -fx-font-size: 13px; -fx-font-weight: bold;"
                                    : "-fx-background-color: transparent; -fx-text-fill: -app-text-primary; -fx-background-radius: 6; -fx-font-size: 13px;");
                                item.setOnMouseEntered(e -> {
                                    if (yr != currentYear) item.setStyle("-fx-background-color: -app-primary-soft; -fx-text-fill: -app-primary; -fx-background-radius: 6; -fx-font-size: 13px;");
                                });
                                item.setOnMouseExited(e -> {
                                    boolean sel = (yr == currentYear);
                                    item.setStyle(sel
                                        ? "-fx-background-color: -app-primary; -fx-text-fill: -app-surface; -fx-background-radius: 6; -fx-font-size: 13px; -fx-font-weight: bold;"
                                        : "-fx-background-color: transparent; -fx-text-fill: -app-text-primary; -fx-background-radius: 6; -fx-font-size: 13px;");
                                });
                                item.setOnMouseClicked(e -> {
                                    java.time.LocalDate cur = dp.getValue() != null ? dp.getValue() : java.time.LocalDate.now();
                                    int maxDay = java.time.YearMonth.of(yr, cur.getMonthValue()).lengthOfMonth();
                                    int day = Math.min(cur.getDayOfMonth(), maxDay);
                                    dp.setValue(java.time.LocalDate.of(yr, cur.getMonthValue(), day));
                                    selectorPopup.hide();
                                });
                                if (selected) scrollToIdx = y - startYear;
                                items.add(item);
                                listBox.getChildren().add(item);
                            }
                            final int scrollIdx = scrollToIdx;
                            javafx.application.Platform.runLater(() -> {
                                double total = items.size();
                                if (total > 0) sp.setVvalue(Math.max(0, (scrollIdx - 3.0) / total));
                            });
                        }

                        VBox popupContent = new VBox(sp);
                        popupContent.setStyle("-fx-background-color: -app-surface; -fx-background-radius: 8;");
                        selectorPopup.getContent().add(popupContent);

                        javafx.geometry.Bounds screenBounds = clickLabel.localToScreen(clickLabel.getBoundsInLocal());
                        selectorPopup.show(clickLabel, screenBounds.getMinX(), screenBounds.getMaxY() + 2);
                    };

                    clickLabel.setOnMouseClicked(showPicker);
                }
            });
        });
    }


    private void showDashboardScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        if (!ensureAuthorized(() -> authorizationService.requireProductsAccess(user))) {
            return;
        }
        javafx.scene.Node content = createProductView(stage, user);
        switchScene(stage, user, "Products", "nav-products", content);
    }

    private void showImportOrderScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        if (!ensureAuthorized(() -> authorizationService.requireImportGoodsAccess(user))) {
            return;
        }
        javafx.scene.Node content = createImportOrderView(stage, user);
        switchScene(stage, user, "Import Goods", "nav-import", content);
    }

    private void showImportOrderScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user, ImportOrderPrefill prefill) {
        pendingImportOrderPrefill = prefill;
        showImportOrderScene(stage, user);
    }

    private void showOrderHistoryScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        VBox content = createOrderHistoryView(stage, user);
        switchScene(stage, user, "Order History", "nav-history", content);
    }

    private void showReturnsRefundsScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        if (!ensureAuthorized(() -> authorizationService.requireReturnsRefundsAccess(user))) {
            return;
        }
        VBox content = createReturnsRefundsView(stage, user);
        switchScene(stage, user, "Returns / Refunds", "nav-returns", content);
    }

    private void showExpensesScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        showExpensesScene(stage, user, null, null);
    }

    private void showExpensesScene(
        Stage stage,
        com.pbl3.project.pbl3_project.entity.User user,
        java.time.LocalDate initialStartDate,
        java.time.LocalDate initialEndDate
    ) {
        if (!ensureAuthorized(() -> authorizationService.requireExpensesAccess(user))) {
            return;
        }
        VBox content = createExpensesView(stage, user, initialStartDate, initialEndDate);
        switchScene(stage, user, "Expenses", "nav-expenses", content);
    }

    private void showCustomersScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        if (!ensureAuthorized(() -> authorizationService.requireCustomersAccess(user))) {
            return;
        }
        VBox content = createCustomersView(stage, user);
        switchScene(stage, user, "Customers", "nav-customers", content);
    }
    
    private void showSalesScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        javafx.scene.Node content = createSalesView(stage, user);
        switchScene(stage, user, "Sales (POS)", "nav-sales", content);
    }

    private void showPromotionsScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        if (!ensureAuthorized(() -> authorizationService.requirePromotionsAccess(user))) {
            return;
        }
        VBox content = createPromotionsView(stage, user);
        switchScene(stage, user, "Promotions", "nav-promotions", content);
    }

    private void showAttributesScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        if (!ensureAuthorized(() -> authorizationService.requireMasterDataAccess(user))) {
            return;
        }
        javafx.scene.Node content = createAttributesView(stage, user);
        switchScene(stage, user, "Master Data", "nav-attributes", content);
    }

    private void showOverviewScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        VBox content = createOverviewView(stage, user);
        switchScene(stage, user, "Dashboard", "nav-dashboard", content);
    }

    private void showPostLoginLoadingScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        javafx.scene.control.ProgressIndicator spinner = new javafx.scene.control.ProgressIndicator();
        spinner.setPrefSize(64, 64);

        Label titleLabel = new Label("Signing In");
        titleLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: 700; -fx-text-fill: -app-text-primary;");

        Label subtitleLabel = new Label("Preparing your workspace...");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -app-text-secondary;");

        VBox loadingContent = new VBox(16, spinner, titleLabel, subtitleLabel);
        loadingContent.setAlignment(Pos.CENTER);
        loadingContent.setPadding(new Insets(36));

        javafx.scene.layout.StackPane root = new javafx.scene.layout.StackPane(loadingContent);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: -app-surface-muted;");

        Scene scene = new Scene(root, MAIN_WINDOW_DEFAULT_WIDTH, MAIN_WINDOW_DEFAULT_HEIGHT);
        applyApplicationStyles(scene);
        stage.setScene(scene);
        stage.setWidth(MAIN_WINDOW_DEFAULT_WIDTH);
        stage.setHeight(MAIN_WINDOW_DEFAULT_HEIGHT);
        stage.centerOnScreen();

        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(Duration.millis(180));
        delay.setOnFinished(e -> showOverviewScene(stage, user));
        delay.play();
    }

    private void showOperationalReportsScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        if (!ensureAuthorized(() -> authorizationService.requireReportsAccess(user))) {
            return;
        }
        VBox content = createOperationalReportsView(stage, user, null, null, null);
        switchScene(stage, user, "Operational Reports", "nav-reports", content);
    }

    private void showOperationalReportsScene(
        Stage stage,
        com.pbl3.project.pbl3_project.entity.User user,
        java.time.LocalDate initialStartDate,
        java.time.LocalDate initialEndDate,
        ReportFocusTarget initialFocusTarget
    ) {
        if (!ensureAuthorized(() -> authorizationService.requireReportsAccess(user))) {
            return;
        }
        VBox content = createOperationalReportsView(stage, user, initialStartDate, initialEndDate, initialFocusTarget);
        switchScene(stage, user, "Operational Reports", "nav-reports", content, true);
    }

    private void showAccountsScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        if (!ensureAuthorized(() -> authorizationService.requireAccountsAccess(user))) {
            return;
        }
        VBox content = createAccountsView(stage, user);
        switchScene(stage, user, "Account Management", "nav-accounts", content);
    }

    private void showSettingsScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        if (!ensureAuthorized(() -> authorizationService.requireSettingsAccess(user))) {
            return;
        }
        VBox content = createSettingsView(stage, user);
        switchScene(stage, user, "Settings", "nav-settings", content);
    }

    private void showMyAccountScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        VBox content = createMyAccountView(stage, user);
        switchScene(stage, user, "My Account", null, content);
    }

    // --- View Creators (Extracted to keep code clean) ---

    private VBox createSettingsView(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        if (!ensureAuthorized(() -> authorizationService.requireSettingsAccess(user))) {
            return new VBox();
        }

        com.pbl3.project.pbl3_project.entity.UserUiPreferences preferences = userUiPreferencesService.getPreferences(user);

        javafx.scene.control.ComboBox<com.pbl3.project.pbl3_project.entity.UiAccentPreset> accentCombo = new javafx.scene.control.ComboBox<>();
        accentCombo.getItems().addAll(com.pbl3.project.pbl3_project.entity.UiAccentPreset.values());
        accentCombo.setValue(preferences.getAccentPreset());
        accentCombo.setConverter(createUiAccentPresetConverter());
        accentCombo.setButtonCell(createUiAccentPresetListCell());
        accentCombo.setCellFactory(list -> createUiAccentPresetListCell());
        accentCombo.setMaxWidth(Double.MAX_VALUE);

        javafx.scene.control.ComboBox<com.pbl3.project.pbl3_project.entity.UiDensityMode> densityCombo = new javafx.scene.control.ComboBox<>();
        densityCombo.getItems().addAll(com.pbl3.project.pbl3_project.entity.UiDensityMode.values());
        densityCombo.setValue(preferences.getDensityMode());
        densityCombo.setConverter(createUiDensityModeConverter());
        densityCombo.setButtonCell(createUiDensityModeListCell());
        densityCombo.setCellFactory(list -> createUiDensityModeListCell());
        densityCombo.setMaxWidth(Double.MAX_VALUE);

        javafx.scene.control.CheckBox reducedMotionCheck = new javafx.scene.control.CheckBox("Reduced motion");
        reducedMotionCheck.setSelected(preferences.isReducedMotion());
        reducedMotionCheck.setStyle("-fx-font-size: 14px; -fx-text-fill: -app-text-primary;");

        javafx.scene.control.CheckBox collapseSidebarCheck = new javafx.scene.control.CheckBox("Collapse sidebar by default");
        collapseSidebarCheck.setSelected(preferences.isSidebarCollapsedByDefault());
        collapseSidebarCheck.setStyle("-fx-font-size: 14px; -fx-text-fill: -app-text-primary;");

        javafx.scene.layout.GridPane appearanceForm = new javafx.scene.layout.GridPane();
        appearanceForm.setHgap(16);
        appearanceForm.setVgap(12);
        javafx.scene.layout.ColumnConstraints appearanceLabelColumn = new javafx.scene.layout.ColumnConstraints();
        appearanceLabelColumn.setMinWidth(160);
        appearanceLabelColumn.setPrefWidth(160);
        javafx.scene.layout.ColumnConstraints appearanceFieldColumn = new javafx.scene.layout.ColumnConstraints();
        appearanceFieldColumn.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        appearanceFieldColumn.setFillWidth(true);
        appearanceForm.getColumnConstraints().addAll(appearanceLabelColumn, appearanceFieldColumn);
        appearanceForm.add(createFormLabel("Accent Preset"), 0, 0);
        appearanceForm.add(accentCombo, 1, 0);
        appearanceForm.add(createFormLabel("Density Mode"), 0, 1);
        appearanceForm.add(densityCombo, 1, 1);
        appearanceForm.add(createFormLabel("Motion"), 0, 2);
        appearanceForm.add(reducedMotionCheck, 1, 2);

        javafx.scene.layout.GridPane layoutForm = new javafx.scene.layout.GridPane();
        layoutForm.setHgap(16);
        layoutForm.setVgap(12);
        javafx.scene.layout.ColumnConstraints layoutLabelColumn = new javafx.scene.layout.ColumnConstraints();
        layoutLabelColumn.setMinWidth(160);
        layoutLabelColumn.setPrefWidth(160);
        javafx.scene.layout.ColumnConstraints layoutFieldColumn = new javafx.scene.layout.ColumnConstraints();
        layoutFieldColumn.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        layoutFieldColumn.setFillWidth(true);
        layoutForm.getColumnConstraints().addAll(layoutLabelColumn, layoutFieldColumn);
        layoutForm.add(createFormLabel("Sidebar"), 0, 0);
        layoutForm.add(collapseSidebarCheck, 1, 0);

        java.util.List<com.pbl3.project.pbl3_project.entity.DashboardSectionKey> dashboardOrder =
            new java.util.ArrayList<>(userUiPreferencesService.resolveDashboardSectionOrder(user));
        java.util.Set<com.pbl3.project.pbl3_project.entity.DashboardSectionKey> hiddenSections =
            new java.util.LinkedHashSet<>(userUiPreferencesService.resolveHiddenDashboardSections(user));

        VBox dashboardRows = new VBox(10);
        dashboardRows.setFillWidth(true);
        Runnable[] renderDashboardRowsRef = new Runnable[1];
        renderDashboardRowsRef[0] = () -> {
            dashboardRows.getChildren().clear();
            for (int index = 0; index < dashboardOrder.size(); index++) {
                com.pbl3.project.pbl3_project.entity.DashboardSectionKey sectionKey = dashboardOrder.get(index);
                boolean visible = !hiddenSections.contains(sectionKey);

                Label label = new Label(sectionKey.getLabel());
                label.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: -app-text-primary;");

                javafx.scene.control.CheckBox visibleCheck = new javafx.scene.control.CheckBox("Visible");
                visibleCheck.setSelected(visible);
                visibleCheck.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                visibleCheck.setOnAction(e -> {
                    if (visibleCheck.isSelected()) {
                        hiddenSections.remove(sectionKey);
                    } else {
                        hiddenSections.add(sectionKey);
                    }
                });

                Button moveUpButton = createPageNavButton("Move Up");
                moveUpButton.setDisable(index == 0);
                final int currentIndex = index;
                moveUpButton.setOnAction(e -> {
                    java.util.Collections.swap(dashboardOrder, currentIndex, currentIndex - 1);
                    renderDashboardRowsRef[0].run();
                });

                Button moveDownButton = createPageNavButton("Move Down");
                moveDownButton.setDisable(index == dashboardOrder.size() - 1);
                moveDownButton.setOnAction(e -> {
                    java.util.Collections.swap(dashboardOrder, currentIndex, currentIndex + 1);
                    renderDashboardRowsRef[0].run();
                });

                javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

                javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(10, label, spacer, visibleCheck, moveUpButton, moveDownButton);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle(
                    "-fx-background-color: -app-surface; " +
                    "-fx-background-radius: 14; " +
                    "-fx-border-color: -app-border; " +
                    "-fx-border-radius: 14; " +
                    "-fx-border-width: 1; " +
                    "-fx-padding: 12 14;"
                );
                dashboardRows.getChildren().add(row);
            }
        };
        renderDashboardRowsRef[0].run();

        Button resetDashboardButton = createPageNavButton("Reset to Default");
        resetDashboardButton.setOnAction(e -> {
            dashboardOrder.clear();
            dashboardOrder.addAll(userUiPreferencesService.getDefaultDashboardOrder());
            hiddenSections.clear();
            renderDashboardRowsRef[0].run();
        });

        VBox dashboardContent = new VBox(14, dashboardRows, resetDashboardButton);
        dashboardContent.setFillWidth(true);

        VBox appearanceSection = createReportSection(
            "Appearance",
            "Choose the accent, density and motion behavior for your own session.",
            new VBox(14, appearanceForm),
            null,
            null
        );
        VBox layoutSection = createReportSection(
            "Layout",
            "Set how the main workspace opens for your account.",
            new VBox(14, layoutForm),
            null,
            null
        );
        VBox dashboardSection = createReportSection(
            "Dashboard",
            "Show, hide and reorder the Dashboard sections shown for your account.",
            dashboardContent,
            null,
            null
        );

        Button saveButton = new Button("Save");
        saveButton.getStyleClass().addAll("button", "primary-button");
        saveButton.setOnAction(event -> {
            try {
                userUiPreferencesService.updatePreferences(
                    user,
                    user,
                    accentCombo.getValue(),
                    densityCombo.getValue(),
                    reducedMotionCheck.isSelected(),
                    collapseSidebarCheck.isSelected(),
                    dashboardOrder,
                    hiddenSections
                );
                applyCurrentUserUiPreferences(stage, user, true);
                toastService.showSuccess("Settings saved");
            } catch (RuntimeException ex) {
                showUserFacingError(ex);
            }
        });

        javafx.scene.layout.HBox actionRow = new javafx.scene.layout.HBox(saveButton);
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        VBox pageContent = new VBox(20, appearanceSection, layoutSection, dashboardSection, actionRow);
        pageContent.getStyleClass().add("reports-page");
        pageContent.setStyle("-fx-background-color: -app-surface-muted;");
        pageContent.setPadding(new Insets(20));
        pageContent.setFillWidth(true);

        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(pageContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        VBox root = new VBox(scrollPane);
        root.setFillWidth(true);
        root.setStyle("-fx-background-color: -app-surface-muted;");
        VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);
        return root;
    }

    private javafx.scene.Node createAttributesView(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(12, 20, 8, 20));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: -app-surface-muted;");

        javafx.scene.layout.StackPane contentArea = new javafx.scene.layout.StackPane();
        
        // 1. Categories
        VBox catView = createSimpleMasterDataView(stage, "Categories", "master-categories",
            categoryService::searchCategories,
            name -> categoryService.saveCategory(new com.pbl3.project.pbl3_project.entity.Category(null, name)),
            categoryService::deleteCategory
        );

        // 2. Brands
        VBox brandView = createSimpleMasterDataView(stage, "Brands", "master-brands",
            brandService::searchBrands,
            name -> brandService.saveBrand(new com.pbl3.project.pbl3_project.entity.Brand(name)),
            brandService::deleteBrand
        );

        // 3. Suppliers
        VBox supplierView = createSupplierMasterDataView(stage);

        // 4. Origins
        VBox originView = createSimpleMasterDataView(stage, "Origins", "master-origins",
            originService::searchOrigins,
            name -> originService.saveOrigin(new com.pbl3.project.pbl3_project.entity.Origin(name)),
            originService::deleteOrigin
        );

        // 5. Units
        VBox unitView = createSimpleMasterDataView(stage, "Units", "master-units",
            unitService::searchUnits,
            name -> unitService.saveUnit(new com.pbl3.project.pbl3_project.entity.Unit(name)),
            unitService::deleteUnit
        );
        javafx.scene.control.TableView<?> catTable = findFirstTableView(catView);
        javafx.scene.control.TableView<?> brandTable = findFirstTableView(brandView);
        javafx.scene.control.TableView<?> supplierTable = findFirstTableView(supplierView);
        javafx.scene.control.TableView<?> originTable = findFirstTableView(originView);
        javafx.scene.control.TableView<?> unitTable = findFirstTableView(unitView);

        VBox[] views = {catView, brandView, supplierView, originView, unitView};
        for (VBox v : views) {
            v.setVisible(false);
            v.setManaged(false);
            contentArea.getChildren().add(v);
        }
        
        // Initial state
        views[0].setVisible(true);
        views[0].setManaged(true);

        String[] tabNames = {"Categories", "Brands", "Suppliers", "Origins", "Units"};
        javafx.scene.Node slidingMenu = createSlidingMenu(tabNames, index -> {
            for (int i = 0; i < views.length; i++) {
                views[i].setVisible(i == index);
                views[i].setManaged(i == index);
            }
        });

        root.getChildren().addAll(slidingMenu, contentArea);
        VBox.setVgrow(contentArea, javafx.scene.layout.Priority.ALWAYS);
        enableDeselectOnOutsideClick(root, catTable, brandTable, supplierTable, originTable, unitTable);
        
        return root;
    }

    // Helper for simple ID/Name entities (Brand, Origin, Unit, Category)
    private <T> VBox createSimpleMasterDataView(Stage stage, String title, String sortStateKey,
                                                java.util.function.BiFunction<String, org.springframework.data.domain.Pageable, org.springframework.data.domain.Page<T>> pageFetcher,
                                                java.util.function.Consumer<String> saver,
                                                java.util.function.Consumer<Long> deleter) {
        VBox root = new VBox();
        applyStandardTablePageLayout(root);
        TableSortState sortState = getOrCreateTableSortState(
            sortStateKey,
            new SortCriterion("name", javafx.scene.control.TableColumn.SortType.ASCENDING)
        );
        java.util.LinkedHashMap<String, String> sortProperties = new java.util.LinkedHashMap<>();
        sortProperties.put("name", "name");
        java.util.LinkedHashMap<String, String> sortLabels = new java.util.LinkedHashMap<>();
        sortLabels.put("name", "Name");
        
        Label header = new Label(title + " Management");
        header.getStyleClass().add("header-label");

        // Expandable Search Bar
        javafx.scene.layout.HBox searchBox = new javafx.scene.layout.HBox(0);
        searchBox.setAlignment(Pos.CENTER);
        searchBox.getStyleClass().add("expandable-search-box");
        searchBox.setPrefSize(40, 40);
        searchBox.setMinSize(40, 40);
        searchBox.setMaxSize(40, 40);

        javafx.scene.shape.SVGPath sIcon = new javafx.scene.shape.SVGPath();
        sIcon.setContent("M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
        sIcon.setFill(PRIMARY_COLOR);

        javafx.scene.layout.Region sSpacer = new javafx.scene.layout.Region();
        sSpacer.setMinWidth(0); sSpacer.setPrefWidth(0);

        TextField sField = new TextField();
        sField.setPromptText(DEFAULT_SEARCH_PROMPT);
        sField.getStyleClass().add("search-text-field");
        sField.setMinWidth(0); sField.setMaxWidth(0); sField.setPrefWidth(0); sField.setOpacity(0);

        searchBox.getChildren().addAll(sIcon, sSpacer, sField);

        javafx.animation.Timeline sExpand = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.millis(150),
                new javafx.animation.KeyValue(searchBox.maxWidthProperty(), 250, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(searchBox.prefWidthProperty(), 250, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(sSpacer.minWidthProperty(), 8, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(sField.minWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(sField.maxWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(sField.prefWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(sField.opacityProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH)
            )
        );
        javafx.animation.Timeline sCollapse = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.millis(150),
                new javafx.animation.KeyValue(searchBox.maxWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(searchBox.prefWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(sSpacer.minWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(sField.minWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(sField.maxWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(sField.prefWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(sField.opacityProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH)
            )
        );
        javafx.scene.control.TableView<T> table = new javafx.scene.control.TableView<>();
        applyStandardTableSizing(table);
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        enableDragSelection(table);

        final int pageSize = 15;
        final int[] currentPage = {0};
        final int[] totalPages = {0};
        final long[] totalElements = {0L};
        java.util.concurrent.atomic.AtomicReference<String> searchRef = new java.util.concurrent.atomic.AtomicReference<>("");

        Label rowCountLabel = createStatusMetaLabel("Showing 0-0 of 0 Row(s)");
        Label pageLabel = createStatusMetaLabel("Page 0 / 0");
        Button prevBtn = createPageNavButton("Prev");
        Button nextBtn = createPageNavButton("Next");

        Runnable updateStatusBar = () -> updatePagedStatus(
            table,
            rowCountLabel,
            pageLabel,
            prevBtn,
            nextBtn,
            totalElements[0],
            currentPage[0],
            totalPages[0],
            pageSize
        );
        Runnable refreshPage = () -> {
            org.springframework.data.domain.Page<T> pageData = pageFetcher.apply(
                searchRef.get(),
                createPageable(sortState, sortProperties, currentPage[0], pageSize)
            );
            if (pageData.getTotalPages() > 0 && currentPage[0] >= pageData.getTotalPages()) {
                currentPage[0] = pageData.getTotalPages() - 1;
                pageData = pageFetcher.apply(
                    searchRef.get(),
                    createPageable(sortState, sortProperties, currentPage[0], pageSize)
                );
            }
            totalElements[0] = pageData.getTotalElements();
            totalPages[0] = pageData.getTotalPages();
            table.setItems(javafx.collections.FXCollections.observableArrayList(pageData.getContent()));
            updateStatusBar.run();
        };
        prevBtn.setOnAction(e -> {
            if (currentPage[0] > 0) {
                currentPage[0]--;
                refreshPage.run();
            }
        });
        nextBtn.setOnAction(e -> {
            if (currentPage[0] + 1 < totalPages[0]) {
                currentPage[0]++;
                refreshPage.run();
            }
        });
        
        searchBox.setOnMouseClicked(ev -> {
            if (searchBox.getMaxWidth() == 40) { sExpand.play(); sField.requestFocus(); }
            else if (ev.getTarget() == sIcon || ev.getTarget() == searchBox) { sField.clear(); root.requestFocus(); sCollapse.play(); }
        });

        javafx.scene.layout.BorderPane topBar = new javafx.scene.layout.BorderPane();
        topBar.setLeft(header);
        topBar.setRight(searchBox);
        javafx.scene.layout.BorderPane.setAlignment(header, Pos.CENTER_LEFT);

        javafx.animation.PauseTransition searchPause = new javafx.animation.PauseTransition(Duration.millis(220));
        searchPause.setOnFinished(e -> {
            currentPage[0] = 0;
            searchRef.set(sField.getText());
            refreshPage.run();
        });
        sField.textProperty().addListener((obs, oldV, newV) -> searchPause.playFromStart());
        
        javafx.scene.control.TableColumn<T, Integer> sttCol = new javafx.scene.control.TableColumn<>("STT");
        sttCol.setSortable(false);
        sttCol.setCellValueFactory(column -> new javafx.beans.property.ReadOnlyObjectWrapper<>(
            currentPage[0] * pageSize + table.getItems().indexOf(column.getValue()) + 1));
        
        javafx.scene.control.TableColumn<T, String> nameCol = new javafx.scene.control.TableColumn<>("Name");
        nameCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("name"));
        
        Runnable deleteAction = () -> {
            java.util.List<T> selectedItems = new java.util.ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selectedItems.isEmpty()) return;
            
            if (!showConfirmDialog("Confirm Deletion", "Are you sure you want to delete " + selectedItems.size() + " selected item(s)?")) {
                return;
            }
            
            int deletedCount = 0;
            for (T item : selectedItems) {
                try {
                    java.lang.reflect.Method getId = item.getClass().getMethod("getId");
                    Long id = (Long) getId.invoke(item);
                    deleter.accept(id);
                    deletedCount++;
                } catch (Exception ex) {
                     toastService.showError("Error deleting item: " + ex.getMessage());
                }
            }
            if (deletedCount > 0) {
                toastService.showSuccess("Deleted " + deletedCount + " items!");
                refreshPage.run();
            }
        };

        table.setOnKeyPressed(event -> {
            javafx.scene.input.KeyCode code = event.getCode();
            if (code == javafx.scene.input.KeyCode.DELETE || code == javafx.scene.input.KeyCode.BACK_SPACE || code == javafx.scene.input.KeyCode.ENTER) {
                deleteAction.run();
            }
        });

        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<T> row = new javafx.scene.control.TableRow<>();
            javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();
            
            javafx.scene.control.MenuItem deleteItem = new javafx.scene.control.MenuItem("Delete");
            deleteItem.setStyle("-fx-text-fill: -app-danger;");
            deleteItem.setOnAction(event -> {
                if (table.getSelectionModel().getSelectedItems().isEmpty() && row.getItem() != null) {
                    table.getSelectionModel().select(row.getItem());
                }
                deleteAction.run();
            });
            
            contextMenu.getItems().add(deleteItem);
            
            row.contextMenuProperty().bind(
                javafx.beans.binding.Bindings.when(row.emptyProperty())
                .then((javafx.scene.control.ContextMenu)null)
                .otherwise(contextMenu)
            );
            return row;
        });
        
        table.getColumns().addAll(sttCol, nameCol);
        java.util.LinkedHashMap<String, javafx.scene.control.TableColumn<T, ?>> sortColumns = new java.util.LinkedHashMap<>();
        sortColumns.put("name", nameCol);
        installSortHeaderIndicators(sortColumns);
        
        // Add Form
        javafx.scene.layout.HBox addBox = new javafx.scene.layout.HBox(10);
        addBox.setAlignment(Pos.CENTER_LEFT);
        TextField nameField = new TextField();
        nameField.setPromptText("Enter " + title + " Name...");
        Button addBtn = createExpandableGreenActionButton("Add", 100);
        addBtn.setOnAction(e -> {
            if (nameField.getText().isEmpty()) return;
            try {
                saver.accept(nameField.getText());
                toastService.showSuccess("Added " + nameField.getText());
                nameField.clear();
                currentPage[0] = 0;
                refreshPage.run();
            } catch (Exception ex) {
                toastService.showError("Error: " + ex.getMessage());
            }
        });
        
        addBox.getChildren().addAll(nameField, addBtn);

        Label sortStatusLabel = createSortStatusLabel(sortState, sortLabels);
        Runnable applySortUi = () -> {
            applySortStateToTable(table, sortColumns, sortState);
            sortStatusLabel.setText(buildSortStatusText(sortState, sortLabels));
        };

        applySortUi.run();
        installManualServerSorting(
            table,
            sortColumns,
            sortState,
            () -> {
                applySortUi.run();
                currentPage[0] = 0;
                refreshPage.run();
            }
        );

        javafx.scene.layout.HBox statusBar = new javafx.scene.layout.HBox(15, sortStatusLabel, rowCountLabel, pageLabel, prevBtn, nextBtn);
        applyStandardTableStatusBar(statusBar);

        root.getChildren().addAll(topBar, addBox, table, statusBar);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        table.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<T>) c -> updateStatusBar.run());
        refreshPage.run();
        enableDeselectOnOutsideClick(root, table);
        return root;
    }

    private VBox createSupplierMasterDataView(Stage stage) {
        VBox root = new VBox();
        applyStandardTablePageLayout(root);
        final String supplierSortStateKey = "master-suppliers";
        TableSortState supplierSortState = getOrCreateTableSortState(
            supplierSortStateKey,
            new SortCriterion("name", javafx.scene.control.TableColumn.SortType.ASCENDING)
        );
        java.util.LinkedHashMap<String, String> supplierSortProperties = new java.util.LinkedHashMap<>();
        supplierSortProperties.put("name", "name");
        supplierSortProperties.put("phone", "phone");
        java.util.LinkedHashMap<String, String> supplierSortLabels = new java.util.LinkedHashMap<>();
        supplierSortLabels.put("name", "Name");
        supplierSortLabels.put("phone", "Phone");
        
        Label header = new Label("Supplier Management");
        header.getStyleClass().add("header-label");

        // Expandable Search Bar
        javafx.scene.layout.HBox searchBox2 = new javafx.scene.layout.HBox(0);
        searchBox2.setAlignment(Pos.CENTER);
        searchBox2.getStyleClass().add("expandable-search-box");
        searchBox2.setPrefSize(40, 40); searchBox2.setMinSize(40, 40); searchBox2.setMaxSize(40, 40);
        javafx.scene.shape.SVGPath sIcon2 = new javafx.scene.shape.SVGPath();
        sIcon2.setContent("M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
        sIcon2.setFill(PRIMARY_COLOR);
        javafx.scene.layout.Region sSpacer2 = new javafx.scene.layout.Region();
        sSpacer2.setMinWidth(0); sSpacer2.setPrefWidth(0);
        TextField sField2 = new TextField();
        sField2.setPromptText(DEFAULT_SEARCH_PROMPT); sField2.getStyleClass().add("search-text-field");
        sField2.setMinWidth(0); sField2.setMaxWidth(0); sField2.setPrefWidth(0); sField2.setOpacity(0);
        searchBox2.getChildren().addAll(sIcon2, sSpacer2, sField2);
        javafx.animation.Timeline sExpand2 = new javafx.animation.Timeline(new javafx.animation.KeyFrame(Duration.millis(150),
            new javafx.animation.KeyValue(searchBox2.maxWidthProperty(), 250, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(searchBox2.prefWidthProperty(), 250, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(sSpacer2.minWidthProperty(), 8, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(sField2.minWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(sField2.maxWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(sField2.prefWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(sField2.opacityProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH)));
        javafx.animation.Timeline sCollapse2 = new javafx.animation.Timeline(new javafx.animation.KeyFrame(Duration.millis(150),
            new javafx.animation.KeyValue(searchBox2.maxWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(searchBox2.prefWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(sSpacer2.minWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(sField2.minWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(sField2.maxWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(sField2.prefWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(sField2.opacityProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH)));
        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.Supplier> table = new javafx.scene.control.TableView<>();
        applyStandardTableSizing(table);
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        enableDragSelection(table);
        final int supplierPageSize = 15;
        final int[] supplierCurrentPage = {0};
        final int[] supplierTotalPages = {0};
        final long[] supplierTotalElements = {0L};
        java.util.concurrent.atomic.AtomicReference<String> supplierSearchRef = new java.util.concurrent.atomic.AtomicReference<>("");

        Label supplierRowCountLabel = createStatusMetaLabel("Showing 0-0 of 0 Row(s)");
        Label supplierPageLabel = createStatusMetaLabel("Page 0 / 0");
        Button supplierPrevBtn = createPageNavButton("Prev");
        Button supplierNextBtn = createPageNavButton("Next");

        Runnable updateSupplierStatusBar = () -> updatePagedStatus(
            table,
            supplierRowCountLabel,
            supplierPageLabel,
            supplierPrevBtn,
            supplierNextBtn,
            supplierTotalElements[0],
            supplierCurrentPage[0],
            supplierTotalPages[0],
            supplierPageSize
        );
        Runnable refreshSupplierPage = () -> {
            org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.Supplier> pageData =
                supplierService.searchSuppliers(
                    supplierSearchRef.get(),
                    createPageable(supplierSortState, supplierSortProperties, supplierCurrentPage[0], supplierPageSize)
                );
            if (pageData.getTotalPages() > 0 && supplierCurrentPage[0] >= pageData.getTotalPages()) {
                supplierCurrentPage[0] = pageData.getTotalPages() - 1;
                pageData = supplierService.searchSuppliers(
                    supplierSearchRef.get(),
                    createPageable(supplierSortState, supplierSortProperties, supplierCurrentPage[0], supplierPageSize)
                );
            }
            supplierTotalElements[0] = pageData.getTotalElements();
            supplierTotalPages[0] = pageData.getTotalPages();
            table.setItems(javafx.collections.FXCollections.observableArrayList(pageData.getContent()));
            updateSupplierStatusBar.run();
        };
        supplierPrevBtn.setOnAction(e -> {
            if (supplierCurrentPage[0] > 0) {
                supplierCurrentPage[0]--;
                refreshSupplierPage.run();
            }
        });
        supplierNextBtn.setOnAction(e -> {
            if (supplierCurrentPage[0] + 1 < supplierTotalPages[0]) {
                supplierCurrentPage[0]++;
                refreshSupplierPage.run();
            }
        });

        searchBox2.setOnMouseClicked(ev -> {
            if (searchBox2.getMaxWidth() == 40) { sExpand2.play(); sField2.requestFocus(); }
            else if (ev.getTarget() == sIcon2 || ev.getTarget() == searchBox2) { sField2.clear(); root.requestFocus(); sCollapse2.play(); }
        });
        javafx.scene.layout.BorderPane topBar2 = new javafx.scene.layout.BorderPane();
        topBar2.setLeft(header); topBar2.setRight(searchBox2);
        javafx.scene.layout.BorderPane.setAlignment(header, Pos.CENTER_LEFT);
        
        javafx.animation.PauseTransition supplierSearchPause = new javafx.animation.PauseTransition(Duration.millis(220));
        supplierSearchPause.setOnFinished(e -> {
            supplierCurrentPage[0] = 0;
            supplierSearchRef.set(sField2.getText());
            refreshSupplierPage.run();
        });
        sField2.textProperty().addListener((obs, oldV, newV) -> supplierSearchPause.playFromStart());

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Supplier, Integer> sttCol = new javafx.scene.control.TableColumn<>("STT");
        sttCol.setSortable(false);
        sttCol.setCellValueFactory(column -> new javafx.beans.property.ReadOnlyObjectWrapper<>(
            supplierCurrentPage[0] * supplierPageSize + table.getItems().indexOf(column.getValue()) + 1));
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Supplier, String> nameCol = new javafx.scene.control.TableColumn<>("Name");
        nameCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("name"));
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Supplier, String> phoneCol = new javafx.scene.control.TableColumn<>("Phone");
        phoneCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("phone"));
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Supplier, String> addrCol = new javafx.scene.control.TableColumn<>("Address");
        addrCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("address"));
        
        Runnable deleteAction = () -> {
            java.util.List<com.pbl3.project.pbl3_project.entity.Supplier> selectedItems = new java.util.ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selectedItems.isEmpty()) return;

            if (!showConfirmDialog("Confirm Deletion", "Are you sure you want to delete " + selectedItems.size() + " selected supplier(s)?")) {
                return;
            }

            int deletedCount = 0;
            for (com.pbl3.project.pbl3_project.entity.Supplier item : selectedItems) {
                try {
                     supplierService.deleteSupplier(item.getId());
                     deletedCount++;
                } catch (Exception ex) {
                     toastService.showError("Error deleting supplier: " + ex.getMessage());
                }
            }
            
            if (deletedCount > 0) {
                 toastService.showSuccess("Deleted " + deletedCount + " suppliers!");
                 refreshSupplierPage.run();
            }
        };

        table.setOnKeyPressed(event -> {
            javafx.scene.input.KeyCode code = event.getCode();
            if (code == javafx.scene.input.KeyCode.DELETE || code == javafx.scene.input.KeyCode.BACK_SPACE || code == javafx.scene.input.KeyCode.ENTER) {
                deleteAction.run();
            }
        });

        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<com.pbl3.project.pbl3_project.entity.Supplier> row = new javafx.scene.control.TableRow<>();
            javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();
            
            javafx.scene.control.MenuItem deleteItem = new javafx.scene.control.MenuItem("Delete");
            deleteItem.setStyle("-fx-text-fill: -app-danger;");
            deleteItem.setOnAction(event -> {
                if (table.getSelectionModel().getSelectedItems().isEmpty() && row.getItem() != null) {
                    table.getSelectionModel().select(row.getItem());
                }
                deleteAction.run();
            });
            
            contextMenu.getItems().add(deleteItem);
            
            row.contextMenuProperty().bind(
                javafx.beans.binding.Bindings.when(row.emptyProperty())
                .then((javafx.scene.control.ContextMenu)null)
                .otherwise(contextMenu)
            );
            return row;
        });
        
        table.getColumns().addAll(sttCol, nameCol, phoneCol, addrCol);
        java.util.LinkedHashMap<String, javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Supplier, ?>> supplierSortColumns =
            new java.util.LinkedHashMap<>();
        supplierSortColumns.put("name", nameCol);
        supplierSortColumns.put("phone", phoneCol);
        installSortHeaderIndicators(supplierSortColumns);
        
        // Add Form
        javafx.scene.layout.HBox addBox = new javafx.scene.layout.HBox(10);
        addBox.setAlignment(Pos.CENTER_LEFT);
        TextField nameField = new TextField(); nameField.setPromptText("Name");
        TextField phoneField = new TextField(); phoneField.setPromptText("Phone");
        TextField addrField = new TextField(); addrField.setPromptText("Address");
        Button addBtn = createExpandableGreenActionButton("Add", 100);
        addBtn.setOnAction(e -> {
            if (nameField.getText().isEmpty()) return;
            try {
                supplierService.saveSupplier(new com.pbl3.project.pbl3_project.entity.Supplier(nameField.getText(), phoneField.getText(), addrField.getText()));
                toastService.showSuccess("Added!");
                nameField.clear(); phoneField.clear(); addrField.clear();
                supplierCurrentPage[0] = 0;
                refreshSupplierPage.run();
            } catch (Exception ex) {
                toastService.showError("Error: " + ex.getMessage());
            }
        });
        
        addBox.getChildren().addAll(nameField, phoneField, addrField, addBtn);

        Label supplierSortStatusLabel = createSortStatusLabel(supplierSortState, supplierSortLabels);
        Runnable applySupplierSortUi = () -> {
            applySortStateToTable(table, supplierSortColumns, supplierSortState);
            supplierSortStatusLabel.setText(buildSortStatusText(supplierSortState, supplierSortLabels));
        };
        applySupplierSortUi.run();
        installManualServerSorting(
            table,
            supplierSortColumns,
            supplierSortState,
            () -> {
                applySupplierSortUi.run();
                supplierCurrentPage[0] = 0;
                refreshSupplierPage.run();
            }
        );

        javafx.scene.layout.HBox statusBar = new javafx.scene.layout.HBox(15, supplierSortStatusLabel, supplierRowCountLabel, supplierPageLabel, supplierPrevBtn, supplierNextBtn);
        applyStandardTableStatusBar(statusBar);

        root.getChildren().addAll(topBar2, addBox, table, statusBar);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        table.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<com.pbl3.project.pbl3_project.entity.Supplier>) c -> updateSupplierStatusBar.run());
        refreshSupplierPage.run();
        enableDeselectOnOutsideClick(root, table);
        return root;
    }

    private javafx.scene.Node createProductView(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        javafx.scene.layout.StackPane root = new javafx.scene.layout.StackPane();
        final String productSortStateKey = "products";
        TableSortState productSortState = getOrCreateTableSortState(
            productSortStateKey,
            new SortCriterion("name", javafx.scene.control.TableColumn.SortType.ASCENDING)
        );
        java.util.LinkedHashMap<String, String> productSortProperties = new java.util.LinkedHashMap<>();
        productSortProperties.put("id", "id");
        productSortProperties.put("sku", "sku");
        productSortProperties.put("name", "name");
        productSortProperties.put("brand", "brand.name");
        productSortProperties.put("price", "price");
        productSortProperties.put("quantity", "quantity");
        
        // --- 1. Available Products (Table View) ---
        VBox productListView = new VBox();
        applyStandardTablePageLayout(productListView);
        productListView.setVisible(false); // Hidden initially
        
        javafx.scene.layout.BorderPane toolbar = new javafx.scene.layout.BorderPane();
        toolbar.setPadding(Insets.EMPTY);
        
        javafx.scene.layout.HBox leftBox = new javafx.scene.layout.HBox(15);
        leftBox.setAlignment(Pos.CENTER_LEFT);
        
        Button backBtn = new Button();
        backBtn.setStyle("-fx-background-color: -app-danger; -fx-background-radius: 20; -fx-border-width: 0; -fx-padding: 0;");
        backBtn.setPrefSize(40, 40);
        backBtn.setMinSize(40, 40);
        backBtn.setMaxSize(40, 40);
        backBtn.setCursor(javafx.scene.Cursor.HAND);
        
        javafx.scene.shape.SVGPath arrowLeft = new javafx.scene.shape.SVGPath();
        arrowLeft.setContent("M19 12H5M12 19l-7-7 7-7");
        arrowLeft.setStroke(SURFACE_COLOR);
        arrowLeft.setStrokeWidth(2.5);
        arrowLeft.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        arrowLeft.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        arrowLeft.setFill(javafx.scene.paint.Color.TRANSPARENT);
        
        javafx.scene.layout.StackPane arrowWrapper = new javafx.scene.layout.StackPane(arrowLeft);
        arrowWrapper.setPrefSize(40, 40);
        arrowWrapper.setMinSize(40, 40);
        arrowWrapper.setMaxSize(40, 40);
        
        Label backLabelText = new Label("Back");
        backLabelText.setStyle("-fx-font-family: 'Be Vietnam Pro', 'Inter', sans-serif; -fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: -app-surface; -fx-padding: 0;");
        backLabelText.setOpacity(0);
        backLabelText.setScaleX(0.8);
        backLabelText.setScaleY(0.8);
        backLabelText.setTranslateX(15);
        
        javafx.scene.effect.GaussianBlur textBlur = new javafx.scene.effect.GaussianBlur(4.0);
        backLabelText.setEffect(textBlur);
        
        javafx.scene.layout.StackPane backBtnContent = new javafx.scene.layout.StackPane(arrowWrapper, backLabelText);
        javafx.scene.layout.StackPane.setAlignment(arrowWrapper, Pos.CENTER_LEFT);
        javafx.scene.layout.StackPane.setAlignment(backLabelText, Pos.CENTER_LEFT);
        javafx.scene.layout.StackPane.setMargin(backLabelText, new Insets(0, 0, 0, 36));
        
        javafx.scene.shape.Rectangle backClip = new javafx.scene.shape.Rectangle();
        backClip.setArcWidth(40);
        backClip.setArcHeight(40);
        backClip.widthProperty().bind(backBtn.widthProperty());
        backClip.heightProperty().bind(backBtn.heightProperty());
        backBtnContent.setClip(backClip);
        
        backBtn.setGraphic(backBtnContent);
        backBtn.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        backBtn.setAlignment(Pos.CENTER_LEFT);
        
        javafx.animation.Timeline hoverInBackBtn = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.millis(250),
                new javafx.animation.KeyValue(backBtn.minWidthProperty(), 100, SPRING_BOUNCE),
                new javafx.animation.KeyValue(backBtn.prefWidthProperty(), 100, SPRING_BOUNCE),
                new javafx.animation.KeyValue(backBtn.maxWidthProperty(), 100, SPRING_BOUNCE),
                new javafx.animation.KeyValue(arrowWrapper.translateXProperty(), 10, SPRING_BOUNCE),
                new javafx.animation.KeyValue(arrowWrapper.scaleXProperty(), 1.05, SPRING_BOUNCE),
                new javafx.animation.KeyValue(arrowWrapper.scaleYProperty(), 1.05, SPRING_BOUNCE),
                new javafx.animation.KeyValue(backLabelText.opacityProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(backLabelText.translateXProperty(), 10, SPRING_BOUNCE),
                new javafx.animation.KeyValue(backLabelText.scaleXProperty(), 1.0, SPRING_BOUNCE),
                new javafx.animation.KeyValue(backLabelText.scaleYProperty(), 1.0, SPRING_BOUNCE),
                new javafx.animation.KeyValue(textBlur.radiusProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH)
            )
        );
        
        javafx.animation.Timeline hoverOutBackBtn = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.millis(250),
                new javafx.animation.KeyValue(backBtn.minWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(backBtn.prefWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(backBtn.maxWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(arrowWrapper.translateXProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(arrowWrapper.scaleXProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(arrowWrapper.scaleYProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(backLabelText.opacityProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(backLabelText.translateXProperty(), 15, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(backLabelText.scaleXProperty(), 0.8, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(backLabelText.scaleYProperty(), 0.8, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(textBlur.radiusProperty(), 4.0, javafx.animation.Interpolator.EASE_BOTH)
            )
        );
        
        backBtn.setOnMouseEntered(e -> {
            backBtn.setStyle("-fx-background-color: -app-danger-hover; -fx-background-radius: 20; -fx-border-width: 0; -fx-effect: dropshadow(three-pass-box, -app-shadow, 15, 0, 0, 6); -fx-padding: 0;");
            hoverOutBackBtn.stop();
            hoverInBackBtn.play();
        });
        
        backBtn.setOnMouseExited(e -> {
            backBtn.setStyle("-fx-background-color: -app-danger; -fx-background-radius: 20; -fx-border-width: 0; -fx-padding: 0;");
            hoverInBackBtn.stop();
            hoverOutBackBtn.play();
        });
        
        backBtn.setOnMousePressed(e -> {
            backBtn.setScaleX(0.95);
            backBtn.setScaleY(0.95);
        });
        
        backBtn.setOnMouseReleased(e -> {
            backBtn.setScaleX(1.0);
            backBtn.setScaleY(1.0);
        });
        
        Label categoryTitle = new Label("All Products");
        categoryTitle.getStyleClass().add("product-header-title");
        
        leftBox.getChildren().addAll(backBtn, categoryTitle);
        toolbar.setLeft(leftBox);
        
        // Expandable Search Bar for filtering within category
        javafx.scene.layout.HBox searchBox = new javafx.scene.layout.HBox(0);
        searchBox.setAlignment(Pos.CENTER);
        searchBox.getStyleClass().add("expandable-search-box");
        searchBox.setPrefSize(40, 40);
        searchBox.setMinSize(40, 40);
        searchBox.setMaxSize(40, 40); // Initial collapsed size
        
        javafx.scene.shape.SVGPath searchIcon = new javafx.scene.shape.SVGPath();
        searchIcon.setContent("M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
        searchIcon.setFill(PRIMARY_COLOR);
        
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        spacer.setMinWidth(0);
        spacer.setPrefWidth(0);

        TextField categorySearchField = new TextField();
        categorySearchField.setPromptText(DEFAULT_SEARCH_PROMPT);
        categorySearchField.getStyleClass().add("search-text-field");
        categorySearchField.setMinWidth(0);
        categorySearchField.setMaxWidth(0);
        categorySearchField.setPrefWidth(0); // Initially hidden width
        categorySearchField.setOpacity(0);   // Initially invisible
        
        searchBox.getChildren().addAll(searchIcon, spacer, categorySearchField);
        
        // Expansion Animation
        javafx.animation.Timeline expandAnim = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.millis(150),
                new javafx.animation.KeyValue(searchBox.maxWidthProperty(), 250, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(searchBox.prefWidthProperty(), 250, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(spacer.minWidthProperty(), 8, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(categorySearchField.minWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(categorySearchField.maxWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(categorySearchField.prefWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(categorySearchField.opacityProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH)
            )
        );
        
        // Collapse Animation
        javafx.animation.Timeline collapseAnim = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.millis(150),
                new javafx.animation.KeyValue(searchBox.maxWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(searchBox.prefWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(spacer.minWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(categorySearchField.minWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(categorySearchField.maxWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(categorySearchField.prefWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(categorySearchField.opacityProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH)
            )
        );
        
        searchBox.setOnMouseClicked(e -> {
            if (searchBox.getMaxWidth() == 40) {
                expandAnim.play();
                categorySearchField.requestFocus();
            } else if (e.getTarget() == searchIcon || e.getTarget() == searchBox) {
                categorySearchField.clear();
                if (searchBox.getParent() != null) {
                    searchBox.getParent().requestFocus();
                }
                collapseAnim.play();
            }
        });

        // Filter Button (Brand)
        javafx.scene.layout.HBox filterBox = new javafx.scene.layout.HBox();
        filterBox.setAlignment(Pos.CENTER);
        filterBox.getStyleClass().add("expandable-search-box");
        filterBox.setPrefSize(40, 40); filterBox.setMinSize(40, 40); filterBox.setMaxSize(40, 40);
        filterBox.setCursor(javafx.scene.Cursor.HAND);
        javafx.scene.shape.SVGPath filterIcon = new javafx.scene.shape.SVGPath();
        filterIcon.setContent("M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z");
        filterIcon.setFill(PRIMARY_COLOR);
        filterBox.getChildren().add(filterIcon);

        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.Product> table = new javafx.scene.control.TableView<>();
        applyStandardTableSizing(table);
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        enableDragSelection(table);

        Button addButton = createExpandableGreenActionButton("Add Product", 150);

        javafx.scene.layout.HBox rightContainer = new javafx.scene.layout.HBox(15, filterBox, searchBox, addButton);
        rightContainer.setAlignment(Pos.CENTER_RIGHT);
        toolbar.setRight(rightContainer);
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, Long> idCol = new javafx.scene.control.TableColumn<>("ID");
        idCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, String> nameCol = new javafx.scene.control.TableColumn<>("Product Name");
        nameCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("name"));
        nameCol.setCellFactory(col -> new javafx.scene.control.TableCell<com.pbl3.project.pbl3_project.entity.Product, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: -app-primary; -fx-font-weight: bold;");
                }
            }
        });
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, String> priceCol = new javafx.scene.control.TableColumn<>("Price");
        priceCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(formatVnd(cell.getValue().getPrice())));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, Integer> qtyCol = new javafx.scene.control.TableColumn<>("Quantity");
        qtyCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("quantity"));

        Runnable deleteAction = () -> {
            java.util.List<com.pbl3.project.pbl3_project.entity.Product> selectedItems = new java.util.ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selectedItems.isEmpty()) return;

            if (!showConfirmDialog("Confirm Deletion", "Are you sure you want to delete " + selectedItems.size() + " selected product(s)?")) {
                return;
            }

            int deletedCount = 0;
            for (com.pbl3.project.pbl3_project.entity.Product product : selectedItems) {
                try {
                    productService.deleteProduct(product.getId(), user);
                    table.getItems().remove(product);
                    deletedCount++;
                } catch (Exception ex) {
                     toastService.showError("Could not delete product: " + ex.getMessage());
                }
            }
            if (deletedCount > 0) {
                 toastService.showSuccess("Deleted " + deletedCount + " products!");
                 if (productListView.getUserData() instanceof Runnable) ((Runnable)productListView.getUserData()).run();
            }
        };

        table.setOnKeyPressed(event -> {
            javafx.scene.input.KeyCode code = event.getCode();
            if (code == javafx.scene.input.KeyCode.DELETE || code == javafx.scene.input.KeyCode.BACK_SPACE || code == javafx.scene.input.KeyCode.ENTER) {
                deleteAction.run();
            }
        });
        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<com.pbl3.project.pbl3_project.entity.Product> row = new javafx.scene.control.TableRow<>();
            javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();
            
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    com.pbl3.project.pbl3_project.entity.Product product = row.getItem();
                    showProductDialog(stage, product, product.getCategory(), user, () -> {
                         if (productListView.getUserData() instanceof Runnable) ((Runnable)productListView.getUserData()).run();
                    });
                }
            });
            
            javafx.scene.control.MenuItem editItem = new javafx.scene.control.MenuItem("Edit");
            editItem.setStyle("-fx-text-fill: -app-primary;");
            editItem.setOnAction(event -> {
                com.pbl3.project.pbl3_project.entity.Product product = row.getItem();
                showProductDialog(stage, product, product.getCategory(), user, () -> {
                     if (productListView.getUserData() instanceof Runnable) ((Runnable)productListView.getUserData()).run();
                });
            });

            javafx.scene.control.MenuItem deleteItem = new javafx.scene.control.MenuItem("Delete");
            deleteItem.setStyle("-fx-text-fill: -app-danger;");
            deleteItem.setOnAction(event -> {
                if (table.getSelectionModel().getSelectedItems().isEmpty() && row.getItem() != null) {
                    table.getSelectionModel().select(row.getItem());
                }
                deleteAction.run();
            });

            contextMenu.getItems().addAll(editItem, new javafx.scene.control.SeparatorMenuItem(), deleteItem);
            
            row.contextMenuProperty().bind(
                javafx.beans.binding.Bindings.when(row.emptyProperty())
                .then((javafx.scene.control.ContextMenu)null)
                .otherwise(contextMenu)
            );
            return row;
        });
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, String> skuCol = new javafx.scene.control.TableColumn<>("SKU");
        skuCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("sku"));
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, String> brandCol = new javafx.scene.control.TableColumn<>("Brand");
        brandCol.setCellValueFactory(cell -> {
             var brand = cell.getValue().getBrand();
             return new javafx.beans.property.SimpleStringProperty(brand != null ? brand.getName() : "-");
        });

        table.getColumns().addAll(idCol, skuCol, nameCol, brandCol, priceCol, qtyCol);
        java.util.LinkedHashMap<String, javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, ?>> productSortColumns =
            new java.util.LinkedHashMap<>();
        productSortColumns.put("id", idCol);
        productSortColumns.put("sku", skuCol);
        productSortColumns.put("name", nameCol);
        productSortColumns.put("brand", brandCol);
        productSortColumns.put("price", priceCol);
        productSortColumns.put("quantity", qtyCol);
        installSortHeaderIndicators(productSortColumns);
        java.util.LinkedHashMap<String, String> productSortLabels = new java.util.LinkedHashMap<>();
        productSortLabels.put("id", "ID");
        productSortLabels.put("sku", "SKU");
        productSortLabels.put("name", "Product Name");
        productSortLabels.put("brand", "Brand");
        productSortLabels.put("price", "Price");
        productSortLabels.put("quantity", "Quantity");
        Label productSortStatusLabel = createSortStatusLabel(productSortState, productSortLabels);

        Label productRowCountLabel = createStatusMetaLabel("Showing 0-0 of 0 Row(s)");
        Label productPageLabel = createStatusMetaLabel("Page 0 / 0");
        Button productPrevBtn = createPageNavButton("Prev");
        Button productNextBtn = createPageNavButton("Next");
        javafx.scene.layout.HBox productStatusBar = new javafx.scene.layout.HBox(15, productSortStatusLabel, productRowCountLabel, productPageLabel, productPrevBtn, productNextBtn);
        applyStandardTableStatusBar(productStatusBar);

        productListView.getChildren().addAll(toolbar, table, productStatusBar);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);


        // --- 2. Category Overview (Grid View) ---
        VBox categoryView = new VBox(20);
        categoryView.setPadding(new Insets(35));
        categoryView.setAlignment(Pos.TOP_CENTER);
        
        Label title = new Label("Product Categories");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: -app-primary;");
        
        javafx.scene.layout.FlowPane categoryGrid = new javafx.scene.layout.FlowPane();
        categoryGrid.setHgap(20);
        categoryGrid.setVgap(20);
        categoryGrid.setAlignment(Pos.CENTER);
        
        Label categoryCountLabel = new Label("Total: 0 Categories");
        categoryCountLabel.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");
        javafx.scene.layout.HBox categoryStatusBar = new javafx.scene.layout.HBox(categoryCountLabel);
        categoryStatusBar.setAlignment(Pos.CENTER_RIGHT);
        categoryStatusBar.setPadding(new Insets(5, 5, 0, 0));

        categoryView.getChildren().addAll(title, categoryGrid, categoryStatusBar);
        
        // --- Logic: Navigation & Refresh ---
        final com.pbl3.project.pbl3_project.entity.Category[] selectedCategory = {null};
        final int productPageSize = 20;
        final int[] productCurrentPage = {0};
        final int[] productTotalPages = {0};
        final long[] productTotalElements = {0L};
        java.util.concurrent.atomic.AtomicReference<String> productSearchRef = new java.util.concurrent.atomic.AtomicReference<>("");
        java.util.concurrent.atomic.AtomicReference<java.util.Set<String>> productBrandsRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<BigDecimal> productMinPriceRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<BigDecimal> productMaxPriceRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<Integer> productMinQtyRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<Integer> productMaxQtyRef = new java.util.concurrent.atomic.AtomicReference<>(null);

        Runnable updateProductStatusBar = () -> updatePagedStatus(
            table,
            productRowCountLabel,
            productPageLabel,
            productPrevBtn,
            productNextBtn,
            productTotalElements[0],
            productCurrentPage[0],
            productTotalPages[0],
            productPageSize
        );
        Runnable refreshProductList = () -> {
            if (selectedCategory[0] == null) {
                table.setItems(javafx.collections.FXCollections.observableArrayList());
                productTotalElements[0] = 0;
                productTotalPages[0] = 0;
                updateProductStatusBar.run();
                return;
            }
            org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.Product> pageData =
                productService.searchProducts(
                    selectedCategory[0].getId(),
                    productSearchRef.get(),
                    productBrandsRef.get(),
                    productMinPriceRef.get(),
                    productMaxPriceRef.get(),
                    productMinQtyRef.get(),
                    productMaxQtyRef.get(),
                    createPageable(productSortState, productSortProperties, productCurrentPage[0], productPageSize)
                );
            if (pageData.getTotalPages() > 0 && productCurrentPage[0] >= pageData.getTotalPages()) {
                productCurrentPage[0] = pageData.getTotalPages() - 1;
                pageData = productService.searchProducts(
                    selectedCategory[0].getId(),
                    productSearchRef.get(),
                    productBrandsRef.get(),
                    productMinPriceRef.get(),
                    productMaxPriceRef.get(),
                    productMinQtyRef.get(),
                    productMaxQtyRef.get(),
                    createPageable(productSortState, productSortProperties, productCurrentPage[0], productPageSize)
                );
            }
            productTotalElements[0] = pageData.getTotalElements();
            productTotalPages[0] = pageData.getTotalPages();
            table.setItems(javafx.collections.FXCollections.observableArrayList(pageData.getContent()));
            updateProductStatusBar.run();
        };
        Runnable applyProductSortUi = () -> {
            applySortStateToTable(table, productSortColumns, productSortState);
            productSortStatusLabel.setText(buildSortStatusText(productSortState, productSortLabels));
        };
        applyProductSortUi.run();
        installManualServerSorting(
            table,
            productSortColumns,
            productSortState,
            () -> {
                applyProductSortUi.run();
                productCurrentPage[0] = 0;
                refreshProductList.run();
            }
        );
        productPrevBtn.setOnAction(e -> {
            if (productCurrentPage[0] > 0) {
                productCurrentPage[0]--;
                refreshProductList.run();
            }
        });
        productNextBtn.setOnAction(e -> {
            if (productCurrentPage[0] + 1 < productTotalPages[0]) {
                productCurrentPage[0]++;
                refreshProductList.run();
            }
        });

        // Wire up filter button (after table & selectedCategory are declared)
        javafx.stage.Popup filterPopup = new javafx.stage.Popup();
        filterPopup.setAutoHide(true);

        filterBox.setOnMouseClicked(ev -> {
            if (filterPopup.isShowing()) { filterPopup.hide(); return; }

            if (selectedCategory[0] == null) {
                return;
            }

            BigDecimal maxPriceValue = productService.getMaxPriceByCategory(selectedCategory[0].getId());
            double maxPrice = maxPriceValue == null ? 0.0 : maxPriceValue.doubleValue();
            int maxQty = productService.getMaxQuantityByCategory(selectedCategory[0].getId());
            if (maxPrice == 0) maxPrice = 1000;
            if (maxQty == 0) maxQty = 100;

            VBox popupContent = new VBox(12);
            popupContent.setPadding(new Insets(16));
            applyFilterPopupContainerStyle(popupContent);
            popupContent.setPrefWidth(280);

            // --- Brand Section ---
            Label brandTitle = new Label("Brands");
            brandTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");

            javafx.scene.control.CheckBox allBrandsCb = new javafx.scene.control.CheckBox("All Brands");
            allBrandsCb.setSelected(true);
            allBrandsCb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary; -fx-cursor: hand;");

            VBox brandCheckboxes = new VBox(6);
            brandCheckboxes.setPadding(new Insets(5, 5, 5, 10));
            java.util.List<javafx.scene.control.CheckBox> brandCbs = new java.util.ArrayList<>();
            java.util.Set<String> brands = productService.getBrandNamesByCategory(selectedCategory[0].getId());
            for (String brandName : brands) {
                javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(brandName);
                cb.setSelected(true);
                cb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary; -fx-cursor: hand;");
                cb.selectedProperty().addListener((obs, ov, nv) -> {
                    boolean allChecked = brandCbs.stream().allMatch(javafx.scene.control.CheckBox::isSelected);
                    allBrandsCb.setSelected(allChecked);
                });
                brandCbs.add(cb);
                brandCheckboxes.getChildren().add(cb);
            }
            allBrandsCb.setOnAction(ae -> {
                for (javafx.scene.control.CheckBox cb : brandCbs) cb.setSelected(allBrandsCb.isSelected());
            });

            javafx.scene.control.ScrollPane brandScroll = new javafx.scene.control.ScrollPane(brandCheckboxes);
            brandScroll.setFitToWidth(true);
            brandScroll.setMaxHeight(140);
            brandScroll.setStyle("-fx-background-color: transparent; -fx-background: -app-surface; -fx-border-color: -app-border; -fx-border-radius: 4;");
            FilterDisclosureSection brandSection = new FilterDisclosureSection(allBrandsCb, brandScroll);

        // ...
            // --- Range Slider Custom Helper ---
            // (Removed inline RangeSlider class)

            // --- Price Section ---
            Label priceTitle = new Label("Price Range");
            priceTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");
            Label priceLabel = new Label(String.format("0 - %,.0f VND", maxPrice));
            priceLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -app-primary; -fx-font-weight: bold;");

            RangeSlider priceSlider = new RangeSlider(0, maxPrice, 0, maxPrice, 240);
            priceSlider.minVal.addListener((o, ov, nv) -> priceLabel.setText(String.format("%,.0f - %,.0f VND", nv.doubleValue(), priceSlider.maxVal.get())));
            priceSlider.maxVal.addListener((o, ov, nv) -> priceLabel.setText(String.format("%,.0f - %,.0f VND", priceSlider.minVal.get(), nv.doubleValue())));

            // --- Quantity Section ---
            Label qtyTitle = new Label("Quantity Range");
            qtyTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");
            Label qtyLabel = new Label("0 - " + maxQty);
            qtyLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -app-primary; -fx-font-weight: bold;");

            RangeSlider qtySlider = new RangeSlider(0, maxQty, 0, maxQty, 240);
            qtySlider.minVal.addListener((o, ov, nv) -> qtyLabel.setText(String.format("%d - %d", nv.intValue(), (int)qtySlider.maxVal.get())));
            qtySlider.maxVal.addListener((o, ov, nv) -> qtyLabel.setText(String.format("%d - %d", (int)qtySlider.minVal.get(), nv.intValue())));

            // --- Buttons ---
            javafx.scene.layout.HBox btnRow = new javafx.scene.layout.HBox(10);
            btnRow.setAlignment(Pos.CENTER_RIGHT);

            final double fMaxPrice = maxPrice;
            final int fMaxQty = maxQty;

            Button resetBtn = new Button("Reset");
            resetBtn.getStyleClass().add("filter-reset-btn");
            resetBtn.setOnAction(ae -> {
                filterBox.setStyle("");
                allBrandsCb.setSelected(true);
                for (javafx.scene.control.CheckBox cb : brandCbs) cb.setSelected(true);
                brandSection.setExpanded(false);
                priceSlider.minVal.set(0); priceSlider.maxVal.set(fMaxPrice);
                qtySlider.minVal.set(0); qtySlider.maxVal.set(fMaxQty);
                // Reset to category data
                productBrandsRef.set(new java.util.LinkedHashSet<>());
                productMinPriceRef.set(null);
                productMaxPriceRef.set(null);
                productMinQtyRef.set(null);
                productMaxQtyRef.set(null);
                productCurrentPage[0] = 0;
                refreshProductList.run();
                filterPopup.hide();
            });

            Button applyBtn = new Button("Apply Filter");
            applyBtn.getStyleClass().add("filter-apply-btn");
            applyBtn.setOnAction(ae -> {
                java.util.Set<String> selectedBrands = new java.util.HashSet<>();
                for (javafx.scene.control.CheckBox cb : brandCbs) {
                    if (cb.isSelected()) selectedBrands.add(cb.getText());
                }
                double pMin = priceSlider.minVal.get();
                double pMax = priceSlider.maxVal.get();
                int qMin = (int) qtySlider.minVal.get();
                int qMax = (int) qtySlider.maxVal.get();

                productBrandsRef.set(allBrandsCb.isSelected() ? new java.util.LinkedHashSet<>() : selectedBrands);
                productMinPriceRef.set(pMin <= 0 ? null : MoneySupport.normalize(BigDecimal.valueOf(pMin)));
                productMaxPriceRef.set(pMax >= fMaxPrice ? null : MoneySupport.normalize(BigDecimal.valueOf(pMax)));
                productMinQtyRef.set(qMin <= 0 ? null : qMin);
                productMaxQtyRef.set(qMax >= fMaxQty ? null : qMax);
                productCurrentPage[0] = 0;
                refreshProductList.run();

                boolean hasFilter = !allBrandsCb.isSelected() || pMin > 0 || pMax < fMaxPrice || qMin > 0 || qMax < fMaxQty;
                filterBox.setStyle(hasFilter ? "-fx-border-color: -app-primary;" : "");
                filterPopup.hide();
            });

            btnRow.getChildren().addAll(resetBtn, applyBtn);

            // Separators
            javafx.scene.control.Separator sep1 = new javafx.scene.control.Separator();
            javafx.scene.control.Separator sep2 = new javafx.scene.control.Separator();

            popupContent.getChildren().addAll(
                brandTitle, brandSection.getNode(), sep1,
                priceTitle, priceLabel, priceSlider, sep2,
                qtyTitle, qtyLabel, qtySlider,
                btnRow
            );

            filterPopup.getContent().clear();
            filterPopup.getContent().add(popupContent);

            showPopupBelow(filterPopup, filterBox, -200, 5);
        });
        
        Runnable loadCategories = () -> {
            categoryGrid.getChildren().clear();
            java.util.List<com.pbl3.project.pbl3_project.entity.Category> categories = categoryRepository.findAll();
            java.util.List<com.pbl3.project.pbl3_project.entity.Product> allProducts = productService.getAllProducts(); // Cache for counts
            
            for (com.pbl3.project.pbl3_project.entity.Category cat : categories) {
                long count = allProducts.stream().filter(p -> p.getCategory() != null && p.getCategory().getId().equals(cat.getId())).count();
                
                VBox card = new VBox(10);
                card.getStyleClass().add("category-card");
                card.setPrefSize(200, 120);
                
                Label nameLbl = new Label(cat.getName());
                nameLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -app-primary;");
                
                Label countLbl = new Label(count + " Products");
                countLbl.getStyleClass().add("category-card-count");
                
                card.getChildren().addAll(nameLbl, countLbl);
                
                card.setOnMouseClicked(e -> {
                    // Navigate to Product List
                    selectedCategory[0] = cat;
                    categoryTitle.setText(cat.getName());
                    categorySearchField.clear();
                    categoryView.setVisible(false);
                    productListView.setVisible(true);
                    
                    productSearchRef.set("");
                    productBrandsRef.set(new java.util.LinkedHashSet<>());
                    productMinPriceRef.set(null);
                    productMaxPriceRef.set(null);
                    productMinQtyRef.set(null);
                    productMaxQtyRef.set(null);
                    productCurrentPage[0] = 0;
                    refreshProductList.run();
                });
                
                categoryGrid.getChildren().add(card);
            }
            categoryCountLabel.setText("Total: " + categories.size() + " Categories");
        };
        
        productListView.setUserData(refreshProductList);
        
        // Live search: filter as user types
        javafx.animation.PauseTransition productSearchPause = new javafx.animation.PauseTransition(Duration.millis(220));
        productSearchPause.setOnFinished(e -> {
            productCurrentPage[0] = 0;
            productSearchRef.set(categorySearchField.getText());
            refreshProductList.run();
        });
        categorySearchField.textProperty().addListener((obs, old, val) -> productSearchPause.playFromStart());
        
        backBtn.setOnAction(e -> {
            productListView.setVisible(false);
            categoryView.setVisible(true);
            selectedCategory[0] = null;
            categorySearchField.clear();
            loadCategories.run(); // Refresh counts
        });
        
        addButton.setOnAction(e -> showProductDialog(stage, null, selectedCategory[0], user, () -> {
            // After add, refresh current view
            if (productListView.isVisible()) refreshProductList.run();
            else loadCategories.run();
        }));

        loadCategories.run();
        table.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<com.pbl3.project.pbl3_project.entity.Product>) c -> updateProductStatusBar.run());
        root.getChildren().addAll(categoryView, productListView);
        enableDeselectOnOutsideClick(root, table);
        return root;
    }
    
    private VBox createOverviewView(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(
            createDashboardStateContent(
                "Loading dashboard",
                "Preparing today's snapshot and the last 7 days sales mix...",
                null,
                true
            )
        );
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        javafx.beans.binding.DoubleBinding dashboardViewportWidth = javafx.beans.binding.Bindings.createDoubleBinding(
            () -> Math.max(420.0, scrollPane.getViewportBounds().getWidth()),
            scrollPane.viewportBoundsProperty()
        );

        VBox root = new VBox(scrollPane);
        VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);
        Runnable[] loadDashboardRef = new Runnable[1];
        loadDashboardRef[0] = () -> {
            scrollPane.setContent(createDashboardStateContent(
                "Loading dashboard",
                "Preparing today's snapshot and the last 7 days sales mix...",
                null,
                true
            ));

            javafx.concurrent.Task<com.pbl3.project.pbl3_project.dto.report.DashboardOverviewData> task =
                new javafx.concurrent.Task<>() {
                    @Override
                    protected com.pbl3.project.pbl3_project.dto.report.DashboardOverviewData call() {
                        return reportService.getDashboardOverviewData(user);
                    }
                };

            task.setOnSucceeded(event -> {
                VBox loadedContent = buildOverviewContent(stage, user, task.getValue(), dashboardViewportWidth);
                loadedContent.setOpacity(0.0);
                scrollPane.setContent(loadedContent);
                javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(140), loadedContent);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });

            task.setOnFailed(event -> {
                Throwable ex = task.getException();
                Button retryButton = new Button("Retry");
                retryButton.getStyleClass().addAll("button", "dashboard-report-secondary-button");
                retryButton.setOnAction(e -> loadDashboardRef[0].run());
                scrollPane.setContent(createDashboardStateContent(
                    "Dashboard unavailable",
                    ex != null && ex.getMessage() != null && !ex.getMessage().isBlank()
                        ? "Could not load dashboard: " + ex.getMessage()
                        : "Could not load dashboard data.",
                    retryButton,
                    false
                ));
                if (ex != null && ex.getMessage() != null && !ex.getMessage().isBlank()) {
                    toastService.showError("Could not load dashboard: " + ex.getMessage());
                } else {
                    toastService.showError("Could not load dashboard");
                }
            });

            Thread worker = new Thread(task, "dashboard-overview-loader");
            worker.setDaemon(true);
            worker.start();
        };

        loadDashboardRef[0].run();
        return root;
    }

    private VBox buildOverviewContent(
        Stage stage,
        com.pbl3.project.pbl3_project.entity.User user,
        com.pbl3.project.pbl3_project.dto.report.DashboardOverviewData dashboardData,
        javafx.beans.value.ObservableNumberValue viewportWidthSource
    ) {
        VBox content = new VBox(20);
        content.getStyleClass().add("dashboard-page");
        content.setPadding(new Insets(20));
        content.setFillWidth(true);
        content.setMaxWidth(Double.MAX_VALUE);

        javafx.beans.binding.DoubleBinding dashboardContentWidth = javafx.beans.binding.Bindings.createDoubleBinding(
            () -> Math.max(420.0, viewportWidthSource.doubleValue() - 40.0),
            viewportWidthSource
        );
        boolean showInventorySnapshot = authorizationService.canViewAllOrders(user);
        boolean canOpenReports = authorizationService.canAccessReports(user);
        boolean canAccessExpenses = authorizationService.canAccessExpenses(user);

        Label titleLabel = new Label("Today's Snapshot");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");

        VBox headerCopy = new VBox(titleLabel);
        javafx.scene.layout.FlowPane headerRow = new javafx.scene.layout.FlowPane();
        headerRow.setHgap(12);
        headerRow.setVgap(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setMaxWidth(Double.MAX_VALUE);
        headerRow.prefWrapLengthProperty().bind(dashboardContentWidth.subtract(30));
        headerRow.getChildren().add(headerCopy);

        VBox revenueCard = createDashboardMetricCard(
            "Today's Revenue",
            formatVnd(dashboardData.todayRevenue()),
            "-app-success",
            formatDashboardCurrencyDelta(dashboardData.revenueDeltaVsYesterday()),
            getDashboardDeltaColor(dashboardData.revenueDeltaVsYesterday(), true),
            createRevenuePanelIcon()
        );
        VBox ordersCard = createDashboardMetricCard(
            "Orders Today",
            String.valueOf(dashboardData.todayOrders()),
            "-app-primary",
            formatDashboardCountDelta(dashboardData.ordersDeltaVsYesterday(), "orders"),
            getDashboardDeltaColor(dashboardData.ordersDeltaVsYesterday(), true),
            createOrdersPanelIcon()
        );
        VBox expensesCard = createDashboardMetricCard(
            "Today's Expenses",
            formatVnd(dashboardData.todayExpenses()),
            "#fe9900",
            formatDashboardCurrencyDelta(dashboardData.expenseDeltaVsYesterday()),
            getDashboardDeltaColor(dashboardData.expenseDeltaVsYesterday(), false),
            createExpensesPanelIcon()
        );
        installDashboardPaneHover(revenueCard);
        installDashboardPaneHover(ordersCard);
        installDashboardPaneHover(expensesCard);
        javafx.scene.layout.GridPane statsRow;
        if (showInventorySnapshot) {
            VBox lowStockCard = createDashboardMetricCard(
                "Low Stock Items",
                String.valueOf(dashboardData.lowStockCount()),
                dashboardData.lowStockCount() > 0 ? "-app-danger" : "-app-text-muted",
                formatDashboardCountDelta(dashboardData.lowStockDeltaVsYesterday(), "items"),
                getDashboardDeltaColor(dashboardData.lowStockDeltaVsYesterday(), false),
                createLowStockPanelIcon()
            );
            installDashboardPaneHover(lowStockCard);
            if (canAccessExpenses) {
                statsRow = createResponsiveDashboardQuadRow(
                    revenueCard,
                    ordersCard,
                    expensesCard,
                    lowStockCard,
                    dashboardContentWidth,
                    1180.0,
                    760.0
                );
            } else {
                statsRow = createResponsiveDashboardKpiRow(
                    revenueCard,
                    ordersCard,
                    lowStockCard,
                    dashboardContentWidth,
                    980.0
                );
            }
            if (canOpenReports) {
                makeDashboardDrillDown(
                    lowStockCard,
                    "Open stock summary in Reports",
                    () -> showOperationalReportsScene(stage, user, null, null, ReportFocusTarget.SUMMARY)
                );
            }
            if (canAccessExpenses) {
                makeDashboardDrillDown(
                    expensesCard,
                    "Open today's expenses",
                    () -> showExpensesScene(stage, user, java.time.LocalDate.now(), java.time.LocalDate.now())
                );
            }
        } else {
            statsRow = createResponsiveDashboardPairRow(
                revenueCard,
                ordersCard,
                dashboardContentWidth,
                720.0
            );
        }

        java.util.function.Function<com.pbl3.project.pbl3_project.dto.report.InsightDrilldownTarget, Runnable> dashboardInsightActionResolver =
            target -> createDashboardInsightAction(stage, user, target);

        VBox whatChangedSection = createReportSection(
            "What Changed Today",
            dashboardData.whatChanged() != null
                ? "Compared with " + dashboardData.whatChanged().baselineRangeLabel()
                : "Compared with yesterday",
            createWhatChangedContent(
                dashboardData.whatChanged(),
                dashboardInsightActionResolver,
                "No prior comparison available yet",
                dashboardContentWidth
            ),
            null,
            null
        );
        bindReportSectionFullWidth(whatChangedSection, dashboardContentWidth);
        enableScrollPerfCache(whatChangedSection);

        VBox actionCenterSection = null;
        if (dashboardData.actionCenter() != null) {
            actionCenterSection = createReportSection(
                "Action Center",
                "Prioritized actions from stock and sales signals",
                createActionCenterContent(stage, user, dashboardData.actionCenter(), 5, dashboardInsightActionResolver),
                null,
                null
            );
            bindReportSectionFullWidth(actionCenterSection, dashboardContentWidth);
            enableScrollPerfCache(actionCenterSection);
        }

        VBox reorderSection = null;
        if (dashboardData.reorder() != null) {
            reorderSection = createReportSection(
                "Explainable Reorder",
                "Suggested replenishment using the last 14 days of demand",
                createExplainableReorderContent(stage, user, dashboardData.reorder(), 5, false),
                null,
                null
            );
            bindReportSectionFullWidth(reorderSection, dashboardContentWidth);
            enableScrollPerfCache(reorderSection);
        }

        javafx.scene.chart.LineChart<String, Number> revenueChart = createDashboardLineChart("Date", "Revenue (VND)", false);
        if (revenueChart.getXAxis() instanceof javafx.scene.chart.CategoryAxis revenueXAxis) {
            configureDashboardCategoryAxis(revenueXAxis, new java.util.ArrayList<>(dashboardData.salesMix().revenueSeries().keySet()));
        }
        javafx.scene.chart.XYChart.Series<String, Number> revenueSeries = new javafx.scene.chart.XYChart.Series<>();
        dashboardData.salesMix().revenueSeries().forEach((label, value) -> revenueSeries.getData().add(
            createDashboardLineData(label, value, SUCCESS_BAR_FILL, label + ": " + formatVnd(value))
        ));
        applyLineSeriesStyling(revenueSeries, SUCCESS_BAR_FILL);
        revenueChart.getData().add(revenueSeries);
        configureDashboardVerticalValueAxis(revenueChart, dashboardData.salesMix().revenueSeries().values(), false);

        javafx.scene.chart.LineChart<String, Number> ordersChart = createDashboardLineChart("Date", "Orders", false);
        if (ordersChart.getXAxis() instanceof javafx.scene.chart.CategoryAxis ordersXAxis) {
            configureDashboardCategoryAxis(ordersXAxis, new java.util.ArrayList<>(dashboardData.salesMix().orderSeries().keySet()));
        }
        javafx.scene.chart.XYChart.Series<String, Number> ordersSeries = new javafx.scene.chart.XYChart.Series<>();
        dashboardData.salesMix().orderSeries().forEach((label, value) -> ordersSeries.getData().add(
            createDashboardLineData(label, value, PRIMARY_BAR_FILL, label + ": " + value + " orders")
        ));
        applyLineSeriesStyling(ordersSeries, PRIMARY_BAR_FILL);
        ordersChart.getData().add(ordersSeries);
        configureDashboardVerticalValueAxis(ordersChart, dashboardData.salesMix().orderSeries().values(), true);

        javafx.scene.chart.LineChart<String, Number> canceledOrdersChart = createDashboardLineChart("Date", "Canceled Orders", false);
        if (canceledOrdersChart.getXAxis() instanceof javafx.scene.chart.CategoryAxis canceledOrdersXAxis) {
            configureDashboardCategoryAxis(canceledOrdersXAxis, new java.util.ArrayList<>(dashboardData.salesMix().canceledOrderSeries().keySet()));
        }
        javafx.scene.chart.XYChart.Series<String, Number> canceledOrdersSeries = new javafx.scene.chart.XYChart.Series<>();
        dashboardData.salesMix().canceledOrderSeries().forEach((label, value) -> canceledOrdersSeries.getData().add(
            createDashboardLineData(label, value, DANGER_BAR_FILL, label + ": " + value + " canceled orders")
        ));
        applyLineSeriesStyling(canceledOrdersSeries, DANGER_BAR_FILL);
        canceledOrdersChart.getData().add(canceledOrdersSeries);
        configureDashboardVerticalValueAxis(canceledOrdersChart, dashboardData.salesMix().canceledOrderSeries().values(), true);

        javafx.scene.Node paymentChartContent = createPaymentMethodShareContent(
            dashboardData.salesMix().paymentMethodShare(),
            "No sales in the last 7 days"
        );
        javafx.scene.Node topSellingChartContent = createTopSellingChartContent(dashboardData.salesMix().topSellingProducts());

        VBox revenueSection = createReportSection(
            "Revenue - Last 7 Days",
            null,
            revenueChart,
            null,
            null
        );
        VBox ordersSection = createReportSection(
            "Orders - Last 7 Days",
            null,
            ordersChart,
            null,
            null
        );
        VBox canceledOrdersSection = createReportSection(
            "Canceled Orders - Last 7 Days",
            null,
            canceledOrdersChart,
            null,
            null
        );
        VBox paymentSection = createReportSection(
            "Payment Method Share - Last 7 Days",
            null,
            paymentChartContent,
            null,
            null
        );
        VBox topSellingSection = createReportSection(
            "Top Selling Products - Last 7 Days",
            null,
            topSellingChartContent,
            null,
            null
        );

        enableScrollPerfCache(revenueChart);
        enableScrollPerfCache(ordersChart);
        enableScrollPerfCache(canceledOrdersChart);
        enableScrollPerfCache(paymentChartContent);
        enableScrollPerfCache(topSellingChartContent);
        enableScrollPerfCache(revenueSection);
        enableScrollPerfCache(ordersSection);
        enableScrollPerfCache(canceledOrdersSection);
        enableScrollPerfCache(paymentSection);
        enableScrollPerfCache(topSellingSection);
        canceledOrdersSection.setMaxWidth(Double.MAX_VALUE);
        canceledOrdersSection.prefWidthProperty().bind(dashboardContentWidth);

        java.util.Map<com.pbl3.project.pbl3_project.entity.DashboardSectionKey, javafx.scene.Node> availableDashboardSections =
            new java.util.EnumMap<>(com.pbl3.project.pbl3_project.entity.DashboardSectionKey.class);
        availableDashboardSections.put(com.pbl3.project.pbl3_project.entity.DashboardSectionKey.KPI_ROW, statsRow);
        availableDashboardSections.put(com.pbl3.project.pbl3_project.entity.DashboardSectionKey.WHAT_CHANGED, whatChangedSection);
        if (actionCenterSection != null) {
            availableDashboardSections.put(com.pbl3.project.pbl3_project.entity.DashboardSectionKey.ACTION_CENTER, actionCenterSection);
        }
        if (reorderSection != null) {
            availableDashboardSections.put(com.pbl3.project.pbl3_project.entity.DashboardSectionKey.EXPLAINABLE_REORDER, reorderSection);
        }
        availableDashboardSections.put(com.pbl3.project.pbl3_project.entity.DashboardSectionKey.REVENUE_CHART, revenueSection);
        availableDashboardSections.put(com.pbl3.project.pbl3_project.entity.DashboardSectionKey.ORDERS_CHART, ordersSection);
        availableDashboardSections.put(com.pbl3.project.pbl3_project.entity.DashboardSectionKey.CANCELED_ORDERS_CHART, canceledOrdersSection);
        availableDashboardSections.put(com.pbl3.project.pbl3_project.entity.DashboardSectionKey.PAYMENT_METHOD_SHARE, paymentSection);
        availableDashboardSections.put(com.pbl3.project.pbl3_project.entity.DashboardSectionKey.TOP_SELLING, topSellingSection);

        java.time.LocalDate dashboardSalesStart = dashboardData.salesMix().startDate();
        java.time.LocalDate dashboardSalesEnd = dashboardData.salesMix().endDate();
        if (canOpenReports) {
            makeDashboardDrillDown(
                revenueCard,
                "Open today's sales summary in Reports",
                () -> showOperationalReportsScene(stage, user, java.time.LocalDate.now(), java.time.LocalDate.now(), ReportFocusTarget.SUMMARY)
            );
            makeDashboardDrillDown(
                ordersCard,
                "Open today's orders summary in Reports",
                () -> showOperationalReportsScene(stage, user, java.time.LocalDate.now(), java.time.LocalDate.now(), ReportFocusTarget.SUMMARY)
            );
            makeDashboardDrillDown(
                revenueSection,
                "Open revenue chart in Reports",
                () -> showOperationalReportsScene(stage, user, dashboardSalesStart, dashboardSalesEnd, ReportFocusTarget.REVENUE)
            );
            makeDashboardDrillDown(
                ordersSection,
                "Open orders chart in Reports",
                () -> showOperationalReportsScene(stage, user, dashboardSalesStart, dashboardSalesEnd, ReportFocusTarget.ORDERS)
            );
            makeDashboardDrillDown(
                canceledOrdersSection,
                "Open canceled orders chart in Reports",
                () -> showOperationalReportsScene(stage, user, dashboardSalesStart, dashboardSalesEnd, ReportFocusTarget.CANCELED_ORDERS)
            );
            makeDashboardDrillDown(
                paymentSection,
                "Open payment method share in Reports",
                () -> showOperationalReportsScene(stage, user, dashboardSalesStart, dashboardSalesEnd, ReportFocusTarget.PAYMENT_METHOD_SHARE)
            );
            makeDashboardDrillDown(
                topSellingSection,
                "Open top selling products in Reports",
                () -> showOperationalReportsScene(stage, user, dashboardSalesStart, dashboardSalesEnd, ReportFocusTarget.TOP_SELLING)
            );
        }

        if (showInventorySnapshot) {
            VBox lowStockPanel = new VBox(14);
            lowStockPanel.setPadding(new Insets(18));
            lowStockPanel.setMaxWidth(Double.MAX_VALUE);
            lowStockPanel.prefWidthProperty().bind(dashboardContentWidth);

            javafx.scene.shape.SVGPath checkIcon = new javafx.scene.shape.SVGPath();
            checkIcon.setContent("M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z");
            checkIcon.setFill(SUCCESS_COLOR);
            checkIcon.setScaleX(0.8);
            checkIcon.setScaleY(0.8);

            javafx.scene.shape.SVGPath crossIcon = new javafx.scene.shape.SVGPath();
            crossIcon.setContent("M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z");
            crossIcon.setFill(DANGER_COLOR);
            crossIcon.setScaleX(0.8);
            crossIcon.setScaleY(0.8);

            Button openProductsButton = new Button("Open Products");
            openProductsButton.getStyleClass().addAll("button", "dashboard-report-secondary-button");
            openProductsButton.setOnAction(e -> showDashboardScene(stage, user));

            Button goToImportButton = new Button("Go to Import Goods");
            goToImportButton.getStyleClass().addAll("button", "dashboard-report-secondary-button");
            goToImportButton.setOnAction(e -> showImportOrderScene(stage, user));

            javafx.scene.layout.FlowPane actionRow = new javafx.scene.layout.FlowPane();
            actionRow.setHgap(10);
            actionRow.setVgap(10);
            actionRow.setAlignment(Pos.CENTER_LEFT);
            actionRow.setMaxWidth(Double.MAX_VALUE);
            actionRow.prefWrapLengthProperty().bind(dashboardContentWidth.subtract(60));
            actionRow.getChildren().addAll(openProductsButton, goToImportButton);

            javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.Product> lowStockTable = new javafx.scene.control.TableView<>();
            prepareNonReorderableTable(lowStockTable);
            lowStockTable.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
            lowStockTable.setPrefHeight(220);
            lowStockTable.setMaxWidth(Double.MAX_VALUE);

            javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, String> nameCol = new javafx.scene.control.TableColumn<>("Product Name");
            nameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));

            javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, String> catCol = new javafx.scene.control.TableColumn<>("Category");
            catCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getCategory() != null ? data.getValue().getCategory().getName() : "-"
            ));

            javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, Integer> qtyCol = new javafx.scene.control.TableColumn<>("Current Qty");
            qtyCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("quantity"));
            qtyCol.setStyle("-fx-alignment: CENTER;");
            qtyCol.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
                @Override
                protected void updateItem(Integer item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(String.valueOf(item));
                        setStyle("-fx-text-fill: -app-danger-hover; -fx-font-weight: 700; -fx-alignment: CENTER;");
                    }
                }
            });

            javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Product, Integer> minCol = new javafx.scene.control.TableColumn<>("Min Level");
            minCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("minStockLevel"));
            minCol.setStyle("-fx-alignment: CENTER;");

            lowStockTable.getColumns().addAll(nameCol, catCol, qtyCol, minCol);
            lowStockTable.setItems(javafx.collections.FXCollections.observableArrayList(dashboardData.lowStockProducts()));

            Label lowStockTitle = new Label();
            lowStockTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700;");
            javafx.scene.layout.HBox titleRow = new javafx.scene.layout.HBox(8);
            titleRow.setAlignment(Pos.CENTER_LEFT);

            if (dashboardData.lowStockProducts().isEmpty()) {
                lowStockPanel.setStyle("-fx-background-color: -app-success-soft; -fx-background-radius: 16; " +
                    "-fx-border-color: -app-success; -fx-border-radius: 16; -fx-border-width: 2; " +
                    "-fx-effect: dropshadow(three-pass-box, -app-shadow, 10, 0, 0, 3);");
                lowStockTitle.setText("Stock Status");
                lowStockTitle.setStyle(lowStockTitle.getStyle() + "-fx-text-fill: -app-success-hover;");
                titleRow.getChildren().addAll(checkIcon, lowStockTitle);
                lowStockPanel.getChildren().addAll(titleRow, actionRow);
            } else {
                lowStockPanel.setStyle("-fx-background-color: -app-danger-soft; -fx-background-radius: 16; " +
                    "-fx-border-color: -app-danger; -fx-border-radius: 16; -fx-border-width: 2; " +
                    "-fx-effect: dropshadow(three-pass-box, -app-shadow, 10, 0, 0, 3);");
                lowStockTitle.setText("Low Stock Alert (" + dashboardData.lowStockProducts().size() + " items)");
                lowStockTitle.setStyle(lowStockTitle.getStyle() + "-fx-text-fill: -app-danger-hover;");
                titleRow.getChildren().addAll(crossIcon, lowStockTitle);
                lowStockPanel.getChildren().addAll(titleRow, actionRow, lowStockTable);
            }

            enableDeselectOnOutsideClick(content, lowStockTable);
            availableDashboardSections.put(com.pbl3.project.pbl3_project.entity.DashboardSectionKey.LOW_STOCK, lowStockPanel);
        }
        content.getChildren().setAll(assembleDashboardNodes(user, headerRow, availableDashboardSections, dashboardContentWidth));
        return content;
    }

    private VBox createDashboardStateContent(String title, String message, Button actionButton, boolean loading) {
        VBox content = new VBox(20);
        content.getStyleClass().add("dashboard-page");
        content.setPadding(new Insets(20));
        content.setFillWidth(true);
        content.setMaxWidth(Double.MAX_VALUE);

        Label titleLabel = new Label("Today's Snapshot");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");

        VBox headerCopy = new VBox(titleLabel);

        VBox stateCard = new VBox(14);
        stateCard.getStyleClass().add("report-section-card");
        stateCard.setPadding(new Insets(22));
        stateCard.setMaxWidth(Double.MAX_VALUE);

        Label stateTitle = new Label(title);
        stateTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: -app-text-primary;");

        Label stateMessage = new Label(message);
        stateMessage.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
        stateMessage.setWrapText(true);

        if (loading) {
            javafx.scene.control.ProgressIndicator indicator = new javafx.scene.control.ProgressIndicator();
            indicator.setPrefSize(38, 38);
            indicator.setMaxSize(38, 38);
            stateCard.getChildren().addAll(stateTitle, stateMessage, indicator);
        } else {
            stateCard.getChildren().addAll(stateTitle, stateMessage);
        }

        if (actionButton != null) {
            stateCard.getChildren().add(actionButton);
        }

        content.getChildren().addAll(headerCopy, stateCard);
        return content;
    }

    private VBox createOperationalReportsStateContent(String title, String message, Button actionButton, boolean loading) {
        VBox content = new VBox(20);
        content.setFillWidth(true);
        content.setMaxWidth(Double.MAX_VALUE);

        VBox stateCard = new VBox(14);
        stateCard.getStyleClass().add("report-section-card");
        stateCard.setPadding(new Insets(22));
        stateCard.setMaxWidth(Double.MAX_VALUE);

        Label stateTitle = new Label(title);
        stateTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: -app-text-primary;");

        Label stateMessage = new Label(message);
        stateMessage.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
        stateMessage.setWrapText(true);

        if (loading) {
            javafx.scene.control.ProgressIndicator indicator = new javafx.scene.control.ProgressIndicator();
            indicator.setPrefSize(38, 38);
            indicator.setMaxSize(38, 38);
            stateCard.getChildren().addAll(stateTitle, stateMessage, indicator);
        } else {
            stateCard.getChildren().addAll(stateTitle, stateMessage);
        }

        if (actionButton != null) {
            stateCard.getChildren().add(actionButton);
        }

        content.getChildren().add(stateCard);
        return content;
    }

    private VBox createOperationalReportsView(
        Stage stage,
        com.pbl3.project.pbl3_project.entity.User user,
        java.time.LocalDate initialStartDate,
        java.time.LocalDate initialEndDate,
        ReportFocusTarget initialFocusTarget
    ) {
        VBox pageContent = new VBox(20);
        pageContent.getStyleClass().add("reports-page");
        pageContent.setPadding(new Insets(20));

        Label titleLabel = new Label("Operational Reports");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");

        Label filterLabel = new Label("Date Range");
        filterLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: -app-text-primary;");

        javafx.scene.control.DatePicker startDatePicker = new javafx.scene.control.DatePicker();
        startDatePicker.setPromptText("Start Date");
        startDatePicker.setPrefWidth(140);
        startDatePicker.setStyle("-fx-font-size: 13px;");

        javafx.scene.control.DatePicker endDatePicker = new javafx.scene.control.DatePicker();
        endDatePicker.setPromptText("End Date");
        endDatePicker.setPrefWidth(140);
        endDatePicker.setStyle("-fx-font-size: 13px;");

        customizeDatePicker(startDatePicker);
        customizeDatePicker(endDatePicker);

        Button applyFilterButton = new Button("Apply");
        applyFilterButton.getStyleClass().addAll("button", "primary-button");
        applyFilterButton.setStyle("-fx-padding: 8 18; -fx-background-radius: 999;");

        Button resetFilterButton = new Button("Reset");
        resetFilterButton.getStyleClass().addAll("button", "dashboard-report-secondary-button");
        resetFilterButton.setStyle("-fx-padding: 8 18; -fx-background-radius: 999;");

        Button todayPresetButton = createReportPresetButton("Today");
        Button last7DaysPresetButton = createReportPresetButton("Last 7 Days");
        Button thisMonthPresetButton = createReportPresetButton("This Month");
        Button allTimePresetButton = createReportPresetButton("All Time");

        Label activeRangeLabel = new Label();
        activeRangeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -app-text-muted;");
        activeRangeLabel.setWrapText(true);

        javafx.scene.layout.HBox filterBar = new javafx.scene.layout.HBox(
            10,
            filterLabel,
            startDatePicker,
            new Label("-"),
            endDatePicker,
            applyFilterButton,
            resetFilterButton
        );
        filterBar.setAlignment(Pos.CENTER_LEFT);

        javafx.scene.layout.HBox presetBar = new javafx.scene.layout.HBox(
            8,
            todayPresetButton,
            last7DaysPresetButton,
            thisMonthPresetButton,
            allTimePresetButton
        );
        presetBar.setAlignment(Pos.CENTER_LEFT);

        VBox headerBox = new VBox(8, titleLabel, filterBar, presetBar, activeRangeLabel);

        VBox reportSections = new VBox(20);
        reportSections.setFillWidth(true);
        reportSections.setMaxWidth(Double.MAX_VALUE);
        final boolean[] initialFocusPending = {initialFocusTarget != null};
        final long[] reportLoadVersion = {0L};
        startDatePicker.setValue(initialStartDate);
        endDatePicker.setValue(initialEndDate);

        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(pageContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        javafx.beans.binding.DoubleBinding reportViewportWidth = javafx.beans.binding.Bindings.createDoubleBinding(
            () -> Math.max(420.0, scrollPane.getViewportBounds().getWidth()),
            scrollPane.viewportBoundsProperty()
        );

        Runnable[] refreshReportsRef = new Runnable[1];
        refreshReportsRef[0] = () -> {
            java.time.LocalDate startDate = startDatePicker.getValue();
            java.time.LocalDate endDate = endDatePicker.getValue();
            long loadVersion = ++reportLoadVersion[0];

            filterBar.setMouseTransparent(true);
            presetBar.setMouseTransparent(true);
            activeRangeLabel.setText("Loading report data...");
            reportSections.setOpacity(1.0);
            reportSections.getChildren().setAll(createOperationalReportsStateContent(
                "Loading reports",
                "Preparing operational reports and sales summaries...",
                null,
                true
            ));

            javafx.concurrent.Task<com.pbl3.project.pbl3_project.dto.report.OperationalReportData> task =
                new javafx.concurrent.Task<>() {
                    @Override
                    protected com.pbl3.project.pbl3_project.dto.report.OperationalReportData call() {
                        return reportService.getOperationalReportData(startDate, endDate);
                    }
                };

            task.setOnSucceeded(event -> {
                if (loadVersion != reportLoadVersion[0]) {
                    return;
                }
                filterBar.setMouseTransparent(false);
                presetBar.setMouseTransparent(false);

                com.pbl3.project.pbl3_project.dto.report.OperationalReportData reportData = task.getValue();
                activeRangeLabel.setText(buildOperationalReportContextLabel(reportData));
                ReportSectionsBundle sectionsBundle = createOperationalReportSections(
                    stage,
                    user,
                    pageContent,
                    scrollPane,
                    reportData,
                    startDate,
                    endDate,
                    initialFocusTarget,
                    reportViewportWidth
                );
                reportSections.getChildren().setAll(sectionsBundle.nodes());

                if (initialFocusPending[0]) {
                    initialFocusPending[0] = false;
                    javafx.scene.Node focusNode = sectionsBundle.anchors().get(initialFocusTarget);
                    if (focusNode != null) {
                        revealReportSection(scrollPane, focusNode);
                    }
                }
            });

            task.setOnFailed(event -> {
                if (loadVersion != reportLoadVersion[0]) {
                    return;
                }
                filterBar.setMouseTransparent(false);
                presetBar.setMouseTransparent(false);

                Throwable ex = task.getException();
                Button retryButton = new Button("Retry");
                retryButton.getStyleClass().addAll("button", "dashboard-report-secondary-button");
                retryButton.setOnAction(e -> refreshReportsRef[0].run());

                activeRangeLabel.setText("Could not load report data.");
                reportSections.setOpacity(1.0);
                reportSections.getChildren().setAll(createOperationalReportsStateContent(
                    "Reports unavailable",
                    ex != null && ex.getMessage() != null && !ex.getMessage().isBlank()
                        ? "Could not load reports: " + ex.getMessage()
                        : "Could not load report data.",
                    retryButton,
                    false
                ));

                if (ex != null && ex.getMessage() != null && !ex.getMessage().isBlank()) {
                    toastService.showError("Could not load reports: " + ex.getMessage());
                } else {
                    toastService.showError("Could not load reports");
                }
            });

            Thread worker = new Thread(task, "operational-reports-loader");
            worker.setDaemon(true);
            worker.start();
        };

        applyFilterButton.setOnAction(e -> {
            java.time.LocalDate startDate = startDatePicker.getValue();
            java.time.LocalDate endDate = endDatePicker.getValue();
            if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
                toastService.showWarning("End date must be on or after start date");
                return;
            }
            refreshReportsRef[0].run();
        });

        resetFilterButton.setOnAction(e -> {
            startDatePicker.setValue(null);
            endDatePicker.setValue(null);
            refreshReportsRef[0].run();
        });

        todayPresetButton.setOnAction(e -> {
            java.time.LocalDate today = java.time.LocalDate.now();
            startDatePicker.setValue(today);
            endDatePicker.setValue(today);
            refreshReportsRef[0].run();
        });

        last7DaysPresetButton.setOnAction(e -> {
            java.time.LocalDate today = java.time.LocalDate.now();
            startDatePicker.setValue(today.minusDays(6));
            endDatePicker.setValue(today);
            refreshReportsRef[0].run();
        });

        thisMonthPresetButton.setOnAction(e -> {
            java.time.LocalDate today = java.time.LocalDate.now();
            startDatePicker.setValue(today.withDayOfMonth(1));
            endDatePicker.setValue(today);
            refreshReportsRef[0].run();
        });

        allTimePresetButton.setOnAction(e -> {
            startDatePicker.setValue(null);
            endDatePicker.setValue(null);
            refreshReportsRef[0].run();
        });

        activeRangeLabel.setText("Loading report data...");
        reportSections.getChildren().setAll(createOperationalReportsStateContent(
            "Loading reports",
            "Preparing operational reports and sales summaries...",
            null,
            true
        ));
        pageContent.getChildren().addAll(headerBox, reportSections);

        VBox root = new VBox(scrollPane);
        VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);
        final javafx.beans.value.ChangeListener<javafx.scene.Scene>[] sceneListenerRef = new javafx.beans.value.ChangeListener[1];
        sceneListenerRef[0] = (obs, oldScene, newScene) -> {
            if (newScene == null) {
                return;
            }
            root.sceneProperty().removeListener(sceneListenerRef[0]);
            javafx.animation.PauseTransition initialLoadDelay =
                new javafx.animation.PauseTransition(javafx.util.Duration.millis(190));
            initialLoadDelay.setOnFinished(event -> refreshReportsRef[0].run());
            initialLoadDelay.play();
        };
        root.sceneProperty().addListener(sceneListenerRef[0]);
        return root;
    }

    private ReportSectionsBundle createOperationalReportSections(
        Stage stage,
        com.pbl3.project.pbl3_project.entity.User user,
        VBox interactionRoot,
        javafx.scene.control.ScrollPane scrollPane,
        com.pbl3.project.pbl3_project.dto.report.OperationalReportData reportData,
        java.time.LocalDate startDate,
        java.time.LocalDate endDate,
        ReportFocusTarget activeFocusTarget,
        javafx.beans.value.ObservableNumberValue widthSource
    ) {
        String rangeLabel = formatOperationalReportRangeLabel(startDate, endDate);
        java.util.Map<ReportFocusTarget, javafx.scene.Node> anchors = new java.util.EnumMap<>(ReportFocusTarget.class);

        VBox netRevenueCard = createDashboardCard(
            "Net Revenue",
            formatVnd(reportData.summary().netRevenue()),
            "-app-success",
            createRevenuePanelIcon()
        );
        VBox estimatedCostCard = createDashboardCard(
            "Estimated Cost (COGS)",
            formatVnd(reportData.summary().estimatedCost()),
            "#fe9900",
            createExpensesPanelIcon()
        );
        boolean grossProfitPositive = MoneySupport.normalize(reportData.summary().grossProfit()).signum() >= 0;
        VBox grossProfitCard = createDashboardCard(
            "Gross Profit",
            formatVnd(reportData.summary().grossProfit()),
            grossProfitPositive ? "-app-primary" : "-app-danger",
            createEstimatedProfitPanelIcon(grossProfitPositive)
        );
        VBox operatingExpensesCard = createDashboardCard(
            "Operating Expenses",
            formatVnd(reportData.summary().operatingExpenses()),
            "#fe9900",
            createExpensesPanelIcon()
        );
        boolean netProfitPositive = MoneySupport.normalize(reportData.summary().netProfit()).signum() >= 0;
        VBox netProfitCard = createDashboardCard(
            "Net Profit",
            formatVnd(reportData.summary().netProfit()),
            netProfitPositive ? "-app-success" : "-app-danger",
            createEstimatedProfitPanelIcon(netProfitPositive)
        );
        VBox unitsSoldCard = createDashboardCard(
            "Net Units Sold",
            String.valueOf(reportData.summary().netUnitsSold()),
            "#fe9900",
            createNetUnitsPanelIcon()
        );
        javafx.scene.layout.GridPane summaryPrimaryRow = createResponsiveDashboardKpiRow(
            netRevenueCard,
            estimatedCostCard,
            grossProfitCard,
            widthSource,
            1180.0
        );
        javafx.scene.layout.GridPane summarySecondaryRow = createResponsiveDashboardKpiRow(
            operatingExpensesCard,
            netProfitCard,
            unitsSoldCard,
            widthSource,
            1180.0
        );
        VBox summaryContent = new VBox(20, summaryPrimaryRow, summarySecondaryRow);

        Button exportSummaryBtn = createReportExportButton("Export Summary CSV");
        exportSummaryBtn.setOnAction(e -> exportCsv(
            stage,
            buildReportCsvFileName("operational-summary", startDate, endDate),
            java.util.List.of("Metric", "Value"),
            java.util.List.of(
                java.util.List.<String>of("Date Range", rangeLabel),
                java.util.List.<String>of("Net Revenue", formatVnd(reportData.summary().netRevenue())),
                java.util.List.<String>of("Estimated Cost", formatVnd(reportData.summary().estimatedCost())),
                java.util.List.<String>of("Gross Profit", formatVnd(reportData.summary().grossProfit())),
                java.util.List.<String>of("Operating Expenses", formatVnd(reportData.summary().operatingExpenses())),
                java.util.List.<String>of("Net Profit", formatVnd(reportData.summary().netProfit())),
                java.util.List.<String>of("Net Units Sold", String.valueOf(reportData.summary().netUnitsSold())),
                java.util.List.<String>of("Active SKUs", String.valueOf(reportData.summary().activeSkuCount())),
                java.util.List.<String>of("Low Stock SKUs", String.valueOf(reportData.summary().lowStockSkuCount())),
                java.util.List.<String>of("Refunded Amount", formatVnd(reportData.summary().refundedAmount()))
            )
        ));

        VBox summarySection = createReportSection(
            "Summary",
            null,
            summaryContent,
            exportSummaryBtn,
            activeFocusTarget == ReportFocusTarget.SUMMARY ? "From Dashboard" : null
        );
        enableScrollPerfCache(summarySection);
        anchors.put(ReportFocusTarget.SUMMARY, summarySection);

        java.util.function.Function<com.pbl3.project.pbl3_project.dto.report.InsightDrilldownTarget, Runnable> reportInsightActionResolver =
            target -> createReportInsightAction(scrollPane, anchors, target);

        Button exportActionCenterBtn = createReportExportButton("Export CSV");
        exportActionCenterBtn.setOnAction(e -> exportCsv(
            stage,
            buildReportCsvFileName("action-center", startDate, endDate),
            java.util.List.of("Type", "Severity", "Title", "Description", "Action", "Impact"),
            reportData.actionCenter() == null || reportData.actionCenter().items() == null
                ? java.util.List.of()
                : reportData.actionCenter().items().stream().map(item -> java.util.List.<String>of(
                    item.type().name(),
                    item.severity().name(),
                    item.title(),
                    item.description(),
                    item.actionLabel() != null ? item.actionLabel() : "",
                    item.impactLabel() != null ? item.impactLabel() : ""
                )).toList()
        ));

        VBox actionCenterSection = createReportSection(
            "Action Center",
            "Prioritized actions from stock and sales signals",
            createActionCenterContent(stage, user, reportData.actionCenter(), Integer.MAX_VALUE, reportInsightActionResolver),
            exportActionCenterBtn,
            activeFocusTarget == ReportFocusTarget.ACTION_CENTER ? "From Dashboard" : null
        );
        bindReportSectionFullWidth(actionCenterSection, widthSource);
        enableScrollPerfCache(actionCenterSection);
        anchors.put(ReportFocusTarget.ACTION_CENTER, actionCenterSection);

        VBox whatChangedSection = createReportSection(
            "What Changed",
            reportData.whatChanged() != null
                ? "Compared with " + reportData.whatChanged().baselineRangeLabel()
                : "Compared with previous period",
            createWhatChangedContent(
                reportData.whatChanged(),
                reportInsightActionResolver,
                "No prior comparison available for the selected range",
                widthSource
            ),
            null,
            activeFocusTarget == ReportFocusTarget.WHAT_CHANGED ? "From Dashboard" : null
        );
        bindReportSectionFullWidth(whatChangedSection, widthSource);
        enableScrollPerfCache(whatChangedSection);
        anchors.put(ReportFocusTarget.WHAT_CHANGED, whatChangedSection);

        Button exportReorderBtn = createReportExportButton("Export CSV");
        exportReorderBtn.setOnAction(e -> exportCsv(
            stage,
            buildReportCsvFileName("explainable-reorder", startDate, endDate),
            java.util.List.of("Product", "Category", "On Hand", "Min Stock", "Avg Daily Units 14d", "Coverage Days", "Suggested Qty", "Last Inbound", "Latest Import Price", "Latest Supplier", "Explanation"),
            reportData.reorder() == null || reportData.reorder().rows() == null
                ? java.util.List.of()
                : reportData.reorder().rows().stream().map(row -> java.util.List.<String>of(
                    row.productName(),
                    row.categoryName(),
                    String.valueOf(row.onHandQuantity()),
                    String.valueOf(row.minStockLevel()),
                    formatCompactDecimal(row.avgDailyUnits14d()),
                    row.coverageKnown() && row.coverageDays() != null ? formatCompactDecimal(row.coverageDays()) : "",
                    String.valueOf(row.suggestedReorderQty()),
                    formatDateTime(row.lastInboundAt()),
                    row.latestImportPrice() != null ? formatVnd(row.latestImportPrice()) : "",
                    row.latestSupplierName() != null ? row.latestSupplierName() : "",
                    row.explanation()
                )).toList()
        ));

        VBox reorderSection = createReportSection(
            "Explainable Reorder",
            "Suggested replenishment using the last 14 days of demand",
            createExplainableReorderContent(stage, user, reportData.reorder(), Integer.MAX_VALUE, true),
            exportReorderBtn,
            activeFocusTarget == ReportFocusTarget.REORDER ? "From Dashboard" : null
        );
        bindReportSectionFullWidth(reorderSection, widthSource);
        anchors.put(ReportFocusTarget.REORDER, reorderSection);

        javafx.scene.chart.LineChart<String, Number> revenueChart = createReportSeriesLineChart("Date", "Revenue (VND)");
        if (revenueChart.getXAxis() instanceof javafx.scene.chart.CategoryAxis revenueXAxis) {
            configureDashboardCategoryAxis(revenueXAxis, new java.util.ArrayList<>(reportData.salesMix().revenueSeries().keySet()));
        }
        javafx.scene.chart.XYChart.Series<String, Number> revenueSeries = new javafx.scene.chart.XYChart.Series<>();
        reportData.salesMix().revenueSeries().forEach((label, value) -> revenueSeries.getData().add(
            createDashboardLineData(label, value, SUCCESS_BAR_FILL, label + ": " + formatVnd(value))
        ));
        applyLineSeriesStyling(revenueSeries, SUCCESS_BAR_FILL);
        revenueChart.getData().add(revenueSeries);
        configureDashboardVerticalValueAxis(revenueChart, reportData.salesMix().revenueSeries().values(), false);

        Button exportRevenueBtn = createReportExportButton("Export CSV");
        exportRevenueBtn.setOnAction(e -> exportCsv(
            stage,
            buildReportCsvFileName("revenue-series", reportData.salesMix().startDate(), reportData.salesMix().endDate()),
            java.util.List.of("Label", "Revenue"),
            reportData.salesMix().revenueSeries().entrySet().stream()
                .map(entry -> java.util.List.<String>of(entry.getKey(), formatVnd(entry.getValue())))
                .toList()
        ));

        VBox revenueSection = createReportSection(
            "Revenue",
            null,
            revenueChart,
            exportRevenueBtn,
            activeFocusTarget == ReportFocusTarget.REVENUE ? "From Dashboard" : null
        );
        bindReportSectionWidth(revenueSection, widthSource);
        enableScrollPerfCache(revenueChart);
        enableScrollPerfCache(revenueSection);
        anchors.put(ReportFocusTarget.REVENUE, revenueSection);

        javafx.scene.chart.LineChart<String, Number> ordersChart = createReportSeriesLineChart("Date", "Orders");
        if (ordersChart.getXAxis() instanceof javafx.scene.chart.CategoryAxis ordersXAxis) {
            configureDashboardCategoryAxis(ordersXAxis, new java.util.ArrayList<>(reportData.salesMix().orderSeries().keySet()));
        }
        javafx.scene.chart.XYChart.Series<String, Number> ordersSeries = new javafx.scene.chart.XYChart.Series<>();
        reportData.salesMix().orderSeries().forEach((label, value) -> ordersSeries.getData().add(
            createDashboardLineData(label, value, PRIMARY_BAR_FILL, label + ": " + value + " orders")
        ));
        applyLineSeriesStyling(ordersSeries, PRIMARY_BAR_FILL);
        ordersChart.getData().add(ordersSeries);
        configureDashboardVerticalValueAxis(ordersChart, reportData.salesMix().orderSeries().values(), true);

        Button exportOrdersBtn = createReportExportButton("Export CSV");
        exportOrdersBtn.setOnAction(e -> exportCsv(
            stage,
            buildReportCsvFileName("orders-series", reportData.salesMix().startDate(), reportData.salesMix().endDate()),
            java.util.List.of("Label", "Orders"),
            reportData.salesMix().orderSeries().entrySet().stream()
                .map(entry -> java.util.List.of(entry.getKey(), String.valueOf(entry.getValue())))
                .toList()
        ));

        VBox ordersSection = createReportSection(
            "Orders",
            null,
            ordersChart,
            exportOrdersBtn,
            activeFocusTarget == ReportFocusTarget.ORDERS ? "From Dashboard" : null
        );
        bindReportSectionWidth(ordersSection, widthSource);
        enableScrollPerfCache(ordersChart);
        enableScrollPerfCache(ordersSection);
        anchors.put(ReportFocusTarget.ORDERS, ordersSection);

        javafx.scene.chart.LineChart<String, Number> canceledOrdersChart = createReportSeriesLineChart("Date", "Canceled Orders");
        if (canceledOrdersChart.getXAxis() instanceof javafx.scene.chart.CategoryAxis canceledOrdersXAxis) {
            configureDashboardCategoryAxis(canceledOrdersXAxis, new java.util.ArrayList<>(reportData.salesMix().canceledOrderSeries().keySet()));
        }
        javafx.scene.chart.XYChart.Series<String, Number> canceledOrdersSeries = new javafx.scene.chart.XYChart.Series<>();
        reportData.salesMix().canceledOrderSeries().forEach((label, value) -> canceledOrdersSeries.getData().add(
            createDashboardLineData(label, value, DANGER_BAR_FILL, label + ": " + value + " canceled orders")
        ));
        applyLineSeriesStyling(canceledOrdersSeries, DANGER_BAR_FILL);
        canceledOrdersChart.getData().add(canceledOrdersSeries);
        configureDashboardVerticalValueAxis(canceledOrdersChart, reportData.salesMix().canceledOrderSeries().values(), true);

        Button exportCanceledOrdersBtn = createReportExportButton("Export CSV");
        exportCanceledOrdersBtn.setOnAction(e -> exportCsv(
            stage,
            buildReportCsvFileName("canceled-orders-series", reportData.salesMix().startDate(), reportData.salesMix().endDate()),
            java.util.List.of("Label", "Canceled Orders"),
            reportData.salesMix().canceledOrderSeries().entrySet().stream()
                .map(entry -> java.util.List.of(entry.getKey(), String.valueOf(entry.getValue())))
                .toList()
        ));

        VBox canceledOrdersSection = createReportSection(
            "Canceled Orders",
            null,
            canceledOrdersChart,
            exportCanceledOrdersBtn,
            activeFocusTarget == ReportFocusTarget.CANCELED_ORDERS ? "From Dashboard" : null
        );
        bindReportSectionFullWidth(canceledOrdersSection, widthSource);
        enableScrollPerfCache(canceledOrdersChart);
        enableScrollPerfCache(canceledOrdersSection);
        anchors.put(ReportFocusTarget.CANCELED_ORDERS, canceledOrdersSection);

        javafx.scene.layout.GridPane salesChartsRow = createResponsiveReportPairRow(
            revenueSection,
            ordersSection,
            widthSource,
            980.0
        );
        enableScrollPerfCache(salesChartsRow);

        javafx.scene.Node paymentChartContent = createPaymentMethodShareContent(
            reportData.salesMix().paymentMethodShare(),
            buildNoSalesRangeText(reportData.salesMix().startDate(), reportData.salesMix().endDate())
        );
        Button exportPaymentBtn = createReportExportButton("Export CSV");
        exportPaymentBtn.setOnAction(e -> exportCsv(
            stage,
            buildReportCsvFileName("payment-method-share", reportData.salesMix().startDate(), reportData.salesMix().endDate()),
            java.util.List.of("Payment Method", "Orders"),
            reportData.salesMix().paymentMethodShare().entrySet().stream()
                .map(entry -> java.util.List.of(formatPaymentMethodLabel(entry.getKey()), String.valueOf(entry.getValue())))
                .toList()
        ));

        VBox paymentSection = createReportSection(
            "Payment Method Share",
            null,
            paymentChartContent,
            exportPaymentBtn,
            activeFocusTarget == ReportFocusTarget.PAYMENT_METHOD_SHARE ? "From Dashboard" : null
        );
        enableScrollPerfCache(paymentChartContent);
        enableScrollPerfCache(paymentSection);
        anchors.put(ReportFocusTarget.PAYMENT_METHOD_SHARE, paymentSection);

        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.dto.report.ExpenseCategorySummaryRow> expenseCategoryTable =
            new javafx.scene.control.TableView<>();
        prepareNonReorderableTable(expenseCategoryTable);
        expenseCategoryTable.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        expenseCategoryTable.setItems(javafx.collections.FXCollections.observableArrayList(reportData.expenseCategorySummaries()));
        expenseCategoryTable.setPrefHeight(240);

        BigDecimal totalOperatingExpenses = MoneySupport.normalize(reportData.summary().operatingExpenses());
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.ExpenseCategorySummaryRow, String> expenseCategoryNameCol =
            new javafx.scene.control.TableColumn<>("Category");
        expenseCategoryNameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            formatExpenseCategoryLabel(data.getValue().category())
        ));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.ExpenseCategorySummaryRow, Number> expenseEntriesCol =
            new javafx.scene.control.TableColumn<>("Entries");
        expenseEntriesCol.setCellValueFactory(data -> new javafx.beans.property.SimpleLongProperty(data.getValue().entryCount()));
        expenseEntriesCol.setStyle("-fx-alignment: CENTER;");
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.ExpenseCategorySummaryRow, String> expenseAmountCol =
            new javafx.scene.control.TableColumn<>("Total Amount");
        expenseAmountCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatVnd(data.getValue().totalAmount())));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.ExpenseCategorySummaryRow, String> expenseShareCol =
            new javafx.scene.control.TableColumn<>("Share");
        expenseShareCol.setCellValueFactory(data -> {
            BigDecimal totalAmount = MoneySupport.normalize(data.getValue().totalAmount());
            if (MoneySupport.isZero(totalOperatingExpenses)) {
                return new javafx.beans.property.SimpleStringProperty("0.0%");
            }
            double share = totalAmount.doubleValue() * 100.0 / totalOperatingExpenses.doubleValue();
            return new javafx.beans.property.SimpleStringProperty(String.format("%.1f%%", share));
        });
        expenseShareCol.setStyle("-fx-alignment: CENTER;");
        expenseCategoryTable.getColumns().addAll(expenseCategoryNameCol, expenseEntriesCol, expenseAmountCol, expenseShareCol);

        Button exportExpensesBtn = createReportExportButton("Export CSV");
        exportExpensesBtn.setOnAction(e -> exportCsv(
            stage,
            buildReportCsvFileName("expenses-by-category", startDate, endDate),
            java.util.List.of("Category", "Entries", "Total Amount", "Share"),
            reportData.expenseCategorySummaries().stream().map(row -> {
                BigDecimal rowTotal = MoneySupport.normalize(row.totalAmount());
                String share = MoneySupport.isZero(totalOperatingExpenses)
                    ? "0.0%"
                    : String.format("%.1f%%", rowTotal.doubleValue() * 100.0 / totalOperatingExpenses.doubleValue());
                return java.util.List.<String>of(
                    formatExpenseCategoryLabel(row.category()),
                    String.valueOf(row.entryCount()),
                    formatVnd(row.totalAmount()),
                    share
                );
            }).toList()
        ));

        VBox expensesSection = createReportSection(
            "Expenses by Category",
            null,
            expenseCategoryTable,
            exportExpensesBtn,
            null
        );
        bindReportSectionFullWidth(expensesSection, widthSource);

        VBox promotionDiscountCard = createDashboardCard(
            "Promotion Discount",
            formatVnd(reportData.promotionReport().totalDiscount()),
            "-app-primary",
            createPromotionsNavIcon()
        );
        VBox promotedOrdersCard = createDashboardCard(
            "Promoted Orders",
            String.valueOf(reportData.promotionReport().promotedOrderCount()),
            "-app-success",
            createPromotionsNavIcon()
        );
        VBox activePromotionsCard = createDashboardCard(
            "Active Promotions",
            String.valueOf(reportData.promotionReport().activePromotions().size()),
            "#fe9900",
            createPromotionsNavIcon()
        );
        javafx.scene.layout.GridPane promotionSummaryRow = createResponsiveDashboardKpiRow(
            promotionDiscountCard,
            promotedOrdersCard,
            activePromotionsCard,
            widthSource,
            1180.0
        );

        Label promotionImpactTitle = new Label("Top Promotions");
        promotionImpactTitle.getStyleClass().add("header-label");
        promotionImpactTitle.setStyle("-fx-font-size: 16px;");
        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.dto.report.PromotionImpactRow> promotionImpactTable =
            new javafx.scene.control.TableView<>();
        prepareNonReorderableTable(promotionImpactTable);
        promotionImpactTable.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        promotionImpactTable.setItems(javafx.collections.FXCollections.observableArrayList(reportData.promotionReport().topPromotions()));
        promotionImpactTable.setPrefHeight(240);
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.PromotionImpactRow, String> impactNameCol =
            new javafx.scene.control.TableColumn<>("Promotion");
        impactNameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().promotionName()));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.PromotionImpactRow, String> impactScopeCol =
            new javafx.scene.control.TableColumn<>("Scope");
        impactScopeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatPromotionScopeLabel(data.getValue().scope())));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.PromotionImpactRow, Number> impactUsageCol =
            new javafx.scene.control.TableColumn<>("Usage");
        impactUsageCol.setCellValueFactory(data -> new javafx.beans.property.SimpleLongProperty(data.getValue().usageCount()));
        impactUsageCol.setStyle("-fx-alignment: CENTER;");
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.PromotionImpactRow, String> impactDiscountCol =
            new javafx.scene.control.TableColumn<>("Discount Given");
        impactDiscountCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatVnd(data.getValue().totalDiscount())));
        promotionImpactTable.getColumns().addAll(impactNameCol, impactScopeCol, impactUsageCol, impactDiscountCol);

        Label activePromotionTitle = new Label("Active Promotions");
        activePromotionTitle.getStyleClass().add("header-label");
        activePromotionTitle.setStyle("-fx-font-size: 16px;");
        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.dto.report.ActivePromotionRow> activePromotionTable =
            new javafx.scene.control.TableView<>();
        prepareNonReorderableTable(activePromotionTable);
        activePromotionTable.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        activePromotionTable.setItems(javafx.collections.FXCollections.observableArrayList(reportData.promotionReport().activePromotions()));
        activePromotionTable.setPrefHeight(220);
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.ActivePromotionRow, String> activePromotionNameCol =
            new javafx.scene.control.TableColumn<>("Promotion");
        activePromotionNameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().promotionName()));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.ActivePromotionRow, String> activePromotionScopeCol =
            new javafx.scene.control.TableColumn<>("Scope");
        activePromotionScopeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatPromotionScopeLabel(data.getValue().scope())));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.ActivePromotionRow, String> activePromotionTargetCol =
            new javafx.scene.control.TableColumn<>("Target");
        activePromotionTargetCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().targetLabel()));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.ActivePromotionRow, String> activePromotionDiscountCol =
            new javafx.scene.control.TableColumn<>("Discount");
        activePromotionDiscountCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().discountLabel()));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.ActivePromotionRow, String> activePromotionStatusCol =
            new javafx.scene.control.TableColumn<>("Status");
        activePromotionStatusCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().statusLabel()));
        activePromotionTable.getColumns().addAll(
            activePromotionNameCol,
            activePromotionScopeCol,
            activePromotionTargetCol,
            activePromotionDiscountCol,
            activePromotionStatusCol
        );

        VBox promotionSectionContent = new VBox(
            16,
            promotionSummaryRow,
            promotionImpactTitle,
            promotionImpactTable,
            activePromotionTitle,
            activePromotionTable
        );
        VBox promotionSection = createReportSection(
            "Promotion Impact",
            "Discount usage and currently active promotions",
            promotionSectionContent,
            null,
            null
        );
        bindReportSectionFullWidth(promotionSection, widthSource);

        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.dto.report.TopSellingProductRow> topSellingTable = new javafx.scene.control.TableView<>();
        prepareNonReorderableTable(topSellingTable);
        topSellingTable.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        topSellingTable.setItems(javafx.collections.FXCollections.observableArrayList(reportData.topSellingProducts()));
        topSellingTable.setPrefHeight(280);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.TopSellingProductRow, String> topProductCol = new javafx.scene.control.TableColumn<>("Product");
        topProductCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().productName()));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.TopSellingProductRow, String> topCategoryCol = new javafx.scene.control.TableColumn<>("Category");
        topCategoryCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().categoryName()));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.TopSellingProductRow, Number> topQtyCol = new javafx.scene.control.TableColumn<>("Net Sold");
        topQtyCol.setCellValueFactory(data -> new javafx.beans.property.SimpleLongProperty(data.getValue().netSoldQuantity()));
        topQtyCol.setStyle("-fx-alignment: CENTER;");
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.TopSellingProductRow, String> topRevenueCol = new javafx.scene.control.TableColumn<>("Revenue");
        topRevenueCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatVnd(data.getValue().netRevenue())));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.TopSellingProductRow, String> topProfitCol = new javafx.scene.control.TableColumn<>("Est. Profit");
        topProfitCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatVnd(data.getValue().estimatedProfit())));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.TopSellingProductRow, Number> topOnHandCol = new javafx.scene.control.TableColumn<>("On Hand");
        topOnHandCol.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().onHandQuantity()));
        topOnHandCol.setStyle("-fx-alignment: CENTER;");
        topSellingTable.getColumns().addAll(topProductCol, topCategoryCol, topQtyCol, topRevenueCol, topProfitCol, topOnHandCol);

        Button exportTopSellingBtn = createReportExportButton("Export CSV");
        exportTopSellingBtn.setOnAction(e -> exportCsv(
            stage,
            buildReportCsvFileName("top-selling-products", startDate, endDate),
            java.util.List.of("Product", "Category", "Net Sold", "Revenue", "Estimated Profit", "On Hand"),
            reportData.topSellingProducts().stream().map(row -> java.util.List.<String>of(
                row.productName(),
                row.categoryName(),
                String.valueOf(row.netSoldQuantity()),
                formatVnd(row.netRevenue()),
                formatVnd(row.estimatedProfit()),
                String.valueOf(row.onHandQuantity())
            )).toList()
        ));

        VBox topSellingSection = createReportSection(
            "Top Selling Products",
            null,
            topSellingTable,
            exportTopSellingBtn,
            activeFocusTarget == ReportFocusTarget.TOP_SELLING ? "From Dashboard" : null
        );
        bindReportSectionFullWidth(topSellingSection, widthSource);
        anchors.put(ReportFocusTarget.TOP_SELLING, topSellingSection);

        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.dto.report.CategoryStockRow> categoryStockTable = new javafx.scene.control.TableView<>();
        prepareNonReorderableTable(categoryStockTable);
        categoryStockTable.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        categoryStockTable.setItems(javafx.collections.FXCollections.observableArrayList(reportData.categoryStocks()));
        categoryStockTable.setPrefHeight(280);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.CategoryStockRow, String> categoryNameCol = new javafx.scene.control.TableColumn<>("Category");
        categoryNameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().categoryName()));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.CategoryStockRow, Number> categorySkuCol = new javafx.scene.control.TableColumn<>("SKUs");
        categorySkuCol.setCellValueFactory(data -> new javafx.beans.property.SimpleLongProperty(data.getValue().skuCount()));
        categorySkuCol.setStyle("-fx-alignment: CENTER;");
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.CategoryStockRow, Number> categoryQtyCol = new javafx.scene.control.TableColumn<>("On Hand Qty");
        categoryQtyCol.setCellValueFactory(data -> new javafx.beans.property.SimpleLongProperty(data.getValue().totalQuantity()));
        categoryQtyCol.setStyle("-fx-alignment: CENTER;");
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.CategoryStockRow, String> categoryRetailCol = new javafx.scene.control.TableColumn<>("Retail Value");
        categoryRetailCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatVnd(data.getValue().retailValue())));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.CategoryStockRow, String> categoryCostCol = new javafx.scene.control.TableColumn<>("Cost Value");
        categoryCostCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatVnd(data.getValue().costValue())));
        categoryStockTable.getColumns().addAll(categoryNameCol, categorySkuCol, categoryQtyCol, categoryRetailCol, categoryCostCol);

        Button exportCategoryStockBtn = createReportExportButton("Export CSV");
        exportCategoryStockBtn.setOnAction(e -> exportCsv(
            stage,
            buildReportCsvFileName("stock-by-category", startDate, endDate),
            java.util.List.of("Category", "SKUs", "On Hand Qty", "Retail Value", "Cost Value"),
            reportData.categoryStocks().stream().map(row -> java.util.List.<String>of(
                row.categoryName(),
                String.valueOf(row.skuCount()),
                String.valueOf(row.totalQuantity()),
                formatVnd(row.retailValue()),
                formatVnd(row.costValue())
            )).toList()
        ));

        VBox categoryStockSection = createReportSection(
            "Inventory by Category",
            null,
            categoryStockTable,
            exportCategoryStockBtn,
            activeFocusTarget == ReportFocusTarget.CATEGORY_STOCK ? "From Dashboard" : null
        );
        bindReportSectionFullWidth(categoryStockSection, widthSource);
        anchors.put(ReportFocusTarget.CATEGORY_STOCK, categoryStockSection);

        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.dto.report.AgingStockRow> agingTable = new javafx.scene.control.TableView<>();
        prepareNonReorderableTable(agingTable);
        agingTable.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        agingTable.setItems(javafx.collections.FXCollections.observableArrayList(reportData.agingStocks()));
        agingTable.setPrefHeight(320);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.AgingStockRow, String> agingProductCol = new javafx.scene.control.TableColumn<>("Product");
        agingProductCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().productName()));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.AgingStockRow, String> agingCategoryCol = new javafx.scene.control.TableColumn<>("Category");
        agingCategoryCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().categoryName()));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.AgingStockRow, Number> agingQtyCol = new javafx.scene.control.TableColumn<>("On Hand");
        agingQtyCol.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().onHandQuantity()));
        agingQtyCol.setStyle("-fx-alignment: CENTER;");
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.AgingStockRow, String> agingInboundCol = new javafx.scene.control.TableColumn<>("Last Inbound");
        agingInboundCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatDateTime(data.getValue().lastInboundAt())));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.AgingStockRow, String> agingDaysCol = new javafx.scene.control.TableColumn<>("Age");
        agingDaysCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().ageDays() >= 0 ? data.getValue().ageDays() + " days" : "Unknown"
        ));
        agingDaysCol.setStyle("-fx-alignment: CENTER;");
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.AgingStockRow, String> agingBucketCol = new javafx.scene.control.TableColumn<>("Bucket");
        agingBucketCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().agingBucket()));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.AgingStockRow, String> agingCostCol = new javafx.scene.control.TableColumn<>("Cost Value");
        agingCostCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatVnd(data.getValue().costValue())));
        agingTable.getColumns().addAll(agingProductCol, agingCategoryCol, agingQtyCol, agingInboundCol, agingDaysCol, agingBucketCol, agingCostCol);

        Button exportAgingBtn = createReportExportButton("Export CSV");
        exportAgingBtn.setOnAction(e -> exportCsv(
            stage,
            buildReportCsvFileName("aging-stock", startDate, endDate),
            java.util.List.of("Product", "Category", "On Hand", "Last Inbound", "Age Days", "Bucket", "Cost Value", "Retail Value"),
            reportData.agingStocks().stream().map(row -> java.util.List.<String>of(
                row.productName(),
                row.categoryName(),
                String.valueOf(row.onHandQuantity()),
                formatDateTime(row.lastInboundAt()),
                row.ageDays() >= 0 ? String.valueOf(row.ageDays()) : "",
                row.agingBucket(),
                formatVnd(row.costValue()),
                formatVnd(row.retailValue())
            )).toList()
        ));

        VBox agingSection = createReportSection(
            "Aging Stock",
            null,
            agingTable,
            exportAgingBtn,
            activeFocusTarget == ReportFocusTarget.AGING_STOCK ? "From Dashboard" : null
        );
        anchors.put(ReportFocusTarget.AGING_STOCK, agingSection);

        enableDeselectOnOutsideClick(interactionRoot, expenseCategoryTable);
        enableDeselectOnOutsideClick(interactionRoot, promotionImpactTable);
        enableDeselectOnOutsideClick(interactionRoot, activePromotionTable);
        enableDeselectOnOutsideClick(interactionRoot, topSellingTable);
        enableDeselectOnOutsideClick(interactionRoot, categoryStockTable);
        enableDeselectOnOutsideClick(interactionRoot, agingTable);

        return new ReportSectionsBundle(
            java.util.List.of(
                summarySection,
                actionCenterSection,
                whatChangedSection,
                reorderSection,
                salesChartsRow,
                canceledOrdersSection,
                paymentSection,
                expensesSection,
                promotionSection,
                topSellingSection,
                categoryStockSection,
                agingSection
            ),
            anchors
        );
    }
    
    private VBox createDashboardCard(String title, String value, String colorHex) {
        return createDashboardCard(title, value, colorHex, null);
    }

    private VBox createDashboardCard(String title, String value, String colorHex, javafx.scene.Node headerIcon) {
        VBox card = new VBox(8);
        card.getStyleClass().add("dashboard-summary-card");
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: -app-surface; -fx-background-radius: 15; " +
            "-fx-effect: dropshadow(three-pass-box, -app-shadow, 5, 0, 0, 1);");
        card.setMinHeight(100);
        
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dashboard-summary-title");
        titleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-muted;");

        javafx.scene.layout.HBox titleRow = new javafx.scene.layout.HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.getChildren().add(titleLabel);
        if (headerIcon != null) {
            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
            javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            titleRow.getChildren().addAll(spacer, headerIcon);
        }
        
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("dashboard-summary-value");
        valueLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + colorHex + ";");
        
        card.getChildren().addAll(titleRow, valueLabel);
        return card;
    }

    private VBox createDashboardMetricCard(String title, String value, String valueColorStyle, String deltaText, String deltaColorStyle) {
        return createDashboardMetricCard(title, value, valueColorStyle, deltaText, deltaColorStyle, null);
    }

    private VBox createDashboardMetricCard(
        String title,
        String value,
        String valueColorStyle,
        String deltaText,
        String deltaColorStyle,
        javafx.scene.Node headerIcon
    ) {
        VBox card = new VBox(10);
        card.getStyleClass().add("dashboard-metric-card");
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMinHeight(118);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dashboard-metric-title");
        titleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-muted;");

        javafx.scene.layout.HBox titleRow = new javafx.scene.layout.HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.getChildren().add(titleLabel);
        if (headerIcon != null) {
            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
            javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            titleRow.getChildren().addAll(spacer, headerIcon);
        }

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("dashboard-metric-value");
        valueLabel.setStyle("-fx-font-size: 30px; -fx-font-weight: 700; -fx-text-fill: " + valueColorStyle + ";");

        Label deltaLabel = new Label(deltaText);
        deltaLabel.getStyleClass().add("dashboard-metric-delta");
        deltaLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: " + deltaColorStyle + ";");
        deltaLabel.setWrapText(true);

        card.getChildren().addAll(titleRow, valueLabel, deltaLabel);
        return card;
    }

    private javafx.scene.Node createRevenuePanelIcon() {
        javafx.scene.shape.Circle outerCircle = new javafx.scene.shape.Circle(12);
        outerCircle.getStyleClass().add("dashboard-card-icon-stroke");
        outerCircle.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.shape.SVGPath amountPath = new javafx.scene.shape.SVGPath();
        amountPath.setContent("M16 8h-6a2 2 0 1 0 0 4h4a2 2 0 1 1 0 4H8");
        amountPath.getStyleClass().add("dashboard-card-icon-stroke");
        amountPath.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.shape.SVGPath dividerPath = new javafx.scene.shape.SVGPath();
        dividerPath.setContent("M12 18V6");
        dividerPath.getStyleClass().add("dashboard-card-icon-stroke");
        dividerPath.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.layout.StackPane iconPane = new javafx.scene.layout.StackPane(outerCircle, amountPath, dividerPath);
        iconPane.getStyleClass().add("dashboard-card-icon-wrap");
        iconPane.setMinSize(28, 28);
        iconPane.setPrefSize(28, 28);
        iconPane.setMaxSize(28, 28);
        iconPane.setScaleX(0.92);
        iconPane.setScaleY(0.92);
        return iconPane;
    }

    private javafx.scene.Node createOrdersPanelIcon() {
        javafx.scene.shape.SVGPath cartPath = new javafx.scene.shape.SVGPath();
        cartPath.setContent(
            "M9 21A1 1 0 1 1 7 21A1 1 0 1 1 9 21Z "
                + "M20 21A1 1 0 1 1 18 21A1 1 0 1 1 20 21Z "
                + "M2.05 2.05h2l2.66 12.42a2 2 0 0 0 2 1.58h9.78a2 2 0 0 0 1.95-1.57l1.65-7.43H5.12"
        );
        cartPath.getStyleClass().add("dashboard-card-icon-stroke-primary");
        cartPath.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.layout.StackPane iconPane = new javafx.scene.layout.StackPane(cartPath);
        iconPane.getStyleClass().add("dashboard-card-icon-wrap");
        iconPane.setMinSize(28, 28);
        iconPane.setPrefSize(28, 28);
        iconPane.setMaxSize(28, 28);
        iconPane.setScaleX(0.92);
        iconPane.setScaleY(0.92);
        return iconPane;
    }

    private javafx.scene.Node createLowStockPanelIcon() {
        javafx.scene.shape.SVGPath packagePath = new javafx.scene.shape.SVGPath();
        packagePath.setContent(
            "M16 16h6 "
                + "M21 10V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l2-1.14 "
                + "M7.5 4.27 16.5 9.42 "
                + "M3.29 7 12 12 20.71 7 "
                + "M12 22V12"
        );
        packagePath.getStyleClass().add("dashboard-card-icon-stroke-danger");
        packagePath.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.layout.StackPane iconPane = new javafx.scene.layout.StackPane(packagePath);
        iconPane.getStyleClass().add("dashboard-card-icon-wrap");
        iconPane.setMinSize(28, 28);
        iconPane.setPrefSize(28, 28);
        iconPane.setMaxSize(28, 28);
        iconPane.setScaleX(0.92);
        iconPane.setScaleY(0.92);
        return iconPane;
    }

    private javafx.scene.Node createEstimatedProfitPanelIcon(boolean positive) {
        javafx.scene.shape.SVGPath trendPath = new javafx.scene.shape.SVGPath();
        trendPath.setContent(
            "M22 7 13.5 15.5 8.5 10.5 2 17 "
                + "M16 7H22V13"
        );
        trendPath.getStyleClass().add(positive ? "dashboard-card-icon-stroke-primary" : "dashboard-card-icon-stroke-danger");
        trendPath.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.layout.StackPane iconPane = new javafx.scene.layout.StackPane(trendPath);
        iconPane.getStyleClass().add("dashboard-card-icon-wrap");
        iconPane.setMinSize(28, 28);
        iconPane.setPrefSize(28, 28);
        iconPane.setMaxSize(28, 28);
        iconPane.setScaleX(0.92);
        iconPane.setScaleY(0.92);
        return iconPane;
    }

    private javafx.scene.Node createExpensesPanelIcon() {
        java.util.function.Function<String, javafx.scene.shape.SVGPath> pathFactory = content -> {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(content);
            path.getStyleClass().add("dashboard-card-icon-stroke-accent");
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            return path;
        };

        javafx.scene.layout.Pane receiptIcon = new javafx.scene.layout.Pane(
            pathFactory.apply("M6 3h9l4 4v14a1 1 0 0 1-1.4.91L15 20l-2 2-2-2-2 2-2-2-2 2A1 1 0 0 1 4 21V5a2 2 0 0 1 2-2"),
            pathFactory.apply("M9 9h5"),
            pathFactory.apply("M9 13h6"),
            pathFactory.apply("M9 17h4")
        );
        receiptIcon.setMinSize(24, 24);
        receiptIcon.setPrefSize(24, 24);
        receiptIcon.setMaxSize(24, 24);
        receiptIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconPane = new javafx.scene.layout.StackPane(receiptIcon);
        iconPane.getStyleClass().add("dashboard-card-icon-wrap");
        iconPane.setMinSize(28, 28);
        iconPane.setPrefSize(28, 28);
        iconPane.setMaxSize(28, 28);
        iconPane.setScaleX(0.92);
        iconPane.setScaleY(0.92);
        return iconPane;
    }

    private javafx.scene.Node createNetUnitsPanelIcon() {
        java.util.function.Function<String, javafx.scene.shape.SVGPath> pathFactory = content -> {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(content);
            path.getStyleClass().add("dashboard-card-icon-stroke-accent");
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            return path;
        };

        javafx.scene.layout.Pane bagIcon = new javafx.scene.layout.Pane(
            pathFactory.apply("M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4Z"),
            pathFactory.apply("M3 6h18"),
            pathFactory.apply("M16 10a4 4 0 0 1-8 0")
        );
        bagIcon.setMinSize(24, 24);
        bagIcon.setPrefSize(24, 24);
        bagIcon.setMaxSize(24, 24);
        bagIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconPane = new javafx.scene.layout.StackPane(bagIcon);
        iconPane.getStyleClass().add("dashboard-card-icon-wrap");
        iconPane.setMinSize(28, 28);
        iconPane.setPrefSize(28, 28);
        iconPane.setMaxSize(28, 28);
        iconPane.setScaleX(0.92);
        iconPane.setScaleY(0.92);
        return iconPane;
    }

    private javafx.scene.Node createCancelSignalPanelIcon() {
        javafx.scene.shape.SVGPath firstStroke = new javafx.scene.shape.SVGPath();
        firstStroke.setContent("M7 7 17 17");
        firstStroke.getStyleClass().add("dashboard-card-icon-stroke-danger");
        firstStroke.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.shape.SVGPath secondStroke = new javafx.scene.shape.SVGPath();
        secondStroke.setContent("M17 7 7 17");
        secondStroke.getStyleClass().add("dashboard-card-icon-stroke-danger");
        secondStroke.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.Group iconGroup = new javafx.scene.Group(firstStroke, secondStroke);
        javafx.scene.layout.StackPane iconPane = new javafx.scene.layout.StackPane(iconGroup);
        iconPane.getStyleClass().add("dashboard-card-icon-wrap");
        iconPane.setMinSize(28, 28);
        iconPane.setPrefSize(28, 28);
        iconPane.setMaxSize(28, 28);
        iconPane.setScaleX(0.92);
        iconPane.setScaleY(0.92);
        return iconPane;
    }

    private javafx.scene.Node createWhatChangedInsightIcon(
        com.pbl3.project.pbl3_project.dto.report.WhatChangedType type,
        com.pbl3.project.pbl3_project.dto.report.InsightSeverity severity
    ) {
        if (type == null) {
            return createEstimatedProfitPanelIcon(severity != com.pbl3.project.pbl3_project.dto.report.InsightSeverity.CRITICAL);
        }
        return switch (type) {
            case REVENUE_CHANGE -> createRevenuePanelIcon();
            case ORDER_COUNT_CHANGE -> createOrdersPanelIcon();
            case AVERAGE_ORDER_VALUE_CHANGE -> createEstimatedProfitPanelIcon(true);
            case CANCEL_RATE_CHANGE -> createCancelSignalPanelIcon();
            case TOP_DRIVER_PRODUCT -> createNetUnitsPanelIcon();
        };
    }

    private javafx.scene.Node createActionCenterInsightIcon(
        com.pbl3.project.pbl3_project.dto.report.ActionCenterType type,
        com.pbl3.project.pbl3_project.dto.report.InsightSeverity severity
    ) {
        if (type == null) {
            return createEstimatedProfitPanelIcon(severity != com.pbl3.project.pbl3_project.dto.report.InsightSeverity.CRITICAL);
        }
        return switch (type) {
            case REORDER_NOW, LOW_COVERAGE, AGED_STOCK -> createLowStockPanelIcon();
            case REVENUE_DROP -> createEstimatedProfitPanelIcon(false);
            case CANCEL_SPIKE -> createCancelSignalPanelIcon();
        };
    }

    private javafx.scene.layout.GridPane createResponsiveDashboardKpiRow(
        VBox firstCard,
        VBox secondCard,
        VBox thirdCard,
        javafx.beans.value.ObservableNumberValue widthSource,
        double threeColumnBreakpoint
    ) {
        javafx.scene.layout.GridPane row = new javafx.scene.layout.GridPane();
        row.setHgap(20);
        row.setVgap(20);
        row.setMaxWidth(Double.MAX_VALUE);
        row.prefWidthProperty().bind(widthSource);

        java.util.List<VBox> cards = java.util.List.of(firstCard, secondCard, thirdCard);
        cards.forEach(card -> {
            card.setMinWidth(0);
            card.setMaxWidth(Double.MAX_VALUE);
            javafx.scene.layout.GridPane.setHgrow(card, javafx.scene.layout.Priority.ALWAYS);
            javafx.scene.layout.GridPane.setFillWidth(card, true);
        });

        final boolean[] threeColumnMode = {false};
        Runnable syncLayout = () -> {
            boolean shouldUseThreeColumns = widthSource.doubleValue() >= threeColumnBreakpoint;
            if (!row.getChildren().isEmpty() && threeColumnMode[0] == shouldUseThreeColumns) {
                return;
            }
            threeColumnMode[0] = shouldUseThreeColumns;
            row.getChildren().clear();
            row.getColumnConstraints().clear();
            row.getRowConstraints().clear();

            if (shouldUseThreeColumns) {
                for (int i = 0; i < 3; i++) {
                    javafx.scene.layout.ColumnConstraints column = new javafx.scene.layout.ColumnConstraints();
                    column.setPercentWidth(100.0 / 3.0);
                    column.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                    column.setFillWidth(true);
                    row.getColumnConstraints().add(column);
                }
                row.add(firstCard, 0, 0);
                row.add(secondCard, 1, 0);
                row.add(thirdCard, 2, 0);
            } else {
                javafx.scene.layout.ColumnConstraints singleColumn = new javafx.scene.layout.ColumnConstraints();
                singleColumn.setPercentWidth(100);
                singleColumn.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                singleColumn.setFillWidth(true);
                row.getColumnConstraints().add(singleColumn);
                row.add(firstCard, 0, 0);
                row.add(secondCard, 0, 1);
                row.add(thirdCard, 0, 2);
            }
        };

        syncLayout.run();
        widthSource.addListener((obs, oldValue, newValue) -> syncLayout.run());
        return row;
    }

    private javafx.scene.layout.GridPane createResponsiveDashboardPairRow(
        VBox firstCard,
        VBox secondCard,
        javafx.beans.value.ObservableNumberValue widthSource,
        double twoColumnBreakpoint
    ) {
        javafx.scene.layout.GridPane row = new javafx.scene.layout.GridPane();
        row.setHgap(20);
        row.setVgap(20);
        row.setMaxWidth(Double.MAX_VALUE);
        row.prefWidthProperty().bind(widthSource);

        java.util.List<VBox> cards = java.util.List.of(firstCard, secondCard);
        cards.forEach(card -> {
            card.setMinWidth(0);
            card.setMaxWidth(Double.MAX_VALUE);
            javafx.scene.layout.GridPane.setHgrow(card, javafx.scene.layout.Priority.ALWAYS);
            javafx.scene.layout.GridPane.setFillWidth(card, true);
        });

        final boolean[] twoColumnMode = {false};
        Runnable syncLayout = () -> {
            boolean shouldUseTwoColumns = widthSource.doubleValue() >= twoColumnBreakpoint;
            if (!row.getChildren().isEmpty() && twoColumnMode[0] == shouldUseTwoColumns) {
                return;
            }
            twoColumnMode[0] = shouldUseTwoColumns;
            row.getChildren().clear();
            row.getColumnConstraints().clear();
            row.getRowConstraints().clear();

            if (shouldUseTwoColumns) {
                for (int i = 0; i < 2; i++) {
                    javafx.scene.layout.ColumnConstraints column = new javafx.scene.layout.ColumnConstraints();
                    column.setPercentWidth(50);
                    column.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                    column.setFillWidth(true);
                    row.getColumnConstraints().add(column);
                }
                row.add(firstCard, 0, 0);
                row.add(secondCard, 1, 0);
            } else {
                javafx.scene.layout.ColumnConstraints singleColumn = new javafx.scene.layout.ColumnConstraints();
                singleColumn.setPercentWidth(100);
                singleColumn.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                singleColumn.setFillWidth(true);
                row.getColumnConstraints().add(singleColumn);
                row.add(firstCard, 0, 0);
                row.add(secondCard, 0, 1);
            }
        };

        syncLayout.run();
        widthSource.addListener((obs, oldValue, newValue) -> syncLayout.run());
        return row;
    }

    private javafx.scene.layout.GridPane createResponsiveDashboardQuadRow(
        VBox firstCard,
        VBox secondCard,
        VBox thirdCard,
        VBox fourthCard,
        javafx.beans.value.ObservableNumberValue widthSource,
        double fourColumnBreakpoint,
        double twoColumnBreakpoint
    ) {
        javafx.scene.layout.GridPane row = new javafx.scene.layout.GridPane();
        row.setHgap(20);
        row.setVgap(20);
        row.setMaxWidth(Double.MAX_VALUE);
        row.prefWidthProperty().bind(widthSource);

        java.util.List<VBox> cards = java.util.List.of(firstCard, secondCard, thirdCard, fourthCard);
        cards.forEach(card -> {
            card.setMinWidth(0);
            card.setMaxWidth(Double.MAX_VALUE);
            javafx.scene.layout.GridPane.setHgrow(card, javafx.scene.layout.Priority.ALWAYS);
            javafx.scene.layout.GridPane.setFillWidth(card, true);
        });

        final int[] modeRef = {-1};
        Runnable syncLayout = () -> {
            int mode;
            if (widthSource.doubleValue() >= fourColumnBreakpoint) {
                mode = 4;
            } else if (widthSource.doubleValue() >= twoColumnBreakpoint) {
                mode = 2;
            } else {
                mode = 1;
            }
            if (!row.getChildren().isEmpty() && modeRef[0] == mode) {
                return;
            }
            modeRef[0] = mode;
            row.getChildren().clear();
            row.getColumnConstraints().clear();
            row.getRowConstraints().clear();

            if (mode == 4) {
                for (int i = 0; i < 4; i++) {
                    javafx.scene.layout.ColumnConstraints column = new javafx.scene.layout.ColumnConstraints();
                    column.setPercentWidth(25);
                    column.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                    column.setFillWidth(true);
                    row.getColumnConstraints().add(column);
                }
                row.add(firstCard, 0, 0);
                row.add(secondCard, 1, 0);
                row.add(thirdCard, 2, 0);
                row.add(fourthCard, 3, 0);
                return;
            }

            if (mode == 2) {
                for (int i = 0; i < 2; i++) {
                    javafx.scene.layout.ColumnConstraints column = new javafx.scene.layout.ColumnConstraints();
                    column.setPercentWidth(50);
                    column.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                    column.setFillWidth(true);
                    row.getColumnConstraints().add(column);
                }
                row.add(firstCard, 0, 0);
                row.add(secondCard, 1, 0);
                row.add(thirdCard, 0, 1);
                row.add(fourthCard, 1, 1);
                return;
            }

            javafx.scene.layout.ColumnConstraints singleColumn = new javafx.scene.layout.ColumnConstraints();
            singleColumn.setPercentWidth(100);
            singleColumn.setHgrow(javafx.scene.layout.Priority.ALWAYS);
            singleColumn.setFillWidth(true);
            row.getColumnConstraints().add(singleColumn);
            row.add(firstCard, 0, 0);
            row.add(secondCard, 0, 1);
            row.add(thirdCard, 0, 2);
            row.add(fourthCard, 0, 3);
        };

        syncLayout.run();
        widthSource.addListener((obs, oldValue, newValue) -> syncLayout.run());
        return row;
    }

    private java.util.List<javafx.scene.Node> assembleDashboardNodes(
        com.pbl3.project.pbl3_project.entity.User user,
        javafx.scene.Node headerRow,
        java.util.Map<com.pbl3.project.pbl3_project.entity.DashboardSectionKey, javafx.scene.Node> availableSections,
        javafx.beans.value.ObservableNumberValue widthSource
    ) {
        java.util.List<javafx.scene.Node> nodes = new java.util.ArrayList<>();
        nodes.add(headerRow);

        java.util.List<com.pbl3.project.pbl3_project.entity.DashboardSectionKey> orderedSections =
            userUiPreferencesService.resolveDashboardSectionOrder(user);
        java.util.Set<com.pbl3.project.pbl3_project.entity.DashboardSectionKey> hiddenSections =
            userUiPreferencesService.resolveHiddenDashboardSections(user);
        java.util.List<VBox> gridBuffer = new java.util.ArrayList<>();

        for (com.pbl3.project.pbl3_project.entity.DashboardSectionKey sectionKey : orderedSections) {
            javafx.scene.Node section = availableSections.get(sectionKey);
            if (section == null || hiddenSections.contains(sectionKey)) {
                continue;
            }
            if (sectionKey.isGridEligible() && section instanceof VBox gridSection) {
                gridBuffer.add(gridSection);
                continue;
            }
            flushDashboardGridBuffer(nodes, gridBuffer, widthSource);
            nodes.add(section);
        }

        flushDashboardGridBuffer(nodes, gridBuffer, widthSource);
        return nodes;
    }

    private void flushDashboardGridBuffer(
        java.util.List<javafx.scene.Node> nodes,
        java.util.List<VBox> gridBuffer,
        javafx.beans.value.ObservableNumberValue widthSource
    ) {
        if (gridBuffer.isEmpty()) {
            return;
        }
        if (gridBuffer.size() == 1) {
            nodes.add(gridBuffer.get(0));
            gridBuffer.clear();
            return;
        }
        javafx.scene.layout.GridPane grid = createResponsiveDashboardSectionGrid(gridBuffer, widthSource, 1120.0);
        enableScrollPerfCache(grid);
        nodes.add(grid);
        gridBuffer.clear();
    }

    private javafx.scene.layout.GridPane createResponsiveDashboardSectionGrid(
        java.util.List<VBox> sections,
        javafx.beans.value.ObservableNumberValue widthSource,
        double twoColumnBreakpoint
    ) {
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setMaxWidth(Double.MAX_VALUE);
        grid.prefWidthProperty().bind(widthSource);

        sections.forEach(section -> {
            section.setMinWidth(0);
            section.setMaxWidth(Double.MAX_VALUE);
            javafx.scene.layout.GridPane.setHgrow(section, javafx.scene.layout.Priority.ALWAYS);
            javafx.scene.layout.GridPane.setFillWidth(section, true);
            javafx.scene.layout.GridPane.setFillHeight(section, true);
        });

        final boolean[] twoColumnMode = {false};
        Runnable syncLayout = () -> {
            boolean shouldUseTwoColumns = widthSource.doubleValue() >= twoColumnBreakpoint;
            if (!grid.getChildren().isEmpty() && twoColumnMode[0] == shouldUseTwoColumns) {
                return;
            }
            twoColumnMode[0] = shouldUseTwoColumns;
            grid.getChildren().clear();
            grid.getColumnConstraints().clear();

            if (shouldUseTwoColumns) {
                for (int i = 0; i < 2; i++) {
                    javafx.scene.layout.ColumnConstraints column = new javafx.scene.layout.ColumnConstraints();
                    column.setPercentWidth(50);
                    column.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                    column.setFillWidth(true);
                    grid.getColumnConstraints().add(column);
                }
                for (int index = 0; index < sections.size(); index++) {
                    grid.add(sections.get(index), index % 2, index / 2);
                }
                return;
            }

            javafx.scene.layout.ColumnConstraints singleColumn = new javafx.scene.layout.ColumnConstraints();
            singleColumn.setPercentWidth(100);
            singleColumn.setHgrow(javafx.scene.layout.Priority.ALWAYS);
            singleColumn.setFillWidth(true);
            grid.getColumnConstraints().add(singleColumn);
            for (int index = 0; index < sections.size(); index++) {
                grid.add(sections.get(index), 0, index);
            }
        };

        syncLayout.run();
        widthSource.addListener((obs, oldValue, newValue) -> syncLayout.run());
        return grid;
    }

    private javafx.scene.layout.GridPane createResponsiveDashboardChartGrid(
        VBox firstSection,
        VBox secondSection,
        VBox thirdSection,
        VBox fourthSection,
        javafx.beans.value.ObservableNumberValue widthSource,
        double twoColumnBreakpoint
    ) {
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setMaxWidth(Double.MAX_VALUE);
        grid.prefWidthProperty().bind(widthSource);

        java.util.List<VBox> sections = java.util.List.of(firstSection, secondSection, thirdSection, fourthSection);
        sections.forEach(section -> {
            section.setMinWidth(0);
            section.setMaxWidth(Double.MAX_VALUE);
            javafx.scene.layout.GridPane.setHgrow(section, javafx.scene.layout.Priority.ALWAYS);
            javafx.scene.layout.GridPane.setFillWidth(section, true);
            javafx.scene.layout.GridPane.setFillHeight(section, true);
        });

        final boolean[] twoColumnMode = {false};
        Runnable syncLayout = () -> {
            boolean shouldUseTwoColumns = widthSource.doubleValue() >= twoColumnBreakpoint;
            if (!grid.getChildren().isEmpty() && twoColumnMode[0] == shouldUseTwoColumns) {
                return;
            }
            twoColumnMode[0] = shouldUseTwoColumns;
            grid.getChildren().clear();
            grid.getColumnConstraints().clear();
            grid.getRowConstraints().clear();

            if (shouldUseTwoColumns) {
                for (int i = 0; i < 2; i++) {
                    javafx.scene.layout.ColumnConstraints column = new javafx.scene.layout.ColumnConstraints();
                    column.setPercentWidth(50);
                    column.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                    column.setFillWidth(true);
                    grid.getColumnConstraints().add(column);
                }
                grid.add(firstSection, 0, 0);
                grid.add(secondSection, 1, 0);
                grid.add(thirdSection, 0, 1);
                grid.add(fourthSection, 1, 1);
            } else {
                javafx.scene.layout.ColumnConstraints singleColumn = new javafx.scene.layout.ColumnConstraints();
                singleColumn.setPercentWidth(100);
                singleColumn.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                singleColumn.setFillWidth(true);
                grid.getColumnConstraints().add(singleColumn);
                grid.add(firstSection, 0, 0);
                grid.add(secondSection, 0, 1);
                grid.add(thirdSection, 0, 2);
                grid.add(fourthSection, 0, 3);
            }
        };

        syncLayout.run();
        widthSource.addListener((obs, oldValue, newValue) -> syncLayout.run());
        return grid;
    }

    private void bindDashboardKpiCardWidth(VBox card, javafx.beans.value.ObservableNumberValue widthSource) {
        card.setMinWidth(0);
        card.setMaxWidth(Double.MAX_VALUE);
        card.prefWidthProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(() -> {
            double width = widthSource.doubleValue();
            if (width <= 0) {
                return 280.0;
            }
            if (width >= 1040) {
                return Math.max(240.0, (width - 60.0) / 3.0);
            }
            if (width >= 720) {
                return Math.max(240.0, (width - 40.0) / 2.0);
            }
            return Math.max(260.0, width - 40.0);
        }, widthSource));
    }

    private void bindDashboardChartSectionWidth(VBox section, javafx.beans.value.ObservableNumberValue widthSource) {
        section.setMinWidth(0);
        section.setMaxWidth(Double.MAX_VALUE);
        section.prefWidthProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(() -> {
            double width = widthSource.doubleValue();
            if (width <= 0) {
                return 420.0;
            }
            if (width >= 1120) {
                return Math.max(380.0, (width - 60.0) / 2.0);
            }
            return Math.max(320.0, width - 40.0);
        }, widthSource));
    }

    private void bindReportSectionWidth(VBox section, javafx.beans.value.ObservableNumberValue widthSource) {
        section.setMinWidth(0);
        section.setMaxWidth(Double.MAX_VALUE);
        section.prefWidthProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(() -> {
            double width = widthSource.doubleValue();
            if (width <= 0) {
                return 420.0;
            }
            if (width >= 980) {
                return Math.max(360.0, (width - 60.0) / 2.0);
            }
            return Math.max(420.0, width - 40.0);
        }, widthSource));
    }

    private void bindReportSectionFullWidth(VBox section, javafx.beans.value.ObservableNumberValue widthSource) {
        section.setMinWidth(0);
        section.setMaxWidth(Double.MAX_VALUE);
        section.prefWidthProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(() -> {
            double width = widthSource.doubleValue();
            if (width <= 0) {
                return 420.0;
            }
            return Math.max(420.0, width - 40.0);
        }, widthSource));
    }

    private javafx.scene.layout.GridPane createResponsiveReportPairRow(
        VBox firstSection,
        VBox secondSection,
        javafx.beans.value.ObservableNumberValue widthSource,
        double twoColumnBreakpoint
    ) {
        javafx.scene.layout.GridPane row = new javafx.scene.layout.GridPane();
        row.setHgap(20);
        row.setVgap(20);
        row.setMaxWidth(Double.MAX_VALUE);

        javafx.scene.layout.GridPane.setHgrow(firstSection, javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.layout.GridPane.setHgrow(secondSection, javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.layout.GridPane.setFillWidth(firstSection, true);
        javafx.scene.layout.GridPane.setFillWidth(secondSection, true);
        javafx.scene.layout.GridPane.setFillHeight(firstSection, true);
        javafx.scene.layout.GridPane.setFillHeight(secondSection, true);
        firstSection.setMaxHeight(Double.MAX_VALUE);
        secondSection.setMaxHeight(Double.MAX_VALUE);

        final boolean[] twoColumnMode = {false};
        Runnable syncLayout = () -> {
            boolean shouldUseTwoColumns = widthSource.doubleValue() >= twoColumnBreakpoint;
            if (!row.getChildren().isEmpty() && twoColumnMode[0] == shouldUseTwoColumns) {
                return;
            }
            twoColumnMode[0] = shouldUseTwoColumns;
            row.getChildren().clear();
            row.getColumnConstraints().clear();
            row.getRowConstraints().clear();

            if (shouldUseTwoColumns) {
                javafx.scene.layout.ColumnConstraints leftColumn = new javafx.scene.layout.ColumnConstraints();
                leftColumn.setPercentWidth(50);
                leftColumn.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                leftColumn.setFillWidth(true);

                javafx.scene.layout.ColumnConstraints rightColumn = new javafx.scene.layout.ColumnConstraints();
                rightColumn.setPercentWidth(50);
                rightColumn.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                rightColumn.setFillWidth(true);

                row.getColumnConstraints().addAll(leftColumn, rightColumn);
                row.add(firstSection, 0, 0);
                row.add(secondSection, 1, 0);
            } else {
                javafx.scene.layout.ColumnConstraints singleColumn = new javafx.scene.layout.ColumnConstraints();
                singleColumn.setPercentWidth(100);
                singleColumn.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                singleColumn.setFillWidth(true);

                row.getColumnConstraints().add(singleColumn);
                row.add(firstSection, 0, 0);
                row.add(secondSection, 0, 1);
            }
        };

        syncLayout.run();
        widthSource.addListener((obs, oldValue, newValue) -> syncLayout.run());
        return row;
    }

    private javafx.scene.layout.GridPane createResponsiveReportSummaryRow(
        VBox firstCard,
        VBox secondCard,
        VBox thirdCard,
        VBox fourthCard,
        javafx.beans.value.ObservableNumberValue widthSource,
        double fourColumnBreakpoint,
        double twoColumnBreakpoint
    ) {
        javafx.scene.layout.GridPane row = new javafx.scene.layout.GridPane();
        row.setHgap(20);
        row.setVgap(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);

        java.util.List<VBox> cards = java.util.List.of(firstCard, secondCard, thirdCard, fourthCard);
        cards.forEach(card -> {
            card.setMinWidth(0);
            card.setMaxWidth(Double.MAX_VALUE);
            javafx.scene.layout.GridPane.setHgrow(card, javafx.scene.layout.Priority.ALWAYS);
            javafx.scene.layout.GridPane.setFillWidth(card, true);
        });

        final int[] layoutMode = {-1};
        Runnable syncLayout = () -> {
            double width = widthSource.doubleValue();
            int nextMode = width >= fourColumnBreakpoint ? 4 : width >= twoColumnBreakpoint ? 2 : 1;
            if (!row.getChildren().isEmpty() && layoutMode[0] == nextMode) {
                return;
            }
            layoutMode[0] = nextMode;
            row.getChildren().clear();
            row.getColumnConstraints().clear();
            row.getRowConstraints().clear();

            if (nextMode == 4) {
                for (int i = 0; i < 4; i++) {
                    javafx.scene.layout.ColumnConstraints column = new javafx.scene.layout.ColumnConstraints();
                    column.setPercentWidth(25);
                    column.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                    column.setFillWidth(true);
                    row.getColumnConstraints().add(column);
                }
                row.add(firstCard, 0, 0);
                row.add(secondCard, 1, 0);
                row.add(thirdCard, 2, 0);
                row.add(fourthCard, 3, 0);
            } else if (nextMode == 2) {
                for (int i = 0; i < 2; i++) {
                    javafx.scene.layout.ColumnConstraints column = new javafx.scene.layout.ColumnConstraints();
                    column.setPercentWidth(50);
                    column.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                    column.setFillWidth(true);
                    row.getColumnConstraints().add(column);
                }
                row.add(firstCard, 0, 0);
                row.add(secondCard, 1, 0);
                row.add(thirdCard, 0, 1);
                row.add(fourthCard, 1, 1);
            } else {
                javafx.scene.layout.ColumnConstraints singleColumn = new javafx.scene.layout.ColumnConstraints();
                singleColumn.setPercentWidth(100);
                singleColumn.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                singleColumn.setFillWidth(true);
                row.getColumnConstraints().add(singleColumn);
                row.add(firstCard, 0, 0);
                row.add(secondCard, 0, 1);
                row.add(thirdCard, 0, 2);
                row.add(fourthCard, 0, 3);
            }
        };

        syncLayout.run();
        widthSource.addListener((obs, oldValue, newValue) -> syncLayout.run());
        return row;
    }

    private void applyFilterPopupContainerStyle(javafx.scene.layout.Region container) {
        container.setStyle(
            "-fx-background-color: -app-surface; " +
            "-fx-background-radius: 18; " +
            "-fx-effect: dropshadow(three-pass-box, -app-shadow, 14, 0, 0, 5); " +
            "-fx-border-color: -app-border; " +
            "-fx-border-radius: 18;"
        );
    }

    private Label createFilterPopupSectionTitle(String text) {
        Label title = new Label(text);
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");
        return title;
    }

    private VBox createFilterPopupScrollContent() {
        VBox scrollContent = new VBox(10);
        scrollContent.setStyle("-fx-background-color: -app-surface;");
        scrollContent.setPadding(new Insets(5, 15, 5, 15));
        return scrollContent;
    }

    private javafx.scene.control.ScrollPane createFilterPopupScrollPane(javafx.scene.Node content, double viewportHeight) {
        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: -app-surface; -fx-background-color: transparent; -fx-border-color: transparent;");
        scrollPane.setPrefViewportHeight(viewportHeight);
        return scrollPane;
    }

    private FilterPopupShell createFilterPopupShell(double prefWidth, double viewportHeight) {
        VBox popupContainer = new VBox(10);
        popupContainer.setPadding(new Insets(15));
        applyFilterPopupContainerStyle(popupContainer);
        popupContainer.setPrefWidth(prefWidth);

        VBox scrollContent = createFilterPopupScrollContent();
        javafx.scene.control.ScrollPane scrollPane = createFilterPopupScrollPane(scrollContent, viewportHeight);
        popupContainer.getChildren().add(scrollPane);
        return new FilterPopupShell(popupContainer, scrollContent, scrollPane);
    }

    private javafx.scene.layout.HBox createFilterPopupActionRow(Button resetBtn, Button applyBtn) {
        javafx.scene.layout.HBox buttonRow = new javafx.scene.layout.HBox(10, resetBtn, applyBtn);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);
        return buttonRow;
    }

    private javafx.scene.chart.LineChart<String, Number> createDashboardLineChart(String xLabel, String yLabel, boolean showLegend) {
        javafx.scene.chart.CategoryAxis xAxis = new javafx.scene.chart.CategoryAxis();
        xAxis.setLabel(xLabel);
        xAxis.setAnimated(false);
        xAxis.setGapStartAndEnd(true);
        xAxis.setStartMargin(18);
        xAxis.setEndMargin(18);
        xAxis.setTickLabelFill(TEXT_MUTED_COLOR);
        xAxis.setTickLabelFont(javafx.scene.text.Font.font("Be Vietnam Pro", javafx.scene.text.FontWeight.MEDIUM, 11));

        javafx.scene.chart.NumberAxis yAxis = new javafx.scene.chart.NumberAxis();
        yAxis.setLabel(yLabel);
        yAxis.setAnimated(false);
        yAxis.setForceZeroInRange(true);
        yAxis.setMinorTickVisible(false);
        yAxis.setTickLabelFill(TEXT_MUTED_COLOR);
        yAxis.setTickLabelFont(javafx.scene.text.Font.font("Be Vietnam Pro", javafx.scene.text.FontWeight.MEDIUM, 11));

        javafx.scene.chart.LineChart<String, Number> chart = new javafx.scene.chart.LineChart<>(xAxis, yAxis);
        chart.getStyleClass().addAll("dashboard-bar-chart", "dashboard-line-chart");
        chart.setTitle(null);
        chart.setLegendVisible(showLegend);
        chart.setCreateSymbols(true);
        chart.setAnimated(false);
        chart.setAlternativeColumnFillVisible(false);
        chart.setAlternativeRowFillVisible(false);
        chart.setHorizontalGridLinesVisible(true);
        chart.setVerticalGridLinesVisible(false);
        chart.setPrefHeight(280);
        chart.setMinHeight(280);
        chart.setMaxHeight(280);
        return chart;
    }

    private javafx.scene.chart.LineChart<String, Number> createReportSeriesLineChart(String xLabel, String yLabel) {
        javafx.scene.chart.CategoryAxis xAxis = new javafx.scene.chart.CategoryAxis();
        xAxis.setLabel(xLabel);
        xAxis.setAnimated(false);
        xAxis.setGapStartAndEnd(true);
        xAxis.setStartMargin(18);
        xAxis.setEndMargin(18);
        xAxis.setTickLabelFill(TEXT_MUTED_COLOR);
        xAxis.setTickLabelFont(javafx.scene.text.Font.font("Be Vietnam Pro", javafx.scene.text.FontWeight.MEDIUM, 11));

        javafx.scene.chart.NumberAxis yAxis = new javafx.scene.chart.NumberAxis();
        yAxis.setLabel(yLabel);
        yAxis.setAnimated(false);
        yAxis.setForceZeroInRange(true);
        yAxis.setMinorTickVisible(false);
        yAxis.setTickLabelFill(TEXT_MUTED_COLOR);
        yAxis.setTickLabelFont(javafx.scene.text.Font.font("Be Vietnam Pro", javafx.scene.text.FontWeight.MEDIUM, 11));

        javafx.scene.chart.LineChart<String, Number> chart = new javafx.scene.chart.LineChart<>(xAxis, yAxis);
        chart.getStyleClass().addAll("dashboard-bar-chart", "dashboard-line-chart");
        chart.setTitle(null);
        chart.setLegendVisible(false);
        chart.setCreateSymbols(true);
        chart.setAnimated(false);
        chart.setAlternativeColumnFillVisible(false);
        chart.setAlternativeRowFillVisible(false);
        chart.setHorizontalGridLinesVisible(true);
        chart.setVerticalGridLinesVisible(false);
        chart.setPrefHeight(280);
        chart.setMinHeight(280);
        chart.setMaxHeight(280);
        return chart;
    }

    private javafx.scene.chart.BarChart<Number, String> createDashboardHorizontalBarChart(String xLabel, String yLabel) {
        javafx.scene.chart.NumberAxis xAxis = new javafx.scene.chart.NumberAxis();
        xAxis.setLabel(xLabel);
        xAxis.setAnimated(false);
        xAxis.setForceZeroInRange(true);
        xAxis.setMinorTickVisible(false);
        xAxis.setTickLabelFill(TEXT_MUTED_COLOR);
        xAxis.setTickLabelFont(javafx.scene.text.Font.font("Be Vietnam Pro", javafx.scene.text.FontWeight.MEDIUM, 11));

        javafx.scene.chart.CategoryAxis yAxis = new javafx.scene.chart.CategoryAxis();
        yAxis.setLabel(yLabel);
        yAxis.setAnimated(false);
        yAxis.setGapStartAndEnd(true);
        yAxis.setStartMargin(18);
        yAxis.setEndMargin(18);
        yAxis.setTickLabelFill(TEXT_MUTED_COLOR);
        yAxis.setTickLabelFont(javafx.scene.text.Font.font("Be Vietnam Pro", javafx.scene.text.FontWeight.MEDIUM, 11));

        javafx.scene.chart.BarChart<Number, String> chart = new javafx.scene.chart.BarChart<>(xAxis, yAxis) {
            @Override
            protected void layoutPlotChildren() {
                super.layoutPlotChildren();
                alignHorizontalBarCenters(this);
            }
        };
        chart.getStyleClass().addAll("dashboard-bar-chart", "horizontal-bar-chart");
        chart.setTitle(null);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setAlternativeColumnFillVisible(false);
        chart.setAlternativeRowFillVisible(false);
        chart.setHorizontalGridLinesVisible(true);
        chart.setVerticalGridLinesVisible(false);
        chart.setCategoryGap(12);
        chart.setBarGap(4);
        chart.setPrefHeight(260);
        chart.setMinHeight(260);
        chart.setMaxHeight(260);
        return chart;
    }

    private void configureDashboardCategoryAxis(javafx.scene.chart.CategoryAxis axis, java.util.List<String> categories) {
        axis.setCategories(javafx.collections.FXCollections.observableArrayList(categories));
        int maxLabelLength = categories.stream()
            .filter(java.util.Objects::nonNull)
            .mapToInt(String::length)
            .max()
            .orElse(0);
        boolean denseMonthlyLabels = categories.size() >= 10 || maxLabelLength >= 6;
        axis.setTickLabelRotation(denseMonthlyLabels ? -32 : 0);
        axis.setTickLabelGap(denseMonthlyLabels ? 8 : 4);
        axis.setTickLabelFont(javafx.scene.text.Font.font(
            "Be Vietnam Pro",
            javafx.scene.text.FontWeight.MEDIUM,
            denseMonthlyLabels ? 10 : 11
        ));
    }

    private void alignVerticalBarCenters(javafx.scene.chart.BarChart<String, Number> chart) {
        if (!(chart.getXAxis() instanceof javafx.scene.chart.CategoryAxis xAxis)) {
            return;
        }
        for (javafx.scene.chart.XYChart.Series<String, Number> series : chart.getData()) {
            for (javafx.scene.chart.XYChart.Data<String, Number> data : series.getData()) {
                javafx.scene.Node node = data.getNode();
                String category = data.getXValue();
                if (node == null || category == null) {
                    continue;
                }
                javafx.geometry.Bounds bounds = node.getBoundsInParent();
                double desiredCenterX = xAxis.getDisplayPosition(category);
                double currentCenterX = bounds.getMinX() + bounds.getWidth() / 2.0;
                node.setTranslateX(node.getTranslateX() + (desiredCenterX - currentCenterX));
            }
        }
    }

    private void alignHorizontalBarCenters(javafx.scene.chart.BarChart<Number, String> chart) {
        if (!(chart.getYAxis() instanceof javafx.scene.chart.CategoryAxis yAxis)) {
            return;
        }
        for (javafx.scene.chart.XYChart.Series<Number, String> series : chart.getData()) {
            for (javafx.scene.chart.XYChart.Data<Number, String> data : series.getData()) {
                javafx.scene.Node node = data.getNode();
                String category = data.getYValue();
                if (node == null || category == null) {
                    continue;
                }
                javafx.geometry.Bounds bounds = node.getBoundsInParent();
                double desiredCenterY = yAxis.getDisplayPosition(category);
                double currentCenterY = bounds.getMinY() + bounds.getHeight() / 2.0;
                node.setTranslateY(node.getTranslateY() + (desiredCenterY - currentCenterY));
            }
        }
    }

    private javafx.scene.chart.XYChart.Data<String, Number> createDashboardLineData(String category, Number value, String strokeValue, String tooltipText) {
        javafx.scene.chart.XYChart.Data<String, Number> data = new javafx.scene.chart.XYChart.Data<>(category, value);
        data.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) {
                newNode.setStyle(
                    "-fx-background-color: " + strokeValue + ", -app-surface; " +
                    "-fx-background-insets: 0, 2; " +
                    "-fx-background-radius: 5, 5; " +
                    "-fx-padding: 5;"
                );
                installTooltip(newNode, tooltipText);
            }
        });
        return data;
    }

    private void applyLineSeriesStyling(javafx.scene.chart.XYChart.Series<String, Number> series, String strokeValue) {
        series.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) {
                newNode.setStyle(
                    "-fx-stroke: " + strokeValue + "; " +
                    "-fx-stroke-width: 2.4px;"
                );
            }
        });
    }

    private javafx.scene.chart.XYChart.Data<Number, String> createDashboardHorizontalBarData(Number value, String category, String barFillValue, String tooltipText) {
        javafx.scene.chart.XYChart.Data<Number, String> data = new javafx.scene.chart.XYChart.Data<>(value, category);
        data.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) {
                newNode.setStyle("-fx-bar-fill: " + barFillValue + ";");
                installTooltip(newNode, tooltipText);
            }
        });
        return data;
    }

    private void configureDashboardVerticalValueAxis(javafx.scene.chart.XYChart<String, Number> chart, java.util.Collection<? extends Number> values, boolean wholeNumbers) {
        if (!(chart.getYAxis() instanceof javafx.scene.chart.NumberAxis yAxis)) {
            return;
        }
        double maxValue = values.stream()
            .filter(java.util.Objects::nonNull)
            .mapToDouble(Number::doubleValue)
            .max()
            .orElse(0.0);
        double upperBound;
        double tickUnit;
        if (wholeNumbers) {
            upperBound = maxValue <= 0 ? 1.0 : Math.max(1.0, Math.ceil(maxValue * 1.4));
            tickUnit = Math.max(1.0, Math.ceil(upperBound / 4.0));
        } else {
            upperBound = maxValue <= 0 ? 100.0 : Math.max(1.0, Math.ceil(maxValue * 1.15));
            tickUnit = Math.max(1.0, Math.ceil(upperBound / 5.0));
        }
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0.0);
        yAxis.setUpperBound(upperBound);
        yAxis.setTickUnit(tickUnit);
    }

    private void configureDashboardHorizontalValueAxis(javafx.scene.chart.BarChart<Number, String> chart, java.util.Collection<? extends Number> values, boolean wholeNumbers) {
        if (!(chart.getXAxis() instanceof javafx.scene.chart.NumberAxis xAxis)) {
            return;
        }
        double maxValue = values.stream()
            .filter(java.util.Objects::nonNull)
            .mapToDouble(Number::doubleValue)
            .max()
            .orElse(0.0);
        double upperBound;
        double tickUnit;
        if (wholeNumbers) {
            upperBound = maxValue <= 0 ? 1.0 : Math.max(1.0, Math.ceil(maxValue * 1.4));
            tickUnit = Math.max(1.0, Math.ceil(upperBound / 4.0));
        } else {
            upperBound = maxValue <= 0 ? 100.0 : Math.max(1.0, Math.ceil(maxValue * 1.15));
            tickUnit = Math.max(1.0, Math.ceil(upperBound / 5.0));
        }
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(0.0);
        xAxis.setUpperBound(upperBound);
        xAxis.setTickUnit(tickUnit);
    }

    private javafx.scene.Node createPaymentMethodShareContent(
        java.util.Map<com.pbl3.project.pbl3_project.entity.PaymentMethod, Long> paymentCounts,
        String emptyText
    ) {
        long total = paymentCounts.values().stream().mapToLong(Long::longValue).sum();
        if (total <= 0) {
            return createDashboardPlaceholder(emptyText);
        }

        javafx.scene.chart.PieChart pieChart = new javafx.scene.chart.PieChart();
        pieChart.getStyleClass().add("dashboard-payment-chart");
        pieChart.setLegendVisible(false);
        pieChart.setLabelsVisible(paymentCounts.values().stream().filter(count -> count != null && count > 0).count() > 1);
        pieChart.setClockwise(true);
        pieChart.setPrefHeight(260);
        pieChart.setMinHeight(260);
        pieChart.setMaxHeight(260);

        java.util.Map<com.pbl3.project.pbl3_project.entity.PaymentMethod, String> sliceColors = new java.util.LinkedHashMap<>();
        sliceColors.put(com.pbl3.project.pbl3_project.entity.PaymentMethod.CASH, PRIMARY_HEX);
        sliceColors.put(com.pbl3.project.pbl3_project.entity.PaymentMethod.CARD, SUCCESS_HEX);
        sliceColors.put(com.pbl3.project.pbl3_project.entity.PaymentMethod.TRANSFER, "#7c93a6");

        javafx.scene.layout.FlowPane legendPane = new javafx.scene.layout.FlowPane();
        legendPane.getStyleClass().add("dashboard-chart-legend");
        legendPane.setHgap(14);
        legendPane.setVgap(8);
        legendPane.setAlignment(Pos.CENTER);

        for (com.pbl3.project.pbl3_project.entity.PaymentMethod method : java.util.List.of(
            com.pbl3.project.pbl3_project.entity.PaymentMethod.CASH,
            com.pbl3.project.pbl3_project.entity.PaymentMethod.CARD,
            com.pbl3.project.pbl3_project.entity.PaymentMethod.TRANSFER
        )) {
            long count = paymentCounts.getOrDefault(method, 0L);
            if (count <= 0) {
                continue;
            }
            String color = sliceColors.getOrDefault(method, PRIMARY_HEX);
            javafx.scene.chart.PieChart.Data data = new javafx.scene.chart.PieChart.Data(formatPaymentMethodLabel(method), count);
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    newNode.setStyle("-fx-pie-color: " + color + ";");
                    double percentage = (count * 100.0) / total;
                    installTooltip(newNode, formatPaymentMethodLabel(method) + ": " + count + " orders (" + String.format("%.1f%%", percentage) + ")");
                }
            });
            pieChart.getData().add(data);

            javafx.scene.layout.HBox legendItem = new javafx.scene.layout.HBox(8);
            legendItem.setAlignment(Pos.CENTER_LEFT);
            javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(5, javafx.scene.paint.Color.web(color));
            javafx.scene.control.Label legendLabel = new javafx.scene.control.Label(
                formatPaymentMethodLabel(method) + " (" + count + ", " + String.format("%.1f%%", (count * 100.0) / total) + ")"
            );
            legendLabel.getStyleClass().add("dashboard-chart-legend-label");
            legendLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -app-text-secondary;");
            legendItem.getChildren().addAll(dot, legendLabel);
            legendPane.getChildren().add(legendItem);
        }

        if (pieChart.getData().isEmpty()) {
            return createDashboardPlaceholder(emptyText);
        }

        javafx.scene.layout.VBox wrapper = new javafx.scene.layout.VBox(12, pieChart, legendPane);
        wrapper.setAlignment(Pos.CENTER);
        return wrapper;
    }

    private javafx.scene.Node createTopSellingChartContent(java.util.List<com.pbl3.project.pbl3_project.dto.report.TopSellingProductRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return createDashboardPlaceholder("No top-selling data in the last 7 days");
        }

        javafx.scene.chart.BarChart<Number, String> chart = createDashboardHorizontalBarChart("Net Sold", "Product");
        if (chart.getYAxis() instanceof javafx.scene.chart.CategoryAxis topSellingYAxis) {
            java.util.List<String> categories = rows.stream()
                .map(row -> abbreviateLabel(row.productName(), 24))
                .toList();
            topSellingYAxis.setCategories(javafx.collections.FXCollections.observableArrayList(categories));
            topSellingYAxis.setTickLabelRotation(0);
            topSellingYAxis.setTickLabelGap(6);
            topSellingYAxis.setTickLabelFont(javafx.scene.text.Font.font(
                "Be Vietnam Pro",
                javafx.scene.text.FontWeight.MEDIUM,
                11
            ));
        }
        double chartHeight = Math.max(260, rows.size() * 44 + 80);
        chart.setPrefHeight(chartHeight);
        chart.setMinHeight(chartHeight);
        chart.setMaxHeight(chartHeight);
        javafx.scene.chart.XYChart.Series<Number, String> series = new javafx.scene.chart.XYChart.Series<>();
        for (com.pbl3.project.pbl3_project.dto.report.TopSellingProductRow row : rows) {
            String shortLabel = abbreviateLabel(row.productName(), 24);
            String tooltipText = row.productName()
                + "\nCategory: " + row.categoryName()
                + "\nNet Sold: " + row.netSoldQuantity()
                + "\nRevenue: " + formatVnd(row.netRevenue());
            series.getData().add(createDashboardHorizontalBarData(row.netSoldQuantity(), shortLabel, PRIMARY_BAR_FILL, tooltipText));
        }
        chart.getData().add(series);
        configureDashboardHorizontalValueAxis(chart, rows.stream().map(com.pbl3.project.pbl3_project.dto.report.TopSellingProductRow::netSoldQuantity).toList(), true);
        return chart;
    }

    private javafx.scene.Node createDashboardPlaceholder(String text) {
        Label placeholder = new Label(text);
        placeholder.getStyleClass().add("dashboard-placeholder-label");
        placeholder.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-muted;");

        javafx.scene.layout.StackPane wrapper = new javafx.scene.layout.StackPane(placeholder);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setMinHeight(260);
        wrapper.setPrefHeight(260);
        wrapper.setMaxHeight(260);
        wrapper.setStyle("-fx-background-color: derive(-app-surface-muted, 8%); -fx-background-radius: 12;");
        return wrapper;
    }

    private void makeDashboardDrillDown(javafx.scene.Node node, String tooltipText, Runnable action) {
        if (node == null || action == null) {
            return;
        }
        if (!node.getStyleClass().contains("dashboard-drilldown-target")) {
            node.getStyleClass().add("dashboard-drilldown-target");
        }
        node.setCursor(javafx.scene.Cursor.HAND);
        installTooltip(node, tooltipText);
        node.setOnMouseClicked(event -> {
            if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                action.run();
            }
        });
    }

    private void installDashboardPaneHover(javafx.scene.Node node) {
        if (node == null) {
            return;
        }
        final javafx.animation.Timeline[] hoverTimelineRef = new javafx.animation.Timeline[1];
        java.util.function.BiConsumer<Double, Double> animateTo = (scale, translateY) -> {
            if (isReducedMotionEnabled(node)) {
                if (hoverTimelineRef[0] != null) {
                    hoverTimelineRef[0].stop();
                }
                node.setScaleX(1.0);
                node.setScaleY(1.0);
                node.setTranslateY(0.0);
                return;
            }
            if (hoverTimelineRef[0] != null) {
                hoverTimelineRef[0].stop();
            }
            hoverTimelineRef[0] = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                    javafx.util.Duration.millis(170),
                    new javafx.animation.KeyValue(node.scaleXProperty(), scale, javafx.animation.Interpolator.EASE_BOTH),
                    new javafx.animation.KeyValue(node.scaleYProperty(), scale, javafx.animation.Interpolator.EASE_BOTH),
                    new javafx.animation.KeyValue(node.translateYProperty(), translateY, javafx.animation.Interpolator.EASE_BOTH)
                )
            );
            hoverTimelineRef[0].play();
        };

        node.setOnMouseEntered(e -> animateTo.accept(1.012, -4.0));
        node.setOnMouseExited(e -> animateTo.accept(1.0, 0.0));
        node.setOnMousePressed(e -> {
            if (!node.isHover()) {
                return;
            }
            animateTo.accept(1.006, -2.0);
        });
        node.setOnMouseReleased(e -> {
            if (node.isHover()) {
                animateTo.accept(1.012, -4.0);
            } else {
                animateTo.accept(1.0, 0.0);
            }
        });
    }

    private void scrollNodeIntoView(javafx.scene.control.ScrollPane scrollPane, javafx.scene.Node node) {
        if (scrollPane == null || node == null || scrollPane.getContent() == null) {
            return;
        }
        javafx.application.Platform.runLater(() -> {
            scrollPane.applyCss();
            scrollPane.layout();
            scrollPane.getContent().applyCss();
            if (scrollPane.getContent() instanceof javafx.scene.Parent contentParent) {
                contentParent.layout();
            }

            javafx.geometry.Bounds contentBounds = scrollPane.getContent().getLayoutBounds();
            javafx.geometry.Bounds viewportBounds = scrollPane.getViewportBounds();
            javafx.geometry.Bounds nodeBoundsInScene = node.localToScene(node.getBoundsInLocal());
            javafx.geometry.Bounds contentBoundsInScene = scrollPane.getContent().localToScene(scrollPane.getContent().getLayoutBounds());
            if (nodeBoundsInScene == null || contentBoundsInScene == null) {
                return;
            }
            double nodeMinYInContent = nodeBoundsInScene.getMinY() - contentBoundsInScene.getMinY();
            double availableHeight = contentBounds.getHeight() - viewportBounds.getHeight();
            if (availableHeight <= 0) {
                scrollPane.setVvalue(0);
                return;
            }
            double topOffset = 12.0;
            double targetVvalue = Math.max(0.0, Math.min(1.0, (nodeMinYInContent - topOffset) / availableHeight));
            scrollPane.setVvalue(targetVvalue);
        });
    }

    private void revealReportSection(javafx.scene.control.ScrollPane scrollPane, javafx.scene.Node node) {
        if (scrollPane == null || node == null) {
            return;
        }
        scrollNodeIntoView(scrollPane, node);

        javafx.animation.PauseTransition secondPass = new javafx.animation.PauseTransition(javafx.util.Duration.millis(90));
        secondPass.setOnFinished(event -> scrollNodeIntoView(scrollPane, node));
        secondPass.play();

        javafx.animation.PauseTransition thirdPass = new javafx.animation.PauseTransition(javafx.util.Duration.millis(190));
        thirdPass.setOnFinished(event -> scrollNodeIntoView(scrollPane, node));
        thirdPass.play();
    }

    private String buildOperationalReportContextLabel(com.pbl3.project.pbl3_project.dto.report.OperationalReportData reportData) {
        return "Sales: "
            + formatOperationalReportRangeLabel(reportData.salesMix().startDate(), reportData.salesMix().endDate())
            + " | Inventory: Current snapshot";
    }

    private void installTooltip(javafx.scene.Node node, String text) {
        javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip(text);
        tooltip.setShowDelay(javafx.util.Duration.millis(120));
        javafx.scene.control.Tooltip.install(node, tooltip);
    }

    private String formatDashboardCurrencyDelta(BigDecimal delta) {
        BigDecimal normalizedDelta = MoneySupport.normalize(delta);
        if (normalizedDelta.signum() == 0) {
            return "No change vs yesterday";
        }
        String sign = normalizedDelta.signum() > 0 ? "+" : "-";
        return sign + formatVnd(normalizedDelta.abs()) + " vs yesterday";
    }

    private String formatDashboardCountDelta(long delta, String noun) {
        if (delta == 0) {
            return "No change vs yesterday";
        }
        String sign = delta > 0 ? "+" : "-";
        return sign + Math.abs(delta) + " " + noun + " vs yesterday";
    }

    private String getDashboardDeltaColor(BigDecimal delta, boolean higherIsBetter) {
        BigDecimal normalizedDelta = MoneySupport.normalize(delta);
        if (normalizedDelta.signum() == 0) {
            return "-app-text-muted";
        }
        boolean positiveDirection = higherIsBetter ? normalizedDelta.signum() > 0 : normalizedDelta.signum() < 0;
        return positiveDirection ? "-app-success-hover" : "-app-danger-hover";
    }

    private String getDashboardDeltaColor(long delta, boolean higherIsBetter) {
        if (delta == 0) {
            return "-app-text-muted";
        }
        boolean positiveDirection = higherIsBetter ? delta > 0 : delta < 0;
        return positiveDirection ? "-app-success-hover" : "-app-danger-hover";
    }

    private String formatPaymentMethodLabel(com.pbl3.project.pbl3_project.entity.PaymentMethod method) {
        if (method == null) {
            return "Unknown";
        }
        return switch (method) {
            case CASH -> "Cash";
            case CARD -> "Card";
            case TRANSFER -> "Transfer";
        };
    }

    private String abbreviateLabel(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text != null ? text : "-";
        }
        return text.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private Button createReportExportButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().addAll("primary-button", "report-export-button");
        button.setFocusTraversable(false);
        button.setStyle("-fx-background-radius: 999; -fx-padding: 8 16; -fx-font-size: 13px;");
        return button;
    }

    private Button createReportPresetButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().addAll("button", "report-preset-button", "dashboard-report-secondary-button");
        button.setFocusTraversable(false);
        button.setStyle("-fx-background-radius: 999; -fx-padding: 6 14; -fx-font-size: 12px;");
        return button;
    }

    private VBox createReportSection(String title, String subtitle, javafx.scene.Node content, Button actionButton, String badgeText) {
        VBox section = new VBox(14);
        section.getStyleClass().add("report-section-card");
        if (badgeText != null && !badgeText.isBlank()) {
            section.getStyleClass().add("report-section-active");
        }
        section.setPadding(new Insets(18));
        section.setMinWidth(0);
        section.setFillWidth(true);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("report-section-title");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(Double.MAX_VALUE);

        VBox titleBox = new VBox(4);
        titleBox.getChildren().add(titleLabel);
        if (subtitle != null && !subtitle.isBlank()) {
            Label subtitleLabel = new Label(subtitle);
            subtitleLabel.getStyleClass().add("report-section-subtitle");
            subtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -app-text-muted;");
            subtitleLabel.setWrapText(true);
            subtitleLabel.setMaxWidth(Double.MAX_VALUE);
            subtitleLabel.prefWidthProperty().bind(titleBox.widthProperty());
            subtitleLabel.maxWidthProperty().bind(titleBox.widthProperty());
            titleBox.getChildren().add(subtitleLabel);
        }
        titleBox.setMinWidth(0);
        titleBox.setMaxWidth(Double.MAX_VALUE);
        titleBox.setFillWidth(true);

        javafx.scene.layout.HBox headerActions = new javafx.scene.layout.HBox(10);
        headerActions.setAlignment(Pos.TOP_RIGHT);
        headerActions.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        headerActions.setPrefWidth(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
        if (badgeText != null && !badgeText.isBlank()) {
            Label badgeLabel = new Label(badgeText);
            badgeLabel.getStyleClass().add("report-section-focus-badge");
            headerActions.getChildren().add(badgeLabel);
        }
        if (actionButton != null) {
            headerActions.getChildren().add(actionButton);
        }

        javafx.scene.layout.HBox.setHgrow(titleBox, javafx.scene.layout.Priority.ALWAYS);

        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(12);
        header.setAlignment(Pos.TOP_LEFT);
        header.setMinWidth(0);
        header.setMaxWidth(Double.MAX_VALUE);
        header.getChildren().add(titleBox);
        if (!headerActions.getChildren().isEmpty()) {
            header.getChildren().add(headerActions);
        }

        titleLabel.prefWidthProperty().bind(titleBox.widthProperty());
        titleLabel.maxWidthProperty().bind(titleBox.widthProperty());

        VBox.setVgrow(content, javafx.scene.layout.Priority.ALWAYS);
        section.getChildren().addAll(header, content);
        return section;
    }

    private ReportFocusTarget mapInsightDrilldownTarget(com.pbl3.project.pbl3_project.dto.report.InsightDrilldownTarget target) {
        if (target == null) {
            return null;
        }
        return switch (target) {
            case SUMMARY -> ReportFocusTarget.SUMMARY;
            case ACTION_CENTER -> ReportFocusTarget.ACTION_CENTER;
            case REORDER -> ReportFocusTarget.REORDER;
            case WHAT_CHANGED -> ReportFocusTarget.WHAT_CHANGED;
            case REVENUE -> ReportFocusTarget.REVENUE;
            case ORDERS -> ReportFocusTarget.ORDERS;
            case CANCELED_ORDERS -> ReportFocusTarget.CANCELED_ORDERS;
            case TOP_SELLING -> ReportFocusTarget.TOP_SELLING;
            case AGING_STOCK -> ReportFocusTarget.AGING_STOCK;
        };
    }

    private Runnable createDashboardInsightAction(
        Stage stage,
        com.pbl3.project.pbl3_project.entity.User user,
        com.pbl3.project.pbl3_project.dto.report.InsightDrilldownTarget target
    ) {
        if (target == null || !authorizationService.canAccessReports(user)) {
            return null;
        }
        ReportFocusTarget focusTarget = mapInsightDrilldownTarget(target);
        if (focusTarget == null) {
            return null;
        }

        java.time.LocalDate startDate = null;
        java.time.LocalDate endDate = null;
        if (target == com.pbl3.project.pbl3_project.dto.report.InsightDrilldownTarget.REVENUE
            || target == com.pbl3.project.pbl3_project.dto.report.InsightDrilldownTarget.ORDERS
            || target == com.pbl3.project.pbl3_project.dto.report.InsightDrilldownTarget.CANCELED_ORDERS
            || target == com.pbl3.project.pbl3_project.dto.report.InsightDrilldownTarget.TOP_SELLING
            || target == com.pbl3.project.pbl3_project.dto.report.InsightDrilldownTarget.WHAT_CHANGED
            || target == com.pbl3.project.pbl3_project.dto.report.InsightDrilldownTarget.SUMMARY) {
            startDate = java.time.LocalDate.now();
            endDate = java.time.LocalDate.now();
        }

        java.time.LocalDate finalStartDate = startDate;
        java.time.LocalDate finalEndDate = endDate;
        return () -> showOperationalReportsScene(stage, user, finalStartDate, finalEndDate, focusTarget);
    }

    private Runnable createReportInsightAction(
        javafx.scene.control.ScrollPane scrollPane,
        java.util.Map<ReportFocusTarget, javafx.scene.Node> anchors,
        com.pbl3.project.pbl3_project.dto.report.InsightDrilldownTarget target
    ) {
        ReportFocusTarget focusTarget = mapInsightDrilldownTarget(target);
        if (focusTarget == null) {
            return null;
        }
        return () -> {
            javafx.scene.Node node = anchors.get(focusTarget);
            if (node != null) {
                revealReportSection(scrollPane, node);
            }
        };
    }

    private Runnable createActionCenterItemAction(
        Stage stage,
        com.pbl3.project.pbl3_project.entity.User user,
        com.pbl3.project.pbl3_project.dto.report.ActionCenterItem item,
        java.util.function.Function<com.pbl3.project.pbl3_project.dto.report.InsightDrilldownTarget, Runnable> drilldownResolver
    ) {
        if (item == null) {
            return null;
        }
        if (item.type() == com.pbl3.project.pbl3_project.dto.report.ActionCenterType.REORDER_NOW
            && item.productId() != null
            && item.suggestedQuantity() != null
            && item.suggestedQuantity() > 0) {
            ImportOrderPrefill prefill = new ImportOrderPrefill(item.productId(), item.suggestedQuantity());
            return () -> showImportOrderScene(stage, user, prefill);
        }
        return drilldownResolver != null ? drilldownResolver.apply(item.drilldownTarget()) : null;
    }

    private Runnable createReorderAction(
        Stage stage,
        com.pbl3.project.pbl3_project.entity.User user,
        com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow row
    ) {
        if (row == null || row.productId() == null || row.suggestedReorderQty() <= 0) {
            return null;
        }
        ImportOrderPrefill prefill = new ImportOrderPrefill(row.productId(), row.suggestedReorderQty());
        return () -> showImportOrderScene(stage, user, prefill);
    }

    private javafx.scene.Node createWhatChangedContent(
        com.pbl3.project.pbl3_project.dto.report.WhatChangedSnapshot snapshot,
        java.util.function.Function<com.pbl3.project.pbl3_project.dto.report.InsightDrilldownTarget, Runnable> actionResolver,
        String emptyText,
        javafx.beans.value.ObservableNumberValue widthSource
    ) {
        if (snapshot == null || snapshot.insights() == null || snapshot.insights().isEmpty()) {
            return createCompactInsightPlaceholder(emptyText);
        }

        java.util.List<VBox> cards = new java.util.ArrayList<>();

        for (com.pbl3.project.pbl3_project.dto.report.WhatChangedInsight insight : snapshot.insights()) {
            Runnable action = actionResolver != null ? actionResolver.apply(insight.drilldownTarget()) : null;
            cards.add(createInsightDigestCard(
                createWhatChangedInsightIcon(insight.type(), insight.severity()),
                formatWhatChangedType(insight.type()),
                insight.headline(),
                insight.detail(),
                insight.severity(),
                extractWhatChangedDeltaChip(insight),
                action != null ? "Open" : null,
                action,
                true
            ));
        }
        return createResponsiveInsightGrid(cards, widthSource, 860.0);
    }

    private javafx.scene.Node createActionCenterContent(
        Stage stage,
        com.pbl3.project.pbl3_project.entity.User user,
        com.pbl3.project.pbl3_project.dto.report.ActionCenterSnapshot snapshot,
        int maxItems,
        java.util.function.Function<com.pbl3.project.pbl3_project.dto.report.InsightDrilldownTarget, Runnable> drilldownResolver
    ) {
        if (snapshot == null || snapshot.items() == null || snapshot.items().isEmpty()) {
            return createCompactInsightPlaceholder("No action items right now");
        }

        VBox content = new VBox(10);
        content.setFillWidth(true);

        int limit = Math.min(maxItems, snapshot.items().size());
        java.util.List<com.pbl3.project.pbl3_project.dto.report.ActionCenterItem> visibleItems =
            snapshot.items().subList(0, limit);
        java.util.List<com.pbl3.project.pbl3_project.dto.report.InsightSeverity> severityOrder = java.util.List.of(
            com.pbl3.project.pbl3_project.dto.report.InsightSeverity.CRITICAL,
            com.pbl3.project.pbl3_project.dto.report.InsightSeverity.WARNING,
            com.pbl3.project.pbl3_project.dto.report.InsightSeverity.INFO
        );

        for (com.pbl3.project.pbl3_project.dto.report.InsightSeverity severity : severityOrder) {
            java.util.List<com.pbl3.project.pbl3_project.dto.report.ActionCenterItem> severityItems = visibleItems.stream()
                .filter(item -> item.severity() == severity)
                .toList();
            if (severityItems.isEmpty()) {
                continue;
            }

            VBox groupBox = new VBox(10);
            groupBox.setFillWidth(true);
            for (com.pbl3.project.pbl3_project.dto.report.ActionCenterItem item : severityItems) {
                Runnable action = createActionCenterItemAction(stage, user, item, drilldownResolver);
                groupBox.getChildren().add(createInsightDigestCard(
                    createActionCenterInsightIcon(item.type(), item.severity()),
                    formatActionCenterType(item.type()),
                    item.title(),
                    item.description(),
                    item.severity(),
                    createActionCenterImpactChip(item),
                    formatInsightActionLabel(item.actionLabel()),
                    action,
                    true
                ));
            }
            boolean collapsible = severity == com.pbl3.project.pbl3_project.dto.report.InsightSeverity.CRITICAL
                || severity == com.pbl3.project.pbl3_project.dto.report.InsightSeverity.WARNING;
            content.getChildren().add(createInsightSeverityGroup(
                severity,
                severityItems.size(),
                groupBox,
                collapsible,
                collapsible
            ));
        }
        return content;
    }

    private javafx.scene.Node createExplainableReorderContent(
        Stage stage,
        com.pbl3.project.pbl3_project.entity.User user,
        com.pbl3.project.pbl3_project.dto.report.ExplainableReorderSnapshot snapshot,
        int maxItems,
        boolean detailed
    ) {
        java.util.List<com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow> rows =
            snapshot != null && snapshot.rows() != null ? snapshot.rows() : java.util.List.of();
        if (rows.isEmpty()) {
            return createCompactInsightPlaceholder("No reorder candidates right now");
        }

        java.util.List<com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow> limitedRows =
            rows.subList(0, Math.min(maxItems, rows.size()));

        return detailed
            ? createExplainableReorderDetailedContent(stage, user, limitedRows)
            : createExplainableReorderSummaryContent(stage, user, limitedRows);
    }

    private void configureExplainableReorderColumn(
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow, ?> column,
        double minWidth,
        double prefWidth,
        boolean resizable
    ) {
        column.setMinWidth(minWidth);
        column.setPrefWidth(prefWidth);
        column.setResizable(resizable);
    }

    private javafx.scene.Node createExplainableReorderSummaryContent(
        Stage stage,
        com.pbl3.project.pbl3_project.entity.User user,
        java.util.List<com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow> rows
    ) {
        VBox content = new VBox(10);
        content.setFillWidth(true);
        for (com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow row : rows) {
            content.getChildren().add(createExplainableReorderSummaryCard(stage, user, row));
        }
        return content;
    }

    private javafx.scene.Node createExplainableReorderDetailedContent(
        Stage stage,
        com.pbl3.project.pbl3_project.entity.User user,
        java.util.List<com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow> rows
    ) {
        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow> table =
            new javafx.scene.control.TableView<>();
        enableDragSelection(table);
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setItems(javafx.collections.FXCollections.observableArrayList(rows));
        double visibleRowCount = Math.max(1, Math.min(6, rows.size()));
        double targetTableHeight = 58 + visibleRowCount * 44;
        table.setMinHeight(targetTableHeight);
        table.setPrefHeight(targetTableHeight);
        table.setMaxHeight(targetTableHeight);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow, String> productCol =
            new javafx.scene.control.TableColumn<>("Product");
        productCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().productName()));
        productCol.setCellFactory(createExplainableReorderTextCell(Pos.CENTER_LEFT));
        configureExplainableReorderColumn(productCol, 220, 260, true);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow, Number> onHandCol =
            new javafx.scene.control.TableColumn<>("On Hand");
        onHandCol.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().onHandQuantity()));
        onHandCol.setStyle("-fx-alignment: CENTER;");
        configureExplainableReorderColumn(onHandCol, 86, 96, false);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow, String> avgCol =
            new javafx.scene.control.TableColumn<>("Avg/Day");
        avgCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            formatCompactDecimal(data.getValue().avgDailyUnits14d())
        ));
        avgCol.setStyle("-fx-alignment: CENTER;");
        configureExplainableReorderColumn(avgCol, 94, 104, false);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow, String> coverageCol =
            new javafx.scene.control.TableColumn<>("Coverage");
        coverageCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatExplainableCoverageShort(data.getValue())));
        coverageCol.setCellFactory(createExplainableCoverageCell());
        coverageCol.setStyle("-fx-alignment: CENTER;");
        configureExplainableReorderColumn(coverageCol, 112, 126, false);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow, Number> suggestedCol =
            new javafx.scene.control.TableColumn<>("Suggested");
        suggestedCol.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().suggestedReorderQty()));
        suggestedCol.setStyle("-fx-alignment: CENTER;");
        configureExplainableReorderColumn(suggestedCol, 98, 108, false);

        table.getColumns().addAll(productCol, onHandCol, avgCol, coverageCol, suggestedCol);

        VBox detailPane = new VBox();
        detailPane.setFillWidth(true);
        detailPane.setMinWidth(0);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldRow, newRow) ->
            updateExplainableReorderDetailPane(detailPane, stage, user, newRow)
        );

        updateExplainableReorderDetailPane(detailPane, stage, user, null);

        VBox content = new VBox(12, table, detailPane);
        content.setFillWidth(true);
        enableDeselectOnOutsideClick(content, table);
        return content;
    }

    private void updateExplainableReorderDetailPane(
        VBox detailPane,
        Stage stage,
        com.pbl3.project.pbl3_project.entity.User user,
        com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow row
    ) {
        detailPane.getChildren().setAll(
            row != null
                ? createExplainableReorderDetailCard(stage, user, row)
                : createCompactInsightPlaceholder("Select a product to view reorder details")
        );
    }

    private VBox createExplainableReorderSummaryCard(
        Stage stage,
        com.pbl3.project.pbl3_project.entity.User user,
        com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow row
    ) {
        VBox card = new VBox(12);
        card.getStyleClass().addAll("insight-card", "reorder-summary-card");
        card.setFillWidth(true);

        Label titleLabel = new Label(row.productName());
        titleLabel.getStyleClass().add("insight-card-title");
        titleLabel.setWrapText(true);

        VBox titleBox = new VBox(4);
        titleBox.setFillWidth(true);
        titleBox.getChildren().add(titleLabel);
        if (row.categoryName() != null && !row.categoryName().isBlank()) {
            Label categoryLabel = new Label(row.categoryName());
            categoryLabel.getStyleClass().add("reorder-summary-subtitle");
            titleBox.getChildren().add(categoryLabel);
        }

        javafx.scene.layout.FlowPane metrics = createExplainableReorderMetricFlow(row);

        Label explanationLabel = new Label(row.explanation());
        explanationLabel.getStyleClass().add("insight-card-detail");
        explanationLabel.setWrapText(true);

        javafx.scene.layout.HBox footer = new javafx.scene.layout.HBox(12);
        footer.setAlignment(Pos.CENTER_LEFT);

        String supportText = buildExplainableReorderSupportText(row);
        if (!supportText.isBlank()) {
            Label supportLabel = new Label(supportText);
            supportLabel.getStyleClass().add("reorder-summary-support");
            supportLabel.setWrapText(true);
            supportLabel.setMaxWidth(Double.MAX_VALUE);
            javafx.scene.layout.HBox.setHgrow(supportLabel, javafx.scene.layout.Priority.ALWAYS);
            footer.getChildren().add(supportLabel);
        }

        Runnable action = createReorderAction(stage, user, row);
        Button actionButton = createImportActionButton(action, row.suggestedReorderQty() <= 0);
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        footer.getChildren().addAll(spacer, actionButton);

        card.getChildren().addAll(titleBox, metrics, explanationLabel, footer);
        return card;
    }

    private VBox createExplainableReorderDetailCard(
        Stage stage,
        com.pbl3.project.pbl3_project.entity.User user,
        com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow row
    ) {
        VBox card = new VBox(14);
        card.getStyleClass().addAll("insight-card", "reorder-detail-card");
        card.setFillWidth(true);

        Label titleLabel = new Label(row.productName());
        titleLabel.getStyleClass().add("insight-card-title");
        titleLabel.setWrapText(true);

        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getChildren().add(titleLabel);
        if (row.categoryName() != null && !row.categoryName().isBlank()) {
            Label categoryChip = new Label(row.categoryName());
            categoryChip.getStyleClass().add("reorder-category-chip");
            header.getChildren().add(categoryChip);
        }

        javafx.scene.layout.FlowPane metrics = createExplainableReorderMetricFlow(row);
        metrics.getChildren().add(createExplainableReorderMetricChip("Min", String.valueOf(row.minStockLevel()), false));

        javafx.scene.layout.FlowPane supportFlow = new javafx.scene.layout.FlowPane();
        supportFlow.setHgap(10);
        supportFlow.setVgap(10);
        supportFlow.setPrefWrapLength(720);
        supportFlow.getChildren().addAll(
            createExplainableReorderSupportCard("Last Inbound", row.lastInboundAt() != null ? formatDateTime(row.lastInboundAt()) : "-"),
            createExplainableReorderSupportCard("Latest Supplier", row.latestSupplierName() != null && !row.latestSupplierName().isBlank() ? row.latestSupplierName() : "-"),
            createExplainableReorderSupportCard("Latest Price", row.latestImportPrice() != null ? formatVnd(row.latestImportPrice()) : "-")
        );

        Label explanationTitle = new Label("Why This Reorder Is Suggested");
        explanationTitle.getStyleClass().add("reorder-detail-heading");

        Label explanationLabel = new Label(row.explanation());
        explanationLabel.getStyleClass().add("insight-card-detail");
        explanationLabel.setWrapText(true);

        Runnable action = createReorderAction(stage, user, row);
        Button actionButton = createImportActionButton(action, row.suggestedReorderQty() <= 0);

        javafx.scene.layout.HBox footer = new javafx.scene.layout.HBox(actionButton);
        footer.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(header, metrics, supportFlow, explanationTitle, explanationLabel, footer);
        return card;
    }

    private javafx.scene.layout.FlowPane createExplainableReorderMetricFlow(
        com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow row
    ) {
        javafx.scene.layout.FlowPane metrics = new javafx.scene.layout.FlowPane();
        metrics.setHgap(8);
        metrics.setVgap(8);
        metrics.setPrefWrapLength(720);
        metrics.getChildren().addAll(
            createExplainableReorderMetricChip("On Hand", String.valueOf(row.onHandQuantity()), false),
            createExplainableReorderMetricChip("Avg/Day", formatCompactDecimal(row.avgDailyUnits14d()), false),
            createExplainableCoverageMetric(row),
            createExplainableReorderMetricChip("Suggested", String.valueOf(row.suggestedReorderQty()), true)
        );
        return metrics;
    }

    private Label createExplainableReorderMetricChip(String label, String value, boolean emphasized) {
        Label chip = new Label(label + ": " + value);
        chip.getStyleClass().add(emphasized ? "reorder-metric-chip-strong" : "reorder-metric-chip");
        return chip;
    }

    private VBox createExplainableReorderSupportCard(String label, String value) {
        VBox card = new VBox(4);
        card.getStyleClass().add("reorder-support-card");

        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("reorder-support-label");

        Label valueNode = new Label(value);
        valueNode.getStyleClass().add("reorder-support-value");
        valueNode.setWrapText(true);

        card.getChildren().addAll(labelNode, valueNode);
        return card;
    }

    private String buildExplainableReorderSupportText(
        com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow row
    ) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        if (row.lastInboundAt() != null) {
            parts.add("Last inbound " + DISPLAY_DATE_FORMATTER.format(row.lastInboundAt().toLocalDate()));
        }
        if (row.latestSupplierName() != null && !row.latestSupplierName().isBlank()) {
            parts.add("Supplier " + row.latestSupplierName());
        }
        return String.join(" • ", parts);
    }

    private String formatExplainableCoverage(
        com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow row
    ) {
        return row != null && row.coverageKnown() && row.coverageDays() != null
            ? formatCompactDecimal(row.coverageDays()) + " days"
            : "Unknown";
    }

    private String formatExplainableCoverageShort(
        com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow row
    ) {
        return row != null && row.coverageKnown() && row.coverageDays() != null
            ? formatCompactDecimal(row.coverageDays()) + "d"
            : "Unknown";
    }

    private String getExplainableCoverageTone(
        com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow row
    ) {
        if (row == null || !row.coverageKnown() || row.coverageDays() == null) {
            return "unknown";
        }
        java.math.BigDecimal coverage = row.coverageDays();
        if (row.onHandQuantity() <= row.minStockLevel() || coverage.compareTo(BigDecimal.valueOf(3)) < 0) {
            return "critical";
        }
        if (coverage.compareTo(BigDecimal.valueOf(7)) < 0) {
            return "warning";
        }
        if (coverage.compareTo(BigDecimal.valueOf(14)) < 0) {
            return "watch";
        }
        return "stable";
    }

    private double getExplainableCoverageProgress(
        com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow row
    ) {
        if (row == null || !row.coverageKnown() || row.coverageDays() == null) {
            return 0.16;
        }
        return Math.max(0.08, Math.min(1.0, row.coverageDays().doubleValue() / 14.0));
    }

    private javafx.scene.Node createExplainableCoverageMetric(
        com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow row
    ) {
        String tone = getExplainableCoverageTone(row);

        Label titleLabel = new Label("Coverage");
        titleLabel.getStyleClass().add("reorder-coverage-label");

        Label valueLabel = new Label(formatExplainableCoverageShort(row));
        valueLabel.getStyleClass().addAll("reorder-coverage-value", "reorder-coverage-value-" + tone);

        javafx.scene.layout.Region track = new javafx.scene.layout.Region();
        track.getStyleClass().add("reorder-coverage-track");
        track.setMinSize(56, 6);
        track.setPrefSize(56, 6);
        track.setMaxSize(56, 6);

        javafx.scene.layout.Region fill = new javafx.scene.layout.Region();
        fill.getStyleClass().addAll("reorder-coverage-fill", "reorder-coverage-fill-" + tone);
        double fillWidth = 56 * getExplainableCoverageProgress(row);
        fill.setMinSize(fillWidth, 6);
        fill.setPrefSize(fillWidth, 6);
        fill.setMaxSize(fillWidth, 6);

        javafx.scene.layout.StackPane bar = new javafx.scene.layout.StackPane(track, fill);
        bar.setAlignment(Pos.CENTER_LEFT);
        javafx.scene.layout.StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        bar.setMinSize(javafx.scene.layout.Region.USE_PREF_SIZE, javafx.scene.layout.Region.USE_PREF_SIZE);
        bar.setPrefSize(javafx.scene.layout.Region.USE_COMPUTED_SIZE, javafx.scene.layout.Region.USE_COMPUTED_SIZE);

        VBox signal = new VBox(4, titleLabel, valueLabel, bar);
        signal.getStyleClass().add("reorder-coverage-signal");
        signal.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        return signal;
    }

    private com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow rowValue(
        javafx.scene.control.TableColumn.CellDataFeatures<
            com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow,
            String
        > data
    ) {
        return data != null ? data.getValue() : null;
    }

    private javafx.util.Callback<
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow, String>,
        javafx.scene.control.TableCell<com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow, String>
    > createExplainableCoverageCell() {
        return col -> new javafx.scene.control.TableCell<>() {
            private final Label badge = new Label();

            {
                setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
                setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setTooltip(null);
                    return;
                }
                com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow row =
                    getTableRow() != null ? getTableRow().getItem() : null;
                String tone = getExplainableCoverageTone(row);
                badge.setText(item);
                badge.getStyleClass().setAll("reorder-coverage-badge", "reorder-coverage-badge-" + tone);
                setTooltip(new javafx.scene.control.Tooltip(formatExplainableCoverage(row)));
                setGraphic(badge);
            }
        };
    }

    private javafx.util.Callback<
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow, String>,
        javafx.scene.control.TableCell<com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow, String>
    > createExplainableReorderTextCell(Pos alignment) {
        return col -> new javafx.scene.control.TableCell<>() {
            private final Label label = new Label();
            private final javafx.scene.layout.StackPane wrapper = new javafx.scene.layout.StackPane(label);

            {
                label.setMaxWidth(Double.MAX_VALUE);
                label.setWrapText(false);
                label.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
                wrapper.setPadding(Insets.EMPTY);
                wrapper.setMaxWidth(Double.MAX_VALUE);
                wrapper.prefWidthProperty().bind(javafx.beans.binding.Bindings.max(0.0, widthProperty().subtract(16)));
                javafx.scene.layout.StackPane.setAlignment(label, alignment);
                setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
                setAlignment(alignment);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setTooltip(null);
                    return;
                }
                label.setText(item);
                setTooltip(item.isBlank() ? null : new javafx.scene.control.Tooltip(item));
                setGraphic(wrapper);
            }
        };
    }

    private VBox createInsightDigestCard(
        javafx.scene.Node leadingIcon,
        String typeLabel,
        String title,
        String detail,
        com.pbl3.project.pbl3_project.dto.report.InsightSeverity severity,
        javafx.scene.Node secondaryChip,
        String actionLabel,
        Runnable action,
        boolean iconOnlyAction
    ) {
        com.pbl3.project.pbl3_project.dto.report.InsightSeverity safeSeverity =
            severity != null ? severity : com.pbl3.project.pbl3_project.dto.report.InsightSeverity.INFO;
        VBox card = new VBox();
        card.getStyleClass().addAll("insight-card", "insight-digest-card");
        card.setFillWidth(true);
        card.setMaxWidth(Double.MAX_VALUE);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().addAll("insight-card-title", "insight-digest-title");
        titleLabel.setWrapText(true);

        javafx.scene.Node severityBadge = createInsightSeverityBadgeIcon(safeSeverity);

        javafx.scene.layout.HBox metaRow = new javafx.scene.layout.HBox(8);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        if (typeLabel != null && !typeLabel.isBlank()) {
            metaRow.getChildren().add(createInsightMetaChip(typeLabel, false));
        }
        if (secondaryChip != null) {
            metaRow.getChildren().add(secondaryChip);
        }
        javafx.scene.layout.Region metaSpacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(metaSpacer, javafx.scene.layout.Priority.ALWAYS);
        metaRow.getChildren().addAll(metaSpacer, severityBadge);

        Label detailLabel = new Label(detail);
        detailLabel.getStyleClass().addAll("insight-card-detail", "insight-digest-detail");
        detailLabel.setWrapText(false);
        detailLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        detailLabel.setText(createInsightPreviewText(detail, 116));
        if (detail != null && !detail.isBlank()) {
            installTooltip(detailLabel, detail);
        }

        VBox body = new VBox(8, metaRow, titleLabel, detailLabel);
        body.getStyleClass().add("insight-digest-body");
        if (leadingIcon != null) {
            body.getStyleClass().add("insight-digest-body-leading");
        }
        body.setFillWidth(true);
        body.setMinWidth(0);

        if (actionLabel != null && !actionLabel.isBlank() && action != null) {
            Button actionButton = iconOnlyAction
                ? createInsightIconActionButton(actionLabel, action)
                : createInsightTextActionButton(actionLabel, action);
            metaRow.getChildren().add(actionButton);
        }

        javafx.scene.layout.StackPane row = new javafx.scene.layout.StackPane();
        row.setAlignment(Pos.TOP_LEFT);
        row.setMinWidth(0);
        row.setMaxWidth(Double.MAX_VALUE);
        applyRoundedRegionClip(row, 16);
        javafx.scene.layout.StackPane.setAlignment(body, Pos.TOP_LEFT);
        row.getChildren().add(body);
        if (leadingIcon != null) {
            javafx.scene.layout.StackPane iconWrap = wrapInsightDigestIcon(leadingIcon, safeSeverity);
            javafx.scene.layout.StackPane.setAlignment(iconWrap, Pos.TOP_LEFT);
            javafx.scene.layout.StackPane.setMargin(iconWrap, new Insets(2, 0, 0, 2));
            row.getChildren().add(iconWrap);
        }

        card.getChildren().add(row);
        return card;
    }

    private Button createInsightTextActionButton(String actionLabel, Runnable action) {
        Button actionButton = new Button(actionLabel);
        actionButton.getStyleClass().addAll(
            "button",
            "dashboard-report-secondary-button",
            "insight-card-action-button",
            "insight-digest-action-button"
        );
        actionButton.setOnAction(e -> action.run());
        return actionButton;
    }

    private Button createInsightIconActionButton(String actionLabel, Runnable action) {
        Button actionButton = new Button();
        actionButton.getStyleClass().add("insight-icon-action-button");
        actionButton.setGraphic(createInsightActionIcon(actionLabel));
        actionButton.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        actionButton.setFocusTraversable(false);
        actionButton.setOnAction(e -> action.run());
        installTooltip(actionButton, actionLabel);
        return actionButton;
    }

    private javafx.scene.Node createInsightActionIcon(String actionLabel) {
        String normalized = actionLabel != null ? actionLabel.trim().toLowerCase(java.util.Locale.ROOT) : "";
        if ("import".equals(normalized) || normalized.contains("import")) {
            return createImportActionIcon();
        }
        return createInsightOpenActionIcon();
    }

    private Button createImportActionButton(Runnable action, boolean disabled) {
        Button actionButton = new Button();
        actionButton.getStyleClass().add("insight-icon-action-button");
        actionButton.setGraphic(createImportActionIcon());
        actionButton.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        actionButton.setFocusTraversable(false);
        actionButton.setDisable(disabled);
        actionButton.setOnAction(e -> {
            if (action != null) {
                action.run();
            }
        });
        installTooltip(actionButton, "Open Import");
        return actionButton;
    }

    private javafx.scene.Node createInsightOpenActionIcon() {
        javafx.scene.shape.SVGPath elbow = new javafx.scene.shape.SVGPath();
        elbow.setContent("M7 7h10v10");
        elbow.getStyleClass().add("insight-open-action-icon-stroke");
        elbow.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.shape.SVGPath arrow = new javafx.scene.shape.SVGPath();
        arrow.setContent("M7 17 17 7");
        arrow.getStyleClass().add("insight-open-action-icon-stroke");
        arrow.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.Group iconGroup = new javafx.scene.Group(elbow, arrow);
        javafx.scene.layout.StackPane iconPane = new javafx.scene.layout.StackPane(iconGroup);
        iconPane.setAlignment(Pos.CENTER);
        iconPane.setMinSize(18, 18);
        iconPane.setPrefSize(18, 18);
        iconPane.setMaxSize(18, 18);
        return iconPane;
    }

    private javafx.scene.Node createImportActionIcon() {
        java.util.function.Function<String, javafx.scene.shape.SVGPath> pathFactory = content -> {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(content);
            path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            path.setSmooth(true);
            path.getStyleClass().add("insight-open-action-icon-stroke");
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            return path;
        };

        javafx.scene.shape.Circle rearWheel = new javafx.scene.shape.Circle(7, 18, 2);
        rearWheel.getStyleClass().add("insight-open-action-icon-stroke");
        rearWheel.setFill(javafx.scene.paint.Color.TRANSPARENT);
        rearWheel.setSmooth(true);

        javafx.scene.shape.Circle frontWheel = new javafx.scene.shape.Circle(17, 18, 2);
        frontWheel.getStyleClass().add("insight-open-action-icon-stroke");
        frontWheel.setFill(javafx.scene.paint.Color.TRANSPARENT);
        frontWheel.setSmooth(true);

        javafx.scene.layout.Pane truckIcon = new javafx.scene.layout.Pane(
            pathFactory.apply("M14 18V6a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v11a1 1 0 0 0 1 1h2"),
            pathFactory.apply("M15 18H9"),
            pathFactory.apply("M19 18h2a1 1 0 0 0 1-1v-3.65a1 1 0 0 0-.22-.624l-3.48-4.35A1 1 0 0 0 17.52 8H14"),
            rearWheel,
            frontWheel
        );
        truckIcon.setMinSize(24, 24);
        truckIcon.setPrefSize(24, 24);
        truckIcon.setMaxSize(24, 24);
        truckIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconPane = new javafx.scene.layout.StackPane(truckIcon);
        iconPane.setAlignment(Pos.CENTER);
        iconPane.setMinSize(20, 20);
        iconPane.setPrefSize(20, 20);
        iconPane.setMaxSize(20, 20);
        iconPane.setScaleX(0.90);
        iconPane.setScaleY(0.90);
        return iconPane;
    }

    private void applyRoundedRegionClip(javafx.scene.layout.Region region, double radius) {
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.setArcWidth(radius * 2);
        clip.setArcHeight(radius * 2);
        clip.widthProperty().bind(region.widthProperty());
        clip.heightProperty().bind(region.heightProperty());
        region.setClip(clip);
    }

    private Label createInsightMetaChip(String text, boolean emphasized) {
        Label chip = new Label(text);
        chip.getStyleClass().add(emphasized ? "insight-meta-chip-strong" : "insight-meta-chip");
        return chip;
    }

    private javafx.scene.Node createInsightSeverityBadgeIcon(
        com.pbl3.project.pbl3_project.dto.report.InsightSeverity severity
    ) {
        javafx.scene.Node icon = createInsightSeverityGroupIcon(severity);
        javafx.scene.layout.StackPane wrap = new javafx.scene.layout.StackPane(icon);
        wrap.getStyleClass().add("insight-severity-badge-icon");
        wrap.setMinSize(26, 26);
        wrap.setPrefSize(26, 26);
        wrap.setMaxSize(26, 26);
        return wrap;
    }

    private javafx.scene.layout.StackPane wrapInsightDigestIcon(
        javafx.scene.Node icon,
        com.pbl3.project.pbl3_project.dto.report.InsightSeverity severity
    ) {
        javafx.scene.layout.StackPane wrap = new javafx.scene.layout.StackPane(icon);
        wrap.getStyleClass().add("insight-digest-icon-wrap");
        if (severity != null) {
            wrap.getStyleClass().add("insight-digest-icon-wrap-" + severity.name().toLowerCase(java.util.Locale.ROOT));
        }
        wrap.setMinSize(38, 38);
        wrap.setPrefSize(38, 38);
        wrap.setMaxSize(38, 38);
        return wrap;
    }

    private javafx.scene.Node createCompactInsightPlaceholder(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("dashboard-placeholder-label");
        label.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-muted;");
        javafx.scene.layout.StackPane wrapper = new javafx.scene.layout.StackPane(label);
        wrapper.setAlignment(Pos.CENTER_LEFT);
        wrapper.setMinHeight(72);
        wrapper.setPrefHeight(72);
        wrapper.getStyleClass().add("insight-placeholder");
        return wrapper;
    }

    private String formatInsightSeverity(com.pbl3.project.pbl3_project.dto.report.InsightSeverity severity) {
        if (severity == null) {
            return "Info";
        }
        return switch (severity) {
            case CRITICAL -> "Critical";
            case WARNING -> "Warning";
            case INFO -> "Info";
        };
    }

    private String formatWhatChangedType(com.pbl3.project.pbl3_project.dto.report.WhatChangedType type) {
        if (type == null) {
            return "Insight";
        }
        return switch (type) {
            case REVENUE_CHANGE -> "Revenue";
            case ORDER_COUNT_CHANGE -> "Orders";
            case AVERAGE_ORDER_VALUE_CHANGE -> "AOV";
            case CANCEL_RATE_CHANGE -> "Cancel Rate";
            case TOP_DRIVER_PRODUCT -> "Driver Product";
        };
    }

    private javafx.scene.Node extractWhatChangedDeltaChip(com.pbl3.project.pbl3_project.dto.report.WhatChangedInsight insight) {
        if (insight == null || insight.headline() == null || insight.headline().isBlank()) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
            .compile("\\b(up|down)\\s+([0-9]+(?:\\.[0-9]+)?%?)\\b", java.util.regex.Pattern.CASE_INSENSITIVE)
            .matcher(insight.headline());
        if (matcher.find()) {
            return createInsightDeltaChip(
                matcher.group(1).toLowerCase(java.util.Locale.ROOT),
                matcher.group(2)
            );
        }
        return insight.type() == com.pbl3.project.pbl3_project.dto.report.WhatChangedType.TOP_DRIVER_PRODUCT
            ? createInsightMetaChip("Driver", false)
            : null;
    }

    private javafx.scene.Node createActionCenterImpactChip(
        com.pbl3.project.pbl3_project.dto.report.ActionCenterItem item
    ) {
        if (item == null || item.impactLabel() == null || item.impactLabel().isBlank()) {
            return null;
        }
        return createInsightMetaChip(createInsightPreviewText(item.impactLabel(), 28), true);
    }

    private javafx.scene.Node createInsightDeltaChip(String direction, String value) {
        String safeDirection = "up".equals(direction) ? "up" : "down";
        Label arrowLabel = new Label("up".equals(safeDirection) ? "\u2191" : "\u2193");
        arrowLabel.getStyleClass().addAll("insight-delta-arrow", "insight-delta-arrow-" + safeDirection);

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().addAll("insight-delta-label", "insight-delta-label-" + safeDirection);

        javafx.scene.layout.HBox chip = new javafx.scene.layout.HBox(4, arrowLabel, valueLabel);
        chip.getStyleClass().addAll("insight-delta-chip", "insight-delta-chip-" + safeDirection);
        chip.setAlignment(Pos.CENTER_LEFT);
        return chip;
    }

    private javafx.scene.Node createInsightSeverityGroup(
        com.pbl3.project.pbl3_project.dto.report.InsightSeverity severity,
        int count,
        VBox groupContent,
        boolean collapsible,
        boolean collapsedInitially
    ) {
        com.pbl3.project.pbl3_project.dto.report.InsightSeverity safeSeverity =
            severity != null ? severity : com.pbl3.project.pbl3_project.dto.report.InsightSeverity.INFO;
        String severityTone = safeSeverity.name().toLowerCase(java.util.Locale.ROOT);

        javafx.scene.Node severityIcon = createInsightSeverityGroupIcon(safeSeverity);

        Label titleLabel = new Label(formatInsightSeverity(safeSeverity));
        titleLabel.getStyleClass().add("insight-severity-group-label");

        Label countLabel = new Label(count + (count == 1 ? " item" : " items"));
        countLabel.getStyleClass().addAll("insight-severity-group-count", "insight-severity-group-count-" + severityTone);

        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(8);
        header.getStyleClass().addAll("insight-severity-group", collapsible ? "insight-severity-group-toggle" : "insight-severity-group-static");
        header.setAlignment(Pos.CENTER_LEFT);

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.layout.HBox trailingWrap = new javafx.scene.layout.HBox(8);
        trailingWrap.getStyleClass().add("insight-severity-group-trailing");
        trailingWrap.setAlignment(Pos.CENTER_RIGHT);
        trailingWrap.getChildren().add(countLabel);
        header.getChildren().addAll(severityIcon, titleLabel, spacer, trailingWrap);

        final boolean[] expanded = { !collapsedInitially };
        javafx.scene.shape.SVGPath chevron = collapsible
            ? createInsightSeverityChevron(expanded[0])
            : null;
        if (collapsible) {
            header.setCursor(javafx.scene.Cursor.HAND);
            header.setFocusTraversable(true);
            trailingWrap.getChildren().add(wrapInsightSeverityChevron(chevron));
            header.setOnMouseClicked(event -> {
                if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                    expanded[0] = !expanded[0];
                    applyInsightSeverityExpandedState(header, groupContent, chevron, expanded[0]);
                }
            });
            header.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ENTER || event.getCode() == javafx.scene.input.KeyCode.SPACE) {
                    expanded[0] = !expanded[0];
                    applyInsightSeverityExpandedState(header, groupContent, chevron, expanded[0]);
                    event.consume();
                }
            });
        }

        VBox wrapper = new VBox(8);
        wrapper.getStyleClass().add("insight-severity-group-wrapper");
        wrapper.setFillWidth(true);
        wrapper.getChildren().addAll(header, groupContent);

        applyInsightSeverityExpandedState(header, groupContent, chevron, expanded[0]);
        return wrapper;
    }

    private javafx.scene.Node createInsightSeverityGroupIcon(
        com.pbl3.project.pbl3_project.dto.report.InsightSeverity severity
    ) {
        com.pbl3.project.pbl3_project.dto.report.InsightSeverity safeSeverity =
            severity != null ? severity : com.pbl3.project.pbl3_project.dto.report.InsightSeverity.INFO;
        return switch (safeSeverity) {
            case CRITICAL -> createCriticalInsightSeverityIcon();
            case WARNING -> createWarningInsightSeverityIcon();
            case INFO -> createInfoInsightSeverityIcon();
        };
    }

    private javafx.scene.Node createCriticalInsightSeverityIcon() {
        javafx.scene.shape.SVGPath outline = new javafx.scene.shape.SVGPath();
        outline.setContent("M8.27 3h7.46L21 8.27v7.46L15.73 21H8.27L3 15.73V8.27Z");
        outline.getStyleClass().add("insight-severity-icon-stroke-critical");
        outline.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.shape.SVGPath mark = new javafx.scene.shape.SVGPath();
        mark.setContent("M12 8v4");
        mark.getStyleClass().add("insight-severity-icon-stroke-critical");
        mark.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(12, 16.3, 0.85);
        dot.getStyleClass().add("insight-severity-icon-fill-critical");

        javafx.scene.layout.Pane iconPane = new javafx.scene.layout.Pane(outline, mark, dot);
        iconPane.getStyleClass().add("insight-severity-group-icon-wrap");
        iconPane.setMinSize(24, 24);
        iconPane.setPrefSize(24, 24);
        iconPane.setMaxSize(24, 24);
        return iconPane;
    }

    private javafx.scene.Node createWarningInsightSeverityIcon() {
        javafx.scene.shape.SVGPath outline = new javafx.scene.shape.SVGPath();
        outline.setContent("M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z");
        outline.getStyleClass().add("insight-severity-icon-stroke-warning");
        outline.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.shape.SVGPath mark = new javafx.scene.shape.SVGPath();
        mark.setContent("M12 9v4");
        mark.getStyleClass().add("insight-severity-icon-stroke-warning");
        mark.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(12, 16.7, 0.85);
        dot.getStyleClass().add("insight-severity-icon-fill-warning");

        javafx.scene.layout.Pane iconPane = new javafx.scene.layout.Pane(outline, mark, dot);
        iconPane.getStyleClass().add("insight-severity-group-icon-wrap");
        iconPane.setMinSize(24, 24);
        iconPane.setPrefSize(24, 24);
        iconPane.setMaxSize(24, 24);
        return iconPane;
    }

    private javafx.scene.Node createInfoInsightSeverityIcon() {
        javafx.scene.shape.SVGPath outline = new javafx.scene.shape.SVGPath();
        outline.setContent("M12 2 A10 10 0 1 1 12 22 A10 10 0 1 1 12 2");
        outline.getStyleClass().add("insight-severity-icon-stroke-info");
        outline.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.shape.SVGPath mark = new javafx.scene.shape.SVGPath();
        mark.setContent("M12 10.4v5.1");
        mark.getStyleClass().add("insight-severity-icon-stroke-info");
        mark.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.shape.SVGPath dot = new javafx.scene.shape.SVGPath();
        dot.setContent("M12 7.1 L12 7.1");
        dot.getStyleClass().add("insight-severity-icon-dot-info");
        dot.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.layout.Pane iconPane = new javafx.scene.layout.Pane(outline, mark, dot);
        iconPane.getStyleClass().add("insight-severity-group-icon-wrap");
        iconPane.setMinSize(24, 24);
        iconPane.setPrefSize(24, 24);
        iconPane.setMaxSize(24, 24);
        return iconPane;
    }

    private javafx.scene.shape.SVGPath createInsightSeverityChevron(boolean expanded) {
        javafx.scene.shape.SVGPath chevron = new javafx.scene.shape.SVGPath();
        chevron.setContent("M8 6 L14 12 L8 18");
        chevron.getStyleClass().add("insight-severity-group-chevron");
        chevron.setStrokeWidth(2.0);
        chevron.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        chevron.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        chevron.setFill(javafx.scene.paint.Color.TRANSPARENT);
        chevron.setRotate(expanded ? 90.0 : 0.0);
        return chevron;
    }

    private javafx.scene.layout.StackPane wrapInsightSeverityChevron(javafx.scene.shape.SVGPath chevron) {
        javafx.scene.layout.StackPane wrap = new javafx.scene.layout.StackPane(chevron);
        wrap.setMinSize(18, 18);
        wrap.setPrefSize(18, 18);
        wrap.setMaxSize(18, 18);
        return wrap;
    }

    private void applyInsightSeverityExpandedState(
        javafx.scene.layout.HBox header,
        javafx.scene.Node content,
        javafx.scene.shape.SVGPath chevron,
        boolean expanded
    ) {
        if (content != null) {
            content.setManaged(expanded);
            content.setVisible(expanded);
        }
        if (header != null) {
            if (expanded) {
                if (!header.getStyleClass().contains("expanded")) {
                    header.getStyleClass().add("expanded");
                }
            } else {
                header.getStyleClass().remove("expanded");
            }
        }
        if (chevron != null) {
            chevron.setRotate(expanded ? 90.0 : 0.0);
        }
    }

    private String formatActionCenterType(com.pbl3.project.pbl3_project.dto.report.ActionCenterType type) {
        if (type == null) {
            return "Action";
        }
        return switch (type) {
            case REORDER_NOW -> "Reorder Now";
            case LOW_COVERAGE -> "Low Coverage";
            case AGED_STOCK -> "Aged Stock";
            case REVENUE_DROP -> "Revenue Drop";
            case CANCEL_SPIKE -> "Cancel Spike";
        };
    }

    private String formatInsightActionLabel(String actionLabel) {
        if (actionLabel == null || actionLabel.isBlank()) {
            return null;
        }
        String normalized = actionLabel.trim();
        if ("Open Import Goods".equalsIgnoreCase(normalized)) {
            return "Import";
        }
        if ("Review Reorder".equalsIgnoreCase(normalized)) {
            return "Review";
        }
        return normalized;
    }

    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        if (text.length() == 1) {
            return text.toUpperCase(java.util.Locale.ROOT);
        }
        return text.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + text.substring(1);
    }

    private String createInsightPreviewText(String detail, int maxLength) {
        if (detail == null) {
            return "";
        }
        String normalized = detail.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        if (maxLength <= 1) {
            return normalized.substring(0, 1);
        }
        return normalized.substring(0, maxLength - 1).trim() + "…";
    }

    private javafx.scene.layout.GridPane createResponsiveInsightGrid(
        java.util.List<VBox> cards,
        javafx.beans.value.ObservableNumberValue widthSource,
        double twoColumnBreakpoint
    ) {
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        grid.setMaxWidth(Double.MAX_VALUE);
        if (widthSource != null) {
            grid.prefWidthProperty().bind(widthSource);
        }

        cards.forEach(card -> {
            card.setMinWidth(0);
            card.setMaxWidth(Double.MAX_VALUE);
            javafx.scene.layout.GridPane.setHgrow(card, javafx.scene.layout.Priority.ALWAYS);
            javafx.scene.layout.GridPane.setFillWidth(card, true);
        });

        final boolean[] twoColumnMode = {false};
        Runnable syncLayout = () -> {
            boolean shouldUseTwoColumns = widthSource != null
                && widthSource.doubleValue() >= twoColumnBreakpoint
                && cards.size() > 1;
            if (!grid.getChildren().isEmpty() && twoColumnMode[0] == shouldUseTwoColumns) {
                return;
            }
            twoColumnMode[0] = shouldUseTwoColumns;
            grid.getChildren().clear();
            grid.getColumnConstraints().clear();
            grid.getRowConstraints().clear();

            if (shouldUseTwoColumns) {
                for (int i = 0; i < 2; i++) {
                    javafx.scene.layout.ColumnConstraints column = new javafx.scene.layout.ColumnConstraints();
                    column.setPercentWidth(50);
                    column.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                    column.setFillWidth(true);
                    grid.getColumnConstraints().add(column);
                }
                for (int i = 0; i < cards.size(); i++) {
                    grid.add(cards.get(i), i % 2, i / 2);
                }
            } else {
                javafx.scene.layout.ColumnConstraints column = new javafx.scene.layout.ColumnConstraints();
                column.setPercentWidth(100);
                column.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                column.setFillWidth(true);
                grid.getColumnConstraints().add(column);
                for (int i = 0; i < cards.size(); i++) {
                    grid.add(cards.get(i), 0, i);
                }
            }
        };

        syncLayout.run();
        if (widthSource != null) {
            widthSource.addListener((obs, oldValue, newValue) -> syncLayout.run());
        }
        return grid;
    }

    private String formatCompactDecimal(BigDecimal value) {
        if (value == null) {
            return "-";
        }
        BigDecimal normalized = MoneySupport.normalize(value).stripTrailingZeros();
        return normalized.scale() < 0 ? normalized.setScale(0).toPlainString() : normalized.toPlainString();
    }

    private void exportCsv(Stage owner, String defaultFileName, java.util.List<String> headers, java.util.List<java.util.List<String>> rows) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Export CSV");
        fileChooser.setInitialFileName(defaultFileName);
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        java.io.File file = fileChooser.showSaveDialog(owner);
        if (file == null) {
            return;
        }

        try (java.io.BufferedWriter writer = java.nio.file.Files.newBufferedWriter(
            file.toPath(),
            java.nio.charset.StandardCharsets.UTF_8,
            java.nio.file.StandardOpenOption.CREATE,
            java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
            java.nio.file.StandardOpenOption.WRITE
        )) {
            writer.write('\uFEFF');
            writer.write(toCsvLine(headers));
            for (java.util.List<String> row : rows) {
                writer.newLine();
                writer.write(toCsvLine(row));
            }
            toastService.showSuccess("Exported " + file.getName());
        } catch (Exception ex) {
            toastService.showError("Export failed: " + ex.getMessage());
        }
    }

    private String toCsvLine(java.util.List<String> values) {
        return values.stream()
            .map(this::escapeCsvCell)
            .collect(java.util.stream.Collectors.joining(","));
    }

    private String escapeCsvCell(String value) {
        String safeValue = value != null ? value : "";
        String escapedValue = safeValue.replace("\"", "\"\"");
        boolean requiresQuotes = escapedValue.contains(",")
            || escapedValue.contains("\"")
            || escapedValue.contains("\n")
            || escapedValue.contains("\r");
        return requiresQuotes ? "\"" + escapedValue + "\"" : escapedValue;
    }

    private String buildReportCsvFileName(String baseName, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return baseName + "-" + buildReportRangeFileSuffix(startDate, endDate) + ".csv";
    }

    private String buildReportRangeFileSuffix(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return "all-time";
        }
        if (startDate != null && endDate != null) {
            if (startDate.equals(endDate)) {
                return startDate.format(FILE_DATE_FORMATTER);
            }
            return startDate.format(FILE_DATE_FORMATTER) + "-to-" + endDate.format(FILE_DATE_FORMATTER);
        }
        if (startDate != null) {
            return "from-" + startDate.format(FILE_DATE_FORMATTER);
        }
        return "until-" + endDate.format(FILE_DATE_FORMATTER);
    }

    private VBox createOrderHistoryView(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        final String orderSortStateKey = "order-history";
        TableSortState orderSortState = getOrCreateTableSortState(
            orderSortStateKey,
            new SortCriterion("createdAt", javafx.scene.control.TableColumn.SortType.DESCENDING)
        );
        java.util.LinkedHashMap<String, String> orderSortProperties = new java.util.LinkedHashMap<>();
        orderSortProperties.put("id", "id");
        orderSortProperties.put("createdAt", "createdAt");
        orderSortProperties.put("totalPrice", "totalPrice");
        orderSortProperties.put("userFullName", "createdByNameSnapshot");
        orderSortProperties.put("customerName", "customerNameSnapshot");
        orderSortProperties.put("status", "status");

        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.Order> table = new javafx.scene.control.TableView<>();
        applyStandardTableSizing(table);
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        enableDragSelection(table);

        final int orderPageSize = 20;
        final int[] orderCurrentPage = {0};
        final int[] orderTotalPages = {0};
        final long[] orderTotalElements = {0L};
        java.util.concurrent.atomic.AtomicReference<String> orderSearchRef = new java.util.concurrent.atomic.AtomicReference<>("");
        java.util.concurrent.atomic.AtomicReference<java.time.LocalDate> orderStartDateRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<java.time.LocalDate> orderEndDateRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<java.util.Set<Long>> orderUsersRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<java.util.Set<com.pbl3.project.pbl3_project.entity.PaymentMethod>> orderMethodsRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<java.util.Set<com.pbl3.project.pbl3_project.entity.OrderStatus>> orderStatusesRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<BigDecimal> orderMinTotalRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<BigDecimal> orderMaxTotalRef = new java.util.concurrent.atomic.AtomicReference<>(null);

        Label orderRowCountLabel = createStatusMetaLabel("Showing 0-0 of 0 Row(s)");
        Label orderPageLabel = createStatusMetaLabel("Page 0 / 0");
        Button orderPrevBtn = createPageNavButton("Prev");
        Button orderNextBtn = createPageNavButton("Next");

        Runnable[] refreshOrderTableRef = new Runnable[1];
        Runnable updateOrderStatusBar = () -> updatePagedStatus(
            table,
            orderRowCountLabel,
            orderPageLabel,
            orderPrevBtn,
            orderNextBtn,
            orderTotalElements[0],
            orderCurrentPage[0],
            orderTotalPages[0],
            orderPageSize
        );
        Runnable loadOrderPage = () -> {
            org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.Order> pageData = orderService.searchOrders(
                user,
                orderSearchRef.get(),
                orderStartDateRef.get(),
                orderEndDateRef.get(),
                orderUsersRef.get(),
                orderMethodsRef.get(),
                orderStatusesRef.get(),
                orderMinTotalRef.get(),
                orderMaxTotalRef.get(),
                createPageable(orderSortState, orderSortProperties, orderCurrentPage[0], orderPageSize)
            );
            if (pageData.getTotalPages() > 0 && orderCurrentPage[0] >= pageData.getTotalPages()) {
                orderCurrentPage[0] = pageData.getTotalPages() - 1;
                pageData = orderService.searchOrders(
                    user,
                    orderSearchRef.get(),
                    orderStartDateRef.get(),
                    orderEndDateRef.get(),
                    orderUsersRef.get(),
                    orderMethodsRef.get(),
                    orderStatusesRef.get(),
                    orderMinTotalRef.get(),
                    orderMaxTotalRef.get(),
                    createPageable(orderSortState, orderSortProperties, orderCurrentPage[0], orderPageSize)
                );
            }
            orderTotalElements[0] = pageData.getTotalElements();
            orderTotalPages[0] = pageData.getTotalPages();
            table.setItems(javafx.collections.FXCollections.observableArrayList(pageData.getContent()));
            updateOrderStatusBar.run();
        };
        refreshOrderTableRef[0] = loadOrderPage;
        orderPrevBtn.setOnAction(e -> {
            if (orderCurrentPage[0] > 0) {
                orderCurrentPage[0]--;
                loadOrderPage.run();
            }
        });
        orderNextBtn.setOnAction(e -> {
            if (orderCurrentPage[0] + 1 < orderTotalPages[0]) {
                orderCurrentPage[0]++;
                loadOrderPage.run();
            }
        });
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, Long> idCol = new javafx.scene.control.TableColumn<>("Order ID");
        idCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, String> dateCol = new javafx.scene.control.TableColumn<>("Created At");
        dateCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(formatDateTime(cell.getValue().getCreatedAt())));
        dateCol.setPrefWidth(200);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, String> totalCol = new javafx.scene.control.TableColumn<>("Net Paid");
        totalCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(formatVnd(cell.getValue().getTotalPrice())));
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, String> userCol = new javafx.scene.control.TableColumn<>("Created By");
        userCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getCreatedByDisplayName()));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, String> customerCol = new javafx.scene.control.TableColumn<>("Customer");
        customerCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getCustomerDisplayName()));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, String> statusCol = new javafx.scene.control.TableColumn<>("Status");
        statusCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(formatOrderStatus(cell.getValue().getStatus())));
        statusCol.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                String textColor = switch (item) {
                    case "Canceled" -> "-app-danger-hover";
                    case "Returned", "Partially Returned" -> "-app-primary-hover";
                    default -> "-app-success-hover";
                };
                setStyle("-fx-text-fill: " + textColor + "; -fx-font-weight: bold; -fx-alignment: CENTER;");
            }
        });

        table.getColumns().addAll(idCol, dateCol, totalCol, userCol, customerCol, statusCol);
        java.util.LinkedHashMap<String, javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, ?>> orderSortColumns =
            new java.util.LinkedHashMap<>();
        orderSortColumns.put("id", idCol);
        orderSortColumns.put("createdAt", dateCol);
        orderSortColumns.put("totalPrice", totalCol);
        orderSortColumns.put("userFullName", userCol);
        orderSortColumns.put("customerName", customerCol);
        orderSortColumns.put("status", statusCol);
        installSortHeaderIndicators(orderSortColumns);
        java.util.LinkedHashMap<String, String> orderSortLabels = new java.util.LinkedHashMap<>();
        orderSortLabels.put("id", "Order ID");
        orderSortLabels.put("createdAt", "Created At");
        orderSortLabels.put("totalPrice", "Net Paid");
        orderSortLabels.put("userFullName", "Created By");
        orderSortLabels.put("customerName", "Customer");
        orderSortLabels.put("status", "Status");
        Label orderSortStatusLabel = createSortStatusLabel(orderSortState, orderSortLabels);
        Runnable applyOrderSortUi = () -> {
            applySortStateToTable(table, orderSortColumns, orderSortState);
            orderSortStatusLabel.setText(buildSortStatusText(orderSortState, orderSortLabels));
        };
        applyOrderSortUi.run();
        installManualServerSorting(
            table,
            orderSortColumns,
            orderSortState,
            () -> {
                applyOrderSortUi.run();
                orderCurrentPage[0] = 0;
                loadOrderPage.run();
            }
        );

        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<com.pbl3.project.pbl3_project.entity.Order> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    try {
                        showOrderDetailsDialog(stage, orderService.getOrderWithItems(row.getItem().getId(), user), user, refreshOrderTableRef[0]);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Error", "Could not load order details: " + ex.getMessage());
                    }
                }
            });
            return row;
        });

        // Expandable Search Bar for Orders
        javafx.scene.layout.HBox oSearchBox = new javafx.scene.layout.HBox(0);
        oSearchBox.setAlignment(Pos.CENTER);
        oSearchBox.getStyleClass().add("expandable-search-box");
        oSearchBox.setPrefSize(40, 40); oSearchBox.setMinSize(40, 40); oSearchBox.setMaxSize(40, 40);
        javafx.scene.shape.SVGPath oIcon = new javafx.scene.shape.SVGPath();
        oIcon.setContent("M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
        oIcon.setFill(PRIMARY_COLOR);
        javafx.scene.layout.Region oSpacer = new javafx.scene.layout.Region();
        oSpacer.setMinWidth(0); oSpacer.setPrefWidth(0);
        TextField oField = new TextField();
        oField.setPromptText(DEFAULT_SEARCH_PROMPT); oField.getStyleClass().add("search-text-field");
        oField.setMinWidth(0); oField.setMaxWidth(0); oField.setPrefWidth(0); oField.setOpacity(0);
        oSearchBox.getChildren().addAll(oIcon, oSpacer, oField);
        javafx.animation.Timeline oExpand = new javafx.animation.Timeline(new javafx.animation.KeyFrame(Duration.millis(150),
            new javafx.animation.KeyValue(oSearchBox.maxWidthProperty(), 250, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oSearchBox.prefWidthProperty(), 250, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oSpacer.minWidthProperty(), 8, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oField.minWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oField.maxWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oField.prefWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oField.opacityProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH)));
        javafx.animation.Timeline oCollapse = new javafx.animation.Timeline(new javafx.animation.KeyFrame(Duration.millis(150),
            new javafx.animation.KeyValue(oSearchBox.maxWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oSearchBox.prefWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oSpacer.minWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oField.minWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oField.maxWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oField.prefWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oField.opacityProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH)));

        javafx.animation.PauseTransition orderSearchPause = new javafx.animation.PauseTransition(Duration.millis(220));
        orderSearchPause.setOnFinished(e -> {
            orderCurrentPage[0] = 0;
            orderSearchRef.set(oField.getText());
            loadOrderPage.run();
        });
        oField.textProperty().addListener((obs, oldV, newV) -> orderSearchPause.playFromStart());

        VBox content = new VBox();
        applyStandardTablePageLayout(content);

        oSearchBox.setOnMouseClicked(ev -> {
            if (oSearchBox.getMaxWidth() == 40) { oExpand.play(); oField.requestFocus(); }
            else if (ev.getTarget() == oIcon || ev.getTarget() == oSearchBox) { oField.clear(); content.requestFocus(); oCollapse.play(); }
        });

        // Filter Button (Payment Method)
        javafx.scene.layout.HBox oFilterBox = new javafx.scene.layout.HBox();
        oFilterBox.setAlignment(Pos.CENTER);
        oFilterBox.getStyleClass().add("expandable-search-box");
        oFilterBox.setPrefSize(40, 40); oFilterBox.setMinSize(40, 40); oFilterBox.setMaxSize(40, 40);
        oFilterBox.setCursor(javafx.scene.Cursor.HAND);
        javafx.scene.shape.SVGPath oFilterIcon = new javafx.scene.shape.SVGPath();
        oFilterIcon.setContent("M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z");
        oFilterIcon.setFill(PRIMARY_COLOR);
        oFilterBox.getChildren().add(oFilterIcon);

        javafx.stage.Popup oFilterPopup = new javafx.stage.Popup();
        oFilterPopup.setAutoHide(true);

        oFilterBox.setOnMouseClicked(fev -> {
            fev.consume();
            try {
                if (oFilterPopup.isShowing()) {
                    oFilterPopup.hide();
                    return;
                }

                VBox popupContainer = new VBox(10);
                popupContainer.setPadding(new Insets(15));
                applyFilterPopupContainerStyle(popupContainer);
                popupContainer.setPrefWidth(350);

                VBox scrollContent = new VBox(10);
                scrollContent.setStyle("-fx-background-color: -app-surface;");
                scrollContent.setPadding(new Insets(5, 15, 5, 15));
                javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(scrollContent);
                scrollPane.setFitToWidth(true);
                scrollPane.setStyle("-fx-background: -app-surface; -fx-background-color: transparent; -fx-border-color: transparent;");
                scrollPane.setPrefViewportHeight(350);

            // --- Date Range ---
            Label dateTitle = new Label("Date Range");
            dateTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");
            javafx.scene.control.DatePicker startDatePicker = new javafx.scene.control.DatePicker();
            startDatePicker.setPromptText("Start Date");
            startDatePicker.setPrefWidth(140);
            startDatePicker.setStyle("-fx-font-size: 13px;");
            javafx.scene.control.DatePicker endDatePicker = new javafx.scene.control.DatePicker();
            endDatePicker.setPromptText("End Date");
            endDatePicker.setPrefWidth(140);
            endDatePicker.setStyle("-fx-font-size: 13px;");
            customizeDatePicker(startDatePicker);
            customizeDatePicker(endDatePicker);
            javafx.scene.layout.HBox dateBox = new javafx.scene.layout.HBox(5, startDatePicker, new Label("-"), endDatePicker);
            dateBox.setAlignment(Pos.CENTER_LEFT);

            javafx.scene.control.Separator sepDate = new javafx.scene.control.Separator();

            // --- Created By ---
            Label userTitle = new Label("Created By");
            userTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");
            
            javafx.scene.control.CheckBox allUsersCb = new javafx.scene.control.CheckBox("All Users");
            allUsersCb.setSelected(true);
            allUsersCb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");

            VBox userScroll = new VBox(8);
            userScroll.setPadding(new Insets(5, 5, 5, 20));
            
            java.util.List<javafx.scene.control.CheckBox> userCbs = new java.util.ArrayList<>();
            java.util.List<com.pbl3.project.pbl3_project.dto.IdLabelOption> userOptions = orderService.getOrderCreatorOptions(user);

            for (com.pbl3.project.pbl3_project.dto.IdLabelOption option : userOptions) {
                if (option.label() == null || option.label().trim().isEmpty()) continue;
                javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(option.label());
                cb.setUserData(option.id());
                cb.setSelected(true);
                cb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                cb.setOnAction(e -> {
                    if (!cb.isSelected()) allUsersCb.setSelected(false);
                    else {
                        boolean all = true;
                        for (javafx.scene.control.CheckBox c : userCbs) if (!c.isSelected()) all = false;
                        allUsersCb.setSelected(all);
                    }
                });
                userCbs.add(cb);
                userScroll.getChildren().add(cb);
            }
            allUsersCb.setOnAction(e -> {
                boolean sel = allUsersCb.isSelected();
                for (javafx.scene.control.CheckBox cb : userCbs) cb.setSelected(sel);
            });
            FilterDisclosureSection userSection = new FilterDisclosureSection(allUsersCb, userScroll);

            javafx.scene.control.Separator sepUser = new javafx.scene.control.Separator();

            // --- Payment Method ---
            Label methodTitle = new Label("Payment Method");
            methodTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");
            
            javafx.scene.control.CheckBox allMethodsCb = new javafx.scene.control.CheckBox("All Methods");
            allMethodsCb.setSelected(true);
            allMethodsCb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");

            VBox methodScroll = new VBox(8);
            methodScroll.setPadding(new Insets(5, 5, 5, 20));
            
            java.util.List<javafx.scene.control.CheckBox> methodCbs = new java.util.ArrayList<>();
            for (com.pbl3.project.pbl3_project.entity.PaymentMethod pm : com.pbl3.project.pbl3_project.entity.PaymentMethod.values()) {
                javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(pm.name());
                cb.setSelected(true);
                cb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                cb.setOnAction(e -> {
                    if (!cb.isSelected()) allMethodsCb.setSelected(false);
                    else {
                        boolean all = true;
                        for (javafx.scene.control.CheckBox c : methodCbs) if (!c.isSelected()) all = false;
                        allMethodsCb.setSelected(all);
                    }
                });
                methodCbs.add(cb);
                methodScroll.getChildren().add(cb);
            }

            allMethodsCb.setOnAction(e -> {
                boolean sel = allMethodsCb.isSelected();
                for (javafx.scene.control.CheckBox cb : methodCbs) cb.setSelected(sel);
            });
            FilterDisclosureSection methodSection = new FilterDisclosureSection(allMethodsCb, methodScroll);

            javafx.scene.control.Separator sepMethod = new javafx.scene.control.Separator();

            // --- Order Status ---
            Label statusTitle = new Label("Order Status");
            statusTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");

            javafx.scene.control.CheckBox allStatusesCb = new javafx.scene.control.CheckBox("All Statuses");
            allStatusesCb.setSelected(true);
            allStatusesCb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");

            VBox statusScroll = new VBox(8);
            statusScroll.setPadding(new Insets(5, 5, 5, 20));

            java.util.List<javafx.scene.control.CheckBox> statusCbs = new java.util.ArrayList<>();
            for (com.pbl3.project.pbl3_project.entity.OrderStatus status : com.pbl3.project.pbl3_project.entity.OrderStatus.values()) {
                javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(formatOrderStatus(status));
                cb.setUserData(status);
                cb.setSelected(true);
                cb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                cb.setOnAction(e -> {
                    if (!cb.isSelected()) {
                        allStatusesCb.setSelected(false);
                    } else {
                        boolean all = true;
                        for (javafx.scene.control.CheckBox c : statusCbs) {
                            if (!c.isSelected()) all = false;
                        }
                        allStatusesCb.setSelected(all);
                    }
                });
                statusCbs.add(cb);
                statusScroll.getChildren().add(cb);
            }

            allStatusesCb.setOnAction(e -> {
                boolean sel = allStatusesCb.isSelected();
                for (javafx.scene.control.CheckBox cb : statusCbs) cb.setSelected(sel);
            });
            FilterDisclosureSection statusSection = new FilterDisclosureSection(allStatusesCb, statusScroll);

            javafx.scene.control.Separator sepStatus = new javafx.scene.control.Separator();

            // --- Price Range ---
            Label priceTitle = new Label("Net Paid Range");
            priceTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");
            
            BigDecimal maxPriceValue = orderService.getOrderMaxTotalPrice(user);
            double maxPrice = maxPriceValue == null ? 0.0 : maxPriceValue.doubleValue();
            if (maxPrice == 0) maxPrice = 1000;
            
            Label priceLabel = new Label("0 - " + String.format("%.0f", maxPrice) + " VND");
            priceLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -app-primary; -fx-font-weight: bold;");
            
            RangeSlider priceSlider = new RangeSlider(0, maxPrice, 0, maxPrice, 280);
            priceSlider.minVal.addListener((o, ov, nv) -> priceLabel.setText(String.format("%.0f - %.0f VND", nv.doubleValue(), priceSlider.maxVal.get())));
            priceSlider.maxVal.addListener((o, ov, nv) -> priceLabel.setText(String.format("%.0f - %.0f VND", priceSlider.minVal.get(), nv.doubleValue())));

            scrollContent.getChildren().addAll(
                dateTitle, dateBox, sepDate,
                userTitle, userSection.getNode(), sepUser,
                methodTitle, methodSection.getNode(), sepMethod,
                statusTitle, statusSection.getNode(), sepStatus,
                priceTitle, priceLabel, priceSlider
            );

            // --- Buttons ---
            javafx.scene.layout.HBox btnRow = new javafx.scene.layout.HBox(10);
            btnRow.setAlignment(Pos.CENTER_RIGHT);
            
            final double fMaxPrice = maxPrice;

            Button resetBtn = new Button("Reset");
            resetBtn.getStyleClass().add("filter-reset-btn");
            resetBtn.setOnAction(ae -> {
                oFilterBox.setStyle("");
                startDatePicker.setValue(null);
                endDatePicker.setValue(null);
                allUsersCb.setSelected(true);
                for (javafx.scene.control.CheckBox cb : userCbs) cb.setSelected(true);
                userSection.setExpanded(false);
                allMethodsCb.setSelected(true);
                for (javafx.scene.control.CheckBox cb : methodCbs) cb.setSelected(true);
                methodSection.setExpanded(false);
                allStatusesCb.setSelected(true);
                for (javafx.scene.control.CheckBox cb : statusCbs) cb.setSelected(true);
                statusSection.setExpanded(false);
                priceSlider.minVal.set(0); priceSlider.maxVal.set(fMaxPrice);
                orderStartDateRef.set(null);
                orderEndDateRef.set(null);
                orderUsersRef.set(new java.util.LinkedHashSet<>());
                orderMethodsRef.set(new java.util.LinkedHashSet<>());
                orderStatusesRef.set(new java.util.LinkedHashSet<>());
                orderMinTotalRef.set(null);
                orderMaxTotalRef.set(null);
                orderCurrentPage[0] = 0;
                loadOrderPage.run();
                oFilterPopup.hide();
            });

            Button applyBtn = new Button("Apply Filter");
            applyBtn.getStyleClass().add("filter-apply-btn");
            applyBtn.setOnAction(ae -> {
                java.util.Set<Long> selectedUsers = new java.util.HashSet<>();
                for (javafx.scene.control.CheckBox cb : userCbs) {
                    if (cb.isSelected() && cb.getUserData() instanceof Long userId) {
                        selectedUsers.add(userId);
                    }
                }
                
                java.util.Set<String> selectedMethods = new java.util.HashSet<>();
                for (javafx.scene.control.CheckBox cb : methodCbs) {
                    if (cb.isSelected()) selectedMethods.add(cb.getText());
                }

                java.util.Set<com.pbl3.project.pbl3_project.entity.OrderStatus> selectedStatuses = new java.util.HashSet<>();
                for (javafx.scene.control.CheckBox cb : statusCbs) {
                    if (cb.isSelected()) {
                        selectedStatuses.add((com.pbl3.project.pbl3_project.entity.OrderStatus) cb.getUserData());
                    }
                }
                
                double pMin = priceSlider.minVal.get();
                double pMax = priceSlider.maxVal.get();
                java.time.LocalDate sDate = startDatePicker.getValue();
                java.time.LocalDate eDate = endDatePicker.getValue();

                orderStartDateRef.set(sDate);
                orderEndDateRef.set(eDate);
                orderUsersRef.set(allUsersCb.isSelected() ? new java.util.LinkedHashSet<>() : selectedUsers);
                java.util.Set<com.pbl3.project.pbl3_project.entity.PaymentMethod> methodFilters = new java.util.LinkedHashSet<>();
                if (!allMethodsCb.isSelected()) {
                    for (String method : selectedMethods) {
                        methodFilters.add(com.pbl3.project.pbl3_project.entity.PaymentMethod.valueOf(method));
                    }
                }
                orderMethodsRef.set(methodFilters);
                orderStatusesRef.set(allStatusesCb.isSelected() ? new java.util.LinkedHashSet<>() : selectedStatuses);
                orderMinTotalRef.set(pMin <= 0 ? null : MoneySupport.normalize(BigDecimal.valueOf(pMin)));
                orderMaxTotalRef.set(pMax >= fMaxPrice ? null : MoneySupport.normalize(BigDecimal.valueOf(pMax)));
                orderCurrentPage[0] = 0;
                loadOrderPage.run();

                boolean hasFilter = !allMethodsCb.isSelected()
                    || !allUsersCb.isSelected()
                    || !allStatusesCb.isSelected()
                    || pMin > 0
                    || pMax < fMaxPrice
                    || sDate != null
                    || eDate != null;
                oFilterBox.setStyle(hasFilter ? "-fx-border-color: -app-primary;" : "");
                oFilterPopup.hide();
            });

            btnRow.getChildren().addAll(resetBtn, applyBtn);

                popupContainer.getChildren().addAll(scrollPane, btnRow);
                oFilterPopup.getContent().clear();
                oFilterPopup.getContent().add(popupContainer);

                showPopupBelow(oFilterPopup, oFilterBox, -290, 5);
            } catch (Exception ex) {
                showUserFacingError(ex);
            }
        });

        Label orderHeader = new Label("Order History");
        orderHeader.getStyleClass().add("header-label");
        Button manageOrderBtn = createExpandableManageActionButton("Manage Order", 164);
        manageOrderBtn.setDisable(true);
        manageOrderBtn.setOnAction(e -> {
            java.util.List<com.pbl3.project.pbl3_project.entity.Order> selectedOrders = new java.util.ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selectedOrders.size() != 1) {
                toastService.showWarning("Select exactly one order to manage");
                return;
            }
            try {
                showOrderDetailsDialog(stage, orderService.getOrderWithItems(selectedOrders.get(0).getId(), user), user, refreshOrderTableRef[0]);
            } catch (Exception ex) {
                showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Error", "Could not load order details: " + ex.getMessage());
            }
        });
        table.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<com.pbl3.project.pbl3_project.entity.Order>) c ->
            manageOrderBtn.setDisable(table.getSelectionModel().getSelectedItems().size() != 1)
        );

        javafx.scene.layout.BorderPane orderToolbar = new javafx.scene.layout.BorderPane();
        orderToolbar.setLeft(orderHeader);
        javafx.scene.layout.HBox oRightBox = new javafx.scene.layout.HBox(15, oFilterBox, oSearchBox, manageOrderBtn);
        oRightBox.setAlignment(Pos.CENTER_RIGHT);
        orderToolbar.setRight(oRightBox);
        javafx.scene.layout.BorderPane.setAlignment(orderHeader, Pos.CENTER_LEFT);

        javafx.scene.layout.HBox orderStatusBar = new javafx.scene.layout.HBox(15, orderSortStatusLabel, orderRowCountLabel, orderPageLabel, orderPrevBtn, orderNextBtn);
        applyStandardTableStatusBar(orderStatusBar);

        content.getChildren().addAll(orderToolbar, table, orderStatusBar);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);

        table.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<com.pbl3.project.pbl3_project.entity.Order>) c -> updateOrderStatusBar.run());
        loadOrderPage.run();
        enableDeselectOnOutsideClick(content, table);
        return content;
    }

    private VBox createReturnsRefundsView(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        final String returnSortStateKey = "returns-refunds";
        TableSortState returnSortState = getOrCreateTableSortState(
            returnSortStateKey,
            new SortCriterion("createdAt", javafx.scene.control.TableColumn.SortType.DESCENDING)
        );
        java.util.LinkedHashMap<String, String> returnSortProperties = new java.util.LinkedHashMap<>();
        returnSortProperties.put("id", "id");
        returnSortProperties.put("createdAt", "createdAt");
        returnSortProperties.put("customerName", "customerNameSnapshot");
        returnSortProperties.put("userFullName", "createdByNameSnapshot");
        returnSortProperties.put("status", "status");
        returnSortProperties.put("refundedAmount", "refundedAmount");
        returnSortProperties.put("grossTotal", "grossSubtotal");

        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.Order> table = new javafx.scene.control.TableView<>();
        applyStandardTableSizing(table);
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        enableDragSelection(table);

        final int returnPageSize = 20;
        final int[] returnCurrentPage = {0};
        final int[] returnTotalPages = {0};
        final long[] returnTotalElements = {0L};
        java.util.concurrent.atomic.AtomicReference<com.pbl3.project.pbl3_project.entity.ReturnRefundScope> scopeRef =
            new java.util.concurrent.atomic.AtomicReference<>(com.pbl3.project.pbl3_project.entity.ReturnRefundScope.PROCESSED);
        java.util.concurrent.atomic.AtomicReference<String> returnSearchRef = new java.util.concurrent.atomic.AtomicReference<>("");
        java.util.concurrent.atomic.AtomicReference<java.time.LocalDate> returnStartDateRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<java.time.LocalDate> returnEndDateRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<java.util.Set<Long>> returnUsersRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<java.util.Set<com.pbl3.project.pbl3_project.entity.PaymentMethod>> returnMethodsRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<java.util.Set<com.pbl3.project.pbl3_project.entity.OrderStatus>> returnStatusesRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<BigDecimal> returnMinTotalRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<BigDecimal> returnMaxTotalRef = new java.util.concurrent.atomic.AtomicReference<>(null);

        Label returnRowCountLabel = createStatusMetaLabel("Showing 0-0 of 0 Row(s)");
        Label returnPageLabel = createStatusMetaLabel("Page 0 / 0");
        Button returnPrevBtn = createPageNavButton("Prev");
        Button returnNextBtn = createPageNavButton("Next");

        Runnable[] refreshReturnTableRef = new Runnable[1];
        Runnable updateReturnStatusBar = () -> updatePagedStatus(
            table,
            returnRowCountLabel,
            returnPageLabel,
            returnPrevBtn,
            returnNextBtn,
            returnTotalElements[0],
            returnCurrentPage[0],
            returnTotalPages[0],
            returnPageSize
        );
        Runnable loadReturnPage = () -> {
            org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.Order> pageData = orderService.searchReturnRefundOrders(
                user,
                scopeRef.get(),
                returnSearchRef.get(),
                returnStartDateRef.get(),
                returnEndDateRef.get(),
                returnUsersRef.get(),
                returnMethodsRef.get(),
                returnStatusesRef.get(),
                returnMinTotalRef.get(),
                returnMaxTotalRef.get(),
                createPageable(returnSortState, returnSortProperties, returnCurrentPage[0], returnPageSize)
            );
            if (pageData.getTotalPages() > 0 && returnCurrentPage[0] >= pageData.getTotalPages()) {
                returnCurrentPage[0] = pageData.getTotalPages() - 1;
                pageData = orderService.searchReturnRefundOrders(
                    user,
                    scopeRef.get(),
                    returnSearchRef.get(),
                    returnStartDateRef.get(),
                    returnEndDateRef.get(),
                    returnUsersRef.get(),
                    returnMethodsRef.get(),
                    returnStatusesRef.get(),
                    returnMinTotalRef.get(),
                    returnMaxTotalRef.get(),
                    createPageable(returnSortState, returnSortProperties, returnCurrentPage[0], returnPageSize)
                );
            }
            returnTotalElements[0] = pageData.getTotalElements();
            returnTotalPages[0] = pageData.getTotalPages();
            table.setItems(javafx.collections.FXCollections.observableArrayList(pageData.getContent()));
            updateReturnStatusBar.run();
        };
        refreshReturnTableRef[0] = loadReturnPage;
        returnPrevBtn.setOnAction(e -> {
            if (returnCurrentPage[0] > 0) {
                returnCurrentPage[0]--;
                loadReturnPage.run();
            }
        });
        returnNextBtn.setOnAction(e -> {
            if (returnCurrentPage[0] + 1 < returnTotalPages[0]) {
                returnCurrentPage[0]++;
                loadReturnPage.run();
            }
        });

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, Long> idCol = new javafx.scene.control.TableColumn<>("Order ID");
        idCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, String> dateCol = new javafx.scene.control.TableColumn<>("Created At");
        dateCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(formatDateTime(cell.getValue().getCreatedAt())));
        dateCol.setPrefWidth(185);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, String> customerCol = new javafx.scene.control.TableColumn<>("Customer");
        customerCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getCustomerDisplayName()));
        customerCol.setPrefWidth(170);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, String> userCol = new javafx.scene.control.TableColumn<>("Created By");
        userCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getCreatedByDisplayName()));
        userCol.setPrefWidth(170);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, String> statusCol = new javafx.scene.control.TableColumn<>("Status");
        statusCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(formatOrderStatus(cell.getValue().getStatus())));
        statusCol.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                String textColor = switch (item) {
                    case "Canceled" -> "-app-danger-hover";
                    case "Returned", "Partially Returned" -> "-app-primary-hover";
                    default -> "-app-success-hover";
                };
                setStyle("-fx-text-fill: " + textColor + "; -fx-font-weight: bold; -fx-alignment: CENTER;");
            }
        });
        statusCol.setPrefWidth(150);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, String> refundedCol = new javafx.scene.control.TableColumn<>("Refunded Amount");
        refundedCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(formatVnd(cell.getValue().getRefundedAmount())));
        refundedCol.setPrefWidth(150);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, String> grossTotalCol = new javafx.scene.control.TableColumn<>("Gross Total");
        grossTotalCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(formatVnd(cell.getValue().getGrossSubtotalSnapshot())));
        grossTotalCol.setPrefWidth(145);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, String> netTotalCol = new javafx.scene.control.TableColumn<>("Net Paid");
        netTotalCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(formatVnd(cell.getValue().getTotalPrice())));
        netTotalCol.setPrefWidth(145);

        table.getColumns().addAll(idCol, dateCol, customerCol, userCol, statusCol, refundedCol, grossTotalCol, netTotalCol);
        java.util.LinkedHashMap<String, javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, ?>> returnSortColumns =
            new java.util.LinkedHashMap<>();
        returnSortColumns.put("id", idCol);
        returnSortColumns.put("createdAt", dateCol);
        returnSortColumns.put("customerName", customerCol);
        returnSortColumns.put("userFullName", userCol);
        returnSortColumns.put("status", statusCol);
        returnSortColumns.put("refundedAmount", refundedCol);
        returnSortColumns.put("grossTotal", grossTotalCol);
        installSortHeaderIndicators(returnSortColumns);
        java.util.LinkedHashMap<String, String> returnSortLabels = new java.util.LinkedHashMap<>();
        returnSortLabels.put("id", "Order ID");
        returnSortLabels.put("createdAt", "Created At");
        returnSortLabels.put("customerName", "Customer");
        returnSortLabels.put("userFullName", "Created By");
        returnSortLabels.put("status", "Status");
        returnSortLabels.put("refundedAmount", "Refunded Amount");
        returnSortLabels.put("grossTotal", "Gross Total");
        Label returnSortStatusLabel = createSortStatusLabel(returnSortState, returnSortLabels);
        Runnable applyReturnSortUi = () -> {
            applySortStateToTable(table, returnSortColumns, returnSortState);
            returnSortStatusLabel.setText(buildSortStatusText(returnSortState, returnSortLabels));
        };
        applyReturnSortUi.run();
        installManualServerSorting(
            table,
            returnSortColumns,
            returnSortState,
            () -> {
                applyReturnSortUi.run();
                returnCurrentPage[0] = 0;
                loadReturnPage.run();
            }
        );

        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<com.pbl3.project.pbl3_project.entity.Order> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    try {
                        showOrderDetailsDialog(stage, orderService.getOrderWithItems(row.getItem().getId(), user), user, refreshReturnTableRef[0]);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Error", "Could not load order details: " + ex.getMessage());
                    }
                }
            });
            return row;
        });

        javafx.scene.layout.HBox returnSearchBox = new javafx.scene.layout.HBox(0);
        returnSearchBox.setAlignment(Pos.CENTER);
        returnSearchBox.getStyleClass().add("expandable-search-box");
        returnSearchBox.setPrefSize(40, 40);
        returnSearchBox.setMinSize(40, 40);
        returnSearchBox.setMaxSize(40, 40);
        javafx.scene.shape.SVGPath returnSearchIcon = new javafx.scene.shape.SVGPath();
        returnSearchIcon.setContent("M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
        returnSearchIcon.setFill(PRIMARY_COLOR);
        javafx.scene.layout.Region returnSpacer = new javafx.scene.layout.Region();
        returnSpacer.setMinWidth(0);
        returnSpacer.setPrefWidth(0);
        TextField returnField = new TextField();
        returnField.setPromptText(DEFAULT_SEARCH_PROMPT);
        returnField.getStyleClass().add("search-text-field");
        returnField.setMinWidth(0);
        returnField.setMaxWidth(0);
        returnField.setPrefWidth(0);
        returnField.setOpacity(0);
        returnSearchBox.getChildren().addAll(returnSearchIcon, returnSpacer, returnField);
        javafx.animation.Timeline returnExpand = new javafx.animation.Timeline(new javafx.animation.KeyFrame(Duration.millis(150),
            new javafx.animation.KeyValue(returnSearchBox.maxWidthProperty(), 250, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(returnSearchBox.prefWidthProperty(), 250, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(returnSpacer.minWidthProperty(), 8, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(returnField.minWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(returnField.maxWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(returnField.prefWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(returnField.opacityProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH)));
        javafx.animation.Timeline returnCollapse = new javafx.animation.Timeline(new javafx.animation.KeyFrame(Duration.millis(150),
            new javafx.animation.KeyValue(returnSearchBox.maxWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(returnSearchBox.prefWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(returnSpacer.minWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(returnField.minWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(returnField.maxWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(returnField.prefWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(returnField.opacityProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH)));

        javafx.animation.PauseTransition returnSearchPause = new javafx.animation.PauseTransition(Duration.millis(220));
        returnSearchPause.setOnFinished(e -> {
            returnCurrentPage[0] = 0;
            returnSearchRef.set(returnField.getText());
            loadReturnPage.run();
        });
        returnField.textProperty().addListener((obs, oldV, newV) -> returnSearchPause.playFromStart());

        VBox content = new VBox();
        applyStandardTablePageLayout(content);

        returnSearchBox.setOnMouseClicked(ev -> {
            if (returnSearchBox.getMaxWidth() == 40) {
                returnExpand.play();
                returnField.requestFocus();
            } else if (ev.getTarget() == returnSearchIcon || ev.getTarget() == returnSearchBox) {
                returnField.clear();
                content.requestFocus();
                returnCollapse.play();
            }
        });

        javafx.scene.layout.HBox returnFilterBox = new javafx.scene.layout.HBox();
        returnFilterBox.setAlignment(Pos.CENTER);
        returnFilterBox.getStyleClass().add("expandable-search-box");
        returnFilterBox.setPrefSize(40, 40);
        returnFilterBox.setMinSize(40, 40);
        returnFilterBox.setMaxSize(40, 40);
        returnFilterBox.setCursor(javafx.scene.Cursor.HAND);
        javafx.scene.shape.SVGPath returnFilterIcon = new javafx.scene.shape.SVGPath();
        returnFilterIcon.setContent("M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z");
        returnFilterIcon.setFill(PRIMARY_COLOR);
        returnFilterBox.getChildren().add(returnFilterIcon);

        Runnable updateReturnFilterAccent = () -> {
            boolean hasFilter = returnStartDateRef.get() != null
                || returnEndDateRef.get() != null
                || !returnUsersRef.get().isEmpty()
                || !returnMethodsRef.get().isEmpty()
                || !returnStatusesRef.get().isEmpty()
                || returnMinTotalRef.get() != null
                || returnMaxTotalRef.get() != null;
            returnFilterBox.setStyle(hasFilter ? "-fx-border-color: -app-primary;" : "");
        };

        javafx.scene.control.ComboBox<com.pbl3.project.pbl3_project.entity.ReturnRefundScope> scopeCombo = new javafx.scene.control.ComboBox<>();
        scopeCombo.getItems().addAll(com.pbl3.project.pbl3_project.entity.ReturnRefundScope.values());
        scopeCombo.setValue(scopeRef.get());
        scopeCombo.setPrefWidth(220);
        scopeCombo.setMinWidth(220);
        scopeCombo.setMaxWidth(220);
        scopeCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(com.pbl3.project.pbl3_project.entity.ReturnRefundScope scope) {
                return scope != null ? scope.getLabel() : "";
            }

            @Override
            public com.pbl3.project.pbl3_project.entity.ReturnRefundScope fromString(String string) {
                return java.util.Arrays.stream(com.pbl3.project.pbl3_project.entity.ReturnRefundScope.values())
                    .filter(scope -> scope.getLabel().equalsIgnoreCase(string))
                    .findFirst()
                    .orElse(com.pbl3.project.pbl3_project.entity.ReturnRefundScope.PROCESSED);
            }
        });
        scopeCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null || newValue == oldValue) {
                return;
            }
            scopeRef.set(newValue);
            returnStatusesRef.set(new java.util.LinkedHashSet<>());
            returnCurrentPage[0] = 0;
            updateReturnFilterAccent.run();
            loadReturnPage.run();
        });
        javafx.scene.layout.HBox scopeBox = new javafx.scene.layout.HBox(8, createStatusMetaLabel("Scope"), scopeCombo);
        scopeBox.setAlignment(Pos.CENTER_LEFT);

        javafx.stage.Popup returnFilterPopup = new javafx.stage.Popup();
        returnFilterPopup.setAutoHide(true);

        returnFilterBox.setOnMouseClicked(fev -> {
            fev.consume();
            try {
                if (returnFilterPopup.isShowing()) {
                    returnFilterPopup.hide();
                    return;
                }

                VBox popupContainer = new VBox(10);
                popupContainer.setPadding(new Insets(15));
                applyFilterPopupContainerStyle(popupContainer);
                popupContainer.setPrefWidth(350);

                VBox scrollContent = new VBox(10);
                scrollContent.setStyle("-fx-background-color: -app-surface;");
                scrollContent.setPadding(new Insets(5, 15, 5, 15));
                javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(scrollContent);
                scrollPane.setFitToWidth(true);
                scrollPane.setStyle("-fx-background: -app-surface; -fx-background-color: transparent; -fx-border-color: transparent;");
                scrollPane.setPrefViewportHeight(350);

                Label dateTitle = new Label("Date Range");
                dateTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");
                javafx.scene.control.DatePicker startDatePicker = new javafx.scene.control.DatePicker(returnStartDateRef.get());
                startDatePicker.setPromptText("Start Date");
                startDatePicker.setPrefWidth(140);
                startDatePicker.setStyle("-fx-font-size: 13px;");
                javafx.scene.control.DatePicker endDatePicker = new javafx.scene.control.DatePicker(returnEndDateRef.get());
                endDatePicker.setPromptText("End Date");
                endDatePicker.setPrefWidth(140);
                endDatePicker.setStyle("-fx-font-size: 13px;");
                customizeDatePicker(startDatePicker);
                customizeDatePicker(endDatePicker);
                javafx.scene.layout.HBox dateBox = new javafx.scene.layout.HBox(5, startDatePicker, new Label("-"), endDatePicker);
                dateBox.setAlignment(Pos.CENTER_LEFT);

                javafx.scene.control.Separator sepDate = new javafx.scene.control.Separator();

                Label userTitle = new Label("Created By");
                userTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");

                javafx.scene.control.CheckBox allUsersCb = new javafx.scene.control.CheckBox("All Users");
                allUsersCb.setSelected(returnUsersRef.get().isEmpty());
                allUsersCb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");

                VBox userScroll = new VBox(8);
                userScroll.setPadding(new Insets(5, 5, 5, 20));

                java.util.List<javafx.scene.control.CheckBox> userCbs = new java.util.ArrayList<>();
                java.util.List<com.pbl3.project.pbl3_project.dto.IdLabelOption> userOptions = orderService.getOrderCreatorOptions(user);
                java.util.Set<Long> activeUserFilters = returnUsersRef.get();
                for (com.pbl3.project.pbl3_project.dto.IdLabelOption option : userOptions) {
                    if (option.label() == null || option.label().trim().isEmpty()) {
                        continue;
                    }
                    javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(option.label());
                    cb.setUserData(option.id());
                    cb.setSelected(activeUserFilters.isEmpty() || activeUserFilters.contains(option.id()));
                    cb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                    cb.setOnAction(e -> {
                        if (!cb.isSelected()) {
                            allUsersCb.setSelected(false);
                        } else {
                            boolean all = true;
                            for (javafx.scene.control.CheckBox c : userCbs) {
                                if (!c.isSelected()) {
                                    all = false;
                                }
                            }
                            allUsersCb.setSelected(all);
                        }
                    });
                    userCbs.add(cb);
                    userScroll.getChildren().add(cb);
                }
                allUsersCb.setOnAction(e -> {
                    boolean sel = allUsersCb.isSelected();
                    for (javafx.scene.control.CheckBox cb : userCbs) {
                        cb.setSelected(sel);
                    }
                });
                FilterDisclosureSection userSection = new FilterDisclosureSection(allUsersCb, userScroll);

                javafx.scene.control.Separator sepUser = new javafx.scene.control.Separator();

                Label methodTitle = new Label("Payment Method");
                methodTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");

                javafx.scene.control.CheckBox allMethodsCb = new javafx.scene.control.CheckBox("All Methods");
                allMethodsCb.setSelected(returnMethodsRef.get().isEmpty());
                allMethodsCb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");

                VBox methodScroll = new VBox(8);
                methodScroll.setPadding(new Insets(5, 5, 5, 20));

                java.util.List<javafx.scene.control.CheckBox> methodCbs = new java.util.ArrayList<>();
                java.util.Set<com.pbl3.project.pbl3_project.entity.PaymentMethod> activeMethodFilters = returnMethodsRef.get();
                for (com.pbl3.project.pbl3_project.entity.PaymentMethod paymentMethod : com.pbl3.project.pbl3_project.entity.PaymentMethod.values()) {
                    javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(paymentMethod.name());
                    cb.setSelected(activeMethodFilters.isEmpty() || activeMethodFilters.contains(paymentMethod));
                    cb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                    cb.setOnAction(e -> {
                        if (!cb.isSelected()) {
                            allMethodsCb.setSelected(false);
                        } else {
                            boolean all = true;
                            for (javafx.scene.control.CheckBox c : methodCbs) {
                                if (!c.isSelected()) {
                                    all = false;
                                }
                            }
                            allMethodsCb.setSelected(all);
                        }
                    });
                    methodCbs.add(cb);
                    methodScroll.getChildren().add(cb);
                }
                allMethodsCb.setOnAction(e -> {
                    boolean sel = allMethodsCb.isSelected();
                    for (javafx.scene.control.CheckBox cb : methodCbs) {
                        cb.setSelected(sel);
                    }
                });
                FilterDisclosureSection methodSection = new FilterDisclosureSection(allMethodsCb, methodScroll);

                javafx.scene.control.Separator sepMethod = new javafx.scene.control.Separator();

                Label statusTitle = new Label("Return / Refund Status");
                statusTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");

                java.util.Set<com.pbl3.project.pbl3_project.entity.OrderStatus> scopeStatuses = scopeRef.get().getStatuses();
                java.util.Set<com.pbl3.project.pbl3_project.entity.OrderStatus> activeStatusFilters = returnStatusesRef.get();
                javafx.scene.control.CheckBox allStatusesCb = new javafx.scene.control.CheckBox("All In Scope");
                allStatusesCb.setSelected(activeStatusFilters.isEmpty());
                allStatusesCb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");

                VBox statusScroll = new VBox(8);
                statusScroll.setPadding(new Insets(5, 5, 5, 20));

                java.util.List<javafx.scene.control.CheckBox> statusCbs = new java.util.ArrayList<>();
                for (com.pbl3.project.pbl3_project.entity.OrderStatus status : com.pbl3.project.pbl3_project.entity.OrderStatus.values()) {
                    if (!scopeStatuses.contains(status)) {
                        continue;
                    }
                    javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(formatOrderStatus(status));
                    cb.setUserData(status);
                    cb.setSelected(activeStatusFilters.isEmpty() || activeStatusFilters.contains(status));
                    cb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                    cb.setOnAction(e -> {
                        if (!cb.isSelected()) {
                            allStatusesCb.setSelected(false);
                        } else {
                            boolean all = true;
                            for (javafx.scene.control.CheckBox c : statusCbs) {
                                if (!c.isSelected()) {
                                    all = false;
                                }
                            }
                            allStatusesCb.setSelected(all);
                        }
                    });
                    statusCbs.add(cb);
                    statusScroll.getChildren().add(cb);
                }
                allStatusesCb.setOnAction(e -> {
                    boolean sel = allStatusesCb.isSelected();
                    for (javafx.scene.control.CheckBox cb : statusCbs) {
                        cb.setSelected(sel);
                    }
                });
                FilterDisclosureSection statusSection = new FilterDisclosureSection(allStatusesCb, statusScroll);

                javafx.scene.control.Separator sepStatus = new javafx.scene.control.Separator();

                Label priceTitle = new Label("Net Paid Range");
                priceTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");

                BigDecimal maxPriceValue = orderService.getOrderMaxTotalPrice(user);
                double maxPrice = maxPriceValue == null ? 0.0 : maxPriceValue.doubleValue();
                if (maxPrice == 0) {
                    maxPrice = 1000;
                }

                double initialMinPrice = returnMinTotalRef.get() == null ? 0.0 : returnMinTotalRef.get().doubleValue();
                double initialMaxPrice = returnMaxTotalRef.get() == null ? maxPrice : Math.min(maxPrice, returnMaxTotalRef.get().doubleValue());
                Label priceLabel = new Label(String.format("%.0f - %.0f VND", initialMinPrice, initialMaxPrice));
                priceLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -app-primary; -fx-font-weight: bold;");

                RangeSlider priceSlider = new RangeSlider(0, maxPrice, initialMinPrice, initialMaxPrice, 280);
                priceSlider.minVal.addListener((o, ov, nv) ->
                    priceLabel.setText(String.format("%.0f - %.0f VND", nv.doubleValue(), priceSlider.maxVal.get()))
                );
                priceSlider.maxVal.addListener((o, ov, nv) ->
                    priceLabel.setText(String.format("%.0f - %.0f VND", priceSlider.minVal.get(), nv.doubleValue()))
                );

                scrollContent.getChildren().addAll(
                    dateTitle, dateBox, sepDate,
                    userTitle, userSection.getNode(), sepUser,
                    methodTitle, methodSection.getNode(), sepMethod,
                    statusTitle, statusSection.getNode(), sepStatus,
                    priceTitle, priceLabel, priceSlider
                );

                javafx.scene.layout.HBox btnRow = new javafx.scene.layout.HBox(10);
                btnRow.setAlignment(Pos.CENTER_RIGHT);

                final double finalMaxPrice = maxPrice;
                Button resetBtn = new Button("Reset");
                resetBtn.getStyleClass().add("filter-reset-btn");
                resetBtn.setOnAction(ae -> {
                    returnStartDateRef.set(null);
                    returnEndDateRef.set(null);
                    returnUsersRef.set(new java.util.LinkedHashSet<>());
                    returnMethodsRef.set(new java.util.LinkedHashSet<>());
                    returnStatusesRef.set(new java.util.LinkedHashSet<>());
                    returnMinTotalRef.set(null);
                    returnMaxTotalRef.set(null);
                    returnCurrentPage[0] = 0;
                    updateReturnFilterAccent.run();
                    loadReturnPage.run();
                    returnFilterPopup.hide();
                });

                Button applyBtn = new Button("Apply Filter");
                applyBtn.getStyleClass().add("filter-apply-btn");
                applyBtn.setOnAction(ae -> {
                    java.util.Set<Long> selectedUsers = new java.util.LinkedHashSet<>();
                    for (javafx.scene.control.CheckBox cb : userCbs) {
                        if (cb.isSelected() && cb.getUserData() instanceof Long userId) {
                            selectedUsers.add(userId);
                        }
                    }

                    java.util.Set<com.pbl3.project.pbl3_project.entity.PaymentMethod> selectedMethods = new java.util.LinkedHashSet<>();
                    for (javafx.scene.control.CheckBox cb : methodCbs) {
                        if (cb.isSelected()) {
                            selectedMethods.add(com.pbl3.project.pbl3_project.entity.PaymentMethod.valueOf(cb.getText()));
                        }
                    }

                    java.util.Set<com.pbl3.project.pbl3_project.entity.OrderStatus> selectedStatuses = new java.util.LinkedHashSet<>();
                    for (javafx.scene.control.CheckBox cb : statusCbs) {
                        if (cb.isSelected()) {
                            selectedStatuses.add((com.pbl3.project.pbl3_project.entity.OrderStatus) cb.getUserData());
                        }
                    }

                    double pMin = priceSlider.minVal.get();
                    double pMax = priceSlider.maxVal.get();

                    returnStartDateRef.set(startDatePicker.getValue());
                    returnEndDateRef.set(endDatePicker.getValue());
                    returnUsersRef.set(allUsersCb.isSelected() ? new java.util.LinkedHashSet<>() : selectedUsers);
                    returnMethodsRef.set(allMethodsCb.isSelected() ? new java.util.LinkedHashSet<>() : selectedMethods);
                    returnStatusesRef.set(allStatusesCb.isSelected() ? new java.util.LinkedHashSet<>() : selectedStatuses);
                    returnMinTotalRef.set(pMin <= 0 ? null : MoneySupport.normalize(BigDecimal.valueOf(pMin)));
                    returnMaxTotalRef.set(pMax >= finalMaxPrice ? null : MoneySupport.normalize(BigDecimal.valueOf(pMax)));
                    returnCurrentPage[0] = 0;
                    updateReturnFilterAccent.run();
                    loadReturnPage.run();
                    returnFilterPopup.hide();
                });

                btnRow.getChildren().addAll(resetBtn, applyBtn);

                popupContainer.getChildren().addAll(scrollPane, btnRow);
                returnFilterPopup.getContent().clear();
                returnFilterPopup.getContent().add(popupContainer);

                showPopupBelow(returnFilterPopup, returnFilterBox, -290, 5);
            } catch (Exception ex) {
                showUserFacingError(ex);
            }
        });

        Label returnHeader = new Label("Returns / Refunds");
        returnHeader.getStyleClass().add("header-label");
        Button manageReturnBtn = createExpandableManageActionButton("Manage Return/Refund", 220);
        manageReturnBtn.setDisable(true);
        manageReturnBtn.setOnAction(e -> {
            java.util.List<com.pbl3.project.pbl3_project.entity.Order> selectedOrders = new java.util.ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selectedOrders.size() != 1) {
                toastService.showWarning("Select exactly one order to manage");
                return;
            }
            try {
                showOrderDetailsDialog(stage, orderService.getOrderWithItems(selectedOrders.get(0).getId(), user), user, refreshReturnTableRef[0]);
            } catch (Exception ex) {
                showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Error", "Could not load order details: " + ex.getMessage());
            }
        });
        table.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<com.pbl3.project.pbl3_project.entity.Order>) c ->
            manageReturnBtn.setDisable(table.getSelectionModel().getSelectedItems().size() != 1)
        );

        javafx.scene.layout.BorderPane returnToolbar = new javafx.scene.layout.BorderPane();
        returnToolbar.setLeft(returnHeader);
        javafx.scene.layout.HBox returnRightBox = new javafx.scene.layout.HBox(15, scopeBox, returnFilterBox, returnSearchBox, manageReturnBtn);
        returnRightBox.setAlignment(Pos.CENTER_RIGHT);
        returnToolbar.setRight(returnRightBox);
        javafx.scene.layout.BorderPane.setAlignment(returnHeader, Pos.CENTER_LEFT);

        javafx.scene.layout.HBox returnStatusBar = new javafx.scene.layout.HBox(
            15,
            returnSortStatusLabel,
            returnRowCountLabel,
            returnPageLabel,
            returnPrevBtn,
            returnNextBtn
        );
        applyStandardTableStatusBar(returnStatusBar);

        content.getChildren().addAll(returnToolbar, table, returnStatusBar);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);

        table.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<com.pbl3.project.pbl3_project.entity.Order>) c -> updateReturnStatusBar.run());
        loadReturnPage.run();
        updateReturnFilterAccent.run();
        enableDeselectOnOutsideClick(content, table);
        return content;
    }

    private VBox createExpensesView(
        Stage stage,
        com.pbl3.project.pbl3_project.entity.User user,
        java.time.LocalDate initialStartDate,
        java.time.LocalDate initialEndDate
    ) {
        final String expenseSortStateKey = "expenses";
        TableSortState expenseSortState = getOrCreateTableSortState(
            expenseSortStateKey,
            new SortCriterion("spentOn", javafx.scene.control.TableColumn.SortType.DESCENDING),
            new SortCriterion("id", javafx.scene.control.TableColumn.SortType.DESCENDING)
        );
        java.util.LinkedHashMap<String, String> expenseSortProperties = new java.util.LinkedHashMap<>();
        expenseSortProperties.put("id", "id");
        expenseSortProperties.put("spentOn", "spentOn");
        expenseSortProperties.put("category", "category");
        expenseSortProperties.put("title", "title");
        expenseSortProperties.put("amount", "amount");
        expenseSortProperties.put("paymentMethod", "paymentMethod");
        expenseSortProperties.put("createdBy", "createdBy.fullName");

        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.Expense> table = new javafx.scene.control.TableView<>();
        applyStandardTableSizing(table);
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.SINGLE);

        final int expensePageSize = 20;
        final int[] expenseCurrentPage = {0};
        final int[] expenseTotalPages = {0};
        final long[] expenseTotalElements = {0L};
        java.util.concurrent.atomic.AtomicReference<String> expenseSearchRef = new java.util.concurrent.atomic.AtomicReference<>("");
        java.util.concurrent.atomic.AtomicReference<java.time.LocalDate> expenseStartDateRef = new java.util.concurrent.atomic.AtomicReference<>(initialStartDate);
        java.util.concurrent.atomic.AtomicReference<java.time.LocalDate> expenseEndDateRef = new java.util.concurrent.atomic.AtomicReference<>(initialEndDate);
        java.util.concurrent.atomic.AtomicReference<java.util.Set<com.pbl3.project.pbl3_project.entity.ExpenseCategory>> expenseCategoriesRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<java.util.Set<com.pbl3.project.pbl3_project.entity.PaymentMethod>> expenseMethodsRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<java.util.Set<Long>> expenseCreatorsRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<BigDecimal> expenseMinAmountRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<BigDecimal> expenseMaxAmountRef = new java.util.concurrent.atomic.AtomicReference<>(null);

        Label expenseRowCountLabel = createStatusMetaLabel("Showing 0-0 of 0 Row(s)");
        Label expensePageLabel = createStatusMetaLabel("Page 0 / 0");
        Button expensePrevBtn = createPageNavButton("Prev");
        Button expenseNextBtn = createPageNavButton("Next");

        Runnable[] refreshExpenseTableRef = new Runnable[1];
        Runnable updateExpenseStatusBar = () -> updatePagedStatus(
            table,
            expenseRowCountLabel,
            expensePageLabel,
            expensePrevBtn,
            expenseNextBtn,
            expenseTotalElements[0],
            expenseCurrentPage[0],
            expenseTotalPages[0],
            expensePageSize
        );
        Runnable loadExpensePage = () -> {
            org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.Expense> pageData = expenseService.searchExpenses(
                user,
                expenseSearchRef.get(),
                expenseStartDateRef.get(),
                expenseEndDateRef.get(),
                expenseCategoriesRef.get(),
                expenseMethodsRef.get(),
                expenseCreatorsRef.get(),
                expenseMinAmountRef.get(),
                expenseMaxAmountRef.get(),
                createPageable(expenseSortState, expenseSortProperties, expenseCurrentPage[0], expensePageSize)
            );
            if (pageData.getTotalPages() > 0 && expenseCurrentPage[0] >= pageData.getTotalPages()) {
                expenseCurrentPage[0] = pageData.getTotalPages() - 1;
                pageData = expenseService.searchExpenses(
                    user,
                    expenseSearchRef.get(),
                    expenseStartDateRef.get(),
                    expenseEndDateRef.get(),
                    expenseCategoriesRef.get(),
                    expenseMethodsRef.get(),
                    expenseCreatorsRef.get(),
                    expenseMinAmountRef.get(),
                    expenseMaxAmountRef.get(),
                    createPageable(expenseSortState, expenseSortProperties, expenseCurrentPage[0], expensePageSize)
                );
            }
            expenseTotalElements[0] = pageData.getTotalElements();
            expenseTotalPages[0] = pageData.getTotalPages();
            table.setItems(javafx.collections.FXCollections.observableArrayList(pageData.getContent()));
            updateExpenseStatusBar.run();
        };
        refreshExpenseTableRef[0] = loadExpensePage;

        expensePrevBtn.setOnAction(e -> {
            if (expenseCurrentPage[0] > 0) {
                expenseCurrentPage[0]--;
                loadExpensePage.run();
            }
        });
        expenseNextBtn.setOnAction(e -> {
            if (expenseCurrentPage[0] + 1 < expenseTotalPages[0]) {
                expenseCurrentPage[0]++;
                loadExpensePage.run();
            }
        });

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Expense, Long> idCol = new javafx.scene.control.TableColumn<>("Expense ID");
        idCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));
        idCol.setPrefWidth(110);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Expense, String> spentOnCol = new javafx.scene.control.TableColumn<>("Spent On");
        spentOnCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(formatDate(cell.getValue().getSpentOn())));
        spentOnCol.setPrefWidth(125);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Expense, String> categoryCol = new javafx.scene.control.TableColumn<>("Category");
        categoryCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(formatExpenseCategoryLabel(cell.getValue().getCategory())));
        categoryCol.setPrefWidth(145);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Expense, String> titleCol = new javafx.scene.control.TableColumn<>("Title");
        titleCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(220);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Expense, String> amountCol = new javafx.scene.control.TableColumn<>("Amount");
        amountCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(formatVnd(cell.getValue().getAmount())));
        amountCol.setPrefWidth(140);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Expense, String> methodCol = new javafx.scene.control.TableColumn<>("Payment Method");
        methodCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(formatPaymentMethodLabel(cell.getValue().getPaymentMethod())));
        methodCol.setPrefWidth(150);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Expense, String> createdByCol = new javafx.scene.control.TableColumn<>("Created By");
        createdByCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getCreatedByDisplayName()));
        createdByCol.setPrefWidth(170);

        table.getColumns().addAll(idCol, spentOnCol, categoryCol, titleCol, amountCol, methodCol, createdByCol);

        java.util.LinkedHashMap<String, javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Expense, ?>> expenseSortColumns =
            new java.util.LinkedHashMap<>();
        expenseSortColumns.put("id", idCol);
        expenseSortColumns.put("spentOn", spentOnCol);
        expenseSortColumns.put("category", categoryCol);
        expenseSortColumns.put("title", titleCol);
        expenseSortColumns.put("amount", amountCol);
        expenseSortColumns.put("paymentMethod", methodCol);
        expenseSortColumns.put("createdBy", createdByCol);
        installSortHeaderIndicators(expenseSortColumns);

        java.util.LinkedHashMap<String, String> expenseSortLabels = new java.util.LinkedHashMap<>();
        expenseSortLabels.put("id", "Expense ID");
        expenseSortLabels.put("spentOn", "Spent On");
        expenseSortLabels.put("category", "Category");
        expenseSortLabels.put("title", "Title");
        expenseSortLabels.put("amount", "Amount");
        expenseSortLabels.put("paymentMethod", "Payment Method");
        expenseSortLabels.put("createdBy", "Created By");
        Label expenseSortStatusLabel = createSortStatusLabel(expenseSortState, expenseSortLabels);
        Runnable applyExpenseSortUi = () -> {
            applySortStateToTable(table, expenseSortColumns, expenseSortState);
            expenseSortStatusLabel.setText(buildSortStatusText(expenseSortState, expenseSortLabels));
        };
        applyExpenseSortUi.run();
        installManualServerSorting(
            table,
            expenseSortColumns,
            expenseSortState,
            () -> {
                applyExpenseSortUi.run();
                expenseCurrentPage[0] = 0;
                loadExpensePage.run();
            }
        );

        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<com.pbl3.project.pbl3_project.entity.Expense> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showExpenseDialog(stage, user, row.getItem(), refreshExpenseTableRef[0]);
                }
            });
            return row;
        });

        ExpandableSearchControl expenseSearchControl = createExpandableSearchControl(250);
        javafx.animation.PauseTransition expenseSearchPause = new javafx.animation.PauseTransition(Duration.millis(220));
        expenseSearchPause.setOnFinished(e -> {
            expenseCurrentPage[0] = 0;
            expenseSearchRef.set(expenseSearchControl.field().getText());
            loadExpensePage.run();
        });
        expenseSearchControl.field().textProperty().addListener((obs, oldV, newV) -> expenseSearchPause.playFromStart());

        javafx.scene.layout.HBox expenseFilterBox = new javafx.scene.layout.HBox();
        expenseFilterBox.setAlignment(Pos.CENTER);
        expenseFilterBox.getStyleClass().add("expandable-search-box");
        expenseFilterBox.setPrefSize(40, 40);
        expenseFilterBox.setMinSize(40, 40);
        expenseFilterBox.setMaxSize(40, 40);
        expenseFilterBox.setCursor(javafx.scene.Cursor.HAND);
        javafx.scene.shape.SVGPath expenseFilterIcon = new javafx.scene.shape.SVGPath();
        expenseFilterIcon.setContent("M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z");
        expenseFilterIcon.setFill(PRIMARY_COLOR);
        expenseFilterBox.getChildren().add(expenseFilterIcon);

        Runnable updateExpenseFilterAccent = () -> {
            boolean hasFilter = expenseStartDateRef.get() != null
                || expenseEndDateRef.get() != null
                || !expenseCategoriesRef.get().isEmpty()
                || !expenseMethodsRef.get().isEmpty()
                || !expenseCreatorsRef.get().isEmpty()
                || expenseMinAmountRef.get() != null
                || expenseMaxAmountRef.get() != null;
            expenseFilterBox.setStyle(hasFilter ? "-fx-border-color: -app-primary;" : "");
        };

        javafx.stage.Popup expenseFilterPopup = new javafx.stage.Popup();
        expenseFilterPopup.setAutoHide(true);
        expenseFilterBox.setOnMouseClicked(fev -> {
            fev.consume();
            try {
                if (expenseFilterPopup.isShowing()) {
                    expenseFilterPopup.hide();
                    return;
                }

                FilterPopupShell shell = createFilterPopupShell(360, 360);
                VBox scrollContent = shell.content();

                Label dateTitle = createFilterPopupSectionTitle("Date Range");
                javafx.scene.control.DatePicker startDatePicker = new javafx.scene.control.DatePicker(expenseStartDateRef.get());
                startDatePicker.setPromptText("Start Date");
                startDatePicker.setPrefWidth(140);
                javafx.scene.control.DatePicker endDatePicker = new javafx.scene.control.DatePicker(expenseEndDateRef.get());
                endDatePicker.setPromptText("End Date");
                endDatePicker.setPrefWidth(140);
                customizeDatePicker(startDatePicker);
                customizeDatePicker(endDatePicker);
                javafx.scene.layout.HBox dateBox = new javafx.scene.layout.HBox(5, startDatePicker, new Label("-"), endDatePicker);
                dateBox.setAlignment(Pos.CENTER_LEFT);

                javafx.scene.control.Separator sepDate = new javafx.scene.control.Separator();

                Label categoryTitle = createFilterPopupSectionTitle("Category");
                javafx.scene.control.CheckBox allCategoriesCb = new javafx.scene.control.CheckBox("All Categories");
                allCategoriesCb.setSelected(expenseCategoriesRef.get().isEmpty());
                allCategoriesCb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                VBox categoryScroll = new VBox(8);
                categoryScroll.setPadding(new Insets(5, 5, 5, 20));
                java.util.List<javafx.scene.control.CheckBox> categoryCbs = new java.util.ArrayList<>();
                java.util.Set<com.pbl3.project.pbl3_project.entity.ExpenseCategory> activeCategories = expenseCategoriesRef.get();
                for (com.pbl3.project.pbl3_project.entity.ExpenseCategory category : com.pbl3.project.pbl3_project.entity.ExpenseCategory.values()) {
                    javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(formatExpenseCategoryLabel(category));
                    cb.setUserData(category);
                    cb.setSelected(activeCategories.isEmpty() || activeCategories.contains(category));
                    cb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                    cb.setOnAction(e -> {
                        if (!cb.isSelected()) {
                            allCategoriesCb.setSelected(false);
                        } else {
                            boolean all = true;
                            for (javafx.scene.control.CheckBox child : categoryCbs) {
                                if (!child.isSelected()) {
                                    all = false;
                                    break;
                                }
                            }
                            allCategoriesCb.setSelected(all);
                        }
                    });
                    categoryCbs.add(cb);
                    categoryScroll.getChildren().add(cb);
                }
                allCategoriesCb.setOnAction(e -> {
                    boolean selected = allCategoriesCb.isSelected();
                    for (javafx.scene.control.CheckBox cb : categoryCbs) {
                        cb.setSelected(selected);
                    }
                });
                FilterDisclosureSection categorySection = new FilterDisclosureSection(allCategoriesCb, categoryScroll);

                javafx.scene.control.Separator sepCategory = new javafx.scene.control.Separator();

                Label methodTitle = createFilterPopupSectionTitle("Payment Method");
                javafx.scene.control.CheckBox allMethodsCb = new javafx.scene.control.CheckBox("All Methods");
                allMethodsCb.setSelected(expenseMethodsRef.get().isEmpty());
                allMethodsCb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                VBox methodScroll = new VBox(8);
                methodScroll.setPadding(new Insets(5, 5, 5, 20));
                java.util.List<javafx.scene.control.CheckBox> methodCbs = new java.util.ArrayList<>();
                java.util.Set<com.pbl3.project.pbl3_project.entity.PaymentMethod> activeMethods = expenseMethodsRef.get();
                for (com.pbl3.project.pbl3_project.entity.PaymentMethod method : com.pbl3.project.pbl3_project.entity.PaymentMethod.values()) {
                    javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(formatPaymentMethodLabel(method));
                    cb.setUserData(method);
                    cb.setSelected(activeMethods.isEmpty() || activeMethods.contains(method));
                    cb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                    cb.setOnAction(e -> {
                        if (!cb.isSelected()) {
                            allMethodsCb.setSelected(false);
                        } else {
                            boolean all = true;
                            for (javafx.scene.control.CheckBox child : methodCbs) {
                                if (!child.isSelected()) {
                                    all = false;
                                    break;
                                }
                            }
                            allMethodsCb.setSelected(all);
                        }
                    });
                    methodCbs.add(cb);
                    methodScroll.getChildren().add(cb);
                }
                allMethodsCb.setOnAction(e -> {
                    boolean selected = allMethodsCb.isSelected();
                    for (javafx.scene.control.CheckBox cb : methodCbs) {
                        cb.setSelected(selected);
                    }
                });
                FilterDisclosureSection methodSection = new FilterDisclosureSection(allMethodsCb, methodScroll);

                javafx.scene.control.Separator sepMethod = new javafx.scene.control.Separator();

                Label creatorTitle = createFilterPopupSectionTitle("Created By");
                javafx.scene.control.CheckBox allCreatorsCb = new javafx.scene.control.CheckBox("All Creators");
                allCreatorsCb.setSelected(expenseCreatorsRef.get().isEmpty());
                allCreatorsCb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                VBox creatorScroll = new VBox(8);
                creatorScroll.setPadding(new Insets(5, 5, 5, 20));
                java.util.List<javafx.scene.control.CheckBox> creatorCbs = new java.util.ArrayList<>();
                java.util.Set<Long> activeCreators = expenseCreatorsRef.get();
                for (com.pbl3.project.pbl3_project.dto.IdLabelOption option : expenseService.getExpenseCreatorOptions(user)) {
                    if (option.label() == null || option.label().isBlank()) {
                        continue;
                    }
                    javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(option.label());
                    cb.setUserData(option.id());
                    cb.setSelected(activeCreators.isEmpty() || activeCreators.contains(option.id()));
                    cb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                    cb.setOnAction(e -> {
                        if (!cb.isSelected()) {
                            allCreatorsCb.setSelected(false);
                        } else {
                            boolean all = true;
                            for (javafx.scene.control.CheckBox child : creatorCbs) {
                                if (!child.isSelected()) {
                                    all = false;
                                    break;
                                }
                            }
                            allCreatorsCb.setSelected(all);
                        }
                    });
                    creatorCbs.add(cb);
                    creatorScroll.getChildren().add(cb);
                }
                allCreatorsCb.setOnAction(e -> {
                    boolean selected = allCreatorsCb.isSelected();
                    for (javafx.scene.control.CheckBox cb : creatorCbs) {
                        cb.setSelected(selected);
                    }
                });
                FilterDisclosureSection creatorSection = new FilterDisclosureSection(allCreatorsCb, creatorScroll);

                javafx.scene.control.Separator sepCreator = new javafx.scene.control.Separator();

                Label amountTitle = createFilterPopupSectionTitle("Amount Range");
                BigDecimal maxAmountValue = expenseService.getExpenseMaxAmount(user);
                double maxAmount = maxAmountValue == null ? 0.0 : maxAmountValue.doubleValue();
                if (maxAmount <= 0) {
                    maxAmount = 1_000_000;
                }
                double initialMinAmount = expenseMinAmountRef.get() == null ? 0.0 : expenseMinAmountRef.get().doubleValue();
                double initialMaxAmount = expenseMaxAmountRef.get() == null ? maxAmount : Math.min(maxAmount, expenseMaxAmountRef.get().doubleValue());
                Label amountLabel = new Label(
                    formatVnd(MoneySupport.normalize(BigDecimal.valueOf(initialMinAmount)))
                        + " - "
                        + formatVnd(MoneySupport.normalize(BigDecimal.valueOf(initialMaxAmount)))
                );
                amountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -app-primary; -fx-font-weight: bold;");
                RangeSlider amountSlider = new RangeSlider(0, maxAmount, initialMinAmount, initialMaxAmount, 280);
                amountSlider.minVal.addListener((o, ov, nv) -> amountLabel.setText(
                    formatVnd(MoneySupport.normalize(BigDecimal.valueOf(nv.doubleValue())))
                        + " - "
                        + formatVnd(MoneySupport.normalize(BigDecimal.valueOf(amountSlider.maxVal.get())))
                ));
                amountSlider.maxVal.addListener((o, ov, nv) -> amountLabel.setText(
                    formatVnd(MoneySupport.normalize(BigDecimal.valueOf(amountSlider.minVal.get())))
                        + " - "
                        + formatVnd(MoneySupport.normalize(BigDecimal.valueOf(nv.doubleValue())))
                ));

                scrollContent.getChildren().addAll(
                    dateTitle, dateBox, sepDate,
                    categoryTitle, categorySection.getNode(), sepCategory,
                    methodTitle, methodSection.getNode(), sepMethod,
                    creatorTitle, creatorSection.getNode(), sepCreator,
                    amountTitle, amountLabel, amountSlider
                );

                final double finalMaxAmount = maxAmount;
                Button resetBtn = new Button("Reset");
                resetBtn.getStyleClass().add("filter-reset-btn");
                resetBtn.setOnAction(ae -> {
                    expenseStartDateRef.set(null);
                    expenseEndDateRef.set(null);
                    expenseCategoriesRef.set(new java.util.LinkedHashSet<>());
                    expenseMethodsRef.set(new java.util.LinkedHashSet<>());
                    expenseCreatorsRef.set(new java.util.LinkedHashSet<>());
                    expenseMinAmountRef.set(null);
                    expenseMaxAmountRef.set(null);
                    expenseCurrentPage[0] = 0;
                    updateExpenseFilterAccent.run();
                    loadExpensePage.run();
                    expenseFilterPopup.hide();
                });

                Button applyBtn = new Button("Apply");
                applyBtn.getStyleClass().add("filter-apply-btn");
                applyBtn.setOnAction(ae -> {
                    java.util.Set<com.pbl3.project.pbl3_project.entity.ExpenseCategory> selectedCategories = new java.util.LinkedHashSet<>();
                    for (javafx.scene.control.CheckBox cb : categoryCbs) {
                        if (cb.isSelected()) {
                            selectedCategories.add((com.pbl3.project.pbl3_project.entity.ExpenseCategory) cb.getUserData());
                        }
                    }

                    java.util.Set<com.pbl3.project.pbl3_project.entity.PaymentMethod> selectedMethods = new java.util.LinkedHashSet<>();
                    for (javafx.scene.control.CheckBox cb : methodCbs) {
                        if (cb.isSelected()) {
                            selectedMethods.add((com.pbl3.project.pbl3_project.entity.PaymentMethod) cb.getUserData());
                        }
                    }

                    java.util.Set<Long> selectedCreators = new java.util.LinkedHashSet<>();
                    for (javafx.scene.control.CheckBox cb : creatorCbs) {
                        if (cb.isSelected()) {
                            selectedCreators.add((Long) cb.getUserData());
                        }
                    }

                    double minAmount = amountSlider.minVal.get();
                    double maxAmountSelected = amountSlider.maxVal.get();
                    expenseStartDateRef.set(startDatePicker.getValue());
                    expenseEndDateRef.set(endDatePicker.getValue());
                    expenseCategoriesRef.set(allCategoriesCb.isSelected() ? new java.util.LinkedHashSet<>() : selectedCategories);
                    expenseMethodsRef.set(allMethodsCb.isSelected() ? new java.util.LinkedHashSet<>() : selectedMethods);
                    expenseCreatorsRef.set(allCreatorsCb.isSelected() ? new java.util.LinkedHashSet<>() : selectedCreators);
                    expenseMinAmountRef.set(minAmount <= 0 ? null : MoneySupport.normalize(BigDecimal.valueOf(minAmount)));
                    expenseMaxAmountRef.set(maxAmountSelected >= finalMaxAmount ? null : MoneySupport.normalize(BigDecimal.valueOf(maxAmountSelected)));
                    expenseCurrentPage[0] = 0;
                    updateExpenseFilterAccent.run();
                    loadExpensePage.run();
                    expenseFilterPopup.hide();
                });

                shell.container().getChildren().add(createFilterPopupActionRow(resetBtn, applyBtn));
                expenseFilterPopup.getContent().clear();
                expenseFilterPopup.getContent().add(shell.container());
                showPopupBelow(expenseFilterPopup, expenseFilterBox, -300, 5);
            } catch (Exception ex) {
                showUserFacingError(ex);
            }
        });

        Label expenseHeader = new Label("Expenses");
        expenseHeader.getStyleClass().add("header-label");

        Button newExpenseBtn = createExpandableGreenActionButton("New Expense", 180);
        newExpenseBtn.setOnAction(e -> showExpenseDialog(stage, user, null, refreshExpenseTableRef[0]));

        Button editExpenseBtn = createExpandableManageActionButton("Edit", 110);
        editExpenseBtn.setDisable(true);
        editExpenseBtn.setOnAction(e -> {
            com.pbl3.project.pbl3_project.entity.Expense selectedExpense = table.getSelectionModel().getSelectedItem();
            if (selectedExpense == null) {
                toastService.showWarning("Select one expense to edit");
                return;
            }
            showExpenseDialog(stage, user, selectedExpense, refreshExpenseTableRef[0]);
        });

        Button deleteExpenseBtn = createExpandableManageActionButton("Delete", 130);
        deleteExpenseBtn.setDisable(true);
        deleteExpenseBtn.setOnAction(e -> {
            com.pbl3.project.pbl3_project.entity.Expense selectedExpense = table.getSelectionModel().getSelectedItem();
            if (selectedExpense == null) {
                toastService.showWarning("Select one expense to delete");
                return;
            }
            if (!showConfirmDialog("Delete Expense", "Delete expense \"" + selectedExpense.getTitle() + "\"?")) {
                return;
            }
            try {
                expenseService.deleteExpense(user, selectedExpense.getId());
                toastService.showSuccess("Expense deleted");
                loadExpensePage.run();
            } catch (Exception ex) {
                showUserFacingError(ex);
            }
        });

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            boolean hasSelection = newValue != null;
            editExpenseBtn.setDisable(!hasSelection);
            deleteExpenseBtn.setDisable(!hasSelection);
        });

        javafx.scene.layout.BorderPane expenseToolbar = new javafx.scene.layout.BorderPane();
        expenseToolbar.setLeft(expenseHeader);
        javafx.scene.layout.HBox expenseRightBox = new javafx.scene.layout.HBox(
            15,
            expenseFilterBox,
            expenseSearchControl.box(),
            newExpenseBtn,
            editExpenseBtn,
            deleteExpenseBtn
        );
        expenseRightBox.setAlignment(Pos.CENTER_RIGHT);
        expenseToolbar.setRight(expenseRightBox);
        javafx.scene.layout.BorderPane.setAlignment(expenseHeader, Pos.CENTER_LEFT);

        javafx.scene.layout.HBox expenseStatusBar = new javafx.scene.layout.HBox(
            15,
            expenseSortStatusLabel,
            expenseRowCountLabel,
            expensePageLabel,
            expensePrevBtn,
            expenseNextBtn
        );
        applyStandardTableStatusBar(expenseStatusBar);

        VBox content = new VBox();
        applyStandardTablePageLayout(content);
        content.getChildren().addAll(expenseToolbar, table, expenseStatusBar);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> updateExpenseStatusBar.run());
        loadExpensePage.run();
        updateExpenseFilterAccent.run();
        enableDeselectOnOutsideClick(content, table);
        return content;
    }

    private VBox createPromotionsView(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        final String promotionSortStateKey = "promotions";
        TableSortState promotionSortState = getOrCreateTableSortState(
            promotionSortStateKey,
            new SortCriterion("createdAt", javafx.scene.control.TableColumn.SortType.DESCENDING),
            new SortCriterion("id", javafx.scene.control.TableColumn.SortType.DESCENDING)
        );
        java.util.LinkedHashMap<String, String> promotionSortProperties = new java.util.LinkedHashMap<>();
        promotionSortProperties.put("id", "id");
        promotionSortProperties.put("name", "name");
        promotionSortProperties.put("scope", "scope");
        promotionSortProperties.put("discountValue", "discountValue");
        promotionSortProperties.put("startsAt", "startsAt");
        promotionSortProperties.put("endsAt", "endsAt");
        promotionSortProperties.put("createdAt", "createdAt");
        promotionSortProperties.put("createdBy", "createdBy.fullName");
        promotionSortProperties.put("target", "targetProduct.name");

        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.Promotion> table = new javafx.scene.control.TableView<>();
        applyStandardTableSizing(table);
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.SINGLE);

        final int promotionPageSize = 20;
        final int[] promotionCurrentPage = {0};
        final int[] promotionTotalPages = {0};
        final long[] promotionTotalElements = {0L};
        java.util.concurrent.atomic.AtomicReference<String> promotionSearchRef = new java.util.concurrent.atomic.AtomicReference<>("");
        java.util.concurrent.atomic.AtomicReference<java.time.LocalDate> promotionStartDateRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<java.time.LocalDate> promotionEndDateRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<java.util.Set<com.pbl3.project.pbl3_project.entity.PromotionScope>> promotionScopesRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<Boolean> promotionEnabledRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<java.util.Set<com.pbl3.project.pbl3_project.entity.PromotionLifecycleStatus>> promotionStatusesRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());

        Label promotionRowCountLabel = createStatusMetaLabel("Showing 0-0 of 0 Row(s)");
        Label promotionPageLabel = createStatusMetaLabel("Page 0 / 0");
        Button promotionPrevBtn = createPageNavButton("Prev");
        Button promotionNextBtn = createPageNavButton("Next");

        Runnable[] refreshPromotionTableRef = new Runnable[1];
        Runnable updatePromotionStatusBar = () -> updatePagedStatus(
            table,
            promotionRowCountLabel,
            promotionPageLabel,
            promotionPrevBtn,
            promotionNextBtn,
            promotionTotalElements[0],
            promotionCurrentPage[0],
            promotionTotalPages[0],
            promotionPageSize
        );
        Runnable loadPromotionPage = () -> {
            org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.Promotion> pageData = promotionService.searchPromotions(
                user,
                promotionSearchRef.get(),
                promotionScopesRef.get(),
                promotionEnabledRef.get(),
                promotionStatusesRef.get(),
                promotionStartDateRef.get(),
                promotionEndDateRef.get(),
                createPageable(promotionSortState, promotionSortProperties, promotionCurrentPage[0], promotionPageSize)
            );
            if (pageData.getTotalPages() > 0 && promotionCurrentPage[0] >= pageData.getTotalPages()) {
                promotionCurrentPage[0] = pageData.getTotalPages() - 1;
                pageData = promotionService.searchPromotions(
                    user,
                    promotionSearchRef.get(),
                    promotionScopesRef.get(),
                    promotionEnabledRef.get(),
                    promotionStatusesRef.get(),
                    promotionStartDateRef.get(),
                    promotionEndDateRef.get(),
                    createPageable(promotionSortState, promotionSortProperties, promotionCurrentPage[0], promotionPageSize)
                );
            }
            promotionTotalElements[0] = pageData.getTotalElements();
            promotionTotalPages[0] = pageData.getTotalPages();
            table.setItems(javafx.collections.FXCollections.observableArrayList(pageData.getContent()));
            updatePromotionStatusBar.run();
        };
        refreshPromotionTableRef[0] = loadPromotionPage;

        promotionPrevBtn.setOnAction(e -> {
            if (promotionCurrentPage[0] > 0) {
                promotionCurrentPage[0]--;
                loadPromotionPage.run();
            }
        });
        promotionNextBtn.setOnAction(e -> {
            if (promotionCurrentPage[0] + 1 < promotionTotalPages[0]) {
                promotionCurrentPage[0]++;
                loadPromotionPage.run();
            }
        });

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Promotion, Long> idCol = new javafx.scene.control.TableColumn<>("Promotion ID");
        idCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));
        idCol.setPrefWidth(115);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Promotion, String> nameCol = new javafx.scene.control.TableColumn<>("Name");
        nameCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(210);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Promotion, String> scopeCol = new javafx.scene.control.TableColumn<>("Scope");
        scopeCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(formatPromotionScopeLabel(cell.getValue().getScope())));
        scopeCol.setPrefWidth(120);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Promotion, String> targetCol = new javafx.scene.control.TableColumn<>("Target");
        targetCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(formatPromotionTargetLabel(cell.getValue())));
        targetCol.setPrefWidth(200);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Promotion, String> discountCol = new javafx.scene.control.TableColumn<>("Discount");
        discountCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(formatPromotionDiscountLabel(cell.getValue())));
        discountCol.setPrefWidth(130);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Promotion, String> scheduleCol = new javafx.scene.control.TableColumn<>("Schedule");
        scheduleCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(formatPromotionScheduleLabel(cell.getValue())));
        scheduleCol.setPrefWidth(240);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Promotion, String> statusCol = new javafx.scene.control.TableColumn<>("Status");
        statusCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
            formatPromotionLifecycleStatusLabel(cell.getValue().getLifecycleStatus())
        ));
        statusCol.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                String textColor = switch (item) {
                    case "Active" -> "-app-success-hover";
                    case "Scheduled" -> "-app-primary-hover";
                    case "Expired" -> "#fe9900";
                    default -> "-app-danger-hover";
                };
                setStyle("-fx-text-fill: " + textColor + "; -fx-font-weight: bold; -fx-alignment: CENTER;");
            }
        });
        statusCol.setPrefWidth(130);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Promotion, String> createdByCol = new javafx.scene.control.TableColumn<>("Created By");
        createdByCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getCreatedByDisplayName()));
        createdByCol.setPrefWidth(170);

        table.getColumns().addAll(idCol, nameCol, scopeCol, targetCol, discountCol, scheduleCol, statusCol, createdByCol);

        java.util.LinkedHashMap<String, javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Promotion, ?>> promotionSortColumns =
            new java.util.LinkedHashMap<>();
        promotionSortColumns.put("id", idCol);
        promotionSortColumns.put("name", nameCol);
        promotionSortColumns.put("scope", scopeCol);
        promotionSortColumns.put("target", targetCol);
        promotionSortColumns.put("discountValue", discountCol);
        promotionSortColumns.put("startsAt", scheduleCol);
        promotionSortColumns.put("createdBy", createdByCol);
        installSortHeaderIndicators(promotionSortColumns);

        java.util.LinkedHashMap<String, String> promotionSortLabels = new java.util.LinkedHashMap<>();
        promotionSortLabels.put("id", "Promotion ID");
        promotionSortLabels.put("name", "Name");
        promotionSortLabels.put("scope", "Scope");
        promotionSortLabels.put("target", "Target");
        promotionSortLabels.put("discountValue", "Discount");
        promotionSortLabels.put("startsAt", "Schedule");
        promotionSortLabels.put("createdBy", "Created By");
        Label promotionSortStatusLabel = createSortStatusLabel(promotionSortState, promotionSortLabels);
        Runnable applyPromotionSortUi = () -> {
            applySortStateToTable(table, promotionSortColumns, promotionSortState);
            promotionSortStatusLabel.setText(buildSortStatusText(promotionSortState, promotionSortLabels));
        };
        applyPromotionSortUi.run();
        installManualServerSorting(
            table,
            promotionSortColumns,
            promotionSortState,
            () -> {
                applyPromotionSortUi.run();
                promotionCurrentPage[0] = 0;
                loadPromotionPage.run();
            }
        );

        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<com.pbl3.project.pbl3_project.entity.Promotion> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showPromotionDialog(stage, user, row.getItem(), refreshPromotionTableRef[0]);
                }
            });
            return row;
        });

        ExpandableSearchControl promotionSearchControl = createExpandableSearchControl(260);
        javafx.animation.PauseTransition promotionSearchPause = new javafx.animation.PauseTransition(Duration.millis(220));
        promotionSearchPause.setOnFinished(e -> {
            promotionCurrentPage[0] = 0;
            promotionSearchRef.set(promotionSearchControl.field().getText());
            loadPromotionPage.run();
        });
        promotionSearchControl.field().textProperty().addListener((obs, oldV, newV) -> promotionSearchPause.playFromStart());

        javafx.scene.layout.HBox promotionFilterBox = new javafx.scene.layout.HBox();
        promotionFilterBox.setAlignment(Pos.CENTER);
        promotionFilterBox.getStyleClass().add("expandable-search-box");
        promotionFilterBox.setPrefSize(40, 40);
        promotionFilterBox.setMinSize(40, 40);
        promotionFilterBox.setMaxSize(40, 40);
        promotionFilterBox.setCursor(javafx.scene.Cursor.HAND);
        javafx.scene.shape.SVGPath promotionFilterIcon = new javafx.scene.shape.SVGPath();
        promotionFilterIcon.setContent("M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z");
        promotionFilterIcon.setFill(PRIMARY_COLOR);
        promotionFilterBox.getChildren().add(promotionFilterIcon);

        Runnable updatePromotionFilterAccent = () -> {
            boolean hasFilter = promotionStartDateRef.get() != null
                || promotionEndDateRef.get() != null
                || !promotionScopesRef.get().isEmpty()
                || promotionEnabledRef.get() != null
                || !promotionStatusesRef.get().isEmpty();
            promotionFilterBox.setStyle(hasFilter ? "-fx-border-color: -app-primary;" : "");
        };

        javafx.stage.Popup promotionFilterPopup = new javafx.stage.Popup();
        promotionFilterPopup.setAutoHide(true);
        promotionFilterBox.setOnMouseClicked(fev -> {
            fev.consume();
            try {
                if (promotionFilterPopup.isShowing()) {
                    promotionFilterPopup.hide();
                    return;
                }

                FilterPopupShell shell = createFilterPopupShell(360, 340);
                VBox scrollContent = shell.content();

                Label dateTitle = createFilterPopupSectionTitle("Date Range");
                javafx.scene.control.DatePicker startDatePicker = new javafx.scene.control.DatePicker(promotionStartDateRef.get());
                startDatePicker.setPromptText("Start Date");
                startDatePicker.setPrefWidth(140);
                javafx.scene.control.DatePicker endDatePicker = new javafx.scene.control.DatePicker(promotionEndDateRef.get());
                endDatePicker.setPromptText("End Date");
                endDatePicker.setPrefWidth(140);
                customizeDatePicker(startDatePicker);
                customizeDatePicker(endDatePicker);
                javafx.scene.layout.HBox dateBox = new javafx.scene.layout.HBox(5, startDatePicker, new Label("-"), endDatePicker);
                dateBox.setAlignment(Pos.CENTER_LEFT);

                javafx.scene.control.Separator sepDate = new javafx.scene.control.Separator();

                Label scopeTitle = createFilterPopupSectionTitle("Scope");
                javafx.scene.control.CheckBox allScopesCb = new javafx.scene.control.CheckBox("All Scopes");
                allScopesCb.setSelected(promotionScopesRef.get().isEmpty());
                allScopesCb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                VBox scopeScroll = new VBox(8);
                scopeScroll.setPadding(new Insets(5, 5, 5, 20));
                java.util.List<javafx.scene.control.CheckBox> scopeCbs = new java.util.ArrayList<>();
                java.util.Set<com.pbl3.project.pbl3_project.entity.PromotionScope> activeScopes = promotionScopesRef.get();
                for (com.pbl3.project.pbl3_project.entity.PromotionScope scope : com.pbl3.project.pbl3_project.entity.PromotionScope.values()) {
                    javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(formatPromotionScopeLabel(scope));
                    cb.setUserData(scope);
                    cb.setSelected(activeScopes.isEmpty() || activeScopes.contains(scope));
                    cb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                    cb.setOnAction(e -> {
                        if (!cb.isSelected()) {
                            allScopesCb.setSelected(false);
                        } else {
                            boolean all = true;
                            for (javafx.scene.control.CheckBox child : scopeCbs) {
                                if (!child.isSelected()) {
                                    all = false;
                                    break;
                                }
                            }
                            allScopesCb.setSelected(all);
                        }
                    });
                    scopeCbs.add(cb);
                    scopeScroll.getChildren().add(cb);
                }
                allScopesCb.setOnAction(e -> {
                    boolean selected = allScopesCb.isSelected();
                    for (javafx.scene.control.CheckBox cb : scopeCbs) {
                        cb.setSelected(selected);
                    }
                });
                FilterDisclosureSection scopeSection = new FilterDisclosureSection(allScopesCb, scopeScroll);

                javafx.scene.control.Separator sepScope = new javafx.scene.control.Separator();

                Label enabledTitle = createFilterPopupSectionTitle("Enabled State");
                javafx.scene.control.ComboBox<String> enabledCombo = new javafx.scene.control.ComboBox<>();
                enabledCombo.getItems().addAll("All", "Enabled", "Disabled");
                enabledCombo.setValue(
                    promotionEnabledRef.get() == null
                        ? "All"
                        : (promotionEnabledRef.get() ? "Enabled" : "Disabled")
                );
                enabledCombo.setPrefWidth(180);

                javafx.scene.control.Separator sepEnabled = new javafx.scene.control.Separator();

                Label statusTitle = createFilterPopupSectionTitle("Current Status");
                javafx.scene.control.CheckBox allStatusesCb = new javafx.scene.control.CheckBox("All Statuses");
                allStatusesCb.setSelected(promotionStatusesRef.get().isEmpty());
                allStatusesCb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                VBox statusScroll = new VBox(8);
                statusScroll.setPadding(new Insets(5, 5, 5, 20));
                java.util.List<javafx.scene.control.CheckBox> statusCbs = new java.util.ArrayList<>();
                java.util.Set<com.pbl3.project.pbl3_project.entity.PromotionLifecycleStatus> activeStatuses = promotionStatusesRef.get();
                for (com.pbl3.project.pbl3_project.entity.PromotionLifecycleStatus status : com.pbl3.project.pbl3_project.entity.PromotionLifecycleStatus.values()) {
                    javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(formatPromotionLifecycleStatusLabel(status));
                    cb.setUserData(status);
                    cb.setSelected(activeStatuses.isEmpty() || activeStatuses.contains(status));
                    cb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                    cb.setOnAction(e -> {
                        if (!cb.isSelected()) {
                            allStatusesCb.setSelected(false);
                        } else {
                            boolean all = true;
                            for (javafx.scene.control.CheckBox child : statusCbs) {
                                if (!child.isSelected()) {
                                    all = false;
                                    break;
                                }
                            }
                            allStatusesCb.setSelected(all);
                        }
                    });
                    statusCbs.add(cb);
                    statusScroll.getChildren().add(cb);
                }
                allStatusesCb.setOnAction(e -> {
                    boolean selected = allStatusesCb.isSelected();
                    for (javafx.scene.control.CheckBox cb : statusCbs) {
                        cb.setSelected(selected);
                    }
                });
                FilterDisclosureSection statusSection = new FilterDisclosureSection(allStatusesCb, statusScroll);

                scrollContent.getChildren().addAll(
                    dateTitle, dateBox, sepDate,
                    scopeTitle, scopeSection.getNode(), sepScope,
                    enabledTitle, enabledCombo, sepEnabled,
                    statusTitle, statusSection.getNode()
                );

                Button resetBtn = new Button("Reset");
                resetBtn.getStyleClass().add("filter-reset-btn");
                resetBtn.setOnAction(ae -> {
                    promotionStartDateRef.set(null);
                    promotionEndDateRef.set(null);
                    promotionScopesRef.set(new java.util.LinkedHashSet<>());
                    promotionEnabledRef.set(null);
                    promotionStatusesRef.set(new java.util.LinkedHashSet<>());
                    promotionCurrentPage[0] = 0;
                    updatePromotionFilterAccent.run();
                    loadPromotionPage.run();
                    promotionFilterPopup.hide();
                });

                Button applyBtn = new Button("Apply");
                applyBtn.getStyleClass().add("filter-apply-btn");
                applyBtn.setOnAction(ae -> {
                    java.util.Set<com.pbl3.project.pbl3_project.entity.PromotionScope> selectedScopes = new java.util.LinkedHashSet<>();
                    for (javafx.scene.control.CheckBox cb : scopeCbs) {
                        if (cb.isSelected()) {
                            selectedScopes.add((com.pbl3.project.pbl3_project.entity.PromotionScope) cb.getUserData());
                        }
                    }
                    java.util.Set<com.pbl3.project.pbl3_project.entity.PromotionLifecycleStatus> selectedStatuses = new java.util.LinkedHashSet<>();
                    for (javafx.scene.control.CheckBox cb : statusCbs) {
                        if (cb.isSelected()) {
                            selectedStatuses.add((com.pbl3.project.pbl3_project.entity.PromotionLifecycleStatus) cb.getUserData());
                        }
                    }
                    promotionStartDateRef.set(startDatePicker.getValue());
                    promotionEndDateRef.set(endDatePicker.getValue());
                    promotionScopesRef.set(allScopesCb.isSelected() ? new java.util.LinkedHashSet<>() : selectedScopes);
                    promotionStatusesRef.set(allStatusesCb.isSelected() ? new java.util.LinkedHashSet<>() : selectedStatuses);
                    promotionEnabledRef.set(switch (enabledCombo.getValue()) {
                        case "Enabled" -> Boolean.TRUE;
                        case "Disabled" -> Boolean.FALSE;
                        default -> null;
                    });
                    promotionCurrentPage[0] = 0;
                    updatePromotionFilterAccent.run();
                    loadPromotionPage.run();
                    promotionFilterPopup.hide();
                });

                shell.container().getChildren().add(createFilterPopupActionRow(resetBtn, applyBtn));
                promotionFilterPopup.getContent().clear();
                promotionFilterPopup.getContent().add(shell.container());
                showPopupBelow(promotionFilterPopup, promotionFilterBox, -300, 5);
            } catch (Exception ex) {
                showUserFacingError(ex);
            }
        });

        Label promotionHeader = new Label("Promotions");
        promotionHeader.getStyleClass().add("header-label");

        Button newPromotionBtn = createExpandableGreenActionButton("New Promotion", 195);
        newPromotionBtn.setOnAction(e -> showPromotionDialog(stage, user, null, refreshPromotionTableRef[0]));

        Button editPromotionBtn = createExpandableManageActionButton("Edit", 110);
        editPromotionBtn.setDisable(true);
        editPromotionBtn.setOnAction(e -> {
            com.pbl3.project.pbl3_project.entity.Promotion selectedPromotion = table.getSelectionModel().getSelectedItem();
            if (selectedPromotion == null) {
                toastService.showWarning("Select one promotion to edit");
                return;
            }
            showPromotionDialog(stage, user, selectedPromotion, refreshPromotionTableRef[0]);
        });

        Button togglePromotionBtn = createExpandableManageActionButton("Enable / Disable", 190);
        togglePromotionBtn.setDisable(true);
        togglePromotionBtn.setOnAction(e -> {
            com.pbl3.project.pbl3_project.entity.Promotion selectedPromotion = table.getSelectionModel().getSelectedItem();
            if (selectedPromotion == null) {
                toastService.showWarning("Select one promotion first");
                return;
            }
            try {
                promotionService.setPromotionEnabled(user, selectedPromotion.getId(), !selectedPromotion.isEnabled());
                toastService.showSuccess(selectedPromotion.isEnabled() ? "Promotion disabled" : "Promotion enabled");
                loadPromotionPage.run();
            } catch (Exception ex) {
                showUserFacingError(ex);
            }
        });

        Button deletePromotionBtn = createExpandableManageActionButton("Delete", 130);
        deletePromotionBtn.setDisable(true);
        deletePromotionBtn.setOnAction(e -> {
            com.pbl3.project.pbl3_project.entity.Promotion selectedPromotion = table.getSelectionModel().getSelectedItem();
            if (selectedPromotion == null) {
                toastService.showWarning("Select one promotion to delete");
                return;
            }
            if (!showConfirmDialog("Delete Promotion", "Delete promotion \"" + selectedPromotion.getName() + "\"?")) {
                return;
            }
            try {
                promotionService.deletePromotion(user, selectedPromotion.getId());
                toastService.showSuccess("Promotion deleted");
                loadPromotionPage.run();
            } catch (Exception ex) {
                showUserFacingError(ex);
            }
        });

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            boolean hasSelection = newValue != null;
            editPromotionBtn.setDisable(!hasSelection);
            togglePromotionBtn.setDisable(!hasSelection);
            deletePromotionBtn.setDisable(!hasSelection);
        });

        javafx.scene.layout.BorderPane promotionToolbar = new javafx.scene.layout.BorderPane();
        promotionToolbar.setLeft(promotionHeader);
        javafx.scene.layout.HBox promotionRightBox = new javafx.scene.layout.HBox(
            15,
            promotionFilterBox,
            promotionSearchControl.box(),
            newPromotionBtn,
            editPromotionBtn,
            togglePromotionBtn,
            deletePromotionBtn
        );
        promotionRightBox.setAlignment(Pos.CENTER_RIGHT);
        promotionToolbar.setRight(promotionRightBox);
        javafx.scene.layout.BorderPane.setAlignment(promotionHeader, Pos.CENTER_LEFT);

        javafx.scene.layout.HBox promotionStatusBar = new javafx.scene.layout.HBox(
            15,
            promotionSortStatusLabel,
            promotionRowCountLabel,
            promotionPageLabel,
            promotionPrevBtn,
            promotionNextBtn
        );
        applyStandardTableStatusBar(promotionStatusBar);

        VBox content = new VBox();
        applyStandardTablePageLayout(content);
        content.getChildren().addAll(promotionToolbar, table, promotionStatusBar);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> updatePromotionStatusBar.run());
        loadPromotionPage.run();
        updatePromotionFilterAccent.run();
        enableDeselectOnOutsideClick(content, table);
        return content;
    }
    
    private javafx.scene.Node createSalesView(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        javafx.scene.control.SplitPane splitPane = new javafx.scene.control.SplitPane();
        splitPane.setDividerPositions(0.65);

        // === LEFT SIDE: Switchable Views ===
        javafx.scene.layout.StackPane leftPane = new javafx.scene.layout.StackPane();
        
        // --- 1. Category Grid View ---
        VBox categoryView = new VBox(20);
        categoryView.setPadding(new Insets(20));
        categoryView.setAlignment(Pos.TOP_CENTER);
        
        Label catTitle = new Label("Select Category");
        catTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: -app-primary;");
        
        javafx.scene.layout.FlowPane categoryGrid = new javafx.scene.layout.FlowPane();
        categoryGrid.setHgap(20);
        categoryGrid.setVgap(20);
        categoryGrid.setAlignment(Pos.CENTER);
        
        categoryView.getChildren().addAll(catTitle, categoryGrid);
        
        // --- 2. Product List View ---
        VBox productView = new VBox(10);
        productView.setPadding(new Insets(10));
        productView.setVisible(false);
        
        javafx.scene.layout.HBox productHeader = new javafx.scene.layout.HBox(10);
        productHeader.setAlignment(Pos.CENTER_LEFT);
        
        Button backBtn = new Button();
        backBtn.setStyle("-fx-background-color: -app-danger; -fx-background-radius: 20; -fx-border-width: 0; -fx-padding: 0;");
        backBtn.setPrefSize(40, 40);
        backBtn.setMinSize(40, 40);
        backBtn.setMaxSize(40, 40);
        backBtn.setCursor(javafx.scene.Cursor.HAND);
        
        javafx.scene.shape.SVGPath arrowLeft = new javafx.scene.shape.SVGPath();
        arrowLeft.setContent("M19 12H5M12 19l-7-7 7-7");
        arrowLeft.setStroke(SURFACE_COLOR);
        arrowLeft.setStrokeWidth(2.5);
        arrowLeft.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        arrowLeft.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        arrowLeft.setFill(javafx.scene.paint.Color.TRANSPARENT);
        
        javafx.scene.layout.StackPane arrowWrapper = new javafx.scene.layout.StackPane(arrowLeft);
        arrowWrapper.setPrefSize(40, 40);
        arrowWrapper.setMinSize(40, 40);
        arrowWrapper.setMaxSize(40, 40);
        
        Label backLabelText = new Label("Back");
        backLabelText.setStyle("-fx-font-family: 'Be Vietnam Pro', 'Inter', sans-serif; -fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: -app-surface; -fx-padding: 0;");
        backLabelText.setOpacity(0);
        backLabelText.setScaleX(0.8);
        backLabelText.setScaleY(0.8);
        backLabelText.setTranslateX(15);
        
        javafx.scene.effect.GaussianBlur textBlur = new javafx.scene.effect.GaussianBlur(4.0);
        backLabelText.setEffect(textBlur);
        
        javafx.scene.layout.StackPane backBtnContent = new javafx.scene.layout.StackPane(arrowWrapper, backLabelText);
        javafx.scene.layout.StackPane.setAlignment(arrowWrapper, Pos.CENTER_LEFT);
        javafx.scene.layout.StackPane.setAlignment(backLabelText, Pos.CENTER_LEFT);
        javafx.scene.layout.StackPane.setMargin(backLabelText, new Insets(0, 0, 0, 36));
        
        javafx.scene.shape.Rectangle backClip = new javafx.scene.shape.Rectangle();
        backClip.setArcWidth(40);
        backClip.setArcHeight(40);
        backClip.widthProperty().bind(backBtn.widthProperty());
        backClip.heightProperty().bind(backBtn.heightProperty());
        backBtnContent.setClip(backClip);
        
        backBtn.setGraphic(backBtnContent);
        backBtn.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        backBtn.setAlignment(Pos.CENTER_LEFT);
        
        javafx.animation.Timeline hoverInBackBtn = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.millis(250),
                new javafx.animation.KeyValue(backBtn.minWidthProperty(), 100, SPRING_BOUNCE),
                new javafx.animation.KeyValue(backBtn.prefWidthProperty(), 100, SPRING_BOUNCE),
                new javafx.animation.KeyValue(backBtn.maxWidthProperty(), 100, SPRING_BOUNCE),
                new javafx.animation.KeyValue(arrowWrapper.translateXProperty(), 10, SPRING_BOUNCE),
                new javafx.animation.KeyValue(arrowWrapper.scaleXProperty(), 1.05, SPRING_BOUNCE),
                new javafx.animation.KeyValue(arrowWrapper.scaleYProperty(), 1.05, SPRING_BOUNCE),
                new javafx.animation.KeyValue(backLabelText.opacityProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(backLabelText.translateXProperty(), 10, SPRING_BOUNCE),
                new javafx.animation.KeyValue(backLabelText.scaleXProperty(), 1.0, SPRING_BOUNCE),
                new javafx.animation.KeyValue(backLabelText.scaleYProperty(), 1.0, SPRING_BOUNCE),
                new javafx.animation.KeyValue(textBlur.radiusProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH)
            )
        );
        
        javafx.animation.Timeline hoverOutBackBtn = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.millis(250),
                new javafx.animation.KeyValue(backBtn.minWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(backBtn.prefWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(backBtn.maxWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(arrowWrapper.translateXProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(arrowWrapper.scaleXProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(arrowWrapper.scaleYProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(backLabelText.opacityProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(backLabelText.translateXProperty(), 15, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(backLabelText.scaleXProperty(), 0.8, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(backLabelText.scaleYProperty(), 0.8, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(textBlur.radiusProperty(), 4.0, javafx.animation.Interpolator.EASE_BOTH)
            )
        );
        
        backBtn.setOnMouseEntered(ev -> {
            backBtn.setStyle("-fx-background-color: -app-danger-hover; -fx-background-radius: 20; -fx-border-width: 0; -fx-effect: dropshadow(three-pass-box, -app-shadow, 15, 0, 0, 6); -fx-padding: 0;");
            hoverOutBackBtn.stop();
            hoverInBackBtn.play();
        });
        
        backBtn.setOnMouseExited(ev -> {
            backBtn.setStyle("-fx-background-color: -app-danger; -fx-background-radius: 20; -fx-border-width: 0; -fx-padding: 0;");
            hoverInBackBtn.stop();
            hoverOutBackBtn.play();
        });
        
        backBtn.setOnMousePressed(ev -> {
            backBtn.setScaleX(0.95);
            backBtn.setScaleY(0.95);
        });
        
        backBtn.setOnMouseReleased(ev -> {
            backBtn.setScaleX(1.0);
            backBtn.setScaleY(1.0);
        });
        
        Label productTitle = new Label("Products");
        productTitle.getStyleClass().add("header-label");
        
        productHeader.getChildren().addAll(backBtn, productTitle);
        
        TextField searchField = new TextField();
        searchField.setPromptText(DEFAULT_SEARCH_PROMPT);
        searchField.setStyle("-fx-background-radius: 20; -fx-padding: 8 15;");
        
        javafx.scene.layout.FlowPane productGrid = new javafx.scene.layout.FlowPane();
        productGrid.setHgap(15);
        productGrid.setVgap(15);
        productGrid.setPadding(new Insets(10));
        productGrid.setAlignment(Pos.TOP_LEFT);
        
        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(productGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        
        productView.getChildren().addAll(productHeader, searchField, scrollPane);
        VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);
        
        leftPane.getChildren().addAll(categoryView, productView);
        
        // === RIGHT SIDE: Cart ===
        VBox rightBox = new VBox(10);
        rightBox.setPadding(new Insets(10));
        rightBox.setStyle("-fx-background-color: -app-surface-muted;");
        Label rightTitle = new Label("Shopping Cart");
        rightTitle.getStyleClass().add("header-label");
        java.util.concurrent.atomic.AtomicReference<com.pbl3.project.pbl3_project.entity.Customer> selectedCustomerRef =
            new java.util.concurrent.atomic.AtomicReference<>(null);

        Label customerSectionLabel = new Label("Customer");
        customerSectionLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: -app-text-muted;");
        Label selectedCustomerNameLabel = new Label("Guest");
        selectedCustomerNameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: -app-text-primary;");
        Label selectedCustomerPhoneLabel = new Label("No customer selected");
        selectedCustomerPhoneLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");

        Button selectCustomerButton = createExpandableManageActionButton("Select Customer", 175);
        Button clearCustomerButton = createExpandableManageActionButton("Clear", 100);
        clearCustomerButton.setDisable(true);

        selectCustomerButton.setOnAction(e -> {
            try {
                com.pbl3.project.pbl3_project.entity.Customer picked = showCustomerPickerDialog(stage, user);
                if (picked != null) {
                    selectedCustomerRef.set(picked);
                    updatePosCustomerCard(selectedCustomerNameLabel, selectedCustomerPhoneLabel, clearCustomerButton, picked);
                }
            } catch (Exception ex) {
                showUserFacingError(ex);
            }
        });
        clearCustomerButton.setOnAction(e -> {
            selectedCustomerRef.set(null);
            updatePosCustomerCard(selectedCustomerNameLabel, selectedCustomerPhoneLabel, clearCustomerButton, null);
        });

        javafx.scene.layout.HBox customerActionRow = new javafx.scene.layout.HBox(10, selectCustomerButton, clearCustomerButton);
        customerActionRow.setAlignment(Pos.CENTER_LEFT);
        VBox customerCard = new VBox(8, customerSectionLabel, selectedCustomerNameLabel, selectedCustomerPhoneLabel, customerActionRow);
        customerCard.setPadding(new Insets(14));
        customerCard.setStyle("-fx-background-color: -app-surface; -fx-background-radius: 18; -fx-border-color: -app-border; -fx-border-radius: 18;");

        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.dto.CreateOrderRequest.OrderItemRequest> cartTable = new javafx.scene.control.TableView<>();
        prepareNonReorderableTable(cartTable);
        cartTable.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.CreateOrderRequest.OrderItemRequest, Long> cIdCol = new javafx.scene.control.TableColumn<>("ID");
        cIdCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("productId"));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.CreateOrderRequest.OrderItemRequest, Integer> cQtyCol = new javafx.scene.control.TableColumn<>("Qty");
        cQtyCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("quantity"));
        cartTable.getColumns().addAll(cIdCol, cQtyCol);

        javafx.collections.ObservableList<com.pbl3.project.pbl3_project.dto.CreateOrderRequest.OrderItemRequest> cartItems = javafx.collections.FXCollections.observableArrayList();
        cartTable.setItems(cartItems);

        // === Data & Logic ===
        java.util.List<com.pbl3.project.pbl3_project.entity.Product> allProducts = productService.getAllProducts();
        final java.util.Map<Long, PromotionService.ProductPricingPreview>[] pricingByProductIdRef =
            new java.util.Map[]{promotionService.previewBestProductPricing(allProducts, java.time.LocalDateTime.now())};
        final com.pbl3.project.pbl3_project.entity.Category[] selectedCategory = {null};
        
        Runnable renderProducts = () -> {
            productGrid.getChildren().clear();
            String query = searchField.getText().toLowerCase();
            
            for (com.pbl3.project.pbl3_project.entity.Product p : allProducts) {
                boolean matchName = p.getName().toLowerCase().contains(query);
                boolean matchCat = selectedCategory[0] != null && p.getCategory() != null && p.getCategory().getId().equals(selectedCategory[0].getId());
                
                if (matchName && matchCat) {
                    PromotionService.ProductPricingPreview pricingPreview = pricingByProductIdRef[0].getOrDefault(
                        p.getId(),
                        new PromotionService.ProductPricingPreview(
                            p,
                            null,
                            MoneySupport.normalize(p.getPrice()),
                            MoneySupport.normalize(p.getPrice()),
                            MoneySupport.ZERO
                        )
                    );
                    VBox card = new VBox(5);
                    card.getStyleClass().add("product-card");
                    card.setPrefSize(140, 180);
                    card.setAlignment(Pos.TOP_CENTER);
                    
                    if (p.getQuantity() <= 0) card.getStyleClass().add("product-card-unavailable");
                    
                    javafx.scene.layout.StackPane imgPlaceholder = new javafx.scene.layout.StackPane();
                    imgPlaceholder.getStyleClass().add("card-image-placeholder");
                    imgPlaceholder.setPrefSize(140, 100);
                    Label initial = new Label(p.getName().substring(0, 1).toUpperCase());
                    initial.setStyle("-fx-font-size: 30px; -fx-text-fill: -app-primary; -fx-font-weight: bold;");
                    imgPlaceholder.getChildren().add(initial);
                    
                    VBox info = new VBox(3);
                    info.setPadding(new Insets(10));
                    info.setAlignment(Pos.CENTER);
                    
                    Label nameLbl = new Label(p.getName());
                    nameLbl.getStyleClass().add("card-name");
                    nameLbl.setMaxWidth(130);
                    
                    Label priceLbl = new Label(formatVnd(pricingPreview.discountedUnitPrice()));
                    priceLbl.getStyleClass().add("card-price");

                    java.util.List<javafx.scene.Node> infoChildren = new java.util.ArrayList<>();
                    infoChildren.add(nameLbl);
                    infoChildren.add(priceLbl);
                    if (pricingPreview.hasPromotion()) {
                        Label originalPriceLbl = new Label(formatVnd(pricingPreview.originalUnitPrice()));
                        originalPriceLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -app-text-muted; -fx-strikethrough: true;");
                        Label promoBadge = new Label(pricingPreview.promotion().getName());
                        promoBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: -app-primary; -fx-background-color: -app-primary-soft; -fx-background-radius: 999; -fx-padding: 3 8;");
                        infoChildren.add(new VBox(2, originalPriceLbl, promoBadge));
                    }
                    
                    Label stockLbl;
                    if (p.getQuantity() > 0) {
                        stockLbl = new Label("Stock: " + p.getQuantity());
                        stockLbl.getStyleClass().add("card-stock");
                    } else {
                        stockLbl = new Label("OUT OF STOCK");
                        stockLbl.getStyleClass().add("card-out-stock");
                    }
                    
                    infoChildren.add(stockLbl);
                    info.getChildren().addAll(infoChildren);
                    card.getChildren().addAll(imgPlaceholder, info);
                    
                    card.setOnMouseClicked(e -> {
                        if (p.getQuantity() > 0) {
                            var existing = cartItems.stream().filter(i -> i.getProductId().equals(p.getId())).findFirst();
                            if (existing.isPresent()) {
                                existing.get().setQuantity(existing.get().getQuantity() + 1);
                                cartTable.refresh();
                            } else {
                                var item = new com.pbl3.project.pbl3_project.dto.CreateOrderRequest.OrderItemRequest();
                                item.setProductId(p.getId());
                                item.setQuantity(1);
                                cartItems.add(item);
                            }
                            javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(100), card);
                            st.setFromX(1.0); st.setFromY(1.0);
                            st.setToX(0.95); st.setToY(0.95);
                            st.setAutoReverse(true);
                            st.setCycleCount(2);
                            st.play();
                        } else {
                            toastService.showWarning("Product out of stock!");
                        }
                    });
                    
                    productGrid.getChildren().add(card);
                }
            }
        };
        
        // Load Categories into Grid
        java.util.List<com.pbl3.project.pbl3_project.entity.Category> categories = categoryRepository.findAll();
        for (com.pbl3.project.pbl3_project.entity.Category cat : categories) {
            long count = allProducts.stream().filter(p -> p.getCategory() != null && p.getCategory().getId().equals(cat.getId())).count();
            
            VBox catCard = new VBox(10);
            catCard.getStyleClass().add("category-card");
            catCard.setPrefSize(160, 120);
            catCard.setAlignment(Pos.CENTER);
            
            Label catName = new Label(cat.getName());
            catName.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -app-primary;");
            
            Label catCount = new Label(count + " products");
            catCount.setStyle("-fx-font-size: 12px; -fx-text-fill: -app-text-muted;");
            
            catCard.getChildren().addAll(catName, catCount);
            
            catCard.setOnMouseClicked(e -> {
                selectedCategory[0] = cat;
                productTitle.setText(cat.getName());
                categoryView.setVisible(false);
                productView.setVisible(true);
                renderProducts.run();
            });
            
            categoryGrid.getChildren().add(catCard);
        }
        
        backBtn.setOnAction(e -> {
            hoverInBackBtn.stop();
            hoverOutBackBtn.stop();
            backBtn.setMinWidth(40);
            backBtn.setPrefWidth(40);
            backBtn.setMaxWidth(40);
            arrowWrapper.setTranslateX(0);
            backLabelText.setOpacity(0);
            
            productView.setVisible(false);
            categoryView.setVisible(true);
            selectedCategory[0] = null;
        });
        
        searchField.textProperty().addListener((obs, old, val) -> renderProducts.run());

        Button checkoutButton = new Button("CHECKOUT");
        checkoutButton.getStyleClass().addAll("button", "success-button");
        checkoutButton.setMaxWidth(Double.MAX_VALUE);
        checkoutButton.setOnAction(e -> {
            if (cartItems.isEmpty()) return;
            
            BigDecimal subtotal = calculatePosCartSubtotal(cartItems, allProducts, pricingByProductIdRef[0]);
            java.util.List<PromotionService.OrderPromotionPreview> orderPromotions =
                promotionService.getEligibleOrderPromotionPreviews(subtotal, java.time.LocalDateTime.now());

            showCheckoutDialog(stage, subtotal, orderPromotions, selection -> {
                try {
                    com.pbl3.project.pbl3_project.dto.CreateOrderRequest req = new com.pbl3.project.pbl3_project.dto.CreateOrderRequest();
                    req.setUserId(user.getId());
                    req.setCustomerId(selectedCustomerRef.get() != null ? selectedCustomerRef.get().getId() : null);
                    req.setItems(new java.util.ArrayList<>(cartItems));
                    req.setPaymentMethod(selection.paymentMethod());
                    req.setSelectedOrderPromotionId(selection.selectedOrderPromotionId());
                    com.pbl3.project.pbl3_project.entity.Order newOrder = orderService.createOrder(req);
                    
                    if (selection.printReceipt()) {
                        receiptService.generateAndOpenReceipt(newOrder);
                    }
                    
                    toastService.showSuccess("Order Paid via " + selection.paymentMethod() + "!");
                    cartItems.clear();
                    selectedCustomerRef.set(null);
                    updatePosCustomerCard(selectedCustomerNameLabel, selectedCustomerPhoneLabel, clearCustomerButton, null);
                    allProducts.clear();
                    allProducts.addAll(productService.getAllProducts());
                    pricingByProductIdRef[0] = promotionService.previewBestProductPricing(allProducts, java.time.LocalDateTime.now());
                    renderProducts.run();
                } catch (Exception ex) {
                    toastService.showError("Order Failed: " + ex.getMessage());
                }
            });
        });

        rightBox.getChildren().addAll(rightTitle, customerCard, cartTable, checkoutButton);
        VBox.setVgrow(cartTable, javafx.scene.layout.Priority.ALWAYS);
        splitPane.getItems().addAll(leftPane, rightBox);
        return splitPane;
    }
    

    // --- Core Navigation & Animation ---
    
    private void switchScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user, String title, String navId, javafx.scene.Node content) {
        switchScene(stage, user, title, navId, content, false);
    }

    private void switchScene(
        Stage stage,
        com.pbl3.project.pbl3_project.entity.User user,
        String title,
        String navId,
        javafx.scene.Node content,
        boolean expandContainingSidebarGroup
    ) {
        javafx.scene.Scene scene = stage.getScene();
        javafx.scene.layout.BorderPane root = resolveMainLayout(scene);
        boolean reducedMotion = isReducedMotionEnabledForUser(user);
        
        if (root != null) {
            root.setCenter(content);
            javafx.scene.layout.BorderPane.setMargin(content, new Insets(15));
            Label pageTitle = (Label) root.lookup("#header-title");
            if (pageTitle != null) {
                pageTitle.setText(title);
            }
            applyCurrentUserUiPreferences(stage, user, false);
            updateSidebarState(root, navId, expandContainingSidebarGroup);
            if (!reducedMotion) {
                javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(150), content);
                tt.setFromY(30);
                tt.setToY(0);
                tt.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

                javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(150), content);
                ft.setFromValue(0.0);
                ft.setToValue(1.0);

                new javafx.animation.ParallelTransition(tt, ft).play();
            } else {
                content.setTranslateY(0.0);
                content.setOpacity(1.0);
            }
        } else {
            javafx.scene.layout.BorderPane layout = createMainLayout(stage, user, title, content, navId);
            layout.setUserData("MAIN_LAYOUT");
            Scene newScene = new Scene(layout, MAIN_WINDOW_DEFAULT_WIDTH, MAIN_WINDOW_DEFAULT_HEIGHT);
            applyApplicationStyles(newScene);
            stage.setScene(newScene);
            stage.setWidth(MAIN_WINDOW_DEFAULT_WIDTH);
            stage.setHeight(MAIN_WINDOW_DEFAULT_HEIGHT);
            toastService.setScene(newScene); // Init toast container
            stage.centerOnScreen();
            if (!reducedMotion) {
                javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(150), layout);
                tt.setFromY(50);
                tt.setToY(0);
                tt.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

                javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(150), layout);
                ft.setFromValue(0.0);
                ft.setToValue(1.0);

                new javafx.animation.ParallelTransition(tt, ft).play();
            } else {
                layout.setTranslateY(0.0);
                layout.setOpacity(1.0);
            }
        }
    }

    private void updateSidebarState(javafx.scene.Parent root, String activeNavId) {
        updateSidebarState(root, activeNavId, false);
    }

    private void updateSidebarState(javafx.scene.Parent root, String activeNavId, boolean expandContainingSidebarGroup) {
        // Find all nav buttons and update class
        for (String id : new String[]{
            "nav-dashboard",
            "nav-reports",
            "nav-products",
            "nav-import",
            "nav-sales",
            "nav-promotions",
            "nav-attributes",
            "nav-history",
            "nav-returns",
            "nav-expenses",
            "nav-customers",
            "nav-stocktake",
            "nav-stock-history",
            "nav-accounts",
            "nav-settings"
        }) {
            javafx.scene.Node btn = root.lookup("#" + id);
            if (btn != null) {
                if (id.equals(activeNavId)) {
                    if (!btn.getStyleClass().contains("active")) btn.getStyleClass().add("active");
                } else {
                    btn.getStyleClass().remove("active");
                }
            }
        }

        if (expandContainingSidebarGroup) {
            for (javafx.scene.Node node : root.lookupAll(".sidebar-section")) {
                if (!(node instanceof VBox section)) {
                    continue;
                }
                Object itemIds = section.getProperties().get("sidebarItemIds");
                if (itemIds instanceof java.util.Set<?> ids && ids.contains(activeNavId)) {
                    setSidebarSectionExpanded(section, true, true);
                    break;
                }
            }
        }

        for (javafx.scene.Node node : root.lookupAll(".sidebar-section")) {
            if (node instanceof VBox section) {
                refreshSidebarSectionActiveDot(section);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void setSidebarSectionExpanded(VBox section, boolean expanded, boolean animated) {
        if (isReducedMotionEnabled(section)) {
            animated = false;
        }
        Object headerNode = section.getProperties().get("sidebarHeader");
        Object contentNode = section.getProperties().get("sidebarContent");
        Object chevronNode = section.getProperties().get("sidebarChevron");
        Object animationRefNode = section.getProperties().get("sidebarAnimationRef");

        if (!(headerNode instanceof javafx.scene.layout.HBox header)
            || !(contentNode instanceof VBox content)
            || !(chevronNode instanceof javafx.scene.shape.SVGPath chevronIcon)) {
            return;
        }

        boolean currentlyExpanded = content.isManaged();
        if (animationRefNode instanceof java.util.concurrent.atomic.AtomicReference<?> rawRef) {
            Object existing = rawRef.get();
            if (existing instanceof javafx.animation.Timeline timeline) {
                timeline.stop();
            }
            ((java.util.concurrent.atomic.AtomicReference<javafx.animation.Timeline>) rawRef).set(null);
        }

        if (!animated || currentlyExpanded == expanded) {
            applySidebarSectionExpandedState(header, content, chevronIcon, expanded);
            refreshSidebarSectionActiveDot(section);
            return;
        }

        java.util.concurrent.atomic.AtomicReference<javafx.animation.Timeline> animationRef =
            animationRefNode instanceof java.util.concurrent.atomic.AtomicReference<?> rawRef
                ? (java.util.concurrent.atomic.AtomicReference<javafx.animation.Timeline>) rawRef
                : new java.util.concurrent.atomic.AtomicReference<>(null);

        if (expanded) {
            content.setManaged(true);
            content.setVisible(true);
            content.setOpacity(1.0);
            content.setTranslateY(-6.0);
            content.setMaxHeight(0.0);
            javafx.animation.Timeline expand = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                    Duration.ZERO,
                    new javafx.animation.KeyValue(content.maxHeightProperty(), 0.0),
                    new javafx.animation.KeyValue(content.opacityProperty(), 0.0),
                    new javafx.animation.KeyValue(content.translateYProperty(), -6.0),
                    new javafx.animation.KeyValue(chevronIcon.rotateProperty(), chevronIcon.getRotate())
                ),
                new javafx.animation.KeyFrame(
                    Duration.millis(140),
                    new javafx.animation.KeyValue(content.maxHeightProperty(), Math.max(content.prefHeight(-1), 1), javafx.animation.Interpolator.EASE_BOTH),
                    new javafx.animation.KeyValue(content.opacityProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH),
                    new javafx.animation.KeyValue(content.translateYProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH),
                    new javafx.animation.KeyValue(chevronIcon.rotateProperty(), 90.0, javafx.animation.Interpolator.EASE_BOTH)
                )
            );
            expand.setOnFinished(ev -> {
                applySidebarSectionExpandedState(header, content, chevronIcon, true);
                refreshSidebarSectionActiveDot(section);
                animationRef.set(null);
            });
            animationRef.set(expand);
            expand.play();
            return;
        }

        double currentHeight = Math.max(content.getHeight(), content.prefHeight(-1));
        content.setManaged(true);
        content.setVisible(true);
        content.setMaxHeight(currentHeight);
        javafx.animation.Timeline collapse = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                Duration.ZERO,
                new javafx.animation.KeyValue(content.maxHeightProperty(), currentHeight),
                new javafx.animation.KeyValue(content.opacityProperty(), content.getOpacity()),
                new javafx.animation.KeyValue(content.translateYProperty(), content.getTranslateY()),
                new javafx.animation.KeyValue(chevronIcon.rotateProperty(), chevronIcon.getRotate())
            ),
            new javafx.animation.KeyFrame(
                Duration.millis(140),
                new javafx.animation.KeyValue(content.maxHeightProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(content.opacityProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(content.translateYProperty(), -6.0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(chevronIcon.rotateProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH)
            )
        );
        collapse.setOnFinished(ev -> {
            applySidebarSectionExpandedState(header, content, chevronIcon, false);
            refreshSidebarSectionActiveDot(section);
            animationRef.set(null);
        });
        animationRef.set(collapse);
        collapse.play();
    }

    private void applySidebarSectionExpandedState(
        javafx.scene.layout.HBox header,
        VBox content,
        javafx.scene.shape.SVGPath chevronIcon,
        boolean expanded
    ) {
        if (expanded) {
            content.setManaged(true);
            content.setVisible(true);
            content.setOpacity(1.0);
            content.setTranslateY(0.0);
            content.setMaxHeight(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
            chevronIcon.setRotate(90.0);
            if (!header.getStyleClass().contains("expanded")) {
                header.getStyleClass().add("expanded");
            }
            return;
        }

        content.setOpacity(0.0);
        content.setTranslateY(-6.0);
        content.setMaxHeight(0.0);
        content.setVisible(false);
        content.setManaged(false);
        chevronIcon.setRotate(0.0);
        header.getStyleClass().remove("expanded");
    }

    private javafx.scene.layout.BorderPane createMainLayout(Stage stage, com.pbl3.project.pbl3_project.entity.User user, String title, javafx.scene.Node centerContent, String activeNavId) {
        javafx.scene.layout.BorderPane root = new javafx.scene.layout.BorderPane();
        com.pbl3.project.pbl3_project.entity.UserUiPreferences preferences = userUiPreferencesService.getPreferences(user);
        
        // Sidebar
        VBox sidebar = new VBox(5);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(220);
        sidebar.setMinWidth(220);
        sidebar.setMaxWidth(220);
        
        Label appTitle = new Label("SALES MGR");
        appTitle.setId("sidebar-app-title");
        appTitle.setWrapText(true);
        appTitle.setMaxWidth(Double.MAX_VALUE);
        appTitle.setStyle("-fx-text-fill: -app-text-primary; -fx-font-weight: bold; -fx-font-size: 20px; -fx-padding: 2 0 8 15;");
        
        Button navDashboard = createNavButton("Dashboard", "nav-dashboard", () -> showOverviewScene(stage, user));
        navDashboard.getStyleClass().add("dashboard-nav-button");
        navDashboard.setGraphic(createDashboardNavIcon());
        navDashboard.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        Button navReports = createNavButton("Reports", "nav-reports", () -> showOperationalReportsScene(stage, user));
        navReports.setGraphic(createReportsNavIcon());
        navReports.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        Button navProducts = createNavButton("Products", "nav-products", () -> showDashboardScene(stage, user));
        navProducts.setGraphic(createProductsNavIcon());
        navProducts.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        Button navImport = createNavButton("Import Goods", "nav-import", () -> showImportOrderScene(stage, user));
        navImport.setGraphic(createImportNavIcon());
        navImport.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        Button navSales = createNavButton("Sales (POS)", "nav-sales", () -> showSalesScene(stage, user));
        navSales.setGraphic(createSalesNavIcon());
        navSales.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        Button navPromotions = createNavButton("Promotions", "nav-promotions", () -> showPromotionsScene(stage, user));
        navPromotions.setGraphic(createPromotionsNavIcon());
        navPromotions.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        Button navAttributes = createNavButton("Master Data", "nav-attributes", () -> showAttributesScene(stage, user));
        navAttributes.setGraphic(createMasterDataNavIcon());
        navAttributes.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        Button navHistory = createNavButton("Order History", "nav-history", () -> showOrderHistoryScene(stage, user));
        navHistory.setGraphic(createOrderHistoryNavIcon());
        navHistory.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        Button navReturns = createNavButton("Returns", "nav-returns", () -> showReturnsRefundsScene(stage, user));
        navReturns.setGraphic(createReturnsNavIcon());
        navReturns.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        Button navExpenses = createNavButton("Expenses", "nav-expenses", () -> showExpensesScene(stage, user));
        navExpenses.setGraphic(createExpensesNavIcon());
        navExpenses.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        Button navCustomers = createNavButton("Customers", "nav-customers", () -> showCustomersScene(stage, user));
        navCustomers.setGraphic(createCustomersNavIcon());
        navCustomers.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        Button navStocktake = createNavButton("Stocktake", "nav-stocktake", () -> showStocktakeScene(stage, user));
        navStocktake.setGraphic(createStocktakeNavIcon());
        navStocktake.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        Button navStockHistory = createNavButton("Audit Log", "nav-stock-history", () -> showStockHistoryScene(stage, user));
        navStockHistory.setGraphic(createAuditLogNavIcon());
        navStockHistory.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        Button navAccounts = createNavButton("Accounts", "nav-accounts", () -> showAccountsScene(stage, user));
        navAccounts.setGraphic(createAccountsNavIcon());
        navAccounts.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        Button navSettings = createNavButton("Settings", "nav-settings", () -> showSettingsScene(stage, user));
        navSettings.setGraphic(createSettingsNavIcon());
        navSettings.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        java.util.stream.Stream.of(
            navDashboard,
            navReports,
            navProducts,
            navImport,
            navSales,
            navPromotions,
            navAttributes,
            navHistory,
            navReturns,
            navExpenses,
            navCustomers,
            navStocktake,
            navStockHistory,
            navAccounts,
            navSettings
        ).forEach(this::installSidebarNavIconMotion);
        Button navLogout = new Button("Logout");
        navLogout.setId("nav-logout");
        navLogout.getStyleClass().clear();
        navLogout.getStyleClass().add("nav-logout-btn");
        installSidebarPressBounce(navLogout);
        navLogout.setOnAction(e -> showLoginScene(stage));
        navLogout.setGraphic(createLogoutNavIcon());
        installSidebarIconMotion(navLogout, navLogout.getGraphic(), null, 1.05, -0.9, 1.0, 0.0);

        // Initial Active State
        if ("nav-dashboard".equals(activeNavId)) navDashboard.getStyleClass().add("active");
        if ("nav-reports".equals(activeNavId)) navReports.getStyleClass().add("active");
        if ("nav-products".equals(activeNavId)) navProducts.getStyleClass().add("active");
        if ("nav-import".equals(activeNavId)) navImport.getStyleClass().add("active");
        if ("nav-sales".equals(activeNavId)) navSales.getStyleClass().add("active");
        if ("nav-promotions".equals(activeNavId)) navPromotions.getStyleClass().add("active");
        if ("nav-attributes".equals(activeNavId)) navAttributes.getStyleClass().add("active");
        if ("nav-history".equals(activeNavId)) navHistory.getStyleClass().add("active");
        if ("nav-returns".equals(activeNavId)) navReturns.getStyleClass().add("active");
        if ("nav-expenses".equals(activeNavId)) navExpenses.getStyleClass().add("active");
        if ("nav-customers".equals(activeNavId)) navCustomers.getStyleClass().add("active");
        if ("nav-stocktake".equals(activeNavId)) navStocktake.getStyleClass().add("active");
        if ("nav-stock-history".equals(activeNavId)) navStockHistory.getStyleClass().add("active");
        if ("nav-accounts".equals(activeNavId)) navAccounts.getStyleClass().add("active");
        if ("nav-settings".equals(activeNavId)) navSettings.getStyleClass().add("active");

        java.util.List<javafx.scene.Node> operationsItems = new java.util.ArrayList<>();
        operationsItems.add(navDashboard);
        if (authorizationService.canAccessSales(user)) {
            operationsItems.add(navSales);
        }
        if (authorizationService.canAccessPromotions(user)) {
            operationsItems.add(navPromotions);
        }
        if (authorizationService.canAccessProducts(user)) {
            operationsItems.add(navProducts);
        }
        if (authorizationService.canAccessImportGoods(user)) {
            operationsItems.add(navImport);
        }
        if (authorizationService.canAccessOrderHistory(user)) {
            operationsItems.add(navHistory);
        }
        if (authorizationService.canAccessReturnsRefunds(user)) {
            operationsItems.add(navReturns);
        }
        if (authorizationService.canAccessExpenses(user)) {
            operationsItems.add(navExpenses);
        }
        if (authorizationService.canAccessCustomers(user)) {
            operationsItems.add(navCustomers);
        }

        java.util.List<javafx.scene.Node> controlItems = new java.util.ArrayList<>();
        if (authorizationService.canAccessReports(user)) {
            controlItems.add(navReports);
        }
        if (authorizationService.canAccessStocktake(user)) {
            controlItems.add(navStocktake);
        }
        if (authorizationService.canAccessAuditLog(user)) {
            controlItems.add(navStockHistory);
        }

        java.util.List<javafx.scene.Node> setupItems = new java.util.ArrayList<>();
        if (authorizationService.canAccessMasterData(user)) {
            setupItems.add(navAttributes);
        }
        if (authorizationService.canAccessAccounts(user)) {
            setupItems.add(navAccounts);
        }

        boolean operationsExpanded = operationsItems.stream().anyMatch(node -> isSidebarNodeActive(node, activeNavId));
        boolean controlExpanded = controlItems.stream().anyMatch(node -> isSidebarNodeActive(node, activeNavId));
        boolean setupExpanded = setupItems.stream().anyMatch(node -> isSidebarNodeActive(node, activeNavId));

        VBox sidebarMenu = new VBox(10);
        sidebarMenu.setFillWidth(true);
        sidebarMenu.setPadding(new Insets(8, 8, 12, 0));

        sidebarMenu.getChildren().add(createSidebarSection("Operations", operationsItems, operationsExpanded));
        if (!controlItems.isEmpty()) {
            sidebarMenu.getChildren().add(createSidebarSection("Control", controlItems, controlExpanded));
        }
        if (!setupItems.isEmpty()) {
            sidebarMenu.getChildren().add(createSidebarSection("Setup", setupItems, setupExpanded));
        }

        javafx.scene.control.ScrollPane sidebarScroll = new javafx.scene.control.ScrollPane(sidebarMenu);
        sidebarScroll.setFitToWidth(true);
        sidebarScroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        sidebarScroll.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sidebarScroll.setPannable(true);
        sidebarScroll.getStyleClass().add("sidebar-scroll");
        javafx.scene.layout.VBox.setVgrow(sidebarScroll, javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.layout.VBox.setMargin(sidebarScroll, new Insets(0, -15, 0, 0));

        VBox sidebarBottomActions = new VBox(8);
        sidebarBottomActions.setFillWidth(true);
        sidebarBottomActions.getChildren().addAll(navSettings, navLogout);

        sidebar.getChildren().addAll(appTitle, sidebarScroll, new javafx.scene.control.Separator(), sidebarBottomActions);
        sidebar.setSpacing(8);
        sidebar.setPadding(new Insets(18, 14, 18, 14));
        
        // Clip sidebar content during animation (with rounded corners)
        javafx.scene.shape.Rectangle clipRect = new javafx.scene.shape.Rectangle();
        clipRect.widthProperty().bind(sidebar.widthProperty());
        clipRect.heightProperty().bind(sidebar.heightProperty());
        clipRect.setArcWidth(40);  // Match sidebar border-radius (20 * 2)
        clipRect.setArcHeight(40);
        sidebar.setClip(clipRect);
        currentSidebarWidth.unbind();
        currentSidebarWidth.bind(sidebar.widthProperty());
        
        // Header
        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(10);
        header.setStyle("-fx-background-color: -app-surface; -fx-padding: 15; -fx-effect: dropshadow(three-pass-box, -app-shadow, 0, 5, 0, 0);");
        header.setAlignment(Pos.CENTER_LEFT);
        
        final double sidebarWidth = 220;
        final boolean[] sidebarHidden = {false};
        Button toggleSidebar = createHamburgerToggleButton();
        java.util.function.BiConsumer<Boolean, Boolean> applySidebarCollapsed = (collapsed, animated) -> {
            if (collapsed == sidebarHidden[0]) {
                return;
            }

            if (collapsed) {
                sidebarHidden[0] = true;
                if (!animated) {
                    sidebar.setPrefWidth(0);
                    sidebar.setMinWidth(0);
                    sidebar.setMaxWidth(0);
                    sidebar.setOpacity(0);
                    javafx.scene.layout.BorderPane.setMargin(sidebar, new Insets(0));
                    root.requestLayout();
                    return;
                }

                setCenterContentCache(root, true);
                javafx.animation.Timeline hideTimeline = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(
                        javafx.util.Duration.ZERO,
                        new javafx.animation.KeyValue(sidebar.prefWidthProperty(), sidebarWidth),
                        new javafx.animation.KeyValue(sidebar.minWidthProperty(), sidebarWidth),
                        new javafx.animation.KeyValue(sidebar.maxWidthProperty(), sidebarWidth),
                        new javafx.animation.KeyValue(sidebar.opacityProperty(), 1.0)
                    ),
                    new javafx.animation.KeyFrame(
                        javafx.util.Duration.millis(150),
                        new javafx.animation.KeyValue(sidebar.prefWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                        new javafx.animation.KeyValue(sidebar.minWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                        new javafx.animation.KeyValue(sidebar.maxWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                        new javafx.animation.KeyValue(sidebar.opacityProperty(), 0, javafx.animation.Interpolator.EASE_IN)
                    )
                );
                hideTimeline.setOnFinished(ev -> {
                    javafx.scene.layout.BorderPane.setMargin(sidebar, new Insets(0));
                    setCenterContentCache(root, false);
                    root.requestLayout();
                });
                hideTimeline.play();
                return;
            }

            sidebarHidden[0] = false;
            javafx.scene.layout.BorderPane.setMargin(sidebar, new Insets(15, 0, 15, 15));
            if (!animated) {
                sidebar.setPrefWidth(sidebarWidth);
                sidebar.setMinWidth(sidebarWidth);
                sidebar.setMaxWidth(sidebarWidth);
                sidebar.setOpacity(1.0);
                root.requestLayout();
                return;
            }

            setCenterContentCache(root, true);
            javafx.animation.Timeline showTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                    javafx.util.Duration.ZERO,
                    new javafx.animation.KeyValue(sidebar.prefWidthProperty(), 0),
                    new javafx.animation.KeyValue(sidebar.minWidthProperty(), 0),
                    new javafx.animation.KeyValue(sidebar.maxWidthProperty(), 0),
                    new javafx.animation.KeyValue(sidebar.opacityProperty(), 0)
                ),
                new javafx.animation.KeyFrame(
                    javafx.util.Duration.millis(150),
                    new javafx.animation.KeyValue(sidebar.prefWidthProperty(), sidebarWidth, javafx.animation.Interpolator.EASE_BOTH),
                    new javafx.animation.KeyValue(sidebar.minWidthProperty(), sidebarWidth, javafx.animation.Interpolator.EASE_BOTH),
                    new javafx.animation.KeyValue(sidebar.maxWidthProperty(), sidebarWidth, javafx.animation.Interpolator.EASE_BOTH),
                    new javafx.animation.KeyValue(sidebar.opacityProperty(), 1.0, javafx.animation.Interpolator.EASE_OUT)
                )
            );
            showTimeline.setOnFinished(ev -> {
                sidebar.setPrefWidth(sidebarWidth);
                sidebar.setMinWidth(sidebarWidth);
                sidebar.setMaxWidth(sidebarWidth);
                setCenterContentCache(root, false);
                root.requestLayout();
            });
            showTimeline.play();
        };
        root.getProperties().put(SIDEBAR_COLLAPSE_APPLIER_KEY, applySidebarCollapsed);

        toggleSidebar.setOnAction(e ->
            applySidebarCollapsed.accept(!sidebarHidden[0], !isReducedMotionEnabled(root))
        );
        
        Label pageTitle = new Label(title);
        pageTitle.setId("header-title"); // ID for lookup
        pageTitle.getStyleClass().add("header-label");
        
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        Button myAccountButton = new Button();
        myAccountButton.getStyleClass().addAll("button", "header-account-button");
        myAccountButton.setGraphic(createMyAccountHeaderIcon());
        myAccountButton.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        myAccountButton.setTooltip(new javafx.scene.control.Tooltip("My Account"));
        myAccountButton.setMinSize(36, 36);
        myAccountButton.setPrefSize(36, 36);
        myAccountButton.setMaxSize(36, 36);
        installSidebarPressBounce(myAccountButton);
        myAccountButton.setOnAction(e -> showMyAccountScene(stage, user));

        Label userLabel = new Label(user.getFullName() + " (" + formatRoleLabel(user.getRole()) + ")");
        userLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -app-text-muted;");

        header.getChildren().addAll(toggleSidebar, pageTitle, spacer, userLabel, myAccountButton);
        
        root.setLeft(sidebar);
        root.setTop(header);
        root.setCenter(centerContent);
        
        javafx.scene.layout.BorderPane.setMargin(sidebar, new Insets(15, 0, 15, 15));
        // Removed margin for centerContent to fix deselect dead zone
        
        root.setStyle("-fx-background-color: -app-surface-muted;");
        applyUserUiPreferences(root, preferences, false);
        applySidebarCollapsed.accept(preferences.isSidebarCollapsedByDefault(), false);
        
        return root;
    }

    private VBox createSidebarSection(String title, java.util.List<javafx.scene.Node> items, boolean expandedInitially) {
        VBox section = new VBox(8);
        section.getStyleClass().add("sidebar-section");
        section.setFillWidth(true);
        section.getProperties().put(
            "sidebarItemIds",
            items.stream()
                .map(javafx.scene.Node::getId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet())
        );

        Label sectionLabel = new Label(title);
        sectionLabel.getStyleClass().add("sidebar-section-label");
        sectionLabel.setMinWidth(0);
        sectionLabel.setMaxWidth(Double.MAX_VALUE);
        javafx.scene.Node sectionIcon = createSidebarSectionIcon(title);

        javafx.scene.shape.SVGPath chevronIcon = new javafx.scene.shape.SVGPath();
        chevronIcon.setContent("M8 6 L14 12 L8 18");
        chevronIcon.getStyleClass().add("sidebar-section-chevron");
        chevronIcon.setStrokeWidth(2.0);
        chevronIcon.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        chevronIcon.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        chevronIcon.setFill(javafx.scene.paint.Color.TRANSPARENT);
        chevronIcon.setRotate(expandedInitially ? 90 : 0);

        javafx.scene.layout.StackPane chevronWrap = new javafx.scene.layout.StackPane(chevronIcon);
        chevronWrap.setMinSize(18, 18);
        chevronWrap.setPrefSize(18, 18);
        chevronWrap.setMaxSize(18, 18);

        javafx.scene.shape.Circle activeDot = new javafx.scene.shape.Circle(3.2);
        activeDot.getStyleClass().add("sidebar-section-active-dot");
        javafx.scene.layout.StackPane activeDotWrap = new javafx.scene.layout.StackPane(activeDot);
        activeDotWrap.setMinSize(8, 8);
        activeDotWrap.setPrefSize(8, 8);
        activeDotWrap.setMaxSize(8, 8);
        activeDotWrap.setTranslateY(1.0);
        activeDotWrap.setVisible(false);
        activeDotWrap.setManaged(false);

        javafx.scene.layout.HBox trailingWrap = new javafx.scene.layout.HBox(4, activeDotWrap, chevronWrap);
        trailingWrap.setAlignment(Pos.CENTER_RIGHT);
        trailingWrap.setMinSize(javafx.scene.layout.Region.USE_PREF_SIZE, 18);
        trailingWrap.setPrefHeight(18);

        javafx.scene.layout.HBox header = sectionIcon == null
            ? new javafx.scene.layout.HBox(8, sectionLabel, trailingWrap)
            : new javafx.scene.layout.HBox(8, sectionIcon, sectionLabel, trailingWrap);
        javafx.scene.layout.HBox.setHgrow(sectionLabel, javafx.scene.layout.Priority.ALWAYS);
        header.getStyleClass().add("sidebar-section-header");
        if (expandedInitially && !header.getStyleClass().contains("expanded")) {
            header.getStyleClass().add("expanded");
        }
        header.setAlignment(Pos.CENTER_LEFT);
        header.setCursor(javafx.scene.Cursor.HAND);
        installSidebarPressBounce(header);
        if (sectionIcon != null) {
            installSidebarIconMotion(header, sectionIcon, "expanded", 1.04, -0.75, 1.018, -0.3);
        }
        section.getProperties().put("sidebarHeader", header);
        section.getProperties().put("sidebarChevron", chevronIcon);
        section.getProperties().put("sidebarActiveDot", activeDotWrap);

        VBox content = new VBox(6);
        content.getStyleClass().add("sidebar-section-content");
        content.setFillWidth(true);
        content.getChildren().addAll(items);
        content.setMinHeight(0);
        content.setOpacity(expandedInitially ? 1.0 : 0.0);
        content.setTranslateY(expandedInitially ? 0 : -6);
        content.setManaged(expandedInitially);
        content.setVisible(expandedInitially);
        content.setMaxHeight(expandedInitially ? javafx.scene.layout.Region.USE_COMPUTED_SIZE : 0);
        section.getProperties().put("sidebarContent", content);

        javafx.scene.shape.Rectangle contentClip = new javafx.scene.shape.Rectangle();
        contentClip.setX(-12);
        contentClip.setY(-12);
        contentClip.widthProperty().bind(content.widthProperty().add(24));
        contentClip.heightProperty().bind(content.heightProperty().add(24));
        content.setClip(contentClip);

        java.util.concurrent.atomic.AtomicReference<javafx.animation.Timeline> animationRef =
            new java.util.concurrent.atomic.AtomicReference<>(null);
        section.getProperties().put("sidebarAnimationRef", animationRef);

        header.setOnMouseClicked(e -> setSidebarSectionExpanded(section, !content.isManaged(), true));

        section.getChildren().addAll(header, content);
        refreshSidebarSectionActiveDot(section);
        return section;
    }

    private void refreshSidebarSectionActiveDot(VBox section) {
        Object contentNode = section.getProperties().get("sidebarContent");
        Object activeDotNode = section.getProperties().get("sidebarActiveDot");

        if (!(contentNode instanceof VBox content) || !(activeDotNode instanceof javafx.scene.layout.StackPane activeDotWrap)) {
            return;
        }

        boolean hasActiveChild = content.getChildren().stream()
            .filter(javafx.scene.Node.class::isInstance)
            .map(javafx.scene.Node.class::cast)
            .anyMatch(node -> node.getStyleClass().contains("active"));
        boolean showDot = hasActiveChild && !content.isManaged();

        activeDotWrap.setManaged(showDot);
        activeDotWrap.setVisible(showDot);
        activeDotWrap.setOpacity(showDot ? 1.0 : 0.0);
    }

    private javafx.scene.Node createSidebarSectionIcon(String title) {
        if ("Operations".equals(title)) {
            javafx.scene.shape.SVGPath activityPath = new javafx.scene.shape.SVGPath();
            activityPath.setContent("M22 12h-2.48a2 2 0 0 0-1.93 1.46l-2.35 8.36a.25.25 0 0 1-.48 0L9.24 2.18a.25.25 0 0 0-.48 0l-2.35 8.36A2 2 0 0 1 4.49 12H2");
            activityPath.setFill(javafx.scene.paint.Color.TRANSPARENT);
            activityPath.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            activityPath.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            activityPath.setSmooth(true);
            activityPath.getStyleClass().add("sidebar-section-icon-stroke");

            javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(activityPath);
            iconWrap.setMinSize(18, 18);
            iconWrap.setPrefSize(18, 18);
            iconWrap.setMaxSize(18, 18);
            iconWrap.setScaleX(0.78);
            iconWrap.setScaleY(0.78);
            iconWrap.setMouseTransparent(true);
            return iconWrap;
        }

        if ("Control".equals(title)) {
            javafx.scene.shape.SVGPath slidersPath = new javafx.scene.shape.SVGPath();
            slidersPath.setContent("M21 4H14 M10 4H3 M21 12H12 M8 12H3 M21 20H16 M12 20H3 M14 2V6 M8 10V14 M16 18V22");
            slidersPath.setFill(javafx.scene.paint.Color.TRANSPARENT);
            slidersPath.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            slidersPath.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            slidersPath.setSmooth(true);
            slidersPath.getStyleClass().add("sidebar-section-icon-stroke");

            javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(slidersPath);
            iconWrap.setMinSize(18, 18);
            iconWrap.setPrefSize(18, 18);
            iconWrap.setMaxSize(18, 18);
            iconWrap.setScaleX(0.78);
            iconWrap.setScaleY(0.78);
            iconWrap.setMouseTransparent(true);
            return iconWrap;
        }

        if (!"Setup".equals(title)) {
            return null;
        }

        javafx.scene.shape.SVGPath wrenchPath = new javafx.scene.shape.SVGPath();
        wrenchPath.setContent("M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.4-3.4a6 6 0 0 1-7.94 7.94l-6.91 6.91a2 2 0 0 1-2.83-2.83l6.9-6.9a6 6 0 0 1 7.94-7.94z");
        wrenchPath.setFill(javafx.scene.paint.Color.TRANSPARENT);
        wrenchPath.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        wrenchPath.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        wrenchPath.setSmooth(true);
        wrenchPath.getStyleClass().add("sidebar-section-icon-stroke");

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(wrenchPath);
        iconWrap.setMinSize(18, 18);
        iconWrap.setPrefSize(18, 18);
        iconWrap.setMaxSize(18, 18);
        iconWrap.setScaleX(0.8);
        iconWrap.setScaleY(0.8);
        iconWrap.setMouseTransparent(true);
        return iconWrap;
    }

    private boolean isSidebarNodeActive(javafx.scene.Node node, String activeNavId) {
        return activeNavId != null && activeNavId.equals(node.getId());
    }
    
    private javafx.scene.Node createSlidingMenu(String[] tabNames, java.util.function.Consumer<Integer> onSelect) {
        javafx.scene.layout.HBox container = new javafx.scene.layout.HBox();
        container.getStyleClass().add("sliding-menu-root");
        container.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        container.setAlignment(Pos.CENTER_LEFT);

        javafx.scene.layout.StackPane wrapper = new javafx.scene.layout.StackPane();
        wrapper.setAlignment(Pos.CENTER_LEFT);
        wrapper.getStyleClass().add("sliding-menu-wrapper");

        javafx.scene.layout.Region outerCapsule = new javafx.scene.layout.Region();
        outerCapsule.getStyleClass().add("sliding-menu-container");
        outerCapsule.setManaged(false);
        outerCapsule.setMouseTransparent(true);

        javafx.scene.layout.Region hoverIndicator = new javafx.scene.layout.Region();
        hoverIndicator.getStyleClass().add("hover-capsule");
        hoverIndicator.setManaged(false);
        hoverIndicator.setMouseTransparent(true);
        hoverIndicator.setOpacity(0);

        javafx.scene.layout.Region indicator = new javafx.scene.layout.Region();
        indicator.getStyleClass().add("active-capsule");
        indicator.setManaged(false);
        indicator.setMouseTransparent(true);

        javafx.scene.layout.HBox tabsBox = new javafx.scene.layout.HBox(0);
        tabsBox.setAlignment(Pos.CENTER_LEFT);
        tabsBox.getStyleClass().add("menu-items-layer");

        javafx.scene.control.ToggleGroup toggleGroup = new javafx.scene.control.ToggleGroup();
        java.util.List<javafx.scene.control.ToggleButton> tabButtons = new java.util.ArrayList<>();

        final javafx.beans.property.DoubleProperty indicatorX = new javafx.beans.property.SimpleDoubleProperty(0);
        final javafx.beans.property.DoubleProperty indicatorY = new javafx.beans.property.SimpleDoubleProperty(0);
        final javafx.beans.property.DoubleProperty indicatorW = new javafx.beans.property.SimpleDoubleProperty(0);
        final javafx.beans.property.DoubleProperty indicatorH = new javafx.beans.property.SimpleDoubleProperty(0);
        final javafx.beans.property.DoubleProperty hoverX = new javafx.beans.property.SimpleDoubleProperty(0);
        final javafx.beans.property.DoubleProperty hoverY = new javafx.beans.property.SimpleDoubleProperty(0);
        final javafx.beans.property.DoubleProperty hoverW = new javafx.beans.property.SimpleDoubleProperty(0);
        final javafx.beans.property.DoubleProperty hoverH = new javafx.beans.property.SimpleDoubleProperty(0);
        final javafx.beans.property.ObjectProperty<javafx.scene.control.ToggleButton> hoveredButtonRef = new javafx.beans.property.SimpleObjectProperty<>();

        Runnable applyIndicatorBounds = () -> indicator.resizeRelocate(
            indicatorX.get(),
            indicatorY.get(),
            Math.max(0, indicatorW.get()),
            Math.max(0, indicatorH.get())
        );
        Runnable applyHoverBounds = () -> hoverIndicator.resizeRelocate(
            hoverX.get(),
            hoverY.get(),
            Math.max(0, hoverW.get()),
            Math.max(0, hoverH.get())
        );

        javafx.beans.value.ChangeListener<Number> indicatorGeometryListener = (obs, oldVal, newVal) -> applyIndicatorBounds.run();
        indicatorX.addListener(indicatorGeometryListener);
        indicatorY.addListener(indicatorGeometryListener);
        indicatorW.addListener(indicatorGeometryListener);
        indicatorH.addListener(indicatorGeometryListener);
        javafx.beans.value.ChangeListener<Number> hoverGeometryListener = (obs, oldVal, newVal) -> applyHoverBounds.run();
        hoverX.addListener(hoverGeometryListener);
        hoverY.addListener(hoverGeometryListener);
        hoverW.addListener(hoverGeometryListener);
        hoverH.addListener(hoverGeometryListener);

        final javafx.animation.Timeline[] indicatorTimelineRef = new javafx.animation.Timeline[1];
        final javafx.animation.Timeline[] hoverTimelineRef = new javafx.animation.Timeline[1];

        java.util.function.BiConsumer<javafx.scene.control.ToggleButton, Boolean> moveHoverToButton = (button, animate) -> {
            if (button == null || wrapper.getScene() == null) {
                return;
            }

            javafx.geometry.Bounds targetBounds = wrapper.sceneToLocal(button.localToScene(button.getBoundsInLocal()));
            if (targetBounds.getWidth() <= 0 || targetBounds.getHeight() <= 0) {
                return;
            }

            if (hoverTimelineRef[0] != null) {
                hoverTimelineRef[0].stop();
            }

            if (!animate || hoverIndicator.getOpacity() <= 0 || hoverW.get() <= 0 || hoverH.get() <= 0) {
                hoverX.set(targetBounds.getMinX());
                hoverY.set(targetBounds.getMinY());
                hoverW.set(targetBounds.getWidth());
                hoverH.set(targetBounds.getHeight());
                hoverIndicator.setOpacity(1);
                return;
            }

            hoverTimelineRef[0] = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(180),
                    new javafx.animation.KeyValue(hoverX, targetBounds.getMinX(), SLIDING_MENU_INTERPOLATOR),
                    new javafx.animation.KeyValue(hoverY, targetBounds.getMinY(), SLIDING_MENU_INTERPOLATOR),
                    new javafx.animation.KeyValue(hoverW, targetBounds.getWidth(), SLIDING_MENU_INTERPOLATOR),
                    new javafx.animation.KeyValue(hoverH, targetBounds.getHeight(), SLIDING_MENU_INTERPOLATOR),
                    new javafx.animation.KeyValue(hoverIndicator.opacityProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH)
                )
            );
            hoverTimelineRef[0].play();
        };

        java.util.function.Consumer<Boolean> hideHoverIndicator = animate -> {
            if (hoverTimelineRef[0] != null) {
                hoverTimelineRef[0].stop();
            }

            if (!animate || hoverIndicator.getOpacity() <= 0) {
                hoverIndicator.setOpacity(0);
                return;
            }

            hoverTimelineRef[0] = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(120),
                    new javafx.animation.KeyValue(hoverIndicator.opacityProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH)
                )
            );
            hoverTimelineRef[0].play();
        };

        java.util.function.BiConsumer<javafx.scene.control.ToggleButton, Boolean> moveIndicatorToButton = (button, animate) -> {
            if (button == null || wrapper.getScene() == null) {
                return;
            }

            javafx.geometry.Bounds targetBounds = wrapper.sceneToLocal(button.localToScene(button.getBoundsInLocal()));
            if (targetBounds.getWidth() <= 0 || targetBounds.getHeight() <= 0) {
                return;
            }

            if (indicatorTimelineRef[0] != null) {
                indicatorTimelineRef[0].stop();
            }

            if (!animate || indicatorW.get() <= 0 || indicatorH.get() <= 0) {
                indicatorX.set(targetBounds.getMinX());
                indicatorY.set(targetBounds.getMinY());
                indicatorW.set(targetBounds.getWidth());
                indicatorH.set(targetBounds.getHeight());
                return;
            }

            indicatorTimelineRef[0] = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(240),
                    new javafx.animation.KeyValue(indicatorX, targetBounds.getMinX(), SLIDING_MENU_INTERPOLATOR),
                    new javafx.animation.KeyValue(indicatorY, targetBounds.getMinY(), SLIDING_MENU_INTERPOLATOR),
                    new javafx.animation.KeyValue(indicatorW, targetBounds.getWidth(), SLIDING_MENU_INTERPOLATOR),
                    new javafx.animation.KeyValue(indicatorH, targetBounds.getHeight(), SLIDING_MENU_INTERPOLATOR)
                )
            );
            indicatorTimelineRef[0].play();
        };

        Runnable syncOuterCapsule = () -> outerCapsule.resizeRelocate(0, 0, wrapper.getWidth(), wrapper.getHeight());
        Runnable syncSelectedIndicator = () -> {
            if (!(toggleGroup.getSelectedToggle() instanceof javafx.scene.control.ToggleButton selectedButton)) {
                return;
            }
            moveIndicatorToButton.accept(selectedButton, false);
        };
        Runnable syncHoverIndicator = () -> {
            javafx.scene.control.ToggleButton hoveredButton = hoveredButtonRef.get();
            if (hoveredButton == null || !hoveredButton.isHover() || hoveredButton == toggleGroup.getSelectedToggle()) {
                hideHoverIndicator.accept(false);
                return;
            }
            moveHoverToButton.accept(hoveredButton, false);
        };

        for (int i = 0; i < tabNames.length; i++) {
            final int index = i;

            javafx.scene.control.ToggleButton tabButton = new javafx.scene.control.ToggleButton(tabNames[i]);
            tabButton.getStyleClass().setAll("tab-button-label");
            tabButton.setToggleGroup(toggleGroup);
            tabButton.setUserData(index);
            tabButton.setFocusTraversable(false);
            tabButton.setMnemonicParsing(false);
            double stableButtonWidth = computeSlidingMenuButtonWidth(tabNames[i]);
            tabButton.setMinWidth(stableButtonWidth);
            tabButton.setPrefWidth(stableButtonWidth);
            tabButton.setMaxWidth(stableButtonWidth);

            tabButton.setOnAction(e -> onSelect.accept(index));
            tabButton.hoverProperty().addListener((obs, wasHovering, isHovering) -> {
                if (isHovering) {
                    hoveredButtonRef.set(tabButton);
                    if (tabButton == toggleGroup.getSelectedToggle()) {
                        hideHoverIndicator.accept(true);
                    } else {
                        moveHoverToButton.accept(tabButton, true);
                    }
                    return;
                }

                javafx.application.Platform.runLater(() -> {
                    javafx.scene.control.ToggleButton nextHoveredButton = null;
                    for (javafx.scene.control.ToggleButton candidate : tabButtons) {
                        if (candidate.isHover()) {
                            nextHoveredButton = candidate;
                            break;
                        }
                    }
                    hoveredButtonRef.set(nextHoveredButton);
                    if (nextHoveredButton == null || nextHoveredButton == toggleGroup.getSelectedToggle()) {
                        hideHoverIndicator.accept(true);
                    } else {
                        moveHoverToButton.accept(nextHoveredButton, true);
                    }
                });
            });

            tabButtons.add(tabButton);
            tabsBox.getChildren().add(tabButton);
        }

        toggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                if (oldToggle != null) {
                    toggleGroup.selectToggle(oldToggle);
                }
                return;
            }
            if (newToggle instanceof javafx.scene.control.ToggleButton selectedButton) {
                boolean animate = oldToggle != null;
                moveIndicatorToButton.accept(selectedButton, animate);
                syncHoverIndicator.run();
            }
        });

        if (!tabButtons.isEmpty()) {
            toggleGroup.selectToggle(tabButtons.get(0));
        }

        wrapper.widthProperty().addListener((obs, oldVal, newVal) -> {
            syncOuterCapsule.run();
            syncSelectedIndicator.run();
            syncHoverIndicator.run();
        });
        wrapper.heightProperty().addListener((obs, oldVal, newVal) -> {
            syncOuterCapsule.run();
            syncSelectedIndicator.run();
            syncHoverIndicator.run();
        });
        tabsBox.layoutBoundsProperty().addListener((obs, oldVal, newVal) -> {
            syncOuterCapsule.run();
            syncSelectedIndicator.run();
            syncHoverIndicator.run();
        });
        for (javafx.scene.control.ToggleButton tabButton : tabButtons) {
            tabButton.layoutBoundsProperty().addListener((obs, oldVal, newVal) -> {
                syncSelectedIndicator.run();
                syncHoverIndicator.run();
            });
            tabButton.localToSceneTransformProperty().addListener((obs, oldVal, newVal) -> {
                syncSelectedIndicator.run();
                syncHoverIndicator.run();
            });
        }

        javafx.application.Platform.runLater(() -> {
            syncOuterCapsule.run();
            syncSelectedIndicator.run();
            syncHoverIndicator.run();
        });

        wrapper.setOnMouseExited(event -> {
            hoveredButtonRef.set(null);
            hideHoverIndicator.accept(true);
        });

        wrapper.getChildren().addAll(outerCapsule, hoverIndicator, indicator, tabsBox);
        container.getChildren().add(wrapper);
        return container;
    }

    private double computeSlidingMenuButtonWidth(String labelText) {
        javafx.scene.text.Text mediumText = new javafx.scene.text.Text(labelText);
        mediumText.setFont(javafx.scene.text.Font.font(SLIDING_MENU_FONT_FAMILY, javafx.scene.text.FontWeight.MEDIUM, SLIDING_MENU_FONT_SIZE));

        javafx.scene.text.Text boldText = new javafx.scene.text.Text(labelText);
        boldText.setFont(javafx.scene.text.Font.font(SLIDING_MENU_FONT_FAMILY, javafx.scene.text.FontWeight.BOLD, SLIDING_MENU_FONT_SIZE));

        double textWidth = Math.max(
            mediumText.getLayoutBounds().getWidth(),
            boldText.getLayoutBounds().getWidth()
        );
        return Math.ceil(textWidth + (SLIDING_MENU_HORIZONTAL_PADDING * 2) + SLIDING_MENU_WIDTH_BUFFER);
    }

    private Button createExpandableGreenActionButton(String labelText, double expandedWidth) {
        String baseStyle = "-fx-background-color: -app-success; -fx-background-radius: 20; -fx-padding: 0;";
        String hoverStyle = "-fx-background-color: -app-success-hover; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, -app-shadow, 15, 0, 0, 6); -fx-padding: 0;";

        Button actionButton = new Button();
        actionButton.setStyle(baseStyle);
        actionButton.setPrefSize(40, 40);
        actionButton.setMinSize(40, 40);
        actionButton.setMaxSize(40, 40);
        actionButton.setCursor(javafx.scene.Cursor.HAND);
        actionButton.setFocusTraversable(false);

        javafx.scene.shape.SVGPath plusIcon = new javafx.scene.shape.SVGPath();
        plusIcon.setContent("M12 5v14M5 12h14");
        plusIcon.setStroke(SURFACE_COLOR);
        plusIcon.setStrokeWidth(2.5);
        plusIcon.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

        javafx.scene.layout.StackPane iconWrapper = new javafx.scene.layout.StackPane(plusIcon);
        iconWrapper.setPrefSize(40, 40);
        iconWrapper.setMinSize(40, 40);
        iconWrapper.setMaxSize(40, 40);

        Label actionLabel = new Label(labelText);
        actionLabel.setStyle("-fx-font-family: 'Be Vietnam Pro', 'Inter', sans-serif; -fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: -app-surface; -fx-padding: 0;");
        actionLabel.setOpacity(0);
        actionLabel.setScaleX(0.8);
        actionLabel.setScaleY(0.8);
        actionLabel.setTranslateX(21);

        javafx.scene.effect.GaussianBlur labelBlur = new javafx.scene.effect.GaussianBlur(4.0);
        actionLabel.setEffect(labelBlur);

        javafx.scene.layout.StackPane buttonContent = new javafx.scene.layout.StackPane(iconWrapper, actionLabel);
        javafx.scene.layout.StackPane.setAlignment(iconWrapper, Pos.CENTER_LEFT);
        javafx.scene.layout.StackPane.setAlignment(actionLabel, Pos.CENTER_LEFT);
        javafx.scene.layout.StackPane.setMargin(actionLabel, new Insets(0, 0, 0, 36));

        javafx.scene.shape.Rectangle buttonClip = new javafx.scene.shape.Rectangle();
        buttonClip.setArcWidth(40);
        buttonClip.setArcHeight(40);
        buttonClip.widthProperty().bind(actionButton.widthProperty());
        buttonClip.heightProperty().bind(actionButton.heightProperty());
        buttonContent.setClip(buttonClip);

        actionButton.setGraphic(buttonContent);
        actionButton.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        actionButton.setAlignment(Pos.CENTER_LEFT);

        javafx.animation.Timeline hoverInAnimation = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.millis(210),
                new javafx.animation.KeyValue(actionButton.minWidthProperty(), expandedWidth, SPRING_BOUNCE),
                new javafx.animation.KeyValue(actionButton.prefWidthProperty(), expandedWidth, SPRING_BOUNCE),
                new javafx.animation.KeyValue(actionButton.maxWidthProperty(), expandedWidth, SPRING_BOUNCE),
                new javafx.animation.KeyValue(plusIcon.rotateProperty(), 90, SPRING_BOUNCE),
                new javafx.animation.KeyValue(iconWrapper.scaleXProperty(), 1.05, SPRING_BOUNCE),
                new javafx.animation.KeyValue(iconWrapper.scaleYProperty(), 1.05, SPRING_BOUNCE),
                new javafx.animation.KeyValue(actionLabel.opacityProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(iconWrapper.translateXProperty(), 6, SPRING_BOUNCE),
                new javafx.animation.KeyValue(actionLabel.translateXProperty(), 6, SPRING_BOUNCE),
                new javafx.animation.KeyValue(actionLabel.scaleXProperty(), 1.0, SPRING_BOUNCE),
                new javafx.animation.KeyValue(actionLabel.scaleYProperty(), 1.0, SPRING_BOUNCE),
                new javafx.animation.KeyValue(labelBlur.radiusProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH)
            )
        );

        javafx.animation.Timeline hoverOutAnimation = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.millis(210),
                new javafx.animation.KeyValue(actionButton.minWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(actionButton.prefWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(actionButton.maxWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(plusIcon.rotateProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(iconWrapper.scaleXProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(iconWrapper.scaleYProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(actionLabel.opacityProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(iconWrapper.translateXProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(actionLabel.translateXProperty(), 21, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(actionLabel.scaleXProperty(), 0.8, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(actionLabel.scaleYProperty(), 0.8, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(labelBlur.radiusProperty(), 4.0, javafx.animation.Interpolator.EASE_BOTH)
            )
        );

        actionButton.setOnMouseEntered(e -> {
            actionButton.setStyle(hoverStyle);
            hoverOutAnimation.stop();
            hoverInAnimation.play();
        });

        actionButton.setOnMouseExited(e -> {
            actionButton.setStyle(baseStyle);
            hoverInAnimation.stop();
            hoverOutAnimation.play();
        });

        actionButton.setOnMousePressed(e -> {
            actionButton.setScaleX(0.95);
            actionButton.setScaleY(0.95);
        });

        actionButton.setOnMouseReleased(e -> {
            actionButton.setScaleX(1.0);
            actionButton.setScaleY(1.0);
        });

        return actionButton;
    }

    private Button createExpandableManageActionButton(String labelText, double expandedWidth) {
        final double collapsedSize = 40;
        final double durationMs = 210;

        String baseStyle = "-fx-background-color: -app-primary; -fx-background-radius: 20; -fx-padding: 0; "
            + "-fx-effect: dropshadow(three-pass-box, rgba(29,125,242,0.30), 12, 0, 0, 4);";
        String hoverStyle = "-fx-background-color: -app-primary-hover; -fx-background-radius: 20; -fx-padding: 0; "
            + "-fx-effect: dropshadow(three-pass-box, rgba(29,125,242,0.40), 18, 0, 0, 6);";
        String disabledStyle = "-fx-background-color: derive(-app-primary, 20%); -fx-background-radius: 20; -fx-padding: 0;";

        Button actionButton = new Button();
        actionButton.getStyleClass().clear();
        actionButton.setStyle(baseStyle);
        actionButton.setPrefSize(collapsedSize, collapsedSize);
        actionButton.setMinSize(collapsedSize, collapsedSize);
        actionButton.setMaxSize(collapsedSize, collapsedSize);
        actionButton.setCursor(javafx.scene.Cursor.HAND);
        actionButton.setFocusTraversable(false);

        javafx.scene.Node clipboardIcon = createManageClipboardIcon();
        javafx.scene.layout.StackPane iconWrapper = new javafx.scene.layout.StackPane(clipboardIcon);
        iconWrapper.setPrefSize(collapsedSize, collapsedSize);
        iconWrapper.setMinSize(collapsedSize, collapsedSize);
        iconWrapper.setMaxSize(collapsedSize, collapsedSize);

        Label actionLabel = new Label(labelText);
        actionLabel.setStyle("-fx-font-family: 'Be Vietnam Pro', 'Inter', sans-serif; -fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: -app-surface; -fx-padding: 0;");
        actionLabel.setOpacity(0);
        actionLabel.setTranslateX(-10);

        javafx.scene.layout.StackPane buttonContent = new javafx.scene.layout.StackPane(iconWrapper, actionLabel);
        javafx.scene.layout.StackPane.setAlignment(iconWrapper, Pos.CENTER_LEFT);
        javafx.scene.layout.StackPane.setAlignment(actionLabel, Pos.CENTER_LEFT);
        javafx.scene.layout.StackPane.setMargin(actionLabel, new Insets(0, 0, 0, 36));

        javafx.scene.shape.Rectangle buttonClip = new javafx.scene.shape.Rectangle();
        buttonClip.setArcWidth(collapsedSize);
        buttonClip.setArcHeight(collapsedSize);
        buttonClip.widthProperty().bind(actionButton.widthProperty());
        buttonClip.heightProperty().bind(actionButton.heightProperty());
        buttonContent.setClip(buttonClip);

        actionButton.setGraphic(buttonContent);
        actionButton.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        actionButton.setAlignment(Pos.CENTER_LEFT);

        javafx.animation.Timeline hoverInAnimation = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.millis(durationMs),
                new javafx.animation.KeyValue(actionButton.minWidthProperty(), expandedWidth, SPRING_BOUNCE),
                new javafx.animation.KeyValue(actionButton.prefWidthProperty(), expandedWidth, SPRING_BOUNCE),
                new javafx.animation.KeyValue(actionButton.maxWidthProperty(), expandedWidth, SPRING_BOUNCE),
                new javafx.animation.KeyValue(iconWrapper.scaleXProperty(), 1.05, SPRING_BOUNCE),
                new javafx.animation.KeyValue(iconWrapper.scaleYProperty(), 1.05, SPRING_BOUNCE),
                new javafx.animation.KeyValue(actionLabel.opacityProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(actionLabel.translateXProperty(), 0, SPRING_BOUNCE)
            )
        );

        javafx.animation.Timeline hoverOutAnimation = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.millis(durationMs),
                new javafx.animation.KeyValue(actionButton.minWidthProperty(), collapsedSize, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(actionButton.prefWidthProperty(), collapsedSize, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(actionButton.maxWidthProperty(), collapsedSize, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(iconWrapper.scaleXProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(iconWrapper.scaleYProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(actionLabel.opacityProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(actionLabel.translateXProperty(), -10, javafx.animation.Interpolator.EASE_BOTH)
            )
        );

        actionButton.setOnMouseEntered(e -> {
            if (actionButton.isDisabled()) {
                return;
            }
            actionButton.setStyle(hoverStyle);
            hoverOutAnimation.stop();
            hoverInAnimation.play();
        });

        actionButton.setOnMouseExited(e -> {
            if (actionButton.isDisabled()) {
                return;
            }
            actionButton.setStyle(baseStyle);
            hoverInAnimation.stop();
            hoverOutAnimation.play();
        });

        actionButton.setOnMousePressed(e -> {
            if (actionButton.isDisabled()) {
                return;
            }
            actionButton.setScaleX(0.95);
            actionButton.setScaleY(0.95);
        });

        actionButton.setOnMouseReleased(e -> {
            actionButton.setScaleX(1.0);
            actionButton.setScaleY(1.0);
        });

        actionButton.disabledProperty().addListener((obs, wasDisabled, isDisabled) -> {
            hoverInAnimation.stop();
            hoverOutAnimation.stop();
            actionButton.setScaleX(1.0);
            actionButton.setScaleY(1.0);
            actionButton.setMinWidth(collapsedSize);
            actionButton.setPrefWidth(collapsedSize);
            actionButton.setMaxWidth(collapsedSize);
            iconWrapper.setScaleX(1.0);
            iconWrapper.setScaleY(1.0);
            actionLabel.setOpacity(0.0);
            actionLabel.setTranslateX(-10);
            actionButton.setOpacity(isDisabled ? 0.55 : 1.0);
            actionButton.setStyle(isDisabled ? disabledStyle : baseStyle);
        });

        return actionButton;
    }

    private <S, T> void enableSingleClickEditing(javafx.scene.control.cell.TextFieldTableCell<S, T> cell, boolean editable) {
        cell.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, event -> {
            if (!editable
                || cell.isEmpty()
                || cell.isEditing()
                || event.getButton() != javafx.scene.input.MouseButton.PRIMARY
                || event.getClickCount() != 1) {
                return;
            }

            javafx.scene.control.TableView<S> table = cell.getTableView();
            javafx.scene.control.TableColumn<S, T> column = cell.getTableColumn();
            if (table == null || column == null || !table.isEditable() || !column.isEditable()) {
                return;
            }

            table.getSelectionModel().select(cell.getIndex());
            table.getFocusModel().focus(cell.getIndex(), column);
            table.edit(cell.getIndex(), column);

            javafx.application.Platform.runLater(() -> {
                javafx.scene.Node editorNode = cell.getGraphic();
                if (!(editorNode instanceof TextField)) {
                    editorNode = cell.lookup(".text-field");
                }
                if (editorNode instanceof TextField textField) {
                    textField.requestFocus();
                    textField.selectAll();
                }
            });

            event.consume();
        });
    }

    private record ExpandableSearchControl(javafx.scene.layout.HBox box, TextField field) {
    }

    private record FilterPopupShell(
        VBox container,
        VBox content,
        javafx.scene.control.ScrollPane scrollPane
    ) {
    }

    private ExpandableSearchControl createExpandableSearchControl(double expandedWidth) {
        javafx.scene.layout.HBox searchBox = new javafx.scene.layout.HBox(0);
        searchBox.setAlignment(Pos.CENTER);
        searchBox.getStyleClass().add("expandable-search-box");
        searchBox.setPrefSize(40, 40);
        searchBox.setMinSize(40, 40);
        searchBox.setMaxSize(40, 40);

        javafx.scene.shape.SVGPath searchIcon = new javafx.scene.shape.SVGPath();
        searchIcon.setContent("M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
        searchIcon.setFill(PRIMARY_COLOR);

        javafx.scene.layout.Region searchSpacer = new javafx.scene.layout.Region();
        searchSpacer.setMinWidth(0);
        searchSpacer.setPrefWidth(0);

        TextField searchField = new TextField();
        searchField.setPromptText(DEFAULT_SEARCH_PROMPT);
        searchField.getStyleClass().add("search-text-field");
        searchField.setMinWidth(0);
        searchField.setMaxWidth(0);
        searchField.setPrefWidth(0);
        searchField.setOpacity(0);

        searchBox.getChildren().addAll(searchIcon, searchSpacer, searchField);

        double targetFieldWidth = Math.max(0, expandedWidth - 60);
        javafx.animation.Timeline searchExpand = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.millis(150),
                new javafx.animation.KeyValue(searchBox.maxWidthProperty(), expandedWidth, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(searchBox.prefWidthProperty(), expandedWidth, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(searchSpacer.minWidthProperty(), 8, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(searchField.minWidthProperty(), targetFieldWidth, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(searchField.maxWidthProperty(), targetFieldWidth, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(searchField.prefWidthProperty(), targetFieldWidth, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(searchField.opacityProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH)
            )
        );
        javafx.animation.Timeline searchCollapse = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.millis(150),
                new javafx.animation.KeyValue(searchBox.maxWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(searchBox.prefWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(searchSpacer.minWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(searchField.minWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(searchField.maxWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(searchField.prefWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(searchField.opacityProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH)
            )
        );

        searchBox.setOnMouseClicked(event -> {
            if (searchBox.getMaxWidth() == 40) {
                searchExpand.play();
                searchField.requestFocus();
            } else if (event.getTarget() == searchIcon || event.getTarget() == searchBox) {
                searchField.clear();
                if (searchBox.getParent() != null) {
                    searchBox.getParent().requestFocus();
                }
                searchCollapse.play();
            }
        });

        return new ExpandableSearchControl(searchBox, searchField);
    }

    private javafx.scene.Node createManageClipboardIcon() {
        javafx.scene.shape.Rectangle board = new javafx.scene.shape.Rectangle(-5.5, -4.5, 11, 14);
        board.setArcWidth(4);
        board.setArcHeight(4);
        board.setFill(javafx.scene.paint.Color.TRANSPARENT);
        board.setStroke(SURFACE_COLOR);
        board.setStrokeWidth(1.6);

        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(-3.0, -7.5, 6.0, 3.8);
        clip.setArcWidth(3.5);
        clip.setArcHeight(3.5);
        clip.setFill(javafx.scene.paint.Color.TRANSPARENT);
        clip.setStroke(SURFACE_COLOR);
        clip.setStrokeWidth(1.6);

        javafx.scene.shape.Line line1 = new javafx.scene.shape.Line(-2.8, -0.5, 2.8, -0.5);
        line1.setStroke(SURFACE_COLOR);
        line1.setStrokeWidth(1.5);
        line1.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

        javafx.scene.shape.Line line2 = new javafx.scene.shape.Line(-2.8, 2.8, 2.8, 2.8);
        line2.setStroke(SURFACE_COLOR);
        line2.setStrokeWidth(1.5);
        line2.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

        javafx.scene.shape.Line line3 = new javafx.scene.shape.Line(-1.8, 6.1, 1.8, 6.1);
        line3.setStroke(SURFACE_COLOR);
        line3.setStrokeWidth(1.5);
        line3.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

        javafx.scene.Group iconGroup = new javafx.scene.Group(board, clip, line1, line2, line3);
        javafx.scene.layout.StackPane iconPane = new javafx.scene.layout.StackPane(iconGroup);
        iconPane.setPrefSize(20, 20);
        iconPane.setMinSize(20, 20);
        iconPane.setMaxSize(20, 20);
        return iconPane;
    }

    private Button createHamburgerToggleButton() {
        Button toggleButton = new Button();
        toggleButton.getStyleClass().clear();
        toggleButton.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-border-color: transparent;");
        toggleButton.setPrefSize(34, 34);
        toggleButton.setMinSize(34, 34);
        toggleButton.setMaxSize(34, 34);
        toggleButton.setCursor(javafx.scene.Cursor.HAND);
        toggleButton.setFocusTraversable(false);
        installSidebarPressBounce(toggleButton);

        javafx.scene.paint.Color baseColor = PRIMARY_COLOR;
        javafx.scene.paint.Color hoverColor = PRIMARY_HOVER_COLOR;
        java.util.function.Function<String, javafx.scene.shape.SVGPath> pathFactory = content -> {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(content);
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            path.setStroke(baseColor);
            path.setStrokeWidth(1.5);
            path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            path.setSmooth(true);
            return path;
        };

        javafx.scene.shape.SVGPath panel = pathFactory.apply("M5 3H19A2 2 0 0 1 21 5V19A2 2 0 0 1 19 21H5A2 2 0 0 1 3 19V5A2 2 0 0 1 5 3Z");
        javafx.scene.shape.SVGPath divider = pathFactory.apply("M9 3V21");

        javafx.scene.layout.Pane iconPane = new javafx.scene.layout.Pane(panel, divider);
        iconPane.setPrefSize(24, 24);
        iconPane.setMinSize(24, 24);
        iconPane.setMaxSize(24, 24);
        iconPane.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(iconPane);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMinSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.setScaleX(1.16);
        iconWrap.setScaleY(1.16);
        iconWrap.setMouseTransparent(true);

        Runnable applyBaseColor = () -> {
            panel.setStroke(baseColor);
            divider.setStroke(baseColor);
        };
        Runnable applyHoverColor = () -> {
            panel.setStroke(hoverColor);
            divider.setStroke(hoverColor);
        };

        toggleButton.hoverProperty().addListener((obs, wasHovering, isHovering) -> {
            if (isHovering) {
                applyHoverColor.run();
            } else {
                applyBaseColor.run();
            }
        });

        toggleButton.setGraphic(iconWrap);
        toggleButton.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        toggleButton.setAlignment(Pos.CENTER);

        return toggleButton;
    }

    // Updated helper to accept ID
    private Button createNavButton(String text, String id, Runnable action) {
        Button btn = new Button(text);
        btn.setId(id); // Set ID for lookup
        btn.getStyleClass().setAll("nav-button");
        installSidebarPressBounce(btn);
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private void installSidebarNavIconMotion(Button button) {
        installSidebarIconMotion(button, button.getGraphic(), "active", 1.055, -1.0, 1.02, -0.35);
    }

    private void installSidebarIconMotion(
        javafx.scene.Node trigger,
        javafx.scene.Node icon,
        String activeStyleClass,
        double hoverScaleMultiplier,
        double hoverTranslateY,
        double activeScaleMultiplier,
        double activeTranslateY
    ) {
        if (trigger == null || icon == null) {
            return;
        }

        final String timelineKey = "sidebarIconMotionTimeline";
        final double baseScaleX = Math.abs(icon.getScaleX()) > 0.0001 ? icon.getScaleX() : 1.0;
        final double baseScaleY = Math.abs(icon.getScaleY()) > 0.0001 ? icon.getScaleY() : 1.0;

        Runnable refreshState = () -> {
            boolean hovered = trigger.isHover();
            boolean active = activeStyleClass != null && trigger.getStyleClass().contains(activeStyleClass);

            double targetScaleX = baseScaleX;
            double targetScaleY = baseScaleY;
            double targetTranslateY = 0.0;

            if (hovered) {
                targetScaleX = baseScaleX * hoverScaleMultiplier;
                targetScaleY = baseScaleY * hoverScaleMultiplier;
                targetTranslateY = hoverTranslateY;
            } else if (active) {
                targetScaleX = baseScaleX * activeScaleMultiplier;
                targetScaleY = baseScaleY * activeScaleMultiplier;
                targetTranslateY = activeTranslateY;
            }

            if (Math.abs(icon.getScaleX() - targetScaleX) < 0.001
                && Math.abs(icon.getScaleY() - targetScaleY) < 0.001
                && Math.abs(icon.getTranslateY() - targetTranslateY) < 0.001) {
                return;
            }

            javafx.animation.Animation existing = (javafx.animation.Animation) icon.getProperties().get(timelineKey);
            if (existing != null) {
                existing.stop();
            }

            if (isReducedMotionEnabled(trigger)) {
                icon.setScaleX(targetScaleX);
                icon.setScaleY(targetScaleY);
                icon.setTranslateY(targetTranslateY);
                icon.getProperties().remove(timelineKey);
                return;
            }

            javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                    Duration.millis(140),
                    new javafx.animation.KeyValue(icon.scaleXProperty(), targetScaleX, javafx.animation.Interpolator.EASE_BOTH),
                    new javafx.animation.KeyValue(icon.scaleYProperty(), targetScaleY, javafx.animation.Interpolator.EASE_BOTH),
                    new javafx.animation.KeyValue(icon.translateYProperty(), targetTranslateY, javafx.animation.Interpolator.EASE_BOTH)
                )
            );
            timeline.setOnFinished(e -> icon.getProperties().remove(timelineKey));
            icon.getProperties().put(timelineKey, timeline);
            timeline.play();
        };

        trigger.hoverProperty().addListener((obs, oldVal, newVal) -> refreshState.run());
        trigger.getStyleClass().addListener((javafx.collections.ListChangeListener<String>) change -> refreshState.run());
        refreshState.run();
    }

    private javafx.scene.Node createDashboardNavIcon() {
        java.util.function.Function<javafx.scene.shape.Rectangle, javafx.scene.shape.Rectangle> rectFactory = rect -> {
            rect.setFill(javafx.scene.paint.Color.TRANSPARENT);
            rect.setArcWidth(2);
            rect.setArcHeight(2);
            rect.setSmooth(true);
            rect.getStyleClass().add("sidebar-nav-icon-stroke");
            return rect;
        };

        javafx.scene.layout.Pane iconPane = new javafx.scene.layout.Pane(
            rectFactory.apply(new javafx.scene.shape.Rectangle(3, 3, 7, 9)),
            rectFactory.apply(new javafx.scene.shape.Rectangle(14, 3, 7, 5)),
            rectFactory.apply(new javafx.scene.shape.Rectangle(14, 12, 7, 9)),
            rectFactory.apply(new javafx.scene.shape.Rectangle(3, 16, 7, 5))
        );
        iconPane.getStyleClass().add("dashboard-nav-icon");
        iconPane.setMinSize(24, 24);
        iconPane.setPrefSize(24, 24);
        iconPane.setMaxSize(24, 24);
        iconPane.setScaleX(0.82);
        iconPane.setScaleY(0.82);
        iconPane.setMouseTransparent(true);
        return iconPane;
    }

    private javafx.scene.Node createSalesNavIcon() {
        java.util.function.Function<String, javafx.scene.shape.SVGPath> pathFactory = content -> {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(content);
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            path.setSmooth(true);
            path.getStyleClass().add("sidebar-nav-icon-stroke");
            return path;
        };

        javafx.scene.layout.Pane storeIcon = new javafx.scene.layout.Pane(
            pathFactory.apply("m2 7 4.41-4.41A2 2 0 0 1 7.83 2h8.34a2 2 0 0 1 1.42.59L22 7"),
            pathFactory.apply("M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8"),
            pathFactory.apply("M15 22v-4a2 2 0 0 0-2-2h-2a2 2 0 0 0-2 2v4"),
            pathFactory.apply("M2 7h20"),
            pathFactory.apply("M22 7v3a2 2 0 0 1-2 2a2.7 2.7 0 0 1-1.59-.63.7.7 0 0 0-.82 0A2.7 2.7 0 0 1 16 12a2.7 2.7 0 0 1-1.59-.63.7.7 0 0 0-.82 0A2.7 2.7 0 0 1 12 12a2.7 2.7 0 0 1-1.59-.63.7.7 0 0 0-.82 0A2.7 2.7 0 0 1 8 12a2.7 2.7 0 0 1-1.59-.63.7.7 0 0 0-.82 0A2.7 2.7 0 0 1 4 12a2 2 0 0 1-2-2V7")
        );
        storeIcon.setMinSize(24, 24);
        storeIcon.setPrefSize(24, 24);
        storeIcon.setMaxSize(24, 24);
        storeIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(storeIcon);
        iconWrap.setMinSize(24, 24);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.setScaleX(0.8);
        iconWrap.setScaleY(0.8);
        iconWrap.setMouseTransparent(true);
        return iconWrap;
    }

    private javafx.scene.Node createPromotionsNavIcon() {
        java.util.function.Function<String, javafx.scene.shape.SVGPath> pathFactory = content -> {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(content);
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            path.setSmooth(true);
            path.getStyleClass().add("sidebar-nav-icon-stroke");
            return path;
        };

        javafx.scene.layout.Pane promotionsIcon = new javafx.scene.layout.Pane(
            pathFactory.apply("M20.59 13.41 11 3.83a2 2 0 0 0-2.82 0L3.41 8.59a2 2 0 0 0 0 2.82l9.59 9.59a2 2 0 0 0 2.82 0l4.77-4.77a2 2 0 0 0 0-2.82Z"),
            pathFactory.apply("M7 7h.01"),
            pathFactory.apply("M10 14 14 10"),
            pathFactory.apply("M8.5 11.5 12.5 15.5")
        );
        promotionsIcon.setMinSize(24, 24);
        promotionsIcon.setPrefSize(24, 24);
        promotionsIcon.setMaxSize(24, 24);
        promotionsIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(promotionsIcon);
        iconWrap.setMinSize(24, 24);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.setScaleX(0.82);
        iconWrap.setScaleY(0.82);
        iconWrap.setMouseTransparent(true);
        return iconWrap;
    }

    private javafx.scene.Node createProductsNavIcon() {
        java.util.function.Function<String, javafx.scene.shape.SVGPath> pathFactory = content -> {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(content);
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            path.setSmooth(true);
            path.getStyleClass().add("sidebar-nav-icon-stroke");
            return path;
        };

        javafx.scene.layout.Pane packageIcon = new javafx.scene.layout.Pane(
            pathFactory.apply("M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z"),
            pathFactory.apply("M16.5 9.4 7.55 4.24"),
            pathFactory.apply("m3.3 7 8.7 5 8.7-5"),
            pathFactory.apply("M12 22V12")
        );
        packageIcon.setMinSize(24, 24);
        packageIcon.setPrefSize(24, 24);
        packageIcon.setMaxSize(24, 24);
        packageIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(packageIcon);
        iconWrap.setMinSize(24, 24);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.setScaleX(0.8);
        iconWrap.setScaleY(0.8);
        iconWrap.setMouseTransparent(true);
        return iconWrap;
    }

    private javafx.scene.Node createImportNavIcon() {
        java.util.function.Function<String, javafx.scene.shape.SVGPath> pathFactory = content -> {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(content);
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            path.setSmooth(true);
            path.getStyleClass().add("sidebar-nav-icon-stroke");
            return path;
        };

        javafx.scene.shape.Circle rearWheel = new javafx.scene.shape.Circle(7, 18, 2);
        rearWheel.setFill(javafx.scene.paint.Color.TRANSPARENT);
        rearWheel.setSmooth(true);
        rearWheel.getStyleClass().add("sidebar-nav-icon-stroke");

        javafx.scene.shape.Circle frontWheel = new javafx.scene.shape.Circle(17, 18, 2);
        frontWheel.setFill(javafx.scene.paint.Color.TRANSPARENT);
        frontWheel.setSmooth(true);
        frontWheel.getStyleClass().add("sidebar-nav-icon-stroke");

        javafx.scene.layout.Pane truckIcon = new javafx.scene.layout.Pane(
            pathFactory.apply("M14 18V6a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v11a1 1 0 0 0 1 1h2"),
            pathFactory.apply("M15 18H9"),
            pathFactory.apply("M19 18h2a1 1 0 0 0 1-1v-3.65a1 1 0 0 0-.22-.624l-3.48-4.35A1 1 0 0 0 17.52 8H14"),
            rearWheel,
            frontWheel
        );
        truckIcon.setMinSize(24, 24);
        truckIcon.setPrefSize(24, 24);
        truckIcon.setMaxSize(24, 24);
        truckIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(truckIcon);
        iconWrap.setMinSize(24, 24);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.setScaleX(0.8);
        iconWrap.setScaleY(0.8);
        iconWrap.setMouseTransparent(true);
        return iconWrap;
    }

    private javafx.scene.Node createOrderHistoryNavIcon() {
        java.util.function.Function<String, javafx.scene.shape.SVGPath> pathFactory = content -> {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(content);
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            path.setSmooth(true);
            path.getStyleClass().add("sidebar-nav-icon-stroke");
            return path;
        };

        javafx.scene.layout.Pane historyIcon = new javafx.scene.layout.Pane(
            pathFactory.apply("M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"),
            pathFactory.apply("M3 3v5h5"),
            pathFactory.apply("M12 7v5l4 2")
        );
        historyIcon.setMinSize(24, 24);
        historyIcon.setPrefSize(24, 24);
        historyIcon.setMaxSize(24, 24);
        historyIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(historyIcon);
        iconWrap.setMinSize(24, 24);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.setScaleX(0.8);
        iconWrap.setScaleY(0.8);
        iconWrap.setMouseTransparent(true);
        return iconWrap;
    }

    private javafx.scene.Node createReturnsNavIcon() {
        java.util.function.Function<String, javafx.scene.shape.SVGPath> pathFactory = content -> {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(content);
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            path.setSmooth(true);
            path.getStyleClass().add("sidebar-nav-icon-stroke");
            return path;
        };

        javafx.scene.layout.Pane returnsIcon = new javafx.scene.layout.Pane(
            pathFactory.apply("M4 6v5h5"),
            pathFactory.apply("m4 11 5-5 5 5"),
            pathFactory.apply("M20 18v-5h-5"),
            pathFactory.apply("m20 13-5 5-5-5")
        );
        returnsIcon.setMinSize(24, 24);
        returnsIcon.setPrefSize(24, 24);
        returnsIcon.setMaxSize(24, 24);
        returnsIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(returnsIcon);
        iconWrap.setMinSize(24, 24);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.setScaleX(0.82);
        iconWrap.setScaleY(0.82);
        iconWrap.setMouseTransparent(true);
        return iconWrap;
    }

    private javafx.scene.Node createExpensesNavIcon() {
        java.util.function.Function<String, javafx.scene.shape.SVGPath> pathFactory = content -> {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(content);
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            path.setSmooth(true);
            path.getStyleClass().add("sidebar-nav-icon-stroke");
            return path;
        };

        javafx.scene.layout.Pane expensesIcon = new javafx.scene.layout.Pane(
            pathFactory.apply("M6 3h9l4 4v14a1 1 0 0 1-1.4.91L15 20l-2 2-2-2-2 2-2-2-2 2A1 1 0 0 1 4 21V5a2 2 0 0 1 2-2"),
            pathFactory.apply("M9 9h5"),
            pathFactory.apply("M9 13h6"),
            pathFactory.apply("M9 17h4")
        );
        expensesIcon.setMinSize(24, 24);
        expensesIcon.setPrefSize(24, 24);
        expensesIcon.setMaxSize(24, 24);
        expensesIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(expensesIcon);
        iconWrap.setMinSize(24, 24);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.setScaleX(0.82);
        iconWrap.setScaleY(0.82);
        iconWrap.setMouseTransparent(true);
        return iconWrap;
    }

    private javafx.scene.Node createCustomersNavIcon() {
        java.util.function.Function<String, javafx.scene.shape.SVGPath> pathFactory = content -> {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(content);
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            path.setSmooth(true);
            path.getStyleClass().add("sidebar-nav-icon-stroke");
            return path;
        };

        javafx.scene.shape.Circle primaryHead = new javafx.scene.shape.Circle(9, 7, 4);
        primaryHead.setFill(javafx.scene.paint.Color.TRANSPARENT);
        primaryHead.setSmooth(true);
        primaryHead.getStyleClass().add("sidebar-nav-icon-stroke");

        javafx.scene.layout.Pane customersIcon = new javafx.scene.layout.Pane(
            primaryHead,
            pathFactory.apply("M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"),
            pathFactory.apply("M22 21v-2a4 4 0 0 0-3-3.87"),
            pathFactory.apply("M16 3.13a4 4 0 0 1 0 7.75")
        );
        customersIcon.setMinSize(24, 24);
        customersIcon.setPrefSize(24, 24);
        customersIcon.setMaxSize(24, 24);
        customersIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(customersIcon);
        iconWrap.setMinSize(24, 24);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.setScaleX(0.8);
        iconWrap.setScaleY(0.8);
        iconWrap.setMouseTransparent(true);
        return iconWrap;
    }

    private javafx.scene.Node createReportsNavIcon() {
        java.util.function.Function<String, javafx.scene.shape.SVGPath> pathFactory = content -> {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(content);
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            path.setSmooth(true);
            path.getStyleClass().add("sidebar-nav-icon-stroke");
            return path;
        };

        javafx.scene.layout.Pane reportsIcon = new javafx.scene.layout.Pane(
            pathFactory.apply("M3 3v16a2 2 0 0 0 2 2h16"),
            pathFactory.apply("M18 17V9"),
            pathFactory.apply("M13 17V5"),
            pathFactory.apply("M8 17v-3")
        );
        reportsIcon.setMinSize(24, 24);
        reportsIcon.setPrefSize(24, 24);
        reportsIcon.setMaxSize(24, 24);
        reportsIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(reportsIcon);
        iconWrap.setMinSize(24, 24);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.setScaleX(0.8);
        iconWrap.setScaleY(0.8);
        iconWrap.setMouseTransparent(true);
        return iconWrap;
    }

    private javafx.scene.Node createStocktakeNavIcon() {
        java.util.function.Function<String, javafx.scene.shape.SVGPath> pathFactory = content -> {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(content);
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            path.setSmooth(true);
            path.getStyleClass().add("sidebar-nav-icon-stroke");
            return path;
        };

        javafx.scene.layout.Pane stocktakeIcon = new javafx.scene.layout.Pane(
            pathFactory.apply("M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"),
            pathFactory.apply("M9 14l2 2 4-4")
        );

        javafx.scene.shape.Rectangle clipTop = new javafx.scene.shape.Rectangle(8, 2, 8, 4);
        clipTop.setArcWidth(2);
        clipTop.setArcHeight(2);
        clipTop.setFill(javafx.scene.paint.Color.TRANSPARENT);
        clipTop.setSmooth(true);
        clipTop.getStyleClass().add("sidebar-nav-icon-stroke");
        stocktakeIcon.getChildren().add(clipTop);

        stocktakeIcon.setMinSize(24, 24);
        stocktakeIcon.setPrefSize(24, 24);
        stocktakeIcon.setMaxSize(24, 24);
        stocktakeIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(stocktakeIcon);
        iconWrap.setMinSize(24, 24);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.setScaleX(0.8);
        iconWrap.setScaleY(0.8);
        iconWrap.setMouseTransparent(true);
        return iconWrap;
    }

    private javafx.scene.Node createLogoutNavIcon() {
        java.util.function.Consumer<javafx.scene.shape.Shape> applyDangerStroke = shape -> {
            shape.setFill(javafx.scene.paint.Color.TRANSPARENT);
            shape.setStroke(DANGER_COLOR);
            shape.setStrokeWidth(1.5);
            shape.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            shape.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            shape.setSmooth(true);
        };

        javafx.scene.shape.SVGPath doorPath = new javafx.scene.shape.SVGPath();
        doorPath.setContent("M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4");
        applyDangerStroke.accept(doorPath);

        javafx.scene.shape.Polyline arrow = new javafx.scene.shape.Polyline(16, 17, 21, 12, 16, 7);
        applyDangerStroke.accept(arrow);

        javafx.scene.shape.Line shaft = new javafx.scene.shape.Line(21, 12, 9, 12);
        applyDangerStroke.accept(shaft);

        javafx.scene.layout.Pane logoutIcon = new javafx.scene.layout.Pane(doorPath, arrow, shaft);
        logoutIcon.setMinSize(24, 24);
        logoutIcon.setPrefSize(24, 24);
        logoutIcon.setMaxSize(24, 24);
        logoutIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(logoutIcon);
        iconWrap.setMinSize(24, 24);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.setScaleX(0.8);
        iconWrap.setScaleY(0.8);
        iconWrap.setMouseTransparent(true);
        return iconWrap;
    }

    private javafx.scene.Node createAuditLogNavIcon() {
        java.util.function.Function<String, javafx.scene.shape.SVGPath> pathFactory = content -> {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(content);
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            path.setSmooth(true);
            path.getStyleClass().add("sidebar-nav-icon-stroke");
            return path;
        };

        javafx.scene.shape.Circle searchCircle = new javafx.scene.shape.Circle(5, 14, 3);
        searchCircle.setFill(javafx.scene.paint.Color.TRANSPARENT);
        searchCircle.setSmooth(true);
        searchCircle.getStyleClass().add("sidebar-nav-icon-stroke");

        javafx.scene.layout.Pane auditLogIcon = new javafx.scene.layout.Pane(
            pathFactory.apply("M14 2v4a2 2 0 0 0 2 2h4"),
            pathFactory.apply("M4.268 21a2 2 0 0 0 1.727 1H18a2 2 0 0 0 2-2V7l-5-5H6a2 2 0 0 0-2 2v3"),
            pathFactory.apply("m9 18-1.5-1.5"),
            searchCircle
        );
        auditLogIcon.setMinSize(24, 24);
        auditLogIcon.setPrefSize(24, 24);
        auditLogIcon.setMaxSize(24, 24);
        auditLogIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(auditLogIcon);
        iconWrap.setMinSize(24, 24);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.setScaleX(0.8);
        iconWrap.setScaleY(0.8);
        iconWrap.setMouseTransparent(true);
        return iconWrap;
    }

    private javafx.scene.Node createMasterDataNavIcon() {
        java.util.function.Function<String, javafx.scene.shape.SVGPath> pathFactory = content -> {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(content);
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            path.setSmooth(true);
            path.getStyleClass().add("sidebar-nav-icon-stroke");
            return path;
        };

        javafx.scene.shape.Ellipse topEllipse = new javafx.scene.shape.Ellipse(12, 5, 9, 3);
        topEllipse.setFill(javafx.scene.paint.Color.TRANSPARENT);
        topEllipse.setSmooth(true);
        topEllipse.getStyleClass().add("sidebar-nav-icon-stroke");

        javafx.scene.layout.Pane databaseIcon = new javafx.scene.layout.Pane(
            topEllipse,
            pathFactory.apply("M3 5V19A9 3 0 0 0 21 19V5"),
            pathFactory.apply("M3 12A9 3 0 0 0 21 12")
        );
        databaseIcon.setMinSize(24, 24);
        databaseIcon.setPrefSize(24, 24);
        databaseIcon.setMaxSize(24, 24);
        databaseIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(databaseIcon);
        iconWrap.setMinSize(24, 24);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.setScaleX(0.8);
        iconWrap.setScaleY(0.8);
        iconWrap.setMouseTransparent(true);
        return iconWrap;
    }

    private javafx.scene.Node createMyAccountHeaderIcon() {
        javafx.scene.shape.Circle outerCircle = new javafx.scene.shape.Circle(12, 12, 10);
        outerCircle.setFill(javafx.scene.paint.Color.TRANSPARENT);
        outerCircle.setSmooth(true);
        outerCircle.getStyleClass().add("header-account-icon-stroke");

        javafx.scene.shape.Circle headCircle = new javafx.scene.shape.Circle(12, 10, 3);
        headCircle.setFill(javafx.scene.paint.Color.TRANSPARENT);
        headCircle.setSmooth(true);
        headCircle.getStyleClass().add("header-account-icon-stroke");

        javafx.scene.shape.SVGPath shouldersPath = new javafx.scene.shape.SVGPath();
        shouldersPath.setContent("M7 20.662V19a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2v1.662");
        shouldersPath.setFill(javafx.scene.paint.Color.TRANSPARENT);
        shouldersPath.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        shouldersPath.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        shouldersPath.setSmooth(true);
        shouldersPath.getStyleClass().add("header-account-icon-stroke");

        javafx.scene.layout.Pane accountIcon = new javafx.scene.layout.Pane(outerCircle, headCircle, shouldersPath);
        accountIcon.setMinSize(24, 24);
        accountIcon.setPrefSize(24, 24);
        accountIcon.setMaxSize(24, 24);
        accountIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(accountIcon);
        iconWrap.setMinSize(24, 24);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.setScaleX(1.16);
        iconWrap.setScaleY(1.16);
        iconWrap.setMouseTransparent(true);
        return iconWrap;
    }

    private javafx.scene.Node createSettingsNavIcon() {
        java.util.function.Function<String, javafx.scene.shape.SVGPath> pathFactory = content -> {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(content);
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            path.setSmooth(true);
            path.getStyleClass().add("sidebar-nav-icon-stroke");
            return path;
        };

        javafx.scene.shape.Circle gearCore = new javafx.scene.shape.Circle(12, 12, 3);
        gearCore.setFill(javafx.scene.paint.Color.TRANSPARENT);
        gearCore.setSmooth(true);
        gearCore.getStyleClass().add("sidebar-nav-icon-stroke");

        javafx.scene.layout.Pane settingsIcon = new javafx.scene.layout.Pane(
            pathFactory.apply("M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09a1.65 1.65 0 0 0-1-1.51 1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09a1.65 1.65 0 0 0 1.51-1 1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33 1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82 1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1"),
            gearCore
        );
        settingsIcon.setMinSize(24, 24);
        settingsIcon.setPrefSize(24, 24);
        settingsIcon.setMaxSize(24, 24);
        settingsIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(settingsIcon);
        iconWrap.setMinSize(24, 24);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.setScaleX(0.8);
        iconWrap.setScaleY(0.8);
        iconWrap.setMouseTransparent(true);
        return iconWrap;
    }

    private javafx.scene.Node createAccountsNavIcon() {
        java.util.function.Function<String, javafx.scene.shape.SVGPath> pathFactory = content -> {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(content);
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            path.setSmooth(true);
            path.getStyleClass().add("sidebar-nav-icon-stroke");
            return path;
        };

        javafx.scene.shape.Circle gearCore = new javafx.scene.shape.Circle(18, 15, 3);
        gearCore.setFill(javafx.scene.paint.Color.TRANSPARENT);
        gearCore.setSmooth(true);
        gearCore.getStyleClass().add("sidebar-nav-icon-stroke");

        javafx.scene.shape.Circle userHead = new javafx.scene.shape.Circle(9, 7, 4);
        userHead.setFill(javafx.scene.paint.Color.TRANSPARENT);
        userHead.setSmooth(true);
        userHead.getStyleClass().add("sidebar-nav-icon-stroke");

        javafx.scene.layout.Pane accountsIcon = new javafx.scene.layout.Pane(
            gearCore,
            userHead,
            pathFactory.apply("M10 15H6a4 4 0 0 0-4 4v2"),
            pathFactory.apply("m21.7 16.4-.9-.3"),
            pathFactory.apply("m15.2 13.9-.9-.3"),
            pathFactory.apply("m16.6 18.7.3-.9"),
            pathFactory.apply("m19.1 12.2.3-.9"),
            pathFactory.apply("m19.6 18.7-.4-1"),
            pathFactory.apply("m16.8 12.3-.4-1"),
            pathFactory.apply("m14.3 16.6 1-.4"),
            pathFactory.apply("m20.7 13.8 1-.4")
        );
        accountsIcon.setMinSize(24, 24);
        accountsIcon.setPrefSize(24, 24);
        accountsIcon.setMaxSize(24, 24);
        accountsIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(accountsIcon);
        iconWrap.setMinSize(24, 24);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.setScaleX(0.8);
        iconWrap.setScaleY(0.8);
        iconWrap.setMouseTransparent(true);
        return iconWrap;
    }

    private void installSidebarPressBounce(javafx.scene.Node node) {
        final String timelineKey = "sidebarPressBounceTimeline";
        final double pressedOffset = 1.8;

        java.util.function.Consumer<javafx.animation.Animation> stopExisting = animation -> {
            if (animation != null) {
                animation.stop();
            }
        };

        java.util.function.Consumer<Double> animateTo = targetY -> {
            if (isReducedMotionEnabled(node)) {
                node.setTranslateY(0.0);
                return;
            }
            javafx.animation.Animation existing = (javafx.animation.Animation) node.getProperties().get(timelineKey);
            stopExisting.accept(existing);
            javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                    javafx.util.Duration.millis(55),
                    new javafx.animation.KeyValue(node.translateYProperty(), targetY, javafx.animation.Interpolator.EASE_BOTH)
                )
            );
            timeline.setOnFinished(e -> node.getProperties().remove(timelineKey));
            node.getProperties().put(timelineKey, timeline);
            timeline.play();
        };

        Runnable bounceBack = () -> {
            if (isReducedMotionEnabled(node)) {
                node.setTranslateY(0.0);
                return;
            }
            javafx.animation.Animation existing = (javafx.animation.Animation) node.getProperties().get(timelineKey);
            stopExisting.accept(existing);
            javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                    javafx.util.Duration.ZERO,
                    new javafx.animation.KeyValue(node.translateYProperty(), node.getTranslateY())
                ),
                new javafx.animation.KeyFrame(
                    javafx.util.Duration.millis(85),
                    new javafx.animation.KeyValue(node.translateYProperty(), -0.9, SPRING_BOUNCE)
                ),
                new javafx.animation.KeyFrame(
                    javafx.util.Duration.millis(165),
                    new javafx.animation.KeyValue(node.translateYProperty(), 0.0, javafx.animation.Interpolator.EASE_OUT)
                )
            );
            timeline.setOnFinished(e -> {
                node.setTranslateY(0);
                node.getProperties().remove(timelineKey);
            });
            node.getProperties().put(timelineKey, timeline);
            timeline.play();
        };

        node.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() != javafx.scene.input.MouseButton.PRIMARY || node.isDisable()) {
                return;
            }
            animateTo.accept(pressedOffset);
        });

        node.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_RELEASED, event -> {
            if (event.getButton() != javafx.scene.input.MouseButton.PRIMARY) {
                return;
            }
            bounceBack.run();
        });

        node.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, event -> {
            if (!event.isPrimaryButtonDown()) {
                return;
            }
            javafx.geometry.Bounds bounds = node.localToScreen(node.getBoundsInLocal());
            if (bounds == null || !bounds.contains(event.getScreenX(), event.getScreenY())) {
                animateTo.accept(0.0);
            } else if (Math.abs(node.getTranslateY() - pressedOffset) > 0.01) {
                animateTo.accept(pressedOffset);
            }
        });

        node.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_EXITED, event -> {
            if (!event.isPrimaryButtonDown() && Math.abs(node.getTranslateY()) > 0.01) {
                bounceBack.run();
            }
        });
    }

    @Override
    public void onApplicationEvent(StageReadyEvent event) {
        Stage stage = event.getStage();
        
        // Explicitly load fonts to ensure they are available
        try {
            javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/BeVietnamPro-Regular.ttf"), 12);
            javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/BeVietnamPro-Bold.ttf"), 12);
        } catch (Exception e) {
            System.err.println("Could not load fonts: " + e.getMessage());
        }

        // Load CSS globally
        stage.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                applyApplicationStyles(newScene);
            }
        });
        
        showLoginScene(stage);
        stage.setTitle("Sales Management System");
        stage.show();
    }

    private void showLoginScene(Stage stage) {
        // UI Layout
        javafx.scene.layout.StackPane mainRoot = new javafx.scene.layout.StackPane();
        mainRoot.setStyle("-fx-background-color: -app-surface-muted;");

        VBox loginBox = new VBox(15);
        loginBox.getStyleClass().add("login-box");
        loginBox.setAlignment(Pos.CENTER); // Center all children
        loginBox.setPadding(new Insets(30));
        loginBox.setMaxWidth(400);

        // Title
        Label titleLabel = new Label("SYSTEM LOGIN");
        titleLabel.getStyleClass().add("title-label");
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setStyle("-fx-alignment: center; -fx-padding: 10 0 5 0;"); // Reduced padding

        // Error Label
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: -app-danger; -fx-font-weight: bold;");
        errorLabel.setAlignment(Pos.CENTER);
        errorLabel.setMaxWidth(Double.MAX_VALUE);

        // --- Floating label input fields ---
        // Username
        TextField usernameField = new TextField();
        usernameField.getStyleClass().add("text-field");
        usernameField.setMaxWidth(350);
        usernameField.setPrefHeight(44);
        usernameField.setStyle("-fx-border-color: -app-border; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 14; -fx-font-size: 14px; -fx-background-color: transparent; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        Label usernameLabel = new Label("Username");
        usernameLabel.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 14px; -fx-background-color: -app-surface-muted; -fx-background-radius: 20; -fx-padding: 0 4 0 4;");
        usernameLabel.setMouseTransparent(true);

        javafx.scene.layout.StackPane usernamePane = new javafx.scene.layout.StackPane(usernameField, usernameLabel);
        usernamePane.setMaxWidth(350);
        javafx.scene.layout.StackPane.setAlignment(usernameLabel, Pos.CENTER_LEFT);
        usernameLabel.setTranslateX(12);

        javafx.animation.Timeline userAnimUp = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.millis(200),
                new javafx.animation.KeyValue(usernameLabel.translateYProperty(), -22, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(usernameLabel.translateXProperty(), 8, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(usernameLabel.scaleXProperty(), 0.85, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(usernameLabel.scaleYProperty(), 0.85, javafx.animation.Interpolator.EASE_BOTH)));
        javafx.animation.Timeline userAnimDown = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.millis(200),
                new javafx.animation.KeyValue(usernameLabel.translateYProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(usernameLabel.translateXProperty(), 12, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(usernameLabel.scaleXProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(usernameLabel.scaleYProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH)));

        Runnable updateUsernameState = () -> {
            boolean focused = usernameField.isFocused();
            boolean hasText = !usernameField.getText().isEmpty();
            if (focused) {
                userAnimDown.stop(); userAnimUp.play();
                usernameLabel.setStyle("-fx-text-fill: -app-primary; -fx-font-size: 14px; -fx-background-color: -app-surface-muted; -fx-background-radius: 20; -fx-padding: 0 4 0 4;");
                usernameField.setStyle("-fx-border-color: -app-primary; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 14; -fx-font-size: 14px; -fx-background-color: transparent; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
            } else if (hasText) {
                userAnimDown.stop(); userAnimUp.play();
                usernameLabel.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 14px; -fx-background-color: -app-surface-muted; -fx-background-radius: 20; -fx-padding: 0 4 0 4;");
                usernameField.setStyle("-fx-border-color: -app-border; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 14; -fx-font-size: 14px; -fx-background-color: transparent; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
            } else {
                userAnimUp.stop(); userAnimDown.play();
                usernameLabel.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 14px; -fx-background-color: transparent; -fx-padding: 0 4 0 4;");
                usernameField.setStyle("-fx-border-color: -app-border; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 14; -fx-font-size: 14px; -fx-background-color: transparent; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
            }
        };
        usernameField.focusedProperty().addListener((obs, old, focused) -> updateUsernameState.run());
        usernameField.textProperty().addListener((obs, old, val) -> updateUsernameState.run());

        // Password
        PasswordField passwordField = new PasswordField();
        passwordField.getStyleClass().add("text-field");
        passwordField.setMaxWidth(350);
        passwordField.setPrefHeight(44);
        passwordField.setStyle("-fx-border-color: -app-border; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 14; -fx-font-size: 14px; -fx-background-color: transparent; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        Label passwordLabel = new Label("Password");
        passwordLabel.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 14px; -fx-background-color: -app-surface-muted; -fx-background-radius: 20; -fx-padding: 0 4 0 4;");
        passwordLabel.setMouseTransparent(true);

        javafx.scene.layout.StackPane passwordPane = new javafx.scene.layout.StackPane(passwordField, passwordLabel);
        passwordPane.setMaxWidth(350);
        javafx.scene.layout.StackPane.setAlignment(passwordLabel, Pos.CENTER_LEFT);
        passwordLabel.setTranslateX(12);

        javafx.animation.Timeline passAnimUp = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.millis(200),
                new javafx.animation.KeyValue(passwordLabel.translateYProperty(), -22, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(passwordLabel.translateXProperty(), 8, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(passwordLabel.scaleXProperty(), 0.85, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(passwordLabel.scaleYProperty(), 0.85, javafx.animation.Interpolator.EASE_BOTH)));
        javafx.animation.Timeline passAnimDown = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.millis(200),
                new javafx.animation.KeyValue(passwordLabel.translateYProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(passwordLabel.translateXProperty(), 12, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(passwordLabel.scaleXProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(passwordLabel.scaleYProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH)));

        Runnable updatePasswordState = () -> {
            boolean focused = passwordField.isFocused();
            boolean hasText = !passwordField.getText().isEmpty();
            if (focused) {
                passAnimDown.stop(); passAnimUp.play();
                passwordLabel.setStyle("-fx-text-fill: -app-primary; -fx-font-size: 14px; -fx-background-color: -app-surface-muted; -fx-background-radius: 20; -fx-padding: 0 4 0 4;");
                passwordField.setStyle("-fx-border-color: -app-primary; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 14; -fx-font-size: 14px; -fx-background-color: transparent; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
            } else if (hasText) {
                passAnimDown.stop(); passAnimUp.play();
                passwordLabel.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 14px; -fx-background-color: -app-surface-muted; -fx-background-radius: 20; -fx-padding: 0 4 0 4;");
                passwordField.setStyle("-fx-border-color: -app-border; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 14; -fx-font-size: 14px; -fx-background-color: transparent; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
            } else {
                passAnimUp.stop(); passAnimDown.play();
                passwordLabel.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 14px; -fx-background-color: transparent; -fx-padding: 0 4 0 4;");
                passwordField.setStyle("-fx-border-color: -app-border; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 14; -fx-font-size: 14px; -fx-background-color: transparent; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
            }
        };
        passwordField.focusedProperty().addListener((obs, old, focused) -> updatePasswordState.run());
        passwordField.textProperty().addListener((obs, old, val) -> updatePasswordState.run());

        // Login Button
        Button loginButton = new Button("LOGIN");
        loginButton.getStyleClass().addAll("button", "primary-button");
        loginButton.setMaxWidth(350);
        loginButton.setDefaultButton(true);
        loginButton.setCursor(javafx.scene.Cursor.HAND);
        
        // Button Click Animation
        loginButton.setOnMousePressed(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), loginButton);
            st.setToX(0.95); st.setToY(0.95);
            st.play();
        });
        loginButton.setOnMouseReleased(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), loginButton);
            st.setToX(1.0); st.setToY(1.0);
            st.play();
        });

        loginButton.setOnAction(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();
            var user = authService.login(username, password);
            
            if (user != null) {
                showPostLoginLoadingScene(stage, user);
            } else {
                errorLabel.setText("Invalid credentials!");
                
                // Shake Animation
                TranslateTransition shake = new TranslateTransition(Duration.millis(50), loginBox);
                shake.setByX(10f);
                shake.setCycleCount(6);
                shake.setAutoReverse(true);
                shake.playFromStart();
            }
        });

        loginBox.getChildren().addAll(titleLabel, errorLabel, usernamePane, passwordPane, loginButton);
        mainRoot.getChildren().add(loginBox);

        Scene scene = new Scene(mainRoot, 450, 350); // Wider window
        applyApplicationStyles(scene);
        stage.setScene(scene);
        stage.centerOnScreen();
    }

    private void showExpenseDialog(
        Stage owner,
        com.pbl3.project.pbl3_project.entity.User user,
        com.pbl3.project.pbl3_project.entity.Expense expense,
        Runnable onSave
    ) {
        if (!ensureAuthorized(() -> authorizationService.requireExpenseWrite(user))) {
            return;
        }
        try {
            boolean editing = expense != null && expense.getId() != null;
            Stage dialog = new Stage();
            dialog.initOwner(owner);
            dialog.setTitle(editing ? "Edit Expense" : "New Expense");
            dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);

            VBox root = new VBox(16);
            root.getStyleClass().add("dialog-root");
            root.setPadding(new Insets(20));

            Label titleLabel = new Label(editing ? "Edit Expense" : "Create New Expense");
            titleLabel.getStyleClass().add("dialog-title");

            javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
            grid.setHgap(14);
            grid.setVgap(14);
            javafx.scene.layout.ColumnConstraints labelColumn = new javafx.scene.layout.ColumnConstraints();
            labelColumn.setMinWidth(120);
            javafx.scene.layout.ColumnConstraints fieldColumn = new javafx.scene.layout.ColumnConstraints();
            fieldColumn.setHgrow(javafx.scene.layout.Priority.ALWAYS);
            grid.getColumnConstraints().addAll(labelColumn, fieldColumn);

            javafx.scene.control.DatePicker spentOnPicker = new javafx.scene.control.DatePicker(
                expense != null && expense.getSpentOn() != null ? expense.getSpentOn() : java.time.LocalDate.now()
            );
            spentOnPicker.setPrefWidth(220);
            customizeDatePicker(spentOnPicker);

            javafx.scene.control.ComboBox<com.pbl3.project.pbl3_project.entity.ExpenseCategory> categoryCombo = new javafx.scene.control.ComboBox<>();
            categoryCombo.getItems().addAll(com.pbl3.project.pbl3_project.entity.ExpenseCategory.values());
            categoryCombo.setValue(expense != null ? expense.getCategory() : null);
            categoryCombo.setPrefWidth(220);
            categoryCombo.setConverter(new javafx.util.StringConverter<>() {
                @Override
                public String toString(com.pbl3.project.pbl3_project.entity.ExpenseCategory value) {
                    return formatExpenseCategoryLabel(value);
                }

                @Override
                public com.pbl3.project.pbl3_project.entity.ExpenseCategory fromString(String string) {
                    return null;
                }
            });

            TextField titleField = new TextField(expense != null && expense.getTitle() != null ? expense.getTitle() : "");
            titleField.setPromptText("Expense title");

            TextField amountField = new TextField(
                expense != null && expense.getAmount() != null ? expense.getAmount().toPlainString() : ""
            );
            amountField.setPromptText("0.00");
            amountField.setTextFormatter(new javafx.scene.control.TextFormatter<>(change ->
                change.getControlNewText().matches("\\d{0,12}(\\.\\d{0,2})?") ? change : null
            ));

            javafx.scene.control.ComboBox<com.pbl3.project.pbl3_project.entity.PaymentMethod> methodCombo = new javafx.scene.control.ComboBox<>();
            methodCombo.getItems().addAll(com.pbl3.project.pbl3_project.entity.PaymentMethod.values());
            methodCombo.setValue(
                expense != null && expense.getPaymentMethod() != null
                    ? expense.getPaymentMethod()
                    : com.pbl3.project.pbl3_project.entity.PaymentMethod.CASH
            );
            methodCombo.setPrefWidth(220);
            methodCombo.setConverter(new javafx.util.StringConverter<>() {
                @Override
                public String toString(com.pbl3.project.pbl3_project.entity.PaymentMethod value) {
                    return formatPaymentMethodLabel(value);
                }

                @Override
                public com.pbl3.project.pbl3_project.entity.PaymentMethod fromString(String string) {
                    return null;
                }
            });

            javafx.scene.control.TextArea noteArea = new javafx.scene.control.TextArea(
                expense != null && expense.getNote() != null ? expense.getNote() : ""
            );
            noteArea.setPromptText("Optional note");
            noteArea.setWrapText(true);
            noteArea.setPrefRowCount(4);

            grid.add(createFormLabel("Spent On *"), 0, 0);
            grid.add(spentOnPicker, 1, 0);
            grid.add(createFormLabel("Category *"), 0, 1);
            grid.add(categoryCombo, 1, 1);
            grid.add(createFormLabel("Title *"), 0, 2);
            grid.add(titleField, 1, 2);
            grid.add(createFormLabel("Amount *"), 0, 3);
            grid.add(amountField, 1, 3);
            grid.add(createFormLabel("Payment Method"), 0, 4);
            grid.add(methodCombo, 1, 4);
            grid.add(createFormLabel("Note"), 0, 5);
            grid.add(noteArea, 1, 5);

            Button cancelBtn = new Button("Cancel");
            cancelBtn.setOnAction(e -> dialog.close());

            Button saveBtn = new Button(editing ? "Save" : "Create");
            saveBtn.getStyleClass().addAll("button", "primary-button");
            saveBtn.setDefaultButton(true);
            saveBtn.setOnAction(e -> {
                try {
                    BigDecimal parsedAmount = parseMoneyInput(amountField.getText(), "Expense amount");
                    if (editing) {
                        expenseService.updateExpense(
                            user,
                            expense.getId(),
                            spentOnPicker.getValue(),
                            categoryCombo.getValue(),
                            titleField.getText(),
                            parsedAmount,
                            methodCombo.getValue(),
                            noteArea.getText()
                        );
                        toastService.showSuccess("Expense updated");
                    } else {
                        expenseService.createExpense(
                            user,
                            spentOnPicker.getValue(),
                            categoryCombo.getValue(),
                            titleField.getText(),
                            parsedAmount,
                            methodCombo.getValue(),
                            noteArea.getText()
                        );
                        toastService.showSuccess("Expense created");
                    }
                    if (onSave != null) {
                        onSave.run();
                    }
                    dialog.close();
                } catch (Exception ex) {
                    showUserFacingError(ex);
                }
            });

            javafx.scene.layout.HBox actionRow = new javafx.scene.layout.HBox(10, cancelBtn, saveBtn);
            actionRow.setAlignment(Pos.CENTER_RIGHT);

            root.getChildren().addAll(titleLabel, grid, actionRow);

            Scene scene = new Scene(root, 520, 420);
            applyApplicationStyles(scene);
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (Exception ex) {
            showUserFacingError(ex);
        }
    }

    private void showPromotionDialog(
        Stage owner,
        com.pbl3.project.pbl3_project.entity.User user,
        com.pbl3.project.pbl3_project.entity.Promotion promotion,
        Runnable onSave
    ) {
        if (!ensureAuthorized(() -> authorizationService.requirePromotionWrite(user))) {
            return;
        }
        try {
            boolean editing = promotion != null && promotion.getId() != null;
            Stage dialog = new Stage();
            dialog.initOwner(owner);
            dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialog.setTitle(editing ? "Edit Promotion" : "New Promotion");

            VBox root = new VBox(16);
            root.getStyleClass().add("dialog-root");
            root.setPadding(new Insets(20));

            Label titleLabel = new Label(editing ? "Edit Promotion" : "Create New Promotion");
            titleLabel.getStyleClass().add("dialog-title");

            javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
            grid.setHgap(14);
            grid.setVgap(14);
            javafx.scene.layout.ColumnConstraints labelColumn = new javafx.scene.layout.ColumnConstraints();
            labelColumn.setMinWidth(140);
            javafx.scene.layout.ColumnConstraints fieldColumn = new javafx.scene.layout.ColumnConstraints();
            fieldColumn.setHgrow(javafx.scene.layout.Priority.ALWAYS);
            grid.getColumnConstraints().addAll(labelColumn, fieldColumn);

            TextField nameField = new TextField(promotion != null && promotion.getName() != null ? promotion.getName() : "");
            nameField.setPromptText("Promotion name");

            javafx.scene.control.ComboBox<com.pbl3.project.pbl3_project.entity.PromotionScope> scopeCombo = new javafx.scene.control.ComboBox<>();
            scopeCombo.getItems().addAll(com.pbl3.project.pbl3_project.entity.PromotionScope.values());
            scopeCombo.setValue(promotion != null && promotion.getScope() != null
                ? promotion.getScope()
                : com.pbl3.project.pbl3_project.entity.PromotionScope.PRODUCT);
            scopeCombo.setPrefWidth(240);
            scopeCombo.setConverter(new javafx.util.StringConverter<>() {
                @Override
                public String toString(com.pbl3.project.pbl3_project.entity.PromotionScope value) {
                    return formatPromotionScopeLabel(value);
                }

                @Override
                public com.pbl3.project.pbl3_project.entity.PromotionScope fromString(String string) {
                    return null;
                }
            });

            javafx.scene.control.ComboBox<com.pbl3.project.pbl3_project.entity.PromotionDiscountType> discountTypeCombo = new javafx.scene.control.ComboBox<>();
            discountTypeCombo.getItems().addAll(com.pbl3.project.pbl3_project.entity.PromotionDiscountType.values());
            discountTypeCombo.setValue(promotion != null && promotion.getDiscountType() != null
                ? promotion.getDiscountType()
                : com.pbl3.project.pbl3_project.entity.PromotionDiscountType.PERCENT);
            discountTypeCombo.setPrefWidth(240);
            discountTypeCombo.setConverter(new javafx.util.StringConverter<>() {
                @Override
                public String toString(com.pbl3.project.pbl3_project.entity.PromotionDiscountType value) {
                    return formatPromotionDiscountTypeLabel(value);
                }

                @Override
                public com.pbl3.project.pbl3_project.entity.PromotionDiscountType fromString(String string) {
                    return null;
                }
            });

            TextField discountValueField = new TextField(
                promotion != null && promotion.getDiscountValue() != null ? promotion.getDiscountValue().toPlainString() : ""
            );
            discountValueField.setPromptText("0.00");
            discountValueField.setTextFormatter(new javafx.scene.control.TextFormatter<>(change ->
                change.getControlNewText().matches("\\d{0,12}(\\.\\d{0,2})?") ? change : null
            ));

            javafx.scene.control.CheckBox enabledCb = new javafx.scene.control.CheckBox("Enabled");
            enabledCb.setSelected(promotion == null || promotion.isEnabled());

            javafx.scene.control.DatePicker startsAtPicker = new javafx.scene.control.DatePicker(
                promotion != null && promotion.getStartsAt() != null ? promotion.getStartsAt().toLocalDate() : null
            );
            startsAtPicker.setPromptText("Start Date");
            customizeDatePicker(startsAtPicker);

            javafx.scene.control.DatePicker endsAtPicker = new javafx.scene.control.DatePicker(
                promotion != null && promotion.getEndsAt() != null ? promotion.getEndsAt().toLocalDate() : null
            );
            endsAtPicker.setPromptText("End Date");
            customizeDatePicker(endsAtPicker);

            java.util.List<com.pbl3.project.pbl3_project.entity.Product> promotionProducts = productService.getAllProducts().stream()
                .filter(product -> product != null && !product.isDeleted())
                .sorted(java.util.Comparator.comparing(com.pbl3.project.pbl3_project.entity.Product::getName, java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
            javafx.scene.control.ComboBox<com.pbl3.project.pbl3_project.entity.Product> targetProductCombo = new javafx.scene.control.ComboBox<>();
            targetProductCombo.setItems(javafx.collections.FXCollections.observableArrayList(promotionProducts));
            targetProductCombo.setValue(promotion != null ? promotion.getTargetProduct() : null);
            targetProductCombo.setPrefWidth(280);
            targetProductCombo.setConverter(new javafx.util.StringConverter<>() {
                @Override
                public String toString(com.pbl3.project.pbl3_project.entity.Product value) {
                    if (value == null) {
                        return "";
                    }
                    return value.getName() + (value.getSku() != null && !value.getSku().isBlank() ? " • " + value.getSku() : "");
                }

                @Override
                public com.pbl3.project.pbl3_project.entity.Product fromString(String string) {
                    return null;
                }
            });

            TextField minOrderTotalField = new TextField(
                promotion != null && promotion.getMinOrderTotal() != null ? promotion.getMinOrderTotal().toPlainString() : ""
            );
            minOrderTotalField.setPromptText("Optional minimum order total");
            minOrderTotalField.setTextFormatter(new javafx.scene.control.TextFormatter<>(change ->
                change.getControlNewText().matches("\\d{0,12}(\\.\\d{0,2})?") ? change : null
            ));

            Label targetProductLabel = createFormLabel("Target Product *");
            Label minOrderTotalLabel = createFormLabel("Min Order Total");

            grid.add(createFormLabel("Name *"), 0, 0);
            grid.add(nameField, 1, 0);
            grid.add(createFormLabel("Scope *"), 0, 1);
            grid.add(scopeCombo, 1, 1);
            grid.add(createFormLabel("Discount Type *"), 0, 2);
            grid.add(discountTypeCombo, 1, 2);
            grid.add(createFormLabel("Discount Value *"), 0, 3);
            grid.add(discountValueField, 1, 3);
            grid.add(createFormLabel("Schedule"), 0, 4);
            grid.add(new javafx.scene.layout.HBox(10, startsAtPicker, endsAtPicker), 1, 4);
            grid.add(targetProductLabel, 0, 5);
            grid.add(targetProductCombo, 1, 5);
            grid.add(minOrderTotalLabel, 0, 6);
            grid.add(minOrderTotalField, 1, 6);
            grid.add(createFormLabel("State"), 0, 7);
            grid.add(enabledCb, 1, 7);

            Runnable updateScopeVisibility = () -> {
                boolean productScope = scopeCombo.getValue() == com.pbl3.project.pbl3_project.entity.PromotionScope.PRODUCT;
                targetProductLabel.setManaged(productScope);
                targetProductLabel.setVisible(productScope);
                targetProductCombo.setManaged(productScope);
                targetProductCombo.setVisible(productScope);

                minOrderTotalLabel.setManaged(!productScope);
                minOrderTotalLabel.setVisible(!productScope);
                minOrderTotalField.setManaged(!productScope);
                minOrderTotalField.setVisible(!productScope);
            };
            scopeCombo.valueProperty().addListener((obs, oldValue, newValue) -> updateScopeVisibility.run());
            updateScopeVisibility.run();

            Button cancelBtn = new Button("Cancel");
            cancelBtn.setOnAction(e -> dialog.close());

            Button saveBtn = new Button(editing ? "Save" : "Create");
            saveBtn.getStyleClass().addAll("button", "primary-button");
            saveBtn.setDefaultButton(true);
            saveBtn.setOnAction(e -> {
                try {
                    BigDecimal discountValue = parseMoneyInput(discountValueField.getText(), "Discount value");
                    BigDecimal minOrderTotal = null;
                    if (!minOrderTotalField.getText().trim().isBlank()) {
                        minOrderTotal = parseMoneyInput(minOrderTotalField.getText(), "Minimum order total");
                    }
                    java.time.LocalDateTime startsAt = startsAtPicker.getValue() != null
                        ? startsAtPicker.getValue().atStartOfDay()
                        : null;
                    java.time.LocalDateTime endsAt = endsAtPicker.getValue() != null
                        ? endsAtPicker.getValue().atTime(23, 59, 59)
                        : null;
                    Long targetProductId = targetProductCombo.getValue() != null ? targetProductCombo.getValue().getId() : null;

                    if (editing) {
                        promotionService.updatePromotion(
                            user,
                            promotion.getId(),
                            nameField.getText(),
                            scopeCombo.getValue(),
                            discountTypeCombo.getValue(),
                            discountValue,
                            enabledCb.isSelected(),
                            startsAt,
                            endsAt,
                            targetProductId,
                            minOrderTotal
                        );
                        toastService.showSuccess("Promotion updated");
                    } else {
                        promotionService.createPromotion(
                            user,
                            nameField.getText(),
                            scopeCombo.getValue(),
                            discountTypeCombo.getValue(),
                            discountValue,
                            enabledCb.isSelected(),
                            startsAt,
                            endsAt,
                            targetProductId,
                            minOrderTotal
                        );
                        toastService.showSuccess("Promotion created");
                    }
                    if (onSave != null) {
                        onSave.run();
                    }
                    dialog.close();
                } catch (Exception ex) {
                    showUserFacingError(ex);
                }
            });

            javafx.scene.layout.HBox actionRow = new javafx.scene.layout.HBox(10, cancelBtn, saveBtn);
            actionRow.setAlignment(Pos.CENTER_RIGHT);

            root.getChildren().addAll(titleLabel, grid, actionRow);

            Scene scene = new Scene(root, 620, 470);
            applyApplicationStyles(scene);
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (Exception ex) {
            showUserFacingError(ex);
        }
    }

    private BigDecimal parseMoneyInput(String raw, String fieldLabel) {
        if (raw == null || raw.trim().isBlank()) {
            throw new ValidationException(fieldLabel + " is required");
        }
        String normalized = raw.trim().replace(",", "");
        try {
            return MoneySupport.normalize(new BigDecimal(normalized));
        } catch (NumberFormatException ex) {
            throw new ValidationException(fieldLabel + " must be a valid number");
        }
    }

    private void showProductDialog(Stage owner, com.pbl3.project.pbl3_project.entity.Product product, com.pbl3.project.pbl3_project.entity.Category contextCategory, com.pbl3.project.pbl3_project.entity.User user, Runnable onSave) {
        try {
            Stage dialog = new Stage();
            dialog.initOwner(owner);
            dialog.setTitle(product == null ? "Add New Product" : "Edit Product");
            dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);

            VBox root = new VBox(15);
            root.getStyleClass().add("dialog-root");
            root.setPadding(new Insets(20));

            Label titleLabel = new Label(product == null ? "Create New Product" : "Edit Product Details");
            titleLabel.getStyleClass().add("dialog-title");

            // --- Form Layout (GridPane) ---
            javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
            grid.setHgap(15); 
            grid.setVgap(15);
            
            // Row 0: Basic Info
            TextField nameField = createStyledTextField(product != null ? product.getName() : "", "Product Name");
            TextField skuField = createStyledTextField(product != null ? product.getSku() : "", "SKU (Unique)");
            
            grid.add(createFormLabel("Product Name *"), 0, 0); grid.add(nameField, 1, 0);
            grid.add(createFormLabel("SKU *"), 2, 0); grid.add(skuField, 3, 0);

            // Row 1: Barcode & Category (Implicit)
            TextField barcodeField = createStyledTextField(product != null ? product.getBarcode() : "", "Barcode (Scan)");
            
            // NOTE: Category input removed primarily because it is inferred from context
            // But we should show it as read-only label
            Label catLabel = new Label(contextCategory != null ? contextCategory.getName() : (product != null && product.getCategory() != null ? product.getCategory().getName() : "Unknown"));
            catLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -app-primary;");
            
            grid.add(createFormLabel("Category"), 0, 1); grid.add(catLabel, 1, 1);
            grid.add(createFormLabel("Barcode"), 2, 1); grid.add(barcodeField, 3, 1);

            // Row 2: Pricing & Stock
            TextField importPriceField = createStyledTextField(product != null ? formatWholeNumberText(product.getImportPrice()) : "", "Import Price");
            TextField priceField = createStyledTextField(product != null ? String.valueOf(product.getPrice()) : "", "Selling Price *");
            TextField qtyField = createStyledTextField(product != null ? String.valueOf(product.getQuantity()) : "0", "Quantity *");

            // Disable import price editing for existing products to enforce "Import Goods" workflow
            if (product != null) {
                importPriceField.setDisable(true);
            }

            grid.add(createFormLabel("Import Price"), 0, 2); grid.add(importPriceField, 1, 2);
            grid.add(createFormLabel("Selling Price *"), 2, 2); grid.add(priceField, 3, 2);
            grid.add(createFormLabel("Quantity *"), 0, 3); grid.add(qtyField, 1, 3);

            // Row 3: Master Data (Brand, Origin)
            javafx.scene.control.ComboBox<com.pbl3.project.pbl3_project.entity.Brand> brandCombo = new javafx.scene.control.ComboBox<>();
            brandCombo.setMaxWidth(Double.MAX_VALUE); brandCombo.setPromptText("Select Brand");
            brandCombo.setItems(javafx.collections.FXCollections.observableArrayList(brandService.getAllBrands()));
            setComboConverter(brandCombo);
            if (product != null) brandCombo.setValue(product.getBrand());

            javafx.scene.control.ComboBox<com.pbl3.project.pbl3_project.entity.Origin> originCombo = new javafx.scene.control.ComboBox<>();
            originCombo.setMaxWidth(Double.MAX_VALUE); originCombo.setPromptText("Select Origin");
            originCombo.setItems(javafx.collections.FXCollections.observableArrayList(originService.getAllOrigins()));
            setComboConverter(originCombo);
            if (product != null) originCombo.setValue(product.getOrigin());
            
            grid.add(createFormLabel("Brand"), 2, 3); grid.add(brandCombo, 3, 3);
            grid.add(createFormLabel("Origin"), 0, 4); grid.add(originCombo, 1, 4);

            // Row 4: Master Data (Unit, Description)
            javafx.scene.control.ComboBox<com.pbl3.project.pbl3_project.entity.Unit> unitCombo = new javafx.scene.control.ComboBox<>();
            unitCombo.setMaxWidth(Double.MAX_VALUE); unitCombo.setPromptText("Select Unit");
            unitCombo.setItems(javafx.collections.FXCollections.observableArrayList(unitService.getAllUnits()));
            setComboConverter(unitCombo);
            if (product != null) unitCombo.setValue(product.getUnit());

            grid.add(createFormLabel("Unit"), 2, 4); grid.add(unitCombo, 3, 4);
            
            // Description and Min Stock Level
            TextField descField = createStyledTextField(product != null ? product.getDescription() : "", "Description");
            TextField minStockField = createStyledTextField(product != null && product.getMinStockLevel() != null ? String.valueOf(product.getMinStockLevel()) : "10", "Min Stock Level");
            grid.add(createFormLabel("Description"), 0, 5); grid.add(descField, 1, 5);
            grid.add(createFormLabel("Min Stock"), 2, 5); grid.add(minStockField, 3, 5);

            // --- Action Buttons ---
            Button saveButton = new Button("SAVE PRODUCT");
            saveButton.getStyleClass().addAll("button", "primary-button");
            saveButton.setMaxWidth(Double.MAX_VALUE);
            saveButton.setOnAction(e -> {
                try {
                    com.pbl3.project.pbl3_project.entity.Product p = product != null ? product : new com.pbl3.project.pbl3_project.entity.Product();
                    
                    if (nameField.getText().isEmpty() || priceField.getText().isEmpty()) {
                       toastService.showError("Please fill required fields (*)");
                       return;
                    }

                    int newQty = Integer.parseInt(qtyField.getText());
                    String reason = "Manual Add/Edit via UI";
                    
                    if (product != null && product.getQuantity() != newQty) {
                        javafx.scene.control.TextInputDialog reasonDialog = new javafx.scene.control.TextInputDialog();
                        reasonDialog.setTitle("Stock Edit Reason");
                        reasonDialog.setHeaderText("Quantity changed: " + product.getQuantity() + " -> " + newQty);
                        reasonDialog.setContentText("Please enter a reason for audit log:");
                        
                        // Apply custom application styling
                        reasonDialog.getDialogPane().getStylesheets().add(getClass().getResource("/application.css").toExternalForm());
                        reasonDialog.getDialogPane().getStyleClass().add("dialog-root");
                        
                        java.util.Optional<String> result = reasonDialog.showAndWait();
                        if (result.isPresent() && !result.get().trim().isEmpty()) {
                            reason = result.get().trim();
                        } else {
                            toastService.showError("Change reason is required to update stock!");
                            return;
                        }
                    }

                    p.setName(nameField.getText());
                    p.setSku(skuField.getText());
                    p.setBarcode(barcodeField.getText());
                    p.setDescription(descField.getText());
                    p.setPrice(parseMoneyInput(priceField.getText()));
                    p.setQuantity(newQty);
                    try { p.setMinStockLevel(Integer.parseInt(minStockField.getText())); } catch (NumberFormatException ex) { p.setMinStockLevel(10); }
                    
                    String importPriceTxt = importPriceField.getText();
                    if (!importPriceTxt.isEmpty()) p.setImportPrice(parseMoneyInput(importPriceTxt));
                    
                    // Implicit Category
                    if (product == null && contextCategory != null) {
                        p.setCategory(contextCategory);
                    } else if (product != null && p.getCategory() == null && contextCategory != null) {
                        p.setCategory(contextCategory);
                    }
                    
                    p.setBrand(brandCombo.getValue());
                    p.setOrigin(originCombo.getValue());
                    p.setUnit(unitCombo.getValue());

                    productService.saveProduct(p, user, reason);
                    toastService.showSuccess("Product saved successfully!");
                    onSave.run();
                    dialog.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    toastService.showError("Save Error: " + ex.getMessage());
                }
            });

            root.getChildren().addAll(titleLabel, grid, new javafx.scene.control.Separator(), saveButton);
            
            Scene scene = new Scene(root, 700, 550); // Wider for Grid
            scene.getStylesheets().add(getClass().getResource("/application.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            toastService.showError("Could not open dialog: " + e.getMessage());
        }
    }
    
    // Helper for ComboBox Generic Converter
    private <T> void setComboConverter(javafx.scene.control.ComboBox<T> comboBox) {
        comboBox.setConverter(new javafx.util.StringConverter<T>() {
            @Override
            public String toString(T object) {
                if (object == null) return "";
                try {
                    return (String) object.getClass().getMethod("getName").invoke(object);
                } catch (Exception e) {
                    return object.toString();
                }
            }
            @Override
            public T fromString(String string) { return null; }
        });
    }

    private javafx.scene.control.Label createDetailMetaValueLabel(String text, String style) {
        javafx.scene.control.Label valueLabel = new javafx.scene.control.Label(text);
        valueLabel.setWrapText(true);
        valueLabel.setMaxWidth(Double.MAX_VALUE);
        valueLabel.setStyle(style);
        return valueLabel;
    }

    private javafx.scene.layout.HBox createDetailMetaRow(String labelText, javafx.scene.Node valueNode) {
        javafx.scene.control.Label keyLabel = new javafx.scene.control.Label(labelText + ":");
        keyLabel.setMinWidth(96);
        keyLabel.setPrefWidth(96);
        keyLabel.setAlignment(Pos.TOP_LEFT);
        keyLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: -app-text-muted;");

        if (valueNode instanceof javafx.scene.layout.Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
            javafx.scene.layout.HBox.setHgrow(region, javafx.scene.layout.Priority.ALWAYS);
        }

        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(8, keyLabel, valueNode);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    private javafx.scene.control.Label createReturnItemChip(String text, boolean emphasized) {
        javafx.scene.control.Label chip = new javafx.scene.control.Label(text);
        chip.getStyleClass().add(emphasized ? "return-item-chip-strong" : "return-item-chip");
        return chip;
    }

    private static final class QuantityStepper extends javafx.scene.layout.HBox {
        private static final javafx.css.PseudoClass FOCUSED_PSEUDO_CLASS = javafx.css.PseudoClass.getPseudoClass("focused");

        private final int minValue;
        private final int maxValue;
        private final javafx.beans.property.IntegerProperty value = new javafx.beans.property.SimpleIntegerProperty(0);
        private final javafx.scene.control.TextField valueField = new javafx.scene.control.TextField("0");
        private final javafx.scene.control.Button minusButton = new javafx.scene.control.Button("-");
        private final javafx.scene.control.Button plusButton = new javafx.scene.control.Button("+");

        private QuantityStepper(int minValue, int maxValue, Runnable onChanged) {
            this.minValue = minValue;
            this.maxValue = Math.max(minValue, maxValue);

            getStyleClass().add("qty-stepper");
            setAlignment(Pos.CENTER);

            minusButton.getStyleClass().setAll("qty-stepper-button");
            minusButton.setText(null);
            minusButton.setGraphic(createStepperIcon(false));
            minusButton.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
            minusButton.setFocusTraversable(false);
            minusButton.setOnAction(e -> setValue(getValue() - 1, true, onChanged));

            valueField.getStyleClass().setAll("qty-stepper-field");
            valueField.setAlignment(Pos.CENTER);
            valueField.setTextFormatter(new javafx.scene.control.TextFormatter<>(change ->
                change.getControlNewText().matches("\\d*") ? change : null
            ));
            valueField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                pseudoClassStateChanged(FOCUSED_PSEUDO_CLASS, isFocused);
                if (!isFocused) {
                    commitEditorText(onChanged);
                }
            });
            valueField.setOnAction(e -> commitEditorText(onChanged));

            plusButton.getStyleClass().setAll("qty-stepper-button");
            plusButton.setText(null);
            plusButton.setGraphic(createStepperIcon(true));
            plusButton.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
            plusButton.setFocusTraversable(false);
            plusButton.setOnAction(e -> setValue(getValue() + 1, true, onChanged));

            getChildren().addAll(minusButton, valueField, plusButton);

            value.addListener((obs, oldVal, newVal) -> {
                String nextText = String.valueOf(newVal.intValue());
                if (!nextText.equals(valueField.getText())) {
                    valueField.setText(nextText);
                }
                minusButton.setDisable(newVal.intValue() <= this.minValue);
                plusButton.setDisable(newVal.intValue() >= this.maxValue);
            });
            value.set(this.minValue);
        }

        private void commitEditorText(Runnable onChanged) {
            String editorText = valueField.getText();
            if (editorText == null || editorText.isBlank()) {
                setValue(minValue, true, onChanged);
                return;
            }
            try {
                setValue(Integer.parseInt(editorText.trim()), true, onChanged);
            } catch (NumberFormatException ignored) {
                valueField.setText(String.valueOf(getValue()));
            }
        }

        private void setValue(int nextValue, boolean notify, Runnable onChanged) {
            int clamped = Math.max(minValue, Math.min(maxValue, nextValue));
            if (value.get() == clamped) {
                valueField.setText(String.valueOf(clamped));
                return;
            }
            value.set(clamped);
            if (notify && onChanged != null) {
                onChanged.run();
            }
        }

        private int getValue() {
            return value.get();
        }

        private javafx.beans.property.IntegerProperty valueProperty() {
            return value;
        }

        private static javafx.scene.Node createStepperIcon(boolean plus) {
            javafx.scene.shape.Line horizontal = new javafx.scene.shape.Line(-4.0, 0.0, 4.0, 0.0);
            horizontal.setStroke(PRIMARY_COLOR);
            horizontal.setStrokeWidth(2.2);
            horizontal.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

            javafx.scene.layout.StackPane iconPane;
            if (plus) {
                javafx.scene.shape.Line vertical = new javafx.scene.shape.Line(0.0, -4.0, 0.0, 4.0);
                vertical.setStroke(PRIMARY_COLOR);
                vertical.setStrokeWidth(2.2);
                vertical.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
                iconPane = new javafx.scene.layout.StackPane(horizontal, vertical);
            } else {
                iconPane = new javafx.scene.layout.StackPane(horizontal);
            }
            iconPane.setMinSize(12, 12);
            iconPane.setPrefSize(12, 12);
            iconPane.setMaxSize(12, 12);
            return iconPane;
        }
    }

    private void showOrderDetailsDialog(Stage owner, com.pbl3.project.pbl3_project.entity.Order order, com.pbl3.project.pbl3_project.entity.User user, Runnable onChanged) {
        try {
            Stage dialog = new Stage();
            dialog.initOwner(owner);
            dialog.setTitle("Order Details #" + order.getId());
            dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);

            VBox root = new VBox(15);
            root.getStyleClass().add("dialog-root");

            Label titleLabel = new Label("Order #" + order.getId());
            titleLabel.getStyleClass().add("dialog-title");
            titleLabel.setMaxWidth(Double.MAX_VALUE);

            Label dateValueLabel = createDetailMetaValueLabel(
                formatDateTimeWithSeconds(order.getCreatedAt()),
                "-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: -app-text-muted;"
            );
            Label userValueLabel = createDetailMetaValueLabel(
                order.getCreatedByDisplayName(),
                "-fx-font-size: 14px; -fx-text-fill: -app-text-secondary;"
            );
            Label customerValueLabel = createDetailMetaValueLabel(
                order.getCustomerDisplayName(),
                "-fx-font-size: 14px; -fx-text-fill: -app-text-secondary;"
            );
            Label customerPhoneValueLabel = createDetailMetaValueLabel(
                order.getCustomerPhoneDisplay(),
                "-fx-font-size: 14px; -fx-text-fill: -app-text-secondary;"
            );
            Label statusValueLabel = createDetailMetaValueLabel(
                formatOrderStatus(order.getStatus()),
                "-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: " + getOrderStatusColor(order.getStatus()) + ";"
            );
            Label refundValueLabel = createDetailMetaValueLabel(
                formatVnd(order.getRefundedAmount()),
                "-fx-font-size: 14px; -fx-text-fill: -app-text-secondary;"
            );

            javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.OrderItem> table = new javafx.scene.control.TableView<>();
            prepareNonReorderableTable(table);
            table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
            
            javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.OrderItem, String> pNameCol = new javafx.scene.control.TableColumn<>("Product");
            pNameCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getProductDisplayName()));
            
            javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.OrderItem, String> grossUnitCol = new javafx.scene.control.TableColumn<>("Gross Unit");
            grossUnitCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                formatVnd(cell.getValue().getOriginalUnitPriceSnapshot())
            ));

            javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.OrderItem, String> priceCol = new javafx.scene.control.TableColumn<>("Net Unit");
            priceCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(formatVnd(cell.getValue().getPrice())));

            javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.OrderItem, Integer> qtyCol = new javafx.scene.control.TableColumn<>("Qty");
            qtyCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("quantity"));

            javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.OrderItem, Integer> returnedCol = new javafx.scene.control.TableColumn<>("Returned");
            returnedCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getReturnedQuantity() != null ? cell.getValue().getReturnedQuantity() : 0));

            javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.OrderItem, Integer> remainingCol = new javafx.scene.control.TableColumn<>("Returnable");
            remainingCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getReturnableQuantity()));

            javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.OrderItem, String> grossLineCol = new javafx.scene.control.TableColumn<>("Gross");
            grossLineCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                formatVnd(cell.getValue().getLineGrossAmount())
            ));

            javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.OrderItem, String> discountCol = new javafx.scene.control.TableColumn<>("Discount");
            discountCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                formatVnd(MoneySupport.add(
                    cell.getValue().getLinePromotionDiscountAmountSnapshot(),
                    cell.getValue().getOrderLevelDiscountAllocatedAmountSnapshot()
                ))
            ));

            javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.OrderItem, String> subTotalCol = new javafx.scene.control.TableColumn<>("Net");
            subTotalCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                formatVnd(cell.getValue().getLineNetAmount())
            ));

            table.getColumns().addAll(pNameCol, grossUnitCol, priceCol, qtyCol, returnedCol, remainingCol, grossLineCol, discountCol, subTotalCol);
            table.setItems(javafx.collections.FXCollections.observableArrayList(order.getOrderItems()));
            table.setPrefHeight(300);

            Label totalLabel = new Label("Gross Subtotal: " + formatVnd(order.getGrossSubtotalSnapshot()));
            totalLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");
            totalLabel.setMaxWidth(Double.MAX_VALUE);
            totalLabel.setAlignment(Pos.CENTER_RIGHT);

            Label discountLabel = new Label("Discounts: " + formatVnd(order.getDiscountTotalSnapshot()));
            discountLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: -app-primary;");
            discountLabel.setMaxWidth(Double.MAX_VALUE);
            discountLabel.setAlignment(Pos.CENTER_RIGHT);

            Label paidLabel = new Label("Amount Paid: " + formatVnd(order.getTotalPrice()));
            paidLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: -app-danger;");
            paidLabel.setMaxWidth(Double.MAX_VALUE);
            paidLabel.setAlignment(Pos.CENTER_RIGHT);

            Label netLabel = new Label("Net After Refunds: " + formatVnd(order.getNetTotal()));
            netLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: -app-success-hover;");
            netLabel.setMaxWidth(Double.MAX_VALUE);
            netLabel.setAlignment(Pos.CENTER_RIGHT);

            Label noteLabel = new Label(order.getStatusNote() == null || order.getStatusNote().isBlank()
                ? "-"
                : order.getStatusNote());
            noteLabel.setWrapText(true);
            noteLabel.setStyle("-fx-text-fill: -app-text-secondary; -fx-font-size: 13px;");

            VBox metaBox = new VBox(
                8,
                createDetailMetaRow("Date", dateValueLabel),
                createDetailMetaRow("Created By", userValueLabel),
                createDetailMetaRow("Customer", customerValueLabel),
                createDetailMetaRow("Customer Phone", customerPhoneValueLabel),
                createDetailMetaRow("Status", statusValueLabel),
                createDetailMetaRow("Refunded", refundValueLabel),
                createDetailMetaRow("Status Note", noteLabel)
            );

            Button returnButton = new Button("Return Items");
            returnButton.getStyleClass().addAll("button", "primary-button", "return-items-button");
            returnButton.setDisable(order.getStatus() == com.pbl3.project.pbl3_project.entity.OrderStatus.CANCELED
                || order.getStatus() == com.pbl3.project.pbl3_project.entity.OrderStatus.RETURNED);
            returnButton.setOnAction(e -> showReturnOrderDialog(dialog, orderService.getOrderWithItems(order.getId(), user), user, () -> {
                if (onChanged != null) {
                    onChanged.run();
                }
                dialog.close();
            }));

            Button cancelOrderButton = new Button("Cancel Order");
            cancelOrderButton.getStyleClass().addAll("button", "danger-button");
            cancelOrderButton.setDisable(order.getStatus() != null && order.getStatus() != com.pbl3.project.pbl3_project.entity.OrderStatus.COMPLETED);
            cancelOrderButton.setOnAction(e -> showCancelOrderDialog(dialog, orderService.getOrderWithItems(order.getId(), user), user, () -> {
                if (onChanged != null) {
                    onChanged.run();
                }
                dialog.close();
            }));

            Button closeButton = new Button("Close");
            closeButton.getStyleClass().addAll("button", "close-button");
            closeButton.setOnAction(e -> dialog.close());

            javafx.scene.layout.HBox actionRow = new javafx.scene.layout.HBox(10, returnButton, cancelOrderButton, closeButton);
            actionRow.setAlignment(Pos.CENTER_RIGHT);

            root.getChildren().addAll(titleLabel, metaBox, table, totalLabel, discountLabel, paidLabel, netLabel, actionRow);
            VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
            enableDeselectOnOutsideClick(root, table);
            
            Scene scene = new Scene(root, 760, 620);
            scene.getStylesheets().add(getClass().getResource("/application.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Error", "Could not show details: " + e.getMessage());
        }
    }

    private void showCancelOrderDialog(Stage owner, com.pbl3.project.pbl3_project.entity.Order order, com.pbl3.project.pbl3_project.entity.User user, Runnable onSuccess) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
        dialog.setTitle("Cancel Order #" + order.getId());

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("dialog-root");

        Label title = new Label("Cancel Order #" + order.getId());
        title.getStyleClass().add("dialog-title");

        Label helper = new Label("This will restore stock for every order item. Reason is required.");
        helper.setStyle("-fx-text-fill: -app-text-secondary; -fx-font-size: 13px;");
        helper.setWrapText(true);

        javafx.scene.control.TextArea reasonArea = new javafx.scene.control.TextArea();
        reasonArea.setPromptText("Enter cancellation reason...");
        reasonArea.setWrapText(true);
        reasonArea.setPrefRowCount(4);

        Button confirmBtn = new Button("Confirm Cancel");
        confirmBtn.getStyleClass().addAll("button", "danger-button");
        confirmBtn.setOnAction(e -> {
            try {
                orderService.cancelOrder(order.getId(), user.getId(), reasonArea.getText());
                toastService.showSuccess("Order canceled successfully");
                dialog.close();
                if (onSuccess != null) {
                    onSuccess.run();
                }
            } catch (Exception ex) {
                showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Cancel Order Failed", ex.getMessage());
            }
        });

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().addAll("button", "close-button");
        closeBtn.setOnAction(e -> dialog.close());

        javafx.scene.layout.HBox actionRow = new javafx.scene.layout.HBox(10, closeBtn, confirmBtn);
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(title, helper, reasonArea, actionRow);

        Scene scene = new Scene(root, 520, 300);
        scene.getStylesheets().add(getClass().getResource("/application.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void showReturnOrderDialog(Stage owner, com.pbl3.project.pbl3_project.entity.Order order, com.pbl3.project.pbl3_project.entity.User user, Runnable onSuccess) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
        dialog.setTitle("Return Items - Order #" + order.getId());

        VBox root = new VBox(16);
        root.setPadding(new Insets(22));
        root.getStyleClass().addAll("dialog-root", "return-dialog-root");

        Label title = new Label("Return Items for Order #" + order.getId());
        title.getStyleClass().add("dialog-title");

        Label helper = new Label("Choose the quantities to return and provide a reason for the audit log.");
        helper.getStyleClass().add("dialog-subtitle");
        helper.setWrapText(true);

        VBox itemsBox = new VBox(12);
        itemsBox.getStyleClass().add("return-items-list");
        itemsBox.setFillWidth(true);
        java.util.Map<Long, QuantityStepper> returnInputs = new java.util.LinkedHashMap<>();
        Label refundCaptionLabel = new Label("Estimated Refund");
        refundCaptionLabel.getStyleClass().add("return-summary-caption");
        Label refundPreviewLabel = new Label(formatVnd(BigDecimal.ZERO));
        refundPreviewLabel.getStyleClass().add("return-summary-value");

        Runnable updateRefundPreview = () -> {
            BigDecimal refundTotal = BigDecimal.ZERO;
            int selectedQuantity = 0;
            for (com.pbl3.project.pbl3_project.entity.OrderItem item : order.getOrderItems()) {
                QuantityStepper stepper = returnInputs.get(item.getId());
                if (stepper != null) {
                    int selected = stepper.getValue();
                    int currentReturned = item.getReturnedQuantity() != null ? item.getReturnedQuantity() : 0;
                    refundTotal = MoneySupport.add(
                        refundTotal,
                        MoneySupport.subtract(
                            item.calculateRefundForReturnedQuantity(currentReturned + selected),
                            item.calculateRefundForReturnedQuantity(currentReturned)
                        )
                    );
                    selectedQuantity += selected;
                }
            }
            refundCaptionLabel.setText(selectedQuantity > 0
                ? "Estimated Refund • " + selectedQuantity + " item" + (selectedQuantity > 1 ? "s" : "")
                : "Estimated Refund");
            refundPreviewLabel.setText(formatVnd(refundTotal));
        };

        for (com.pbl3.project.pbl3_project.entity.OrderItem item : order.getOrderItems()) {
            int returnableQuantity = item.getReturnableQuantity();
            if (returnableQuantity <= 0) {
                continue;
            }

            Label productLabel = new Label(item.getProductDisplayName());
            productLabel.getStyleClass().add("return-item-name");

            javafx.scene.layout.FlowPane metaPane = new javafx.scene.layout.FlowPane(8, 8);
            metaPane.getStyleClass().add("return-item-meta-row");
            metaPane.getChildren().addAll(
                createReturnItemChip("Ordered " + item.getQuantity(), false),
                createReturnItemChip("Returned " + (item.getReturnedQuantity() != null ? item.getReturnedQuantity() : 0), false),
                createReturnItemChip("Returnable " + returnableQuantity, true),
                createReturnItemChip("Net Unit " + formatVnd(item.getPrice()), false)
            );

            QuantityStepper qtyStepper = new QuantityStepper(0, returnableQuantity, updateRefundPreview);
            returnInputs.put(item.getId(), qtyStepper);

            Label qtyLabel = new Label("Return Qty");
            qtyLabel.getStyleClass().add("return-item-qty-label");

            VBox qtyBox = new VBox(8, qtyLabel, qtyStepper);
            qtyBox.getStyleClass().add("return-item-qty-box");
            qtyBox.setAlignment(Pos.CENTER);

            VBox infoBox = new VBox(10, productLabel, metaPane);
            javafx.scene.layout.HBox.setHgrow(infoBox, javafx.scene.layout.Priority.ALWAYS);

            javafx.scene.layout.HBox contentRow = new javafx.scene.layout.HBox(16, infoBox, qtyBox);
            contentRow.setAlignment(Pos.CENTER_LEFT);

            VBox itemCard = new VBox(contentRow);
            itemCard.getStyleClass().add("return-item-card");
            itemsBox.getChildren().add(itemCard);
        }

        if (returnInputs.isEmpty()) {
            toastService.showInfo("This order has no returnable items");
            return;
        }

        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(itemsBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPannable(true);
        scrollPane.getStyleClass().add("return-items-scroll");
        double visibleItemCount = Math.min(returnInputs.size(), 3);
        double itemsViewportHeight = Math.max(210, visibleItemCount * 136.0 + 12);
        scrollPane.setPrefViewportHeight(itemsViewportHeight);

        javafx.scene.layout.StackPane itemsPanel = new javafx.scene.layout.StackPane(scrollPane);
        itemsPanel.getStyleClass().add("return-items-panel");

        VBox refundSummaryCard = new VBox(4, refundCaptionLabel, refundPreviewLabel);
        refundSummaryCard.getStyleClass().add("return-summary-card");

        javafx.scene.control.TextArea reasonArea = new javafx.scene.control.TextArea();
        reasonArea.setPromptText("Explain why these items are being returned...");
        reasonArea.setWrapText(true);
        reasonArea.setPrefRowCount(3);
        reasonArea.getStyleClass().add("return-reason-area");

        Label reasonLabel = new Label("Return Reason");
        reasonLabel.getStyleClass().add("return-section-label");
        VBox reasonBox = new VBox(8, reasonLabel, reasonArea);

        Button confirmBtn = new Button("Confirm Return");
        confirmBtn.getStyleClass().addAll("button", "primary-button");
        confirmBtn.setOnAction(e -> {
            java.util.Map<Long, Integer> returnQuantities = new java.util.LinkedHashMap<>();
            for (java.util.Map.Entry<Long, QuantityStepper> entry : returnInputs.entrySet()) {
                entry.getValue().commitEditorText(updateRefundPreview);
                if (entry.getValue().getValue() > 0) {
                    returnQuantities.put(entry.getKey(), entry.getValue().getValue());
                }
            }

            try {
                orderService.returnOrderItems(order.getId(), user.getId(), returnQuantities, reasonArea.getText());
                toastService.showSuccess("Order items returned successfully");
                dialog.close();
                if (onSuccess != null) {
                    onSuccess.run();
                }
            } catch (Exception ex) {
                showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Return Failed", ex.getMessage());
            }
        });

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().addAll("button", "close-button");
        closeBtn.setOnAction(e -> dialog.close());

        javafx.scene.layout.HBox actionRow = new javafx.scene.layout.HBox(10, closeBtn, confirmBtn);
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(title, helper, itemsPanel, refundSummaryCard, reasonBox, actionRow);

        double dialogHeight = Math.min(760, 370 + itemsViewportHeight);
        Scene scene = new Scene(root, 720, dialogHeight);
        scene.getStylesheets().add(getClass().getResource("/application.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private String formatOrderStatus(com.pbl3.project.pbl3_project.entity.OrderStatus status) {
        com.pbl3.project.pbl3_project.entity.OrderStatus safeStatus =
            status != null ? status : com.pbl3.project.pbl3_project.entity.OrderStatus.COMPLETED;
        return switch (safeStatus) {
            case COMPLETED -> "Completed";
            case PARTIALLY_RETURNED -> "Partially Returned";
            case RETURNED -> "Returned";
            case CANCELED -> "Canceled";
        };
    }

    private String getOrderStatusColor(com.pbl3.project.pbl3_project.entity.OrderStatus status) {
        com.pbl3.project.pbl3_project.entity.OrderStatus safeStatus =
            status != null ? status : com.pbl3.project.pbl3_project.entity.OrderStatus.COMPLETED;
        return switch (safeStatus) {
            case COMPLETED -> "-app-success-hover";
            case PARTIALLY_RETURNED, RETURNED -> "-app-primary-hover";
            case CANCELED -> "-app-danger-hover";
        };
    }

    private String formatDateTime(java.time.LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DISPLAY_DATE_TIME_FORMATTER) : "-";
    }

    private String formatDateTimeWithSeconds(java.time.LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DISPLAY_DATE_TIME_SECONDS_FORMATTER) : "-";
    }

    private String formatDate(java.time.LocalDate date) {
        return date != null ? date.format(DISPLAY_DATE_FORMATTER) : "-";
    }

    private String formatOperationalReportRangeLabel(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return "All Time";
        }
        if (startDate != null && endDate != null) {
            return formatDate(startDate) + " - " + formatDate(endDate);
        }
        if (startDate != null) {
            return "From " + formatDate(startDate);
        }
        return "Until " + formatDate(endDate);
    }

    private String buildNoSalesRangeText(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return "No sales in all time";
        }
        if (startDate != null && endDate != null) {
            if (startDate.equals(endDate)) {
                return "No sales on " + formatDate(startDate);
            }
            return "No sales from " + formatDate(startDate) + " to " + formatDate(endDate);
        }
        if (startDate != null) {
            return "No sales from " + formatDate(startDate);
        }
        return "No sales until " + formatDate(endDate);
    }

    private String formatImportOrderStatus(com.pbl3.project.pbl3_project.entity.ImportOrderStatus status) {
        com.pbl3.project.pbl3_project.entity.ImportOrderStatus safeStatus =
            status != null ? status : com.pbl3.project.pbl3_project.entity.ImportOrderStatus.COMPLETED;
        return switch (safeStatus) {
            case COMPLETED -> "Completed";
            case CANCELED -> "Canceled";
        };
    }

    private String getImportOrderStatusColor(com.pbl3.project.pbl3_project.entity.ImportOrderStatus status) {
        com.pbl3.project.pbl3_project.entity.ImportOrderStatus safeStatus =
            status != null ? status : com.pbl3.project.pbl3_project.entity.ImportOrderStatus.COMPLETED;
        return switch (safeStatus) {
            case COMPLETED -> "-app-success-hover";
            case CANCELED -> "-app-danger-hover";
        };
    }

    private String formatTransactionTypeLabel(com.pbl3.project.pbl3_project.entity.InventoryTransactionType type) {
        return formatTransactionTypeLabel(type != null ? type.name() : null);
    }

    private String formatTransactionTypeLabel(String type) {
        if (type == null || type.isBlank()) {
            return "Unknown";
        }
        return switch (type) {
            case "IMPORT" -> "Import Goods";
            case "CANCEL_IMPORT" -> "Import Canceled";
            case "SALE" -> "Sale";
            case "CANCEL_SALE" -> "Sale Canceled";
            case "RETURN" -> "Customer Return";
            case "MANUAL_ADJUST" -> "Manual Adjustment";
            case "REVALUE" -> "Inventory Revalued";
            case "DELETE" -> "Product Deleted";
            default -> {
                String normalized = type.toLowerCase().replace('_', ' ');
                StringBuilder builder = new StringBuilder();
                for (String part : normalized.split("\\s+")) {
                    if (part.isEmpty()) continue;
                    if (builder.length() > 0) builder.append(' ');
                    builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
                }
                yield builder.isEmpty() ? type : builder.toString();
            }
        };
    }

    private String getTransactionTypeColor(com.pbl3.project.pbl3_project.entity.InventoryTransactionType type) {
        return getTransactionTypeColor(type != null ? type.name() : null);
    }

    private String getTransactionTypeColor(String type) {
        if (type == null || type.isBlank()) {
            return "-app-text-secondary";
        }
        return switch (type) {
            case "IMPORT" -> "-app-success-hover";
            case "CANCEL_IMPORT" -> "-app-danger-hover";
            case "SALE" -> "-app-primary-hover";
            case "CANCEL_SALE", "DELETE" -> "-app-danger-hover";
            case "RETURN", "MANUAL_ADJUST", "REVALUE" -> "-app-primary";
            default -> "-app-text-secondary";
        };
    }

    private String formatVnd(BigDecimal amount) {
        return String.format("%,.0f VND", MoneySupport.normalize(amount).doubleValue());
    }

    private String formatVnd(double amount) {
        return String.format("%,.0f VND", amount);
    }

    private String formatWholeNumberText(BigDecimal value) {
        return value == null ? "" : String.valueOf(MoneySupport.normalize(value).setScale(0, java.math.RoundingMode.HALF_UP).toBigIntegerExact());
    }

    private BigDecimal parseMoneyInput(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        return MoneySupport.normalize(new BigDecimal(value.replace(",", "").trim()));
    }

    private TextField createStyledTextField(String value, String prompt) {
        TextField tf = new TextField(value);
        tf.setPromptText(prompt);
        tf.getStyleClass().add("text-field");
        return tf;
    }

    private javafx.scene.control.TextArea createStyledTextArea(String value, String prompt, int prefRowCount) {
        javafx.scene.control.TextArea area = new javafx.scene.control.TextArea(value);
        area.setPromptText(prompt);
        area.getStyleClass().add("text-area");
        area.setWrapText(true);
        area.setPrefRowCount(prefRowCount);
        return area;
    }

    private void applyReadOnlyTextInput(javafx.scene.control.TextInputControl control) {
        control.setEditable(false);
        control.setFocusTraversable(false);
    }

    private Label createFormLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("form-label");
        return l;
    }

    private javafx.util.StringConverter<com.pbl3.project.pbl3_project.entity.UiAccentPreset> createUiAccentPresetConverter() {
        return new javafx.util.StringConverter<>() {
            @Override
            public String toString(com.pbl3.project.pbl3_project.entity.UiAccentPreset preset) {
                return preset != null ? preset.getLabel() : "";
            }

            @Override
            public com.pbl3.project.pbl3_project.entity.UiAccentPreset fromString(String string) {
                return java.util.Arrays.stream(com.pbl3.project.pbl3_project.entity.UiAccentPreset.values())
                    .filter(preset -> preset.getLabel().equalsIgnoreCase(string))
                    .findFirst()
                    .orElse(com.pbl3.project.pbl3_project.entity.UiAccentPreset.BLUE);
            }
        };
    }

    private javafx.scene.control.ListCell<com.pbl3.project.pbl3_project.entity.UiAccentPreset> createUiAccentPresetListCell() {
        return new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(com.pbl3.project.pbl3_project.entity.UiAccentPreset item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getLabel());
            }
        };
    }

    private javafx.util.StringConverter<com.pbl3.project.pbl3_project.entity.UiDensityMode> createUiDensityModeConverter() {
        return new javafx.util.StringConverter<>() {
            @Override
            public String toString(com.pbl3.project.pbl3_project.entity.UiDensityMode densityMode) {
                return densityMode != null ? densityMode.getLabel() : "";
            }

            @Override
            public com.pbl3.project.pbl3_project.entity.UiDensityMode fromString(String string) {
                return java.util.Arrays.stream(com.pbl3.project.pbl3_project.entity.UiDensityMode.values())
                    .filter(mode -> mode.getLabel().equalsIgnoreCase(string))
                    .findFirst()
                    .orElse(com.pbl3.project.pbl3_project.entity.UiDensityMode.COMFORTABLE);
            }
        };
    }

    private javafx.scene.control.ListCell<com.pbl3.project.pbl3_project.entity.UiDensityMode> createUiDensityModeListCell() {
        return new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(com.pbl3.project.pbl3_project.entity.UiDensityMode item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getLabel());
            }
        };
    }

    private <T> void enableDragSelection(javafx.scene.control.TableView<T> table) {
        prepareNonReorderableTable(table);
        final int[] dragAnchor = new int[] { -1 };

        // 1. Filter to handle "Click Again to Deselect" logic
        table.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
             if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                 // Check if it's a simple click (no modifiers)
                 boolean isSimpleClick = !event.isShortcutDown() && !event.isShiftDown();
                 
                 javafx.scene.Node node = event.getPickResult().getIntersectedNode();
                 while (node != null && node != table && !(node instanceof javafx.scene.control.TableRow)) {
                     node = node.getParent();
                 }
                 
                 if (node instanceof javafx.scene.control.TableRow) {
                     javafx.scene.control.TableRow<?> row = (javafx.scene.control.TableRow<?>) node;
                     if (!row.isEmpty()) {
                         int index = row.getIndex();
                         if (isSimpleClick && event.getClickCount() == 1 && table.getSelectionModel().isSelected(index)) {
                             // Deselect and consume to prevent "Select Only" behavior
                             table.getSelectionModel().clearSelection(index);
                             dragAnchor[0] = index; 
                             event.consume();
                         } else {
                             // Let standard behavior run or record anchor
                             dragAnchor[0] = index;
                         }
                     }
                 }
             }
        });

        table.setOnMouseDragged(event -> {
            if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY && dragAnchor[0] >= 0) {
                javafx.scene.Node node = event.getPickResult().getIntersectedNode();
                while (node != null && node != table && !(node instanceof javafx.scene.control.TableRow)) {
                    node = node.getParent();
                }
                if (node instanceof javafx.scene.control.TableRow) {
                     javafx.scene.control.TableRow<?> row = (javafx.scene.control.TableRow<?>) node;
                     if (!row.isEmpty()) {
                         int currentIndex = row.getIndex();
                         table.getSelectionModel().clearSelection();
                         int start = Math.min(dragAnchor[0], currentIndex);
                         int end = Math.max(dragAnchor[0], currentIndex);
                         table.getSelectionModel().selectRange(start, end + 1);
                     }
                }
            }
        });
    }

    private void prepareNonReorderableTable(javafx.scene.control.TableView<?> table) {
        if (table == null) {
            return;
        }
        table.getSelectionModel().setCellSelectionEnabled(false);
        disableColumnReordering(table);
    }

    private void disableColumnReordering(javafx.scene.control.TableView<?> table) {
        if (table == null) {
            return;
        }
        installColumnReorderGuard(table.getColumns());
    }

    private void installColumnReorderGuard(
        javafx.collections.ObservableList<? extends javafx.scene.control.TableColumnBase<?, ?>> columns
    ) {
        if (columns == null) {
            return;
        }
        for (javafx.scene.control.TableColumnBase<?, ?> column : columns) {
            installColumnReorderGuard(column);
        }
        columns.addListener(
            (javafx.collections.ListChangeListener<javafx.scene.control.TableColumnBase<?, ?>>) change -> {
                while (change.next()) {
                    if (change.wasAdded()) {
                        for (javafx.scene.control.TableColumnBase<?, ?> column : change.getAddedSubList()) {
                            installColumnReorderGuard(column);
                        }
                    }
                }
            }
        );
    }

    private void installColumnReorderGuard(javafx.scene.control.TableColumnBase<?, ?> column) {
        if (column == null) {
            return;
        }
        column.setReorderable(false);
        if (Boolean.TRUE.equals(column.getProperties().get(COLUMN_REORDER_GUARD_KEY))) {
            return;
        }
        column.getProperties().put(COLUMN_REORDER_GUARD_KEY, Boolean.TRUE);
        installColumnReorderGuard(column.getColumns());
    }

    private void enableDeselectOnOutsideClick(javafx.scene.layout.Pane root, javafx.scene.control.TableView<?> table) {
        enableDeselectOnOutsideClick(root, new javafx.scene.control.TableView<?>[]{table});
    }

    private void enableDeselectOnOutsideClick(javafx.scene.layout.Pane root, javafx.scene.control.TableView<?>... tables) {
        root.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
            boolean isSafe = false;
            javafx.scene.Node curr = (javafx.scene.Node) event.getTarget();
            
            boolean clickedEmptyTableArea = false;
            javafx.scene.Node checkNode = curr;
            while (checkNode != null && checkNode != root) {
                if (checkNode instanceof javafx.scene.control.IndexedCell) {
                    if (((javafx.scene.control.IndexedCell<?>) checkNode).isEmpty()) {
                        clickedEmptyTableArea = true;
                    }
                    break;
                }
                if (checkNode.getClass().getSimpleName().equals("TableBodyStack")) {
                    clickedEmptyTableArea = true;
                    break;
                }
                checkNode = checkNode.getParent();
            }

            while (curr != null && curr != root) {
                if (isInsideAnyTable(curr, tables) && !clickedEmptyTableArea) {
                    isSafe = true;
                    break;
                }
                if (
                    curr instanceof javafx.scene.control.Button || 
                    curr instanceof javafx.scene.control.TextField || 
                    curr instanceof javafx.scene.control.ComboBox ||
                    curr instanceof javafx.scene.control.DatePicker ||
                    curr instanceof javafx.scene.control.MenuBar ||
                    curr.getStyleClass().contains("expandable-search-box") ||
                    curr.getStyleClass().contains("search-field") ||
                    curr.getStyleClass().contains("search-text-field")
                ) {
                    isSafe = true; 
                    break; 
                }
                curr = curr.getParent();
            }
            if (!isSafe) {
                clearSelections(tables);
                root.requestFocus();
            }
        });

        root.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                clearSelections(tables);
                root.requestFocus();
            }
        });
    }

    private void showPopupBelow(javafx.stage.Popup popup, javafx.scene.Node owner, double xOffset, double yOffset) {
        if (popup == null || owner == null) {
            return;
        }
        Runnable showAction = () -> {
            if (owner.getScene() == null || owner.getScene().getWindow() == null) {
                return;
            }
            javafx.geometry.Bounds bounds = owner.localToScreen(owner.getBoundsInLocal());
            if (bounds == null) {
                return;
            }
            double anchorX = bounds.getMinX() + xOffset;
            double anchorY = bounds.getMaxY() + yOffset;
            double popupWidth = 0.0;
            if (!popup.getContent().isEmpty()) {
                javafx.scene.Node content = popup.getContent().get(0);
                if (content instanceof javafx.scene.Parent parent) {
                    parent.applyCss();
                    parent.autosize();
                }
                if (content instanceof javafx.scene.layout.Region region) {
                    popupWidth = Math.max(region.prefWidth(-1), region.getLayoutBounds().getWidth());
                } else {
                    popupWidth = content.getLayoutBounds().getWidth();
                }
            }
            javafx.stage.Screen screen = javafx.stage.Screen.getScreensForRectangle(
                bounds.getMinX(),
                bounds.getMinY(),
                Math.max(bounds.getWidth(), 1),
                Math.max(bounds.getHeight(), 1)
            ).stream().findFirst().orElse(javafx.stage.Screen.getPrimary());
            javafx.geometry.Rectangle2D visualBounds = screen.getVisualBounds();
            if (popupWidth > 0.0) {
                anchorX = Math.max(
                    visualBounds.getMinX() + 8,
                    Math.min(anchorX, visualBounds.getMaxX() - popupWidth - 8)
                );
            }
            anchorY = Math.max(visualBounds.getMinY() + 8, anchorY);
            popup.show(owner, anchorX, anchorY);
        };
        javafx.application.Platform.runLater(showAction);
    }

    private boolean isInsideAnyTable(javafx.scene.Node node, javafx.scene.control.TableView<?>... tables) {
        if (node == null || tables == null) {
            return false;
        }
        for (javafx.scene.control.TableView<?> table : tables) {
            if (table != null && node == table) {
                return true;
            }
        }
        return false;
    }

    private void clearSelections(javafx.scene.control.TableView<?>... tables) {
        if (tables == null) {
            return;
        }
        for (javafx.scene.control.TableView<?> table : tables) {
            if (table != null) {
                table.getSelectionModel().clearSelection();
            }
        }
    }

    private javafx.scene.control.TableView<?> findFirstTableView(javafx.scene.Node node) {
        if (node instanceof javafx.scene.control.TableView<?> tableView) {
            return tableView;
        }
        if (node instanceof javafx.scene.Parent parent) {
            for (javafx.scene.Node child : parent.getChildrenUnmodifiable()) {
                javafx.scene.control.TableView<?> childTable = findFirstTableView(child);
                if (childTable != null) {
                    return childTable;
                }
            }
        }
        return null;
    }

    private static java.util.List<SortCriterion> copyCriteria(java.util.List<SortCriterion> criteria) {
        java.util.List<SortCriterion> copy = new java.util.ArrayList<>();
        if (criteria == null) {
            return copy;
        }
        for (SortCriterion criterion : criteria) {
            if (criterion == null || criterion.uiKey() == null || criterion.uiKey().isBlank() || criterion.direction() == null) {
                continue;
            }
            copy.add(new SortCriterion(criterion.uiKey(), criterion.direction()));
        }
        return copy;
    }

    private TableSortState getOrCreateTableSortState(String stateKey, SortCriterion... defaultCriteria) {
        return sessionSortStates.computeIfAbsent(
            stateKey,
            key -> new TableSortState(java.util.Arrays.asList(defaultCriteria))
        );
    }

    private void advanceSortState(TableSortState sortState, String uiKey, boolean multiSort) {
        if (sortState == null || uiKey == null || uiKey.isBlank()) {
            return;
        }
        java.util.List<SortCriterion> current = sortState.snapshot();
        int existingIndex = -1;
        for (int i = 0; i < current.size(); i++) {
            if (uiKey.equals(current.get(i).uiKey())) {
                existingIndex = i;
                break;
            }
        }

        if (!multiSort) {
            if (existingIndex < 0) {
                sortState.replace(java.util.List.of(new SortCriterion(uiKey, javafx.scene.control.TableColumn.SortType.ASCENDING)));
                return;
            }
            javafx.scene.control.TableColumn.SortType direction = current.get(existingIndex).direction();
            if (direction == javafx.scene.control.TableColumn.SortType.ASCENDING) {
                sortState.replace(java.util.List.of(new SortCriterion(uiKey, javafx.scene.control.TableColumn.SortType.DESCENDING)));
            } else {
                sortState.clear();
            }
            return;
        }

        if (existingIndex < 0) {
            current.add(new SortCriterion(uiKey, javafx.scene.control.TableColumn.SortType.ASCENDING));
            sortState.replace(current);
            return;
        }

        SortCriterion existing = current.get(existingIndex);
        if (existing.direction() == javafx.scene.control.TableColumn.SortType.ASCENDING) {
            current.set(existingIndex, new SortCriterion(uiKey, javafx.scene.control.TableColumn.SortType.DESCENDING));
        } else {
            current.remove(existingIndex);
        }
        sortState.replace(current);
    }

    private <T> void applySortStateToTable(
        javafx.scene.control.TableView<T> table,
        java.util.Map<String, javafx.scene.control.TableColumn<T, ?>> columnsByKey,
        TableSortState sortState
    ) {
        if (table == null || columnsByKey == null || sortState == null) {
            return;
        }
        table.getSortOrder().clear();
        updateSortHeaderIndicators(columnsByKey, sortState);
    }

    private <T> void installSortHeaderIndicators(java.util.Map<String, javafx.scene.control.TableColumn<T, ?>> columnsByKey) {
        if (columnsByKey == null || columnsByKey.isEmpty()) {
            return;
        }
        for (javafx.scene.control.TableColumn<T, ?> column : columnsByKey.values()) {
            if (column == null) {
                continue;
            }

            String baseText = column.getProperties().get(SORT_HEADER_BASE_TEXT_KEY) instanceof String storedText
                ? storedText
                : (column.getText() == null ? "" : column.getText());
            column.getProperties().put(SORT_HEADER_BASE_TEXT_KEY, baseText);

            if (column.getProperties().get(SORT_HEADER_TRIANGLE_KEY) instanceof javafx.scene.shape.Polygon) {
                continue;
            }

            Label headerLabel = new Label(baseText);
            headerLabel.setAlignment(Pos.CENTER);
            headerLabel.setMaxWidth(Double.MAX_VALUE);
            headerLabel.setMouseTransparent(true);
            headerLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

            javafx.scene.shape.Polygon triangle = new javafx.scene.shape.Polygon(
                0.0, 6.0,
                5.0, 0.0,
                10.0, 6.0
            );
            triangle.setFill(TEXT_MUTED_COLOR);
            triangle.setOpacity(0.0);
            triangle.setMouseTransparent(true);

            javafx.scene.layout.Region leftSpacer = new javafx.scene.layout.Region();
            leftSpacer.setMinWidth(16);
            leftSpacer.setPrefWidth(16);
            leftSpacer.setMaxWidth(16);
            leftSpacer.setMouseTransparent(true);

            javafx.scene.layout.StackPane arrowBox = new javafx.scene.layout.StackPane(triangle);
            arrowBox.setMinWidth(16);
            arrowBox.setPrefWidth(16);
            arrowBox.setMaxWidth(16);
            arrowBox.setAlignment(Pos.CENTER_RIGHT);
            arrowBox.setMouseTransparent(true);

            javafx.scene.layout.BorderPane headerGraphic = new javafx.scene.layout.BorderPane();
            headerGraphic.setMaxWidth(Double.MAX_VALUE);
            headerGraphic.setMouseTransparent(true);
            headerGraphic.setLeft(leftSpacer);
            headerGraphic.setCenter(headerLabel);
            headerGraphic.setRight(arrowBox);
            headerGraphic.prefWidthProperty().bind(
                javafx.beans.binding.Bindings.createDoubleBinding(
                    () -> Math.max(0.0, column.getWidth() - 24.0),
                    column.widthProperty()
                )
            );
            javafx.scene.layout.BorderPane.setAlignment(headerLabel, Pos.CENTER);
            javafx.scene.layout.BorderPane.setAlignment(arrowBox, Pos.CENTER_RIGHT);

            column.setText(null);
            column.setGraphic(headerGraphic);
            column.getProperties().put(SORT_HEADER_LABEL_KEY, headerLabel);
            column.getProperties().put(SORT_HEADER_TRIANGLE_KEY, triangle);
        }
    }

    private <T> void updateSortHeaderIndicators(
        java.util.Map<String, javafx.scene.control.TableColumn<T, ?>> columnsByKey,
        TableSortState sortState
    ) {
        if (columnsByKey == null || columnsByKey.isEmpty() || sortState == null) {
            return;
        }

        for (javafx.scene.control.TableColumn<T, ?> column : columnsByKey.values()) {
            if (column == null) {
                continue;
            }
            if (column.getProperties().get(SORT_HEADER_LABEL_KEY) instanceof Label headerLabel
                && column.getProperties().get(SORT_HEADER_BASE_TEXT_KEY) instanceof String baseText) {
                headerLabel.setText(baseText);
            }
            if (column.getProperties().get(SORT_HEADER_TRIANGLE_KEY) instanceof javafx.scene.shape.Polygon triangle) {
                triangle.setOpacity(0.0);
                triangle.setRotate(0.0);
                triangle.setFill(TEXT_MUTED_COLOR);
            }
        }

        java.util.List<SortCriterion> criteria = sortState.snapshot();
        for (int index = 0; index < criteria.size(); index++) {
            SortCriterion criterion = criteria.get(index);
            javafx.scene.control.TableColumn<T, ?> column = columnsByKey.get(criterion.uiKey());
            if (column == null) {
                continue;
            }
            if (!(column.getProperties().get(SORT_HEADER_TRIANGLE_KEY) instanceof javafx.scene.shape.Polygon triangle)) {
                continue;
            }
            triangle.setOpacity(index == 0 ? 1.0 : 0.8);
            triangle.setRotate(
                criterion.direction() == javafx.scene.control.TableColumn.SortType.ASCENDING ? 0.0 : 180.0
            );
            triangle.setFill(index == 0 ? PRIMARY_COLOR : TEXT_MUTED_COLOR);
        }
    }

    private org.springframework.data.domain.Pageable createPageable(
        TableSortState sortState,
        java.util.Map<String, String> propertyByKey,
        int page,
        int size
    ) {
        if (sortState == null || propertyByKey == null || propertyByKey.isEmpty()) {
            return org.springframework.data.domain.PageRequest.of(page, size);
        }
        java.util.List<org.springframework.data.domain.Sort.Order> orders = new java.util.ArrayList<>();
        for (SortCriterion criterion : sortState.snapshot()) {
            String property = propertyByKey.get(criterion.uiKey());
            if (property == null || property.isBlank()) {
                continue;
            }
            org.springframework.data.domain.Sort.Direction direction =
                criterion.direction() == javafx.scene.control.TableColumn.SortType.ASCENDING
                    ? org.springframework.data.domain.Sort.Direction.ASC
                    : org.springframework.data.domain.Sort.Direction.DESC;
            orders.add(new org.springframework.data.domain.Sort.Order(direction, property));
        }
        if (orders.isEmpty()) {
            return org.springframework.data.domain.PageRequest.of(page, size);
        }
        return org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by(orders));
    }

    private String buildSortStatusText(TableSortState sortState, java.util.Map<String, String> labelsByKey) {
        if (sortState == null || sortState.isEmpty() || labelsByKey == null || labelsByKey.isEmpty()) {
            return "";
        }
        java.util.List<SortCriterion> criteria = sortState.snapshot();
        StringBuilder builder = new StringBuilder();
        int visibleCriteria = Math.min(criteria.size(), 2);
        for (int i = 0; i < visibleCriteria; i++) {
            SortCriterion criterion = criteria.get(i);
            String label = labelsByKey.getOrDefault(criterion.uiKey(), criterion.uiKey());
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(label);
            builder.append(criterion.direction() == javafx.scene.control.TableColumn.SortType.ASCENDING ? " ↑" : " ↓");
        }
        if (criteria.size() > visibleCriteria) {
            builder.append(", +").append(criteria.size() - visibleCriteria);
        }
        return builder.toString();
    }

    private Label createSortStatusLabel(TableSortState sortState, java.util.Map<String, String> labelsByKey) {
        Label sortStatusLabel = new Label(buildSortStatusText(sortState, labelsByKey));
        sortStatusLabel.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px; -fx-padding: 0 10 0 0;");
        return sortStatusLabel;
    }

    private <T> void installManualServerSorting(
        javafx.scene.control.TableView<T> table,
        java.util.Map<String, javafx.scene.control.TableColumn<T, ?>> columnsByKey,
        TableSortState sortState,
        Runnable onSortChanged
    ) {
        if (table == null || columnsByKey == null || columnsByKey.isEmpty() || sortState == null || onSortChanged == null) {
            return;
        }
        table.setSortPolicy(tv -> true);
        table.getColumns().forEach(column -> column.setSortable(false));

        java.util.Map<javafx.scene.control.TableColumn<?, ?>, String> keyByColumn = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, javafx.scene.control.TableColumn<T, ?>> entry : columnsByKey.entrySet()) {
            keyByColumn.put(entry.getValue(), entry.getKey());
        }

        Runnable attachHandlers = () -> {
            table.applyCss();
            table.layout();
            javafx.scene.Node headerNode = table.lookup(".column-header-background");
            if (!(headerNode instanceof javafx.scene.control.skin.TableHeaderRow headerRow)) {
                return;
            }
            attachSortHandlersRecursive(headerRow.getRootHeader(), keyByColumn, sortState, onSortChanged);
        };

        table.skinProperty().addListener((obs, oldSkin, newSkin) -> javafx.application.Platform.runLater(attachHandlers));
        javafx.application.Platform.runLater(attachHandlers);
    }

    private void attachSortHandlersRecursive(
        javafx.scene.control.skin.TableColumnHeader header,
        java.util.Map<javafx.scene.control.TableColumn<?, ?>, String> keyByColumn,
        TableSortState sortState,
        Runnable onSortChanged
    ) {
        if (header == null) {
            return;
        }

        javafx.scene.control.TableColumnBase<?, ?> columnBase = header.getTableColumn();
        if (columnBase instanceof javafx.scene.control.TableColumn<?, ?> column) {
            String uiKey = keyByColumn.get(column);
            if (uiKey != null && !Boolean.TRUE.equals(header.getProperties().get(MANUAL_SORT_HANDLER_ATTACHED_KEY))) {
                header.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_RELEASED, event -> {
                    if (event.getButton() != javafx.scene.input.MouseButton.PRIMARY || !event.isStillSincePress()) {
                        return;
                    }
                    event.consume();
                    advanceSortState(sortState, uiKey, event.isShiftDown());
                    onSortChanged.run();
                });
                header.getProperties().put(MANUAL_SORT_HANDLER_ATTACHED_KEY, Boolean.TRUE);
            }
        }

        if (header instanceof javafx.scene.control.skin.NestedTableColumnHeader nestedHeader) {
            for (javafx.scene.control.skin.TableColumnHeader childHeader : nestedHeader.getColumnHeaders()) {
                attachSortHandlersRecursive(childHeader, keyByColumn, sortState, onSortChanged);
            }
        }
    }

    private <T> Label createSortStatusLabel(javafx.scene.control.TableView<T> table) {
        Label sortStatusLabel = new Label("");
        sortStatusLabel.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px; -fx-padding: 0 10 0 0;");
        
        Runnable updateLabel = () -> {
            if (table.getSortOrder().isEmpty()) {
                sortStatusLabel.setText("");
            } else {
                javafx.scene.control.TableColumn<T, ?> col = table.getSortOrder().get(0);
                String order = col.getSortType() == javafx.scene.control.TableColumn.SortType.ASCENDING ? "Ascending" : "Descending";
                sortStatusLabel.setText(col.getText() + ": " + order);
            }
        };

        table.getSortOrder().addListener((javafx.collections.ListChangeListener<javafx.scene.control.TableColumn<T, ?>>) c -> updateLabel.run());
        
        for (javafx.scene.control.TableColumn<T, ?> col : table.getColumns()) {
            col.sortTypeProperty().addListener((obs, oldVal, newVal) -> updateLabel.run());
        }

        return sortStatusLabel;
    }

    private <T> Label createRowCountBox(javafx.scene.control.TableView<T> table) {
        Label label = new Label("Total: 0 Row(s)");
        label.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");
        
        Runnable updateLabel = () -> {
            int total = table.getItems() != null ? table.getItems().size() : 0;
            int selected = table.getSelectionModel().getSelectedItems() != null ? table.getSelectionModel().getSelectedItems().size() : 0;
            if (selected > 0) {
                label.setText("Total: " + total + " Row(s) (Selected: " + selected + ")");
            } else {
                label.setText("Total: " + total + " Row(s)");
            }
        };

        javafx.collections.ListChangeListener<T> listChangeListener = c -> updateLabel.run();
        
        table.itemsProperty().addListener((obs, oldList, newList) -> {
            if (oldList != null) oldList.removeListener(listChangeListener);
            if (newList != null) newList.addListener(listChangeListener);
            updateLabel.run();
        });
        
        if (table.getItems() != null) {
            table.getItems().addListener(listChangeListener);
        }
        
        table.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<T>) c -> updateLabel.run());
        
        updateLabel.run();
        return label;
    }

    private Label createStatusMetaLabel(String initialText) {
        Label label = new Label(initialText);
        label.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 12px;");
        return label;
    }

    private Button createPageNavButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().addAll("button", "dashboard-report-secondary-button");
        button.setStyle("-fx-padding: 4 12; -fx-background-radius: 999;");
        return button;
    }

    private void applyStandardTablePageLayout(VBox root) {
        applyStandardTablePageLayout(root, STANDARD_TABLE_PAGE_PADDING);
    }

    private void applyStandardTablePageLayout(VBox root, Insets padding) {
        root.setSpacing(STANDARD_TABLE_PAGE_SPACING);
        root.setPadding(padding);
        root.setFillWidth(true);
    }

    private void applyStandardTableSizing(javafx.scene.control.TableView<?> table) {
        table.setMinHeight(0);
        table.setMaxHeight(Double.MAX_VALUE);
        table.setPrefWidth(Double.MAX_VALUE);
        table.setMaxWidth(Double.MAX_VALUE);
    }

    private void applyStandardTableStatusBar(javafx.scene.layout.HBox statusBar) {
        statusBar.setAlignment(Pos.CENTER_RIGHT);
        statusBar.setPadding(STANDARD_TABLE_STATUS_PADDING);
        statusBar.setMaxWidth(Double.MAX_VALUE);
    }

    private void updatePagedStatus(
        javafx.scene.control.TableView<?> table,
        Label rowCountLabel,
        Label pageLabel,
        Button prevButton,
        Button nextButton,
        long totalElements,
        int currentPage,
        int totalPages,
        int pageSize
    ) {
        long safeTotal = Math.max(0, totalElements);
        int safePage = Math.max(0, currentPage);
        int safeTotalPages = Math.max(totalPages, safeTotal == 0 ? 0 : 1);
        long startRow = safeTotal == 0 ? 0 : (long) safePage * pageSize + 1;
        long endRow = safeTotal == 0 ? 0 : Math.min(safeTotal, (long) (safePage + 1) * pageSize);
        int selected = table.getSelectionModel().getSelectedItems() != null
            ? table.getSelectionModel().getSelectedItems().size()
            : 0;

        String baseText = "Showing " + startRow + "-" + endRow + " of " + safeTotal + " Row(s)";
        if (selected > 0) {
            rowCountLabel.setText(baseText + " (Selected: " + selected + ")");
        } else {
            rowCountLabel.setText(baseText);
        }

        if (safeTotalPages <= 0) {
            pageLabel.setText("Page 0 / 0");
        } else {
            pageLabel.setText("Page " + (safePage + 1) + " / " + safeTotalPages);
        }
        prevButton.setDisable(safePage <= 0 || safeTotalPages <= 1);
        nextButton.setDisable(safeTotalPages <= 1 || safePage >= safeTotalPages - 1);
    }

    private void showAlert(javafx.scene.control.Alert.AlertType type, String title, String content) {
        // Use ToastService for non-blocking notifications
        switch (type) {
            case ERROR -> toastService.showError(content);
            case WARNING -> toastService.showWarning(content);
            case INFORMATION -> toastService.showSuccess(content);
            default -> toastService.showInfo(content);
        }
    }

    private boolean showConfirmDialog(String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        
        // Custom Figma Graphic
        javafx.scene.layout.StackPane graphicContainer = new javafx.scene.layout.StackPane();
        graphicContainer.setPrefSize(48, 48);
        graphicContainer.setMaxSize(48, 48);
        graphicContainer.setStyle("-fx-background-color: -app-primary; -fx-background-radius: 50%; -fx-effect: dropshadow(three-pass-box, -app-shadow, 10, 0, 0, 4);");

        javafx.scene.shape.SVGPath questionSVG = new javafx.scene.shape.SVGPath();
        questionSVG.setContent("M11,18h2v-2h-2V18z M12,2C6.48,2,2,6.48,2,12s4.48,10,10,10s10-4.48,10-10S17.52,2,12,2z M12,20c-4.41,0-8-3.59-8-8s3.59-8,8-8s8,3.59,8,8S16.41,20,12,20z M12,6c-2.21,0-4,1.79-4,4h2c0-1.1,0.9-2,2-2s2,0.9,2,2c0,2-3,1.75-3,5h2c0-2.25,3-2.5,3-5C16,7.79,14.21,6,12,6z");
        questionSVG.setFill(SURFACE_COLOR);
        questionSVG.setScaleX(1.3);
        questionSVG.setScaleY(1.3);
        
        graphicContainer.getChildren().add(questionSVG);
        alert.setGraphic(graphicContainer);
        
        // Apply Global Styles
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/application.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("custom-alert");
        
        java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK;
    }
    private void showCheckoutDialog(
        Stage owner,
        BigDecimal subtotalAmount,
        java.util.List<PromotionService.OrderPromotionPreview> eligibleOrderPromotions,
        java.util.function.Consumer<CheckoutSelection> onConfirm
    ) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Checkout");

        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: -app-surface;");

        Label title = new Label("Payment");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");

        Label subtotalLbl = new Label("Subtotal: " + formatVnd(subtotalAmount));
        subtotalLbl.setStyle("-fx-font-size: 16px; -fx-text-fill: -app-text-secondary; -fx-font-weight: 600;");

        Label promotionDiscountLbl = new Label("Promotion Discount: " + formatVnd(BigDecimal.ZERO));
        promotionDiscountLbl.setStyle("-fx-font-size: 15px; -fx-text-fill: -app-primary; -fx-font-weight: 600;");

        Label totalLbl = new Label("Total to Pay: " + formatVnd(subtotalAmount));
        totalLbl.setStyle("-fx-font-size: 18px; -fx-text-fill: -app-danger-hover; -fx-font-weight: bold;");

        // Payment Method
        javafx.scene.control.ComboBox<com.pbl3.project.pbl3_project.entity.PaymentMethod> methodCombo = new javafx.scene.control.ComboBox<>();
        methodCombo.getItems().addAll(com.pbl3.project.pbl3_project.entity.PaymentMethod.values());
        methodCombo.setValue(com.pbl3.project.pbl3_project.entity.PaymentMethod.CASH);
        methodCombo.setStyle("-fx-font-size: 14px; -fx-pref-width: 250px;");

        javafx.scene.control.ComboBox<PromotionService.OrderPromotionPreview> orderPromotionCombo = new javafx.scene.control.ComboBox<>();
        orderPromotionCombo.getItems().add(null);
        if (eligibleOrderPromotions != null) {
            orderPromotionCombo.getItems().addAll(eligibleOrderPromotions);
        }
        orderPromotionCombo.setValue(null);
        orderPromotionCombo.setPrefWidth(250);
        javafx.util.StringConverter<PromotionService.OrderPromotionPreview> promotionConverter = new javafx.util.StringConverter<>() {
            @Override
            public String toString(PromotionService.OrderPromotionPreview value) {
                return value == null ? "No order promotion" : value.displayLabel();
            }

            @Override
            public PromotionService.OrderPromotionPreview fromString(String string) {
                return null;
            }
        };
        orderPromotionCombo.setConverter(promotionConverter);
        orderPromotionCombo.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(PromotionService.OrderPromotionPreview item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : promotionConverter.toString(item));
            }
        });
        orderPromotionCombo.setCellFactory(listView -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(PromotionService.OrderPromotionPreview item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : promotionConverter.toString(item));
            }
        });

        // Cash Input
        VBox cashBox = new VBox(10);
        cashBox.setAlignment(Pos.CENTER_LEFT);
        TextField givenField = new TextField();
        givenField.setPromptText("Amount Given");
        givenField.setStyle("-fx-font-size: 14px; -fx-pref-width: 250px;");
        
        Label changeLbl = new Label("Change: " + formatVnd(BigDecimal.ZERO));
        changeLbl.setStyle("-fx-font-size: 16px; -fx-text-fill: -app-success-hover; -fx-font-weight: bold;");

        cashBox.getChildren().addAll(new Label("Amount Given:"), givenField, changeLbl);

        javafx.scene.control.CheckBox printReceiptCb = new javafx.scene.control.CheckBox("Print Receipt (PDF)");
        printReceiptCb.setSelected(true);
        printReceiptCb.setStyle("-fx-font-size: 14px; -fx-text-fill: -app-text-primary;");

        Button confirmBtn = new Button("PAY & PRINT");
        confirmBtn.getStyleClass().addAll("button", "success-button");
        confirmBtn.setDisable(true);
        confirmBtn.setPrefWidth(250);

        java.util.function.Supplier<BigDecimal> effectiveTotalSupplier = () -> {
            PromotionService.OrderPromotionPreview selectedPromotion = orderPromotionCombo.getValue();
            return selectedPromotion != null ? selectedPromotion.discountedTotal() : subtotalAmount;
        };

        // Logic
        Runnable updateState = () -> {
             PromotionService.OrderPromotionPreview selectedPromotion = orderPromotionCombo.getValue();
             BigDecimal discountAmount = selectedPromotion != null ? selectedPromotion.discountAmount() : MoneySupport.ZERO;
             BigDecimal effectiveTotal = effectiveTotalSupplier.get();
             promotionDiscountLbl.setText("Promotion Discount: " + formatVnd(discountAmount));
             totalLbl.setText("Total to Pay: " + formatVnd(effectiveTotal));
             boolean isCash = methodCombo.getValue() == com.pbl3.project.pbl3_project.entity.PaymentMethod.CASH;
             cashBox.setVisible(isCash);
             cashBox.setManaged(isCash);
             if (!isCash) {
                 confirmBtn.setDisable(false);
             } else {
                 try {
                     BigDecimal given = parseMoneyInput(givenField.getText());
                     BigDecimal change = MoneySupport.subtract(given, effectiveTotal);
                     changeLbl.setText("Change: " + formatVnd(change));
                     if (given.compareTo(effectiveTotal) >= 0) {
                         confirmBtn.setDisable(false);
                         changeLbl.setStyle("-fx-font-size: 16px; -fx-text-fill: -app-success-hover; -fx-font-weight: bold;");
                     } else {
                         confirmBtn.setDisable(true);
                         changeLbl.setStyle("-fx-font-size: 16px; -fx-text-fill: -app-danger-hover; -fx-font-weight: bold;");
                     }
                 } catch (Exception e) {
                     confirmBtn.setDisable(true);
                     changeLbl.setText("Change: " + formatVnd(BigDecimal.ZERO));
                 }
             }
        };

        methodCombo.setOnAction(e -> updateState.run());
        orderPromotionCombo.setOnAction(e -> updateState.run());

        // Validation
        givenField.textProperty().addListener((obs, old, val) -> {
            try {
                BigDecimal given = parseMoneyInput(val);
                BigDecimal change = MoneySupport.subtract(given, effectiveTotalSupplier.get());
                changeLbl.setText("Change: " + formatVnd(change));
                if (change.signum() >= 0) {
                    confirmBtn.setDisable(false);
                    changeLbl.setStyle("-fx-font-size: 16px; -fx-text-fill: -app-success-hover; -fx-font-weight: bold;");
                } else {
                    confirmBtn.setDisable(true);
                    changeLbl.setStyle("-fx-font-size: 16px; -fx-text-fill: -app-danger-hover; -fx-font-weight: bold;");
                }
            } catch (NumberFormatException e) {
                confirmBtn.setDisable(true);
                changeLbl.setText("Invalid Amount");
            }
        });

        // Initialize state
        updateState.run();

        confirmBtn.setOnAction(e -> {
            PromotionService.OrderPromotionPreview selectedPromotion = orderPromotionCombo.getValue();
            onConfirm.accept(new CheckoutSelection(
                methodCombo.getValue(),
                printReceiptCb.isSelected(),
                selectedPromotion != null && selectedPromotion.promotion() != null
                    ? selectedPromotion.promotion().getId()
                    : null
            ));
            dialog.close();
        });
        
        Button cancelBtn = new Button("CANCEL");
        cancelBtn.getStyleClass().addAll("button", "checkout-cancel-button");
        cancelBtn.setStyle("-fx-background-color: -app-border; -fx-text-fill: -app-text-primary;");
        cancelBtn.setPrefWidth(250);
        cancelBtn.setOnAction(e -> dialog.close());

        root.getChildren().addAll(
            title,
            subtotalLbl,
            new Label("Order Promotion"),
            orderPromotionCombo,
            promotionDiscountLbl,
            totalLbl,
            methodCombo,
            cashBox,
            printReceiptCb,
            confirmBtn,
            cancelBtn
        );

        Scene scene = new Scene(root, 430, 590);
        if (owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private javafx.scene.layout.VBox createImportOrderView(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        final String importSortStateKey = "import-goods";
        TableSortState importSortState = getOrCreateTableSortState(
            importSortStateKey,
            new SortCriterion("createdAt", javafx.scene.control.TableColumn.SortType.DESCENDING)
        );
        java.util.LinkedHashMap<String, String> importSortProperties = new java.util.LinkedHashMap<>();
        importSortProperties.put("id", "id");
        importSortProperties.put("supplier", "supplierNameSnapshot");
        importSortProperties.put("createdAt", "createdAt");
        importSortProperties.put("totalCost", "totalCost");
        importSortProperties.put("status", "status");
        VBox root = new VBox();
        applyStandardTablePageLayout(root);
        root.setStyle("-fx-background-color: transparent;");

        // Toolbar
        javafx.scene.layout.BorderPane toolbar = new javafx.scene.layout.BorderPane();
        Label title = new Label("Import Goods");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");

        // Expandable Search Bar for Import Orders
        javafx.scene.layout.HBox iSearchBox = new javafx.scene.layout.HBox(0);
        iSearchBox.setAlignment(Pos.CENTER);
        iSearchBox.getStyleClass().add("expandable-search-box");
        iSearchBox.setPrefSize(40, 40); iSearchBox.setMinSize(40, 40); iSearchBox.setMaxSize(40, 40);
        javafx.scene.shape.SVGPath iIcon = new javafx.scene.shape.SVGPath();
        iIcon.setContent("M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
        iIcon.setFill(PRIMARY_COLOR);
        javafx.scene.layout.Region iSpacer = new javafx.scene.layout.Region();
        iSpacer.setMinWidth(0); iSpacer.setPrefWidth(0);
        TextField iField = new TextField();
        iField.setPromptText(DEFAULT_SEARCH_PROMPT); iField.getStyleClass().add("search-text-field");
        iField.setMinWidth(0); iField.setMaxWidth(0); iField.setPrefWidth(0); iField.setOpacity(0);
        iSearchBox.getChildren().addAll(iIcon, iSpacer, iField);
        javafx.animation.Timeline iExpand = new javafx.animation.Timeline(new javafx.animation.KeyFrame(Duration.millis(150),
            new javafx.animation.KeyValue(iSearchBox.maxWidthProperty(), 250, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(iSearchBox.prefWidthProperty(), 250, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(iSpacer.minWidthProperty(), 8, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(iField.minWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(iField.maxWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(iField.prefWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(iField.opacityProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH)));
        javafx.animation.Timeline iCollapse = new javafx.animation.Timeline(new javafx.animation.KeyFrame(Duration.millis(150),
            new javafx.animation.KeyValue(iSearchBox.maxWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(iSearchBox.prefWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(iSpacer.minWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(iField.minWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(iField.maxWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(iField.prefWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(iField.opacityProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH)));
        iSearchBox.setOnMouseClicked(ev -> {
            if (iSearchBox.getMaxWidth() == 40) { iExpand.play(); iField.requestFocus(); }
            else if (ev.getTarget() == iIcon || ev.getTarget() == iSearchBox) { iField.clear(); root.requestFocus(); iCollapse.play(); }
        });

        // Filter Button (Supplier)
        javafx.scene.layout.HBox iFilterBox = new javafx.scene.layout.HBox();
        iFilterBox.setAlignment(Pos.CENTER);
        iFilterBox.getStyleClass().add("expandable-search-box");
        iFilterBox.setPrefSize(40, 40); iFilterBox.setMinSize(40, 40); iFilterBox.setMaxSize(40, 40);
        iFilterBox.setCursor(javafx.scene.Cursor.HAND);
        javafx.scene.shape.SVGPath iFilterIcon = new javafx.scene.shape.SVGPath();
        iFilterIcon.setContent("M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z");
        iFilterIcon.setFill(PRIMARY_COLOR);
        iFilterBox.getChildren().add(iFilterIcon);

        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.ImportOrder> table = new javafx.scene.control.TableView<>();
        applyStandardTableSizing(table);
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        enableDragSelection(table);

        final int importPageSize = 20;
        final int[] importCurrentPage = {0};
        final int[] importTotalPages = {0};
        final long[] importTotalElements = {0L};
        java.util.concurrent.atomic.AtomicReference<String> importSearchRef = new java.util.concurrent.atomic.AtomicReference<>("");
        java.util.concurrent.atomic.AtomicReference<java.time.LocalDate> importStartDateRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<java.time.LocalDate> importEndDateRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<java.util.Set<Long>> importSuppliersRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<java.util.Set<com.pbl3.project.pbl3_project.entity.ImportOrderStatus>> importStatusesRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<BigDecimal> importMinTotalRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<BigDecimal> importMaxTotalRef = new java.util.concurrent.atomic.AtomicReference<>(null);

        Label importRowCountLabel = createStatusMetaLabel("Showing 0-0 of 0 Row(s)");
        Label importPageLabel = createStatusMetaLabel("Page 0 / 0");
        Button importPrevBtn = createPageNavButton("Prev");
        Button importNextBtn = createPageNavButton("Next");

        Runnable[] refreshImportTableRef = new Runnable[1];
        Runnable updateImportStatusBar = () -> updatePagedStatus(
            table,
            importRowCountLabel,
            importPageLabel,
            importPrevBtn,
            importNextBtn,
            importTotalElements[0],
            importCurrentPage[0],
            importTotalPages[0],
            importPageSize
        );
        Runnable loadImportPage = () -> {
            org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.ImportOrder> pageData =
                importOrderService.searchImportOrders(
                    importSearchRef.get(),
                    importStartDateRef.get(),
                    importEndDateRef.get(),
                    importSuppliersRef.get(),
                    importStatusesRef.get(),
                    importMinTotalRef.get(),
                    importMaxTotalRef.get(),
                    createPageable(importSortState, importSortProperties, importCurrentPage[0], importPageSize)
                );
            if (pageData.getTotalPages() > 0 && importCurrentPage[0] >= pageData.getTotalPages()) {
                importCurrentPage[0] = pageData.getTotalPages() - 1;
                pageData = importOrderService.searchImportOrders(
                    importSearchRef.get(),
                    importStartDateRef.get(),
                    importEndDateRef.get(),
                    importSuppliersRef.get(),
                    importStatusesRef.get(),
                    importMinTotalRef.get(),
                    importMaxTotalRef.get(),
                    createPageable(importSortState, importSortProperties, importCurrentPage[0], importPageSize)
                );
            }
            importTotalElements[0] = pageData.getTotalElements();
            importTotalPages[0] = pageData.getTotalPages();
            table.setItems(javafx.collections.FXCollections.observableArrayList(pageData.getContent()));
            updateImportStatusBar.run();
        };
        refreshImportTableRef[0] = loadImportPage;
        importPrevBtn.setOnAction(e -> {
            if (importCurrentPage[0] > 0) {
                importCurrentPage[0]--;
                loadImportPage.run();
            }
        });
        importNextBtn.setOnAction(e -> {
            if (importCurrentPage[0] + 1 < importTotalPages[0]) {
                importCurrentPage[0]++;
                loadImportPage.run();
            }
        });

        Button manageImportBtn = createExpandableManageActionButton("Manage Import", 170);
        manageImportBtn.setDisable(true);
        manageImportBtn.setOnAction(e -> {
            java.util.List<com.pbl3.project.pbl3_project.entity.ImportOrder> selectedImports =
                new java.util.ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selectedImports.size() != 1) {
                toastService.showWarning("Select exactly one import order to manage");
                return;
            }
            try {
                showImportOrderDetailsDialog(
                    stage,
                    importOrderService.getImportOrderWithItems(selectedImports.get(0).getId()),
                    user,
                    refreshImportTableRef[0]
                );
            } catch (Exception ex) {
                showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Error", "Could not load import order details: " + ex.getMessage());
            }
        });

        Button createBtn = createExpandableGreenActionButton("New Import", 140);
        createBtn.setOnAction(e -> showCreateImportDialog(stage, user, refreshImportTableRef[0], null));

        javafx.scene.layout.HBox rightBox = new javafx.scene.layout.HBox(15, iFilterBox, iSearchBox, manageImportBtn, createBtn);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        toolbar.setLeft(title);
        toolbar.setRight(rightBox);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.ImportOrder, String> idCol = new javafx.scene.control.TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty("IMP-" + data.getValue().getId()));
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.ImportOrder, String> suppCol = new javafx.scene.control.TableColumn<>("Supplier");
        suppCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getSupplierDisplayName()));
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.ImportOrder, String> dateCol = new javafx.scene.control.TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatDateTime(data.getValue().getCreatedAt())));
        
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.ImportOrder, String> costCol = new javafx.scene.control.TableColumn<>("Total Cost");
        costCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.format("%,.0f VND", data.getValue().getTotalCost())));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.ImportOrder, String> statusCol = new javafx.scene.control.TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatImportOrderStatus(data.getValue().getStatus())));
        statusCol.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                com.pbl3.project.pbl3_project.entity.ImportOrder order = getTableRow() != null ? getTableRow().getItem() : null;
                setStyle("-fx-text-fill: " + getImportOrderStatusColor(order != null ? order.getStatus() : null)
                    + "; -fx-font-weight: bold; -fx-alignment: CENTER;");
            }
        });

        table.getColumns().addAll(idCol, suppCol, dateCol, costCol, statusCol);
        java.util.LinkedHashMap<String, javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.ImportOrder, ?>> importSortColumns =
            new java.util.LinkedHashMap<>();
        importSortColumns.put("id", idCol);
        importSortColumns.put("supplier", suppCol);
        importSortColumns.put("createdAt", dateCol);
        importSortColumns.put("totalCost", costCol);
        importSortColumns.put("status", statusCol);
        installSortHeaderIndicators(importSortColumns);
        java.util.LinkedHashMap<String, String> importSortLabels = new java.util.LinkedHashMap<>();
        importSortLabels.put("id", "ID");
        importSortLabels.put("supplier", "Supplier");
        importSortLabels.put("createdAt", "Date");
        importSortLabels.put("totalCost", "Total Cost");
        importSortLabels.put("status", "Status");
        Label importSortStatusLabel = createSortStatusLabel(importSortState, importSortLabels);
        Runnable applyImportSortUi = () -> {
            applySortStateToTable(table, importSortColumns, importSortState);
            importSortStatusLabel.setText(buildSortStatusText(importSortState, importSortLabels));
        };
        applyImportSortUi.run();
        installManualServerSorting(
            table,
            importSortColumns,
            importSortState,
            () -> {
                applyImportSortUi.run();
                importCurrentPage[0] = 0;
                loadImportPage.run();
            }
        );

        table.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<com.pbl3.project.pbl3_project.entity.ImportOrder>) c ->
            manageImportBtn.setDisable(table.getSelectionModel().getSelectedItems().size() != 1)
        );

        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<com.pbl3.project.pbl3_project.entity.ImportOrder> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    try {
                        showImportOrderDetailsDialog(stage, importOrderService.getImportOrderWithItems(row.getItem().getId()), user, refreshImportTableRef[0]);
                    } catch (Exception ex) {
                        showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Error", "Could not load import order details: " + ex.getMessage());
                    }
                }
            });
            return row;
        });

        javafx.animation.PauseTransition importSearchPause = new javafx.animation.PauseTransition(Duration.millis(220));
        importSearchPause.setOnFinished(e -> {
            importCurrentPage[0] = 0;
            importSearchRef.set(iField.getText());
            loadImportPage.run();
        });
        iField.textProperty().addListener((obs, oldV, newV) -> importSearchPause.playFromStart());

        javafx.scene.layout.HBox importStatusBar = new javafx.scene.layout.HBox(15, importSortStatusLabel, importRowCountLabel, importPageLabel, importPrevBtn, importNextBtn);
        applyStandardTableStatusBar(importStatusBar);

        // Wire up supplier filter
        javafx.stage.Popup iFilterPopup = new javafx.stage.Popup();
        iFilterPopup.setAutoHide(true);

        iFilterBox.setOnMouseClicked(fev -> {
            fev.consume();
            try {
                if (iFilterPopup.isShowing()) {
                    iFilterPopup.hide();
                    return;
                }

                VBox popupContainer = new VBox(10);
                popupContainer.setPadding(new Insets(15));
                applyFilterPopupContainerStyle(popupContainer);
                popupContainer.setPrefWidth(350);

                VBox scrollContent = new VBox(10);
                scrollContent.setStyle("-fx-background-color: -app-surface;");
                scrollContent.setPadding(new Insets(5, 15, 5, 15));
                javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(scrollContent);
                scrollPane.setFitToWidth(true);
                scrollPane.setStyle("-fx-background: -app-surface; -fx-background-color: transparent; -fx-border-color: transparent;");
                scrollPane.setPrefViewportHeight(350);

            // --- Date Range ---
            Label dateTitle = new Label("Date Range");
            dateTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");
            javafx.scene.control.DatePicker startDatePicker = new javafx.scene.control.DatePicker();
            startDatePicker.setPromptText("Start Date");
            startDatePicker.setPrefWidth(140);
            startDatePicker.setStyle("-fx-font-size: 13px;");
            javafx.scene.control.DatePicker endDatePicker = new javafx.scene.control.DatePicker();
            endDatePicker.setPromptText("End Date");
            endDatePicker.setPrefWidth(140);
            endDatePicker.setStyle("-fx-font-size: 13px;");
            customizeDatePicker(startDatePicker);
            customizeDatePicker(endDatePicker);
            javafx.scene.layout.HBox dateBox = new javafx.scene.layout.HBox(5, startDatePicker, new Label("-"), endDatePicker);
            dateBox.setAlignment(Pos.CENTER_LEFT);

            javafx.scene.control.Separator sepDate = new javafx.scene.control.Separator();

            // --- Supplier Filter ---
            Label suppTitle = new Label("Suppliers");
            suppTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");
            
            javafx.scene.control.CheckBox allSuppCb = new javafx.scene.control.CheckBox("All Suppliers");
            allSuppCb.setSelected(true);
            allSuppCb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");

            javafx.scene.control.ScrollPane suppScroll = new javafx.scene.control.ScrollPane();
            VBox suppBox = new VBox(8);
            suppBox.setPadding(new Insets(5, 5, 5, 20));
            suppScroll.setContent(suppBox);
            suppScroll.setFitToWidth(true);
            suppScroll.setMaxHeight(140);
            suppScroll.setStyle("-fx-background-color: transparent; -fx-background: -app-surface; -fx-border-color: -app-border; -fx-border-radius: 4;");

            java.util.List<com.pbl3.project.pbl3_project.dto.IdLabelOption> supplierOptions = importOrderService.getImportSupplierOptions();

            java.util.List<javafx.scene.control.CheckBox> suppCbs = new java.util.ArrayList<>();
            for (com.pbl3.project.pbl3_project.dto.IdLabelOption option : supplierOptions) {
                if (option.label() == null || option.label().trim().isEmpty()) continue;
                javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(option.label());
                cb.setUserData(option.id());
                cb.setSelected(true);
                cb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                cb.setOnAction(e -> {
                    if (!cb.isSelected()) allSuppCb.setSelected(false);
                    else {
                        boolean all = true;
                        for (javafx.scene.control.CheckBox c : suppCbs) if (!c.isSelected()) all = false;
                        allSuppCb.setSelected(all);
                    }
                });
                suppCbs.add(cb);
                suppBox.getChildren().add(cb);
            }

            allSuppCb.setOnAction(e -> {
                boolean sel = allSuppCb.isSelected();
                for (javafx.scene.control.CheckBox cb : suppCbs) cb.setSelected(sel);
            });
            FilterDisclosureSection supplierSection = new FilterDisclosureSection(allSuppCb, suppScroll);

            javafx.scene.control.Separator sepSupp = new javafx.scene.control.Separator();

            // --- Status Filter ---
            Label statusTitle = new Label("Status");
            statusTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");

            javafx.scene.control.CheckBox allStatusesCb = new javafx.scene.control.CheckBox("All Statuses");
            allStatusesCb.setSelected(true);
            allStatusesCb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");

            VBox statusBox = new VBox(8);
            statusBox.setPadding(new Insets(5, 5, 5, 20));

            java.util.List<javafx.scene.control.CheckBox> statusCbs = new java.util.ArrayList<>();
            for (com.pbl3.project.pbl3_project.entity.ImportOrderStatus status : com.pbl3.project.pbl3_project.entity.ImportOrderStatus.values()) {
                javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(formatImportOrderStatus(status));
                cb.setUserData(status);
                cb.setSelected(true);
                cb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                cb.setOnAction(e -> {
                    if (!cb.isSelected()) allStatusesCb.setSelected(false);
                    else {
                        boolean all = true;
                        for (javafx.scene.control.CheckBox c : statusCbs) if (!c.isSelected()) all = false;
                        allStatusesCb.setSelected(all);
                    }
                });
                statusCbs.add(cb);
                statusBox.getChildren().add(cb);
            }

            allStatusesCb.setOnAction(e -> {
                boolean sel = allStatusesCb.isSelected();
                for (javafx.scene.control.CheckBox cb : statusCbs) cb.setSelected(sel);
            });
            FilterDisclosureSection statusSection = new FilterDisclosureSection(allStatusesCb, statusBox);

            javafx.scene.control.Separator sepStatus = new javafx.scene.control.Separator();

            // --- Price Range ---
            Label priceTitle = new Label("Total Cost Range");
            priceTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");
            
            BigDecimal maxPriceValue = importOrderService.getImportMaxTotalCost();
            double maxPrice = maxPriceValue == null ? 0.0 : maxPriceValue.doubleValue();
            if (maxPrice == 0) maxPrice = 1000;
            
            Label priceLabel = new Label("0 - " + String.format("%.0f", maxPrice) + " VND");
            priceLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -app-primary; -fx-font-weight: bold;");
            
            RangeSlider priceSlider = new RangeSlider(0, maxPrice, 0, maxPrice, 290);
            priceSlider.minVal.addListener((o, ov, nv) -> priceLabel.setText(String.format("%.0f - %.0f VND", nv.doubleValue(), priceSlider.maxVal.get())));
            priceSlider.maxVal.addListener((o, ov, nv) -> priceLabel.setText(String.format("%.0f - %.0f VND", priceSlider.minVal.get(), nv.doubleValue())));

            scrollContent.getChildren().addAll(
                dateTitle, dateBox, sepDate,
                suppTitle, supplierSection.getNode(), sepSupp,
                statusTitle, statusSection.getNode(), sepStatus,
                priceTitle, priceLabel, priceSlider
            );

            // --- Buttons ---
            javafx.scene.layout.HBox btnRow = new javafx.scene.layout.HBox(10);
            btnRow.setAlignment(Pos.CENTER_RIGHT);
            
            final double fMaxPrice = maxPrice;

            Button resetBtn = new Button("Reset");
            resetBtn.getStyleClass().add("filter-reset-btn");
            resetBtn.setOnAction(ae -> {
                iFilterBox.setStyle("");
                startDatePicker.setValue(null);
                endDatePicker.setValue(null);
                allSuppCb.setSelected(true);
                for (javafx.scene.control.CheckBox cb : suppCbs) cb.setSelected(true);
                supplierSection.setExpanded(false);
                allStatusesCb.setSelected(true);
                for (javafx.scene.control.CheckBox cb : statusCbs) cb.setSelected(true);
                statusSection.setExpanded(false);
                priceSlider.minVal.set(0); priceSlider.maxVal.set(fMaxPrice);
                importStartDateRef.set(null);
                importEndDateRef.set(null);
                importSuppliersRef.set(new java.util.LinkedHashSet<>());
                importStatusesRef.set(new java.util.LinkedHashSet<>());
                importMinTotalRef.set(null);
                importMaxTotalRef.set(null);
                importCurrentPage[0] = 0;
                loadImportPage.run();
                iFilterPopup.hide();
            });

            Button applyBtn = new Button("Apply Filter");
            applyBtn.getStyleClass().add("filter-apply-btn");
            applyBtn.setOnAction(ae -> {
                java.util.Set<Long> selectedSupps = new java.util.HashSet<>();
                for (javafx.scene.control.CheckBox cb : suppCbs) {
                    if (cb.isSelected() && cb.getUserData() instanceof Long supplierId) {
                        selectedSupps.add(supplierId);
                    }
                }
                java.util.Set<com.pbl3.project.pbl3_project.entity.ImportOrderStatus> selectedStatuses = new java.util.HashSet<>();
                for (javafx.scene.control.CheckBox cb : statusCbs) {
                    if (cb.isSelected()) {
                        selectedStatuses.add((com.pbl3.project.pbl3_project.entity.ImportOrderStatus) cb.getUserData());
                    }
                }
                double pMin = priceSlider.minVal.get();
                double pMax = priceSlider.maxVal.get();
                java.time.LocalDate sDate = startDatePicker.getValue();
                java.time.LocalDate eDate = endDatePicker.getValue();

                importStartDateRef.set(sDate);
                importEndDateRef.set(eDate);
                importSuppliersRef.set(allSuppCb.isSelected() ? new java.util.LinkedHashSet<>() : selectedSupps);
                importStatusesRef.set(allStatusesCb.isSelected() ? new java.util.LinkedHashSet<>() : selectedStatuses);
                importMinTotalRef.set(pMin <= 0 ? null : MoneySupport.normalize(BigDecimal.valueOf(pMin)));
                importMaxTotalRef.set(pMax >= fMaxPrice ? null : MoneySupport.normalize(BigDecimal.valueOf(pMax)));
                importCurrentPage[0] = 0;
                loadImportPage.run();

                boolean hasFilter = !allSuppCb.isSelected()
                    || !allStatusesCb.isSelected()
                    || pMin > 0
                    || pMax < fMaxPrice
                    || sDate != null
                    || eDate != null;
                iFilterBox.setStyle(hasFilter ? "-fx-border-color: -app-primary;" : "");
                iFilterPopup.hide();
            });

            btnRow.getChildren().addAll(resetBtn, applyBtn);

                popupContainer.getChildren().addAll(scrollPane, btnRow);
                iFilterPopup.getContent().clear();
                iFilterPopup.getContent().add(popupContainer);

                showPopupBelow(iFilterPopup, iFilterBox, -290, 5);
            } catch (Exception ex) {
                showUserFacingError(ex);
            }
        });

        root.getChildren().addAll(toolbar, table, importStatusBar);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);

        table.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<com.pbl3.project.pbl3_project.entity.ImportOrder>) c -> updateImportStatusBar.run());
        loadImportPage.run();
        enableDeselectOnOutsideClick(root, table);
        ImportOrderPrefill prefill = pendingImportOrderPrefill;
        pendingImportOrderPrefill = null;
        if (prefill != null) {
            javafx.application.Platform.runLater(() -> showCreateImportDialog(stage, user, refreshImportTableRef[0], prefill));
        }
        return root;
    }

    private void showCreateImportDialog(Stage owner, com.pbl3.project.pbl3_project.entity.User user, Runnable onSuccess, ImportOrderPrefill prefill) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("New Import Order");

        VBox root = new VBox(16);
        root.getStyleClass().add("dialog-root");
        root.setPrefWidth(940);
        root.setPrefHeight(650);

        Label titleLabel = new Label("New Import");
        titleLabel.getStyleClass().add("dialog-title");
        titleLabel.setPadding(Insets.EMPTY);

        // Top: Supplier
        javafx.scene.layout.HBox topBox = new javafx.scene.layout.HBox(10);
        topBox.setAlignment(Pos.CENTER_LEFT);
        Label suppLbl = new Label("Select Supplier:");
        suppLbl.setStyle("-fx-font-weight: bold;");
        javafx.scene.control.ComboBox<com.pbl3.project.pbl3_project.entity.Supplier> supplierCombo = new javafx.scene.control.ComboBox<>();
        supplierCombo.setPrefWidth(340);
        supplierCombo.setPrefHeight(42);
        supplierCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(com.pbl3.project.pbl3_project.entity.Supplier s) { return s == null ? "" : s.getName(); }
            @Override public com.pbl3.project.pbl3_project.entity.Supplier fromString(String s) { return null; }
        });
        supplierCombo.getItems().addAll(supplierService.getAllSuppliers());
        topBox.getChildren().addAll(suppLbl, supplierCombo);

        VBox supplierCard = new VBox(topBox);
        supplierCard.getStyleClass().add("report-section-card");
        supplierCard.setPadding(new Insets(16));

        class TempItem {
            com.pbl3.project.pbl3_project.entity.Product product;
            int quantity;
            BigDecimal importPrice;
            TempItem(com.pbl3.project.pbl3_project.entity.Product p, int q, BigDecimal ip) { this.product=p; this.quantity=q; this.importPrice=ip; }
            public BigDecimal getTotal() { return MoneySupport.multiply(importPrice, quantity); }
        }

        javafx.scene.control.TableView<TempItem> table = new javafx.scene.control.TableView<>();
        prepareNonReorderableTable(table);
        Label totalLabel = new Label("Total Cost: 0 VND");
        totalLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: -app-danger-hover;");

        Runnable updateTotalAction = () -> {
            BigDecimal total = table.getItems().stream()
                .map(TempItem::getTotal)
                .reduce(BigDecimal.ZERO, MoneySupport::add);
            totalLabel.setText("Total Cost: " + formatVnd(total));
        };

        javafx.scene.control.TableColumn<TempItem, String> nameCol = new javafx.scene.control.TableColumn<>("Product");
        nameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().product.getName()));
        
        javafx.scene.control.TableColumn<TempItem, Integer> qtyCol = new javafx.scene.control.TableColumn<>("Qty");
        qtyCol.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().quantity));
        
        javafx.scene.control.TableColumn<TempItem, String> priceCol = new javafx.scene.control.TableColumn<>("Unit Price");
        priceCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatVnd(data.getValue().importPrice)));

        javafx.scene.control.TableColumn<TempItem, String> totalCol = new javafx.scene.control.TableColumn<>("Total");
        totalCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatVnd(data.getValue().getTotal())));

        javafx.scene.control.TableColumn<TempItem, Void> actionCol = new javafx.scene.control.TableColumn<>("Action");
        actionCol.setCellFactory(param -> new javafx.scene.control.TableCell<>() {
            private final Button btn = new Button("Remove");
            {
                btn.getStyleClass().addAll("button", "danger-button");
                btn.setOnAction(e -> {
                    TempItem item = getTableView().getItems().get(getIndex());
                    table.getItems().remove(item);
                    updateTotalAction.run();
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
        table.getColumns().addAll(nameCol, qtyCol, priceCol, totalCol, actionCol);
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);

        // Add Item Form
        javafx.scene.layout.VBox addBox = new javafx.scene.layout.VBox(12);
        addBox.getStyleClass().add("report-section-card");
        addBox.setPadding(new Insets(16));

        java.util.List<com.pbl3.project.pbl3_project.entity.Category> availableCategories =
            categoryService.getAllCategories().stream()
                .sorted(java.util.Comparator.comparing(
                    com.pbl3.project.pbl3_project.entity.Category::getName,
                    String.CASE_INSENSITIVE_ORDER
                ))
                .toList();

        java.util.List<com.pbl3.project.pbl3_project.entity.Product> availableProducts =
            productService.getAllProducts().stream()
                .sorted(java.util.Comparator.comparing(
                    com.pbl3.project.pbl3_project.entity.Product::getName,
                    String.CASE_INSENSITIVE_ORDER
                ))
                .toList();

        javafx.scene.control.ComboBox<com.pbl3.project.pbl3_project.entity.Category> categoryCombo = new javafx.scene.control.ComboBox<>();
        categoryCombo.setPrefWidth(210);
        categoryCombo.setPrefHeight(42);
        categoryCombo.setPromptText("Select Category");
        categoryCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(com.pbl3.project.pbl3_project.entity.Category c) { return c == null ? "" : c.getName(); }
            @Override public com.pbl3.project.pbl3_project.entity.Category fromString(String string) { return null; }
        });
        categoryCombo.getItems().addAll(availableCategories);

        javafx.scene.control.ComboBox<com.pbl3.project.pbl3_project.entity.Product> productCombo = new javafx.scene.control.ComboBox<>();
        productCombo.setPrefWidth(360);
        productCombo.setPrefHeight(42);
        productCombo.setPromptText(DEFAULT_SEARCH_PROMPT);
        productCombo.setEditable(true);
        productCombo.setVisibleRowCount(10);
        productCombo.getEditor().setPromptText(DEFAULT_SEARCH_PROMPT);
        productCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(com.pbl3.project.pbl3_project.entity.Product p) { return p == null ? "" : p.getName() + " (Stock: " + p.getQuantity() + ")"; }
            @Override public com.pbl3.project.pbl3_project.entity.Product fromString(String string) { return null; }
        });
        productCombo.setDisable(true);

        TextField qtyField = new TextField(); qtyField.setPromptText("Quantity"); qtyField.setPrefWidth(120); qtyField.setPrefHeight(42);
        TextField priceField = new TextField(); priceField.setPromptText("Import Price"); priceField.setPrefWidth(160); priceField.setPrefHeight(42);
        final boolean[] syncingProductEditor = {false};
        final String[] productSearchQuery = {""};

        Runnable refreshProductChoices = () -> {
            com.pbl3.project.pbl3_project.entity.Category selectedCategory = categoryCombo.getValue();
            com.pbl3.project.pbl3_project.entity.Product selectedProduct = productCombo.getValue();
            String normalizedSearch = productSearchQuery[0] == null
                ? ""
                : productSearchQuery[0].trim().toLowerCase();

            java.util.List<com.pbl3.project.pbl3_project.entity.Product> filteredProducts =
                selectedCategory == null
                    ? java.util.List.of()
                    : availableProducts.stream()
                        .filter(product -> product.getCategory() != null
                            && java.util.Objects.equals(product.getCategory().getId(), selectedCategory.getId()))
                        .filter(product -> normalizedSearch.isEmpty()
                            || product.getName().toLowerCase().contains(normalizedSearch)
                            || (product.getSku() != null && product.getSku().toLowerCase().contains(normalizedSearch)))
                        .toList();

            syncingProductEditor[0] = true;
            productCombo.getItems().setAll(filteredProducts);

            boolean keepSelection = selectedProduct != null && filteredProducts.stream()
                .anyMatch(product -> java.util.Objects.equals(product.getId(), selectedProduct.getId()));

            if (keepSelection) {
                productCombo.setValue(selectedProduct);
                productCombo.getEditor().setText(productCombo.getConverter().toString(selectedProduct));
            } else {
                productCombo.setValue(null);
                productCombo.getEditor().setText(selectedCategory == null ? "" : productSearchQuery[0]);
                priceField.clear();
            }

            boolean categorySelected = selectedCategory != null;
            productCombo.setDisable(!categorySelected);
            if (productCombo.isDisable() && selectedCategory == null) {
                productCombo.getEditor().clear();
            }
            syncingProductEditor[0] = false;
        };

        categoryCombo.setOnAction(e -> {
            productSearchQuery[0] = "";
            refreshProductChoices.run();
        });

        Runnable openProductDropdown = () -> {
            if (productCombo.isDisable()) {
                return;
            }
            if (productCombo.getValue() == null && productCombo.getEditor().getText().isBlank()) {
                productSearchQuery[0] = "";
                refreshProductChoices.run();
            }
            if (!productCombo.getItems().isEmpty()) {
                productCombo.show();
            }
        };

        productCombo.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (syncingProductEditor[0]) {
                return;
            }
            if (productCombo.getValue() != null) {
                String renderedSelection = productCombo.getConverter().toString(productCombo.getValue());
                if (java.util.Objects.equals(renderedSelection, newValue)) {
                    return;
                }
            }
            productSearchQuery[0] = newValue == null ? "" : newValue;
            if (productCombo.getValue() != null) {
                productCombo.setValue(null);
                priceField.clear();
            }
            refreshProductChoices.run();
            if (!productCombo.isDisable() && !productCombo.getItems().isEmpty()) {
                productCombo.show();
            }
        });

        productCombo.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
            if (productCombo.isDisable()) {
                return;
            }
            if (productCombo.isShowing()) {
                productCombo.hide();
                e.consume();
                return;
            }
            productCombo.requestFocus();
            productCombo.getEditor().requestFocus();
            javafx.application.Platform.runLater(openProductDropdown);
            e.consume();
        });

        productCombo.setOnAction(e -> {
            com.pbl3.project.pbl3_project.entity.Product p = productCombo.getValue();
            if (syncingProductEditor[0]) {
                return;
            }
            if (p != null) {
                productSearchQuery[0] = "";
                syncingProductEditor[0] = true;
                productCombo.getEditor().setText(productCombo.getConverter().toString(p));
                syncingProductEditor[0] = false;
                if (p.getImportPrice() != null) {
                    priceField.setText(formatWholeNumberText(p.getImportPrice()));
                } else {
                    priceField.clear();
                }
            }
        });

        Button addBtn = new Button("Add Item");
        addBtn.getStyleClass().addAll("button", "secondary-button");
        addBtn.setPrefWidth(130);
        addBtn.setPrefHeight(42);
        addBtn.setOnAction(e -> {
            if (categoryCombo.getValue() == null) { toastService.showWarning("Select a category"); return; }
            com.pbl3.project.pbl3_project.entity.Product p = productCombo.getValue();
            if (p == null) { toastService.showWarning("Select a product"); return; }
            try {
                int q = Integer.parseInt(qtyField.getText());
                BigDecimal pr = parseMoneyInput(priceField.getText());
                if (q <= 0 || pr.signum() < 0) throw new NumberFormatException();
                table.getItems().add(new TempItem(p, q, pr));
                updateTotalAction.run();
                productSearchQuery[0] = "";
                productCombo.setValue(null);
                syncingProductEditor[0] = true;
                productCombo.getEditor().clear();
                syncingProductEditor[0] = false;
                qtyField.clear();
                priceField.clear();
            } catch (Exception ex) {
                toastService.showError("Invalid quantity or price");
            }
        });
        javafx.scene.layout.HBox productSelectionRow = new javafx.scene.layout.HBox(10, categoryCombo, productCombo);
        productSelectionRow.setAlignment(Pos.CENTER_LEFT);
        javafx.scene.layout.HBox.setHgrow(productCombo, javafx.scene.layout.Priority.ALWAYS);

        javafx.scene.layout.HBox productDetailsRow = new javafx.scene.layout.HBox(10, qtyField, priceField, addBtn);
        productDetailsRow.setAlignment(Pos.CENTER_LEFT);

        addBox.getChildren().addAll(productSelectionRow, productDetailsRow);

        if (prefill != null && prefill.productId() != null) {
            com.pbl3.project.pbl3_project.entity.Product prefillProduct = availableProducts.stream()
                .filter(product -> java.util.Objects.equals(product.getId(), prefill.productId()))
                .findFirst()
                .orElse(null);

            if (prefillProduct == null || prefillProduct.getCategory() == null) {
                toastService.showWarning("The suggested product is no longer available for import");
            } else {
                categoryCombo.setValue(prefillProduct.getCategory());
                productSearchQuery[0] = "";
                refreshProductChoices.run();
                productCombo.setValue(prefillProduct);
                syncingProductEditor[0] = true;
                productCombo.getEditor().setText(productCombo.getConverter().toString(prefillProduct));
                syncingProductEditor[0] = false;
                qtyField.setText(String.valueOf(Math.max(1, prefill.quantity())));
                if (prefillProduct.getImportPrice() != null) {
                    priceField.setText(formatWholeNumberText(prefillProduct.getImportPrice()));
                } else {
                    priceField.clear();
                }
            }
        }

        // Bottom Actions
        javafx.scene.layout.HBox actionBox = new javafx.scene.layout.HBox(15);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        
        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("button");
        cancelBtn.setOnAction(e -> dialog.close());

        Button confirmBtn = new Button("Confirm Import");
        confirmBtn.getStyleClass().addAll("button", "success-button");
        confirmBtn.setOnAction(e -> {
            if (supplierCombo.getValue() == null) { toastService.showWarning("Select a supplier!"); return; }
            if (table.getItems().isEmpty()) { toastService.showWarning("Add at least one product!"); return; }
            try {
                com.pbl3.project.pbl3_project.dto.CreateImportOrderRequest req = new com.pbl3.project.pbl3_project.dto.CreateImportOrderRequest();
                req.setUserId(user.getId());
                req.setSupplierId(supplierCombo.getValue().getId());
                req.setNotes("Import via UI");
                java.util.List<com.pbl3.project.pbl3_project.dto.CreateImportOrderRequest.ImportOrderItemRequest> items = new java.util.ArrayList<>();
                for (TempItem ti : table.getItems()) {
                    var itReq = new com.pbl3.project.pbl3_project.dto.CreateImportOrderRequest.ImportOrderItemRequest();
                    itReq.setProductId(ti.product.getId());
                    itReq.setQuantity(ti.quantity);
                    itReq.setImportPrice(ti.importPrice);
                    items.add(itReq);
                }
                req.setItems(items);
                importOrderService.createImportOrder(req);
                toastService.showSuccess("Import Order Created!");
                onSuccess.run();
                dialog.close();
            } catch (Exception ex) {
                toastService.showError("Failed: " + ex.getMessage());
            }
        });

        actionBox.getChildren().addAll(totalLabel, new javafx.scene.layout.Region(), cancelBtn, confirmBtn);
        javafx.scene.layout.HBox.setHgrow(actionBox.getChildren().get(1), javafx.scene.layout.Priority.ALWAYS);

        VBox tableCard = new VBox(table);
        tableCard.getStyleClass().add("report-section-card");
        tableCard.setPadding(new Insets(12));
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        VBox.setVgrow(tableCard, javafx.scene.layout.Priority.ALWAYS);

        root.getChildren().addAll(titleLabel, supplierCard, addBox, tableCard, actionBox);
        
        Scene scene = new Scene(root);
        if (owner.getScene() != null) scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void showImportOrderDetailsDialog(Stage owner, com.pbl3.project.pbl3_project.entity.ImportOrder order, com.pbl3.project.pbl3_project.entity.User user, Runnable onChanged) {
        try {
            Stage dialog = new Stage();
            dialog.initOwner(owner);
            dialog.setTitle("Import Order Details #" + order.getId());
            dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);

            VBox root = new VBox(15);
            root.getStyleClass().add("dialog-root");

            Label titleLabel = new Label("Import Order #" + order.getId());
            titleLabel.getStyleClass().add("dialog-title");
            titleLabel.setMaxWidth(Double.MAX_VALUE);

            Label supplierValueLabel = createDetailMetaValueLabel(
                order.getSupplierDisplayName(),
                "-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: -app-text-muted;"
            );

            Label dateValueLabel = createDetailMetaValueLabel(
                formatDateTimeWithSeconds(order.getCreatedAt()),
                "-fx-font-size: 14px; -fx-text-fill: -app-text-secondary;"
            );

            Label userValueLabel = createDetailMetaValueLabel(
                order.getCreatedByDisplayName(),
                "-fx-font-size: 14px; -fx-text-fill: -app-text-secondary;"
            );

            Label statusValueLabel = createDetailMetaValueLabel(
                formatImportOrderStatus(order.getStatus()),
                "-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: " + getImportOrderStatusColor(order.getStatus()) + ";"
            );

            Label statusNoteLabel = new Label(order.getStatusNote() == null || order.getStatusNote().isBlank()
                ? "-"
                : order.getStatusNote());
            statusNoteLabel.setWrapText(true);
            statusNoteLabel.setStyle("-fx-text-fill: -app-text-secondary; -fx-font-size: 13px;");

            Label notesLabel = new Label(order.getNotes() == null || order.getNotes().isBlank()
                ? "-"
                : order.getNotes());
            notesLabel.setWrapText(true);
            notesLabel.setStyle("-fx-text-fill: -app-text-secondary; -fx-font-size: 13px;");

            VBox metaBox = new VBox(
                8,
                createDetailMetaRow("Supplier", supplierValueLabel),
                createDetailMetaRow("Date", dateValueLabel),
                createDetailMetaRow("Created By", userValueLabel),
                createDetailMetaRow("Status", statusValueLabel),
                createDetailMetaRow("Status Note", statusNoteLabel),
                createDetailMetaRow("Import Notes", notesLabel)
            );

            javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.ImportOrderItem> table = new javafx.scene.control.TableView<>();
            prepareNonReorderableTable(table);
            table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);

            javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.ImportOrderItem, String> productCol = new javafx.scene.control.TableColumn<>("Product");
            productCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                cell.getValue().getProductDisplayName()
            ));

            javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.ImportOrderItem, Integer> qtyCol = new javafx.scene.control.TableColumn<>("Qty");
            qtyCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("quantity"));

            javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.ImportOrderItem, String> importPriceCol = new javafx.scene.control.TableColumn<>("Import Price");
            importPriceCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(formatVnd(cell.getValue().getImportPrice())));

            javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.ImportOrderItem, String> subtotalCol = new javafx.scene.control.TableColumn<>("Subtotal");
            subtotalCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                formatVnd(MoneySupport.multiply(
                    cell.getValue().getImportPrice(),
                    cell.getValue().getQuantity() != null ? cell.getValue().getQuantity() : 0
                ))
            ));

            table.getColumns().addAll(productCol, qtyCol, importPriceCol, subtotalCol);
            table.setItems(javafx.collections.FXCollections.observableArrayList(order.getItems()));
            table.setPrefHeight(300);

            Label totalLabel = new Label("Total Cost: " + formatVnd(order.getTotalCost()));
            totalLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: -app-danger;");
            totalLabel.setMaxWidth(Double.MAX_VALUE);
            totalLabel.setAlignment(Pos.CENTER_RIGHT);

            Button cancelImportButton = new Button("Cancel Import");
            cancelImportButton.getStyleClass().addAll("button", "danger-button");
            cancelImportButton.setDisable(order.getStatus() != null
                && order.getStatus() != com.pbl3.project.pbl3_project.entity.ImportOrderStatus.COMPLETED);
            cancelImportButton.setOnAction(e -> showCancelImportOrderDialog(dialog, importOrderService.getImportOrderWithItems(order.getId()), user, () -> {
                if (onChanged != null) {
                    onChanged.run();
                }
                dialog.close();
            }));

            Button closeButton = new Button("Close");
            closeButton.getStyleClass().addAll("button", "close-button");
            closeButton.setOnAction(e -> dialog.close());

            javafx.scene.layout.HBox actionRow = new javafx.scene.layout.HBox(10, cancelImportButton, closeButton);
            actionRow.setAlignment(Pos.CENTER_RIGHT);

            root.getChildren().addAll(titleLabel, metaBox, table, totalLabel, actionRow);
            VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
            enableDeselectOnOutsideClick(root, table);

            Scene scene = new Scene(root, 760, 600);
            scene.getStylesheets().add(getClass().getResource("/application.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Error", "Could not show import order details: " + e.getMessage());
        }
    }

    private void showCancelImportOrderDialog(Stage owner, com.pbl3.project.pbl3_project.entity.ImportOrder order, com.pbl3.project.pbl3_project.entity.User user, Runnable onSuccess) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
        dialog.setTitle("Cancel Import Order #" + order.getId());

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("dialog-root");

        Label title = new Label("Cancel Import Order #" + order.getId());
        title.getStyleClass().add("dialog-title");

        Label helper = new Label("This will reverse received stock and recompute moving-average cost. Reason is required.");
        helper.setStyle("-fx-text-fill: -app-text-secondary; -fx-font-size: 13px;");
        helper.setWrapText(true);

        javafx.scene.control.TextArea reasonArea = new javafx.scene.control.TextArea();
        reasonArea.setPromptText("Enter cancellation reason...");
        reasonArea.setWrapText(true);
        reasonArea.setPrefRowCount(4);

        Button confirmBtn = new Button("Confirm Cancel");
        confirmBtn.getStyleClass().addAll("button", "danger-button");
        confirmBtn.setOnAction(e -> {
            try {
                importOrderService.cancelImportOrder(order.getId(), user.getId(), reasonArea.getText());
                toastService.showSuccess("Import order canceled successfully");
                dialog.close();
                if (onSuccess != null) {
                    onSuccess.run();
                }
            } catch (Exception ex) {
                showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Cancel Import Failed", ex.getMessage());
            }
        });

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().addAll("button", "close-button");
        closeBtn.setOnAction(e -> dialog.close());

        javafx.scene.layout.HBox actionRow = new javafx.scene.layout.HBox(10, closeBtn, confirmBtn);
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(title, helper, reasonArea, actionRow);

        Scene scene = new Scene(root, 540, 300);
        scene.getStylesheets().add(getClass().getResource("/application.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private CustomerOrderAggregate getCustomerAggregateFor(
        com.pbl3.project.pbl3_project.entity.Customer customer,
        java.util.Map<Long, CustomerOrderAggregate> aggregateMap
    ) {
        if (customer == null || customer.getId() == null) {
            return CustomerOrderAggregate.empty(null);
        }
        if (aggregateMap == null) {
            return CustomerOrderAggregate.empty(customer.getId());
        }
        return aggregateMap.getOrDefault(customer.getId(), CustomerOrderAggregate.empty(customer.getId()));
    }

    private VBox createCustomersView(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        VBox root = new VBox();
        applyStandardTablePageLayout(root);

        Label header = new Label("Customers");
        header.getStyleClass().add("header-label");

        ExpandableSearchControl searchControl = createExpandableSearchControl(300);
        TextField searchField = searchControl.field();

        final int pageSize = 20;
        final int[] currentPage = {0};
        final int[] totalPages = {0};
        final long[] totalElements = {0L};
        Runnable[] loadPageRef = new Runnable[1];
        java.util.concurrent.atomic.AtomicReference<Boolean> enabledFilterRef =
            new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<java.util.Map<Long, CustomerOrderAggregate>> aggregateMapRef =
            new java.util.concurrent.atomic.AtomicReference<>(java.util.Map.of());

        javafx.scene.layout.HBox filterBox = new javafx.scene.layout.HBox();
        filterBox.setAlignment(Pos.CENTER);
        filterBox.getStyleClass().add("expandable-search-box");
        filterBox.setPrefSize(40, 40);
        filterBox.setMinSize(40, 40);
        filterBox.setMaxSize(40, 40);
        filterBox.setCursor(javafx.scene.Cursor.HAND);
        javafx.scene.shape.SVGPath filterIcon = new javafx.scene.shape.SVGPath();
        filterIcon.setContent("M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z");
        filterIcon.setFill(PRIMARY_COLOR);
        filterBox.getChildren().add(filterIcon);

        Popup filterPopup = new Popup();
        filterPopup.setAutoHide(true);

        FilterPopupShell filterShell = createFilterPopupShell(320, 220);
        VBox popupContainer = filterShell.container();
        VBox scrollContent = filterShell.content();

        Label statusLabel = createFilterPopupSectionTitle("Status");
        javafx.scene.control.CheckBox allStatusesCb = new javafx.scene.control.CheckBox("All Statuses");
        allStatusesCb.setSelected(true);
        allStatusesCb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
        VBox statusScroll = new VBox(8);
        statusScroll.setPadding(new Insets(5, 5, 5, 20));
        java.util.List<javafx.scene.control.CheckBox> statusCbs = new java.util.ArrayList<>();
        java.util.Map<String, Boolean> customerStatuses = java.util.Map.of(
            "Active", Boolean.TRUE,
            "Disabled", Boolean.FALSE
        );
        for (java.util.Map.Entry<String, Boolean> entry : customerStatuses.entrySet()) {
            javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(entry.getKey());
            cb.setUserData(entry.getValue());
            cb.setSelected(true);
            cb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
            cb.setOnAction(e -> {
                if (!cb.isSelected()) {
                    allStatusesCb.setSelected(false);
                } else {
                    boolean all = true;
                    for (javafx.scene.control.CheckBox statusCb : statusCbs) {
                        if (!statusCb.isSelected()) {
                            all = false;
                            break;
                        }
                    }
                    allStatusesCb.setSelected(all);
                }
            });
            statusCbs.add(cb);
            statusScroll.getChildren().add(cb);
        }
        allStatusesCb.setOnAction(e -> {
            boolean selected = allStatusesCb.isSelected();
            for (javafx.scene.control.CheckBox cb : statusCbs) {
                cb.setSelected(selected);
            }
        });
        FilterDisclosureSection statusSection = new FilterDisclosureSection(allStatusesCb, statusScroll);

        Button resetFilterBtn = new Button("Reset");
        resetFilterBtn.getStyleClass().add("filter-reset-btn");
        resetFilterBtn.setOnAction(e -> {
            allStatusesCb.setSelected(true);
            for (javafx.scene.control.CheckBox cb : statusCbs) {
                cb.setSelected(true);
            }
            statusSection.setExpanded(false);
            enabledFilterRef.set(null);
            currentPage[0] = 0;
            loadPageRef[0].run();
            filterBox.setStyle("");
            filterPopup.hide();
        });

        Button applyFilterBtn = new Button("Apply Filter");
        applyFilterBtn.getStyleClass().add("filter-apply-btn");
        applyFilterBtn.setOnAction(e -> {
            java.util.Set<Boolean> selectedStatuses = new java.util.LinkedHashSet<>();
            for (javafx.scene.control.CheckBox cb : statusCbs) {
                if (cb.isSelected()) {
                    selectedStatuses.add((Boolean) cb.getUserData());
                }
            }
            enabledFilterRef.set(selectedStatuses.size() == 1 ? selectedStatuses.iterator().next() : null);
            currentPage[0] = 0;
            loadPageRef[0].run();
            boolean hasFilter = !allStatusesCb.isSelected();
            filterBox.setStyle(hasFilter ? "-fx-border-color: -app-primary;" : "");
            filterPopup.hide();
        });

        scrollContent.getChildren().addAll(statusLabel, statusSection.getNode());
        popupContainer.getChildren().add(createFilterPopupActionRow(resetFilterBtn, applyFilterBtn));
        filterPopup.getContent().add(popupContainer);

        filterBox.setOnMouseClicked(e -> {
            if (filterPopup.isShowing()) {
                filterPopup.hide();
                return;
            }
            showPopupBelow(filterPopup, filterBox, -200, 5);
        });

        final String customerSortStateKey = "customers";
        TableSortState customerSortState = getOrCreateTableSortState(
            customerSortStateKey,
            new SortCriterion("fullName", javafx.scene.control.TableColumn.SortType.ASCENDING)
        );
        java.util.LinkedHashMap<String, String> customerSortProperties = new java.util.LinkedHashMap<>();
        customerSortProperties.put("id", "id");
        customerSortProperties.put("fullName", "fullName");
        customerSortProperties.put("phone", "phone");
        customerSortProperties.put("enabled", "enabled");
        java.util.LinkedHashMap<String, String> customerSortLabels = new java.util.LinkedHashMap<>();
        customerSortLabels.put("id", "ID");
        customerSortLabels.put("fullName", "Name");
        customerSortLabels.put("phone", "Phone");
        customerSortLabels.put("enabled", "Status");

        Button createButton = createExpandableGreenActionButton("Add Customer", 170);
        Button detailsButton = createExpandableManageActionButton("View Details", 150);
        Button editButton = createExpandableManageActionButton("Edit", 110);
        Button toggleStatusButton = createExpandableManageActionButton("Enable / Disable", 185);
        detailsButton.setDisable(true);
        editButton.setDisable(true);
        toggleStatusButton.setDisable(true);

        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.Customer> table = new javafx.scene.control.TableView<>();
        applyStandardTableSizing(table);
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        enableDragSelection(table);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Customer, Long> idCol = new javafx.scene.control.TableColumn<>("ID");
        idCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Customer, String> nameCol = new javafx.scene.control.TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getFullName()));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Customer, String> phoneCol = new javafx.scene.control.TableColumn<>("Phone");
        phoneCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getPhone()));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Customer, String> statusCol = new javafx.scene.control.TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatCustomerStatus(data.getValue().isEnabled())));
        statusCol.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                String color = "Active".equals(item) ? "-app-success-hover" : "-app-danger-hover";
                setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-alignment: CENTER;");
            }
        });

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Customer, String> ordersCol = new javafx.scene.control.TableColumn<>("Orders");
        ordersCol.setSortable(false);
        ordersCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            String.valueOf(getCustomerAggregateFor(data.getValue(), aggregateMapRef.get()).orderCount())
        ));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Customer, String> totalSpentCol = new javafx.scene.control.TableColumn<>("Total Spent");
        totalSpentCol.setSortable(false);
        totalSpentCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            formatVnd(getCustomerAggregateFor(data.getValue(), aggregateMapRef.get()).totalSpent())
        ));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Customer, String> lastPurchaseCol = new javafx.scene.control.TableColumn<>("Last Purchase");
        lastPurchaseCol.setSortable(false);
        lastPurchaseCol.setCellValueFactory(data -> {
            CustomerOrderAggregate aggregate = getCustomerAggregateFor(data.getValue(), aggregateMapRef.get());
            return new javafx.beans.property.SimpleStringProperty(
                aggregate.lastPurchase() != null ? formatDateTime(aggregate.lastPurchase()) : "-"
            );
        });

        table.getColumns().addAll(idCol, nameCol, phoneCol, statusCol, ordersCol, totalSpentCol, lastPurchaseCol);
        java.util.LinkedHashMap<String, javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Customer, ?>> customerSortColumns =
            new java.util.LinkedHashMap<>();
        customerSortColumns.put("id", idCol);
        customerSortColumns.put("fullName", nameCol);
        customerSortColumns.put("phone", phoneCol);
        customerSortColumns.put("enabled", statusCol);
        installSortHeaderIndicators(customerSortColumns);

        Label rowCountLabel = createStatusMetaLabel("Showing 0-0 of 0 Row(s)");
        Label pageLabel = createStatusMetaLabel("Page 0 / 0");
        Button prevBtn = createPageNavButton("Prev");
        Button nextBtn = createPageNavButton("Next");

        Runnable updateStatusBar = () -> updatePagedStatus(
            table,
            rowCountLabel,
            pageLabel,
            prevBtn,
            nextBtn,
            totalElements[0],
            currentPage[0],
            totalPages[0],
            pageSize
        );

        Runnable loadPage = () -> {
            org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.Customer> pageData =
                customerService.searchCustomers(
                    user,
                    searchField.getText(),
                    enabledFilterRef.get(),
                    createPageable(customerSortState, customerSortProperties, currentPage[0], pageSize)
                );
            if (pageData.getTotalPages() > 0 && currentPage[0] >= pageData.getTotalPages()) {
                currentPage[0] = pageData.getTotalPages() - 1;
                pageData = customerService.searchCustomers(
                    user,
                    searchField.getText(),
                    enabledFilterRef.get(),
                    createPageable(customerSortState, customerSortProperties, currentPage[0], pageSize)
                );
            }
            totalElements[0] = pageData.getTotalElements();
            totalPages[0] = pageData.getTotalPages();
            aggregateMapRef.set(customerService.getCustomerAggregates(user, pageData.getContent()));
            table.setItems(javafx.collections.FXCollections.observableArrayList(pageData.getContent()));
            table.refresh();
            updateStatusBar.run();
        };
        loadPageRef[0] = loadPage;

        Label customerSortStatusLabel = createSortStatusLabel(customerSortState, customerSortLabels);
        Runnable applyCustomerSortUi = () -> {
            applySortStateToTable(table, customerSortColumns, customerSortState);
            customerSortStatusLabel.setText(buildSortStatusText(customerSortState, customerSortLabels));
        };
        applyCustomerSortUi.run();
        installManualServerSorting(
            table,
            customerSortColumns,
            customerSortState,
            () -> {
                applyCustomerSortUi.run();
                currentPage[0] = 0;
                loadPage.run();
            }
        );

        javafx.animation.PauseTransition searchPause = new javafx.animation.PauseTransition(Duration.millis(220));
        searchPause.setOnFinished(e -> {
            currentPage[0] = 0;
            loadPage.run();
        });
        searchField.textProperty().addListener((obs, oldV, newV) -> searchPause.playFromStart());

        prevBtn.setOnAction(e -> {
            if (currentPage[0] > 0) {
                currentPage[0]--;
                loadPage.run();
            }
        });
        nextBtn.setOnAction(e -> {
            if (currentPage[0] + 1 < totalPages[0]) {
                currentPage[0]++;
                loadPage.run();
            }
        });

        table.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<com.pbl3.project.pbl3_project.entity.Customer>) c -> {
            boolean single = table.getSelectionModel().getSelectedItems().size() == 1;
            detailsButton.setDisable(!single);
            editButton.setDisable(!single);
            toggleStatusButton.setDisable(!single);
        });

        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<com.pbl3.project.pbl3_project.entity.Customer> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showCustomerDetailsDialog(stage, user, row.getItem(), loadPageRef[0]);
                }
            });
            return row;
        });

        createButton.setOnAction(e -> showCustomerUpsertDialog(stage, user, null, loadPageRef[0]));
        detailsButton.setOnAction(e -> {
            com.pbl3.project.pbl3_project.entity.Customer selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showCustomerDetailsDialog(stage, user, selected, loadPageRef[0]);
            }
        });
        editButton.setOnAction(e -> {
            com.pbl3.project.pbl3_project.entity.Customer selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showCustomerUpsertDialog(stage, user, selected, loadPageRef[0]);
            }
        });
        toggleStatusButton.setOnAction(e -> {
            com.pbl3.project.pbl3_project.entity.Customer selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            try {
                com.pbl3.project.pbl3_project.entity.Customer updated =
                    customerService.setCustomerEnabled(user, selected.getId(), !selected.isEnabled());
                toastService.showSuccess("Customer " + (updated.isEnabled() ? "enabled" : "disabled"));
                loadPageRef[0].run();
            } catch (Exception ex) {
                showUserFacingError(ex);
            }
        });

        javafx.scene.layout.BorderPane toolbar = new javafx.scene.layout.BorderPane();
        toolbar.setLeft(header);
        javafx.scene.layout.HBox rightBox = new javafx.scene.layout.HBox(
            12,
            filterBox,
            searchControl.box(),
            createButton,
            detailsButton,
            editButton,
            toggleStatusButton
        );
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        toolbar.setRight(rightBox);

        javafx.scene.layout.HBox statusBar = new javafx.scene.layout.HBox(
            15,
            customerSortStatusLabel,
            rowCountLabel,
            pageLabel,
            prevBtn,
            nextBtn
        );
        applyStandardTableStatusBar(statusBar);

        root.getChildren().addAll(toolbar, table, statusBar);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        enableDeselectOnOutsideClick(root, table);
        loadPage.run();
        return root;
    }

    private void showCustomerUpsertDialog(
        Stage owner,
        com.pbl3.project.pbl3_project.entity.User actor,
        com.pbl3.project.pbl3_project.entity.Customer target,
        Runnable onSuccess
    ) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
        dialog.setTitle(target == null ? "Add Customer" : "Edit Customer");

        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("dialog-root");
        root.setFillWidth(true);

        Label titleLabel = new Label(target == null ? "Add Customer" : "Edit Customer");
        titleLabel.getStyleClass().add("dialog-title");

        TextField nameField = createStyledTextField(target != null ? target.getFullName() : "", "Full Name");
        TextField phoneField = createStyledTextField(target != null ? target.getPhone() : "", "Phone");
        nameField.setPrefWidth(220);
        nameField.setMaxWidth(Double.MAX_VALUE);
        phoneField.setPrefWidth(220);
        phoneField.setMaxWidth(Double.MAX_VALUE);

        javafx.scene.layout.GridPane form = new javafx.scene.layout.GridPane();
        form.setHgap(14);
        form.setVgap(14);
        javafx.scene.layout.ColumnConstraints labelColumn = new javafx.scene.layout.ColumnConstraints();
        labelColumn.setMinWidth(78);
        labelColumn.setPrefWidth(78);
        labelColumn.setMaxWidth(78);
        javafx.scene.layout.ColumnConstraints fieldColumn = new javafx.scene.layout.ColumnConstraints();
        fieldColumn.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        fieldColumn.setFillWidth(true);
        form.getColumnConstraints().setAll(labelColumn, fieldColumn);
        form.add(createFormLabel("Full Name *"), 0, 0);
        form.add(nameField, 1, 0);
        form.add(createFormLabel("Phone *"), 0, 1);
        form.add(phoneField, 1, 1);

        Button saveBtn = new Button(target == null ? "Create" : "Save");
        saveBtn.getStyleClass().addAll("button", "success-button");
        saveBtn.setPrefHeight(36);
        saveBtn.setMinHeight(36);
        saveBtn.setPrefWidth(104);
        saveBtn.setMinWidth(104);
        saveBtn.setOnAction(e -> {
            try {
                if (target == null) {
                    customerService.createCustomer(actor, nameField.getText(), phoneField.getText());
                    toastService.showSuccess("Customer created");
                } else {
                    customerService.updateCustomer(actor, target.getId(), nameField.getText(), phoneField.getText());
                    toastService.showSuccess("Customer updated");
                }
                dialog.close();
                if (onSuccess != null) {
                    onSuccess.run();
                }
            } catch (Exception ex) {
                showUserFacingError(ex);
            }
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("filter-reset-btn");
        cancelBtn.setPrefHeight(36);
        cancelBtn.setMinHeight(36);
        cancelBtn.setPrefWidth(104);
        cancelBtn.setMinWidth(104);
        cancelBtn.setOnAction(e -> dialog.close());

        javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(10, cancelBtn, saveBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(2, 0, 0, 0));

        root.getChildren().addAll(titleLabel, form, actions);

        Scene scene = new Scene(root, 440, 230);
        scene.getStylesheets().add(getClass().getResource("/application.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private javafx.scene.layout.VBox createCustomerSummaryCard(String labelText, String valueText, String valueStyle) {
        Label keyLabel = new Label(labelText);
        keyLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -app-text-muted; -fx-font-weight: 600;");
        Label valueLabel = new Label(valueText);
        valueLabel.setWrapText(true);
        valueLabel.setStyle(valueStyle);

        VBox card = new VBox(6, keyLabel, valueLabel);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: -app-surface-muted; -fx-background-radius: 16; -fx-border-color: -app-border; -fx-border-radius: 16;");
        card.setPrefWidth(180);
        return card;
    }

    private void showCustomerDetailsDialog(
        Stage owner,
        com.pbl3.project.pbl3_project.entity.User actor,
        com.pbl3.project.pbl3_project.entity.Customer customer,
        Runnable onChanged
    ) {
        try {
            com.pbl3.project.pbl3_project.entity.Customer managedCustomer = customerService.getCustomerById(actor, customer.getId());
            CustomerOrderAggregate aggregate = customerService.getCustomerAggregate(actor, managedCustomer.getId());

            Stage dialog = new Stage();
            dialog.initOwner(owner);
            dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialog.setTitle("Customer #" + managedCustomer.getId());

            VBox root = new VBox(15);
            root.getStyleClass().add("dialog-root");

            Label titleLabel = new Label("Customer #" + managedCustomer.getId());
            titleLabel.getStyleClass().add("dialog-title");
            titleLabel.setMaxWidth(Double.MAX_VALUE);

            VBox metaBox = new VBox(
                8,
                createDetailMetaRow("Name", createDetailMetaValueLabel(managedCustomer.getFullName(), "-fx-font-size: 14px; -fx-text-fill: -app-text-secondary;")),
                createDetailMetaRow("Phone", createDetailMetaValueLabel(managedCustomer.getPhone(), "-fx-font-size: 14px; -fx-text-fill: -app-text-secondary;")),
                createDetailMetaRow("Status", createDetailMetaValueLabel(
                    formatCustomerStatus(managedCustomer.isEnabled()),
                    "-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: " + (managedCustomer.isEnabled() ? "-app-success-hover" : "-app-danger-hover") + ";"
                )),
                createDetailMetaRow("Created At", createDetailMetaValueLabel(formatDateTimeWithSeconds(managedCustomer.getCreatedAt()), "-fx-font-size: 14px; -fx-text-fill: -app-text-secondary;")),
                createDetailMetaRow("Updated At", createDetailMetaValueLabel(formatDateTimeWithSeconds(managedCustomer.getUpdatedAt()), "-fx-font-size: 14px; -fx-text-fill: -app-text-secondary;"))
            );

            javafx.scene.layout.HBox summaryRow = new javafx.scene.layout.HBox(
                12,
                createCustomerSummaryCard("Total Orders", String.valueOf(aggregate.orderCount()), "-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: -app-primary;"),
                createCustomerSummaryCard("Total Spent", formatVnd(aggregate.totalSpent()), "-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: -app-success-hover;"),
                createCustomerSummaryCard(
                    "Last Purchase",
                    aggregate.lastPurchase() != null ? formatDateTime(aggregate.lastPurchase()) : "-",
                    "-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: -app-text-primary;"
                )
            );

            Label historyLabel = new Label("Purchase History");
            historyLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: -app-text-primary;");

            javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.Order> table = new javafx.scene.control.TableView<>();
            table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            prepareNonReorderableTable(table);

            javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, Long> idCol = new javafx.scene.control.TableColumn<>("Order ID");
            idCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));

            javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, String> dateCol = new javafx.scene.control.TableColumn<>("Created At");
            dateCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatDateTime(data.getValue().getCreatedAt())));

            javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, String> totalCol = new javafx.scene.control.TableColumn<>("Total");
            totalCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatVnd(data.getValue().getTotalPrice())));

            javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, String> userCol = new javafx.scene.control.TableColumn<>("Created By");
            userCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getCreatedByDisplayName()));

            javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, String> statusCol = new javafx.scene.control.TableColumn<>("Status");
            statusCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatOrderStatus(data.getValue().getStatus())));

            table.getColumns().addAll(idCol, dateCol, totalCol, userCol, statusCol);

            final int pageSize = 8;
            final int[] currentPage = {0};
            final int[] totalPages = {0};
            final long[] totalElements = {0L};

            Label rowCountLabel = createStatusMetaLabel("Showing 0-0 of 0 Row(s)");
            Label pageLabel = createStatusMetaLabel("Page 0 / 0");
            Button prevBtn = createPageNavButton("Prev");
            Button nextBtn = createPageNavButton("Next");

            Runnable updateStatusBar = () -> updatePagedStatus(
                table,
                rowCountLabel,
                pageLabel,
                prevBtn,
                nextBtn,
                totalElements[0],
                currentPage[0],
                totalPages[0],
                pageSize
            );
            Runnable loadPage = () -> {
                org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.Order> pageData =
                    customerService.searchCustomerOrders(
                        actor,
                        managedCustomer.getId(),
                        org.springframework.data.domain.PageRequest.of(currentPage[0], pageSize, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
                    );
                if (pageData.getTotalPages() > 0 && currentPage[0] >= pageData.getTotalPages()) {
                    currentPage[0] = pageData.getTotalPages() - 1;
                    pageData = customerService.searchCustomerOrders(
                        actor,
                        managedCustomer.getId(),
                        org.springframework.data.domain.PageRequest.of(currentPage[0], pageSize, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
                    );
                }
                totalElements[0] = pageData.getTotalElements();
                totalPages[0] = pageData.getTotalPages();
                table.setItems(javafx.collections.FXCollections.observableArrayList(pageData.getContent()));
                updateStatusBar.run();
            };

            prevBtn.setOnAction(e -> {
                if (currentPage[0] > 0) {
                    currentPage[0]--;
                    loadPage.run();
                }
            });
            nextBtn.setOnAction(e -> {
                if (currentPage[0] + 1 < totalPages[0]) {
                    currentPage[0]++;
                    loadPage.run();
                }
            });
            table.setRowFactory(tv -> {
                javafx.scene.control.TableRow<com.pbl3.project.pbl3_project.entity.Order> row = new javafx.scene.control.TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !row.isEmpty()) {
                        showOrderDetailsDialog(dialog, orderService.getOrderWithItems(row.getItem().getId(), actor), actor, onChanged);
                    }
                });
                return row;
            });
            enableDeselectOnOutsideClick(root, table);
            loadPage.run();

            Button closeButton = new Button("Close");
            closeButton.getStyleClass().addAll("button", "close-button");
            closeButton.setOnAction(e -> dialog.close());

            javafx.scene.layout.HBox statusBar = new javafx.scene.layout.HBox(15, rowCountLabel, pageLabel, prevBtn, nextBtn);
            statusBar.setAlignment(Pos.CENTER_RIGHT);

            root.getChildren().addAll(titleLabel, metaBox, summaryRow, historyLabel, table, statusBar, closeButton);
            VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);

            Scene scene = new Scene(root, 860, 720);
            scene.getStylesheets().add(getClass().getResource("/application.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (Exception ex) {
            showUserFacingError(ex);
        }
    }

    private com.pbl3.project.pbl3_project.entity.Customer showCustomerPickerDialog(
        Stage owner,
        com.pbl3.project.pbl3_project.entity.User actor
    ) {
        java.util.concurrent.atomic.AtomicReference<com.pbl3.project.pbl3_project.entity.Customer> selectedCustomerRef =
            new java.util.concurrent.atomic.AtomicReference<>(null);

        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
        dialog.setTitle("Select Customer");

        VBox root = new VBox();
        applyStandardTablePageLayout(root);
        root.getStyleClass().add("dialog-root");

        Label titleLabel = new Label("Select Customer");
        titleLabel.getStyleClass().add("dialog-title");

        TextField searchField = createStyledTextField("", DEFAULT_SEARCH_PROMPT);

        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.Customer> table = new javafx.scene.control.TableView<>();
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        prepareNonReorderableTable(table);
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.SINGLE);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Customer, String> nameCol = new javafx.scene.control.TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getFullName()));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Customer, String> phoneCol = new javafx.scene.control.TableColumn<>("Phone");
        phoneCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getPhone()));
        table.getColumns().addAll(nameCol, phoneCol);

        final int pageSize = 10;
        final int[] currentPage = {0};
        final int[] totalPages = {0};
        final long[] totalElements = {0L};

        Label rowCountLabel = createStatusMetaLabel("Showing 0-0 of 0 Row(s)");
        Label pageLabel = createStatusMetaLabel("Page 0 / 0");
        Button prevBtn = createPageNavButton("Prev");
        Button nextBtn = createPageNavButton("Next");

        Runnable updateStatusBar = () -> updatePagedStatus(
            table,
            rowCountLabel,
            pageLabel,
            prevBtn,
            nextBtn,
            totalElements[0],
            currentPage[0],
            totalPages[0],
            pageSize
        );
        Runnable loadPage = () -> {
            org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.Customer> pageData =
                customerService.searchActiveCustomersForSales(
                    actor,
                    searchField.getText(),
                    org.springframework.data.domain.PageRequest.of(currentPage[0], pageSize, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "fullName"))
                );
            if (pageData.getTotalPages() > 0 && currentPage[0] >= pageData.getTotalPages()) {
                currentPage[0] = pageData.getTotalPages() - 1;
                pageData = customerService.searchActiveCustomersForSales(
                    actor,
                    searchField.getText(),
                    org.springframework.data.domain.PageRequest.of(currentPage[0], pageSize, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "fullName"))
                );
            }
            totalElements[0] = pageData.getTotalElements();
            totalPages[0] = pageData.getTotalPages();
            table.setItems(javafx.collections.FXCollections.observableArrayList(pageData.getContent()));
            updateStatusBar.run();
        };

        javafx.animation.PauseTransition searchPause = new javafx.animation.PauseTransition(Duration.millis(220));
        searchPause.setOnFinished(e -> {
            currentPage[0] = 0;
            loadPage.run();
        });
        searchField.textProperty().addListener((obs, oldV, newV) -> searchPause.playFromStart());

        prevBtn.setOnAction(e -> {
            if (currentPage[0] > 0) {
                currentPage[0]--;
                loadPage.run();
            }
        });
        nextBtn.setOnAction(e -> {
            if (currentPage[0] + 1 < totalPages[0]) {
                currentPage[0]++;
                loadPage.run();
            }
        });

        Button selectBtn = createExpandableGreenActionButton("Select", 120);
        selectBtn.setDisable(true);
        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("filter-reset-btn");
        closeBtn.setOnAction(e -> dialog.close());

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> selectBtn.setDisable(newV == null));
        selectBtn.setOnAction(e -> {
            selectedCustomerRef.set(table.getSelectionModel().getSelectedItem());
            dialog.close();
        });
        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<com.pbl3.project.pbl3_project.entity.Customer> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    selectedCustomerRef.set(row.getItem());
                    dialog.close();
                }
            });
            return row;
        });

        javafx.scene.layout.HBox statusBar = new javafx.scene.layout.HBox(15, rowCountLabel, pageLabel, prevBtn, nextBtn);
        statusBar.setAlignment(Pos.CENTER_RIGHT);
        javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(10, closeBtn, selectBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(titleLabel, searchField, table, statusBar, actions);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        enableDeselectOnOutsideClick(root, table);
        loadPage.run();

        Scene scene = new Scene(root, 520, 520);
        scene.getStylesheets().add(getClass().getResource("/application.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
        return selectedCustomerRef.get();
    }

    private void updatePosCustomerCard(
        Label customerNameLabel,
        Label customerPhoneLabel,
        Button clearCustomerButton,
        com.pbl3.project.pbl3_project.entity.Customer customer
    ) {
        if (customer == null) {
            customerNameLabel.setText("Guest");
            customerPhoneLabel.setText("No customer selected");
            clearCustomerButton.setDisable(true);
            return;
        }
        customerNameLabel.setText(customer.getFullName());
        customerPhoneLabel.setText(customer.getPhone());
        clearCustomerButton.setDisable(false);
    }

    private VBox createAccountsView(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        VBox root = new VBox(12);
        root.setPadding(new Insets(20));

        Label header = new Label("Account Management");
        header.getStyleClass().add("header-label");

        javafx.scene.layout.HBox searchBox = new javafx.scene.layout.HBox(0);
        searchBox.setAlignment(Pos.CENTER);
        searchBox.getStyleClass().add("expandable-search-box");
        searchBox.setPrefSize(40, 40);
        searchBox.setMinSize(40, 40);
        searchBox.setMaxSize(40, 40);

        javafx.scene.shape.SVGPath searchIcon = new javafx.scene.shape.SVGPath();
        searchIcon.setContent("M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
        searchIcon.setFill(PRIMARY_COLOR);

        javafx.scene.layout.Region searchSpacer = new javafx.scene.layout.Region();
        searchSpacer.setMinWidth(0);
        searchSpacer.setPrefWidth(0);

        TextField searchField = new TextField();
        searchField.setPromptText(DEFAULT_SEARCH_PROMPT);
        searchField.getStyleClass().add("search-text-field");
        searchField.setMinWidth(0);
        searchField.setMaxWidth(0);
        searchField.setPrefWidth(0);
        searchField.setOpacity(0);

        searchBox.getChildren().addAll(searchIcon, searchSpacer, searchField);

        javafx.animation.Timeline searchExpand = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.millis(150),
                new javafx.animation.KeyValue(searchBox.maxWidthProperty(), 300, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(searchBox.prefWidthProperty(), 300, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(searchSpacer.minWidthProperty(), 8, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(searchField.minWidthProperty(), 240, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(searchField.maxWidthProperty(), 240, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(searchField.prefWidthProperty(), 240, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(searchField.opacityProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH)
            )
        );
        javafx.animation.Timeline searchCollapse = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.millis(150),
                new javafx.animation.KeyValue(searchBox.maxWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(searchBox.prefWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(searchSpacer.minWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(searchField.minWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(searchField.maxWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(searchField.prefWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(searchField.opacityProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH)
            )
        );
        searchBox.setOnMouseClicked(e -> {
            if (searchBox.getMaxWidth() == 40) {
                searchExpand.play();
                searchField.requestFocus();
            } else if (e.getTarget() == searchIcon || e.getTarget() == searchBox) {
                searchField.clear();
                if (searchBox.getParent() != null) {
                    searchBox.getParent().requestFocus();
                }
                searchCollapse.play();
            }
        });

        final int pageSize = 20;
        final int[] currentPage = {0};
        final int[] totalPages = {0};
        final long[] totalElements = {0L};
        Runnable[] loadPageRef = new Runnable[1];
        java.util.concurrent.atomic.AtomicReference<java.util.Set<com.pbl3.project.pbl3_project.entity.Role>> roleFiltersRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<Boolean> enabledFilterRef =
            new java.util.concurrent.atomic.AtomicReference<>(null);

        javafx.scene.layout.HBox filterBox = new javafx.scene.layout.HBox();
        filterBox.setAlignment(Pos.CENTER);
        filterBox.getStyleClass().add("expandable-search-box");
        filterBox.setPrefSize(40, 40);
        filterBox.setMinSize(40, 40);
        filterBox.setMaxSize(40, 40);
        filterBox.setCursor(javafx.scene.Cursor.HAND);
        javafx.scene.shape.SVGPath filterIcon = new javafx.scene.shape.SVGPath();
        filterIcon.setContent("M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z");
        filterIcon.setFill(PRIMARY_COLOR);
        filterBox.getChildren().add(filterIcon);

        javafx.stage.Popup filterPopup = new javafx.stage.Popup();
        filterPopup.setAutoHide(true);

        FilterPopupShell filterShell = createFilterPopupShell(320, 240);
        VBox popupContainer = filterShell.container();
        VBox scrollContent = filterShell.content();

        Label roleLabel = createFilterPopupSectionTitle("Role");
        javafx.scene.control.CheckBox allRolesCb = new javafx.scene.control.CheckBox("All Roles");
        allRolesCb.setSelected(true);
        allRolesCb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
        VBox roleScroll = new VBox(8);
        roleScroll.setPadding(new Insets(5, 5, 5, 20));
        java.util.List<javafx.scene.control.CheckBox> roleCbs = new java.util.ArrayList<>();
        for (com.pbl3.project.pbl3_project.entity.Role role : com.pbl3.project.pbl3_project.entity.Role.values()) {
            javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(formatRoleLabel(role));
            cb.setUserData(role);
            cb.setSelected(true);
            cb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
            cb.setOnAction(e -> {
                if (!cb.isSelected()) {
                    allRolesCb.setSelected(false);
                } else {
                    boolean all = true;
                    for (javafx.scene.control.CheckBox roleCb : roleCbs) {
                        if (!roleCb.isSelected()) {
                            all = false;
                            break;
                        }
                    }
                    allRolesCb.setSelected(all);
                }
            });
            roleCbs.add(cb);
            roleScroll.getChildren().add(cb);
        }
        allRolesCb.setOnAction(e -> {
            boolean selected = allRolesCb.isSelected();
            for (javafx.scene.control.CheckBox cb : roleCbs) {
                cb.setSelected(selected);
            }
        });
        FilterDisclosureSection roleSection = new FilterDisclosureSection(allRolesCb, roleScroll);

        javafx.scene.control.Separator roleSeparator = new javafx.scene.control.Separator();

        Label statusLabel = createFilterPopupSectionTitle("Status");
        javafx.scene.control.CheckBox allStatusesCb = new javafx.scene.control.CheckBox("All Statuses");
        allStatusesCb.setSelected(true);
        allStatusesCb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
        VBox statusScroll = new VBox(8);
        statusScroll.setPadding(new Insets(5, 5, 5, 20));
        java.util.List<javafx.scene.control.CheckBox> statusCbs = new java.util.ArrayList<>();
        java.util.Map<String, Boolean> accountStatuses = java.util.Map.of(
            "Active", Boolean.TRUE,
            "Disabled", Boolean.FALSE
        );
        for (java.util.Map.Entry<String, Boolean> entry : accountStatuses.entrySet()) {
            javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(entry.getKey());
            cb.setUserData(entry.getValue());
            cb.setSelected(true);
            cb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
            cb.setOnAction(e -> {
                if (!cb.isSelected()) {
                    allStatusesCb.setSelected(false);
                } else {
                    boolean all = true;
                    for (javafx.scene.control.CheckBox statusCb : statusCbs) {
                        if (!statusCb.isSelected()) {
                            all = false;
                            break;
                        }
                    }
                    allStatusesCb.setSelected(all);
                }
            });
            statusCbs.add(cb);
            statusScroll.getChildren().add(cb);
        }
        allStatusesCb.setOnAction(e -> {
            boolean selected = allStatusesCb.isSelected();
            for (javafx.scene.control.CheckBox cb : statusCbs) {
                cb.setSelected(selected);
            }
        });
        FilterDisclosureSection statusSection = new FilterDisclosureSection(allStatusesCb, statusScroll);

        Button resetFilterBtn = new Button("Reset");
        resetFilterBtn.getStyleClass().add("filter-reset-btn");
        resetFilterBtn.setOnAction(e -> {
            allRolesCb.setSelected(true);
            for (javafx.scene.control.CheckBox cb : roleCbs) {
                cb.setSelected(true);
            }
            roleSection.setExpanded(false);
            allStatusesCb.setSelected(true);
            for (javafx.scene.control.CheckBox cb : statusCbs) {
                cb.setSelected(true);
            }
            statusSection.setExpanded(false);
            roleFiltersRef.set(new java.util.LinkedHashSet<>());
            enabledFilterRef.set(null);
            currentPage[0] = 0;
            loadPageRef[0].run();
            filterBox.setStyle("");
            filterPopup.hide();
        });

        Button applyFilterBtn = new Button("Apply Filter");
        applyFilterBtn.getStyleClass().add("filter-apply-btn");
        applyFilterBtn.setOnAction(e -> {
            java.util.Set<com.pbl3.project.pbl3_project.entity.Role> selectedRoles = new java.util.LinkedHashSet<>();
            for (javafx.scene.control.CheckBox cb : roleCbs) {
                if (cb.isSelected()) {
                    selectedRoles.add((com.pbl3.project.pbl3_project.entity.Role) cb.getUserData());
                }
            }

            java.util.Set<Boolean> selectedStatuses = new java.util.LinkedHashSet<>();
            for (javafx.scene.control.CheckBox cb : statusCbs) {
                if (cb.isSelected()) {
                    selectedStatuses.add((Boolean) cb.getUserData());
                }
            }

            roleFiltersRef.set(allRolesCb.isSelected() ? new java.util.LinkedHashSet<>() : selectedRoles);
            enabledFilterRef.set(selectedStatuses.size() == 1 ? selectedStatuses.iterator().next() : null);
            currentPage[0] = 0;
            loadPageRef[0].run();
            boolean hasFilter = !allRolesCb.isSelected() || !allStatusesCb.isSelected();
            filterBox.setStyle(hasFilter ? "-fx-border-color: -app-primary;" : "");
            filterPopup.hide();
        });

        javafx.scene.layout.HBox filterActions = createFilterPopupActionRow(resetFilterBtn, applyFilterBtn);
        scrollContent.getChildren().addAll(roleLabel, roleSection.getNode(), roleSeparator, statusLabel, statusSection.getNode());
        popupContainer.getChildren().add(filterActions);
        filterPopup.getContent().add(popupContainer);

        filterBox.setOnMouseClicked(e -> {
            if (filterPopup.isShowing()) {
                filterPopup.hide();
                return;
            }
            showPopupBelow(filterPopup, filterBox, -200, 5);
        });

        Button createButton = createExpandableGreenActionButton("Create Account", 180);
        Button editButton = createExpandableManageActionButton("Edit", 110);
        Button resetPwdButton = createExpandableManageActionButton("Reset Password", 170);
        Button toggleStatusButton = createExpandableManageActionButton("Enable / Disable", 185);
        final String accountSortStateKey = "accounts";
        TableSortState accountSortState = getOrCreateTableSortState(
            accountSortStateKey,
            new SortCriterion("username", javafx.scene.control.TableColumn.SortType.ASCENDING)
        );
        java.util.LinkedHashMap<String, String> accountSortProperties = new java.util.LinkedHashMap<>();
        accountSortProperties.put("username", "username");
        accountSortProperties.put("fullName", "fullName");
        accountSortProperties.put("role", "role");
        java.util.LinkedHashMap<String, String> accountSortLabels = new java.util.LinkedHashMap<>();
        accountSortLabels.put("username", "Username");
        accountSortLabels.put("fullName", "Full Name");
        accountSortLabels.put("role", "Role");
        editButton.setDisable(true);
        resetPwdButton.setDisable(true);
        toggleStatusButton.setDisable(true);

        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.User> table = new javafx.scene.control.TableView<>();
        applyStandardTableSizing(table);
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        enableDragSelection(table);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.User, String> usernameCol = new javafx.scene.control.TableColumn<>("Username");
        usernameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getUsername()));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.User, String> fullNameCol = new javafx.scene.control.TableColumn<>("Full Name");
        fullNameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getFullName()));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.User, String> roleCol = new javafx.scene.control.TableColumn<>("Role");
        roleCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatRoleLabel(data.getValue().getRole())));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.User, String> statusCol = new javafx.scene.control.TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatUserStatus(data.getValue().isEnabled())));
        table.getColumns().addAll(usernameCol, fullNameCol, roleCol, statusCol);
        java.util.LinkedHashMap<String, javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.User, ?>> accountSortColumns =
            new java.util.LinkedHashMap<>();
        accountSortColumns.put("username", usernameCol);
        accountSortColumns.put("fullName", fullNameCol);
        accountSortColumns.put("role", roleCol);
        installSortHeaderIndicators(accountSortColumns);

        Label rowCountLabel = createStatusMetaLabel("Showing 0-0 of 0 Row(s)");
        Label pageLabel = createStatusMetaLabel("Page 0 / 0");
        Button prevBtn = createPageNavButton("Prev");
        Button nextBtn = createPageNavButton("Next");

        Runnable updateStatusBar = () -> updatePagedStatus(
            table,
            rowCountLabel,
            pageLabel,
            prevBtn,
            nextBtn,
            totalElements[0],
            currentPage[0],
            totalPages[0],
            pageSize
        );

        Runnable loadPage = () -> {
            org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.User> pageData =
                userAccountService.searchUsers(
                    user,
                    searchField.getText(),
                    roleFiltersRef.get(),
                    enabledFilterRef.get(),
                    createPageable(accountSortState, accountSortProperties, currentPage[0], pageSize)
                );
            if (pageData.getTotalPages() > 0 && currentPage[0] >= pageData.getTotalPages()) {
                currentPage[0] = pageData.getTotalPages() - 1;
                pageData = userAccountService.searchUsers(
                    user,
                    searchField.getText(),
                    roleFiltersRef.get(),
                    enabledFilterRef.get(),
                    createPageable(accountSortState, accountSortProperties, currentPage[0], pageSize)
                );
            }
            totalElements[0] = pageData.getTotalElements();
            totalPages[0] = pageData.getTotalPages();
            table.setItems(javafx.collections.FXCollections.observableArrayList(pageData.getContent()));
            updateStatusBar.run();
        };
        loadPageRef[0] = loadPage;
        Label accountSortStatusLabel = createSortStatusLabel(accountSortState, accountSortLabels);
        Runnable applyAccountSortUi = () -> {
            applySortStateToTable(table, accountSortColumns, accountSortState);
            accountSortStatusLabel.setText(buildSortStatusText(accountSortState, accountSortLabels));
        };
        applyAccountSortUi.run();
        installManualServerSorting(
            table,
            accountSortColumns,
            accountSortState,
            () -> {
                applyAccountSortUi.run();
                currentPage[0] = 0;
                loadPage.run();
            }
        );

        prevBtn.setOnAction(e -> {
            if (currentPage[0] > 0) {
                currentPage[0]--;
                loadPage.run();
            }
        });
        nextBtn.setOnAction(e -> {
            if (currentPage[0] + 1 < totalPages[0]) {
                currentPage[0]++;
                loadPage.run();
            }
        });

        javafx.animation.PauseTransition searchPause = new javafx.animation.PauseTransition(Duration.millis(220));
        searchPause.setOnFinished(e -> {
            currentPage[0] = 0;
            loadPage.run();
        });
        searchField.textProperty().addListener((obs, oldValue, newValue) -> searchPause.playFromStart());

        createButton.setOnAction(e -> showAccountUpsertDialog(stage, user, null, loadPage));
        editButton.setOnAction(e -> {
            com.pbl3.project.pbl3_project.entity.User selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showAccountUpsertDialog(stage, user, selected, loadPage);
            }
        });
        resetPwdButton.setOnAction(e -> {
            com.pbl3.project.pbl3_project.entity.User selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showResetAccountPasswordDialog(stage, user, selected, loadPage);
            }
        });
        toggleStatusButton.setOnAction(e -> {
            com.pbl3.project.pbl3_project.entity.User selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            try {
                com.pbl3.project.pbl3_project.entity.User updatedUser =
                    userAccountService.setUserEnabled(user, selected.getId(), !selected.isEnabled());
                syncSessionUser(user, updatedUser);
                toastService.showSuccess((updatedUser.isEnabled() ? "Enabled " : "Disabled ") + updatedUser.getUsername());
                loadPage.run();
                if (!authorizationService.canAccessAccounts(user)) {
                    showOverviewScene(stage, user);
                }
            } catch (Exception ex) {
                toastService.showError(ex.getMessage());
            }
        });

        table.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<com.pbl3.project.pbl3_project.entity.User>) c -> {
            boolean single = table.getSelectionModel().getSelectedItems().size() == 1;
            editButton.setDisable(!single);
            resetPwdButton.setDisable(!single);
            toggleStatusButton.setDisable(!single);
            updateStatusBar.run();
        });

        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<com.pbl3.project.pbl3_project.entity.User> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showAccountUpsertDialog(stage, user, row.getItem(), loadPage);
                }
            });
            return row;
        });

        javafx.scene.layout.BorderPane toolbar = new javafx.scene.layout.BorderPane();
        toolbar.setLeft(header);
        javafx.scene.layout.HBox rightBox = new javafx.scene.layout.HBox(12, filterBox, searchBox, createButton, editButton, resetPwdButton, toggleStatusButton);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        toolbar.setRight(rightBox);
        javafx.scene.layout.BorderPane.setAlignment(header, Pos.CENTER_LEFT);

        javafx.scene.layout.HBox statusBar = new javafx.scene.layout.HBox(15, accountSortStatusLabel, rowCountLabel, pageLabel, prevBtn, nextBtn);
        applyStandardTableStatusBar(statusBar);

        root.getChildren().addAll(toolbar, table, statusBar);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        loadPage.run();
        enableDeselectOnOutsideClick(root, table);
        return root;
    }

    private VBox createMyAccountView(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        VBox root = new VBox(18);
        root.setPadding(new Insets(24));

        Label header = new Label("My Account");
        header.getStyleClass().add("header-label");

        VBox infoCard = new VBox(10);
        infoCard.getStyleClass().add("report-section-card");
        infoCard.setStyle("-fx-padding: 20;");
        Label usernameLabel = new Label("Username: " + user.getUsername());
        Label fullNameLabel = new Label("Full Name: " + user.getFullName());
        Label roleLabel = new Label("Role: " + formatRoleLabel(user.getRole()));
        Label statusLabel = new Label("Status: " + formatUserStatus(user.isEnabled()));
        infoCard.getChildren().addAll(usernameLabel, fullNameLabel, roleLabel, statusLabel);

        VBox passwordCard = new VBox(12);
        passwordCard.getStyleClass().add("report-section-card");
        passwordCard.setStyle("-fx-padding: 20;");
        Label passwordTitle = new Label("Change Password");
        passwordTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: -app-text-primary;");
        PasswordField currentField = new PasswordField();
        currentField.setPromptText("Current Password");
        PasswordField newField = new PasswordField();
        newField.setPromptText("New Password");
        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Confirm New Password");
        Button changeButton = createExpandableGreenActionButton("Change Password", 190);
        changeButton.setOnAction(e -> {
            try {
                com.pbl3.project.pbl3_project.entity.User updatedUser =
                    userAccountService.changeOwnPassword(user, currentField.getText(), newField.getText(), confirmField.getText());
                syncSessionUser(user, updatedUser);
                currentField.clear();
                newField.clear();
                confirmField.clear();
                toastService.showSuccess("Password updated successfully");
            } catch (Exception ex) {
                toastService.showError(ex.getMessage());
            }
        });
        passwordCard.getChildren().addAll(passwordTitle, currentField, newField, confirmField, changeButton);

        root.getChildren().addAll(header, infoCard, passwordCard);
        return root;
    }

    private void showAccountUpsertDialog(
        Stage owner,
        com.pbl3.project.pbl3_project.entity.User actor,
        com.pbl3.project.pbl3_project.entity.User target,
        Runnable onSuccess
    ) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
        dialog.setTitle(target == null ? "Create Account" : "Edit Account");

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));

        TextField usernameField = new TextField(target != null ? target.getUsername() : "");
        usernameField.setPromptText("Username");
        TextField fullNameField = new TextField(target != null ? target.getFullName() : "");
        fullNameField.setPromptText("Full Name");
        javafx.scene.control.ComboBox<com.pbl3.project.pbl3_project.entity.Role> roleCombo = new javafx.scene.control.ComboBox<>();
        roleCombo.getItems().addAll(com.pbl3.project.pbl3_project.entity.Role.values());
        roleCombo.setValue(target != null ? target.getRole() : com.pbl3.project.pbl3_project.entity.Role.STAFF);
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText(target == null ? "Temporary Password" : "Optional New Password");
        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText(target == null ? "Confirm Password" : "Confirm New Password");

        Button saveBtn = createExpandableGreenActionButton(target == null ? "Create" : "Save", 120);
        saveBtn.setOnAction(e -> {
            try {
                com.pbl3.project.pbl3_project.entity.User updatedUser = null;
                if (target == null) {
                    if (!passwordField.getText().equals(confirmField.getText())) {
                        throw new RuntimeException("Password confirmation does not match");
                    }
                    userAccountService.createUser(actor, usernameField.getText(), passwordField.getText(), fullNameField.getText(), roleCombo.getValue());
                    toastService.showSuccess("Account created");
                } else {
                    updatedUser = userAccountService.updateUserProfile(actor, target.getId(), usernameField.getText(), fullNameField.getText());
                    if (target.getRole() != roleCombo.getValue()) {
                        updatedUser = userAccountService.changeUserRole(actor, target.getId(), roleCombo.getValue());
                    }
                    if (!passwordField.getText().isBlank() || !confirmField.getText().isBlank()) {
                        if (!passwordField.getText().equals(confirmField.getText())) {
                            throw new RuntimeException("Password confirmation does not match");
                        }
                        updatedUser = userAccountService.resetUserPassword(actor, target.getId(), passwordField.getText());
                    }
                    if (updatedUser != null) {
                        syncSessionUser(actor, updatedUser);
                    }
                    toastService.showSuccess("Account updated");
                }
                dialog.close();
                if (!authorizationService.canAccessAccounts(actor)) {
                    showOverviewScene(owner, actor);
                } else {
                    onSuccess.run();
                }
            } catch (Exception ex) {
                toastService.showError(ex.getMessage());
            }
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("filter-reset-btn");
        cancelBtn.setOnAction(e -> dialog.close());

        javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(10, cancelBtn, saveBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);
        root.getChildren().addAll(
            new Label(target == null ? "Create Account" : "Edit Account"),
            usernameField,
            fullNameField,
            roleCombo,
            passwordField,
            confirmField,
            actions
        );

        Scene scene = new Scene(root, 420, 320);
        scene.getStylesheets().add(getClass().getResource("/application.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void showResetAccountPasswordDialog(
        Stage owner,
        com.pbl3.project.pbl3_project.entity.User actor,
        com.pbl3.project.pbl3_project.entity.User target,
        Runnable onSuccess
    ) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
        dialog.setTitle("Reset Password");

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        Label title = new Label("Reset password for " + target.getUsername());
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("New Password");
        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Confirm New Password");
        Button resetBtn = createExpandableGreenActionButton("Reset Password", 170);
        resetBtn.setOnAction(e -> {
            try {
                if (!passwordField.getText().equals(confirmField.getText())) {
                    throw new RuntimeException("Password confirmation does not match");
                }
                userAccountService.resetUserPassword(actor, target.getId(), passwordField.getText());
                toastService.showSuccess("Password reset");
                dialog.close();
                onSuccess.run();
            } catch (Exception ex) {
                toastService.showError(ex.getMessage());
            }
        });
        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("filter-reset-btn");
        cancelBtn.setOnAction(e -> dialog.close());
        javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(10, cancelBtn, resetBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);
        root.getChildren().addAll(title, passwordField, confirmField, actions);

        Scene scene = new Scene(root, 420, 220);
        scene.getStylesheets().add(getClass().getResource("/application.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void showStockHistoryScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        if (!ensureAuthorized(() -> authorizationService.requireAuditLogAccess(user))) {
            return;
        }
        VBox content = createStockHistoryView(stage, user);
        switchScene(stage, user, "Audit Log", "nav-stock-history", content);
    }

    private void showStocktakeScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        if (!ensureAuthorized(() -> authorizationService.requireStocktakeAccess(user))) {
            return;
        }
        VBox content = createStocktakeView(stage, user);
        switchScene(stage, user, "Stocktake", "nav-stocktake", content);
    }

    private VBox createStockHistoryView(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));

        javafx.scene.layout.StackPane contentArea = new javafx.scene.layout.StackPane();
        java.util.List<java.util.function.Supplier<VBox>> viewFactories = java.util.List.of(
            () -> createInventoryAuditView(stage, user),
            () -> createOperationalAuditView(user),
            () -> createAccountAuditView(user)
        );
        VBox[] views = new VBox[viewFactories.size()];

        java.util.function.IntFunction<VBox> ensureViewLoaded = index -> {
            if (views[index] != null) {
                return views[index];
            }
            VBox view = viewFactories.get(index).get();
            views[index] = view;
            javafx.scene.control.TableView<?> table = findFirstTableView(view);
            if (table != null) {
                enableDeselectOnOutsideClick(root, table);
            }
            return view;
        };

        java.util.function.IntConsumer showView = index -> {
            VBox targetView = ensureViewLoaded.apply(index);
            contentArea.getChildren().setAll(targetView);
        };

        showView.accept(0);

        javafx.scene.Node slidingMenu = createSlidingMenu(
            new String[]{"Inventory Audit", "Operational Audit", "Account Audit"},
            showView::accept
        );

        VBox.setVgrow(contentArea, javafx.scene.layout.Priority.ALWAYS);
        root.getChildren().addAll(slidingMenu, contentArea);

        javafx.animation.PauseTransition preloadOperational = new javafx.animation.PauseTransition(Duration.millis(120));
        preloadOperational.setOnFinished(event -> {
            ensureViewLoaded.apply(1);
            javafx.animation.PauseTransition preloadAccount = new javafx.animation.PauseTransition(Duration.millis(120));
            preloadAccount.setOnFinished(nextEvent -> ensureViewLoaded.apply(2));
            preloadAccount.play();
        });
        javafx.application.Platform.runLater(preloadOperational::play);

        return root;
    }

    private VBox createInventoryAuditView(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        VBox root = new VBox();
        applyStandardTablePageLayout(root, Insets.EMPTY);
        final String inventoryAuditSortStateKey = "inventory-audit";
        TableSortState inventoryAuditSortState = getOrCreateTableSortState(
            inventoryAuditSortStateKey,
            new SortCriterion("createdAt", javafx.scene.control.TableColumn.SortType.DESCENDING)
        );
        java.util.LinkedHashMap<String, String> inventoryAuditSortProperties = new java.util.LinkedHashMap<>();
        inventoryAuditSortProperties.put("createdAt", "createdAt");
        inventoryAuditSortProperties.put("transactionType", "transactionType");
        inventoryAuditSortProperties.put("productName", "product.name");
        inventoryAuditSortProperties.put("quantityChange", "quantityChange");
        inventoryAuditSortProperties.put("username", "user.username");
        java.util.LinkedHashMap<String, String> inventoryAuditSortLabels = new java.util.LinkedHashMap<>();
        inventoryAuditSortLabels.put("createdAt", "Date");
        inventoryAuditSortLabels.put("transactionType", "Action");
        inventoryAuditSortLabels.put("productName", "Product");
        inventoryAuditSortLabels.put("quantityChange", "Change");
        inventoryAuditSortLabels.put("username", "User");

        Label header = new Label("Inventory Audit");
        header.getStyleClass().add("header-label");

        ExpandableSearchControl inventorySearchControl = createExpandableSearchControl(320);
        TextField hField = inventorySearchControl.field();

        javafx.scene.layout.BorderPane topBar = new javafx.scene.layout.BorderPane();
        // Filter Button (Transaction Type)
        javafx.scene.layout.HBox hFilterBox = new javafx.scene.layout.HBox();
        hFilterBox.setAlignment(Pos.CENTER);
        hFilterBox.getStyleClass().add("expandable-search-box");
        hFilterBox.setPrefSize(40, 40); hFilterBox.setMinSize(40, 40); hFilterBox.setMaxSize(40, 40);
        hFilterBox.setCursor(javafx.scene.Cursor.HAND);
        javafx.scene.shape.SVGPath hFilterIcon = new javafx.scene.shape.SVGPath();
        hFilterIcon.setContent("M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z");
        hFilterIcon.setFill(PRIMARY_COLOR);
        hFilterBox.getChildren().add(hFilterIcon);

        javafx.scene.layout.HBox hRightBox = new javafx.scene.layout.HBox(12, hFilterBox, inventorySearchControl.box());
        hRightBox.setAlignment(Pos.CENTER_RIGHT);
        topBar.setLeft(header); topBar.setRight(hRightBox);
        javafx.scene.layout.BorderPane.setAlignment(header, Pos.CENTER_LEFT);

        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.InventoryTransaction> table = new javafx.scene.control.TableView<>();
        applyStandardTableSizing(table);
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        enableDragSelection(table);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.InventoryTransaction, String> dateCol = new javafx.scene.control.TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> {
            java.time.LocalDateTime dt = data.getValue().getCreatedAt();
            return new javafx.beans.property.SimpleStringProperty(formatDateTime(dt));
        });

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.InventoryTransaction, String> typeCol = new javafx.scene.control.TableColumn<>("Action");
        typeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatTransactionTypeLabel(data.getValue().getTransactionType())));
        typeCol.setStyle("-fx-alignment: CENTER;");
        typeCol.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                com.pbl3.project.pbl3_project.entity.InventoryTransaction tx = getTableRow() != null ? getTableRow().getItem() : null;
                String textColor = getTransactionTypeColor(tx != null ? tx.getTransactionType() : null);
                setStyle("-fx-text-fill: " + textColor + "; -fx-font-weight: 700; -fx-alignment: CENTER;");
            }
        });

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.InventoryTransaction, String> productCol = new javafx.scene.control.TableColumn<>("Product");
        productCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getProduct() != null ? data.getValue().getProduct().getName() : "Unknown"
        ));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.InventoryTransaction, String> qtyCol = new javafx.scene.control.TableColumn<>("Change");
        qtyCol.setCellValueFactory(data -> {
            Integer changeParam = data.getValue().getQuantityChange();
            int change = changeParam != null ? changeParam : 0;
            String prefix = change > 0 ? "+" : "";
            return new javafx.beans.property.SimpleStringProperty(prefix + change);
        });
        qtyCol.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.InventoryTransaction, String> userCol = new javafx.scene.control.TableColumn<>("User");
        userCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getUser() != null ? data.getValue().getUser().getUsername() : "System"
        ));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.InventoryTransaction, String> notesCol = new javafx.scene.control.TableColumn<>("Notes/Ref");
        notesCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getNotes()));

        table.getColumns().addAll(dateCol, typeCol, productCol, qtyCol, userCol, notesCol);
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        java.util.LinkedHashMap<String, javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.InventoryTransaction, ?>> inventoryAuditSortColumns =
            new java.util.LinkedHashMap<>();
        inventoryAuditSortColumns.put("createdAt", dateCol);
        inventoryAuditSortColumns.put("transactionType", typeCol);
        inventoryAuditSortColumns.put("productName", productCol);
        inventoryAuditSortColumns.put("quantityChange", qtyCol);
        inventoryAuditSortColumns.put("username", userCol);
        installSortHeaderIndicators(inventoryAuditSortColumns);

        final int txPageSize = 25;
        final int[] txCurrentPage = {0};
        final int[] txTotalPages = {0};
        final long[] txTotalElements = {0L};
        java.util.concurrent.atomic.AtomicReference<String> txSearchRef = new java.util.concurrent.atomic.AtomicReference<>("");
        java.util.concurrent.atomic.AtomicReference<java.time.LocalDate> txStartDateRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<java.time.LocalDate> txEndDateRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<java.util.Set<String>> txUsersRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<java.util.Set<String>> txTypesRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<Double> txMinAbsQtyRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<Double> txMaxAbsQtyRef = new java.util.concurrent.atomic.AtomicReference<>(null);

        Label txRowCountLabel = createStatusMetaLabel("Showing 0-0 of 0 Row(s)");
        Label txPageLabel = createStatusMetaLabel("Page 0 / 0");
        Button txPrevBtn = createPageNavButton("Prev");
        Button txNextBtn = createPageNavButton("Next");

        Runnable updateTxStatusBar = () -> updatePagedStatus(
            table,
            txRowCountLabel,
            txPageLabel,
            txPrevBtn,
            txNextBtn,
            txTotalElements[0],
            txCurrentPage[0],
            txTotalPages[0],
            txPageSize
        );
        Runnable loadTransactionPage = () -> {
            org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.InventoryTransaction> pageData =
                transactionService.searchTransactions(
                    txSearchRef.get(),
                    txStartDateRef.get(),
                    txEndDateRef.get(),
                    txUsersRef.get(),
                    txTypesRef.get(),
                    txMinAbsQtyRef.get(),
                    txMaxAbsQtyRef.get(),
                    createPageable(inventoryAuditSortState, inventoryAuditSortProperties, txCurrentPage[0], txPageSize)
                );
            if (pageData.getTotalPages() > 0 && txCurrentPage[0] >= pageData.getTotalPages()) {
                txCurrentPage[0] = pageData.getTotalPages() - 1;
                pageData = transactionService.searchTransactions(
                    txSearchRef.get(),
                    txStartDateRef.get(),
                    txEndDateRef.get(),
                    txUsersRef.get(),
                    txTypesRef.get(),
                    txMinAbsQtyRef.get(),
                    txMaxAbsQtyRef.get(),
                    createPageable(inventoryAuditSortState, inventoryAuditSortProperties, txCurrentPage[0], txPageSize)
                );
            }
            txTotalElements[0] = pageData.getTotalElements();
            txTotalPages[0] = pageData.getTotalPages();
            table.setItems(javafx.collections.FXCollections.observableArrayList(pageData.getContent()));
            updateTxStatusBar.run();
        };
        Label inventoryAuditSortStatusLabel = createSortStatusLabel(inventoryAuditSortState, inventoryAuditSortLabels);
        Runnable applyInventoryAuditSortUi = () -> {
            applySortStateToTable(table, inventoryAuditSortColumns, inventoryAuditSortState);
            inventoryAuditSortStatusLabel.setText(buildSortStatusText(inventoryAuditSortState, inventoryAuditSortLabels));
        };
        applyInventoryAuditSortUi.run();
        installManualServerSorting(
            table,
            inventoryAuditSortColumns,
            inventoryAuditSortState,
            () -> {
                applyInventoryAuditSortUi.run();
                txCurrentPage[0] = 0;
                loadTransactionPage.run();
            }
        );
        txPrevBtn.setOnAction(e -> {
            if (txCurrentPage[0] > 0) {
                txCurrentPage[0]--;
                loadTransactionPage.run();
            }
        });
        txNextBtn.setOnAction(e -> {
            if (txCurrentPage[0] + 1 < txTotalPages[0]) {
                txCurrentPage[0]++;
                loadTransactionPage.run();
            }
        });

        javafx.animation.PauseTransition txSearchPause = new javafx.animation.PauseTransition(Duration.millis(220));
        txSearchPause.setOnFinished(e -> {
            txCurrentPage[0] = 0;
            txSearchRef.set(hField.getText());
            loadTransactionPage.run();
        });
        hField.textProperty().addListener((obs, oldV, newV) -> txSearchPause.playFromStart());

        javafx.scene.layout.HBox txStatusBar = new javafx.scene.layout.HBox(15, inventoryAuditSortStatusLabel, txRowCountLabel, txPageLabel, txPrevBtn, txNextBtn);
        applyStandardTableStatusBar(txStatusBar);

        // Wire up transaction type filter
        javafx.stage.Popup hFilterPopup = new javafx.stage.Popup();
        hFilterPopup.setAutoHide(true);

        hFilterBox.setOnMouseClicked(fev -> {
            if (hFilterPopup.isShowing()) {
                hFilterPopup.hide();
                return;
            }

            VBox popupContainer = new VBox(10);
            popupContainer.setPadding(new Insets(15));
            applyFilterPopupContainerStyle(popupContainer);
            popupContainer.setPrefWidth(350);

            VBox scrollContent = new VBox(10);
            scrollContent.setStyle("-fx-background-color: -app-surface;");
            scrollContent.setPadding(new Insets(5, 15, 5, 15));
            javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(scrollContent);
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background: -app-surface; -fx-background-color: transparent; -fx-border-color: transparent;");
            scrollPane.setPrefViewportHeight(350);

            // --- Date Range ---
            Label dateTitle = new Label("Date Range");
            dateTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");
            javafx.scene.control.DatePicker startDatePicker = new javafx.scene.control.DatePicker();
            startDatePicker.setPromptText("Start Date");
            startDatePicker.setPrefWidth(140);
            startDatePicker.setStyle("-fx-font-size: 13px;");
            javafx.scene.control.DatePicker endDatePicker = new javafx.scene.control.DatePicker();
            endDatePicker.setPromptText("End Date");
            endDatePicker.setPrefWidth(140);
            endDatePicker.setStyle("-fx-font-size: 13px;");
            customizeDatePicker(startDatePicker);
            customizeDatePicker(endDatePicker);
            javafx.scene.layout.HBox dateBox = new javafx.scene.layout.HBox(5, startDatePicker, new Label("-"), endDatePicker);
            dateBox.setAlignment(Pos.CENTER_LEFT);

            javafx.scene.control.Separator sepDate = new javafx.scene.control.Separator();

            // --- User Filter ---
            Label userTitle = new Label("User");
            userTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");
            
            javafx.scene.control.CheckBox allUsersCb = new javafx.scene.control.CheckBox("All Users");
            allUsersCb.setSelected(true);
            allUsersCb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");

            VBox userScroll = new VBox(8);
            userScroll.setPadding(new Insets(5, 5, 5, 20));
            
            java.util.List<javafx.scene.control.CheckBox> userCbs = new java.util.ArrayList<>();
            java.util.Set<String> userNames = new java.util.LinkedHashSet<>(transactionService.getTransactionUsernames());
            
            for (String uName : userNames) {
                if (uName.trim().isEmpty()) continue;
                javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(uName);
                cb.setSelected(true);
                cb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                cb.setOnAction(e -> {
                    if (!cb.isSelected()) allUsersCb.setSelected(false);
                    else {
                        boolean all = true;
                        for (javafx.scene.control.CheckBox c : userCbs) if (!c.isSelected()) all = false;
                        allUsersCb.setSelected(all);
                    }
                });
                userCbs.add(cb);
                userScroll.getChildren().add(cb);
            }
            allUsersCb.setOnAction(e -> {
                boolean sel = allUsersCb.isSelected();
                for (javafx.scene.control.CheckBox cb : userCbs) cb.setSelected(sel);
            });
            FilterDisclosureSection userSection = new FilterDisclosureSection(allUsersCb, userScroll);

            javafx.scene.control.Separator sepUser = new javafx.scene.control.Separator();

            // --- Transaction Type Filter ---
            Label typeTitle = new Label("Transaction Type");
            typeTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");
            
            javafx.scene.control.CheckBox allTypesCb = new javafx.scene.control.CheckBox("All Types");
            allTypesCb.setSelected(true);
            allTypesCb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");

            VBox typeScroll = new VBox(8);
            typeScroll.setPadding(new Insets(5, 5, 5, 20));
            
            java.util.List<javafx.scene.control.CheckBox> typeCbs = new java.util.ArrayList<>();
            String[] types = {"IMPORT", "CANCEL_IMPORT", "SALE", "CANCEL_SALE", "RETURN", "MANUAL_ADJUST", "REVALUE", "DELETE"};
            for (String type : types) {
                javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(formatTransactionTypeLabel(type));
                cb.setUserData(type);
                cb.setSelected(true);
                cb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                cb.setOnAction(e -> {
                    if (!cb.isSelected()) allTypesCb.setSelected(false);
                    else {
                        boolean all = true;
                        for (javafx.scene.control.CheckBox c : typeCbs) if (!c.isSelected()) all = false;
                        allTypesCb.setSelected(all);
                    }
                });
                typeCbs.add(cb);
                typeScroll.getChildren().add(cb);
            }

            allTypesCb.setOnAction(e -> {
                boolean sel = allTypesCb.isSelected();
                for (javafx.scene.control.CheckBox cb : typeCbs) cb.setSelected(sel);
            });
            FilterDisclosureSection typeSection = new FilterDisclosureSection(allTypesCb, typeScroll);

            javafx.scene.control.Separator sepType = new javafx.scene.control.Separator();

            // --- Quantity Range Filter ---
            Label qtyTitle = new Label("Quantity Change Range");
            qtyTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");
            
            double maxQty = transactionService.getTransactionMaxAbsoluteQuantity();
            if (maxQty == 0) maxQty = 100;
            
            Label qtyLabel = new Label("0 - " + String.format("%.0f", maxQty));
            qtyLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -app-primary; -fx-font-weight: bold;");
            
            RangeSlider qtySlider = new RangeSlider(0, maxQty, 0, maxQty, 280);
            qtySlider.minVal.addListener((o, ov, nv) -> qtyLabel.setText(String.format("%.0f - %.0f", nv.doubleValue(), qtySlider.maxVal.get())));
            qtySlider.maxVal.addListener((o, ov, nv) -> qtyLabel.setText(String.format("%.0f - %.0f", qtySlider.minVal.get(), nv.doubleValue())));

            scrollContent.getChildren().addAll(
                dateTitle, dateBox, sepDate,
                userTitle, userSection.getNode(), sepUser,
                typeTitle, typeSection.getNode(), sepType,
                qtyTitle, qtyLabel, qtySlider
            );

            // --- Buttons ---
            javafx.scene.layout.HBox btnRow = new javafx.scene.layout.HBox(10);
            btnRow.setAlignment(Pos.CENTER_RIGHT);
            
            final double fMaxQty = maxQty;

            Button resetBtn = new Button("Reset");
            resetBtn.getStyleClass().add("filter-reset-btn");
            resetBtn.setOnAction(ae -> {
                hFilterBox.setStyle("");
                startDatePicker.setValue(null);
                endDatePicker.setValue(null);
                allUsersCb.setSelected(true);
                for (javafx.scene.control.CheckBox cb : userCbs) cb.setSelected(true);
                userSection.setExpanded(false);
                allTypesCb.setSelected(true);
                for (javafx.scene.control.CheckBox cb : typeCbs) cb.setSelected(true);
                typeSection.setExpanded(false);
                qtySlider.minVal.set(0); qtySlider.maxVal.set(fMaxQty);
                txStartDateRef.set(null);
                txEndDateRef.set(null);
                txUsersRef.set(new java.util.LinkedHashSet<>());
                txTypesRef.set(new java.util.LinkedHashSet<>());
                txMinAbsQtyRef.set(null);
                txMaxAbsQtyRef.set(null);
                txCurrentPage[0] = 0;
                loadTransactionPage.run();
                hFilterPopup.hide();
            });

            Button applyBtn = new Button("Apply Filter");
            applyBtn.getStyleClass().add("filter-apply-btn");
            applyBtn.setOnAction(ae -> {
                java.util.Set<String> selectedUsers = new java.util.HashSet<>();
                for (javafx.scene.control.CheckBox cb : userCbs) {
                    if (cb.isSelected()) selectedUsers.add(cb.getText());
                }
                
                java.util.Set<String> selectedTypes = new java.util.HashSet<>();
                for (javafx.scene.control.CheckBox cb : typeCbs) {
                    if (cb.isSelected()) selectedTypes.add((String) cb.getUserData());
                }
                double qMin = qtySlider.minVal.get();
                double qMax = qtySlider.maxVal.get();
                java.time.LocalDate sDate = startDatePicker.getValue();
                java.time.LocalDate eDate = endDatePicker.getValue();

                txStartDateRef.set(sDate);
                txEndDateRef.set(eDate);
                txUsersRef.set(allUsersCb.isSelected() ? new java.util.LinkedHashSet<>() : selectedUsers);
                txTypesRef.set(allTypesCb.isSelected() ? new java.util.LinkedHashSet<>() : selectedTypes);
                txMinAbsQtyRef.set(qMin <= 0 ? null : qMin);
                txMaxAbsQtyRef.set(qMax >= fMaxQty ? null : qMax);
                txCurrentPage[0] = 0;
                loadTransactionPage.run();

                boolean hasFilter = !allTypesCb.isSelected() || !allUsersCb.isSelected() || qMin > 0 || qMax < fMaxQty || sDate != null || eDate != null;
                hFilterBox.setStyle(hasFilter ? "-fx-border-color: -app-primary;" : "");
                hFilterPopup.hide();
            });

            btnRow.getChildren().addAll(resetBtn, applyBtn);

            popupContainer.getChildren().addAll(scrollPane, btnRow);
            hFilterPopup.getContent().clear();
            hFilterPopup.getContent().add(popupContainer);

            showPopupBelow(hFilterPopup, hFilterBox, -290, 5);
        });

        root.getChildren().addAll(topBar, table, txStatusBar);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);

        table.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<com.pbl3.project.pbl3_project.entity.InventoryTransaction>) c -> updateTxStatusBar.run());
        loadTransactionPage.run();
        enableDeselectOnOutsideClick(root, table);
        return root;
    }

    private VBox createOperationalAuditView(com.pbl3.project.pbl3_project.entity.User user) {
        VBox root = new VBox();
        applyStandardTablePageLayout(root, Insets.EMPTY);
        final String operationalAuditSortStateKey = "operational-audit";
        TableSortState operationalAuditSortState = getOrCreateTableSortState(
            operationalAuditSortStateKey,
            new SortCriterion("createdAt", javafx.scene.control.TableColumn.SortType.DESCENDING)
        );
        java.util.LinkedHashMap<String, String> operationalAuditSortProperties = new java.util.LinkedHashMap<>();
        operationalAuditSortProperties.put("createdAt", "createdAt");
        operationalAuditSortProperties.put("actorUsername", "actor.username");
        operationalAuditSortProperties.put("action", "action");
        operationalAuditSortProperties.put("subjectType", "subjectType");
        operationalAuditSortProperties.put("subjectLabel", "subjectLabel");
        java.util.LinkedHashMap<String, String> operationalAuditSortLabels = new java.util.LinkedHashMap<>();
        operationalAuditSortLabels.put("createdAt", "Date");
        operationalAuditSortLabels.put("actorUsername", "Actor");
        operationalAuditSortLabels.put("action", "Action");
        operationalAuditSortLabels.put("subjectType", "Subject");
        operationalAuditSortLabels.put("subjectLabel", "Label");

        Label header = new Label("Operational Audit");
        header.getStyleClass().add("header-label");

        ExpandableSearchControl searchControl = createExpandableSearchControl(340);
        TextField searchField = searchControl.field();

        javafx.scene.layout.HBox filterBox = new javafx.scene.layout.HBox();
        filterBox.setAlignment(Pos.CENTER);
        filterBox.getStyleClass().add("expandable-search-box");
        filterBox.setPrefSize(40, 40);
        filterBox.setMinSize(40, 40);
        filterBox.setMaxSize(40, 40);
        filterBox.setCursor(javafx.scene.Cursor.HAND);
        javafx.scene.shape.SVGPath filterIcon = new javafx.scene.shape.SVGPath();
        filterIcon.setContent("M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z");
        filterIcon.setFill(PRIMARY_COLOR);
        filterBox.getChildren().add(filterIcon);

        javafx.scene.layout.HBox rightBox = new javafx.scene.layout.HBox(12, filterBox, searchControl.box());
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        javafx.scene.layout.BorderPane topBar = new javafx.scene.layout.BorderPane();
        topBar.setLeft(header);
        topBar.setRight(rightBox);
        javafx.scene.layout.BorderPane.setAlignment(header, Pos.CENTER_LEFT);

        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.OperationalAuditLog> table = new javafx.scene.control.TableView<>();
        applyStandardTableSizing(table);
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        enableDragSelection(table);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.OperationalAuditLog, String> dateCol = new javafx.scene.control.TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatDateTime(data.getValue().getCreatedAt())));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.OperationalAuditLog, String> actorCol = new javafx.scene.control.TableColumn<>("Actor");
        actorCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getActor() != null ? data.getValue().getActor().getUsername() : "System"
        ));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.OperationalAuditLog, String> actionCol = new javafx.scene.control.TableColumn<>("Action");
        actionCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatOperationalAuditActionLabel(data.getValue().getAction())));
        actionCol.setStyle("-fx-alignment: CENTER;");

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.OperationalAuditLog, String> subjectTypeCol = new javafx.scene.control.TableColumn<>("Subject");
        subjectTypeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatOperationalSubjectTypeLabel(data.getValue().getSubjectType())));
        subjectTypeCol.setStyle("-fx-alignment: CENTER;");

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.OperationalAuditLog, String> subjectLabelCol = new javafx.scene.control.TableColumn<>("Label");
        subjectLabelCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getSubjectLabel() == null ? "" : data.getValue().getSubjectLabel()
        ));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.OperationalAuditLog, String> detailsCol = new javafx.scene.control.TableColumn<>("Details");
        detailsCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getDetails() == null ? "" : data.getValue().getDetails()
        ));

        table.getColumns().addAll(dateCol, actorCol, actionCol, subjectTypeCol, subjectLabelCol, detailsCol);
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        java.util.LinkedHashMap<String, javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.OperationalAuditLog, ?>> operationalAuditSortColumns =
            new java.util.LinkedHashMap<>();
        operationalAuditSortColumns.put("createdAt", dateCol);
        operationalAuditSortColumns.put("actorUsername", actorCol);
        operationalAuditSortColumns.put("action", actionCol);
        operationalAuditSortColumns.put("subjectType", subjectTypeCol);
        operationalAuditSortColumns.put("subjectLabel", subjectLabelCol);
        installSortHeaderIndicators(operationalAuditSortColumns);

        final int pageSize = 25;
        final int[] currentPage = {0};
        final int[] totalPages = {0};
        final long[] totalElements = {0L};
        java.util.concurrent.atomic.AtomicReference<String> searchRef = new java.util.concurrent.atomic.AtomicReference<>("");
        java.util.concurrent.atomic.AtomicReference<java.time.LocalDate> startDateRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<java.time.LocalDate> endDateRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<java.util.Set<String>> actorUsernamesRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<java.util.Set<com.pbl3.project.pbl3_project.entity.OperationalAuditAction>> actionsRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<java.util.Set<com.pbl3.project.pbl3_project.entity.OperationalSubjectType>> subjectTypesRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());

        Label rowCountLabel = createStatusMetaLabel("Showing 0-0 of 0 Row(s)");
        Label pageLabel = createStatusMetaLabel("Page 0 / 0");
        Button prevBtn = createPageNavButton("Prev");
        Button nextBtn = createPageNavButton("Next");

        Runnable updateStatusBar = () -> updatePagedStatus(
            table,
            rowCountLabel,
            pageLabel,
            prevBtn,
            nextBtn,
            totalElements[0],
            currentPage[0],
            totalPages[0],
            pageSize
        );
        Runnable loadPage = () -> {
            org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.OperationalAuditLog> pageData =
                operationalAuditLogService.searchOperationalAuditLogs(
                    user,
                    searchRef.get(),
                    startDateRef.get(),
                    endDateRef.get(),
                    actorUsernamesRef.get(),
                    actionsRef.get(),
                    subjectTypesRef.get(),
                    createPageable(operationalAuditSortState, operationalAuditSortProperties, currentPage[0], pageSize)
                );
            if (pageData.getTotalPages() > 0 && currentPage[0] >= pageData.getTotalPages()) {
                currentPage[0] = pageData.getTotalPages() - 1;
                pageData = operationalAuditLogService.searchOperationalAuditLogs(
                    user,
                    searchRef.get(),
                    startDateRef.get(),
                    endDateRef.get(),
                    actorUsernamesRef.get(),
                    actionsRef.get(),
                    subjectTypesRef.get(),
                    createPageable(operationalAuditSortState, operationalAuditSortProperties, currentPage[0], pageSize)
                );
            }
            totalElements[0] = pageData.getTotalElements();
            totalPages[0] = pageData.getTotalPages();
            table.setItems(javafx.collections.FXCollections.observableArrayList(pageData.getContent()));
            updateStatusBar.run();
        };
        Label operationalAuditSortStatusLabel = createSortStatusLabel(operationalAuditSortState, operationalAuditSortLabels);
        Runnable applyOperationalAuditSortUi = () -> {
            applySortStateToTable(table, operationalAuditSortColumns, operationalAuditSortState);
            operationalAuditSortStatusLabel.setText(buildSortStatusText(operationalAuditSortState, operationalAuditSortLabels));
        };
        applyOperationalAuditSortUi.run();
        installManualServerSorting(
            table,
            operationalAuditSortColumns,
            operationalAuditSortState,
            () -> {
                applyOperationalAuditSortUi.run();
                currentPage[0] = 0;
                loadPage.run();
            }
        );

        prevBtn.setOnAction(e -> {
            if (currentPage[0] > 0) {
                currentPage[0]--;
                loadPage.run();
            }
        });
        nextBtn.setOnAction(e -> {
            if (currentPage[0] + 1 < totalPages[0]) {
                currentPage[0]++;
                loadPage.run();
            }
        });

        javafx.animation.PauseTransition searchPause = new javafx.animation.PauseTransition(Duration.millis(220));
        searchPause.setOnFinished(e -> {
            currentPage[0] = 0;
            searchRef.set(searchField.getText());
            loadPage.run();
        });
        searchField.textProperty().addListener((obs, oldV, newV) -> searchPause.playFromStart());

        javafx.scene.layout.HBox statusBar = new javafx.scene.layout.HBox(15, operationalAuditSortStatusLabel, rowCountLabel, pageLabel, prevBtn, nextBtn);
        applyStandardTableStatusBar(statusBar);

        javafx.stage.Popup filterPopup = new javafx.stage.Popup();
        filterPopup.setAutoHide(true);
        filterBox.setOnMouseClicked(event -> {
            if (filterPopup.isShowing()) {
                filterPopup.hide();
                return;
            }

            FilterPopupShell filterShell = createFilterPopupShell(360, 250);
            VBox popupContainer = filterShell.container();
            VBox scrollContent = filterShell.content();

            Label dateTitle = createFilterPopupSectionTitle("Date Range");
            javafx.scene.control.DatePicker startDatePicker = new javafx.scene.control.DatePicker(startDateRef.get());
            startDatePicker.setPromptText("Start Date");
            startDatePicker.setPrefWidth(140);
            javafx.scene.control.DatePicker endDatePicker = new javafx.scene.control.DatePicker(endDateRef.get());
            endDatePicker.setPromptText("End Date");
            endDatePicker.setPrefWidth(140);
            customizeDatePicker(startDatePicker);
            customizeDatePicker(endDatePicker);
            javafx.scene.layout.HBox dateBox = new javafx.scene.layout.HBox(5, startDatePicker, new Label("-"), endDatePicker);
            dateBox.setAlignment(Pos.CENTER_LEFT);

            Label actorTitle = createFilterPopupSectionTitle("Actor");
            javafx.scene.control.ComboBox<String> actorCombo = new javafx.scene.control.ComboBox<>();
            actorCombo.getItems().add("All Actors");
            actorCombo.getItems().addAll(operationalAuditLogService.getActorUsernames(user));
            actorCombo.setValue(actorUsernamesRef.get().isEmpty() ? "All Actors" : actorUsernamesRef.get().iterator().next());
            actorCombo.setMaxWidth(Double.MAX_VALUE);

            Label actionTitle = createFilterPopupSectionTitle("Action");
            javafx.scene.control.ComboBox<String> actionCombo = new javafx.scene.control.ComboBox<>();
            java.util.Map<String, com.pbl3.project.pbl3_project.entity.OperationalAuditAction> actionLookup = new java.util.LinkedHashMap<>();
            actionCombo.getItems().add("All Actions");
            for (com.pbl3.project.pbl3_project.entity.OperationalAuditAction action : com.pbl3.project.pbl3_project.entity.OperationalAuditAction.values()) {
                String label = formatOperationalAuditActionLabel(action);
                actionLookup.put(label, action);
                actionCombo.getItems().add(label);
            }
            actionCombo.setValue(actionsRef.get().isEmpty() ? "All Actions" : formatOperationalAuditActionLabel(actionsRef.get().iterator().next()));
            actionCombo.setMaxWidth(Double.MAX_VALUE);

            Label subjectTitle = createFilterPopupSectionTitle("Subject");
            javafx.scene.control.ComboBox<String> subjectCombo = new javafx.scene.control.ComboBox<>();
            java.util.Map<String, com.pbl3.project.pbl3_project.entity.OperationalSubjectType> subjectLookup = new java.util.LinkedHashMap<>();
            subjectCombo.getItems().add("All Subjects");
            for (com.pbl3.project.pbl3_project.entity.OperationalSubjectType type : com.pbl3.project.pbl3_project.entity.OperationalSubjectType.values()) {
                String label = formatOperationalSubjectTypeLabel(type);
                subjectLookup.put(label, type);
                subjectCombo.getItems().add(label);
            }
            subjectCombo.setValue(subjectTypesRef.get().isEmpty() ? "All Subjects" : formatOperationalSubjectTypeLabel(subjectTypesRef.get().iterator().next()));
            subjectCombo.setMaxWidth(Double.MAX_VALUE);

            Button resetBtn = new Button("Reset");
            resetBtn.getStyleClass().add("filter-reset-btn");
            resetBtn.setOnAction(e -> {
                filterBox.setStyle("");
                startDateRef.set(null);
                endDateRef.set(null);
                actorUsernamesRef.set(new java.util.LinkedHashSet<>());
                actionsRef.set(new java.util.LinkedHashSet<>());
                subjectTypesRef.set(new java.util.LinkedHashSet<>());
                currentPage[0] = 0;
                loadPage.run();
                filterPopup.hide();
            });

            Button applyBtn = new Button("Apply Filter");
            applyBtn.getStyleClass().add("filter-apply-btn");
            applyBtn.setOnAction(e -> {
                startDateRef.set(startDatePicker.getValue());
                endDateRef.set(endDatePicker.getValue());

                java.util.Set<String> actorFilters = new java.util.LinkedHashSet<>();
                if (!"All Actors".equals(actorCombo.getValue())) {
                    actorFilters.add(actorCombo.getValue());
                }
                actorUsernamesRef.set(actorFilters);

                java.util.Set<com.pbl3.project.pbl3_project.entity.OperationalAuditAction> actionFilters = new java.util.LinkedHashSet<>();
                if (!"All Actions".equals(actionCombo.getValue())) {
                    actionFilters.add(actionLookup.get(actionCombo.getValue()));
                }
                actionsRef.set(actionFilters);

                java.util.Set<com.pbl3.project.pbl3_project.entity.OperationalSubjectType> subjectFilters = new java.util.LinkedHashSet<>();
                if (!"All Subjects".equals(subjectCombo.getValue())) {
                    subjectFilters.add(subjectLookup.get(subjectCombo.getValue()));
                }
                subjectTypesRef.set(subjectFilters);

                currentPage[0] = 0;
                loadPage.run();
                boolean hasFilter = startDateRef.get() != null
                    || endDateRef.get() != null
                    || !actorUsernamesRef.get().isEmpty()
                    || !actionsRef.get().isEmpty()
                    || !subjectTypesRef.get().isEmpty();
                filterBox.setStyle(hasFilter ? "-fx-border-color: -app-primary;" : "");
                filterPopup.hide();
            });

            javafx.scene.layout.HBox buttonRow = createFilterPopupActionRow(resetBtn, applyBtn);

            scrollContent.getChildren().addAll(
                dateTitle, dateBox,
                new javafx.scene.control.Separator(),
                actorTitle, actorCombo,
                actionTitle, actionCombo,
                subjectTitle, subjectCombo,
                new javafx.scene.control.Separator()
            );
            popupContainer.getChildren().add(buttonRow);
            filterPopup.getContent().clear();
            filterPopup.getContent().add(popupContainer);

            showPopupBelow(filterPopup, filterBox, -280, 5);
        });

        root.getChildren().addAll(topBar, table, statusBar);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        loadPage.run();
        enableDeselectOnOutsideClick(root, table);
        return root;
    }

    private VBox createStocktakeView(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        VBox root = new VBox();
        applyStandardTablePageLayout(root);

        final String stocktakeSortStateKey = "stocktake-sessions";
        TableSortState stocktakeSortState = getOrCreateTableSortState(
            stocktakeSortStateKey,
            new SortCriterion("createdAt", javafx.scene.control.TableColumn.SortType.DESCENDING)
        );
        java.util.LinkedHashMap<String, String> stocktakeSortProperties = new java.util.LinkedHashMap<>();
        stocktakeSortProperties.put("createdAt", "createdAt");
        stocktakeSortProperties.put("status", "status");
        stocktakeSortProperties.put("scopeType", "scopeType");
        stocktakeSortProperties.put("createdBy", "createdBy.username");
        java.util.LinkedHashMap<String, String> stocktakeSortLabels = new java.util.LinkedHashMap<>();
        stocktakeSortLabels.put("createdAt", "Created At");
        stocktakeSortLabels.put("status", "Status");
        stocktakeSortLabels.put("scopeType", "Scope");
        stocktakeSortLabels.put("createdBy", "Created By");

        Label header = new Label("Stocktake");
        header.getStyleClass().add("header-label");

        ExpandableSearchControl searchControl = createExpandableSearchControl(320);
        TextField searchField = searchControl.field();

        javafx.scene.layout.HBox filterBox = new javafx.scene.layout.HBox();
        filterBox.setAlignment(Pos.CENTER);
        filterBox.getStyleClass().add("expandable-search-box");
        filterBox.setPrefSize(40, 40);
        filterBox.setMinSize(40, 40);
        filterBox.setMaxSize(40, 40);
        filterBox.setCursor(javafx.scene.Cursor.HAND);
        javafx.scene.shape.SVGPath filterIcon = new javafx.scene.shape.SVGPath();
        filterIcon.setContent("M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z");
        filterIcon.setFill(PRIMARY_COLOR);
        filterBox.getChildren().add(filterIcon);

        Button newSessionButton = createExpandableGreenActionButton("New Session", 160);
        Button openButton = createExpandableManageActionButton("Open", 120);
        Button applyButton = createExpandableManageActionButton("Apply", 120);
        Button cancelButton = createExpandableManageActionButton("Cancel", 130);

        javafx.scene.layout.HBox rightBox = new javafx.scene.layout.HBox(
            12,
            newSessionButton,
            openButton,
            applyButton,
            cancelButton,
            filterBox,
            searchControl.box()
        );
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        javafx.scene.layout.BorderPane topBar = new javafx.scene.layout.BorderPane();
        topBar.setLeft(header);
        topBar.setRight(rightBox);

        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.StocktakeSession> table = new javafx.scene.control.TableView<>();
        applyStandardTableSizing(table);
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        enableDragSelection(table);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.StocktakeSession, Number> idCol = new javafx.scene.control.TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new javafx.beans.property.SimpleLongProperty(data.getValue().getId()));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.StocktakeSession, String> createdAtCol = new javafx.scene.control.TableColumn<>("Created At");
        createdAtCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatDateTime(data.getValue().getCreatedAt())));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.StocktakeSession, String> scopeCol = new javafx.scene.control.TableColumn<>("Scope");
        scopeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatStocktakeScopeLabel(data.getValue().getScopeType())));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.StocktakeSession, String> categoryCol = new javafx.scene.control.TableColumn<>("Category");
        categoryCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getCategory() != null ? data.getValue().getCategory().getName() : "-"
        ));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.StocktakeSession, String> statusCol = new javafx.scene.control.TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatStocktakeStatusLabel(data.getValue().getStatus())));
        statusCol.setStyle("-fx-alignment: CENTER;");

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.StocktakeSession, String> createdByCol = new javafx.scene.control.TableColumn<>("Created By");
        createdByCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getCreatedBy() != null ? data.getValue().getCreatedBy().getUsername() : "System"
        ));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.StocktakeSession, String> notesCol = new javafx.scene.control.TableColumn<>("Notes");
        notesCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getNotes() == null ? "" : data.getValue().getNotes()
        ));

        table.getColumns().addAll(idCol, createdAtCol, scopeCol, categoryCol, statusCol, createdByCol, notesCol);
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);

        java.util.LinkedHashMap<String, javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.StocktakeSession, ?>> stocktakeSortColumns =
            new java.util.LinkedHashMap<>();
        stocktakeSortColumns.put("createdAt", createdAtCol);
        stocktakeSortColumns.put("status", statusCol);
        stocktakeSortColumns.put("scopeType", scopeCol);
        stocktakeSortColumns.put("createdBy", createdByCol);
        installSortHeaderIndicators(stocktakeSortColumns);

        final int pageSize = 20;
        final int[] currentPage = {0};
        final int[] totalPages = {0};
        final long[] totalElements = {0L};
        java.util.concurrent.atomic.AtomicReference<String> searchRef = new java.util.concurrent.atomic.AtomicReference<>("");
        java.util.concurrent.atomic.AtomicReference<java.time.LocalDate> startDateRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<java.time.LocalDate> endDateRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<java.util.Set<com.pbl3.project.pbl3_project.entity.StocktakeSessionStatus>> statusFiltersRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<java.util.Set<com.pbl3.project.pbl3_project.entity.StocktakeScopeType>> scopeFiltersRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());

        Label rowCountLabel = createStatusMetaLabel("Showing 0-0 of 0 Row(s)");
        Label pageLabel = createStatusMetaLabel("Page 0 / 0");
        Button prevBtn = createPageNavButton("Prev");
        Button nextBtn = createPageNavButton("Next");

        Runnable updateStatusBar = () -> updatePagedStatus(
            table,
            rowCountLabel,
            pageLabel,
            prevBtn,
            nextBtn,
            totalElements[0],
            currentPage[0],
            totalPages[0],
            pageSize
        );
        Runnable loadPage = () -> {
            org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.StocktakeSession> pageData =
                stocktakeService.searchSessions(
                    user,
                    searchRef.get(),
                    startDateRef.get(),
                    endDateRef.get(),
                    statusFiltersRef.get(),
                    scopeFiltersRef.get(),
                    createPageable(stocktakeSortState, stocktakeSortProperties, currentPage[0], pageSize)
                );
            if (pageData.getTotalPages() > 0 && currentPage[0] >= pageData.getTotalPages()) {
                currentPage[0] = pageData.getTotalPages() - 1;
                pageData = stocktakeService.searchSessions(
                    user,
                    searchRef.get(),
                    startDateRef.get(),
                    endDateRef.get(),
                    statusFiltersRef.get(),
                    scopeFiltersRef.get(),
                    createPageable(stocktakeSortState, stocktakeSortProperties, currentPage[0], pageSize)
                );
            }
            totalElements[0] = pageData.getTotalElements();
            totalPages[0] = pageData.getTotalPages();
            table.setItems(javafx.collections.FXCollections.observableArrayList(pageData.getContent()));
            updateStatusBar.run();
        };

        Label stocktakeSortStatusLabel = createSortStatusLabel(stocktakeSortState, stocktakeSortLabels);
        Runnable applyStocktakeSortUi = () -> {
            applySortStateToTable(table, stocktakeSortColumns, stocktakeSortState);
            stocktakeSortStatusLabel.setText(buildSortStatusText(stocktakeSortState, stocktakeSortLabels));
        };
        applyStocktakeSortUi.run();
        installManualServerSorting(
            table,
            stocktakeSortColumns,
            stocktakeSortState,
            () -> {
                applyStocktakeSortUi.run();
                currentPage[0] = 0;
                loadPage.run();
            }
        );

        prevBtn.setOnAction(e -> {
            if (currentPage[0] > 0) {
                currentPage[0]--;
                loadPage.run();
            }
        });
        nextBtn.setOnAction(e -> {
            if (currentPage[0] + 1 < totalPages[0]) {
                currentPage[0]++;
                loadPage.run();
            }
        });

        javafx.animation.PauseTransition searchPause = new javafx.animation.PauseTransition(Duration.millis(220));
        searchPause.setOnFinished(e -> {
            currentPage[0] = 0;
            searchRef.set(searchField.getText());
            loadPage.run();
        });
        searchField.textProperty().addListener((obs, oldV, newV) -> searchPause.playFromStart());

        javafx.scene.layout.HBox statusBar = new javafx.scene.layout.HBox(15, stocktakeSortStatusLabel, rowCountLabel, pageLabel, prevBtn, nextBtn);
        applyStandardTableStatusBar(statusBar);

        Runnable openSelectedSession = () -> {
            com.pbl3.project.pbl3_project.entity.StocktakeSession session = table.getSelectionModel().getSelectedItem();
            if (session == null) {
                toastService.showWarning("Select a stocktake session");
                return;
            }
            showStocktakeSessionDialog(stage, user, session.getId(), loadPage);
        };

        newSessionButton.setOnAction(e -> showCreateStocktakeDialog(stage, user, created -> {
            loadPage.run();
            showStocktakeSessionDialog(stage, user, created.getId(), loadPage);
        }));
        openButton.setOnAction(e -> openSelectedSession.run());
        applyButton.setOnAction(e -> {
            com.pbl3.project.pbl3_project.entity.StocktakeSession session = table.getSelectionModel().getSelectedItem();
            if (session == null) {
                toastService.showWarning("Select a stocktake session");
                return;
            }
            if (session.getStatus() != com.pbl3.project.pbl3_project.entity.StocktakeSessionStatus.OPEN) {
                toastService.showWarning("Only open stocktake sessions can be applied");
                return;
            }
            if (!showConfirmDialog("Apply Stocktake", "Apply stocktake #" + session.getId() + "?")) {
                return;
            }
            try {
                stocktakeService.applySession(user, session.getId());
                toastService.showSuccess("Stocktake applied");
                loadPage.run();
            } catch (Exception ex) {
                showUserFacingError(ex);
            }
        });
        cancelButton.setOnAction(e -> {
            com.pbl3.project.pbl3_project.entity.StocktakeSession session = table.getSelectionModel().getSelectedItem();
            if (session == null) {
                toastService.showWarning("Select a stocktake session");
                return;
            }
            if (session.getStatus() != com.pbl3.project.pbl3_project.entity.StocktakeSessionStatus.OPEN) {
                toastService.showWarning("Only open stocktake sessions can be canceled");
                return;
            }
            javafx.scene.control.TextInputDialog notesDialog = new javafx.scene.control.TextInputDialog();
            notesDialog.setTitle("Cancel Stocktake");
            notesDialog.setHeaderText("Cancel stocktake #" + session.getId());
            notesDialog.setContentText("Notes:");
            if (stage.getScene() != null) {
                notesDialog.getDialogPane().getStylesheets().addAll(stage.getScene().getStylesheets());
            }
            java.util.Optional<String> result = notesDialog.showAndWait();
            if (result.isEmpty()) {
                return;
            }
            try {
                stocktakeService.cancelSession(user, session.getId(), result.get());
                toastService.showSuccess("Stocktake canceled");
                loadPage.run();
            } catch (Exception ex) {
                showUserFacingError(ex);
            }
        });

        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<com.pbl3.project.pbl3_project.entity.StocktakeSession> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showStocktakeSessionDialog(stage, user, row.getItem().getId(), loadPage);
                }
            });
            return row;
        });

        javafx.stage.Popup filterPopup = new javafx.stage.Popup();
        filterPopup.setAutoHide(true);
        filterBox.setOnMouseClicked(event -> {
            if (filterPopup.isShowing()) {
                filterPopup.hide();
                return;
            }

            FilterPopupShell filterShell = createFilterPopupShell(340, 240);
            VBox popupContainer = filterShell.container();
            VBox scrollContent = filterShell.content();

            Label dateTitle = createFilterPopupSectionTitle("Date Range");
            javafx.scene.control.DatePicker startDatePicker = new javafx.scene.control.DatePicker(startDateRef.get());
            startDatePicker.setPromptText("Start Date");
            javafx.scene.control.DatePicker endDatePicker = new javafx.scene.control.DatePicker(endDateRef.get());
            endDatePicker.setPromptText("End Date");
            customizeDatePicker(startDatePicker);
            customizeDatePicker(endDatePicker);
            javafx.scene.layout.HBox dateBox = new javafx.scene.layout.HBox(5, startDatePicker, new Label("-"), endDatePicker);
            dateBox.setAlignment(Pos.CENTER_LEFT);

            Label statusTitle = createFilterPopupSectionTitle("Status");
            javafx.scene.control.ComboBox<String> statusCombo = new javafx.scene.control.ComboBox<>();
            java.util.Map<String, com.pbl3.project.pbl3_project.entity.StocktakeSessionStatus> statusLookup = new java.util.LinkedHashMap<>();
            statusCombo.getItems().add("All Statuses");
            for (com.pbl3.project.pbl3_project.entity.StocktakeSessionStatus status : com.pbl3.project.pbl3_project.entity.StocktakeSessionStatus.values()) {
                String label = formatStocktakeStatusLabel(status);
                statusLookup.put(label, status);
                statusCombo.getItems().add(label);
            }
            statusCombo.setValue(statusFiltersRef.get().isEmpty() ? "All Statuses" : formatStocktakeStatusLabel(statusFiltersRef.get().iterator().next()));
            statusCombo.setMaxWidth(Double.MAX_VALUE);

            Label scopeTitle = createFilterPopupSectionTitle("Scope");
            javafx.scene.control.ComboBox<String> scopeCombo = new javafx.scene.control.ComboBox<>();
            java.util.Map<String, com.pbl3.project.pbl3_project.entity.StocktakeScopeType> scopeLookup = new java.util.LinkedHashMap<>();
            scopeCombo.getItems().add("All Scopes");
            for (com.pbl3.project.pbl3_project.entity.StocktakeScopeType scope : com.pbl3.project.pbl3_project.entity.StocktakeScopeType.values()) {
                String label = formatStocktakeScopeLabel(scope);
                scopeLookup.put(label, scope);
                scopeCombo.getItems().add(label);
            }
            scopeCombo.setValue(scopeFiltersRef.get().isEmpty() ? "All Scopes" : formatStocktakeScopeLabel(scopeFiltersRef.get().iterator().next()));
            scopeCombo.setMaxWidth(Double.MAX_VALUE);

            Button resetBtn = new Button("Reset");
            resetBtn.getStyleClass().add("filter-reset-btn");
            resetBtn.setOnAction(e -> {
                filterBox.setStyle("");
                startDateRef.set(null);
                endDateRef.set(null);
                statusFiltersRef.set(new java.util.LinkedHashSet<>());
                scopeFiltersRef.set(new java.util.LinkedHashSet<>());
                currentPage[0] = 0;
                loadPage.run();
                filterPopup.hide();
            });

            Button applyBtn = new Button("Apply Filter");
            applyBtn.getStyleClass().add("filter-apply-btn");
            applyBtn.setOnAction(e -> {
                startDateRef.set(startDatePicker.getValue());
                endDateRef.set(endDatePicker.getValue());

                java.util.Set<com.pbl3.project.pbl3_project.entity.StocktakeSessionStatus> statusFilters = new java.util.LinkedHashSet<>();
                if (!"All Statuses".equals(statusCombo.getValue())) {
                    statusFilters.add(statusLookup.get(statusCombo.getValue()));
                }
                statusFiltersRef.set(statusFilters);

                java.util.Set<com.pbl3.project.pbl3_project.entity.StocktakeScopeType> scopeFilters = new java.util.LinkedHashSet<>();
                if (!"All Scopes".equals(scopeCombo.getValue())) {
                    scopeFilters.add(scopeLookup.get(scopeCombo.getValue()));
                }
                scopeFiltersRef.set(scopeFilters);

                currentPage[0] = 0;
                loadPage.run();
                boolean hasFilter = startDateRef.get() != null
                    || endDateRef.get() != null
                    || !statusFiltersRef.get().isEmpty()
                    || !scopeFiltersRef.get().isEmpty();
                filterBox.setStyle(hasFilter ? "-fx-border-color: -app-primary;" : "");
                filterPopup.hide();
            });

            javafx.scene.layout.HBox buttonRow = createFilterPopupActionRow(resetBtn, applyBtn);

            scrollContent.getChildren().addAll(
                dateTitle, dateBox,
                new javafx.scene.control.Separator(),
                statusTitle, statusCombo,
                scopeTitle, scopeCombo,
                new javafx.scene.control.Separator()
            );
            popupContainer.getChildren().add(buttonRow);
            filterPopup.getContent().clear();
            filterPopup.getContent().add(popupContainer);

            showPopupBelow(filterPopup, filterBox, -260, 5);
        });

        root.getChildren().addAll(topBar, table, statusBar);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        loadPage.run();
        enableDeselectOnOutsideClick(root, table);
        return root;
    }

    private void showCreateStocktakeDialog(
        Stage owner,
        com.pbl3.project.pbl3_project.entity.User user,
        java.util.function.Consumer<com.pbl3.project.pbl3_project.entity.StocktakeSession> onSuccess
    ) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
        dialog.setTitle("New Stocktake Session");

        VBox root = new VBox(16);
        root.getStyleClass().add("dialog-root");
        root.setPadding(new Insets(20));
        root.setPrefWidth(520);

        Label titleLabel = new Label("New Stocktake Session");
        titleLabel.getStyleClass().add("dialog-title");

        javafx.scene.control.ComboBox<com.pbl3.project.pbl3_project.entity.StocktakeScopeType> scopeCombo = new javafx.scene.control.ComboBox<>();
        scopeCombo.getItems().addAll(com.pbl3.project.pbl3_project.entity.StocktakeScopeType.values());
        scopeCombo.setValue(com.pbl3.project.pbl3_project.entity.StocktakeScopeType.ALL_PRODUCTS);
        scopeCombo.setMaxWidth(Double.MAX_VALUE);

        javafx.scene.control.ComboBox<com.pbl3.project.pbl3_project.entity.Category> categoryCombo = new javafx.scene.control.ComboBox<>();
        categoryCombo.getItems().addAll(categoryService.getAllCategories());
        categoryCombo.setDisable(true);
        categoryCombo.setMaxWidth(Double.MAX_VALUE);
        setComboConverter(categoryCombo);

        TextField notesField = createStyledTextField("", "Notes");

        scopeCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(com.pbl3.project.pbl3_project.entity.StocktakeScopeType value) {
                return formatStocktakeScopeLabel(value);
            }
            @Override public com.pbl3.project.pbl3_project.entity.StocktakeScopeType fromString(String string) {
                return null;
            }
        });

        scopeCombo.setOnAction(e -> {
            boolean categoryScope = scopeCombo.getValue() == com.pbl3.project.pbl3_project.entity.StocktakeScopeType.CATEGORY;
            categoryCombo.setDisable(!categoryScope);
            if (!categoryScope) {
                categoryCombo.setValue(null);
            }
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("filter-reset-btn");
        cancelBtn.setOnAction(e -> dialog.close());

        java.util.concurrent.atomic.AtomicReference<com.pbl3.project.pbl3_project.entity.StocktakeSession> createdSessionRef =
            new java.util.concurrent.atomic.AtomicReference<>();

        Button createBtn = createExpandableGreenActionButton("Create Session", 180);
        createBtn.setOnAction(e -> {
            try {
                com.pbl3.project.pbl3_project.entity.StocktakeSession created = stocktakeService.createSession(
                    user,
                    scopeCombo.getValue(),
                    categoryCombo.getValue(),
                    notesField.getText()
                );
                createdSessionRef.set(created);
                toastService.showSuccess("Stocktake session created");
                dialog.close();
            } catch (Exception ex) {
                showUserFacingError(ex);
            }
        });

        javafx.scene.layout.HBox actionRow = new javafx.scene.layout.HBox(10, cancelBtn, createBtn);
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(
            titleLabel,
            createFormLabel("Scope"), scopeCombo,
            createFormLabel("Category"), categoryCombo,
            createFormLabel("Notes"), notesField,
            actionRow
        );

        Scene scene = new Scene(root);
        if (owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        dialog.setScene(scene);
        dialog.showAndWait();

        if (createdSessionRef.get() != null && onSuccess != null) {
            javafx.application.Platform.runLater(() -> onSuccess.accept(createdSessionRef.get()));
        }
    }

    private void showStocktakeSessionDialog(
        Stage owner,
        com.pbl3.project.pbl3_project.entity.User user,
        Long sessionId,
        Runnable onChanged
    ) {
        try {
            com.pbl3.project.pbl3_project.entity.StocktakeSession session = stocktakeService.getSessionWithItems(user, sessionId);
            Stage dialog = new Stage();
            dialog.initOwner(owner);
            dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialog.setTitle("Stocktake #" + session.getId());

            VBox root = new VBox(14);
            root.getStyleClass().add("dialog-root");
            root.setPadding(new Insets(18));
            root.setPrefWidth(920);
            root.setPrefHeight(680);

            Label titleLabel = new Label("Stocktake #" + session.getId());
            titleLabel.getStyleClass().add("dialog-title");

            Label metaLabel = new Label(
                formatStocktakeScopeLabel(session.getScopeType())
                    + " • "
                    + formatStocktakeStatusLabel(session.getStatus())
                    + " • "
                    + formatDateTime(session.getCreatedAt())
            );
            metaLabel.setStyle("-fx-text-fill: -app-text-secondary; -fx-font-size: 13px;");

            boolean editable = session.getStatus() == com.pbl3.project.pbl3_project.entity.StocktakeSessionStatus.OPEN;
            TextField notesField = createStyledTextField(
                session.getNotes() == null ? "" : session.getNotes(),
                "Session notes"
            );
            notesField.setDisable(!editable);

            class StocktakeDraftRow {
                private final Long itemId;
                private final String productName;
                private final int systemQuantity;
                private final BigDecimal unitCost;
                private int countedQuantity;
                private String notes;

                private StocktakeDraftRow(com.pbl3.project.pbl3_project.entity.StocktakeItem item) {
                    this.itemId = item.getId();
                    this.productName = item.getProduct() != null ? item.getProduct().getName() : "Unknown";
                    this.systemQuantity = item.getSystemQuantity() != null ? item.getSystemQuantity() : 0;
                    this.unitCost = item.getUnitCostSnapshot();
                    this.countedQuantity = item.getCountedQuantity() != null ? item.getCountedQuantity() : 0;
                    this.notes = item.getNotes();
                }

                public Long getItemId() { return itemId; }
                public String getProductName() { return productName; }
                public int getSystemQuantity() { return systemQuantity; }
                public int getCountedQuantity() { return countedQuantity; }
                public void setCountedQuantity(int countedQuantity) { this.countedQuantity = countedQuantity; }
                public int getVariance() { return countedQuantity - systemQuantity; }
                public String getNotes() { return notes; }
                public void setNotes(String notes) { this.notes = notes; }
                public BigDecimal getUnitCost() { return unitCost; }
            }

            javafx.collections.ObservableList<StocktakeDraftRow> rows = javafx.collections.FXCollections.observableArrayList(
                session.getItems().stream().map(StocktakeDraftRow::new).toList()
            );

            javafx.scene.control.TableView<StocktakeDraftRow> table = new javafx.scene.control.TableView<>(rows);
            prepareNonReorderableTable(table);
            table.setEditable(editable);
            table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
            table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.SINGLE);
            table.getStyleClass().add("stocktake-session-table");

            javafx.scene.control.TableColumn<StocktakeDraftRow, String> productCol = new javafx.scene.control.TableColumn<>("Product");
            productCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getProductName()));

            javafx.scene.control.TableColumn<StocktakeDraftRow, Number> systemCol = new javafx.scene.control.TableColumn<>("System Qty");
            systemCol.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getSystemQuantity()));
            systemCol.setStyle("-fx-alignment: CENTER;");

            javafx.scene.control.TableColumn<StocktakeDraftRow, Integer> countedCol = new javafx.scene.control.TableColumn<>("Counted Qty");
            countedCol.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getCountedQuantity()));
            countedCol.setStyle("-fx-alignment: CENTER;");
            countedCol.setEditable(editable);
            countedCol.setCellFactory(column -> {
                javafx.scene.control.cell.TextFieldTableCell<StocktakeDraftRow, Integer> cell =
                    new javafx.scene.control.cell.TextFieldTableCell<>(new javafx.util.converter.IntegerStringConverter()) {
                        @Override
                        public void updateItem(Integer item, boolean empty) {
                            super.updateItem(item, empty);
                            getStyleClass().removeAll("stocktake-editable-cell", "stocktake-placeholder-cell", "stocktake-note-cell");
                            setCursor(!empty && editable ? javafx.scene.Cursor.TEXT : javafx.scene.Cursor.DEFAULT);
                            if (!empty && editable && !isEditing()) {
                                getStyleClass().add("stocktake-editable-cell");
                            }
                        }
                    };
                cell.setAlignment(Pos.CENTER);
                enableSingleClickEditing(cell, editable);
                return cell;
            });
            countedCol.setOnEditCommit(event -> {
                int value = event.getNewValue() == null ? 0 : event.getNewValue().intValue();
                if (value < 0) {
                    showUserFacingError(new ValidationException("Counted quantity cannot be negative"));
                    table.refresh();
                    return;
                }
                event.getRowValue().setCountedQuantity(value);
                table.refresh();
            });

            javafx.scene.control.TableColumn<StocktakeDraftRow, Number> varianceCol = new javafx.scene.control.TableColumn<>("Variance");
            varianceCol.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getVariance()));
            varianceCol.setStyle("-fx-alignment: CENTER;");

            javafx.scene.control.TableColumn<StocktakeDraftRow, String> unitCostCol = new javafx.scene.control.TableColumn<>("Unit Cost");
            unitCostCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatVnd(data.getValue().getUnitCost())));

            javafx.scene.control.TableColumn<StocktakeDraftRow, String> notesCol = new javafx.scene.control.TableColumn<>("Item Notes");
            notesCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getNotes() == null ? "" : data.getValue().getNotes()));
            notesCol.setCellFactory(column -> {
                javafx.scene.control.cell.TextFieldTableCell<StocktakeDraftRow, String> cell =
                    new javafx.scene.control.cell.TextFieldTableCell<>(new javafx.util.converter.DefaultStringConverter()) {
                        @Override
                        public void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            getStyleClass().removeAll("stocktake-editable-cell", "stocktake-placeholder-cell", "stocktake-note-cell");
                            setCursor(!empty && editable ? javafx.scene.Cursor.TEXT : javafx.scene.Cursor.DEFAULT);
                            if (empty) {
                                return;
                            }
                            if (editable && !isEditing()) {
                                getStyleClass().add("stocktake-note-cell");
                                if (item == null || item.isBlank()) {
                                    setText("Click to add note");
                                    getStyleClass().add("stocktake-placeholder-cell");
                                }
                            } else if (!isEditing() && (item == null || item.isBlank())) {
                                setText("-");
                                getStyleClass().add("stocktake-placeholder-cell");
                            }
                        }
                    };
                cell.setAlignment(Pos.CENTER_LEFT);
                enableSingleClickEditing(cell, editable);
                return cell;
            });
            notesCol.setOnEditCommit(event -> {
                event.getRowValue().setNotes(event.getNewValue());
                table.refresh();
            });
            notesCol.setEditable(editable);

            table.getColumns().addAll(productCol, systemCol, countedCol, varianceCol, unitCostCol, notesCol);
            VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
            if (editable) {
                notesField.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event ->
                    table.getSelectionModel().clearSelection()
                );
                notesField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                    if (isFocused) {
                        table.getSelectionModel().clearSelection();
                    }
                });
            }

            java.util.function.Supplier<java.util.List<com.pbl3.project.pbl3_project.service.StocktakeService.StocktakeItemUpdate>> collectUpdates = () ->
                rows.stream()
                    .map(row -> new com.pbl3.project.pbl3_project.service.StocktakeService.StocktakeItemUpdate(
                        row.getItemId(),
                        row.getCountedQuantity(),
                        row.getNotes()
                    ))
                    .toList();

            Button closeBtn = new Button("Close");
            closeBtn.getStyleClass().add("close-button");
            closeBtn.setOnAction(e -> dialog.close());

            Button saveBtn = new Button("Save Draft");
            saveBtn.getStyleClass().add("primary-button");
            saveBtn.setDisable(!editable);
            saveBtn.setOnAction(e -> {
                try {
                    stocktakeService.updateSessionItems(user, sessionId, notesField.getText(), collectUpdates.get());
                    toastService.showSuccess("Stocktake draft saved");
                    if (onChanged != null) {
                        onChanged.run();
                    }
                } catch (Exception ex) {
                    showUserFacingError(ex);
                }
            });

            Button applyBtn = new Button("Apply Session");
            applyBtn.getStyleClass().add("success-button");
            applyBtn.setDisable(!editable);
            applyBtn.setOnAction(e -> {
                try {
                    stocktakeService.updateSessionItems(user, sessionId, notesField.getText(), collectUpdates.get());
                    stocktakeService.applySession(user, sessionId);
                    toastService.showSuccess("Stocktake applied");
                    dialog.close();
                    if (onChanged != null) {
                        onChanged.run();
                    }
                } catch (Exception ex) {
                    showUserFacingError(ex);
                }
            });

            Button cancelBtn = new Button("Cancel Session");
            cancelBtn.getStyleClass().add("danger-button");
            cancelBtn.setDisable(!editable);
            cancelBtn.setOnAction(e -> {
                try {
                    stocktakeService.cancelSession(user, sessionId, notesField.getText());
                    toastService.showSuccess("Stocktake canceled");
                    dialog.close();
                    if (onChanged != null) {
                        onChanged.run();
                    }
                } catch (Exception ex) {
                    showUserFacingError(ex);
                }
            });

            javafx.scene.layout.HBox actionRow = new javafx.scene.layout.HBox(10, closeBtn, saveBtn, applyBtn, cancelBtn);
            actionRow.setAlignment(Pos.CENTER_RIGHT);

            root.getChildren().addAll(titleLabel, metaLabel, createFormLabel("Session Notes"), notesField, table, actionRow);
            Scene scene = new Scene(root);
            if (owner.getScene() != null) {
                scene.getStylesheets().addAll(owner.getScene().getStylesheets());
            }
            dialog.setScene(scene);
            enableDeselectOnOutsideClick(root, table);
            dialog.showAndWait();
        } catch (Exception ex) {
            showUserFacingError(ex);
        }
    }

    private VBox createAccountAuditView(com.pbl3.project.pbl3_project.entity.User user) {
        VBox root = new VBox();
        applyStandardTablePageLayout(root, Insets.EMPTY);
        final String accountAuditSortStateKey = "account-audit";
        TableSortState accountAuditSortState = getOrCreateTableSortState(
            accountAuditSortStateKey,
            new SortCriterion("createdAt", javafx.scene.control.TableColumn.SortType.DESCENDING)
        );
        java.util.LinkedHashMap<String, String> accountAuditSortProperties = new java.util.LinkedHashMap<>();
        accountAuditSortProperties.put("createdAt", "createdAt");
        accountAuditSortProperties.put("actorUsername", "actor.username");
        accountAuditSortProperties.put("targetUsername", "targetUser.username");
        accountAuditSortProperties.put("action", "action");
        java.util.LinkedHashMap<String, String> accountAuditSortLabels = new java.util.LinkedHashMap<>();
        accountAuditSortLabels.put("createdAt", "Date");
        accountAuditSortLabels.put("actorUsername", "Actor");
        accountAuditSortLabels.put("targetUsername", "Target User");
        accountAuditSortLabels.put("action", "Action");

        Label header = new Label("Account Audit");
        header.getStyleClass().add("header-label");

        ExpandableSearchControl searchControl = createExpandableSearchControl(320);
        TextField searchField = searchControl.field();

        javafx.scene.layout.HBox filterBox = new javafx.scene.layout.HBox();
        filterBox.setAlignment(Pos.CENTER);
        filterBox.getStyleClass().add("expandable-search-box");
        filterBox.setPrefSize(40, 40);
        filterBox.setMinSize(40, 40);
        filterBox.setMaxSize(40, 40);
        filterBox.setCursor(javafx.scene.Cursor.HAND);
        javafx.scene.shape.SVGPath filterIcon = new javafx.scene.shape.SVGPath();
        filterIcon.setContent("M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z");
        filterIcon.setFill(PRIMARY_COLOR);
        filterBox.getChildren().add(filterIcon);

        javafx.scene.layout.HBox rightBox = new javafx.scene.layout.HBox(12, filterBox, searchControl.box());
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        javafx.scene.layout.BorderPane topBar = new javafx.scene.layout.BorderPane();
        topBar.setLeft(header);
        topBar.setRight(rightBox);
        javafx.scene.layout.BorderPane.setAlignment(header, Pos.CENTER_LEFT);

        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.AccountAuditLog> table = new javafx.scene.control.TableView<>();
        applyStandardTableSizing(table);
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        enableDragSelection(table);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.AccountAuditLog, String> dateCol = new javafx.scene.control.TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatDateTime(data.getValue().getCreatedAt())));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.AccountAuditLog, String> actorCol = new javafx.scene.control.TableColumn<>("Actor");
        actorCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getActor() != null ? data.getValue().getActor().getUsername() : "System"
        ));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.AccountAuditLog, String> targetCol = new javafx.scene.control.TableColumn<>("Target User");
        targetCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getTargetUser() != null ? data.getValue().getTargetUser().getUsername() : "-"
        ));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.AccountAuditLog, String> actionCol = new javafx.scene.control.TableColumn<>("Action");
        actionCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatAccountAuditActionLabel(data.getValue().getAction())));
        actionCol.setStyle("-fx-alignment: CENTER;");

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.AccountAuditLog, String> detailsCol = new javafx.scene.control.TableColumn<>("Details");
        detailsCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getDetails() == null ? "" : data.getValue().getDetails()
        ));

        table.getColumns().addAll(dateCol, actorCol, targetCol, actionCol, detailsCol);
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        java.util.LinkedHashMap<String, javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.AccountAuditLog, ?>> accountAuditSortColumns =
            new java.util.LinkedHashMap<>();
        accountAuditSortColumns.put("createdAt", dateCol);
        accountAuditSortColumns.put("actorUsername", actorCol);
        accountAuditSortColumns.put("targetUsername", targetCol);
        accountAuditSortColumns.put("action", actionCol);
        installSortHeaderIndicators(accountAuditSortColumns);

        final int pageSize = 25;
        final int[] currentPage = {0};
        final int[] totalPages = {0};
        final long[] totalElements = {0L};
        java.util.concurrent.atomic.AtomicReference<String> searchRef = new java.util.concurrent.atomic.AtomicReference<>("");
        java.util.concurrent.atomic.AtomicReference<java.time.LocalDate> startDateRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<java.time.LocalDate> endDateRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<java.util.Set<String>> actorUsernamesRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<java.util.Set<String>> targetUsernamesRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<java.util.Set<com.pbl3.project.pbl3_project.entity.AccountAuditAction>> actionsRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());

        Label rowCountLabel = createStatusMetaLabel("Showing 0-0 of 0 Row(s)");
        Label pageLabel = createStatusMetaLabel("Page 0 / 0");
        Button prevBtn = createPageNavButton("Prev");
        Button nextBtn = createPageNavButton("Next");

        Runnable updateStatusBar = () -> updatePagedStatus(
            table,
            rowCountLabel,
            pageLabel,
            prevBtn,
            nextBtn,
            totalElements[0],
            currentPage[0],
            totalPages[0],
            pageSize
        );
        Runnable loadPage = () -> {
            org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.AccountAuditLog> pageData =
                accountAuditLogService.searchAccountAuditLogs(
                    user,
                    searchRef.get(),
                    startDateRef.get(),
                    endDateRef.get(),
                    actorUsernamesRef.get(),
                    targetUsernamesRef.get(),
                    actionsRef.get(),
                    createPageable(accountAuditSortState, accountAuditSortProperties, currentPage[0], pageSize)
                );
            if (pageData.getTotalPages() > 0 && currentPage[0] >= pageData.getTotalPages()) {
                currentPage[0] = pageData.getTotalPages() - 1;
                pageData = accountAuditLogService.searchAccountAuditLogs(
                    user,
                    searchRef.get(),
                    startDateRef.get(),
                    endDateRef.get(),
                    actorUsernamesRef.get(),
                    targetUsernamesRef.get(),
                    actionsRef.get(),
                    createPageable(accountAuditSortState, accountAuditSortProperties, currentPage[0], pageSize)
                );
            }
            totalElements[0] = pageData.getTotalElements();
            totalPages[0] = pageData.getTotalPages();
            table.setItems(javafx.collections.FXCollections.observableArrayList(pageData.getContent()));
            updateStatusBar.run();
        };
        Label accountAuditSortStatusLabel = createSortStatusLabel(accountAuditSortState, accountAuditSortLabels);
        Runnable applyAccountAuditSortUi = () -> {
            applySortStateToTable(table, accountAuditSortColumns, accountAuditSortState);
            accountAuditSortStatusLabel.setText(buildSortStatusText(accountAuditSortState, accountAuditSortLabels));
        };
        applyAccountAuditSortUi.run();
        installManualServerSorting(
            table,
            accountAuditSortColumns,
            accountAuditSortState,
            () -> {
                applyAccountAuditSortUi.run();
                currentPage[0] = 0;
                loadPage.run();
            }
        );

        prevBtn.setOnAction(e -> {
            if (currentPage[0] > 0) {
                currentPage[0]--;
                loadPage.run();
            }
        });
        nextBtn.setOnAction(e -> {
            if (currentPage[0] + 1 < totalPages[0]) {
                currentPage[0]++;
                loadPage.run();
            }
        });

        javafx.animation.PauseTransition searchPause = new javafx.animation.PauseTransition(Duration.millis(220));
        searchPause.setOnFinished(e -> {
            currentPage[0] = 0;
            searchRef.set(searchField.getText());
            loadPage.run();
        });
        searchField.textProperty().addListener((obs, oldV, newV) -> searchPause.playFromStart());

        javafx.scene.layout.HBox statusBar = new javafx.scene.layout.HBox(15, accountAuditSortStatusLabel, rowCountLabel, pageLabel, prevBtn, nextBtn);
        applyStandardTableStatusBar(statusBar);

        javafx.stage.Popup filterPopup = new javafx.stage.Popup();
        filterPopup.setAutoHide(true);

        filterBox.setOnMouseClicked(event -> {
            if (filterPopup.isShowing()) {
                filterPopup.hide();
                return;
            }

            FilterPopupShell filterShell = createFilterPopupShell(360, 250);
            VBox popupContainer = filterShell.container();
            VBox scrollContent = filterShell.content();

            Label dateTitle = createFilterPopupSectionTitle("Date Range");
            javafx.scene.control.DatePicker startDatePicker = new javafx.scene.control.DatePicker(startDateRef.get());
            startDatePicker.setPromptText("Start Date");
            startDatePicker.setPrefWidth(140);
            startDatePicker.setStyle("-fx-font-size: 13px;");
            javafx.scene.control.DatePicker endDatePicker = new javafx.scene.control.DatePicker(endDateRef.get());
            endDatePicker.setPromptText("End Date");
            endDatePicker.setPrefWidth(140);
            endDatePicker.setStyle("-fx-font-size: 13px;");
            customizeDatePicker(startDatePicker);
            customizeDatePicker(endDatePicker);
            javafx.scene.layout.HBox dateBox = new javafx.scene.layout.HBox(5, startDatePicker, new Label("-"), endDatePicker);
            dateBox.setAlignment(Pos.CENTER_LEFT);

            Label actorTitle = createFilterPopupSectionTitle("Actor");
            javafx.scene.control.ComboBox<String> actorCombo = new javafx.scene.control.ComboBox<>();
            actorCombo.getItems().add("All Actors");
            actorCombo.getItems().addAll(accountAuditLogService.getActorUsernames(user));
            actorCombo.setValue(actorUsernamesRef.get().isEmpty() ? "All Actors" : actorUsernamesRef.get().iterator().next());
            actorCombo.setMaxWidth(Double.MAX_VALUE);

            Label targetTitle = createFilterPopupSectionTitle("Target User");
            javafx.scene.control.ComboBox<String> targetCombo = new javafx.scene.control.ComboBox<>();
            targetCombo.getItems().add("All Targets");
            targetCombo.getItems().addAll(accountAuditLogService.getTargetUsernames(user));
            targetCombo.setValue(targetUsernamesRef.get().isEmpty() ? "All Targets" : targetUsernamesRef.get().iterator().next());
            targetCombo.setMaxWidth(Double.MAX_VALUE);

            Label actionTitle = createFilterPopupSectionTitle("Action");
            javafx.scene.control.ComboBox<String> actionCombo = new javafx.scene.control.ComboBox<>();
            java.util.Map<String, com.pbl3.project.pbl3_project.entity.AccountAuditAction> actionLookup = new java.util.LinkedHashMap<>();
            actionCombo.getItems().add("All Actions");
            for (com.pbl3.project.pbl3_project.entity.AccountAuditAction action : com.pbl3.project.pbl3_project.entity.AccountAuditAction.values()) {
                String label = formatAccountAuditActionLabel(action);
                actionLookup.put(label, action);
                actionCombo.getItems().add(label);
            }
            actionCombo.setValue(actionsRef.get().isEmpty()
                ? "All Actions"
                : formatAccountAuditActionLabel(actionsRef.get().iterator().next()));
            actionCombo.setMaxWidth(Double.MAX_VALUE);

            Button resetBtn = new Button("Reset");
            resetBtn.getStyleClass().add("filter-reset-btn");
            resetBtn.setOnAction(ae -> {
                filterBox.setStyle("");
                startDateRef.set(null);
                endDateRef.set(null);
                actorUsernamesRef.set(new java.util.LinkedHashSet<>());
                targetUsernamesRef.set(new java.util.LinkedHashSet<>());
                actionsRef.set(new java.util.LinkedHashSet<>());
                currentPage[0] = 0;
                loadPage.run();
                filterPopup.hide();
            });

            Button applyBtn = new Button("Apply Filter");
            applyBtn.getStyleClass().add("filter-apply-btn");
            applyBtn.setOnAction(ae -> {
                java.time.LocalDate startDate = startDatePicker.getValue();
                java.time.LocalDate endDate = endDatePicker.getValue();
                if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
                    toastService.showWarning("End date must be on or after start date");
                    return;
                }

                java.util.LinkedHashSet<String> selectedActors = new java.util.LinkedHashSet<>();
                java.util.LinkedHashSet<String> selectedTargets = new java.util.LinkedHashSet<>();
                java.util.LinkedHashSet<com.pbl3.project.pbl3_project.entity.AccountAuditAction> selectedActions = new java.util.LinkedHashSet<>();

                if (actorCombo.getValue() != null && !"All Actors".equals(actorCombo.getValue())) {
                    selectedActors.add(actorCombo.getValue());
                }
                if (targetCombo.getValue() != null && !"All Targets".equals(targetCombo.getValue())) {
                    selectedTargets.add(targetCombo.getValue());
                }
                if (actionCombo.getValue() != null && !"All Actions".equals(actionCombo.getValue())) {
                    com.pbl3.project.pbl3_project.entity.AccountAuditAction selectedAction = actionLookup.get(actionCombo.getValue());
                    if (selectedAction != null) {
                        selectedActions.add(selectedAction);
                    }
                }

                startDateRef.set(startDate);
                endDateRef.set(endDate);
                actorUsernamesRef.set(selectedActors);
                targetUsernamesRef.set(selectedTargets);
                actionsRef.set(selectedActions);
                currentPage[0] = 0;
                loadPage.run();

                boolean hasFilter = startDate != null
                    || endDate != null
                    || !selectedActors.isEmpty()
                    || !selectedTargets.isEmpty()
                    || !selectedActions.isEmpty();
                filterBox.setStyle(hasFilter ? "-fx-border-color: -app-primary;" : "");
                filterPopup.hide();
            });

            javafx.scene.layout.HBox buttonRow = createFilterPopupActionRow(resetBtn, applyBtn);
            scrollContent.getChildren().addAll(
                dateTitle, dateBox,
                new javafx.scene.control.Separator(),
                actorTitle, actorCombo,
                targetTitle, targetCombo,
                actionTitle, actionCombo,
                new javafx.scene.control.Separator()
            );
            popupContainer.getChildren().add(buttonRow);

            filterPopup.getContent().clear();
            filterPopup.getContent().add(popupContainer);
            showPopupBelow(filterPopup, filterBox, -280, 5);
        });

        root.getChildren().addAll(topBar, table, statusBar);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);

        table.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<com.pbl3.project.pbl3_project.entity.AccountAuditLog>) c -> updateStatusBar.run());
        loadPage.run();
        enableDeselectOnOutsideClick(root, table);
        return root;
    }

    private String formatAccountAuditActionLabel(com.pbl3.project.pbl3_project.entity.AccountAuditAction action) {
        return formatEnumWords(action == null ? null : action.name());
    }

    private String formatOperationalAuditActionLabel(com.pbl3.project.pbl3_project.entity.OperationalAuditAction action) {
        return formatEnumWords(action == null ? null : action.name());
    }

    private String formatOperationalSubjectTypeLabel(com.pbl3.project.pbl3_project.entity.OperationalSubjectType subjectType) {
        return formatEnumWords(subjectType == null ? null : subjectType.name());
    }

    private String formatStocktakeScopeLabel(com.pbl3.project.pbl3_project.entity.StocktakeScopeType scopeType) {
        return formatEnumWords(scopeType == null ? null : scopeType.name());
    }

    private String formatStocktakeStatusLabel(com.pbl3.project.pbl3_project.entity.StocktakeSessionStatus status) {
        return formatEnumWords(status == null ? null : status.name());
    }

    private String formatEnumWords(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String[] parts = value.toLowerCase().split("_");
        StringBuilder label = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!label.isEmpty()) {
                label.append(' ');
            }
            label.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return label.toString();
    }

}
