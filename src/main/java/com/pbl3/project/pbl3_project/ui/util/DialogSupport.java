package com.pbl3.project.pbl3_project.ui.util;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import java.util.Optional;

public final class DialogSupport {

    private DialogSupport() {
    }

    public static void preventInitialFieldFocus(Stage dialog, Parent root) {
        if (dialog == null || root == null) {
            return;
        }
        root.setFocusTraversable(true);
        javafx.event.EventHandler<WindowEvent> existingOnShown = dialog.getOnShown();
        dialog.setOnShown(event -> {
            if (existingOnShown != null) {
                existingOnShown.handle(event);
            }
            root.requestFocus();
            Platform.runLater(root::requestFocus);
        });
    }

    public static boolean showConfirm(Window owner, String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        Window ownerWindow = owner != null ? owner : focusedWindow().orElse(null);
        if (ownerWindow != null) {
            alert.initOwner(ownerWindow);
            alert.initModality(javafx.stage.Modality.WINDOW_MODAL);
        }
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.setGraphic(createQuestionGraphic());
        applyDialogStyles(alert.getDialogPane(), ownerWindow);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    public static Optional<String> promptText(Window owner, String title, String header, String content) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(content);
        applyDialogStyles(dialog.getDialogPane(), owner);
        return dialog.showAndWait();
    }

    public static boolean showTypedDangerConfirm(
        Window owner,
        String title,
        String content,
        String requiredText
    ) {
        String required = requiredText == null ? "" : requiredText.trim();
        if (required.isEmpty()) {
            return showConfirm(owner, title, content);
        }

        Dialog<Boolean> dialog = new Dialog<>();
        Window ownerWindow = owner != null ? owner : focusedWindow().orElse(null);
        if (ownerWindow != null) {
            dialog.initOwner(ownerWindow);
            dialog.initModality(Modality.WINDOW_MODAL);
        }
        dialog.setTitle(title);
        dialog.setHeaderText(null);

        ButtonType confirmType = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmType, ButtonType.CANCEL);
        dialog.getDialogPane().setGraphic(createQuestionGraphic());

        Label messageLabel = new Label(content);
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("dialog-loading-message");

        Label instructionLabel = new Label("Type " + required + " to continue.");
        instructionLabel.setWrapText(true);
        instructionLabel.getStyleClass().add("dialog-loading-message");

        TextField confirmField = new TextField();
        confirmField.setPromptText(required);
        confirmField.setMaxWidth(Double.MAX_VALUE);

        VBox contentBox = new VBox(10, messageLabel, instructionLabel, confirmField);
        contentBox.setFillWidth(true);
        dialog.getDialogPane().setContent(contentBox);
        applyDialogStyles(dialog.getDialogPane(), ownerWindow);

        Button confirmButton = (Button) dialog.getDialogPane().lookupButton(confirmType);
        confirmButton.setDisable(true);
        confirmField.textProperty().addListener((obs, oldValue, newValue) ->
            confirmButton.setDisable(!required.equals(newValue == null ? "" : newValue.trim()))
        );
        dialog.setOnShown(event -> {
            confirmField.requestFocus();
            Platform.runLater(confirmField::requestFocus);
        });
        dialog.setResultConverter(buttonType ->
            buttonType == confirmType && required.equals(confirmField.getText() == null ? "" : confirmField.getText().trim())
        );

        Optional<Boolean> result = dialog.showAndWait();
        return result.orElse(false);
    }

    public static Stage showLoadingWindow(Stage owner, String title, String message, double width, double height) {
        Stage dialog = new Stage();
        if (owner != null) {
            dialog.initOwner(owner);
            dialog.initModality(Modality.WINDOW_MODAL);
        }
        dialog.setTitle(title);
        VBox root = createLoadingContent(title, message);
        Scene scene = new Scene(root, width, height);
        applySceneStyles(scene, owner);
        dialog.setScene(scene);
        dialog.setMinWidth(width);
        dialog.setMinHeight(height);
        dialog.show();
        centerWindowOnOwner(dialog);
        return dialog;
    }

    public static void centerWindowOnOwner(Stage dialog) {
        if (dialog == null) {
            return;
        }
        Runnable centerAction = () -> {
            if (!dialog.isShowing()) {
                return;
            }
            dialog.sizeToScene();
            Window owner = dialog.getOwner();
            if (owner != null && owner.isShowing() && owner.getWidth() > 0 && owner.getHeight() > 0) {
                double x = owner.getX() + (owner.getWidth() - dialog.getWidth()) / 2.0;
                double y = owner.getY() + (owner.getHeight() - dialog.getHeight()) / 2.0;
                Rectangle2D bounds = resolveScreenBounds(owner);
                dialog.setX(clamp(x, bounds.getMinX(), bounds.getMaxX() - dialog.getWidth()));
                dialog.setY(clamp(y, bounds.getMinY(), bounds.getMaxY() - dialog.getHeight()));
            } else {
                dialog.centerOnScreen();
            }
        };
        if (Platform.isFxApplicationThread()) {
            centerAction.run();
        } else {
            Platform.runLater(centerAction);
        }
        Platform.runLater(centerAction);
    }

    public static VBox createLoadingContent(String title, String message) {
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(44, 44);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dialog-loading-title");
        titleLabel.setWrapText(true);

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("dialog-loading-message");
        messageLabel.setWrapText(true);

        VBox root = new VBox(14, spinner, titleLabel, messageLabel);
        root.getStyleClass().addAll("dialog-root", "dialog-loading-root");
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(28));
        root.setFillWidth(true);
        return root;
    }

    private static Optional<Window> focusedWindow() {
        return Window.getWindows().stream()
            .filter(Window::isFocused)
            .findFirst();
    }

    private static javafx.scene.Node createQuestionGraphic() {
        javafx.scene.layout.StackPane graphicContainer = new javafx.scene.layout.StackPane();
        graphicContainer.setPrefSize(48, 48);
        graphicContainer.setMaxSize(48, 48);
        graphicContainer.setStyle("-fx-background-color: -app-primary; -fx-background-radius: 50%; -fx-effect: dropshadow(three-pass-box, -app-shadow, 10, 0, 0, 4);");

        javafx.scene.shape.SVGPath questionSVG = new javafx.scene.shape.SVGPath();
        questionSVG.setContent("M11,18h2v-2h-2V18z M12,2C6.48,2,2,6.48,2,12s4.48,10,10,10s10-4.48,10-10S17.52,2,12,2z M12,20c-4.41,0-8-3.59-8-8s3.59-8,8-8s8,3.59,8,8S16.41,20,12,20z M12,6c-2.21,0-4,1.79-4,4h2c0-1.1,0.9-2,2-2s2,0.9,2,2c0,2-3,1.75-3,5h2c0-2.25,3-2.5,3-5C16,7.79,14.21,6,12,6z");
        questionSVG.setFill(javafx.scene.paint.Color.WHITE);
        questionSVG.setScaleX(1.3);
        questionSVG.setScaleY(1.3);

        graphicContainer.getChildren().add(questionSVG);
        return graphicContainer;
    }

    private static void applyDialogStyles(javafx.scene.control.DialogPane pane, Window owner) {
        if (pane == null) {
            return;
        }
        if (owner != null && owner.getScene() != null) {
            pane.getStylesheets().addAll(owner.getScene().getStylesheets());
        } else {
            java.net.URL stylesheet = DialogSupport.class.getResource("/application.css");
            if (stylesheet != null) {
                pane.getStylesheets().add(stylesheet.toExternalForm());
            }
        }
        if (!pane.getStyleClass().contains("custom-alert")) {
            pane.getStyleClass().add("custom-alert");
        }
    }

    private static void applySceneStyles(Scene scene, Window owner) {
        if (scene == null) {
            return;
        }
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
            return;
        }
        java.net.URL stylesheet = DialogSupport.class.getResource("/application.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }
    }

    private static Rectangle2D resolveScreenBounds(Window owner) {
        return Screen.getScreensForRectangle(owner.getX(), owner.getY(), owner.getWidth(), owner.getHeight())
            .stream()
            .findFirst()
            .orElse(Screen.getPrimary())
            .getVisualBounds();
    }

    private static double clamp(double value, double min, double max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
