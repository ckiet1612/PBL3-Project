package com.pbl3.project.pbl3_project.ui.dialog;

import com.pbl3.project.pbl3_project.dto.CreateImportOrderRequest;
import com.pbl3.project.pbl3_project.entity.Category;
import com.pbl3.project.pbl3_project.entity.ImportOrder;
import com.pbl3.project.pbl3_project.entity.ImportOrderItem;
import com.pbl3.project.pbl3_project.entity.ImportOrderStatus;
import com.pbl3.project.pbl3_project.entity.Product;
import com.pbl3.project.pbl3_project.entity.Supplier;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.service.CategoryService;
import com.pbl3.project.pbl3_project.service.ImportOrderService;
import com.pbl3.project.pbl3_project.service.MoneySupport;
import com.pbl3.project.pbl3_project.service.ProductService;
import com.pbl3.project.pbl3_project.service.SupplierService;
import com.pbl3.project.pbl3_project.service.ToastService;
import com.pbl3.project.pbl3_project.ui.component.DialogFormFactory;
import com.pbl3.project.pbl3_project.ui.component.StatusBadgeFactory;
import com.pbl3.project.pbl3_project.ui.util.AsyncUiTask;
import com.pbl3.project.pbl3_project.ui.util.DialogSupport;
import com.pbl3.project.pbl3_project.ui.util.ImeInputSupport;
import com.pbl3.project.pbl3_project.ui.util.SearchableComboBoxSupport;
import com.pbl3.project.pbl3_project.ui.util.TableViewSupport;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public final class ImportOrderDialog {

    private static final DateTimeFormatter DATE_TIME_SECONDS_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private ImportOrderDialog() {
    }

    public record Prefill(Long productId, int quantity) {
    }

    public record Context(
        ImportOrderService importOrderService,
        SupplierService supplierService,
        CategoryService categoryService,
        ProductService productService,
        ToastService toastService,
        Consumer<Throwable> errorHandler
    ) {
    }

    private record CreateDialogData(
        List<Supplier> suppliers,
        List<Category> categories,
        List<Product> products
    ) {
    }

    public static void showCreate(
        Stage owner,
        User user,
        Runnable onSuccess,
        Prefill prefill,
        Context context
    ) {
        Stage dialog = DialogSupport.showLoadingWindow(owner, "New Import Order", "Loading import form...", 420, 240);

        javafx.concurrent.Task<CreateDialogData> task = new javafx.concurrent.Task<>() {
            @Override
            protected CreateDialogData call() {
                return new CreateDialogData(
                    context.supplierService().getAllSuppliers(),
                    context.categoryService().getAllCategories().stream()
                        .sorted(Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER))
                        .toList(),
                    context.productService().getAllProducts().stream()
                        .sorted(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER))
                        .toList()
                );
            }
        };
        task.setOnSucceeded(event -> {
            if (!dialog.isShowing()) {
                return;
            }
            populateCreateDialog(dialog, user, onSuccess, prefill, context, task.getValue());
        });
        task.setOnFailed(event -> {
            dialog.close();
            handleError(context, task.getException());
        });
        Thread worker = new Thread(task, "import-dialog-options-loader");
        worker.setDaemon(true);
        worker.start();
    }

    private static void populateCreateDialog(
        Stage dialog,
        User user,
        Runnable onSuccess,
        Prefill prefill,
        Context context,
        CreateDialogData data
    ) {
        dialog.setTitle("New Import Order");

        VBox root = new VBox(16);
        root.getStyleClass().addAll("dialog-root", "import-dialog-root");
        root.setPadding(new Insets(24));

        Label titleLabel = new Label("New Import");
        titleLabel.getStyleClass().add("import-dialog-title");
        titleLabel.setPadding(Insets.EMPTY);

        ComboBox<Supplier> supplierCombo = new ComboBox<>();
        supplierCombo.setMaxWidth(Double.MAX_VALUE);
        supplierCombo.setPromptText("Search supplier");
        supplierCombo.getStyleClass().addAll("product-dialog-combo-box", "import-dialog-combo-box");
        SearchableComboBoxSupport.install(
            supplierCombo,
            data.suppliers(),
            ImportOrderDialog::supplierDisplayText,
            ImportOrderDialog::supplierSearchText
        );
        supplierCombo.getEditor().setPromptText("Search supplier name, phone or address");

        GridPane supplierGrid = DialogFormFactory.grid();
        supplierGrid.add(DialogFormFactory.fieldBlock("Supplier *", supplierCombo, null), 0, 0, 2, 1);
        VBox supplierCard = DialogFormFactory.section("Supplier", supplierGrid);

        TableView<TempItem> table = new TableView<>();
        table.getStyleClass().add("import-dialog-table");
        TableViewSupport.prepareNonReorderableTable(table);

        Label totalLabel = new Label(MessageFormat.format("Total Cost: {0}", "0 VND"));
        totalLabel.getStyleClass().add("import-dialog-total-label");
        Runnable updateTotalAction = () -> {
            BigDecimal total = table.getItems().stream()
                .map(TempItem::getTotal)
                .reduce(BigDecimal.ZERO, MoneySupport::add);
            totalLabel.setText(MessageFormat.format("Total Cost: {0}", formatVnd(total)));
        };

        configureCreateItemsTable(table, updateTotalAction);

        VBox addBox = new VBox(12);
        addBox.getStyleClass().add("import-dialog-section");

        List<Category> availableCategories = data.categories();
        List<Product> availableProducts = data.products();
        Function<Product, String> importProductDisplayText =
            product -> product == null ? "" : product.getName() + " (Stock: " + product.getQuantity() + ")";

        ComboBox<Category> categoryCombo = new ComboBox<>();
        categoryCombo.setMaxWidth(Double.MAX_VALUE);
        categoryCombo.setPromptText("Category");
        categoryCombo.getStyleClass().addAll("product-dialog-combo-box", "import-dialog-combo-box");
        categoryCombo.setConverter(categoryConverter());
        categoryCombo.getItems().addAll(availableCategories);

        ComboBox<Product> productCombo = createProductCombo(availableProducts, importProductDisplayText);

        TextField qtyField = new TextField();
        qtyField.setPromptText("Qty");
        qtyField.getStyleClass().add("product-dialog-input");
        TextField priceField = new TextField();
        priceField.setPromptText("Import price");
        priceField.getStyleClass().add("product-dialog-input");

        final boolean[] syncingProductEditor = {false};
        final boolean[] suppressProductPopup = {false};
        final String[] productSearchQuery = {""};
        java.util.concurrent.atomic.AtomicBoolean productImeComposing =
            ImeInputSupport.trackComposition(productCombo.getEditor());
        PauseTransition productPopupDelay = new PauseTransition(Duration.millis(90));
        productPopupDelay.setOnFinished(event -> {
            if (!productImeComposing.get()
                && !productCombo.isDisable()
                && productCombo.getEditor() != null
                && productCombo.getEditor().isFocused()
                && !productCombo.getItems().isEmpty()
                && !productCombo.isShowing()) {
                productCombo.show();
            }
        });

        Runnable refreshProductChoices = () -> refreshProductChoices(
            categoryCombo,
            productCombo,
            priceField,
            availableProducts,
            importProductDisplayText,
            productSearchQuery,
            syncingProductEditor
        );

        categoryCombo.setOnAction(event -> {
            productSearchQuery[0] = "";
            refreshProductChoices.run();
        });

        productCombo.setOnShowing(event -> {
            if (productCombo.getValue() == null && productCombo.getEditor().getText().isBlank()) {
                productSearchQuery[0] = "";
                refreshProductChoices.run();
            }
        });
        productCombo.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (productCombo.getEditor() == null || !productCombo.getEditor().isFocused()) {
                return;
            }
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER && !productImeComposing.get()) {
                Product resolvedProduct = resolveImportProductSelection(
                    categoryCombo.getValue(),
                    productCombo,
                    availableProducts,
                    importProductDisplayText
                );
                if (resolvedProduct != null) {
                    productCombo.setValue(resolvedProduct);
                    productCombo.getEditor().setText(productCombo.getConverter().toString(resolvedProduct));
                    productCombo.getEditor().positionCaret(productCombo.getEditor().getText().length());
                    if (resolvedProduct.getImportPrice() != null) {
                        priceField.setText(formatWholeNumberText(resolvedProduct.getImportPrice()));
                    } else {
                        priceField.clear();
                    }
                    productCombo.hide();
                }
                event.consume();
            }
        });

        productCombo.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (syncingProductEditor[0] || suppressProductPopup[0]) {
                return;
            }
            Product selected = productCombo.getValue();
            if (selected != null && Objects.equals(productCombo.getConverter().toString(selected), newValue)) {
                return;
            }
            productSearchQuery[0] = newValue == null ? "" : newValue;
            if (selected != null) {
                productCombo.setValue(null);
                priceField.clear();
            }
            refreshProductChoices.run();
            if (!productImeComposing.get()
                && !productCombo.isDisable()
                && !productCombo.getItems().isEmpty()
                && !productCombo.isShowing()) {
                productPopupDelay.playFromStart();
            }
        });

        productCombo.setOnAction(event -> {
            Product product = productCombo.getValue();
            if (syncingProductEditor[0]) {
                return;
            }
            if (product != null) {
                productSearchQuery[0] = "";
                syncingProductEditor[0] = true;
                productCombo.getEditor().setText(productCombo.getConverter().toString(product));
                syncingProductEditor[0] = false;
                if (product.getImportPrice() != null) {
                    priceField.setText(formatWholeNumberText(product.getImportPrice()));
                } else {
                    priceField.clear();
                }
            }
        });

        Button addBtn = new Button("Add Item");
        addBtn.getStyleClass().addAll("button", "import-dialog-add-button");
        addBtn.setOnAction(event -> {
            if (categoryCombo.getValue() == null) {
                context.toastService().showWarning("Select a category.");
                return;
            }
            Product product = productCombo.getValue();
            if (product == null) {
                context.toastService().showWarning("Select a product.");
                return;
            }
            try {
                int quantity = Integer.parseInt(qtyField.getText());
                BigDecimal importPrice = parseMoneyInput(priceField.getText());
                if (quantity <= 0 || importPrice.signum() < 0) {
                    throw new NumberFormatException();
                }
                TempItem addedItem = new TempItem(product, quantity, importPrice);
                table.getItems().add(addedItem);
                updateTotalAction.run();
                resetProductEntry(productCombo, qtyField, priceField, table, addedItem, productSearchQuery, syncingProductEditor, suppressProductPopup);
            } catch (Exception ex) {
                context.toastService().showError("Invalid quantity or price.");
            }
        });

        GridPane addGrid = DialogFormFactory.grid();
        addGrid.add(DialogFormFactory.fieldBlock("Category *", categoryCombo, null), 0, 0);
        addGrid.add(DialogFormFactory.fieldBlock("Product *", productCombo, null), 1, 0);
        addGrid.add(DialogFormFactory.fieldBlock("Quantity *", qtyField, null), 0, 1);
        addGrid.add(DialogFormFactory.fieldBlock("Import Price *", priceField, null), 1, 1);

        HBox addActionRow = new HBox(addBtn);
        addActionRow.setAlignment(Pos.CENTER_RIGHT);
        Label addItemsTitle = new Label("Add Items");
        addItemsTitle.getStyleClass().add("product-dialog-section-title");
        addBox.getChildren().addAll(addItemsTitle, addGrid, addActionRow);

        applyPrefill(prefill, availableProducts, categoryCombo, productCombo, qtyField, priceField, productSearchQuery, syncingProductEditor, refreshProductChoices, context);

        HBox actionBox = createCreateFooter(dialog, supplierCombo, data.suppliers(), table, totalLabel, user, onSuccess, context);

        VBox tableCard = DialogFormFactory.section("Import Items", table);
        table.setMinHeight(220);
        table.setPrefHeight(260);
        tableCard.setMinHeight(280);
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        VBox contentBox = new VBox(16);
        contentBox.getChildren().addAll(titleLabel, supplierCard, addBox, tableCard);

        ScrollPane contentScroll = new ScrollPane(contentBox);
        contentScroll.setFitToWidth(true);
        contentScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        contentScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        contentScroll.getStyleClass().addAll("product-dialog-scroll", "import-dialog-scroll");
        VBox.setVgrow(contentScroll, Priority.ALWAYS);

        root.getChildren().addAll(contentScroll, actionBox);

        Scene scene = new Scene(root, 920, 720);
        applyApplicationStyles(scene);
        dialog.setScene(scene);
        dialog.setMinWidth(860);
        dialog.setMinHeight(640);
        DialogSupport.preventInitialFieldFocus(dialog, root);
        DialogSupport.centerWindowOnOwner(dialog);
        if (!dialog.isShowing()) {
            dialog.show();
        }
    }

    public static void showDetails(
        Stage owner,
        ImportOrder order,
        User user,
        Runnable onChanged,
        Context context
    ) {
        try {
            Stage dialog = new Stage();
            dialog.initOwner(owner);
            dialog.setTitle(MessageFormat.format("Import Order Details #{0}", order.getId()));
            dialog.initModality(Modality.WINDOW_MODAL);

            VBox root = new VBox(16);
            root.getStyleClass().addAll("dialog-root", "import-dialog-root", "import-detail-dialog-root");

            Label titleLabel = new Label(MessageFormat.format("Import Order #{0}", order.getId()));
            titleLabel.getStyleClass().add("import-dialog-title");
            titleLabel.setPadding(Insets.EMPTY);

            VBox overviewCard = createOverviewCard(order);
            VBox notesCard = createNotesCard(order);
            TableView<ImportOrderItem> table = createImportDetailItemsTable(order);
            VBox itemsCard = DialogFormFactory.section("Import Items", table);
            VBox.setVgrow(table, Priority.ALWAYS);
            VBox.setVgrow(itemsCard, Priority.ALWAYS);

            Label totalLabel = new Label(MessageFormat.format("Total Cost: {0}", formatVnd(order.getTotalCost())));
            totalLabel.getStyleClass().add("import-dialog-total-label");
            HBox totalPill = new HBox(totalLabel);
            totalPill.getStyleClass().add("import-dialog-total-pill");
            totalPill.setAlignment(Pos.CENTER_LEFT);

            Button closeButton = new Button("Close");
            closeButton.getStyleClass().addAll("button", "dialog-cancel-button");
            closeButton.setOnAction(event -> dialog.close());

            Button cancelImportButton = new Button("Cancel Import");
            cancelImportButton.getStyleClass().addAll("button", "danger-button", "import-detail-danger-button");
            cancelImportButton.setDisable(order.getStatus() != null && order.getStatus() != ImportOrderStatus.COMPLETED);
            cancelImportButton.setOnAction(event -> loadImportOrderForAction(
                dialog,
                order.getId(),
                context,
                loadedOrder -> showCancel(
                    dialog,
                    loadedOrder,
                    user,
                    () -> {
                        if (onChanged != null) {
                            onChanged.run();
                        }
                        dialog.close();
                    },
                    context
                )
            ));

            HBox actionRow = new HBox(14);
            actionRow.setAlignment(Pos.CENTER_RIGHT);
            actionRow.getStyleClass().add("import-dialog-footer");
            actionRow.getChildren().addAll(totalPill, new Region(), closeButton, cancelImportButton);
            HBox.setHgrow(actionRow.getChildren().get(1), Priority.ALWAYS);

            VBox contentBox = new VBox(16, titleLabel, overviewCard, notesCard, itemsCard);

            ScrollPane contentScroll = new ScrollPane(contentBox);
            contentScroll.setFitToWidth(true);
            contentScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            contentScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            contentScroll.getStyleClass().addAll("product-dialog-scroll", "import-dialog-scroll");
            VBox.setVgrow(contentScroll, Priority.ALWAYS);

            root.getChildren().addAll(contentScroll, actionRow);
            TableViewSupport.enableDeselectOnOutsideClick(root, table);

            Scene scene = new Scene(root, 820, 680);
            applyApplicationStyles(scene);
            dialog.setScene(scene);
            dialog.setMinWidth(780);
            dialog.setMinHeight(620);
            Platform.runLater(() -> {
                contentScroll.setVvalue(0);
                root.requestFocus();
            });
            dialog.showAndWait();
        } catch (Exception ex) {
            handleError(context, ex);
        }
    }

    private static void loadImportOrderForAction(
        Stage owner,
        Long orderId,
        Context context,
        Consumer<ImportOrder> onLoaded
    ) {
        Stage loadingDialog = DialogSupport.showLoadingWindow(
            owner,
            MessageFormat.format("Import Order #{0}", orderId),
            "Loading import order details...",
            420,
            240
        );
        javafx.concurrent.Task<ImportOrder> task = new javafx.concurrent.Task<>() {
            @Override
            protected ImportOrder call() {
                return context.importOrderService().getImportOrderWithItems(orderId);
            }
        };
        task.setOnSucceeded(event -> {
            if (!loadingDialog.isShowing()) {
                return;
            }
            loadingDialog.close();
            onLoaded.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            loadingDialog.close();
            handleError(context, task.getException());
        });
        Thread worker = new Thread(task, "import-action-dialog-loader");
        worker.setDaemon(true);
        worker.start();
    }

    public static void showCancel(
        Stage owner,
        ImportOrder order,
        User user,
        Runnable onSuccess,
        Context context
    ) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle(MessageFormat.format("Cancel Import Order #{0}", order.getId()));

        VBox root = new VBox(16);
        root.getStyleClass().addAll("dialog-root", "import-dialog-root", "import-cancel-dialog-root");

        Label title = new Label(MessageFormat.format("Cancel Import Order #{0}", order.getId()));
        title.getStyleClass().add("import-dialog-title");
        title.setPadding(Insets.EMPTY);

        Label helper = new Label("This will reverse received stock and recompute moving-average cost. Reason is required.");
        helper.setWrapText(true);
        helper.getStyleClass().add("import-cancel-helper");

        Label totalLabel = new Label(MessageFormat.format("Total Cost: {0}", formatVnd(order.getTotalCost())));
        totalLabel.getStyleClass().add("import-cancel-total");

        TextArea reasonArea = new TextArea();
        reasonArea.setPromptText("Return Reason");
        reasonArea.setWrapText(true);
        reasonArea.setPrefRowCount(5);
        reasonArea.getStyleClass().addAll("product-dialog-text-area", "import-cancel-reason-area");

        VBox warningCard = new VBox(12, helper, totalLabel, DialogFormFactory.fieldBlock("Reason *", reasonArea, null));
        warningCard.getStyleClass().add("import-cancel-warning-card");

        Button confirmBtn = new Button("Confirm Cancel");
        confirmBtn.getStyleClass().addAll("button", "danger-button", "import-detail-danger-button");
        confirmBtn.setOnAction(event -> {
            try {
                context.importOrderService().cancelImportOrder(order.getId(), user.getId(), reasonArea.getText());
                context.toastService().showSuccess("Import order canceled.");
                dialog.close();
                if (onSuccess != null) {
                    onSuccess.run();
                }
            } catch (Exception ex) {
                handleError(context, ex);
            }
        });

        Button closeBtn = new Button("Cancel");
        closeBtn.getStyleClass().addAll("button", "dialog-cancel-button");
        closeBtn.setOnAction(event -> dialog.close());

        HBox actionRow = new HBox(14, closeBtn, confirmBtn);
        actionRow.setAlignment(Pos.CENTER_RIGHT);
        actionRow.getStyleClass().add("import-dialog-footer");

        root.getChildren().addAll(title, warningCard, actionRow);

        Scene scene = new Scene(root, 690, 410);
        applyApplicationStyles(scene);
        dialog.setScene(scene);
        dialog.setMinWidth(650);
        dialog.setMinHeight(380);
        DialogSupport.preventInitialFieldFocus(dialog, root);
        dialog.showAndWait();
    }

    private static ComboBox<Product> createProductCombo(
        List<Product> availableProducts,
        Function<Product, String> importProductDisplayText
    ) {
        ComboBox<Product> productCombo = new ComboBox<>();
        productCombo.setMaxWidth(Double.MAX_VALUE);
        productCombo.setPromptText("Search product");
        productCombo.getStyleClass().addAll("product-dialog-combo-box", "import-dialog-combo-box");
        productCombo.setEditable(true);
        productCombo.setVisibleRowCount(10);
        productCombo.getEditor().setPromptText("Search SKU, barcode or product");
        productCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Product product) {
                return importProductDisplayText.apply(product);
            }

            @Override
            public Product fromString(String string) {
                if (string == null || string.isBlank()) {
                    return null;
                }
                String normalized = string.trim();
                return availableProducts.stream()
                    .filter(product -> importProductDisplayText.apply(product).equals(normalized)
                        || product.getName().equalsIgnoreCase(normalized)
                        || (product.getSku() != null && product.getSku().equalsIgnoreCase(normalized))
                        || (product.getBarcode() != null && product.getBarcode().equalsIgnoreCase(normalized)))
                    .findFirst()
                    .orElse(null);
            }
        });
        productCombo.setDisable(true);
        return productCombo;
    }

    private static void configureCreateItemsTable(TableView<TempItem> table, Runnable updateTotalAction) {
        TableColumn<TempItem, String> nameCol = new TableColumn<>("Product");
        nameCol.setPrefWidth(260);
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().product.getName()));

        TableColumn<TempItem, Integer> qtyCol = new TableColumn<>("Qty");
        qtyCol.setPrefWidth(90);
        qtyCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().quantity));

        TableColumn<TempItem, String> priceCol = new TableColumn<>("Unit Price");
        priceCol.setPrefWidth(150);
        priceCol.setCellValueFactory(data -> new SimpleStringProperty(formatVnd(data.getValue().importPrice)));

        TableColumn<TempItem, String> totalCol = new TableColumn<>("Total");
        totalCol.setPrefWidth(150);
        totalCol.setCellValueFactory(data -> new SimpleStringProperty(formatVnd(data.getValue().getTotal())));

        TableColumn<TempItem, Void> actionCol = new TableColumn<>("Action");
        actionCol.setPrefWidth(110);
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Remove");

            {
                btn.getStyleClass().addAll("button", "danger-button", "import-dialog-remove-button");
                btn.setOnAction(event -> {
                    TempItem item = getTableView().getItems().get(getIndex());
                    table.getItems().remove(item);
                    updateTotalAction.run();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
        table.getColumns().addAll(nameCol, qtyCol, priceCol, totalCol, actionCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private static void refreshProductChoices(
        ComboBox<Category> categoryCombo,
        ComboBox<Product> productCombo,
        TextField priceField,
        List<Product> availableProducts,
        Function<Product, String> importProductDisplayText,
        String[] productSearchQuery,
        boolean[] syncingProductEditor
    ) {
        Category selectedCategory = categoryCombo.getValue();
        Product selectedProduct = productCombo.getValue();
        String normalizedSearch = productSearchQuery[0] == null ? "" : productSearchQuery[0].trim().toLowerCase();
        List<Product> filteredProducts = selectedCategory == null
            ? List.of()
            : availableProducts.stream()
                .filter(product -> product.getCategory() != null && Objects.equals(product.getCategory().getId(), selectedCategory.getId()))
                .filter(product -> normalizedSearch.isEmpty()
                    || product.getName().toLowerCase().contains(normalizedSearch)
                    || (product.getSku() != null && product.getSku().toLowerCase().contains(normalizedSearch))
                    || (product.getBarcode() != null && product.getBarcode().toLowerCase().contains(normalizedSearch)))
                .toList();

        syncingProductEditor[0] = true;
        productCombo.getItems().setAll(filteredProducts);
        boolean keepSelection = selectedProduct != null && filteredProducts.stream()
            .anyMatch(product -> Objects.equals(product.getId(), selectedProduct.getId()));

        if (keepSelection) {
            productCombo.setValue(selectedProduct);
            setProductComboEditorText(productCombo, productCombo.getConverter().toString(selectedProduct));
        } else {
            if (productCombo.getValue() != null) {
                productCombo.setValue(null);
            }
            setProductComboEditorText(productCombo, selectedCategory == null ? "" : productSearchQuery[0]);
            priceField.clear();
        }

        boolean categorySelected = selectedCategory != null;
        productCombo.setDisable(!categorySelected);
        if (productCombo.isDisable() && selectedCategory == null) {
            setProductComboEditorText(productCombo, "");
        }
        syncingProductEditor[0] = false;
    }

    private static void setProductComboEditorText(ComboBox<Product> productCombo, String text) {
        if (productCombo.getEditor() == null) {
            return;
        }
        String safeText = text == null ? "" : text;
        String currentText = productCombo.getEditor().getText();
        if (!Objects.equals(currentText, safeText)) {
            productCombo.getEditor().setText(safeText);
        }
    }

    private static Product resolveImportProductSelection(
        Category selectedCategory,
        ComboBox<Product> productCombo,
        List<Product> availableProducts,
        Function<Product, String> importProductDisplayText
    ) {
        String rawText = productCombo.getEditor() == null ? "" : productCombo.getEditor().getText();
        String normalizedText = rawText == null ? "" : rawText.trim();
        if (selectedCategory == null || normalizedText.isEmpty()) {
            return null;
        }
        Product currentValue = productCombo.getValue();
        if (currentValue != null && Objects.equals(importProductDisplayText.apply(currentValue), normalizedText)) {
            return currentValue;
        }
        List<Product> scopedProducts = availableProducts.stream()
            .filter(product -> product.getCategory() != null && Objects.equals(product.getCategory().getId(), selectedCategory.getId()))
            .toList();
        Product exactMatch = scopedProducts.stream()
            .filter(product -> importProductDisplayText.apply(product).equalsIgnoreCase(normalizedText)
                || product.getName().equalsIgnoreCase(normalizedText)
                || (product.getSku() != null && product.getSku().equalsIgnoreCase(normalizedText))
                || (product.getBarcode() != null && product.getBarcode().equalsIgnoreCase(normalizedText)))
            .findFirst()
            .orElse(null);
        if (exactMatch != null) {
            return exactMatch;
        }
        String query = normalizedText.toLowerCase();
        List<Product> partialMatches = scopedProducts.stream()
            .filter(product -> product.getName().toLowerCase().contains(query)
                || (product.getSku() != null && product.getSku().toLowerCase().contains(query))
                || (product.getBarcode() != null && product.getBarcode().toLowerCase().contains(query)))
            .toList();
        return partialMatches.size() == 1 ? partialMatches.get(0) : null;
    }

    private static void resetProductEntry(
        ComboBox<Product> productCombo,
        TextField qtyField,
        TextField priceField,
        TableView<TempItem> table,
        TempItem addedItem,
        String[] productSearchQuery,
        boolean[] syncingProductEditor,
        boolean[] suppressProductPopup
    ) {
        productSearchQuery[0] = "";
        suppressProductPopup[0] = true;
        productCombo.hide();
        productCombo.setValue(null);
        syncingProductEditor[0] = true;
        productCombo.getEditor().clear();
        syncingProductEditor[0] = false;
        suppressProductPopup[0] = false;
        qtyField.clear();
        priceField.clear();
        Platform.runLater(() -> {
            productCombo.hide();
            table.getSelectionModel().select(addedItem);
            table.scrollTo(addedItem);
            table.requestFocus();
        });
    }

    private static void applyPrefill(
        Prefill prefill,
        List<Product> availableProducts,
        ComboBox<Category> categoryCombo,
        ComboBox<Product> productCombo,
        TextField qtyField,
        TextField priceField,
        String[] productSearchQuery,
        boolean[] syncingProductEditor,
        Runnable refreshProductChoices,
        Context context
    ) {
        if (prefill == null || prefill.productId() == null) {
            return;
        }
        Product prefillProduct = availableProducts.stream()
            .filter(product -> Objects.equals(product.getId(), prefill.productId()))
            .findFirst()
            .orElse(null);
        if (prefillProduct == null || prefillProduct.getCategory() == null) {
            context.toastService().showWarning("Select a product before adding.");
            return;
        }
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

    private static String supplierDisplayText(Supplier supplier) {
        return supplier == null ? "" : supplier.getName();
    }

    private static String supplierSearchText(Supplier supplier) {
        if (supplier == null) {
            return "";
        }
        return String.join(
            " ",
            nullToBlank(supplier.getName()),
            nullToBlank(supplier.getPhone()),
            nullToBlank(supplier.getAddress())
        );
    }

    private static String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private static HBox createCreateFooter(
        Stage dialog,
        ComboBox<Supplier> supplierCombo,
        List<Supplier> suppliers,
        TableView<TempItem> table,
        Label totalLabel,
        User user,
        Runnable onSuccess,
        Context context
    ) {
        HBox actionBox = new HBox(15);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        actionBox.getStyleClass().add("import-dialog-footer");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("button", "dialog-cancel-button");
        cancelBtn.setOnAction(event -> dialog.close());

        Button confirmBtn = new Button("Confirm Import");
        confirmBtn.getStyleClass().addAll("button", "success-button", "import-dialog-confirm-button");
        confirmBtn.setOnAction(event -> {
            Supplier selectedSupplier = SearchableComboBoxSupport.resolveValue(
                supplierCombo,
                suppliers,
                ImportOrderDialog::supplierDisplayText,
                ImportOrderDialog::supplierSearchText
            );
            if (selectedSupplier == null) {
                context.toastService().showWarning("Select a supplier.");
                return;
            }
            supplierCombo.setValue(selectedSupplier);
            if (table.getItems().isEmpty()) {
                context.toastService().showWarning("Add at least one product.");
                return;
            }
            try {
                CreateImportOrderRequest request = new CreateImportOrderRequest();
                request.setUserId(user.getId());
                request.setSupplierId(selectedSupplier.getId());
                request.setNotes("Import via UI");
                List<CreateImportOrderRequest.ImportOrderItemRequest> items = new ArrayList<>();
                for (TempItem item : table.getItems()) {
                    var itemRequest = new CreateImportOrderRequest.ImportOrderItemRequest();
                    itemRequest.setProductId(item.product.getId());
                    itemRequest.setQuantity(item.quantity);
                    itemRequest.setImportPrice(item.importPrice);
                    items.add(itemRequest);
                }
                request.setItems(items);
                AsyncUiTask.runButtonTask(
                    confirmBtn,
                    cancelBtn,
                    "Creating...",
                    () -> context.importOrderService().createImportOrder(request),
                    created -> {
                        context.toastService().showSuccess("Import order created.");
                        if (onSuccess != null) {
                            onSuccess.run();
                        }
                        dialog.close();
                    },
                    ex -> context.toastService().showError(MessageFormat.format("Error: {0}", ex.getMessage())),
                    "import-order-create"
                );
            } catch (Exception ex) {
                context.toastService().showError(MessageFormat.format("Error: {0}", ex.getMessage()));
            }
        });

        HBox totalPill = new HBox(totalLabel);
        totalPill.getStyleClass().add("import-dialog-total-pill");
        totalPill.setAlignment(Pos.CENTER_LEFT);
        actionBox.getChildren().addAll(totalPill, new Region(), cancelBtn, confirmBtn);
        HBox.setHgrow(actionBox.getChildren().get(1), Priority.ALWAYS);
        return actionBox;
    }

    private static VBox createOverviewCard(ImportOrder order) {
        Label supplierValueLabel = createImportDetailValue(order.getSupplierDisplayName(), true);
        Label dateValueLabel = createImportDetailValue(formatDateTimeWithSeconds(order.getCreatedAt()), false);
        Label userValueLabel = createImportDetailValue(order.getCreatedByDisplayName(), false);
        Label statusValueLabel = StatusBadgeFactory.importOrder(order.getStatus());
        HBox statusBadgeWrap = new HBox(statusValueLabel);
        statusBadgeWrap.setAlignment(Pos.CENTER_LEFT);

        GridPane overviewGrid = DialogFormFactory.grid();
        overviewGrid.add(DialogFormFactory.fieldBlock("Supplier", supplierValueLabel, null), 0, 0);
        overviewGrid.add(DialogFormFactory.fieldBlock("Status", statusBadgeWrap, null), 1, 0);
        overviewGrid.add(DialogFormFactory.fieldBlock("Created At", dateValueLabel, null), 0, 1);
        overviewGrid.add(DialogFormFactory.fieldBlock("Created By", userValueLabel, null), 1, 1);
        return DialogFormFactory.section("Overview", overviewGrid);
    }

    private static VBox createNotesCard(ImportOrder order) {
        Label statusNoteLabel = new Label(order.getStatusNote() == null || order.getStatusNote().isBlank() ? "-" : order.getStatusNote());
        statusNoteLabel.setWrapText(true);
        statusNoteLabel.getStyleClass().add("import-detail-note-text");

        Label notesLabel = new Label(order.getNotes() == null || order.getNotes().isBlank() ? "-" : order.getNotes());
        notesLabel.setWrapText(true);
        notesLabel.getStyleClass().add("import-detail-note-text");

        GridPane notesGrid = DialogFormFactory.grid();
        notesGrid.add(DialogFormFactory.fieldBlock("Status Note", statusNoteLabel, null), 0, 0);
        notesGrid.add(DialogFormFactory.fieldBlock("Import Notes", notesLabel, null), 1, 0);
        return DialogFormFactory.section("Notes", notesGrid);
    }

    private static TableView<ImportOrderItem> createImportDetailItemsTable(ImportOrder order) {
        TableView<ImportOrderItem> table = new TableView<>();
        table.getStyleClass().addAll("import-dialog-table", "import-detail-table");
        TableViewSupport.prepareNonReorderableTable(table);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<ImportOrderItem, String> productCol = new TableColumn<>("Product");
        productCol.setPrefWidth(300);
        productCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getProductDisplayName()));

        TableColumn<ImportOrderItem, Integer> qtyCol = new TableColumn<>("Qty");
        qtyCol.setPrefWidth(90);
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<ImportOrderItem, String> importPriceCol = new TableColumn<>("Import");
        importPriceCol.setPrefWidth(160);
        importPriceCol.setCellValueFactory(cell -> new SimpleStringProperty(formatVnd(cell.getValue().getImportPrice())));

        TableColumn<ImportOrderItem, String> subtotalCol = new TableColumn<>("Subtotal");
        subtotalCol.setPrefWidth(170);
        subtotalCol.setCellValueFactory(cell -> new SimpleStringProperty(formatVnd(MoneySupport.multiply(
            cell.getValue().getImportPrice(),
            cell.getValue().getQuantity() != null ? cell.getValue().getQuantity() : 0
        ))));

        table.getColumns().addAll(productCol, qtyCol, importPriceCol, subtotalCol);
        table.setItems(FXCollections.observableArrayList(order.getItems() == null ? List.of() : order.getItems()));
        table.setMinHeight(260);
        table.setPrefHeight(320);
        return table;
    }

    private static Label createImportDetailValue(String text, boolean strong) {
        Label label = new Label(text == null || text.isBlank() ? "-" : text);
        label.setWrapText(true);
        label.getStyleClass().add(strong ? "import-detail-value-strong" : "import-detail-value");
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private static StringConverter<Category> categoryConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Category category) {
                return category == null ? "" : category.getName();
            }

            @Override
            public Category fromString(String string) {
                return null;
            }
        };
    }

    private static String formatDateTimeWithSeconds(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_SECONDS_FORMATTER) : "-";
    }

    private static String formatVnd(BigDecimal amount) {
        return String.format("%,.0f VND", MoneySupport.normalize(amount).doubleValue());
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
        scene.getStylesheets().add(Objects.requireNonNull(ImportOrderDialog.class.getResource("/application.css")).toExternalForm());
    }

    private static void handleError(Context context, Throwable throwable) {
        if (context != null && context.errorHandler() != null) {
            context.errorHandler().accept(throwable);
        } else if (throwable != null) {
            throw new RuntimeException(throwable);
        }
    }

    private static final class TempItem {
        private final Product product;
        private final int quantity;
        private final BigDecimal importPrice;

        private TempItem(Product product, int quantity, BigDecimal importPrice) {
            this.product = product;
            this.quantity = quantity;
            this.importPrice = importPrice;
        }

        public BigDecimal getTotal() {
            return MoneySupport.multiply(importPrice, quantity);
        }
    }
}
