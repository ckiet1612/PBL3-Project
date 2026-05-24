package com.pbl3.project.pbl3_project.ui.dialog;

import com.pbl3.project.pbl3_project.entity.Order;
import com.pbl3.project.pbl3_project.entity.OrderItem;
import com.pbl3.project.pbl3_project.entity.OrderStatus;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.service.MoneySupport;
import com.pbl3.project.pbl3_project.service.OrderService;
import com.pbl3.project.pbl3_project.service.ReceiptService;
import com.pbl3.project.pbl3_project.service.ToastService;
import com.pbl3.project.pbl3_project.ui.component.DialogFormFactory;
import com.pbl3.project.pbl3_project.ui.component.QuantityStepper;
import com.pbl3.project.pbl3_project.ui.component.StatusBadgeFactory;
import com.pbl3.project.pbl3_project.ui.util.DialogSupport;
import com.pbl3.project.pbl3_project.ui.util.TableViewSupport;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public final class OrderDialog {

    private static final DateTimeFormatter DATE_TIME_SECONDS_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private OrderDialog() {
    }

    public record Context(
        OrderService orderService,
        ReceiptService receiptService,
        ToastService toastService,
        Consumer<Throwable> errorHandler
    ) {
    }

    public static void showDetails(
        Stage owner,
        Order order,
        User user,
        Runnable onChanged,
        Context context
    ) {
        try {
            Stage dialog = new Stage();
            dialog.initOwner(owner);
            dialog.setTitle(MessageFormat.format("Order Details #{0}", order.getId()));
            dialog.initModality(Modality.WINDOW_MODAL);

            VBox root = new VBox(16);
            root.getStyleClass().addAll("dialog-root", "order-detail-dialog-root");

            Label titleLabel = new Label(MessageFormat.format("Order #{0}", order.getId()));
            titleLabel.getStyleClass().add("import-dialog-title");
            titleLabel.setPadding(Insets.EMPTY);

            VBox overviewCard = createOverviewCard(order);
            VBox totalsCard = createTotalsCard(order);
            VBox noteCard = createStatusNoteCard(order);
            TableView<OrderItem> table = createItemsTable(order);
            VBox itemsCard = DialogFormFactory.section("Order Items", table);
            VBox.setVgrow(table, Priority.ALWAYS);
            VBox.setVgrow(itemsCard, Priority.ALWAYS);

            Button returnButton = new Button("Return Items");
            returnButton.getStyleClass().addAll("button", "primary-button", "order-detail-primary-button");
            returnButton.setDisable(order.getStatus() == OrderStatus.CANCELED || order.getStatus() == OrderStatus.RETURNED);
            returnButton.setOnAction(event -> loadOrderForAction(
                dialog,
                order.getId(),
                user,
                context,
                loadedOrder -> showReturn(
                    dialog,
                    loadedOrder,
                    user,
                    () -> closeAndRefresh(dialog, onChanged),
                    context
                )
            ));

            Button cancelOrderButton = new Button("Cancel Order");
            cancelOrderButton.getStyleClass().addAll("button", "danger-button", "order-detail-danger-button");
            cancelOrderButton.setDisable(order.getStatus() != null && order.getStatus() != OrderStatus.COMPLETED);
            cancelOrderButton.setOnAction(event -> loadOrderForAction(
                dialog,
                order.getId(),
                user,
                context,
                loadedOrder -> showCancel(
                    dialog,
                    loadedOrder,
                    user,
                    () -> closeAndRefresh(dialog, onChanged),
                    context
                )
            ));

            Button closeButton = new Button("Close");
            closeButton.getStyleClass().addAll("button", "dialog-cancel-button");
            closeButton.setOnAction(event -> dialog.close());

            Button receiptButton = new Button(resolveReceiptButtonText(order, context));
            receiptButton.getStyleClass().addAll("button", "dashboard-report-secondary-button");
            receiptButton.setOnAction(event -> openOrGenerateReceipt(dialog, order, receiptButton, context));

            Label footerNetLabel = new Label(MessageFormat.format("Net: {0}", formatVnd(order.getNetTotal())));
            footerNetLabel.getStyleClass().add("order-detail-footer-total");
            HBox footerNetPill = new HBox(footerNetLabel);
            footerNetPill.getStyleClass().addAll("order-total-chip", "order-total-chip-success");

            HBox actionRow = new HBox(14);
            actionRow.setAlignment(Pos.CENTER_RIGHT);
            actionRow.getStyleClass().add("import-dialog-footer");
            actionRow.getChildren().addAll(footerNetPill, new Region(), receiptButton, closeButton, returnButton, cancelOrderButton);
            HBox.setHgrow(actionRow.getChildren().get(1), Priority.ALWAYS);

            VBox contentBox = new VBox(16, titleLabel, overviewCard, totalsCard, noteCard, itemsCard);
            ScrollPane contentScroll = new ScrollPane(contentBox);
            contentScroll.setFitToWidth(true);
            contentScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            contentScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            contentScroll.getStyleClass().addAll("product-dialog-scroll", "import-dialog-scroll");
            VBox.setVgrow(contentScroll, Priority.ALWAYS);

            root.getChildren().addAll(contentScroll, actionRow);
            TableViewSupport.enableDeselectOnOutsideClick(root, table);

            Scene scene = new Scene(root, 980, 720);
            applyApplicationStyles(scene);
            dialog.setScene(scene);
            dialog.setMinWidth(900);
            dialog.setMinHeight(650);
            javafx.application.Platform.runLater(() -> {
                contentScroll.setVvalue(0);
                root.requestFocus();
            });
            dialog.showAndWait();
        } catch (Exception ex) {
            handleError(context, ex);
        }
    }

    private static String resolveReceiptButtonText(Order order, Context context) {
        if (context == null || context.receiptService() == null) {
            return "Generate Receipt";
        }
        return context.receiptService().hasStoredReceiptFile(order) ? "Open Receipt" : "Generate Receipt";
    }

    private static void openOrGenerateReceipt(Stage owner, Order order, Button receiptButton, Context context) {
        if (context == null || context.receiptService() == null || order == null) {
            return;
        }
        if (context.receiptService().hasStoredReceiptFile(order)) {
            try {
                context.receiptService().openStoredReceipt(order);
            } catch (ReceiptService.ReceiptGenerationException ex) {
                context.toastService().showWarning(ex.getMessage());
                receiptButton.setText("Generate Receipt");
            }
            return;
        }

        Stage loadingDialog = DialogSupport.showLoadingWindow(
            owner,
            MessageFormat.format("Receipt #{0}", order.getId()),
            "Generating receipt PDF...",
            420,
            240
        );
        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() {
                context.receiptService().generateAndOpenReceipt(order);
                return null;
            }
        };
        task.setOnSucceeded(event -> {
            loadingDialog.close();
            receiptButton.setText("Open Receipt");
            context.toastService().showSuccess("Receipt generated");
        });
        task.setOnFailed(event -> {
            loadingDialog.close();
            handleError(context, task.getException());
        });
        Thread worker = new Thread(task, "order-receipt-generator");
        worker.setDaemon(true);
        worker.start();
    }

    private static void loadOrderForAction(
        Stage owner,
        Long orderId,
        User user,
        Context context,
        Consumer<Order> onLoaded
    ) {
        Stage loadingDialog = DialogSupport.showLoadingWindow(
            owner,
            MessageFormat.format("Order #{0}", orderId),
            "Loading order details...",
            420,
            240
        );
        javafx.concurrent.Task<Order> task = new javafx.concurrent.Task<>() {
            @Override
            protected Order call() {
                return context.orderService().getOrderWithItems(orderId, user);
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
        Thread worker = new Thread(task, "order-action-dialog-loader");
        worker.setDaemon(true);
        worker.start();
    }

    public static void showCancel(
        Stage owner,
        Order order,
        User user,
        Runnable onSuccess,
        Context context
    ) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle(MessageFormat.format("Cancel Order #{0}", order.getId()));

        VBox root = new VBox(16);
        root.getStyleClass().addAll("dialog-root", "order-cancel-dialog-root");

        Label title = new Label(MessageFormat.format("Cancel Order #{0}", order.getId()));
        title.getStyleClass().add("import-dialog-title");
        title.setPadding(Insets.EMPTY);

        Label helper = new Label("This will restore stock for every order item. Reason is required.");
        helper.setWrapText(true);
        helper.getStyleClass().add("import-cancel-helper");

        Label totalLabel = new Label(MessageFormat.format("Amount Paid: {0}", formatVnd(order.getTotalPrice())));
        totalLabel.getStyleClass().add("import-cancel-total");

        TextArea reasonArea = new TextArea();
        reasonArea.setPromptText("Return Reason");
        reasonArea.setWrapText(true);
        reasonArea.setPrefRowCount(5);
        reasonArea.getStyleClass().addAll("product-dialog-text-area", "order-cancel-reason-area");

        VBox warningCard = new VBox(12, helper, totalLabel, DialogFormFactory.fieldBlock("Reason *", reasonArea, null));
        warningCard.getStyleClass().add("order-cancel-warning-card");

        Button confirmBtn = new Button("Confirm Cancel");
        confirmBtn.getStyleClass().addAll("button", "danger-button", "order-detail-danger-button");
        confirmBtn.setOnAction(event -> {
            try {
                context.orderService().cancelOrder(order.getId(), user.getId(), reasonArea.getText());
                context.toastService().showSuccess("Order canceled.");
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

        Scene scene = new Scene(root, 650, 410);
        applyApplicationStyles(scene);
        dialog.setScene(scene);
        dialog.setMinWidth(610);
        dialog.setMinHeight(380);
        DialogSupport.preventInitialFieldFocus(dialog, root);
        dialog.showAndWait();
    }

    public static void showReturn(
        Stage owner,
        Order order,
        User user,
        Runnable onSuccess,
        Context context
    ) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle(MessageFormat.format("Return Items - Order #{0}", order.getId()));

        VBox root = new VBox(16);
        root.setPadding(new Insets(22));
        root.setFocusTraversable(true);
        root.getStyleClass().addAll("dialog-root", "return-dialog-root");

        Label title = new Label(MessageFormat.format("Return Items for Order #{0}", order.getId()));
        title.getStyleClass().add("dialog-title");

        Label helper = new Label("Choose the quantities to return and provide a reason for the audit log.");
        helper.getStyleClass().add("dialog-subtitle");
        helper.setWrapText(true);

        VBox itemsBox = new VBox(12);
        itemsBox.getStyleClass().add("return-items-list");
        itemsBox.setFillWidth(true);
        Map<Long, QuantityStepper> returnInputs = new LinkedHashMap<>();

        Label refundCaptionLabel = new Label("Estimated Refund");
        refundCaptionLabel.getStyleClass().add("return-summary-caption");
        Label refundPreviewLabel = new Label(formatVnd(BigDecimal.ZERO));
        refundPreviewLabel.getStyleClass().add("return-summary-value");

        Runnable updateRefundPreview = () -> updateRefundPreview(order, returnInputs, refundCaptionLabel, refundPreviewLabel);
        for (OrderItem item : order.getOrderItems()) {
            int returnableQuantity = item.getReturnableQuantity();
            if (returnableQuantity <= 0) {
                continue;
            }
            itemsBox.getChildren().add(createReturnItemCard(item, returnableQuantity, updateRefundPreview, returnInputs));
        }

        if (returnInputs.isEmpty()) {
            context.toastService().showInfo("This order has no returnable items");
            return;
        }

        ScrollPane scrollPane = new ScrollPane(itemsBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPannable(true);
        scrollPane.getStyleClass().add("return-items-scroll");
        double visibleItemCount = Math.min(returnInputs.size(), 3);
        double itemsViewportHeight = Math.max(210, visibleItemCount * 136.0 + 12);
        scrollPane.setPrefViewportHeight(itemsViewportHeight);

        StackPane itemsPanel = new StackPane(scrollPane);
        itemsPanel.getStyleClass().add("return-items-panel");

        VBox refundSummaryCard = new VBox(4, refundCaptionLabel, refundPreviewLabel);
        refundSummaryCard.getStyleClass().add("return-summary-card");

        TextArea reasonArea = new TextArea();
        reasonArea.setPromptText("Return Reason");
        reasonArea.setWrapText(true);
        reasonArea.setPrefRowCount(3);
        reasonArea.getStyleClass().add("return-reason-area");

        Label reasonLabel = new Label("Return Reason");
        reasonLabel.getStyleClass().add("return-section-label");
        VBox reasonBox = new VBox(8, reasonLabel, reasonArea);

        Button confirmBtn = new Button("Confirm Return");
        confirmBtn.getStyleClass().addAll("button", "primary-button");
        confirmBtn.setOnAction(event -> {
            Map<Long, Integer> returnQuantities = new LinkedHashMap<>();
            for (Map.Entry<Long, QuantityStepper> entry : returnInputs.entrySet()) {
                entry.getValue().commitEditorText(updateRefundPreview);
                if (entry.getValue().getValue() > 0) {
                    returnQuantities.put(entry.getKey(), entry.getValue().getValue());
                }
            }

            try {
                context.orderService().returnOrderItems(order.getId(), user.getId(), returnQuantities, reasonArea.getText());
                context.toastService().showSuccess("Return processed.");
                dialog.close();
                if (onSuccess != null) {
                    onSuccess.run();
                }
            } catch (Exception ex) {
                handleError(context, ex);
            }
        });

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().addAll("button", "close-button");
        closeBtn.setOnAction(event -> dialog.close());

        HBox actionRow = new HBox(10, closeBtn, confirmBtn);
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(title, helper, itemsPanel, refundSummaryCard, reasonBox, actionRow);

        double dialogHeight = Math.min(760, 370 + itemsViewportHeight);
        Scene scene = new Scene(root, 720, dialogHeight);
        applyApplicationStyles(scene);
        dialog.setScene(scene);
        DialogSupport.preventInitialFieldFocus(dialog, root);
        dialog.showAndWait();
    }

    private static VBox createOverviewCard(Order order) {
        Label statusValueLabel = StatusBadgeFactory.order(order.getStatus());
        HBox statusBadgeWrap = new HBox(statusValueLabel);
        statusBadgeWrap.setAlignment(Pos.CENTER_LEFT);

        javafx.scene.layout.GridPane overviewGrid = DialogFormFactory.grid();
        overviewGrid.add(DialogFormFactory.fieldBlock("Date", createOrderDetailValue(formatDateTimeWithSeconds(order.getCreatedAt()), true), null), 0, 0);
        overviewGrid.add(DialogFormFactory.fieldBlock("Status", statusBadgeWrap, null), 1, 0);
        overviewGrid.add(DialogFormFactory.fieldBlock("Created By", createOrderDetailValue(order.getCreatedByDisplayName(), false), null), 0, 1);
        overviewGrid.add(DialogFormFactory.fieldBlock("Customer", createOrderDetailValue(order.getCustomerDisplayName(), false), null), 1, 1);
        overviewGrid.add(DialogFormFactory.fieldBlock("Customer Phone", createOrderDetailValue(order.getCustomerPhoneDisplay(), false), null), 0, 2);
        overviewGrid.add(DialogFormFactory.fieldBlock("Refunded", createOrderDetailValue(formatVnd(order.getRefundedAmount()), false), null), 1, 2);
        return DialogFormFactory.section("Overview", overviewGrid);
    }

    private static TableView<OrderItem> createItemsTable(Order order) {
        TableView<OrderItem> table = new TableView<>();
        table.getStyleClass().addAll("import-dialog-table", "order-detail-table");
        TableViewSupport.prepareNonReorderableTable(table);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<OrderItem, String> pNameCol = new TableColumn<>("Product");
        pNameCol.setPrefWidth(340);
        pNameCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getProductDisplayName()));

        TableColumn<OrderItem, String> priceCol = new TableColumn<>("Net Unit");
        priceCol.setPrefWidth(150);
        priceCol.setCellValueFactory(cell -> new SimpleStringProperty(formatVnd(cell.getValue().getPrice())));

        TableColumn<OrderItem, Integer> qtyCol = new TableColumn<>("Qty");
        qtyCol.setPrefWidth(90);
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<OrderItem, String> returnCol = new TableColumn<>("Returned");
        returnCol.setPrefWidth(130);
        returnCol.setCellValueFactory(cell -> new SimpleStringProperty(
            (cell.getValue().getReturnedQuantity() != null ? cell.getValue().getReturnedQuantity() : 0)
                + " / "
                + cell.getValue().getQuantity()
        ));

        TableColumn<OrderItem, String> discountCol = new TableColumn<>("Discount");
        discountCol.setPrefWidth(130);
        discountCol.setCellValueFactory(cell -> new SimpleStringProperty(formatVnd(MoneySupport.add(
            cell.getValue().getLinePromotionDiscountAmountSnapshot(),
            cell.getValue().getOrderLevelDiscountAllocatedAmountSnapshot()
        ))));

        TableColumn<OrderItem, String> subTotalCol = new TableColumn<>("Net");
        subTotalCol.setPrefWidth(160);
        subTotalCol.setCellValueFactory(cell -> new SimpleStringProperty(formatVnd(cell.getValue().getLineNetAmount())));

        table.getColumns().addAll(pNameCol, priceCol, qtyCol, returnCol, discountCol, subTotalCol);
        table.setItems(FXCollections.observableArrayList(order.getOrderItems() == null ? List.of() : order.getOrderItems()));
        table.setMinHeight(260);
        table.setPrefHeight(320);
        return table;
    }

    private static VBox createTotalsCard(Order order) {
        FlowPane totalsPane = new FlowPane(10, 10);
        totalsPane.getChildren().addAll(
            createOrderTotalChip("Gross Subtotal", formatVnd(order.getGrossSubtotalSnapshot()), "neutral"),
            createOrderTotalChip("Discounts", formatVnd(order.getDiscountTotalSnapshot()), "primary"),
            createOrderTotalChip("Amount Paid", formatVnd(order.getTotalPrice()), "danger"),
            createOrderTotalChip("Net After Refunds", formatVnd(order.getNetTotal()), "success")
        );
        return DialogFormFactory.section("Totals", totalsPane);
    }

    private static VBox createStatusNoteCard(Order order) {
        Label noteLabel = new Label(order.getStatusNote() == null || order.getStatusNote().isBlank() ? "-" : order.getStatusNote());
        noteLabel.setWrapText(true);
        noteLabel.getStyleClass().add("import-detail-note-text");
        return DialogFormFactory.section("Status Note", noteLabel);
    }

    private static Label createOrderDetailValue(String text, boolean strong) {
        Label label = new Label(text == null || text.isBlank() ? "-" : text);
        label.setWrapText(true);
        label.getStyleClass().add(strong ? "order-detail-value-strong" : "order-detail-value");
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private static VBox createOrderTotalChip(String labelText, String valueText, String tone) {
        Label label = new Label(labelText);
        label.getStyleClass().add("order-total-chip-label");
        Label value = new Label(valueText);
        value.getStyleClass().add("order-total-chip-value");
        VBox chip = new VBox(3, label, value);
        chip.getStyleClass().addAll("order-total-chip", "order-total-chip-" + tone);
        return chip;
    }

    private static VBox createReturnItemCard(
        OrderItem item,
        int returnableQuantity,
        Runnable updateRefundPreview,
        Map<Long, QuantityStepper> returnInputs
    ) {
        Label productLabel = new Label(item.getProductDisplayName());
        productLabel.getStyleClass().add("return-item-name");

        FlowPane metaPane = new FlowPane(8, 8);
        metaPane.getStyleClass().add("return-item-meta-row");
        metaPane.getChildren().addAll(
            createReturnItemChip(MessageFormat.format("Ordered {0}", item.getQuantity()), false),
            createReturnItemChip(MessageFormat.format("Returned {0}", (item.getReturnedQuantity() != null ? item.getReturnedQuantity() : 0)), false),
            createReturnItemChip(MessageFormat.format("Returnable {0}", returnableQuantity), true),
            createReturnItemChip(MessageFormat.format("Net Unit {0}", formatVnd(item.getPrice())), false)
        );

        QuantityStepper qtyStepper = new QuantityStepper(0, returnableQuantity, updateRefundPreview);
        returnInputs.put(item.getId(), qtyStepper);

        Label qtyLabel = new Label("Return Qty");
        qtyLabel.getStyleClass().add("return-item-qty-label");

        VBox qtyBox = new VBox(8, qtyLabel, qtyStepper);
        qtyBox.getStyleClass().add("return-item-qty-box");
        qtyBox.setAlignment(Pos.CENTER);

        VBox infoBox = new VBox(10, productLabel, metaPane);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        HBox contentRow = new HBox(16, infoBox, qtyBox);
        contentRow.setAlignment(Pos.CENTER_LEFT);

        VBox itemCard = new VBox(contentRow);
        itemCard.getStyleClass().add("return-item-card");
        return itemCard;
    }

    private static Label createReturnItemChip(String text, boolean emphasized) {
        Label chip = new Label(text);
        chip.getStyleClass().add(emphasized ? "return-item-chip-strong" : "return-item-chip");
        return chip;
    }

    private static void updateRefundPreview(
        Order order,
        Map<Long, QuantityStepper> returnInputs,
        Label refundCaptionLabel,
        Label refundPreviewLabel
    ) {
        BigDecimal refundTotal = BigDecimal.ZERO;
        int selectedQuantity = 0;
        for (OrderItem item : order.getOrderItems()) {
            QuantityStepper stepper = returnInputs.get(item.getId());
            if (stepper == null) {
                continue;
            }
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
        refundCaptionLabel.setText(selectedQuantity > 0
            ? MessageFormat.format("Estimated Refund • {0} item(s)", selectedQuantity)
            : "Estimated Refund");
        refundPreviewLabel.setText(formatVnd(refundTotal));
    }

    private static void closeAndRefresh(Stage dialog, Runnable onChanged) {
        if (onChanged != null) {
            onChanged.run();
        }
        dialog.close();
    }

    private static String formatDateTimeWithSeconds(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_SECONDS_FORMATTER) : "-";
    }

    private static String formatVnd(BigDecimal amount) {
        return String.format("%,.0f VND", MoneySupport.normalize(amount).doubleValue());
    }

    private static void applyApplicationStyles(Scene scene) {
        scene.getStylesheets().add(Objects.requireNonNull(OrderDialog.class.getResource("/application.css")).toExternalForm());
    }

    private static void handleError(Context context, Throwable throwable) {
        if (context != null && context.errorHandler() != null) {
            context.errorHandler().accept(throwable);
        } else if (throwable != null) {
            throw new RuntimeException(throwable);
        }
    }

}
