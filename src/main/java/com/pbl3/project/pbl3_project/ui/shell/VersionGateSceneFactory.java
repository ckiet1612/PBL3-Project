package com.pbl3.project.pbl3_project.ui.shell;

import com.pbl3.project.pbl3_project.service.ApplicationVersionService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public final class VersionGateSceneFactory {

    public record Context(
        Stage stage,
        ApplicationVersionService.VersionCheckResult result,
        Runnable retryAction
    ) {
    }

    public void show(Context context) {
        StackPane root = new StackPane();
        root.getStyleClass().addAll("login-root", "version-gate-root");
        root.setPadding(new Insets(28));

        VBox card = new VBox(16);
        card.getStyleClass().add("version-gate-card");
        card.setMaxWidth(620);
        card.setAlignment(Pos.CENTER_LEFT);

        Label eyebrow = new Label("Update required");
        eyebrow.getStyleClass().add("version-gate-eyebrow");

        Label title = new Label("This client cannot continue");
        title.getStyleClass().add("version-gate-title");

        Label message = new Label(context.result().message());
        message.getStyleClass().add("version-gate-message");
        message.setWrapText(true);

        VBox details = new VBox(8,
            createDetailRow("Current version", context.result().currentVersion()),
            createDetailRow("Required version", context.result().requiredVersion() == null ? "Not available" : context.result().requiredVersion())
        );
        details.getStyleClass().add("version-gate-details");

        Button retryButton = new Button("Retry");
        retryButton.getStyleClass().addAll("primary-button", "version-gate-primary-button");
        retryButton.setOnAction(event -> {
            if (context.retryAction() != null) {
                context.retryAction().run();
            }
        });

        Button exitButton = new Button("Exit");
        exitButton.getStyleClass().addAll("dialog-cancel-button", "version-gate-secondary-button");
        exitButton.setOnAction(event -> Platform.exit());

        HBox actions = new HBox(12, exitButton, retryButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.getStyleClass().add("version-gate-actions");

        card.getChildren().addAll(eyebrow, title, message, details, actions);
        root.getChildren().add(card);

        Scene scene = new Scene(root, 760, 460);
        applyApplicationStyles(scene);
        context.stage().setScene(scene);
        context.stage().setMinWidth(700);
        context.stage().setMinHeight(420);
        context.stage().centerOnScreen();
    }

    private HBox createDetailRow(String labelText, String valueText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("version-gate-detail-label");
        Label value = new Label(valueText == null || valueText.isBlank() ? "-" : valueText);
        value.getStyleClass().add("version-gate-detail-value");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(12, label, spacer, value);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("version-gate-detail-row");
        return row;
    }

    private void applyApplicationStyles(Scene scene) {
        String stylesheet = getClass().getResource("/application.css").toExternalForm();
        if (!scene.getStylesheets().contains(stylesheet)) {
            scene.getStylesheets().add(stylesheet);
        }
    }
}
