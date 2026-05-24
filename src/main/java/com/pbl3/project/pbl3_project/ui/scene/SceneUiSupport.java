package com.pbl3.project.pbl3_project.ui.scene;

import com.pbl3.project.pbl3_project.entity.Expense;
import com.pbl3.project.pbl3_project.entity.ExpenseCategory;
import com.pbl3.project.pbl3_project.entity.Order;
import com.pbl3.project.pbl3_project.entity.OrderStatus;
import com.pbl3.project.pbl3_project.entity.PaymentMethod;
import com.pbl3.project.pbl3_project.entity.Promotion;
import com.pbl3.project.pbl3_project.entity.PromotionDiscountType;
import com.pbl3.project.pbl3_project.entity.PromotionLifecycleStatus;
import com.pbl3.project.pbl3_project.entity.PromotionScope;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.ui.util.SortCriterion;
import com.pbl3.project.pbl3_project.ui.util.TableSortState;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import org.springframework.data.domain.Pageable;

public interface SceneUiSupport {
    TableSortState getOrCreateTableSortState(String stateKey, SortCriterion... defaultCriteria);

    void applyStandardTableSizing(TableView<?> table);

    void applyStandardTablePageLayout(VBox root);

    void applyStandardTablePageLayout(VBox root, Insets padding);

    void applyStandardTableStatusBar(HBox statusBar);

    Label createStatusMetaLabel(String text);

    void updatePagedStatus(
        TableView<?> table,
        Label rowCountLabel,
        Label pageLabel,
        Button prevButton,
        Button nextButton,
        long totalElements,
        int currentPage,
        int totalPages,
        int pageSize
    );

    Pageable createPageable(TableSortState sortState, Map<String, String> propertyByUiKey, int page, int size);

    double getTableVerticalScrollValue(TableView<?> table);

    <T> void restoreTableSelectionById(TableView<T> table, Long id, Function<T, Long> idExtractor);

    void restoreTableVerticalScrollValue(TableView<?> table, double value);

    <T> void installSortHeaderIndicators(LinkedHashMap<String, TableColumn<T, ?>> columnsByKey);

    <T> void applySortStateToTable(TableView<T> table, LinkedHashMap<String, TableColumn<T, ?>> columnsByKey, TableSortState sortState);

    String buildSortStatusText(TableSortState sortState, Map<String, String> labelsByKey);

    Label createSortStatusLabel(TableSortState sortState, Map<String, String> labelsByKey);

    <T> void installManualServerSorting(
        TableView<T> table,
        LinkedHashMap<String, TableColumn<T, ?>> columnsByKey,
        TableSortState sortState,
        Runnable onSortChanged
    );

    void customizeDatePicker(DatePicker datePicker);

    void showPopupBelow(Popup popup, Node owner, double xOffset, double yOffset);

    Node createSlidingMenu(String[] tabNames, Consumer<Integer> onSelect);

    boolean showConfirmDialog(String title, String content);

    void showExpenseDialog(Stage owner, User user, Expense expense, Runnable onSuccess);

    void showPromotionDialog(Stage owner, User user, Promotion promotion, Runnable onSuccess);

    String formatExpenseCategoryLabel(ExpenseCategory category);

    String formatPaymentMethodLabel(PaymentMethod method);

    String formatUserDisplayName(User user);

    String formatDate(LocalDate date);

    String formatDateTime(LocalDateTime dateTime);

    String formatOrderStatus(OrderStatus status);

    String formatVnd(BigDecimal amount);

    void showOrderDetailsDialog(Stage owner, Order order, User user, Runnable onChanged);

    String formatPromotionScopeLabel(PromotionScope scope);

    String formatPromotionDiscountTypeLabel(PromotionDiscountType discountType);

    String formatPromotionLifecycleStatusLabel(PromotionLifecycleStatus status);

    String formatPromotionTargetLabel(Promotion promotion);

    String formatPromotionScheduleLabel(Promotion promotion);

    String formatPromotionOwnerLabel(Promotion promotion);

    String formatPromotionDiscountLabel(Promotion promotion);
}
