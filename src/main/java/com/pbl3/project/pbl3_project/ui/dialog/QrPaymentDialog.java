package com.pbl3.project.pbl3_project.ui.dialog;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.pbl3.project.pbl3_project.dto.payment.QrPaymentStatusDto;
import com.pbl3.project.pbl3_project.entity.QrPaymentStatus;
import com.pbl3.project.pbl3_project.service.MoneySupport;
import com.pbl3.project.pbl3_project.service.QrPaymentService;
import com.pbl3.project.pbl3_project.ui.util.DialogSupport;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Consumer;

public final class QrPaymentDialog {
    private static final int QR_IMAGE_SIZE = 214;
    private static final double DIALOG_WIDTH = 400;

    private QrPaymentDialog() {
    }

    public static void show(
        Stage owner,
        QrPaymentStatusDto initialStatus,
        QrPaymentService qrPaymentService,
        Consumer<QrPaymentStatusDto> onPaid,
        Consumer<Throwable> errorHandler
    ) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("QR Payment");

        Label title = new Label("QR / VietQR");
        title.getStyleClass().add("qr-payment-title");

        Label statusLabel = new Label("Waiting for payment...");
        statusLabel.getStyleClass().add("qr-payment-status");

        VBox header = new VBox(6, title, statusLabel);
        header.setAlignment(Pos.CENTER);

        ImageView qrImage = new ImageView(createQrImage(initialStatus.qrCode(), QR_IMAGE_SIZE));
        qrImage.setFitWidth(QR_IMAGE_SIZE);
        qrImage.setFitHeight(QR_IMAGE_SIZE);
        qrImage.setPreserveRatio(true);

        VBox qrImageFrame = new VBox(qrImage);
        qrImageFrame.setAlignment(Pos.CENTER);
        qrImageFrame.getStyleClass().add("qr-payment-image-frame");

        Label amountCaption = new Label("Amount due");
        amountCaption.getStyleClass().add("qr-payment-caption");

        Label amountLabel = new Label(formatVnd(initialStatus.amount()));
        amountLabel.getStyleClass().add("qr-payment-amount");

        VBox amountBox = new VBox(2, amountCaption, amountLabel);
        amountBox.setAlignment(Pos.CENTER);

        Label orderCodeLabel = new Label("Code " + initialStatus.orderCode());
        orderCodeLabel.getStyleClass().add("qr-payment-code");

        Label countdownLabel = new Label();
        countdownLabel.getStyleClass().add("qr-payment-countdown");

        VBox qrCard = new VBox(9, amountBox, qrImageFrame, orderCodeLabel, countdownLabel);
        qrCard.setAlignment(Pos.CENTER);
        qrCard.getStyleClass().add("qr-payment-card");

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().addAll("pos-dialog-secondary-button", "dialog-cancel-button");
        cancelButton.setMaxWidth(Double.MAX_VALUE);

        Button retryButton = new Button("Check Status");
        retryButton.getStyleClass().add("pos-dialog-primary-button");
        retryButton.setMaxWidth(Double.MAX_VALUE);

        HBox actions = new HBox(12, cancelButton, retryButton);
        actions.setAlignment(Pos.CENTER);
        actions.getStyleClass().add("qr-payment-actions");
        HBox.setHgrow(cancelButton, Priority.ALWAYS);
        HBox.setHgrow(retryButton, Priority.ALWAYS);

        VBox root = new VBox(12, header, qrCard, actions);
        root.setPadding(new Insets(18));
        root.setAlignment(Pos.TOP_CENTER);
        root.setPrefWidth(DIALOG_WIDTH);
        root.setMinWidth(DIALOG_WIDTH);
        root.getStyleClass().add("qr-payment-root");

        Scene scene = new Scene(root);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        } else {
            scene.getStylesheets().add(Objects.requireNonNull(QrPaymentDialog.class.getResource("/application.css")).toExternalForm());
        }
        dialog.setScene(scene);
        dialog.setResizable(false);

        final QrPaymentStatusDto[] currentStatus = {initialStatus};
        Timeline countdown = new Timeline(new KeyFrame(Duration.seconds(1), event ->
            updateCountdown(countdownLabel, currentStatus[0])
        ));
        countdown.setCycleCount(Timeline.INDEFINITE);

        final Timeline[] pollerRef = new Timeline[1];
        Timeline poller = new Timeline(new KeyFrame(Duration.seconds(2), event ->
            runStatusTask(
                () -> qrPaymentService.refreshPaymentStatus(initialStatus.id()),
                status -> handleStatus(dialog, statusLabel, cancelButton, retryButton, currentStatus, status, pollerRef[0], countdown, onPaid),
                errorHandler,
                retryButton,
                "Checking..."
            )
        ));
        pollerRef[0] = poller;
        poller.setCycleCount(Timeline.INDEFINITE);

        retryButton.setOnAction(event ->
            runStatusTask(
                () -> qrPaymentService.refreshPaymentStatus(initialStatus.id()),
                status -> handleStatus(dialog, statusLabel, cancelButton, retryButton, currentStatus, status, poller, countdown, onPaid),
                errorHandler,
                retryButton,
                "Checking..."
            )
        );
        cancelButton.setOnAction(event -> cancelOrClose(dialog, qrPaymentService, currentStatus[0], errorHandler, poller, countdown));
        dialog.setOnCloseRequest(event -> {
            if (isPending(currentStatus[0])) {
                event.consume();
                cancelOrClose(dialog, qrPaymentService, currentStatus[0], errorHandler, poller, countdown);
            }
        });
        dialog.setOnShown(event -> {
            updateCountdown(countdownLabel, currentStatus[0]);
            countdown.play();
            poller.play();
        });
        dialog.setOnHidden(event -> {
            poller.stop();
            countdown.stop();
        });

        dialog.show();
    }

    private static void runStatusTask(
        StatusSupplier supplier,
        Consumer<QrPaymentStatusDto> onSuccess,
        Consumer<Throwable> errorHandler,
        Button button,
        String runningText
    ) {
        if (button.isDisabled()) {
            return;
        }
        String originalText = button.getText();
        button.setDisable(true);
        button.setText(runningText);
        Task<QrPaymentStatusDto> task = new Task<>() {
            @Override
            protected QrPaymentStatusDto call() {
                return supplier.get();
            }
        };
        task.setOnSucceeded(event -> {
            button.setDisable(false);
            button.setText(originalText);
            onSuccess.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            button.setDisable(false);
            button.setText(originalText);
            if (errorHandler != null) {
                errorHandler.accept(task.getException());
            }
        });
        Thread worker = new Thread(task, "qr-payment-status");
        worker.setDaemon(true);
        worker.start();
    }

    private static void handleStatus(
        Stage dialog,
        Label statusLabel,
        Button cancelButton,
        Button retryButton,
        QrPaymentStatusDto[] currentStatus,
        QrPaymentStatusDto status,
        Timeline poller,
        Timeline countdown,
        Consumer<QrPaymentStatusDto> onPaid
    ) {
        currentStatus[0] = status;
        if (status.status() == QrPaymentStatus.PAID || status.status() == QrPaymentStatus.ORDER_CREATED) {
            poller.stop();
            countdown.stop();
            statusLabel.setText("Payment received. Finalizing order...");
            dialog.close();
            if (onPaid != null) {
                onPaid.accept(status);
            }
        } else if (status.status() == QrPaymentStatus.EXPIRED) {
            poller.stop();
            countdown.stop();
            statusLabel.setText("QR payment expired.");
            cancelButton.setText("Close");
            retryButton.setDisable(true);
        } else if (status.status() == QrPaymentStatus.CANCELLED) {
            poller.stop();
            countdown.stop();
            statusLabel.setText("QR payment cancelled.");
            cancelButton.setText("Close");
            retryButton.setDisable(true);
        } else if (status.status() == QrPaymentStatus.ORDER_CREATING) {
            statusLabel.setText("Finalizing order...");
            cancelButton.setDisable(true);
            retryButton.setDisable(true);
        } else if (status.status() == QrPaymentStatus.PAID_ORDER_FAILED) {
            poller.stop();
            countdown.stop();
            statusLabel.setText("Payment received, but order creation failed.");
            cancelButton.setText("Close");
            retryButton.setDisable(true);
        }
    }

    private static void cancelOrClose(
        Stage dialog,
        QrPaymentService qrPaymentService,
        QrPaymentStatusDto status,
        Consumer<Throwable> errorHandler,
        Timeline poller,
        Timeline countdown
    ) {
        if (!isPending(status)) {
            dialog.close();
            return;
        }
        if (!DialogSupport.showConfirm(dialog, "Cancel QR payment", "Cancel this pending QR payment?")) {
            return;
        }
        poller.stop();
        countdown.stop();
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                qrPaymentService.cancelPayment(status.id());
                return null;
            }
        };
        task.setOnSucceeded(event -> dialog.close());
        task.setOnFailed(event -> {
            if (errorHandler != null) {
                errorHandler.accept(task.getException());
            }
        });
        Thread worker = new Thread(task, "qr-payment-cancel");
        worker.setDaemon(true);
        worker.start();
    }

    private static void updateCountdown(Label countdownLabel, QrPaymentStatusDto status) {
        if (status == null || status.expiresAt() == null) {
            countdownLabel.setText("");
            return;
        }
        long seconds = java.time.Duration.between(LocalDateTime.now(), status.expiresAt()).toSeconds();
        if (seconds <= 0) {
            countdownLabel.setText("Expired");
            return;
        }
        countdownLabel.setText("Expires in " + (seconds / 60) + ":" + String.format("%02d", seconds % 60));
    }

    private static boolean isPending(QrPaymentStatusDto status) {
        return status != null && status.status() == QrPaymentStatus.PENDING;
    }

    private static Image createQrImage(String payload, int size) {
        if (payload != null && (payload.startsWith("http://") || payload.startsWith("https://"))) {
            return new Image(payload, size, size, true, true, true);
        }
        try {
            BitMatrix matrix = new QRCodeWriter().encode(payload == null ? "" : payload, BarcodeFormat.QR_CODE, size, size);
            WritableImage image = new WritableImage(size, size);
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    image.getPixelWriter().setColor(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return image;
        } catch (Exception ex) {
            WritableImage fallback = new WritableImage(size, size);
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    fallback.getPixelWriter().setColor(x, y, Color.WHITE);
                }
            }
            return fallback;
        }
    }

    private static String formatVnd(BigDecimal amount) {
        return String.format("%,.0f VND", MoneySupport.normalize(amount).doubleValue());
    }

    @FunctionalInterface
    private interface StatusSupplier {
        QrPaymentStatusDto get();
    }
}
