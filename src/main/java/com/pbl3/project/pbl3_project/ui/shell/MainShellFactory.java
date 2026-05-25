package com.pbl3.project.pbl3_project.ui.shell;

import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.entity.UserUiPreferences;
import com.pbl3.project.pbl3_project.service.AuthorizationService;
import com.pbl3.project.pbl3_project.service.NotificationService;
import com.pbl3.project.pbl3_project.service.UserUiPreferencesService;
import com.pbl3.project.pbl3_project.ui.component.SidebarIconFactory;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.stage.Stage;
import javafx.util.Duration;

public final class MainShellFactory {

    private static final String SIDEBAR_COLLAPSE_APPLIER_KEY = "sidebarCollapseApplier";
    private static final List<String> UI_ROOT_STYLE_CLASSES = List.of(
        "ui-accent-blue",
        "ui-accent-emerald",
        "ui-accent-amber",
        "ui-density-compact",
        "ui-reduced-motion"
    );
    private static final Color PRIMARY_COLOR = Color.web("#1d7df2");
    private static final Color PRIMARY_HOVER_COLOR = Color.web("#176fd8");
    private static final Interpolator SPRING_BOUNCE = new Interpolator() {
        @Override
        protected double curve(double t) {
            double tension = 0.4;
            t -= 1.0;
            return t * t * ((tension + 1) * t + tension) + 1.0;
        }
    };

    public record Context(
        Stage stage,
        User user,
        String title,
        Node centerContent,
        String activeNavId,
        AuthorizationService authorizationService,
        NotificationService notificationService,
        UserUiPreferencesService userUiPreferencesService,
        DoubleProperty currentSidebarWidth,
        Consumer<String> errorNotifier,
        MainShellNavigationActions navigationActions
    ) {
    }

    public BorderPane create(Context context) {
        BorderPane root = new BorderPane();
        UserUiPreferences preferences = context.userUiPreferencesService().getPreferences(context.user());

        VBox sidebar = createSidebar(context);
        HBox header = createHeader(context, root, sidebar, preferences);

        root.setLeft(sidebar);
        root.setTop(header);
        root.setCenter(context.centerContent());
        BorderPane.setMargin(sidebar, new Insets(15, 0, 15, 15));
        root.setStyle("-fx-background-color: -app-surface-muted;");
        applyUserUiPreferences(root, preferences, false);
        applyInitialSidebarState(root, sidebar, preferences);
        return root;
    }

    public static BorderPane resolveMainLayout(Scene scene) {
        if (scene == null) {
            return null;
        }
        Parent currentRoot = scene.getRoot();
        if (currentRoot instanceof BorderPane borderPane && "MAIN_LAYOUT".equals(borderPane.getUserData())) {
            return borderPane;
        }
        if (currentRoot instanceof StackPane stackPane
            && !stackPane.getChildren().isEmpty()
            && stackPane.getChildren().get(0) instanceof BorderPane borderPane
            && "MAIN_LAYOUT".equals(borderPane.getUserData())) {
            return borderPane;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static void applyUserUiPreferences(BorderPane root, UserUiPreferences preferences, boolean applySidebarPreference) {
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
        if (applier instanceof BiConsumer<?, ?> rawApplier) {
            ((BiConsumer<Boolean, Boolean>) rawApplier).accept(
                preferences.isSidebarCollapsedByDefault(),
                !preferences.isReducedMotion()
            );
        }
    }

    public static boolean isReducedMotionEnabled(Node node) {
        Node current = node;
        while (current != null) {
            if (current.getStyleClass().contains("ui-reduced-motion")) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    public static void updateSidebarState(Parent root, String activeNavId, boolean expandContainingSidebarGroup) {
        for (String id : new String[]{
            "nav-dashboard", "nav-reports", "nav-products", "nav-import", "nav-sales", "nav-promotions",
            "nav-attributes", "nav-history", "nav-returns", "nav-expenses", "nav-customers", "nav-stocktake",
            "nav-stock-history", "nav-accounts", "nav-settings"
        }) {
            Node button = root.lookup("#" + id);
            if (button == null) {
                continue;
            }
            if (id.equals(activeNavId)) {
                if (!button.getStyleClass().contains("active")) {
                    button.getStyleClass().add("active");
                }
            } else {
                button.getStyleClass().remove("active");
            }
        }

        if (expandContainingSidebarGroup) {
            for (Node node : root.lookupAll(".sidebar-section")) {
                if (node instanceof VBox section
                    && section.getProperties().get("sidebarItemIds") instanceof Set<?> ids
                    && ids.contains(activeNavId)) {
                    setSidebarSectionExpanded(section, true, true);
                    break;
                }
            }
        }

        for (Node node : root.lookupAll(".sidebar-section")) {
            if (node instanceof VBox section) {
                refreshSidebarSectionActiveDot(section);
            }
        }
    }

    public static void refreshTablesAfterContainerResize(Parent root) {
        if (root == null) {
            return;
        }
        Platform.runLater(() -> {
            root.applyCss();
            root.requestLayout();
            for (Node node : root.lookupAll(".table-view")) {
                if (node instanceof TableView<?> table) {
                    requestStableTableLayout(table);
                }
            }
        });
    }

    private VBox createSidebar(Context context) {
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

        Button navDashboard = createNavButton("Dashboard", "nav-dashboard", context.navigationActions().showDashboard(), context.errorNotifier());
        navDashboard.getStyleClass().add("dashboard-nav-button");
        navDashboard.setGraphic(SidebarIconFactory.createDashboardNavIcon());
        Button navReports = createNavButton("Reports", "nav-reports", context.navigationActions().showReports(), context.errorNotifier());
        navReports.setGraphic(SidebarIconFactory.createReportsNavIcon());
        Button navProducts = createNavButton("Products", "nav-products", context.navigationActions().showProducts(), context.errorNotifier());
        navProducts.setGraphic(SidebarIconFactory.createProductsNavIcon());
        Button navImport = createNavButton("Import Goods", "nav-import", context.navigationActions().showImportGoods(), context.errorNotifier());
        navImport.setGraphic(SidebarIconFactory.createImportNavIcon());
        Button navSales = createNavButton("Sales (POS)", "nav-sales", context.navigationActions().showSales(), context.errorNotifier());
        navSales.setGraphic(SidebarIconFactory.createSalesNavIcon());
        Button navPromotions = createNavButton("Promotions", "nav-promotions", context.navigationActions().showPromotions(), context.errorNotifier());
        navPromotions.setGraphic(SidebarIconFactory.createPromotionsNavIcon());
        Button navAttributes = createNavButton("Master Data", "nav-attributes", context.navigationActions().showMasterData(), context.errorNotifier());
        navAttributes.setGraphic(SidebarIconFactory.createMasterDataNavIcon());
        Button navHistory = createNavButton("Order History", "nav-history", context.navigationActions().showOrderHistory(), context.errorNotifier());
        navHistory.setGraphic(SidebarIconFactory.createOrderHistoryNavIcon());
        Button navReturns = createNavButton("Returns", "nav-returns", context.navigationActions().showReturns(), context.errorNotifier());
        navReturns.setGraphic(SidebarIconFactory.createReturnsNavIcon());
        Button navExpenses = createNavButton("Expenses", "nav-expenses", context.navigationActions().showExpenses(), context.errorNotifier());
        navExpenses.setGraphic(SidebarIconFactory.createExpensesNavIcon());
        Button navCustomers = createNavButton("Customers", "nav-customers", context.navigationActions().showCustomers(), context.errorNotifier());
        navCustomers.setGraphic(SidebarIconFactory.createCustomersNavIcon());
        Button navStocktake = createNavButton("Stocktake", "nav-stocktake", context.navigationActions().showStocktake(), context.errorNotifier());
        navStocktake.setGraphic(SidebarIconFactory.createStocktakeNavIcon());
        Button navStockHistory = createNavButton("Audit Log", "nav-stock-history", context.navigationActions().showAuditLog(), context.errorNotifier());
        navStockHistory.setGraphic(SidebarIconFactory.createAuditLogNavIcon());
        Button navAccounts = createNavButton("Accounts", "nav-accounts", context.navigationActions().showAccounts(), context.errorNotifier());
        navAccounts.setGraphic(SidebarIconFactory.createAccountsNavIcon());
        Button navSettings = createNavButton("Settings", "nav-settings", context.navigationActions().showSettings(), context.errorNotifier());
        navSettings.setGraphic(SidebarIconFactory.createSettingsNavIcon());

        List<Button> allNavButtons = List.of(
            navDashboard, navReports, navProducts, navImport, navSales, navPromotions, navAttributes,
            navHistory, navReturns, navExpenses, navCustomers, navStocktake, navStockHistory, navAccounts, navSettings
        );
        allNavButtons.forEach(button -> {
            button.setContentDisplay(ContentDisplay.LEFT);
            installSidebarNavIconMotion(button);
        });

        Button navLogout = new Button("Logout");
        navLogout.setId("nav-logout");
        navLogout.getStyleClass().setAll("nav-logout-btn");
        installSidebarPressBounce(navLogout);
        navLogout.setOnAction(e -> context.navigationActions().showLogin().run());
        navLogout.setGraphic(SidebarIconFactory.createLogoutNavIcon());
        installSidebarIconMotion(navLogout, navLogout.getGraphic(), null, 1.05, -0.9, 1.0, 0.0);
        applyActiveNav(context.activeNavId(), allNavButtons);

        List<Node> operationsItems = new ArrayList<>();
        operationsItems.add(navDashboard);
        if (context.authorizationService().canAccessSales(context.user())) operationsItems.add(navSales);
        if (context.authorizationService().canAccessPromotions(context.user())) operationsItems.add(navPromotions);
        if (context.authorizationService().canAccessProducts(context.user())) operationsItems.add(navProducts);
        if (context.authorizationService().canAccessImportGoods(context.user())) operationsItems.add(navImport);
        if (context.authorizationService().canAccessOrderHistory(context.user())) operationsItems.add(navHistory);
        if (context.authorizationService().canAccessReturnsRefunds(context.user())) operationsItems.add(navReturns);
        if (context.authorizationService().canAccessExpenses(context.user())) operationsItems.add(navExpenses);
        if (context.authorizationService().canAccessCustomers(context.user())) operationsItems.add(navCustomers);

        List<Node> controlItems = new ArrayList<>();
        if (context.authorizationService().canAccessReports(context.user())) controlItems.add(navReports);
        if (context.authorizationService().canAccessStocktake(context.user())) controlItems.add(navStocktake);
        if (context.authorizationService().canAccessAuditLog(context.user())) controlItems.add(navStockHistory);

        List<Node> setupItems = new ArrayList<>();
        if (context.authorizationService().canAccessMasterData(context.user())) setupItems.add(navAttributes);
        if (context.authorizationService().canAccessAccounts(context.user())) setupItems.add(navAccounts);

        VBox sidebarMenu = new VBox(10);
        sidebarMenu.setFillWidth(true);
        sidebarMenu.setPadding(new Insets(8, 8, 12, 0));
        sidebarMenu.getChildren().add(createSidebarSection("Operations", operationsItems, hasActiveChild(operationsItems, context.activeNavId())));
        if (!controlItems.isEmpty()) {
            sidebarMenu.getChildren().add(createSidebarSection("Control", controlItems, hasActiveChild(controlItems, context.activeNavId())));
        }
        if (!setupItems.isEmpty()) {
            sidebarMenu.getChildren().add(createSidebarSection("Setup", setupItems, hasActiveChild(setupItems, context.activeNavId())));
        }

        ScrollPane sidebarScroll = new ScrollPane(sidebarMenu);
        sidebarScroll.setFitToWidth(true);
        sidebarScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sidebarScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sidebarScroll.setPannable(true);
        sidebarScroll.getStyleClass().add("sidebar-scroll");
        VBox.setVgrow(sidebarScroll, Priority.ALWAYS);
        VBox.setMargin(sidebarScroll, new Insets(0, -15, 0, 0));

        VBox sidebarBottomActions = new VBox(8, navSettings, navLogout);
        sidebarBottomActions.setFillWidth(true);
        sidebar.getChildren().addAll(appTitle, sidebarScroll, new Separator(), sidebarBottomActions);
        sidebar.setSpacing(8);
        sidebar.setPadding(new Insets(18, 14, 18, 14));

        Rectangle clipRect = new Rectangle();
        clipRect.widthProperty().bind(sidebar.widthProperty());
        clipRect.heightProperty().bind(sidebar.heightProperty());
        clipRect.setArcWidth(40);
        clipRect.setArcHeight(40);
        sidebar.setClip(clipRect);
        context.currentSidebarWidth().unbind();
        context.currentSidebarWidth().bind(sidebar.widthProperty());
        return sidebar;
    }

    private HBox createHeader(Context context, BorderPane root, VBox sidebar, UserUiPreferences preferences) {
        HBox header = new HBox(10);
        header.getStyleClass().add("app-header");
        header.setAlignment(Pos.CENTER_LEFT);

        boolean[] sidebarHidden = {false};
        Button toggleSidebar = createHamburgerToggleButton();
        BiConsumer<Boolean, Boolean> applySidebarCollapsed = (collapsed, animated) -> applySidebarCollapsed(root, sidebar, sidebarHidden, collapsed, animated);
        root.getProperties().put(SIDEBAR_COLLAPSE_APPLIER_KEY, applySidebarCollapsed);
        toggleSidebar.setOnAction(e -> applySidebarCollapsed.accept(!sidebarHidden[0], !isReducedMotionEnabled(root)));

        Label pageTitle = new Label(context.title());
        pageTitle.setId("header-title");
        pageTitle.getStyleClass().add("header-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button myAccountButton = new Button();
        myAccountButton.getStyleClass().addAll("button", "header-account-button");
        myAccountButton.setGraphic(SidebarIconFactory.createMyAccountHeaderIcon());
        myAccountButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        myAccountButton.setTooltip(new Tooltip("My Account"));
        myAccountButton.setMinSize(36, 36);
        myAccountButton.setPrefSize(36, 36);
        myAccountButton.setMaxSize(36, 36);
        installSidebarPressBounce(myAccountButton);
        myAccountButton.setOnAction(e -> context.navigationActions().showMyAccount().run());

        Button notificationButton = createNotificationButton(context, root);
        Label userLabel = new Label(formatUserLabel(context.user()));
        userLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -app-text-muted;");
        header.getChildren().addAll(toggleSidebar, pageTitle, spacer, userLabel, notificationButton, myAccountButton);
        return header;
    }

    private Button createNotificationButton(Context context, BorderPane root) {
        Button button = new Button();
        button.getStyleClass().addAll("button", "header-notification-button");
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setTooltip(new Tooltip("Notifications"));
        button.setMinSize(36, 36);
        button.setPrefSize(36, 36);
        button.setMaxSize(36, 36);
        button.setFocusTraversable(false);
        installSidebarPressBounce(button);

        SVGPath bellIcon = new SVGPath();
        bellIcon.setContent("M18 8A6 6 0 0 0 6 8c0 7-3 7-3 9h18c0-2-3-2-3-9M13.73 21a2 2 0 0 1-3.46 0");
        bellIcon.getStyleClass().add("header-notification-icon-stroke");
        bellIcon.setStrokeLineCap(StrokeLineCap.ROUND);
        bellIcon.setStrokeLineJoin(StrokeLineJoin.ROUND);
        bellIcon.setFill(Color.TRANSPARENT);
        bellIcon.setStrokeWidth(1.9);
        bellIcon.setScaleX(1.08);
        bellIcon.setScaleY(1.08);

        Label badge = new Label();
        badge.getStyleClass().add("header-notification-badge");
        badge.setVisible(false);
        badge.setManaged(false);
        badge.setMouseTransparent(true);
        StackPane graphic = new StackPane(bellIcon, badge);
        graphic.setMinSize(28, 28);
        graphic.setPrefSize(28, 28);
        graphic.setMaxSize(28, 28);
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        button.setGraphic(graphic);
        button.setOnAction(e -> context.navigationActions().showNotifications().run());

        Runnable refreshBadge = () -> refreshNotificationBadge(context, badge);
        refreshBadge.run();
        Timeline poll = new Timeline(new KeyFrame(Duration.seconds(60), e -> refreshBadge.run()));
        poll.setCycleCount(Animation.INDEFINITE);
        poll.play();
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                poll.stop();
            }
        });
        return button;
    }

    private void refreshNotificationBadge(Context context, Label badge) {
        if (context.notificationService() == null || context.user() == null || context.user().getId() == null) {
            badge.setVisible(false);
            badge.setManaged(false);
            return;
        }
        javafx.concurrent.Task<Long> task = new javafx.concurrent.Task<>() {
            @Override
            protected Long call() {
                return context.notificationService().countUnread(context.user());
            }
        };
        task.setOnSucceeded(event -> {
            long unread = task.getValue() == null ? 0L : task.getValue();
            badge.setText(unread > 99 ? "99+" : Long.toString(unread));
            badge.setVisible(unread > 0);
            badge.setManaged(unread > 0);
        });
        task.setOnFailed(event -> {
            badge.setVisible(false);
            badge.setManaged(false);
        });
        Thread worker = new Thread(task, "notification-badge-refresh");
        worker.setDaemon(true);
        worker.start();
    }

    private void applyInitialSidebarState(BorderPane root, VBox sidebar, UserUiPreferences preferences) {
        @SuppressWarnings("unchecked")
        BiConsumer<Boolean, Boolean> applier = (BiConsumer<Boolean, Boolean>) root.getProperties().get(SIDEBAR_COLLAPSE_APPLIER_KEY);
        if (applier != null) {
            applier.accept(preferences.isSidebarCollapsedByDefault(), false);
        }
    }

    private void applySidebarCollapsed(BorderPane root, VBox sidebar, boolean[] sidebarHidden, boolean collapsed, boolean animated) {
        if (collapsed == sidebarHidden[0]) {
            return;
        }
        double sidebarWidth = 220;
        if (collapsed) {
            sidebarHidden[0] = true;
            if (!animated) {
                sidebar.setPrefWidth(0);
                sidebar.setMinWidth(0);
                sidebar.setMaxWidth(0);
                sidebar.setOpacity(0);
                BorderPane.setMargin(sidebar, new Insets(0));
                root.requestLayout();
                refreshTablesAfterContainerResize(root);
                return;
            }
            setCenterContentCache(root, true);
            Timeline hideTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(sidebar.prefWidthProperty(), sidebarWidth),
                    new KeyValue(sidebar.minWidthProperty(), sidebarWidth),
                    new KeyValue(sidebar.maxWidthProperty(), sidebarWidth),
                    new KeyValue(sidebar.opacityProperty(), 1.0)
                ),
                new KeyFrame(Duration.millis(150),
                    new KeyValue(sidebar.prefWidthProperty(), 0, Interpolator.EASE_BOTH),
                    new KeyValue(sidebar.minWidthProperty(), 0, Interpolator.EASE_BOTH),
                    new KeyValue(sidebar.maxWidthProperty(), 0, Interpolator.EASE_BOTH),
                    new KeyValue(sidebar.opacityProperty(), 0, Interpolator.EASE_IN)
                )
            );
            hideTimeline.setOnFinished(ev -> {
                BorderPane.setMargin(sidebar, new Insets(0));
                setCenterContentCache(root, false);
                root.requestLayout();
                refreshTablesAfterContainerResize(root);
            });
            hideTimeline.play();
            return;
        }

        sidebarHidden[0] = false;
        BorderPane.setMargin(sidebar, new Insets(15, 0, 15, 15));
        if (!animated) {
            sidebar.setPrefWidth(sidebarWidth);
            sidebar.setMinWidth(sidebarWidth);
            sidebar.setMaxWidth(sidebarWidth);
            sidebar.setOpacity(1.0);
            root.requestLayout();
            refreshTablesAfterContainerResize(root);
            return;
        }
        setCenterContentCache(root, true);
        Timeline showTimeline = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(sidebar.prefWidthProperty(), 0),
                new KeyValue(sidebar.minWidthProperty(), 0),
                new KeyValue(sidebar.maxWidthProperty(), 0),
                new KeyValue(sidebar.opacityProperty(), 0)
            ),
            new KeyFrame(Duration.millis(150),
                new KeyValue(sidebar.prefWidthProperty(), sidebarWidth, Interpolator.EASE_BOTH),
                new KeyValue(sidebar.minWidthProperty(), sidebarWidth, Interpolator.EASE_BOTH),
                new KeyValue(sidebar.maxWidthProperty(), sidebarWidth, Interpolator.EASE_BOTH),
                new KeyValue(sidebar.opacityProperty(), 1.0, Interpolator.EASE_OUT)
            )
        );
        showTimeline.setOnFinished(ev -> {
            sidebar.setPrefWidth(sidebarWidth);
            sidebar.setMinWidth(sidebarWidth);
            sidebar.setMaxWidth(sidebarWidth);
            setCenterContentCache(root, false);
            root.requestLayout();
            refreshTablesAfterContainerResize(root);
        });
        showTimeline.play();
    }

    private static void setCenterContentCache(BorderPane root, boolean enabled) {
        if (root == null || root.getCenter() == null) {
            return;
        }
        root.getCenter().setCache(enabled);
        root.getCenter().setCacheHint(enabled ? CacheHint.SPEED : CacheHint.DEFAULT);
    }

    private static boolean hasActiveChild(List<Node> items, String activeNavId) {
        return items.stream().anyMatch(node -> activeNavId != null && activeNavId.equals(node.getId()));
    }

    private static void applyActiveNav(String activeNavId, List<Button> buttons) {
        for (Button button : buttons) {
            if (button.getId() != null && button.getId().equals(activeNavId) && !button.getStyleClass().contains("active")) {
                button.getStyleClass().add("active");
            }
        }
    }

    private static Button createNavButton(String text, String id, Runnable action, Consumer<String> errorNotifier) {
        Button button = new Button(text);
        button.setId(id);
        button.getStyleClass().setAll("nav-button");
        installSidebarPressBounce(button);
        button.setOnAction(e -> {
            try {
                action.run();
            } catch (Throwable ex) {
                ex.printStackTrace();
                String message = ex.getMessage() == null || ex.getMessage().isBlank()
                    ? MessageFormat.format("Could not open {0}", text)
                    : ex.getMessage();
                errorNotifier.accept(message);
            }
        });
        return button;
    }

    private static VBox createSidebarSection(String title, List<Node> items, boolean expandedInitially) {
        VBox section = new VBox(8);
        section.getStyleClass().add("sidebar-section");
        section.setFillWidth(true);
        section.getProperties().put(
            "sidebarItemIds",
            items.stream().map(Node::getId).filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet())
        );

        Label sectionLabel = new Label(title);
        sectionLabel.getStyleClass().add("sidebar-section-label");
        sectionLabel.setMinWidth(0);
        sectionLabel.setMaxWidth(Double.MAX_VALUE);
        Node sectionIcon = createSidebarSectionIcon(title);

        SVGPath chevronIcon = new SVGPath();
        chevronIcon.setContent("M8 6 L14 12 L8 18");
        chevronIcon.getStyleClass().add("sidebar-section-chevron");
        chevronIcon.setStrokeWidth(2.0);
        chevronIcon.setStrokeLineCap(StrokeLineCap.ROUND);
        chevronIcon.setStrokeLineJoin(StrokeLineJoin.ROUND);
        chevronIcon.setFill(Color.TRANSPARENT);
        chevronIcon.setRotate(expandedInitially ? 90 : 0);

        StackPane chevronWrap = fixedStack(chevronIcon, 18, 18);
        Circle activeDot = new Circle(3.2);
        activeDot.getStyleClass().add("sidebar-section-active-dot");
        StackPane activeDotWrap = fixedStack(activeDot, 8, 8);
        activeDotWrap.setTranslateY(1.0);
        activeDotWrap.setVisible(false);
        activeDotWrap.setManaged(false);

        HBox trailingWrap = new HBox(4, activeDotWrap, chevronWrap);
        trailingWrap.setAlignment(Pos.CENTER_RIGHT);
        trailingWrap.setMinSize(Region.USE_PREF_SIZE, 18);
        trailingWrap.setPrefHeight(18);

        HBox header = sectionIcon == null
            ? new HBox(8, sectionLabel, trailingWrap)
            : new HBox(8, sectionIcon, sectionLabel, trailingWrap);
        HBox.setHgrow(sectionLabel, Priority.ALWAYS);
        header.getStyleClass().add("sidebar-section-header");
        if (expandedInitially) {
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
        content.setMaxHeight(expandedInitially ? Region.USE_COMPUTED_SIZE : 0);
        section.getProperties().put("sidebarContent", content);

        Rectangle contentClip = new Rectangle();
        contentClip.setX(-12);
        contentClip.setY(-12);
        contentClip.widthProperty().bind(content.widthProperty().add(24));
        contentClip.heightProperty().bind(content.heightProperty().add(24));
        content.setClip(contentClip);

        section.getProperties().put("sidebarAnimationRef", new AtomicReference<Timeline>(null));
        header.setOnMouseClicked(e -> setSidebarSectionExpanded(section, !content.isManaged(), true));
        section.getChildren().addAll(header, content);
        refreshSidebarSectionActiveDot(section);
        return section;
    }

    private static StackPane fixedStack(Node child, double width, double height) {
        StackPane stack = new StackPane(child);
        stack.setMinSize(width, height);
        stack.setPrefSize(width, height);
        stack.setMaxSize(width, height);
        return stack;
    }

    private static void refreshSidebarSectionActiveDot(VBox section) {
        Object contentNode = section.getProperties().get("sidebarContent");
        Object activeDotNode = section.getProperties().get("sidebarActiveDot");
        if (!(contentNode instanceof VBox content) || !(activeDotNode instanceof StackPane activeDotWrap)) {
            return;
        }
        boolean hasActiveChild = content.getChildren().stream().anyMatch(node -> node.getStyleClass().contains("active"));
        boolean showDot = hasActiveChild && !content.isManaged();
        activeDotWrap.setManaged(showDot);
        activeDotWrap.setVisible(showDot);
        activeDotWrap.setOpacity(showDot ? 1.0 : 0.0);
    }

    @SuppressWarnings("unchecked")
    private static void setSidebarSectionExpanded(VBox section, boolean expanded, boolean animated) {
        if (isReducedMotionEnabled(section)) {
            animated = false;
        }
        Object headerNode = section.getProperties().get("sidebarHeader");
        Object contentNode = section.getProperties().get("sidebarContent");
        Object chevronNode = section.getProperties().get("sidebarChevron");
        Object animationRefNode = section.getProperties().get("sidebarAnimationRef");
        if (!(headerNode instanceof HBox header) || !(contentNode instanceof VBox content) || !(chevronNode instanceof SVGPath chevronIcon)) {
            return;
        }
        boolean currentlyExpanded = content.isManaged();
        AtomicReference<Timeline> animationRef = animationRefNode instanceof AtomicReference<?> rawRef
            ? (AtomicReference<Timeline>) rawRef
            : new AtomicReference<>(null);
        if (animationRef.get() != null) {
            animationRef.get().stop();
            animationRef.set(null);
        }
        if (!animated || currentlyExpanded == expanded) {
            applySidebarSectionExpandedState(header, content, chevronIcon, expanded);
            refreshSidebarSectionActiveDot(section);
            return;
        }
        if (expanded) {
            content.setManaged(true);
            content.setVisible(true);
            content.setOpacity(1.0);
            content.setTranslateY(-6.0);
            content.setMaxHeight(0.0);
            Timeline expand = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(content.maxHeightProperty(), 0.0),
                    new KeyValue(content.opacityProperty(), 0.0),
                    new KeyValue(content.translateYProperty(), -6.0),
                    new KeyValue(chevronIcon.rotateProperty(), chevronIcon.getRotate())
                ),
                new KeyFrame(Duration.millis(140),
                    new KeyValue(content.maxHeightProperty(), Math.max(content.prefHeight(-1), 1), Interpolator.EASE_BOTH),
                    new KeyValue(content.opacityProperty(), 1.0, Interpolator.EASE_BOTH),
                    new KeyValue(content.translateYProperty(), 0.0, Interpolator.EASE_BOTH),
                    new KeyValue(chevronIcon.rotateProperty(), 90.0, Interpolator.EASE_BOTH)
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
        Timeline collapse = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(content.maxHeightProperty(), currentHeight),
                new KeyValue(content.opacityProperty(), content.getOpacity()),
                new KeyValue(content.translateYProperty(), content.getTranslateY()),
                new KeyValue(chevronIcon.rotateProperty(), chevronIcon.getRotate())
            ),
            new KeyFrame(Duration.millis(140),
                new KeyValue(content.maxHeightProperty(), 0.0, Interpolator.EASE_BOTH),
                new KeyValue(content.opacityProperty(), 0.0, Interpolator.EASE_BOTH),
                new KeyValue(content.translateYProperty(), -6.0, Interpolator.EASE_BOTH),
                new KeyValue(chevronIcon.rotateProperty(), 0.0, Interpolator.EASE_BOTH)
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

    private static void applySidebarSectionExpandedState(HBox header, VBox content, SVGPath chevronIcon, boolean expanded) {
        if (expanded) {
            content.setManaged(true);
            content.setVisible(true);
            content.setOpacity(1.0);
            content.setTranslateY(0.0);
            content.setMaxHeight(Region.USE_COMPUTED_SIZE);
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

    private static Node createSidebarSectionIcon(String title) {
        return switch (title) {
            case "Operations" -> wrapSectionIcon("M22 12h-2.48a2 2 0 0 0-1.93 1.46l-2.35 8.36a.25.25 0 0 1-.48 0L9.24 2.18a.25.25 0 0 0-.48 0l-2.35 8.36A2 2 0 0 1 4.49 12H2", 0.78);
            case "Control" -> wrapSectionIcon("M21 4H14 M10 4H3 M21 12H12 M8 12H3 M21 20H16 M12 20H3 M14 2V6 M8 10V14 M16 18V22", 0.78);
            case "Setup" -> wrapSectionIcon("M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.4-3.4a6 6 0 0 1-7.94 7.94l-6.91 6.91a2 2 0 0 1-2.83-2.83l6.9-6.9a6 6 0 0 1 7.94-7.94z", 0.8);
            default -> null;
        };
    }

    private static Node wrapSectionIcon(String pathContent, double scale) {
        SVGPath path = new SVGPath();
        path.setContent(pathContent);
        path.setFill(Color.TRANSPARENT);
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        path.setStrokeLineJoin(StrokeLineJoin.ROUND);
        path.setSmooth(true);
        path.getStyleClass().add("sidebar-section-icon-stroke");
        StackPane iconWrap = new StackPane(path);
        iconWrap.setMinSize(18, 18);
        iconWrap.setPrefSize(18, 18);
        iconWrap.setMaxSize(18, 18);
        iconWrap.setScaleX(scale);
        iconWrap.setScaleY(scale);
        iconWrap.setMouseTransparent(true);
        return iconWrap;
    }

    private static Button createHamburgerToggleButton() {
        Button toggleButton = new Button();
        toggleButton.getStyleClass().clear();
        toggleButton.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-border-color: transparent;");
        toggleButton.setPrefSize(34, 34);
        toggleButton.setMinSize(34, 34);
        toggleButton.setMaxSize(34, 34);
        toggleButton.setCursor(javafx.scene.Cursor.HAND);
        toggleButton.setFocusTraversable(false);
        installSidebarPressBounce(toggleButton);

        java.util.function.Function<String, SVGPath> pathFactory = content -> {
            SVGPath path = new SVGPath();
            path.setContent(content);
            path.setFill(Color.TRANSPARENT);
            path.setStroke(PRIMARY_COLOR);
            path.setStrokeWidth(1.5);
            path.setStrokeLineCap(StrokeLineCap.ROUND);
            path.setStrokeLineJoin(StrokeLineJoin.ROUND);
            path.setSmooth(true);
            return path;
        };
        SVGPath panel = pathFactory.apply("M5 3H19A2 2 0 0 1 21 5V19A2 2 0 0 1 19 21H5A2 2 0 0 1 3 19V5A2 2 0 0 1 5 3Z");
        SVGPath divider = pathFactory.apply("M9 3V21");
        javafx.scene.layout.Pane iconPane = new javafx.scene.layout.Pane(panel, divider);
        iconPane.setPrefSize(24, 24);
        iconPane.setMinSize(24, 24);
        iconPane.setMaxSize(24, 24);
        iconPane.setMouseTransparent(true);
        StackPane iconWrap = new StackPane(iconPane);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMinSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.setScaleX(1.16);
        iconWrap.setScaleY(1.16);
        iconWrap.setMouseTransparent(true);
        toggleButton.hoverProperty().addListener((obs, wasHovering, isHovering) -> {
            Color color = isHovering ? PRIMARY_HOVER_COLOR : PRIMARY_COLOR;
            panel.setStroke(color);
            divider.setStroke(color);
        });
        toggleButton.setGraphic(iconWrap);
        toggleButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        toggleButton.setAlignment(Pos.CENTER);
        return toggleButton;
    }

    private static void installSidebarNavIconMotion(Button button) {
        installSidebarIconMotion(button, button.getGraphic(), "active", 1.055, -1.0, 1.02, -0.35);
    }

    private static void installSidebarIconMotion(
        Node trigger,
        Node icon,
        String activeStyleClass,
        double hoverScaleMultiplier,
        double hoverTranslateY,
        double activeScaleMultiplier,
        double activeTranslateY
    ) {
        if (trigger == null || icon == null) {
            return;
        }
        String timelineKey = "sidebarIconMotionTimeline";
        double baseScaleX = Math.abs(icon.getScaleX()) > 0.0001 ? icon.getScaleX() : 1.0;
        double baseScaleY = Math.abs(icon.getScaleY()) > 0.0001 ? icon.getScaleY() : 1.0;
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
            Animation existing = (Animation) icon.getProperties().get(timelineKey);
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
            Timeline timeline = new Timeline(new KeyFrame(
                Duration.millis(140),
                new KeyValue(icon.scaleXProperty(), targetScaleX, Interpolator.EASE_BOTH),
                new KeyValue(icon.scaleYProperty(), targetScaleY, Interpolator.EASE_BOTH),
                new KeyValue(icon.translateYProperty(), targetTranslateY, Interpolator.EASE_BOTH)
            ));
            timeline.setOnFinished(e -> icon.getProperties().remove(timelineKey));
            icon.getProperties().put(timelineKey, timeline);
            timeline.play();
        };
        trigger.hoverProperty().addListener((obs, oldVal, newVal) -> refreshState.run());
        trigger.getStyleClass().addListener((ListChangeListener<String>) change -> refreshState.run());
        refreshState.run();
    }

    private static void installSidebarPressBounce(Node node) {
        String timelineKey = "sidebarPressBounceTimeline";
        double pressedOffset = 1.8;
        Consumer<Double> animateTo = targetY -> {
            if (isReducedMotionEnabled(node)) {
                node.setTranslateY(0.0);
                return;
            }
            Animation existing = (Animation) node.getProperties().get(timelineKey);
            if (existing != null) {
                existing.stop();
            }
            Timeline timeline = new Timeline(new KeyFrame(
                Duration.millis(55),
                new KeyValue(node.translateYProperty(), targetY, Interpolator.EASE_BOTH)
            ));
            timeline.setOnFinished(e -> node.getProperties().remove(timelineKey));
            node.getProperties().put(timelineKey, timeline);
            timeline.play();
        };
        Runnable bounceBack = () -> {
            if (isReducedMotionEnabled(node)) {
                node.setTranslateY(0.0);
                return;
            }
            Animation existing = (Animation) node.getProperties().get(timelineKey);
            if (existing != null) {
                existing.stop();
            }
            Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(node.translateYProperty(), node.getTranslateY())),
                new KeyFrame(Duration.millis(85), new KeyValue(node.translateYProperty(), -0.9, SPRING_BOUNCE)),
                new KeyFrame(Duration.millis(165), new KeyValue(node.translateYProperty(), 0.0, Interpolator.EASE_OUT))
            );
            timeline.setOnFinished(e -> {
                node.setTranslateY(0);
                node.getProperties().remove(timelineKey);
            });
            node.getProperties().put(timelineKey, timeline);
            timeline.play();
        };
        node.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() == MouseButton.PRIMARY && !node.isDisable()) {
                animateTo.accept(pressedOffset);
            }
        });
        node.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_RELEASED, event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                bounceBack.run();
            }
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

    private static void requestStableTableLayout(TableView<?> table) {
        if (table == null) {
            return;
        }
        table.applyCss();
        table.requestLayout();
        Platform.runLater(() -> {
            table.applyCss();
            table.requestLayout();
        });
    }

    private static String formatUserLabel(User user) {
        String name = user.getFullName() == null || user.getFullName().isBlank() ? user.getUsername() : user.getFullName();
        return name + " (" + enumText(user.getRole()) + ")";
    }

    private static String enumText(Enum<?> value) {
        if (value == null) {
            return "-";
        }
        String[] parts = value.name().toLowerCase().split("_");
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
