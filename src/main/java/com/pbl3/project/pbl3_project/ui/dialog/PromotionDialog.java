package com.pbl3.project.pbl3_project.ui.dialog;

import com.pbl3.project.pbl3_project.entity.Product;
import com.pbl3.project.pbl3_project.entity.Promotion;
import com.pbl3.project.pbl3_project.entity.PromotionDiscountType;
import com.pbl3.project.pbl3_project.entity.PromotionScope;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.service.MoneySupport;
import com.pbl3.project.pbl3_project.service.ProductService;
import com.pbl3.project.pbl3_project.service.PromotionService;
import com.pbl3.project.pbl3_project.service.ToastService;
import com.pbl3.project.pbl3_project.service.ValidationException;
import com.pbl3.project.pbl3_project.ui.component.DialogFormFactory;
import com.pbl3.project.pbl3_project.ui.util.AsyncUiTask;
import com.pbl3.project.pbl3_project.ui.util.DialogSupport;
import com.pbl3.project.pbl3_project.ui.util.FxFormatters;
import com.pbl3.project.pbl3_project.ui.util.SearchableComboBoxSupport;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public final class PromotionDialog {

    private PromotionDialog() {
    }

    public record Context(
        ProductService productService,
        PromotionService promotionService,
        ToastService toastService,
        Consumer<Throwable> errorHandler,
        Consumer<DatePicker> datePickerCustomizer
    ) {
    }

    private record PromotionDialogData(List<Product> products) {
    }

    public static void show(Stage owner, User user, Promotion promotion, Runnable onSave, Context context) {
        boolean editing = promotion != null && promotion.getId() != null;
        Stage dialog = DialogSupport.showLoadingWindow(
            owner,
            editing ? "Edit Promotion" : "New Promotion",
            "Loading promotion form...",
            420,
            240
        );

        javafx.concurrent.Task<PromotionDialogData> task = new javafx.concurrent.Task<>() {
            @Override
            protected PromotionDialogData call() {
                return new PromotionDialogData(loadPromotionProducts(context));
            }
        };
        task.setOnSucceeded(event -> {
            if (!dialog.isShowing()) {
                return;
            }
            populatePromotionDialog(dialog, user, promotion, onSave, context, task.getValue());
        });
        task.setOnFailed(event -> {
            dialog.close();
            handleError(context, task.getException());
        });
        Thread worker = new Thread(task, "promotion-dialog-products-loader");
        worker.setDaemon(true);
        worker.start();
    }

    private static void populatePromotionDialog(
        Stage dialog,
        User user,
        Promotion promotion,
        Runnable onSave,
        Context context,
        PromotionDialogData data
    ) {
        try {
            boolean editing = promotion != null && promotion.getId() != null;
            dialog.setTitle(editing ? "Edit Promotion" : "New Promotion");

            VBox root = new VBox(16);
            root.getStyleClass().addAll("dialog-root", "promotion-dialog-root");
            root.setPadding(new Insets(20));

            Label titleLabel = new Label(editing ? "Edit Promotion" : "Create New Promotion");
            titleLabel.getStyleClass().add("dialog-title");

            TextField nameField = DialogFormFactory.textField(
                promotion != null && promotion.getName() != null ? promotion.getName() : "",
                "Promotion Name"
            );
            nameField.getStyleClass().add("promotion-dialog-input");

            ComboBox<PromotionScope> scopeCombo = new ComboBox<>();
            scopeCombo.getItems().addAll(PromotionScope.values());
            scopeCombo.setValue(promotion != null && promotion.getScope() != null ? promotion.getScope() : PromotionScope.PRODUCT);
            scopeCombo.setMaxWidth(Double.MAX_VALUE);
            scopeCombo.getStyleClass().addAll("product-dialog-combo-box", "promotion-dialog-combo-box");
            installEnumConverter(scopeCombo);
            installPromotionPopupCells(scopeCombo);

            ComboBox<PromotionDiscountType> discountTypeCombo = new ComboBox<>();
            discountTypeCombo.getItems().addAll(PromotionDiscountType.values());
            discountTypeCombo.setValue(
                promotion != null && promotion.getDiscountType() != null
                    ? promotion.getDiscountType()
                    : PromotionDiscountType.PERCENT
            );
            discountTypeCombo.setMaxWidth(Double.MAX_VALUE);
            discountTypeCombo.getStyleClass().addAll("product-dialog-combo-box", "promotion-dialog-combo-box");
            installEnumConverter(discountTypeCombo);
            installPromotionPopupCells(discountTypeCombo);

            TextField discountValueField = new TextField(
                promotion != null && promotion.getDiscountValue() != null ? promotion.getDiscountValue().toPlainString() : ""
            );
            discountValueField.setPromptText("0.00");
            discountValueField.getStyleClass().add("promotion-dialog-input");
            discountValueField.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d{0,12}(\\.\\d{0,2})?") ? change : null
            ));

            CheckBox enabledCb = new CheckBox("Enabled");
            enabledCb.setSelected(promotion == null || promotion.isEnabled());
            enabledCb.getStyleClass().add("promotion-dialog-checkbox");

            DatePicker startsAtPicker = new DatePicker(
                promotion != null && promotion.getStartsAt() != null ? promotion.getStartsAt().toLocalDate() : null
            );
            startsAtPicker.setPromptText("Start Date");
            startsAtPicker.getStyleClass().add("promotion-dialog-date-picker");
            applyDatePickerCustomizer(startsAtPicker, context);

            DatePicker endsAtPicker = new DatePicker(
                promotion != null && promotion.getEndsAt() != null ? promotion.getEndsAt().toLocalDate() : null
            );
            endsAtPicker.setPromptText("End Date");
            endsAtPicker.getStyleClass().add("promotion-dialog-date-picker");
            applyDatePickerCustomizer(endsAtPicker, context);

            ComboBox<Product> targetProductCombo = new ComboBox<>();
            targetProductCombo.setMaxWidth(Double.MAX_VALUE);
            targetProductCombo.setPromptText("Search product");
            targetProductCombo.getStyleClass().addAll("product-dialog-combo-box", "promotion-dialog-combo-box");
            SearchableComboBoxSupport.install(
                targetProductCombo,
                data.products(),
                PromotionDialog::productDisplayText,
                PromotionDialog::productSearchText
            );
            targetProductCombo.getEditor().setPromptText("Search SKU, barcode or product");
            targetProductCombo.setValue(promotion != null ? promotion.getTargetProduct() : null);
            installPromotionPopupCells(targetProductCombo);

            TextField minOrderTotalField = new TextField(
                promotion != null && promotion.getMinOrderTotal() != null ? promotion.getMinOrderTotal().toPlainString() : ""
            );
            minOrderTotalField.setPromptText("Total");
            minOrderTotalField.getStyleClass().add("promotion-dialog-input");
            minOrderTotalField.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d{0,12}(\\.\\d{0,2})?") ? change : null
            ));

            HBox scheduleBox = new HBox(10, startsAtPicker, endsAtPicker);
            scheduleBox.setFillHeight(true);
            HBox.setHgrow(startsAtPicker, Priority.ALWAYS);
            HBox.setHgrow(endsAtPicker, Priority.ALWAYS);
            startsAtPicker.setMaxWidth(Double.MAX_VALUE);
            endsAtPicker.setMaxWidth(Double.MAX_VALUE);

            GridPane basicsGrid = DialogFormFactory.grid();
            basicsGrid.add(DialogFormFactory.fieldBlock("Name *", nameField, null), 0, 0);
            basicsGrid.add(DialogFormFactory.fieldBlock("Scope *", scopeCombo, null), 1, 0);

            GridPane discountGrid = DialogFormFactory.grid();
            discountGrid.add(DialogFormFactory.fieldBlock("Discount Type *", discountTypeCombo, null), 0, 0);
            discountGrid.add(DialogFormFactory.fieldBlock("Discount Value *", discountValueField, null), 1, 0);
            discountGrid.add(DialogFormFactory.fieldBlock("Schedule", scheduleBox, null), 0, 1, 2, 1);

            VBox targetProductBlock = DialogFormFactory.fieldBlock("Target Product *", targetProductCombo, null);
            VBox minOrderTotalBlock = DialogFormFactory.fieldBlock("Min Order Total", minOrderTotalField, null);
            VBox stateBlock = DialogFormFactory.fieldBlock("State", enabledCb, null);
            GridPane eligibilityGrid = DialogFormFactory.grid();
            eligibilityGrid.add(targetProductBlock, 0, 0);
            eligibilityGrid.add(minOrderTotalBlock, 0, 0);
            eligibilityGrid.add(stateBlock, 1, 0);

            VBox form = new VBox(14,
                DialogFormFactory.section("Basics", basicsGrid),
                DialogFormFactory.section("Discount & Schedule", discountGrid),
                DialogFormFactory.section("Eligibility", eligibilityGrid)
            );
            form.setFillWidth(true);

            ScrollPane formScroll = new ScrollPane(form);
            formScroll.setFitToWidth(true);
            formScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            formScroll.getStyleClass().add("promotion-dialog-scroll");
            VBox.setVgrow(formScroll, Priority.ALWAYS);

            Runnable updateScopeVisibility = () -> {
                boolean productScope = scopeCombo.getValue() == PromotionScope.PRODUCT;
                targetProductBlock.setManaged(productScope);
                targetProductBlock.setVisible(productScope);
                minOrderTotalBlock.setManaged(!productScope);
                minOrderTotalBlock.setVisible(!productScope);
            };
            scopeCombo.valueProperty().addListener((obs, oldValue, newValue) -> updateScopeVisibility.run());
            updateScopeVisibility.run();

            Button cancelBtn = new Button("Cancel");
            cancelBtn.getStyleClass().addAll(
                "button",
                "dashboard-report-secondary-button",
                "promotion-dialog-secondary-button",
                "dialog-cancel-button"
            );
            cancelBtn.setOnAction(e -> dialog.close());

            Button saveBtn = new Button(editing ? "Save" : "Create");
            saveBtn.getStyleClass().addAll("button", "primary-button", "promotion-dialog-primary-button");
            saveBtn.setDefaultButton(true);
            saveBtn.setOnAction(e -> savePromotion(
                dialog,
                saveBtn,
                cancelBtn,
                user,
                promotion,
                editing,
                nameField,
                scopeCombo,
                discountTypeCombo,
                discountValueField,
                enabledCb,
                startsAtPicker,
                endsAtPicker,
                targetProductCombo,
                data.products(),
                minOrderTotalField,
                onSave,
                context
            ));

            HBox actionRow = new HBox(10, cancelBtn, saveBtn);
            actionRow.setAlignment(Pos.CENTER_RIGHT);

            root.getChildren().addAll(titleLabel, formScroll, actionRow);

            Scene scene = new Scene(root, 480, 675);
            applyApplicationStyles(scene);
            dialog.setScene(scene);
            DialogSupport.preventInitialFieldFocus(dialog, root);
            DialogSupport.centerWindowOnOwner(dialog);
            if (!dialog.isShowing()) {
                dialog.show();
            }
        } catch (Exception ex) {
            dialog.close();
            handleError(context, ex);
        }
    }

    private static void savePromotion(
        Stage dialog,
        Button saveButton,
        Button cancelButton,
        User user,
        Promotion promotion,
        boolean editing,
        TextField nameField,
        ComboBox<PromotionScope> scopeCombo,
        ComboBox<PromotionDiscountType> discountTypeCombo,
        TextField discountValueField,
        CheckBox enabledCb,
        DatePicker startsAtPicker,
        DatePicker endsAtPicker,
        ComboBox<Product> targetProductCombo,
        List<Product> products,
        TextField minOrderTotalField,
        Runnable onSave,
        Context context
    ) {
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
            Product targetProduct = SearchableComboBoxSupport.resolveValue(
                targetProductCombo,
                products,
                PromotionDialog::productDisplayText,
                PromotionDialog::productSearchText
            );
            if (targetProduct != null) {
                targetProductCombo.setValue(targetProduct);
            }
            Long targetProductId = targetProduct != null ? targetProduct.getId() : null;
            String name = nameField.getText();
            PromotionScope scope = scopeCombo.getValue();
            PromotionDiscountType discountType = discountTypeCombo.getValue();
            boolean enabled = enabledCb.isSelected();

            BigDecimal parsedDiscountValue = discountValue;
            BigDecimal parsedMinOrderTotal = minOrderTotal;
            AsyncUiTask.runButtonTask(
                saveButton,
                cancelButton,
                editing ? "Saving..." : "Creating...",
                () -> {
                    if (editing) {
                        return context.promotionService().updatePromotion(
                            user,
                            promotion.getId(),
                            name,
                            scope,
                            discountType,
                            parsedDiscountValue,
                            enabled,
                            startsAt,
                            endsAt,
                            targetProductId,
                            parsedMinOrderTotal
                        );
                    }
                    return context.promotionService().createPromotion(
                        user,
                        name,
                        scope,
                        discountType,
                        parsedDiscountValue,
                        enabled,
                        startsAt,
                        endsAt,
                        targetProductId,
                        parsedMinOrderTotal
                    );
                },
                saved -> {
                    context.toastService().showSuccess(editing ? "Promotion updated." : "Promotion created.");
                    if (onSave != null) {
                        onSave.run();
                    }
                    dialog.close();
                },
                ex -> handleError(context, ex),
                "promotion-save"
            );
        } catch (Exception ex) {
            handleError(context, ex);
        }
    }

    private static List<Product> loadPromotionProducts(Context context) {
        return context.productService().getAllProducts().stream()
            .filter(product -> product != null && !product.isDeleted())
            .sorted(Comparator.comparing(Product::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
            .toList();
    }

    private static void applyDatePickerCustomizer(DatePicker datePicker, Context context) {
        if (context.datePickerCustomizer() != null) {
            context.datePickerCustomizer().accept(datePicker);
        }
    }

    private static <T extends Enum<T>> void installEnumConverter(ComboBox<T> comboBox) {
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(T value) {
                return FxFormatters.enumText(value);
            }

            @Override
            public T fromString(String string) {
                return null;
            }
        });
    }

    private static StringConverter<Product> productConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Product value) {
                return productDisplayText(value);
            }

            @Override
            public Product fromString(String string) {
                return null;
            }
        };
    }

    private static String productDisplayText(Product product) {
        if (product == null) {
            return "";
        }
        return product.getName() + (product.getSku() != null && !product.getSku().isBlank() ? " • " + product.getSku() : "");
    }

    private static String productSearchText(Product product) {
        if (product == null) {
            return "";
        }
        return String.join(
            " ",
            nullToBlank(product.getName()),
            nullToBlank(product.getSku()),
            nullToBlank(product.getBarcode())
        );
    }

    private static String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private static <T> void installPromotionPopupCells(ComboBox<T> comboBox) {
        comboBox.setCellFactory(listView -> new ListCell<>() {
            {
                getStyleClass().add("promotion-dialog-popup-cell");
            }

            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null
                    ? null
                    : comboBox.getConverter() != null ? comboBox.getConverter().toString(item) : String.valueOf(item));
            }
        });
    }

    private static BigDecimal parseMoneyInput(String raw, String fieldLabel) {
        if (raw == null || raw.trim().isBlank()) {
            throw new ValidationException(java.text.MessageFormat.format("{0} is required", fieldLabel));
        }
        String normalized = raw.trim().replace(",", "");
        try {
            return MoneySupport.normalize(new BigDecimal(normalized));
        } catch (NumberFormatException ex) {
            throw new ValidationException(java.text.MessageFormat.format("{0} must be a valid number", fieldLabel));
        }
    }

    private static void applyApplicationStyles(Scene scene) {
        String stylesheet = PromotionDialog.class.getResource("/application.css").toExternalForm();
        if (!scene.getStylesheets().contains(stylesheet)) {
            scene.getStylesheets().add(stylesheet);
        }
    }

    private static void handleError(Context context, Throwable throwable) {
        if (context.errorHandler() != null) {
            context.errorHandler().accept(throwable);
        } else {
            context.toastService().showError(throwable.getMessage() != null ? throwable.getMessage() : "Operation failed");
        }
    }
}
