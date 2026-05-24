package com.pbl3.project.pbl3_project.ui.dialog;

import com.pbl3.project.pbl3_project.entity.Brand;
import com.pbl3.project.pbl3_project.entity.Category;
import com.pbl3.project.pbl3_project.entity.Origin;
import com.pbl3.project.pbl3_project.entity.Product;
import com.pbl3.project.pbl3_project.entity.Unit;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.repository.CategoryRepository;
import com.pbl3.project.pbl3_project.service.BrandService;
import com.pbl3.project.pbl3_project.service.MoneySupport;
import com.pbl3.project.pbl3_project.service.OriginService;
import com.pbl3.project.pbl3_project.service.ProductService;
import com.pbl3.project.pbl3_project.service.ToastService;
import com.pbl3.project.pbl3_project.service.UnitService;
import com.pbl3.project.pbl3_project.ui.component.DialogFormFactory;
import com.pbl3.project.pbl3_project.ui.util.AsyncUiTask;
import com.pbl3.project.pbl3_project.ui.util.DialogSupport;
import com.pbl3.project.pbl3_project.ui.util.ImeInputSupport;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

public final class ProductDialog {

    private ProductDialog() {
    }

    public record Context(
        CategoryRepository categoryRepository,
        BrandService brandService,
        OriginService originService,
        UnitService unitService,
        ProductService productService,
        ToastService toastService
    ) {
    }

    private record ProductDialogData(
        List<Category> categories,
        List<Brand> brands,
        List<Origin> origins,
        List<Unit> units
    ) {
    }

    public static void show(Stage owner, Product product, Category contextCategory, User user, Runnable onSave, Context context) {
        Stage dialog = DialogSupport.showLoadingWindow(
            owner,
            product == null ? "Add New Product" : "Edit Product",
            "Loading product form...",
            420,
            240
        );

        javafx.concurrent.Task<ProductDialogData> task = new javafx.concurrent.Task<>() {
            @Override
            protected ProductDialogData call() {
                return new ProductDialogData(
                    context.categoryRepository().findAll(),
                    context.brandService().getAllBrands(),
                    context.originService().getAllOrigins(),
                    context.unitService().getAllUnits()
                );
            }
        };
        task.setOnSucceeded(event -> {
            if (!dialog.isShowing()) {
                return;
            }
            populateProductDialog(owner, dialog, product, contextCategory, user, onSave, context, task.getValue());
        });
        task.setOnFailed(event -> {
            dialog.close();
            Throwable ex = task.getException();
            context.toastService().showError(MessageFormat.format(
                "Could not open dialog: {0}",
                ex != null && ex.getMessage() != null ? ex.getMessage() : "Unknown error"
            ));
        });
        Thread worker = new Thread(task, "product-dialog-options-loader");
        worker.setDaemon(true);
        worker.start();
    }

    private static void populateProductDialog(
        Stage owner,
        Stage dialog,
        Product product,
        Category contextCategory,
        User user,
        Runnable onSave,
        Context context,
        ProductDialogData data
    ) {
        try {
            dialog.setTitle(product == null ? "Add New Product" : "Edit Product");

            VBox root = new VBox(16);
            root.getStyleClass().addAll("dialog-root", "product-dialog-root");
            root.setPadding(new Insets(24));

            boolean categoryLockedByScope = product == null && contextCategory != null;
            VBox header = DialogFormFactory.header(
                product == null ? "Add New Product" : "Edit Product",
                categoryLockedByScope && contextCategory != null
                    ? MessageFormat.format("Category preset: {0}", contextCategory.getName())
                    : null
            );

            TextField nameField = DialogFormFactory.textField(product != null ? product.getName() : "", "Product Name");
            TextField skuField = DialogFormFactory.textField(product != null ? product.getSku() : "", "SKU");
            TextField barcodeField = DialogFormFactory.textField(product != null ? product.getBarcode() : "", "Barcode");

            List<Category> categoryOptions = data.categories();
            ComboBox<Category> categoryCombo = new ComboBox<>();
            categoryCombo.setMaxWidth(Double.MAX_VALUE);
            categoryCombo.setPromptText("Category");
            installSearchableComboBox(categoryCombo, categoryOptions);
            if (product != null && product.getCategory() != null) {
                categoryCombo.setValue(product.getCategory());
            } else if (contextCategory != null) {
                categoryCombo.setValue(contextCategory);
            }

            List<Brand> brandOptions = data.brands();
            ComboBox<Brand> brandCombo = new ComboBox<>();
            brandCombo.setMaxWidth(Double.MAX_VALUE);
            brandCombo.setPromptText("Brand");
            installSearchableComboBox(brandCombo, brandOptions);
            if (product != null) {
                brandCombo.setValue(product.getBrand());
            }

            List<Origin> originOptions = data.origins();
            ComboBox<Origin> originCombo = new ComboBox<>();
            originCombo.setMaxWidth(Double.MAX_VALUE);
            originCombo.setPromptText("Origin");
            installSearchableComboBox(originCombo, originOptions);
            if (product != null) {
                originCombo.setValue(product.getOrigin());
            }

            List<Unit> unitOptions = data.units();
            ComboBox<Unit> unitCombo = new ComboBox<>();
            unitCombo.setMaxWidth(Double.MAX_VALUE);
            unitCombo.setPromptText("Unit");
            installSearchableComboBox(unitCombo, unitOptions);
            if (product != null) {
                unitCombo.setValue(product.getUnit());
            }

            TextField importPriceField = DialogFormFactory.textField(
                product != null ? formatWholeNumberText(product.getImportPrice()) : "",
                "Import Price"
            );
            TextField priceField = DialogFormFactory.textField(
                product != null ? formatWholeNumberText(product.getPrice()) : "",
                "Selling Price"
            );
            TextField qtyField = DialogFormFactory.textField(
                product != null ? String.valueOf(safeProductInt(product.getQuantity())) : "0",
                "Quantity"
            );
            TextField minStockField = DialogFormFactory.textField(
                product != null && product.getMinStockLevel() != null ? String.valueOf(product.getMinStockLevel()) : "10",
                "Min Stock Level"
            );
            TextArea descArea = DialogFormFactory.textArea(product != null ? product.getDescription() : "", "Description", 3);
            List.of(nameField, skuField, barcodeField, importPriceField, priceField, qtyField, minStockField)
                .forEach(field -> field.getStyleClass().add("product-dialog-input"));
            descArea.getStyleClass().add("product-dialog-text-area");

            if (product != null) {
                importPriceField.setDisable(true);
            }

            Label nameError = DialogFormFactory.errorLabel();
            Label categoryError = DialogFormFactory.errorLabel();
            Label priceError = DialogFormFactory.errorLabel();
            Label importPriceError = DialogFormFactory.errorLabel();
            Label qtyError = DialogFormFactory.errorLabel();
            Label minStockError = DialogFormFactory.errorLabel();

            GridPane identityGrid = DialogFormFactory.grid();
            identityGrid.add(DialogFormFactory.fieldBlock("Product Name *", nameField, nameError), 0, 0, categoryLockedByScope ? 2 : 1, 1);
            if (!categoryLockedByScope) {
                identityGrid.add(DialogFormFactory.fieldBlock("Category *", categoryCombo, categoryError), 1, 0);
            }
            identityGrid.add(DialogFormFactory.fieldBlock("SKU", skuField, null), 0, 1);
            identityGrid.add(DialogFormFactory.fieldBlock("Barcode", barcodeField, null), 1, 1);
            identityGrid.add(DialogFormFactory.fieldBlock("Brand", brandCombo, null), 0, 2);
            identityGrid.add(DialogFormFactory.fieldBlock("Origin", originCombo, null), 1, 2);
            identityGrid.add(DialogFormFactory.fieldBlock("Unit", unitCombo, null), 0, 3);
            identityGrid.add(DialogFormFactory.fieldBlock("Description", descArea, null), 0, 4, 2, 1);

            GridPane pricingGrid = DialogFormFactory.grid();
            pricingGrid.add(DialogFormFactory.fieldBlock("Selling Price *", priceField, priceError), 0, 0);
            pricingGrid.add(DialogFormFactory.fieldBlock("Import Price", importPriceField, importPriceError), 1, 0);

            GridPane inventoryGrid = DialogFormFactory.grid();
            inventoryGrid.add(DialogFormFactory.fieldBlock("Quantity *", qtyField, qtyError), 0, 0);
            inventoryGrid.add(DialogFormFactory.fieldBlock("Min Stock *", minStockField, minStockError), 1, 0);

            VBox form = new VBox(14,
                DialogFormFactory.section("Identity", identityGrid),
                DialogFormFactory.section("Pricing", pricingGrid),
                DialogFormFactory.section("Inventory", inventoryGrid)
            );
            form.setFillWidth(true);

            javafx.scene.control.ScrollPane formScroll = new javafx.scene.control.ScrollPane(form);
            formScroll.setFitToWidth(true);
            formScroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
            formScroll.getStyleClass().add("product-dialog-scroll");
            VBox.setVgrow(formScroll, Priority.ALWAYS);

            Button saveButton = new Button(product == null ? "Create Product" : "Save Product");
            saveButton.getStyleClass().addAll("button", "primary-button", "product-dialog-primary-button");
            Button cancelButton = new Button("Cancel");
            cancelButton.getStyleClass().addAll("button", "product-dialog-secondary-button", "dialog-cancel-button");
            cancelButton.setOnAction(e -> dialog.close());
            saveButton.setOnAction(e -> saveProduct(
                owner,
                dialog,
                saveButton,
                cancelButton,
                product,
                user,
                onSave,
                context,
                categoryLockedByScope,
                contextCategory,
                categoryOptions,
                brandOptions,
                originOptions,
                unitOptions,
                nameField,
                skuField,
                barcodeField,
                categoryCombo,
                brandCombo,
                originCombo,
                unitCombo,
                importPriceField,
                priceField,
                qtyField,
                minStockField,
                descArea,
                nameError,
                categoryError,
                priceError,
                importPriceError,
                qtyError,
                minStockError
            ));

            HBox footer = new HBox(10, cancelButton, saveButton);
            footer.getStyleClass().add("product-dialog-footer");
            footer.setAlignment(Pos.CENTER_RIGHT);

            root.getChildren().addAll(header, formScroll, footer);
            Scene scene = new Scene(root, 760, 720);
            applyApplicationStyles(scene);
            dialog.setScene(scene);
            DialogSupport.preventInitialFieldFocus(dialog, root);
            dialog.setMinWidth(720);
            dialog.setMinHeight(680);
            DialogSupport.centerWindowOnOwner(dialog);
            if (!dialog.isShowing()) {
                dialog.show();
            }
        } catch (Exception e) {
            dialog.close();
            context.toastService().showError(MessageFormat.format("Could not open dialog: {0}", e.getMessage()));
        }
    }

    private static void saveProduct(
        Stage owner,
        Stage dialog,
        Button saveButton,
        Button cancelButton,
        Product product,
        User user,
        Runnable onSave,
        Context context,
        boolean categoryLockedByScope,
        Category contextCategory,
        List<Category> categoryOptions,
        List<Brand> brandOptions,
        List<Origin> originOptions,
        List<Unit> unitOptions,
        TextField nameField,
        TextField skuField,
        TextField barcodeField,
        ComboBox<Category> categoryCombo,
        ComboBox<Brand> brandCombo,
        ComboBox<Origin> originCombo,
        ComboBox<Unit> unitCombo,
        TextField importPriceField,
        TextField priceField,
        TextField qtyField,
        TextField minStockField,
        TextArea descArea,
        Label nameError,
        Label categoryError,
        Label priceError,
        Label importPriceError,
        Label qtyError,
        Label minStockError
    ) {
        clearErrors(nameError, categoryError, priceError, importPriceError, qtyError, minStockError);

        boolean valid = true;
        String productName = nameField.getText() == null ? "" : nameField.getText().trim();
        if (productName.isEmpty()) {
            DialogFormFactory.setError(nameError, "Product name is required");
            valid = false;
        }
        Category resolvedCategory = categoryLockedByScope ? contextCategory : resolveSearchableComboValue(categoryCombo, categoryOptions);
        if (resolvedCategory == null) {
            DialogFormFactory.setError(categoryError, "Category is required");
            valid = false;
        }

        BigDecimal sellingPrice = BigDecimal.ZERO;
        if (priceField.getText() == null || priceField.getText().isBlank()) {
            DialogFormFactory.setError(priceError, "Selling price is required");
            valid = false;
        } else {
            try {
                sellingPrice = parseMoneyInput(priceField.getText());
                if (sellingPrice.compareTo(BigDecimal.ZERO) < 0) {
                    DialogFormFactory.setError(priceError, "Selling price cannot be negative");
                    valid = false;
                }
            } catch (Exception ex) {
                DialogFormFactory.setError(priceError, "Invalid money value");
                valid = false;
            }
        }

        BigDecimal importPrice = product != null ? product.getImportPrice() : null;
        if (importPriceField.getText() != null && !importPriceField.getText().isBlank()) {
            try {
                importPrice = parseMoneyInput(importPriceField.getText());
                if (importPrice.compareTo(BigDecimal.ZERO) < 0) {
                    DialogFormFactory.setError(importPriceError, "Import price cannot be negative");
                    valid = false;
                }
            } catch (Exception ex) {
                DialogFormFactory.setError(importPriceError, "Invalid money value");
                valid = false;
            }
        }

        int newQty = 0;
        try {
            newQty = Integer.parseInt(qtyField.getText() == null ? "" : qtyField.getText().trim());
            if (newQty < 0) {
                DialogFormFactory.setError(qtyError, "Quantity cannot be negative");
                valid = false;
            }
        } catch (NumberFormatException ex) {
            DialogFormFactory.setError(qtyError, "Quantity must be a whole number");
            valid = false;
        }

        int minStock = 10;
        try {
            minStock = Integer.parseInt(minStockField.getText() == null ? "" : minStockField.getText().trim());
            if (minStock < 0) {
                DialogFormFactory.setError(minStockError, "Min stock cannot be negative");
                valid = false;
            }
        } catch (NumberFormatException ex) {
            DialogFormFactory.setError(minStockError, "Min stock must be a whole number");
            valid = false;
        }

        if (!valid) {
            context.toastService().showError("Please fix invalid product fields.");
            return;
        }

        try {
            String reason = "Manual Add/Edit via UI";
            if (product != null && safeProductInt(product.getQuantity()) != newQty) {
                Optional<String> result = askStockEditReason(owner, product, newQty);
                if (result.isPresent() && !result.get().trim().isEmpty()) {
                    reason = result.get().trim();
                } else {
                    context.toastService().showError("Stock adjustment reason is required.");
                    return;
                }
            }

            Product target = product != null ? product : new Product();
            target.setName(productName);
            target.setSku(blankToNull(skuField.getText()));
            target.setBarcode(blankToNull(barcodeField.getText()));
            target.setDescription(blankToNull(descArea.getText()));
            target.setCategory(resolvedCategory);
            target.setBrand(resolveSearchableComboValue(brandCombo, brandOptions));
            target.setOrigin(resolveSearchableComboValue(originCombo, originOptions));
            target.setUnit(resolveSearchableComboValue(unitCombo, unitOptions));
            target.setPrice(sellingPrice);
            target.setImportPrice(importPrice);
            target.setQuantity(newQty);
            target.setMinStockLevel(minStock);
            final String saveReason = reason;

            AsyncUiTask.runButtonTask(
                saveButton,
                cancelButton,
                "Saving...",
                () -> context.productService().saveProduct(target, user, saveReason),
                saved -> {
                    context.toastService().showSuccess("Product saved.");
                    onSave.run();
                    dialog.close();
                },
                ex -> context.toastService().showError(MessageFormat.format("Save failed: {0}", ex.getMessage())),
                "product-save"
            );
        } catch (Exception ex) {
            context.toastService().showError(MessageFormat.format("Save failed: {0}", ex.getMessage()));
        }
    }

    private static Optional<String> askStockEditReason(Stage owner, Product product, int newQty) {
        TextInputDialog reasonDialog = new TextInputDialog();
        reasonDialog.initOwner(owner);
        reasonDialog.setTitle("Stock Edit Reason");
        reasonDialog.setHeaderText(MessageFormat.format("Quantity changed: {0} -> {1}", safeProductInt(product.getQuantity()), newQty));
        reasonDialog.setContentText("Please enter a reason for audit log:");
        reasonDialog.getDialogPane().getStylesheets().add(ProductDialog.class.getResource("/application.css").toExternalForm());
        reasonDialog.getDialogPane().getStyleClass().add("dialog-root");
        return reasonDialog.showAndWait();
    }

    private static void clearErrors(Label... labels) {
        for (Label label : labels) {
            DialogFormFactory.setError(label, null);
        }
    }

    private static <T> void setComboConverter(ComboBox<T> comboBox, List<T> sourceItems) {
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(T object) {
                return getComboDisplayText(object);
            }

            @Override
            public T fromString(String string) {
                String normalizedText = string == null ? "" : string.trim();
                if (normalizedText.isEmpty()) {
                    return null;
                }
                T currentValue = comboBox.getValue();
                if (currentValue != null && sameComboText(normalizedText, getComboDisplayText(currentValue))) {
                    return currentValue;
                }
                List<T> safeItems = sourceItems == null ? List.of() : sourceItems;
                return safeItems.stream()
                    .filter(item -> sameComboText(normalizedText, getComboDisplayText(item)))
                    .findFirst()
                    .orElse(null);
            }
        });
    }

    private static <T> void installSearchableComboBox(ComboBox<T> comboBox, List<T> sourceItems) {
        List<T> safeItems = sourceItems == null ? List.of() : new ArrayList<>(sourceItems);
        ObservableList<T> observableItems = FXCollections.observableArrayList(safeItems);
        FilteredList<T> filteredItems = new FilteredList<>(observableItems, item -> true);
        comboBox.setItems(filteredItems);
        comboBox.setEditable(true);
        comboBox.setMinHeight(40);
        comboBox.setPrefHeight(40);
        comboBox.getEditor().setMinHeight(36);
        comboBox.getEditor().setPrefHeight(36);
        if (!comboBox.getStyleClass().contains("product-dialog-combo-box")) {
            comboBox.getStyleClass().add("product-dialog-combo-box");
        }
        setComboConverter(comboBox, safeItems);
        AtomicBoolean syncingSelection = new AtomicBoolean(false);
        AtomicBoolean imeComposing = ImeInputSupport.trackComposition(comboBox.getEditor());
        PauseTransition popupDelay = new PauseTransition(Duration.millis(90));
        popupDelay.setOnFinished(event -> {
            if (!imeComposing.get()
                && comboBox.getEditor() != null
                && comboBox.getEditor().isFocused()
                && !comboBox.isShowing()
                && !filteredItems.isEmpty()) {
                comboBox.show();
            }
        });

        Runnable syncVisibleRows = () -> {
            int visibleRows = Math.max(1, Math.min(8, filteredItems.size()));
            comboBox.setVisibleRowCount(visibleRows);
            javafx.application.Platform.runLater(() -> {
                if (comboBox.getSkin() instanceof ComboBoxListViewSkin<?> skin
                    && skin.getPopupContent() instanceof javafx.scene.control.ListView<?> popupList) {
                    double rowHeight = 36;
                    double popupPadding = 8;
                    double popupHeight = visibleRows * rowHeight + popupPadding;
                    popupList.setFixedCellSize(rowHeight);
                    popupList.setPrefHeight(popupHeight);
                    popupList.setMaxHeight(popupHeight);
                }
            });
        };
        filteredItems.addListener((ListChangeListener<T>) change -> syncVisibleRows.run());
        syncVisibleRows.run();

        comboBox.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (comboBox.getEditor() == null || !comboBox.getEditor().isFocused()) {
                return;
            }
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER && !imeComposing.get()) {
                completeSearchableComboCurrentText(comboBox, safeItems, syncingSelection);
                event.consume();
            }
        });

        comboBox.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (syncingSelection.get()) {
                return;
            }
            T currentValue = comboBox.getValue();
            if (currentValue != null && sameComboText(newText, getComboDisplayText(currentValue))) {
                filteredItems.setPredicate(item -> true);
                syncVisibleRows.run();
                return;
            }
            String query = newText == null ? "" : newText.trim().toLowerCase();
            filteredItems.setPredicate(item -> query.isEmpty() || getComboDisplayText(item).toLowerCase().contains(query));
            syncVisibleRows.run();
            if (!imeComposing.get() && comboBox.getEditor().isFocused() && !comboBox.isShowing()) {
                popupDelay.playFromStart();
            }
        });

        comboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            syncingSelection.set(true);
            javafx.application.Platform.runLater(() -> syncingSelection.set(false));
        });

        comboBox.setOnShowing(event -> {
            popupDelay.stop();
            T currentValue = comboBox.getValue();
            String editorText = comboBox.getEditor().getText();
            if (currentValue != null && sameComboText(editorText, getComboDisplayText(currentValue))) {
                filteredItems.setPredicate(item -> true);
                syncVisibleRows.run();
                return;
            }
            String query = comboBox.getEditor().getText() == null ? "" : comboBox.getEditor().getText().trim().toLowerCase();
            filteredItems.setPredicate(item -> query.isEmpty() || getComboDisplayText(item).toLowerCase().contains(query));
            syncVisibleRows.run();
        });
    }

    private static <T> void restoreSearchableComboSelection(
        ComboBox<T> comboBox,
        List<T> sourceItems,
        T fallbackValue,
        String fallbackText,
        AtomicBoolean syncingSelection
    ) {
        T currentValue = comboBox.getValue() != null ? comboBox.getValue() : fallbackValue;
        T restoredValue = resolveComboListItem(sourceItems, currentValue, fallbackText);
        if (restoredValue == null) {
            return;
        }

        String displayText = getComboDisplayText(restoredValue);
        syncingSelection.set(true);
        comboBox.setValue(restoredValue);
        comboBox.getSelectionModel().select(restoredValue);
        if (comboBox.isEditable() && comboBox.getEditor() != null && !sameComboText(comboBox.getEditor().getText(), displayText)) {
            comboBox.getEditor().setText(displayText);
            comboBox.getEditor().positionCaret(displayText.length());
        }
        javafx.application.Platform.runLater(() -> syncingSelection.set(false));
    }

    private static <T> void completeSearchableComboCurrentText(
        ComboBox<T> comboBox,
        List<T> sourceItems,
        AtomicBoolean syncingSelection
    ) {
        T resolvedValue = resolveSearchableComboValue(comboBox, sourceItems);
        if (resolvedValue == null) {
            return;
        }
        String displayText = getComboDisplayText(resolvedValue);
        syncingSelection.set(true);
        comboBox.setValue(resolvedValue);
        comboBox.getSelectionModel().select(resolvedValue);
        comboBox.getEditor().setText(displayText);
        comboBox.getEditor().positionCaret(displayText.length());
        comboBox.hide();
        javafx.application.Platform.runLater(() -> syncingSelection.set(false));
    }

    private static <T> T resolveComboListItem(List<T> sourceItems, T value, String fallbackText) {
        List<T> safeItems = sourceItems == null ? List.of() : sourceItems;
        String valueText = getComboDisplayText(value);
        String targetText = valueText == null || valueText.isBlank() ? fallbackText : valueText;
        if (targetText != null && !targetText.isBlank()) {
            Optional<T> matchingItem = safeItems.stream()
                .filter(item -> sameComboText(targetText, getComboDisplayText(item)))
                .findFirst();
            if (matchingItem.isPresent()) {
                return matchingItem.get();
            }
        }
        return value;
    }

    private static <T> T resolveSearchableComboValue(ComboBox<T> comboBox, List<T> sourceItems) {
        if (comboBox == null) {
            return null;
        }
        String editorText = comboBox.isEditable() && comboBox.getEditor() != null
            ? comboBox.getEditor().getText()
            : getComboDisplayText(comboBox.getValue());
        String normalizedText = editorText == null ? "" : editorText.trim();
        if (normalizedText.isEmpty()) {
            return null;
        }
        T currentValue = comboBox.getValue();
        if (currentValue != null && normalizedText.equals(getComboDisplayText(currentValue))) {
            return currentValue;
        }
        List<T> safeItems = sourceItems == null ? List.of() : sourceItems;
        Optional<T> exactMatch = safeItems.stream()
            .filter(item -> normalizedText.equalsIgnoreCase(getComboDisplayText(item)))
            .findFirst();
        if (exactMatch.isPresent()) {
            return exactMatch.get();
        }
        List<T> partialMatches = safeItems.stream()
            .filter(item -> getComboDisplayText(item).toLowerCase().contains(normalizedText.toLowerCase()))
            .toList();
        return partialMatches.size() == 1 ? partialMatches.get(0) : null;
    }

    private static boolean sameComboText(String first, String second) {
        String normalizedFirst = first == null ? "" : first.trim();
        String normalizedSecond = second == null ? "" : second.trim();
        return normalizedFirst.equalsIgnoreCase(normalizedSecond);
    }

    private static String getComboDisplayText(Object object) {
        if (object == null) {
            return "";
        }
        try {
            Object value = object.getClass().getMethod("getName").invoke(object);
            return value == null ? "" : String.valueOf(value);
        } catch (Exception e) {
            return object.toString();
        }
    }

    private static int safeProductInt(Integer value) {
        return value == null ? 0 : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String formatWholeNumberText(BigDecimal value) {
        return value == null ? "" : String.valueOf(MoneySupport.normalize(value).setScale(0, RoundingMode.HALF_UP).toBigIntegerExact());
    }

    private static BigDecimal parseMoneyInput(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        return MoneySupport.normalize(new BigDecimal(value.replace(",", "").trim()));
    }

    private static void applyApplicationStyles(Scene scene) {
        String stylesheet = ProductDialog.class.getResource("/application.css").toExternalForm();
        if (!scene.getStylesheets().contains(stylesheet)) {
            scene.getStylesheets().add(stylesheet);
        }
    }
}
