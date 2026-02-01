package com.pbl3.project.pbl3_project.service;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.springframework.stereotype.Service;

/**
 * Toast notification service for non-blocking alerts.
 * Shows auto-dismissing notifications in bottom-right corner.
 */
@Service
public class ToastService {
    
    private VBox toastContainer;
    private Scene currentScene;
    
    public void setScene(Scene scene) {
        this.currentScene = scene;
        setupContainer();
    }
    
    private void setupContainer() {
        if (currentScene == null) return;
        
        toastContainer = new VBox(10);
        toastContainer.setAlignment(Pos.BOTTOM_RIGHT);
        toastContainer.setPadding(new Insets(0, 20, 20, 0));
        toastContainer.setMouseTransparent(true);
        toastContainer.setPickOnBounds(false);
        
        if (currentScene.getRoot() instanceof StackPane) {
            ((StackPane) currentScene.getRoot()).getChildren().add(toastContainer);
        } else if (currentScene.getRoot() instanceof javafx.scene.layout.BorderPane) {
            // Wrap existing root in StackPane for overlay
            javafx.scene.layout.BorderPane oldRoot = (javafx.scene.layout.BorderPane) currentScene.getRoot();
            StackPane newRoot = new StackPane();
            newRoot.getChildren().addAll(oldRoot, toastContainer);
            StackPane.setAlignment(toastContainer, Pos.BOTTOM_RIGHT);
            currentScene.setRoot(newRoot);
        }
    }
    
    public void showSuccess(String message) {
        showToast(message, "#4CAF50", "✓", 3000);
    }
    
    public void showError(String message) {
        showToast(message, "#F44336", "✕", 5000);
    }
    
    public void showInfo(String message) {
        showToast(message, "#2196F3", "ℹ", 3000);
    }
    
    public void showWarning(String message) {
        showToast(message, "#FF9800", "⚠", 4000);
    }
    
    private void showToast(String message, String bgColor, String icon, int durationMs) {
        if (toastContainer == null) {
            // Fallback: just print to console if container not ready
            System.out.println("[TOAST] " + icon + " " + message);
            return;
        }
        
        // Create toast HBox
        HBox toast = new HBox(10);
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.setPadding(new Insets(12, 20, 12, 16));
        toast.setStyle(
            "-fx-background-color: " + bgColor + ";" +
            "-fx-background-radius: 8;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 3);"
        );
        toast.setMaxWidth(350);
        toast.setOpacity(0);
        toast.setTranslateX(50);
        
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        
        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        msgLabel.setWrapText(true);
        
        toast.getChildren().addAll(iconLabel, msgLabel);
        toastContainer.getChildren().add(toast);
        
        // Slide in animation
        Timeline slideIn = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(toast.opacityProperty(), 0),
                new KeyValue(toast.translateXProperty(), 50)
            ),
            new KeyFrame(Duration.millis(200),
                new KeyValue(toast.opacityProperty(), 1, Interpolator.EASE_OUT),
                new KeyValue(toast.translateXProperty(), 0, Interpolator.EASE_OUT)
            )
        );
        
        // Hold, then fade out
        Timeline fadeOut = new Timeline(
            new KeyFrame(Duration.millis(durationMs)),
            new KeyFrame(Duration.millis(durationMs + 300),
                new KeyValue(toast.opacityProperty(), 0, Interpolator.EASE_IN),
                new KeyValue(toast.translateXProperty(), 30, Interpolator.EASE_IN)
            )
        );
        
        fadeOut.setOnFinished(e -> toastContainer.getChildren().remove(toast));
        
        slideIn.setOnFinished(e -> fadeOut.play());
        slideIn.play();
    }
}
