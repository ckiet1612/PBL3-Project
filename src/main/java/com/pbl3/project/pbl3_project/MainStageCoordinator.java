package com.pbl3.project.pbl3_project;

import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import com.pbl3.project.pbl3_project.service.*;
import com.pbl3.project.pbl3_project.PrimaryStageReadyEvent;
import com.pbl3.project.pbl3_project.ui.ApplicationServices;
import com.pbl3.project.pbl3_project.ui.scene.*;
import com.pbl3.project.pbl3_project.ui.shell.MainShellFactory;
import com.pbl3.project.pbl3_project.ui.shell.MainShellNavigationActions;
import com.pbl3.project.pbl3_project.ui.shell.LoginSceneFactory;
import com.pbl3.project.pbl3_project.ui.shell.PostLoginLoadingSceneFactory;
import com.pbl3.project.pbl3_project.ui.shell.SceneErrorContentFactory;
import com.pbl3.project.pbl3_project.ui.shell.MainSceneRouter;
import com.pbl3.project.pbl3_project.ui.shell.VersionGateSceneFactory;
import com.pbl3.project.pbl3_project.ui.bootstrap.TenantBootstrapStore;
import com.pbl3.project.pbl3_project.ui.util.DialogSupport;
import com.pbl3.project.pbl3_project.ui.util.UiTaskExecutor;
import com.pbl3.project.pbl3_project.ui.scene.model.ImportOrderPrefill;
import com.pbl3.project.pbl3_project.ui.scene.model.ProductViewPreset;
import com.pbl3.project.pbl3_project.ui.scene.model.ReportFocusTarget;
import com.pbl3.project.pbl3_project.ui.util.TableSortState;
import java.net.URI;
import java.util.Optional;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Component
public class MainStageCoordinator implements ApplicationListener<PrimaryStageReadyEvent> {

    private final ApplicationServices services;
    private final SceneRuntimeContextFactory sceneContextFactory;
    private final Environment environment;
    private final MainSceneRouter sceneRouter = new MainSceneRouter();

    private static final double MAIN_WINDOW_DEFAULT_WIDTH = 1360;
    private static final double MAIN_WINDOW_DEFAULT_HEIGHT = 860;
    private final javafx.beans.property.DoubleProperty currentSidebarWidth = new javafx.beans.property.SimpleDoubleProperty(220.0);
    private final java.util.Map<String, TableSortState> sessionSortStates = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, javafx.scene.Node> routeContentCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicInteger routePreloadGeneration = new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.atomic.AtomicBoolean updateCheckStarted = new java.util.concurrent.atomic.AtomicBoolean();
    private int routeRequestVersion;
    private com.pbl3.project.pbl3_project.entity.User currentAuthenticatedUser;
    private boolean openShiftLeaveCheckRunning;
    private boolean closeApprovedByOpenShiftGuard;
    private Runnable currentSceneReloader = () -> {
    };

    private record RoutePreload(
        String cacheKey,
        Runnable accessCheck,
        java.util.function.Supplier<javafx.scene.Node> contentFactory
    ) {
    }

    public MainStageCoordinator(
        ApplicationServices services,
        SceneRuntimeContextFactory sceneContextFactory,
        Environment environment
    ) {
        this.services = services;
        this.sceneContextFactory = sceneContextFactory;
        this.environment = environment;
    }

    private boolean ensureAuthorized(Runnable action) {
        try {
            action.run();
            return true;
        } catch (AuthorizationException ex) {
            services.toastService().showError(ex.getMessage());
            return false;
        }
    }

    private void showUserFacingError(Throwable throwable) {
        services.toastService().showError(resolveUserFacingMessage(throwable));
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
        return MainShellFactory.resolveMainLayout(scene);
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
        if (applySidebarPreference) {
            routeContentCache.clear();
            routePreloadGeneration.incrementAndGet();
        }
        applyUserUiPreferences(root, services.userUiPreferencesService().getPreferences(user), applySidebarPreference);
    }

    private void applyUserUiPreferences(
        javafx.scene.layout.BorderPane root,
        com.pbl3.project.pbl3_project.entity.UserUiPreferences preferences,
        boolean applySidebarPreference
    ) {
        MainShellFactory.applyUserUiPreferences(root, preferences, applySidebarPreference);
    }

    private boolean isReducedMotionEnabledForUser(com.pbl3.project.pbl3_project.entity.User user) {
        if (user == null) {
            return false;
        }
        return services.userUiPreferencesService().getPreferences(user).isReducedMotion();
    }

    private SceneRuntimeContext sceneContext(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        return sceneContextFactory.create(
            stage,
            user,
            sessionSortStates,
            createSceneNavigator(stage, user),
            this::showUserFacingError,
            (title, message) -> showConfirmDialog(stage, title, message),
            (targetUser, applySidebarPreference) -> applyCurrentUserUiPreferences(stage, targetUser, applySidebarPreference)
        );
    }

    private SceneNavigation createSceneNavigator(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        return new SceneNavigation() {
            @Override
            public void showDashboard() {
                showDashboardScene(stage, user);
            }

            @Override
            public void showProducts(ProductViewPreset preset) {
                showProductsScene(stage, user, preset);
            }

            @Override
            public void showImportGoods() {
                showImportGoodsScene(stage, user);
            }

            @Override
            public void showImportGoods(ImportOrderPrefill prefill) {
                showImportGoodsScene(stage, user, prefill);
            }

            @Override
            public void showSales() {
                showSalesPosScene(stage, user);
            }

            @Override
            public void showPromotions() {
                showPromotionsScene(stage, user);
            }

            @Override
            public void showStocktake() {
                showStocktakeScene(stage, user);
            }

            @Override
            public void showSettings() {
                showSettingsScene(stage, user);
            }

            @Override
            public void showReports(java.time.LocalDate startDate, java.time.LocalDate endDate, ReportFocusTarget focusTarget) {
                showReportsScene(stage, user, startDate, endDate, focusTarget);
            }

            @Override
            public void showExpenses(java.time.LocalDate startDate, java.time.LocalDate endDate) {
                showExpensesScene(stage, user, startDate, endDate);
            }

            @Override
            public void refreshCurrentScene() {
                currentSceneReloader.run();
            }
        };
    }

    private void showProductsScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        showProductsScene(stage, user, null);
    }

    private void openScene(
        Stage stage,
        com.pbl3.project.pbl3_project.entity.User user,
        String title,
        String navId,
        java.util.function.Supplier<javafx.scene.Node> contentFactory
    ) {
        openScene(stage, user, title, navId, contentFactory, false);
    }

    private void openScene(
        Stage stage,
        com.pbl3.project.pbl3_project.entity.User user,
        String title,
        String navId,
        java.util.function.Supplier<javafx.scene.Node> contentFactory,
        boolean expandContainingSidebarGroup
    ) {
        openScene(stage, user, title, navId, contentFactory, expandContainingSidebarGroup, null);
    }

    private void openScene(
        Stage stage,
        com.pbl3.project.pbl3_project.entity.User user,
        String title,
        String navId,
        java.util.function.Supplier<javafx.scene.Node> contentFactory,
        boolean expandContainingSidebarGroup,
        String cacheKey
    ) {
        currentSceneReloader = () -> {
            invalidateCachedRouteContent(user, cacheKey);
            openScene(stage, user, title, navId, contentFactory, expandContainingSidebarGroup, cacheKey);
        };
        routeSceneWithContent(stage, user, title, navId, contentFactory, expandContainingSidebarGroup, cacheKey);
    }

    private void openAuthorizedScene(
        Stage stage,
        com.pbl3.project.pbl3_project.entity.User user,
        String title,
        String navId,
        Runnable accessCheck,
        java.util.function.Supplier<javafx.scene.Node> contentFactory
    ) {
        openAuthorizedScene(stage, user, title, navId, accessCheck, contentFactory, false);
    }

    private void openAuthorizedScene(
        Stage stage,
        com.pbl3.project.pbl3_project.entity.User user,
        String title,
        String navId,
        Runnable accessCheck,
        java.util.function.Supplier<javafx.scene.Node> contentFactory,
        boolean expandContainingSidebarGroup
    ) {
        openAuthorizedScene(stage, user, title, navId, accessCheck, contentFactory, expandContainingSidebarGroup, null);
    }

    private void openAuthorizedScene(
        Stage stage,
        com.pbl3.project.pbl3_project.entity.User user,
        String title,
        String navId,
        Runnable accessCheck,
        java.util.function.Supplier<javafx.scene.Node> contentFactory,
        boolean expandContainingSidebarGroup,
        String cacheKey
    ) {
        if (!ensureAuthorized(accessCheck)) {
            return;
        }
        currentSceneReloader = () -> {
            invalidateCachedRouteContent(user, cacheKey);
            openAuthorizedScene(
                stage,
                user,
                title,
                navId,
                accessCheck,
                contentFactory,
                expandContainingSidebarGroup,
                cacheKey
            );
        };
        routeSceneWithContent(stage, user, title, navId, contentFactory, expandContainingSidebarGroup, cacheKey);
    }

    private void openAuthorizedRecoverableScene(
        Stage stage,
        com.pbl3.project.pbl3_project.entity.User user,
        String title,
        String navId,
        Runnable accessCheck,
        java.util.function.Supplier<javafx.scene.Node> contentFactory
    ) {
        openAuthorizedRecoverableScene(stage, user, title, navId, accessCheck, contentFactory, null);
    }

    private void openAuthorizedRecoverableScene(
        Stage stage,
        com.pbl3.project.pbl3_project.entity.User user,
        String title,
        String navId,
        Runnable accessCheck,
        java.util.function.Supplier<javafx.scene.Node> contentFactory,
        String cacheKey
    ) {
        if (!ensureAuthorized(accessCheck)) {
            return;
        }
        currentSceneReloader = () -> {
            invalidateCachedRouteContent(user, cacheKey);
            openAuthorizedRecoverableScene(stage, user, title, navId, accessCheck, contentFactory, cacheKey);
        };
        routeSceneWithContent(stage, user, title, navId, contentFactory, false, cacheKey);
    }

    private void routeSceneWithContent(
        Stage stage,
        com.pbl3.project.pbl3_project.entity.User user,
        String title,
        String navId,
        java.util.function.Supplier<javafx.scene.Node> contentFactory,
        boolean expandContainingSidebarGroup,
        String cacheKey
    ) {
        int requestVersion = ++routeRequestVersion;
        activateSidebarSelection(stage, title, navId, expandContainingSidebarGroup);
        String scopedCacheKey = scopedRouteCacheKey(user, cacheKey);

        if (scopedCacheKey != null) {
            javafx.scene.Node cachedContent = routeContentCache.get(scopedCacheKey);
            if (cachedContent != null) {
                routeScene(stage, user, title, navId, cachedContent, expandContainingSidebarGroup);
                return;
            }
        }

        routeScene(stage, user, title, navId, createRouteLoadingContent(title), expandContainingSidebarGroup);
        loadRouteContentAsync(
            stage,
            user,
            title,
            navId,
            contentFactory,
            expandContainingSidebarGroup,
            scopedCacheKey,
            requestVersion
        );
    }

    private void activateSidebarSelection(
        Stage stage,
        String title,
        String navId,
        boolean expandContainingSidebarGroup
    ) {
        if (stage == null || stage.getScene() == null) {
            return;
        }
        javafx.scene.layout.BorderPane root = resolveMainLayout(stage.getScene());
        if (root == null) {
            return;
        }
        javafx.scene.control.Label pageTitle = (javafx.scene.control.Label) root.lookup("#header-title");
        if (pageTitle != null) {
            pageTitle.setText(title);
        }
        MainShellFactory.updateSidebarState(root, navId, expandContainingSidebarGroup);
    }

    private void loadRouteContentAsync(
        Stage stage,
        com.pbl3.project.pbl3_project.entity.User user,
        String title,
        String navId,
        java.util.function.Supplier<javafx.scene.Node> contentFactory,
        boolean expandContainingSidebarGroup,
        String scopedCacheKey,
        int requestVersion
    ) {
        javafx.concurrent.Task<javafx.scene.Node> loadContentTask = new javafx.concurrent.Task<>() {
            @Override
            protected javafx.scene.Node call() {
                return contentFactory.get();
            }
        };
        loadContentTask.setOnSucceeded(event -> {
            if (requestVersion != routeRequestVersion) {
                return;
            }
            javafx.scene.Node loadedContent = loadContentTask.getValue();
            if (scopedCacheKey != null) {
                routeContentCache.put(scopedCacheKey, loadedContent);
            }
            routeScene(stage, user, title, navId, loadedContent, expandContainingSidebarGroup);
        });
        loadContentTask.setOnFailed(event -> {
            if (requestVersion != routeRequestVersion) {
                return;
            }
            Throwable ex = loadContentTask.getException();
            if (ex != null) {
                ex.printStackTrace();
                services.toastService().showError(resolveUserFacingMessage(ex));
            }
            routeScene(
                stage,
                user,
                title,
                navId,
                SceneErrorContentFactory.create(title, ex, currentSceneReloader),
                expandContainingSidebarGroup
            );
        });
        String threadKey = scopedCacheKey != null ? scopedCacheKey : (navId != null ? navId : title);
        Thread worker = new Thread(loadContentTask, "sidebar-route-loader-" + threadKey.replaceAll("[^A-Za-z0-9_-]", "-"));
        worker.setDaemon(true);
        worker.start();
    }

    private String scopedRouteCacheKey(
        com.pbl3.project.pbl3_project.entity.User user,
        String cacheKey
    ) {
        if (cacheKey == null || cacheKey.isBlank()) {
            return null;
        }
        return userScopedRouteCacheKey(user, cacheKey);
    }

    private javafx.scene.Node createRouteLoadingContent(String title) {
        javafx.scene.control.ProgressIndicator spinner = new javafx.scene.control.ProgressIndicator();
        spinner.setPrefSize(42, 42);

        javafx.scene.control.Label titleLabel = new javafx.scene.control.Label("Loading " + title);
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: -app-text-primary;");

        javafx.scene.control.Label subtitleLabel = new javafx.scene.control.Label("Preparing this workspace...");
        subtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(12, spinner, titleLabel, subtitleLabel);
        content.setAlignment(javafx.geometry.Pos.CENTER);
        content.setPadding(new javafx.geometry.Insets(32));
        content.setMaxWidth(Double.MAX_VALUE);
        content.setMaxHeight(Double.MAX_VALUE);
        content.getStyleClass().add("reports-page");
        return content;
    }

    private void invalidateCachedRouteContent(com.pbl3.project.pbl3_project.entity.User user, String cacheKey) {
        if (cacheKey == null || cacheKey.isBlank()) {
            return;
        }
        routePreloadGeneration.incrementAndGet();
        routeContentCache.remove(userScopedRouteCacheKey(user, cacheKey));
    }

    private String userScopedRouteCacheKey(com.pbl3.project.pbl3_project.entity.User user, String routeKey) {
        String userKey = user != null && user.getId() != null ? user.getId().toString() : "anonymous";
        return userKey + "::" + routeKey;
    }

    private void showProductsScene(
        Stage stage,
        com.pbl3.project.pbl3_project.entity.User user,
        ProductViewPreset preset
    ) {
        openAuthorizedScene(
            stage,
            user,
            "Products",
            "nav-products",
            () -> services.authorizationService().requireProductsAccess(user),
            () -> ProductsScene.create(sceneContext(stage, user), user, new ProductsScene.Options(preset)),
            false,
            preset == null ? "products" : null
        );
    }

    private void showImportGoodsScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        showImportGoodsScene(stage, user, null);
    }

    private void showImportGoodsScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user, ImportOrderPrefill prefill) {
        openAuthorizedScene(
            stage,
            user,
            "Import Goods",
            "nav-import",
            () -> services.authorizationService().requireImportGoodsAccess(user),
            () -> ImportGoodsScene.create(sceneContext(stage, user), user, new ImportGoodsScene.Options(prefill)),
            false,
            prefill == null ? "import-goods" : null
        );
    }

    private void showOrderHistoryScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        openScene(
            stage,
            user,
            "Order History",
            "nav-history",
            () -> OrderHistoryScene.create(sceneContext(stage, user), user, new OrderHistoryScene.Options()),
            false,
            "order-history"
        );
    }

    private void showReturnsRefundsScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        openAuthorizedRecoverableScene(
            stage,
            user,
            "Returns / Refunds",
            "nav-returns",
            () -> services.authorizationService().requireReturnsRefundsAccess(user),
            () -> ReturnsRefundsScene.create(sceneContext(stage, user), user, new ReturnsRefundsScene.Options()),
            "returns-refunds"
        );
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
        openAuthorizedRecoverableScene(
            stage,
            user,
            "Expenses",
            "nav-expenses",
            () -> services.authorizationService().requireExpensesAccess(user),
            () -> ExpensesScene.create(
                sceneContext(stage, user),
                user,
                new ExpensesScene.Options(initialStartDate, initialEndDate)
            ),
            initialStartDate == null && initialEndDate == null ? "expenses" : null
        );
    }

    private void showCustomersScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        openAuthorizedScene(
            stage,
            user,
            "Customers",
            "nav-customers",
            () -> services.authorizationService().requireCustomersAccess(user),
            () -> CustomersScene.create(sceneContext(stage, user), user, new CustomersScene.Options()),
            false,
            "customers"
        );
    }

    private void showSalesPosScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        openScene(
            stage,
            user,
            "Sales (POS)",
            "nav-sales",
            () -> SalesPosScene.create(sceneContext(stage, user), user, new SalesPosScene.Options()),
            false,
            "sales-pos"
        );
    }

    private void showPromotionsScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        openAuthorizedRecoverableScene(
            stage,
            user,
            "Promotions",
            "nav-promotions",
            () -> services.authorizationService().requirePromotionsAccess(user),
            () -> PromotionsScene.create(sceneContext(stage, user), user, new PromotionsScene.Options()),
            "promotions"
        );
    }

    private void showMasterDataScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        openAuthorizedScene(
            stage,
            user,
            "Master Data",
            "nav-attributes",
            () -> services.authorizationService().requireMasterDataAccess(user),
            () -> MasterDataScene.create(sceneContext(stage, user), user, new MasterDataScene.Options()),
            false,
            "master-data"
        );
    }

    private void showDashboardScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        openScene(
            stage,
            user,
            "Dashboard",
            "nav-dashboard",
            () -> DashboardScene.create(sceneContext(stage, user), user, new DashboardScene.Options()),
            false,
            "dashboard"
        );
    }

    private void showPostLoginLoadingScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        new PostLoginLoadingSceneFactory().show(new PostLoginLoadingSceneFactory.Context(
            stage,
            MAIN_WINDOW_DEFAULT_WIDTH,
            MAIN_WINDOW_DEFAULT_HEIGHT,
            this::applyApplicationStyles,
            () -> {
                showDashboardScene(stage, user);
                startBackgroundRoutePreload(stage, user, "dashboard");
            }
        ));
    }

    private void showReportsScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        invalidateCachedRouteContent(user, "reports");
        openAuthorizedScene(
            stage,
            user,
            "Operational Reports",
            "nav-reports",
            () -> services.authorizationService().requireReportsAccess(user),
            () -> ReportsScene.create(sceneContext(stage, user), user, new ReportsScene.Options(null, null, null)),
            false,
            "reports"
        );
    }

    private void showReportsScene(
        Stage stage,
        com.pbl3.project.pbl3_project.entity.User user,
        java.time.LocalDate initialStartDate,
        java.time.LocalDate initialEndDate,
        ReportFocusTarget initialFocusTarget
    ) {
        invalidateCachedRouteContent(user, "reports");
        openAuthorizedScene(
            stage,
            user,
            "Operational Reports",
            "nav-reports",
            () -> services.authorizationService().requireReportsAccess(user),
            () -> ReportsScene.create(
                sceneContext(stage, user),
                user,
                new ReportsScene.Options(initialStartDate, initialEndDate, initialFocusTarget)
            ),
            true
        );
    }

    private void showAccountsScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        openAuthorizedScene(
            stage,
            user,
            "Accounts",
            "nav-accounts",
            () -> services.authorizationService().requireAccountsAccess(user),
            () -> AccountsScene.create(sceneContext(stage, user), user, new AccountsScene.Options()),
            false,
            "accounts"
        );
    }

    private void showSettingsScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        openAuthorizedScene(
            stage,
            user,
            "Settings",
            "nav-settings",
            () -> services.authorizationService().requireSettingsAccess(user),
            () -> SettingsScene.create(sceneContext(stage, user), user, new SettingsScene.Options()),
            false,
            "settings"
        );
    }

    private void showMyAccountScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        openScene(
            stage,
            user,
            "My Account",
            null,
            () -> MyAccountScene.create(sceneContext(stage, user), user, new MyAccountScene.Options())
        );
    }

    private void showNotificationsScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        openScene(
            stage,
            user,
            "Notifications",
            null,
            () -> NotificationsScene.create(sceneContext(stage, user), user, new NotificationsScene.Options()),
            false,
            null
        );
    }

    private void routeScene(
        Stage stage,
        com.pbl3.project.pbl3_project.entity.User user,
        String title,
        String navId,
        javafx.scene.Node content,
        boolean expandContainingSidebarGroup
    ) {
        sceneRouter.switchScene(new MainSceneRouter.Route(
            stage,
            title,
            navId,
            content,
            expandContainingSidebarGroup,
            MAIN_WINDOW_DEFAULT_WIDTH,
            MAIN_WINDOW_DEFAULT_HEIGHT,
            () -> createMainLayout(stage, user, title, content, navId),
            this::initializeMainScene,
            null,
            () -> isReducedMotionEnabledForUser(user)
        ));
    }

    private void initializeMainScene(Scene scene) {
        applyApplicationStyles(scene);
        services.toastService().setScene(scene);
    }

    private javafx.scene.layout.BorderPane createMainLayout(Stage stage, com.pbl3.project.pbl3_project.entity.User user, String title, javafx.scene.Node centerContent, String activeNavId) {
        return new MainShellFactory().create(new MainShellFactory.Context(
            stage,
            user,
            title,
            centerContent,
            activeNavId,
            services.authorizationService(),
            services.notificationService(),
            services.userUiPreferencesService(),
            currentSidebarWidth,
            services.toastService()::showError,
            createMainNavigationActions(stage, user)
        ));
    }

    private MainShellNavigationActions createMainNavigationActions(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        return new MainShellNavigationActions(
            () -> showDashboardScene(stage, user),
            () -> showReportsScene(stage, user),
            () -> showProductsScene(stage, user),
            () -> showImportGoodsScene(stage, user),
            () -> showSalesPosScene(stage, user),
            () -> showPromotionsScene(stage, user),
            () -> showMasterDataScene(stage, user),
            () -> showOrderHistoryScene(stage, user),
            () -> showReturnsRefundsScene(stage, user),
            () -> showExpensesScene(stage, user),
            () -> showCustomersScene(stage, user),
            () -> showStocktakeScene(stage, user),
            () -> showAuditLogScene(stage, user),
            () -> showAccountsScene(stage, user),
            () -> showSettingsScene(stage, user),
            () -> showMyAccountScene(stage, user),
            () -> showNotificationsScene(stage, user),
            () -> requestLogout(stage, user)
        );
    }

    private void startBackgroundRoutePreload(
        Stage stage,
        com.pbl3.project.pbl3_project.entity.User user,
        String currentCacheKey
    ) {
        int preloadGeneration = routePreloadGeneration.incrementAndGet();
        String userKey = user != null && user.getId() != null ? user.getId().toString() : "anonymous";
        Thread worker = new Thread(() -> {
            for (RoutePreload route : buildRoutePreloads(stage, user)) {
                if (preloadGeneration != routePreloadGeneration.get()) {
                    return;
                }
                if (route.cacheKey() == null || route.cacheKey().equals(currentCacheKey)) {
                    continue;
                }
                String scopedCacheKey = scopedRouteCacheKey(user, route.cacheKey());
                if (scopedCacheKey == null || routeContentCache.containsKey(scopedCacheKey)) {
                    continue;
                }
                if (!isRoutePreloadAllowed(route)) {
                    continue;
                }
                try {
                    Thread.sleep(250);
                    if (preloadGeneration != routePreloadGeneration.get()
                        || routeContentCache.containsKey(scopedCacheKey)) {
                        continue;
                    }
                    javafx.scene.Node content = route.contentFactory().get();
                    if (preloadGeneration != routePreloadGeneration.get()) {
                        return;
                    }
                    routeContentCache.putIfAbsent(scopedCacheKey, content);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Throwable ex) {
                    System.err.println("Could not preload route " + route.cacheKey() + ": " + resolveUserFacingMessage(ex));
                }
            }
        }, "sidebar-route-preloader-" + userKey);
        worker.setDaemon(true);
        worker.start();
    }

    private boolean isRoutePreloadAllowed(RoutePreload route) {
        try {
            if (route.accessCheck() != null) {
                route.accessCheck().run();
            }
            return true;
        } catch (AuthorizationException ex) {
            return false;
        }
    }

    private java.util.List<RoutePreload> buildRoutePreloads(
        Stage stage,
        com.pbl3.project.pbl3_project.entity.User user
    ) {
        return java.util.List.of(
            new RoutePreload(
                "dashboard",
                null,
                () -> DashboardScene.create(sceneContext(stage, user), user, new DashboardScene.Options())
            ),
            new RoutePreload(
                "products",
                () -> services.authorizationService().requireProductsAccess(user),
                () -> ProductsScene.create(sceneContext(stage, user), user, new ProductsScene.Options(null))
            ),
            new RoutePreload(
                "order-history",
                null,
                () -> OrderHistoryScene.create(sceneContext(stage, user), user, new OrderHistoryScene.Options())
            ),
            new RoutePreload(
                "sales-pos",
                null,
                () -> SalesPosScene.create(sceneContext(stage, user), user, new SalesPosScene.Options())
            ),
            new RoutePreload(
                "customers",
                () -> services.authorizationService().requireCustomersAccess(user),
                () -> CustomersScene.create(sceneContext(stage, user), user, new CustomersScene.Options())
            ),
            new RoutePreload(
                "import-goods",
                () -> services.authorizationService().requireImportGoodsAccess(user),
                () -> ImportGoodsScene.create(sceneContext(stage, user), user, new ImportGoodsScene.Options(null))
            ),
            new RoutePreload(
                "returns-refunds",
                () -> services.authorizationService().requireReturnsRefundsAccess(user),
                () -> ReturnsRefundsScene.create(sceneContext(stage, user), user, new ReturnsRefundsScene.Options())
            ),
            new RoutePreload(
                "promotions",
                () -> services.authorizationService().requirePromotionsAccess(user),
                () -> PromotionsScene.create(sceneContext(stage, user), user, new PromotionsScene.Options())
            ),
            new RoutePreload(
                "expenses",
                () -> services.authorizationService().requireExpensesAccess(user),
                () -> ExpensesScene.create(sceneContext(stage, user), user, new ExpensesScene.Options(null, null))
            ),
            new RoutePreload(
                "reports",
                () -> services.authorizationService().requireReportsAccess(user),
                () -> ReportsScene.create(sceneContext(stage, user), user, new ReportsScene.Options(null, null, null))
            ),
            new RoutePreload(
                "master-data",
                () -> services.authorizationService().requireMasterDataAccess(user),
                () -> MasterDataScene.create(sceneContext(stage, user), user, new MasterDataScene.Options())
            ),
            new RoutePreload(
                "stocktake",
                () -> services.authorizationService().requireStocktakeAccess(user),
                () -> StocktakeScene.create(sceneContext(stage, user), user, new StocktakeScene.Options())
            ),
            new RoutePreload(
                "audit-log",
                () -> services.authorizationService().requireAuditLogAccess(user),
                () -> AuditLogScene.create(sceneContext(stage, user), user, new AuditLogScene.Options())
            ),
            new RoutePreload(
                "accounts",
                () -> services.authorizationService().requireAccountsAccess(user),
                () -> AccountsScene.create(sceneContext(stage, user), user, new AccountsScene.Options())
            ),
            new RoutePreload(
                "settings",
                () -> services.authorizationService().requireSettingsAccess(user),
                () -> SettingsScene.create(sceneContext(stage, user), user, new SettingsScene.Options())
            )
        );
    }

    @Override
    public void onApplicationEvent(PrimaryStageReadyEvent event) {
        Stage stage = event.getStage();

        // Load bundled fonts before the first scene so JavaFX does not fall back to the platform font.
        try {
            javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/BeVietnamPro-Regular.ttf"), 12);
            javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/BeVietnamPro-Bold.ttf"), 12);
        } catch (Exception e) {
            System.err.println("Could not load fonts: " + e.getMessage());
        }

        stage.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                applyApplicationStyles(newScene);
            }
        });
        stage.setOnCloseRequest(closeEvent -> {
            if (closeApprovedByOpenShiftGuard || currentAuthenticatedUser == null) {
                closeApprovedByOpenShiftGuard = false;
                return;
            }
            closeEvent.consume();
            requestLeaveWithOpenShiftGuard(stage, currentAuthenticatedUser, "exit the app", () -> {
                closeApprovedByOpenShiftGuard = true;
                stage.close();
            });
        });

        showLoginScene(stage);
        stage.setTitle("Sales Management System");
        stage.show();
    }

    private void showLoginScene(Stage stage) {
        currentAuthenticatedUser = null;
        routeRequestVersion++;
        routePreloadGeneration.incrementAndGet();
        routeContentCache.clear();
        currentSceneReloader = () -> {
        };
        var versionCheck = services.applicationVersionService().checkClientCompatibility();
        if (!versionCheck.compatible()) {
            new VersionGateSceneFactory().show(new VersionGateSceneFactory.Context(
                stage,
                versionCheck,
                () -> showLoginScene(stage)
            ));
            return;
        }
        new LoginSceneFactory().show(new LoginSceneFactory.Context(
            stage,
            services.authService(),
            services.toastService(),
            user -> {
                currentAuthenticatedUser = user;
                showPostLoginLoadingScene(stage, user);
            },
            shouldShowUseAnotherWorkspaceAction() ? () -> requestUseAnotherWorkspace(stage) : null
        ));
        checkForDesktopUpdate(stage);
    }

    private void checkForDesktopUpdate(Stage stage) {
        if (!updateCheckStarted.compareAndSet(false, true)) {
            return;
        }

        javafx.concurrent.Task<Optional<DesktopUpdateService.DesktopUpdate>> task = new javafx.concurrent.Task<>() {
            @Override
            protected Optional<DesktopUpdateService.DesktopUpdate> call() {
                return services.desktopUpdateService().checkForUpdate();
            }
        };
        task.setOnSucceeded(event -> task.getValue().ifPresent(update -> showDesktopUpdateDialog(stage, update)));
        UiTaskExecutor.execute(task, "desktop-update-checker");
    }

    private void showDesktopUpdateDialog(Stage stage, DesktopUpdateService.DesktopUpdate update) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        if (stage != null) {
            alert.initOwner(stage);
            alert.initModality(javafx.stage.Modality.WINDOW_MODAL);
        }
        alert.setTitle("Update Available");
        alert.setHeaderText("New version " + update.latestVersion() + " is available");

        StringBuilder content = new StringBuilder();
        content.append("Current version: ").append(update.currentVersion()).append('\n');
        content.append("Latest version: ").append(update.latestVersion());
        if (update.mandatory()) {
            content.append("\n\nThis update is required for compatibility.");
        }
        if (update.releaseNotes() != null && !update.releaseNotes().isBlank()) {
            content.append("\n\n").append(update.releaseNotes());
        }
        alert.setContentText(content.toString());

        javafx.scene.control.ButtonType downloadButton = new javafx.scene.control.ButtonType(
            "Download",
            javafx.scene.control.ButtonBar.ButtonData.OK_DONE
        );
        javafx.scene.control.ButtonType laterButton = new javafx.scene.control.ButtonType(
            "Later",
            javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE
        );
        alert.getButtonTypes().setAll(downloadButton, laterButton);
        applyDialogStyles(alert.getDialogPane(), stage);

        Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == downloadButton) {
            openExternalLink(services.desktopUpdateService().buildDownloadUri(update));
        }
    }

    private void openExternalLink(URI uri) {
        if (uri == null) {
            return;
        }
        try {
            if (java.awt.Desktop.isDesktopSupported()
                && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(uri);
                return;
            }
            services.toastService().showWarning("Could not open browser automatically: " + uri);
        } catch (Exception ex) {
            services.toastService().showError("Could not open update link: " + resolveUserFacingMessage(ex));
        }
    }

    private void applyDialogStyles(javafx.scene.control.DialogPane pane, javafx.stage.Window owner) {
        if (pane == null) {
            return;
        }
        if (owner != null && owner.getScene() != null) {
            pane.getStylesheets().addAll(owner.getScene().getStylesheets());
        } else {
            java.net.URL stylesheet = getClass().getResource("/application.css");
            if (stylesheet != null) {
                pane.getStylesheets().add(stylesheet.toExternalForm());
            }
        }
        pane.getStyleClass().add("custom-alert");
    }

    private boolean shouldShowUseAnotherWorkspaceAction() {
        return environment.acceptsProfiles(Profiles.of("tenant-client")) && new TenantBootstrapStore().load().isPresent();
    }

    private void requestUseAnotherWorkspace(Stage stage) {
        if (!DialogSupport.showConfirm(
            stage,
            "Use another workspace",
            "Remove the saved workspace from this device? The app will close. Reopen it to join or create another workspace."
        )) {
            return;
        }
        try {
            new TenantBootstrapStore().clear();
            javafx.application.Platform.exit();
        } catch (Exception ex) {
            services.toastService().showError("Could not remove saved workspace: " + resolveUserFacingMessage(ex));
        }
    }

    private void requestLogout(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        requestLeaveWithOpenShiftGuard(stage, user, "logout", () -> showLoginScene(stage));
    }

    private void requestLeaveWithOpenShiftGuard(
        Stage stage,
        com.pbl3.project.pbl3_project.entity.User user,
        String action,
        Runnable onAllowed
    ) {
        if (openShiftLeaveCheckRunning) {
            return;
        }
        if (user == null || user.getId() == null) {
            onAllowed.run();
            return;
        }

        openShiftLeaveCheckRunning = true;
        java.util.concurrent.atomic.AtomicReference<javafx.stage.Stage> loadingWindowRef =
            new java.util.concurrent.atomic.AtomicReference<>();
        javafx.animation.PauseTransition delayedLoading = new javafx.animation.PauseTransition(javafx.util.Duration.millis(180));
        delayedLoading.setOnFinished(loadingEvent -> loadingWindowRef.set(DialogSupport.showLoadingWindow(
            stage,
            "Checking sales shift",
            "Checking whether this user still has an open shift...",
            380,
            220
        )));

        javafx.concurrent.Task<Boolean> checkTask = new javafx.concurrent.Task<>() {
            @Override
            protected Boolean call() {
                return services.salesShiftService().getOpenShift(user).isPresent();
            }
        };
        checkTask.setOnSucceeded(taskSucceededEvent -> {
            openShiftLeaveCheckRunning = false;
            closeLoadingWindow(delayedLoading, loadingWindowRef);
            boolean hasOpenShift = Boolean.TRUE.equals(checkTask.getValue());
            if (!hasOpenShift || confirmOpenShiftBeforeLeaving(stage, action)) {
                onAllowed.run();
            }
        });
        checkTask.setOnFailed(taskFailedEvent -> {
            openShiftLeaveCheckRunning = false;
            closeLoadingWindow(delayedLoading, loadingWindowRef);
            services.toastService().showError("Could not check open sales shift: " + resolveUserFacingMessage(checkTask.getException()));
        });

        delayedLoading.play();
        Thread worker = new Thread(checkTask, "open-shift-leave-check");
        worker.setDaemon(true);
        worker.start();
    }

    private void closeLoadingWindow(
        javafx.animation.PauseTransition delayedLoading,
        java.util.concurrent.atomic.AtomicReference<javafx.stage.Stage> loadingWindowRef
    ) {
        delayedLoading.stop();
        javafx.stage.Stage loadingWindow = loadingWindowRef.getAndSet(null);
        if (loadingWindow != null) {
            loadingWindow.close();
        }
    }

    private boolean confirmOpenShiftBeforeLeaving(Stage stage, String action) {
        String message = "You still have an open sales shift. Close the shift before you " + action
            + " to avoid forgetting end-of-shift cash reconciliation.\n\nContinue anyway?";
        return showConfirmDialog(stage, "Open sales shift", message);
    }

    private boolean showConfirmDialog(javafx.stage.Window owner, String title, String content) {
        return DialogSupport.showConfirm(owner, title, content);
    }

    private void showAuditLogScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        openAuthorizedScene(
            stage,
            user,
            "Audit Log",
            "nav-stock-history",
            () -> services.authorizationService().requireAuditLogAccess(user),
            () -> AuditLogScene.create(sceneContext(stage, user), user, new AuditLogScene.Options()),
            false,
            "audit-log"
        );
    }

    private void showStocktakeScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        openAuthorizedScene(
            stage,
            user,
            "Stocktake",
            "nav-stocktake",
            () -> services.authorizationService().requireStocktakeAccess(user),
            () -> StocktakeScene.create(sceneContext(stage, user), user, new StocktakeScene.Options()),
            false,
            "stocktake"
        );
    }
}
