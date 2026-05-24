package com.pbl3.project.pbl3_project.ui.scene;

import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.ui.ApplicationServices;
import com.pbl3.project.pbl3_project.ui.util.TableSortState;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

@Component
public final class SceneRuntimeContextFactory {

    private final ApplicationServices services;

    public SceneRuntimeContextFactory(ApplicationServices services) {
        this.services = services;
    }

    public SceneRuntimeContext create(
        Stage owner,
        User user,
        Map<String, TableSortState> sessionSortStates,
        SceneNavigation navigator,
        Consumer<Throwable> errorHandler,
        BiFunction<String, String, Boolean> confirmDialog,
        BiConsumer<User, Boolean> uiPreferenceApplier
    ) {
        return new SceneRuntimeContext(
            owner,
            services.authService(),
            services.productService(),
            services.categoryRepository(),
            services.orderService(),
            services.reportService(),
            services.salesShiftService(),
            services.notificationService(),
            services.toastService(),
            services.authorizationService(),
            services.userAccountService(),
            services.accountAuditLogService(),
            services.operationalAuditLogService(),
            services.customerService(),
            services.categoryService(),
            services.brandService(),
            services.supplierService(),
            services.originService(),
            services.unitService(),
            services.importOrderService(),
            services.expenseService(),
            services.promotionService(),
            services.sePaySettingsService(),
            services.qrPaymentService(),
            services.realtimeDataSyncService(),
            services.receiptService(),
            services.userUiPreferencesService(),
            services.transactionService(),
            services.stocktakeService(),
            services.dataBackupService(),
            navigator,
            createSupport(sessionSortStates, errorHandler),
            errorHandler,
            confirmDialog,
            uiPreferenceApplier
        );
    }

    private SceneUiSupport createSupport(
        Map<String, TableSortState> sessionSortStates,
        Consumer<Throwable> errorHandler
    ) {
        return new DefaultSceneUiSupport(
            sessionSortStates,
            services.expenseService(),
            services.promotionService(),
            services.productService(),
            services.orderService(),
            services.receiptService(),
            services.toastService(),
            errorHandler
        );
    }
}
