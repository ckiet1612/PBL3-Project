package com.pbl3.project.pbl3_project.ui.scene;

import com.pbl3.project.pbl3_project.repository.CategoryRepository;
import com.pbl3.project.pbl3_project.service.AccountAuditLogService;
import com.pbl3.project.pbl3_project.service.AuthService;
import com.pbl3.project.pbl3_project.service.AuthorizationService;
import com.pbl3.project.pbl3_project.service.BrandService;
import com.pbl3.project.pbl3_project.service.CategoryService;
import com.pbl3.project.pbl3_project.service.CustomerService;
import com.pbl3.project.pbl3_project.service.DataBackupService;
import com.pbl3.project.pbl3_project.service.ExpenseService;
import com.pbl3.project.pbl3_project.service.ImportOrderService;
import com.pbl3.project.pbl3_project.service.InventoryTransactionService;
import com.pbl3.project.pbl3_project.service.NotificationService;
import com.pbl3.project.pbl3_project.service.OrderService;
import com.pbl3.project.pbl3_project.service.OperationalAuditLogService;
import com.pbl3.project.pbl3_project.service.OriginService;
import com.pbl3.project.pbl3_project.service.ProductService;
import com.pbl3.project.pbl3_project.service.PromotionService;
import com.pbl3.project.pbl3_project.service.QrPaymentService;
import com.pbl3.project.pbl3_project.service.RealtimeDataSyncService;
import com.pbl3.project.pbl3_project.service.ReceiptService;
import com.pbl3.project.pbl3_project.service.ReportService;
import com.pbl3.project.pbl3_project.service.SalesShiftService;
import com.pbl3.project.pbl3_project.service.SePaySettingsService;
import com.pbl3.project.pbl3_project.service.StocktakeService;
import com.pbl3.project.pbl3_project.service.SupplierService;
import com.pbl3.project.pbl3_project.service.ToastService;
import com.pbl3.project.pbl3_project.service.UnitService;
import com.pbl3.project.pbl3_project.service.UserAccountService;
import com.pbl3.project.pbl3_project.service.UserUiPreferencesService;
import java.util.function.BiFunction;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javafx.stage.Stage;

public record SceneRuntimeContext(
    Stage owner,
    AuthService authService,
    ProductService productService,
    CategoryRepository categoryRepository,
    OrderService orderService,
    ReportService reportService,
    SalesShiftService salesShiftService,
    NotificationService notificationService,
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
    SePaySettingsService sePaySettingsService,
    QrPaymentService qrPaymentService,
    RealtimeDataSyncService realtimeDataSyncService,
    ReceiptService receiptService,
    UserUiPreferencesService userUiPreferencesService,
    InventoryTransactionService transactionService,
    StocktakeService stocktakeService,
    DataBackupService dataBackupService,
    SceneNavigation navigator,
    SceneUiSupport support,
    Consumer<Throwable> errorHandler,
    BiFunction<String, String, Boolean> confirmDialog,
    BiConsumer<com.pbl3.project.pbl3_project.entity.User, Boolean> uiPreferenceApplier
) {
    public void showUserFacingError(Throwable throwable) {
        if (errorHandler != null) {
            errorHandler.accept(throwable);
        }
    }

    public boolean confirm(String title, String message) {
        return confirmDialog != null && Boolean.TRUE.equals(confirmDialog.apply(title, message));
    }

    public void applyUiPreferences(com.pbl3.project.pbl3_project.entity.User user, boolean applySidebarPreference) {
        if (uiPreferenceApplier != null) {
            uiPreferenceApplier.accept(user, applySidebarPreference);
        }
    }
}
