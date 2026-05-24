package com.pbl3.project.pbl3_project.ui.scene.pos;

import com.pbl3.project.pbl3_project.service.SalesShiftService;
import com.pbl3.project.pbl3_project.ui.scene.SceneUiSupport;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class PosShiftCardSupport {
    private PosShiftCardSupport() {
    }

    public static VBox createShiftCard(
        Label statusLabel,
        Label openedLabel,
        Label openingCashLabel,
        Label salesLabel,
        Label refundsLabel,
        Label expensesLabel,
        Label expectedCashLabel,
        Button openButton,
        Button closeButton
    ) {
        Label title = new Label("Sales Shift");
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: 900; -fx-text-fill: -app-text-primary;");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 900; -fx-padding: 4 10; -fx-background-radius: 999;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(8, title, statusLabel, spacer, openButton, closeButton);
        header.setAlignment(Pos.CENTER_LEFT);

        HBox metrics = new HBox(
            12,
            createShiftMetricBlock("Opened", openedLabel),
            createShiftMetricBlock("Sales", salesLabel),
            createShiftMetricBlock("Expected", expectedCashLabel)
        );
        metrics.setAlignment(Pos.CENTER_LEFT);
        metrics.setMinWidth(0);
        for (Node child : metrics.getChildren()) {
            HBox.setHgrow(child, Priority.ALWAYS);
        }
        metrics.visibleProperty().bind(statusLabel.textProperty().isNotEqualTo("NO SHIFT"));
        metrics.managedProperty().bind(metrics.visibleProperty());

        VBox card = new VBox(6, header, metrics);
        card.setFillWidth(true);
        card.setStyle("""
            -fx-background-color: -app-surface;
            -fx-background-radius: 14;
            -fx-border-color: -app-border;
            -fx-border-radius: 14;
            -fx-padding: 10 12;
            """);
        return card;
    }

    public static void updateShiftCard(
        SceneUiSupport support,
        SalesShiftService.ShiftSummary summary,
        Label statusLabel,
        Label openedLabel,
        Label openingCashLabel,
        Label salesLabel,
        Label refundsLabel,
        Label expensesLabel,
        Label expectedCashLabel,
        Button openButton,
        Button closeButton
    ) {
        boolean hasOpenShift = summary != null;
        statusLabel.setText(hasOpenShift ? "OPEN #" + summary.shiftId() : "NO SHIFT");
        statusLabel.setStyle(hasOpenShift
            ? "-fx-font-size: 12px; -fx-font-weight: 900; -fx-padding: 4 10; -fx-background-radius: 999; -fx-background-color: -app-success-soft; -fx-text-fill: -app-success-hover;"
            : "-fx-font-size: 12px; -fx-font-weight: 900; -fx-padding: 4 10; -fx-background-radius: 999; -fx-background-color: -app-danger-soft; -fx-text-fill: -app-danger;"
        );
        openButton.setVisible(!hasOpenShift);
        openButton.setManaged(!hasOpenShift);
        closeButton.setVisible(hasOpenShift);
        closeButton.setManaged(hasOpenShift);

        if (!hasOpenShift) {
            openedLabel.setText("Required before checkout");
            openingCashLabel.setText("-");
            salesLabel.setText("-");
            refundsLabel.setText("-");
            expensesLabel.setText("-");
            expectedCashLabel.setText("-");
            return;
        }

        openedLabel.setText(support.formatDateTime(summary.openedAt()));
        openingCashLabel.setText(support.formatVnd(summary.openingCashAmount()));
        salesLabel.setText(support.formatVnd(summary.salesRevenue()));
        refundsLabel.setText(support.formatVnd(summary.refundAmount()));
        expensesLabel.setText(support.formatVnd(summary.expenseAmount()));
        expectedCashLabel.setText(support.formatVnd(summary.expectedCashAmount()));
    }

    public static void showPendingShiftState(
        Label statusLabel,
        Button openButton,
        Button closeButton,
        Button checkoutButton,
        String statusText,
        String activeButtonText
    ) {
        statusLabel.setText(statusText);
        statusLabel.setStyle(
            "-fx-font-size: 12px; -fx-font-weight: 900; -fx-padding: 4 10; "
                + "-fx-background-radius: 999; -fx-background-color: -app-primary-soft; -fx-text-fill: -app-primary;"
        );
        if (openButton.isVisible()) {
            openButton.setText(activeButtonText);
        }
        if (closeButton.isVisible()) {
            closeButton.setText(activeButtonText);
        }
        openButton.setDisable(true);
        closeButton.setDisable(true);
        checkoutButton.setDisable(true);
    }

    private static VBox createShiftMetricBlock(String label, Label valueLabel) {
        Label caption = new Label(label);
        caption.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: -app-text-muted;");
        valueLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 900; -fx-text-fill: -app-text-primary;");
        valueLabel.setText("-");
        valueLabel.setMaxWidth(Double.MAX_VALUE);
        valueLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        VBox block = new VBox(1, caption, valueLabel);
        block.setMinWidth(0);
        block.setMaxWidth(Double.MAX_VALUE);
        return block;
    }
}
