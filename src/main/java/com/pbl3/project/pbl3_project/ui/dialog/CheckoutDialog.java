package com.pbl3.project.pbl3_project.ui.dialog;

import com.pbl3.project.pbl3_project.entity.PaymentMethod;
import com.pbl3.project.pbl3_project.service.MoneySupport;
import com.pbl3.project.pbl3_project.service.PromotionService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class CheckoutDialog {

    private CheckoutDialog() {
    }

    public record Selection(
        PaymentMethod paymentMethod,
        boolean printReceipt,
        Long selectedOrderPromotionId,
        BigDecimal amountDue
    ) {
    }

    public record QrPaymentAvailability(boolean available, String message) {
        public static QrPaymentAvailability ready() {
            return new QrPaymentAvailability(true, "SePay QR payment is ready.");
        }

        public static QrPaymentAvailability checking() {
            return new QrPaymentAvailability(false, "Checking SePay QR payment settings...");
        }

        public static QrPaymentAvailability unavailable(String message) {
            return new QrPaymentAvailability(false, message);
        }
    }

    public static void show(
        Stage owner,
        BigDecimal subtotalAmount,
        List<PromotionService.OrderPromotionPreview> eligibleOrderPromotions,
        Consumer<Selection> onConfirm
    ) {
        show(owner, subtotalAmount, eligibleOrderPromotions, null, onConfirm);
    }

    public static void show(
        Stage owner,
        BigDecimal subtotalAmount,
        List<PromotionService.OrderPromotionPreview> eligibleOrderPromotions,
        Supplier<QrPaymentAvailability> qrPaymentAvailabilitySupplier,
        Consumer<Selection> onConfirm
    ) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Checkout");

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.getStyleClass().add("pos-checkout-root");

        Label title = new Label("Checkout");
        title.getStyleClass().add("pos-checkout-title");
        VBox header = new VBox(title);
        header.getStyleClass().add("pos-checkout-header");

        Label subtotalValueLabel = new Label(formatVnd(subtotalAmount));
        Label promotionDiscountValueLabel = new Label(formatVnd(BigDecimal.ZERO));
        Label totalValueLabel = new Label(formatVnd(subtotalAmount));

        VBox summaryCard = new VBox(
            10,
            createMetricRow("Subtotal", subtotalValueLabel, false),
            createMetricRow("Order Discount", promotionDiscountValueLabel, false),
            createMetricRow("Total Due", totalValueLabel, true)
        );
        summaryCard.getStyleClass().addAll("pos-checkout-card", "pos-checkout-summary-card");

        Label promotionLabel = new Label("Order Promotion");
        promotionLabel.getStyleClass().add("pos-checkout-section-title");

        java.util.concurrent.atomic.AtomicReference<QrPaymentAvailability> qrAvailabilityRef =
            new java.util.concurrent.atomic.AtomicReference<>(
                qrPaymentAvailabilitySupplier == null ? QrPaymentAvailability.ready() : QrPaymentAvailability.checking()
            );

        ComboBox<PaymentMethod> methodCombo = new ComboBox<>();
        methodCombo.getItems().addAll(PaymentMethod.values());
        methodCombo.setValue(PaymentMethod.CASH);
        methodCombo.getStyleClass().add("pos-checkout-field");
        methodCombo.getStyleClass().add("pos-checkout-combo-box");
        methodCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(PaymentMethod value) {
                return value == null ? "" : paymentMethodLabel(value);
            }

            @Override
            public PaymentMethod fromString(String string) {
                return null;
            }
        });
        methodCombo.setMaxWidth(Double.MAX_VALUE);
        methodCombo.setCellFactory(listView -> createPaymentMethodCell(qrAvailabilityRef));
        methodCombo.setButtonCell(createPaymentMethodCell(qrAvailabilityRef));

        ComboBox<PromotionService.OrderPromotionPreview> orderPromotionCombo = createOrderPromotionCombo(eligibleOrderPromotions);

        Label paymentLabel = new Label("Payment Method");
        paymentLabel.getStyleClass().add("pos-checkout-section-title");

        VBox paymentCard = new VBox(12);
        paymentCard.getStyleClass().add("pos-checkout-card");
        GridPane paymentGrid = new GridPane();
        paymentGrid.setHgap(14);
        paymentGrid.setVgap(12);
        paymentGrid.add(createFieldBlock(promotionLabel, orderPromotionCombo), 0, 0);
        paymentGrid.add(createFieldBlock(paymentLabel, methodCombo), 1, 0);
        Label qrStatusLabel = new Label();
        qrStatusLabel.setWrapText(true);
        qrStatusLabel.setManaged(false);
        qrStatusLabel.setVisible(false);
        qrStatusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: -app-text-secondary;");
        paymentCard.getChildren().addAll(paymentGrid, qrStatusLabel);

        VBox cashBox = new VBox(12);
        cashBox.getStyleClass().addAll("pos-checkout-card", "pos-checkout-cash-box");
        cashBox.setAlignment(Pos.CENTER_LEFT);
        Label cashLabel = new Label("Cash Received");
        cashLabel.getStyleClass().add("pos-checkout-section-title");
        TextField givenField = new TextField();
        givenField.setPromptText("Amount received");
        givenField.getStyleClass().add("pos-checkout-field");
        givenField.setMaxWidth(Double.MAX_VALUE);

        Label changeLbl = new Label(formatVnd(BigDecimal.ZERO));
        changeLbl.getStyleClass().add("pos-checkout-change-value");
        HBox changeRow = createMetricRow("Change Due", changeLbl, false);
        cashBox.getChildren().addAll(createFieldBlock(cashLabel, givenField), changeRow);

        CheckBox printReceiptCb = new CheckBox("Print Receipt (PDF)");
        printReceiptCb.setSelected(true);
        printReceiptCb.getStyleClass().add("pos-checkout-checkbox");
        VBox optionsCard = new VBox(printReceiptCb);
        optionsCard.getStyleClass().addAll("pos-checkout-card", "pos-checkout-options-card");

        Button confirmBtn = new Button("Complete Payment");
        confirmBtn.getStyleClass().add("pos-dialog-primary-button");
        confirmBtn.setDisable(true);
        confirmBtn.setMaxWidth(Double.MAX_VALUE);

        Supplier<BigDecimal> effectiveTotalSupplier = () -> {
            PromotionService.OrderPromotionPreview selectedPromotion = orderPromotionCombo.getValue();
            return selectedPromotion != null ? selectedPromotion.discountedTotal() : subtotalAmount;
        };

        Runnable updateState = () -> {
            PromotionService.OrderPromotionPreview selectedPromotion = orderPromotionCombo.getValue();
            BigDecimal discountAmount = selectedPromotion != null ? selectedPromotion.discountAmount() : MoneySupport.ZERO;
            BigDecimal effectiveTotal = effectiveTotalSupplier.get();
            promotionDiscountValueLabel.setText(formatVnd(discountAmount));
            totalValueLabel.setText(formatVnd(effectiveTotal));

            boolean isCash = methodCombo.getValue() == PaymentMethod.CASH;
            boolean isQr = methodCombo.getValue() == PaymentMethod.QR;
            QrPaymentAvailability qrAvailability = sanitizeAvailability(qrAvailabilityRef.get());
            cashBox.setVisible(isCash);
            cashBox.setManaged(isCash);
            confirmBtn.setText(isQr ? "Generate QR" : "Complete Payment");
            boolean shouldShowQrStatus = isQr || !qrAvailability.available();
            qrStatusLabel.setManaged(shouldShowQrStatus);
            qrStatusLabel.setVisible(shouldShowQrStatus);
            qrStatusLabel.setText(qrAvailability.message());
            qrStatusLabel.setStyle(qrAvailability.available()
                ? "-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: -app-success;"
                : "-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: -app-danger;");
            if (!isCash) {
                confirmBtn.setDisable(isQr && !qrAvailability.available());
            } else {
                updateCashState(givenField, effectiveTotal, changeLbl, confirmBtn);
            }
        };

        methodCombo.setOnAction(event -> updateState.run());
        orderPromotionCombo.setOnAction(event -> updateState.run());
        givenField.textProperty().addListener((obs, oldValue, newValue) ->
            updateCashState(givenField, effectiveTotalSupplier.get(), changeLbl, confirmBtn)
        );

        updateState.run();

        confirmBtn.setOnAction(event -> {
            PromotionService.OrderPromotionPreview selectedPromotion = orderPromotionCombo.getValue();
            if (onConfirm != null) {
                onConfirm.accept(new Selection(
                    methodCombo.getValue(),
                    printReceiptCb.isSelected(),
                    selectedPromotion != null && selectedPromotion.promotion() != null
                        ? selectedPromotion.promotion().getId()
                        : null,
                    effectiveTotalSupplier.get()
                ));
            }
            dialog.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("pos-dialog-secondary-button", "dialog-cancel-button");
        cancelBtn.setMaxWidth(Double.MAX_VALUE);
        cancelBtn.setOnAction(event -> dialog.close());

        HBox actionRow = new HBox(12, cancelBtn, confirmBtn);
        actionRow.setAlignment(Pos.CENTER);
        HBox.setHgrow(cancelBtn, Priority.ALWAYS);
        HBox.setHgrow(confirmBtn, Priority.ALWAYS);
        actionRow.getStyleClass().add("pos-checkout-actions");

        root.getChildren().addAll(
            header,
            summaryCard,
            paymentCard,
            cashBox,
            optionsCard
        );

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.getStyleClass().add("pos-checkout-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox shell = new VBox(scrollPane, actionRow);
        shell.getStyleClass().add("pos-checkout-shell");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        Scene scene = new Scene(shell, 560, 720);
        if (owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        dialog.setScene(scene);
        dialog.setResizable(true);
        dialog.setMinWidth(560);
        dialog.setMinHeight(620);
        dialog.setOnShown(event -> runQrPaymentAvailabilityCheck(
            dialog,
            qrPaymentAvailabilitySupplier,
            qrAvailabilityRef,
            methodCombo,
            updateState
        ));
        dialog.showAndWait();
    }

    private static ListCell<PaymentMethod> createPaymentMethodCell(
        java.util.concurrent.atomic.AtomicReference<QrPaymentAvailability> qrAvailabilityRef
    ) {
        return new ListCell<>() {
            {
                getStyleClass().add("pos-checkout-popup-cell");
            }

            @Override
            protected void updateItem(PaymentMethod item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setDisable(false);
                    return;
                }
                QrPaymentAvailability availability = sanitizeAvailability(qrAvailabilityRef.get());
                boolean disabledQr = item == PaymentMethod.QR && !availability.available();
                setText(disabledQr ? paymentMethodLabel(item) + " (Unavailable)" : paymentMethodLabel(item));
                setDisable(disabledQr);
            }
        };
    }

    private static void runQrPaymentAvailabilityCheck(
        Stage dialog,
        Supplier<QrPaymentAvailability> qrPaymentAvailabilitySupplier,
        java.util.concurrent.atomic.AtomicReference<QrPaymentAvailability> qrAvailabilityRef,
        ComboBox<PaymentMethod> methodCombo,
        Runnable updateState
    ) {
        if (qrPaymentAvailabilitySupplier == null) {
            return;
        }
        javafx.concurrent.Task<QrPaymentAvailability> task = new javafx.concurrent.Task<>() {
            @Override
            protected QrPaymentAvailability call() {
                return sanitizeAvailability(qrPaymentAvailabilitySupplier.get());
            }
        };
        task.setOnSucceeded(event -> applyQrAvailability(
            dialog,
            qrAvailabilityRef,
            methodCombo,
            updateState,
            task.getValue()
        ));
        task.setOnFailed(event -> applyQrAvailability(
            dialog,
            qrAvailabilityRef,
            methodCombo,
            updateState,
            QrPaymentAvailability.unavailable(
                task.getException() != null && task.getException().getMessage() != null
                    ? task.getException().getMessage()
                    : "Could not verify SePay QR payment settings."
            )
        ));
        Thread worker = new Thread(task, "checkout-qr-availability");
        worker.setDaemon(true);
        worker.start();
    }

    private static void applyQrAvailability(
        Stage dialog,
        java.util.concurrent.atomic.AtomicReference<QrPaymentAvailability> qrAvailabilityRef,
        ComboBox<PaymentMethod> methodCombo,
        Runnable updateState,
        QrPaymentAvailability availability
    ) {
        if (!dialog.isShowing()) {
            return;
        }
        qrAvailabilityRef.set(sanitizeAvailability(availability));
        PaymentMethod selectedMethod = methodCombo.getValue();
        methodCombo.getItems().setAll(PaymentMethod.values());
        methodCombo.setValue(selectedMethod != null ? selectedMethod : PaymentMethod.CASH);
        updateState.run();
    }

    private static QrPaymentAvailability sanitizeAvailability(QrPaymentAvailability availability) {
        if (availability == null) {
            return QrPaymentAvailability.unavailable("Could not verify SePay QR payment settings.");
        }
        String message = availability.message();
        if (message == null || message.isBlank()) {
            message = availability.available()
                ? "SePay QR payment is ready."
                : "SePay QR payment is not available.";
        }
        return new QrPaymentAvailability(availability.available(), message);
    }

    private static ComboBox<PromotionService.OrderPromotionPreview> createOrderPromotionCombo(
        List<PromotionService.OrderPromotionPreview> eligibleOrderPromotions
    ) {
        ComboBox<PromotionService.OrderPromotionPreview> combo = new ComboBox<>();
        combo.getItems().add(null);
        if (eligibleOrderPromotions != null) {
            combo.getItems().addAll(eligibleOrderPromotions);
        }
        combo.setValue(null);
        combo.getStyleClass().add("pos-checkout-field");
        combo.getStyleClass().add("pos-checkout-combo-box");
        combo.setMaxWidth(Double.MAX_VALUE);

        StringConverter<PromotionService.OrderPromotionPreview> promotionConverter = new StringConverter<>() {
            @Override
            public String toString(PromotionService.OrderPromotionPreview value) {
                return value == null ? "No order promotion" : value.displayLabel();
            }

            @Override
            public PromotionService.OrderPromotionPreview fromString(String string) {
                return null;
            }
        };
        combo.setConverter(promotionConverter);
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(PromotionService.OrderPromotionPreview item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : promotionConverter.toString(item));
            }
        });
        combo.setCellFactory(listView -> new ListCell<>() {
            {
                getStyleClass().add("pos-checkout-popup-cell");
            }

            @Override
            protected void updateItem(PromotionService.OrderPromotionPreview item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : promotionConverter.toString(item));
            }
        });
        return combo;
    }

    private static HBox createMetricRow(String labelText, Label valueLabel, boolean emphasized) {
        Label label = new Label(labelText);
        label.getStyleClass().add("pos-checkout-metric-label");

        valueLabel.getStyleClass().add(emphasized ? "pos-checkout-total-value" : "pos-checkout-metric-value");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(8, label, spacer, valueLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static VBox createFieldBlock(Label label, javafx.scene.Node field) {
        VBox block = new VBox(7, label, field);
        block.getStyleClass().add("pos-checkout-field-block");
        block.setMaxWidth(Double.MAX_VALUE);
        if (field instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        GridPane.setHgrow(block, Priority.ALWAYS);
        GridPane.setFillWidth(block, true);
        return block;
    }

    private static void updateCashState(
        TextField givenField,
        BigDecimal effectiveTotal,
        Label changeLbl,
        Button confirmBtn
    ) {
        try {
            BigDecimal given = parseMoneyInput(givenField.getText());
            BigDecimal change = MoneySupport.subtract(given, effectiveTotal);
            changeLbl.setText(formatVnd(change));
            if (given.compareTo(effectiveTotal) >= 0) {
                confirmBtn.setDisable(false);
                changeLbl.getStyleClass().remove("pos-checkout-change-negative");
            } else {
                confirmBtn.setDisable(true);
                addChangeNegativeClass(changeLbl);
            }
        } catch (Exception ex) {
            confirmBtn.setDisable(true);
            changeLbl.setText(givenField.getText() == null || givenField.getText().isBlank() ? formatVnd(BigDecimal.ZERO) : "Invalid amount");
            addChangeNegativeClass(changeLbl);
        }
    }

    private static void addChangeNegativeClass(Label changeLbl) {
        if (!changeLbl.getStyleClass().contains("pos-checkout-change-negative")) {
            changeLbl.getStyleClass().add("pos-checkout-change-negative");
        }
    }

    private static BigDecimal parseMoneyInput(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        return MoneySupport.normalize(new BigDecimal(value.replace(",", "").trim()));
    }

    private static String formatVnd(BigDecimal amount) {
        return String.format("%,.0f VND", MoneySupport.normalize(amount).doubleValue());
    }

    private static String paymentMethodLabel(PaymentMethod method) {
        return switch (method) {
            case CASH -> "Cash";
            case CARD -> "Card";
            case QR -> "QR / VietQR";
        };
    }
}
