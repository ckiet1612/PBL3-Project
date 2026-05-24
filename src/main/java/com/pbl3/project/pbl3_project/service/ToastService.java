package com.pbl3.project.pbl3_project.service;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Shows short non-blocking messages on top of the current JavaFX scene.
 */
@Service
public class ToastService {
    private static final Insets TOAST_LAYER_PADDING = new Insets(12, 0, 0, 0);
    private static final double TOAST_MAX_WIDTH = 460;
    private static final double TOAST_SIDE_MARGIN = 40;
    private static final double TOAST_TOP_MARGIN = 14;
    private static final int TOAST_SHOW_ANIMATION_MS = 140;
    private static final int TOAST_HIDE_ANIMATION_MS = 110;
    private static final double TOAST_SHOW_START_Y = -12;
    private static final double TOAST_HIDE_END_Y = -6;
    private static final String TRANSPARENT_TOAST_LAYER_STYLE =
        "-fx-background-color: transparent; -fx-background: transparent;";
    
    private VBox toastContainer;
    private VBox floatingToastContainer;
    private Popup floatingToastPopup;
    private Window floatingToastOwner;
    private final Set<Window> hookedToastOwners = Collections.newSetFromMap(new WeakHashMap<>());
    private Scene currentScene;
    
    public void setScene(Scene scene) {
        this.currentScene = scene;
        setupContainer();
    }
    
    private void setupContainer() {
        if (currentScene == null) return;

        if (toastContainer != null && toastContainer.getParent() instanceof Pane parent) {
            parent.getChildren().remove(toastContainer);
        }
        
        toastContainer = new VBox(10);
        toastContainer.setAlignment(Pos.TOP_CENTER);
        toastContainer.setPadding(TOAST_LAYER_PADDING);
        toastContainer.setMouseTransparent(true);
        toastContainer.setPickOnBounds(false);
        toastContainer.setStyle(TRANSPARENT_TOAST_LAYER_STYLE);
        
        if (currentScene.getRoot() instanceof StackPane) {
            StackPane root = (StackPane) currentScene.getRoot();
            root.getChildren().add(toastContainer);
            StackPane.setAlignment(toastContainer, Pos.TOP_CENTER);
        } else if (currentScene.getRoot() instanceof javafx.scene.layout.BorderPane) {
            // A StackPane lets the toast layer sit above the normal page layout.
            javafx.scene.layout.BorderPane oldRoot = (javafx.scene.layout.BorderPane) currentScene.getRoot();
            StackPane newRoot = new StackPane();
            newRoot.getChildren().addAll(oldRoot, toastContainer);
            StackPane.setAlignment(toastContainer, Pos.TOP_CENTER);
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
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> showToast(message, bgColor, icon, durationMs));
            return;
        }

        HBox toast = createToast(message, bgColor, icon);
        Window owner = resolveToastOwner();
        if (owner != null) {
            showFloatingToast(owner, toast, durationMs);
            return;
        }

        if (toastContainer == null) {
            System.out.println("[TOAST] " + icon + " " + message);
            return;
        }

        toastContainer.getChildren().add(toast);
        animateToast(toastContainer, toast, durationMs, () -> {
        });
    }

    private HBox createToast(String message, String bgColor, String icon) {
        HBox toast = new HBox(10);
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.setPadding(new Insets(12, 20, 12, 16));
        toast.setStyle(
            "-fx-background-color: " + bgColor + ";" +
            "-fx-background-radius: 8;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 3);"
        );
        toast.setMaxWidth(460);
        toast.setCache(true);
        toast.setCacheHint(CacheHint.SPEED);
        toast.setOpacity(0);
        toast.setTranslateY(TOAST_SHOW_START_Y);
        
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        
        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        msgLabel.setWrapText(true);
        
        toast.getChildren().addAll(iconLabel, msgLabel);
        return toast;
    }

    private void showFloatingToast(Window owner, HBox toast, int durationMs) {
        ensureFloatingToastLayer();
        floatingToastContainer.getChildren().add(toast);
        showFloatingToastLayer(owner);
        animateToast(floatingToastContainer, toast, durationMs, () -> {
            if (floatingToastContainer.getChildren().isEmpty() && floatingToastPopup != null) {
                floatingToastPopup.hide();
                floatingToastOwner = null;
            }
        });
    }

    private void ensureFloatingToastLayer() {
        if (floatingToastPopup != null && floatingToastContainer != null) {
            return;
        }

        floatingToastContainer = new VBox(10);
        floatingToastContainer.setAlignment(Pos.TOP_CENTER);
        floatingToastContainer.setPadding(TOAST_LAYER_PADDING);
        floatingToastContainer.setMouseTransparent(true);
        floatingToastContainer.setPickOnBounds(false);
        floatingToastContainer.setStyle(TRANSPARENT_TOAST_LAYER_STYLE);

        floatingToastPopup = new Popup();
        floatingToastPopup.setAutoFix(true);
        floatingToastPopup.setAutoHide(false);
        floatingToastPopup.setHideOnEscape(false);
        floatingToastPopup.getContent().add(floatingToastContainer);
    }

    private void showFloatingToastLayer(Window owner) {
        if (owner == null || !owner.isShowing()) {
            return;
        }

        if (floatingToastOwner != owner && floatingToastPopup.isShowing()) {
            floatingToastPopup.hide();
        }
        floatingToastOwner = owner;
        hookToastOwner(owner);
        positionFloatingToastLayer(owner);
        if (!floatingToastPopup.isShowing()) {
            floatingToastPopup.show(owner, resolveFloatingToastX(owner), resolveFloatingToastY(owner));
            makeFloatingToastSceneTransparent();
        } else {
            positionFloatingToastLayer(owner);
        }
    }

    private void makeFloatingToastSceneTransparent() {
        Scene scene = floatingToastPopup == null ? null : floatingToastPopup.getScene();
        if (scene != null) {
            scene.setFill(Color.TRANSPARENT);
            scene.getRoot().setStyle(TRANSPARENT_TOAST_LAYER_STYLE);
        }
    }

    private void hookToastOwner(Window owner) {
        if (hookedToastOwners.add(owner)) {
            owner.addEventHandler(WindowEvent.WINDOW_HIDDEN, event -> rehostFloatingToastLayer());
        }
    }

    private void rehostFloatingToastLayer() {
        if (floatingToastContainer == null || floatingToastContainer.getChildren().isEmpty()) {
            return;
        }

        Platform.runLater(() -> {
            Window nextOwner = resolveToastOwner();
            if (nextOwner != null) {
                showFloatingToastLayer(nextOwner);
            }
        });
    }

    private void positionFloatingToastLayer(Window owner) {
        if (floatingToastContainer == null || owner == null || !owner.isShowing()) {
            return;
        }

        double width = resolveFloatingToastWidth(owner);
        floatingToastContainer.setMinWidth(width);
        floatingToastContainer.setPrefWidth(width);
        floatingToastContainer.setMaxWidth(width);
        floatingToastPopup.setX(resolveFloatingToastX(owner));
        floatingToastPopup.setY(resolveFloatingToastY(owner));
    }

    private double resolveFloatingToastX(Window owner) {
        double width = resolveFloatingToastWidth(owner);
        return owner.getX() + Math.max(12, (owner.getWidth() - width) / 2);
    }

    private double resolveFloatingToastY(Window owner) {
        return owner.getY() + TOAST_TOP_MARGIN;
    }

    private double resolveFloatingToastWidth(Window owner) {
        double ownerWidth = owner.getWidth();
        if (Double.isNaN(ownerWidth) || ownerWidth <= 0) {
            return TOAST_MAX_WIDTH;
        }
        return Math.min(TOAST_MAX_WIDTH, Math.max(220, ownerWidth - TOAST_SIDE_MARGIN));
    }

    private Window resolveToastOwner() {
        return Window.getWindows().stream()
            .filter(Window::isShowing)
            .filter(window -> !(window instanceof PopupWindow))
            .filter(Window::isFocused)
            .findFirst()
            .or(() -> Window.getWindows().stream()
                .filter(Window::isShowing)
                .filter(window -> !(window instanceof PopupWindow))
                .reduce((first, second) -> second))
            .or(() -> {
                Window sceneWindow = currentScene == null ? null : currentScene.getWindow();
                return sceneWindow != null && sceneWindow.isShowing()
                    ? java.util.Optional.of(sceneWindow)
                    : java.util.Optional.empty();
            })
            .orElse(null);
    }

    private void animateToast(VBox container, HBox toast, int durationMs, Runnable onRemoved) {
        Timeline slideIn = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(toast.opacityProperty(), 0),
                new KeyValue(toast.translateYProperty(), TOAST_SHOW_START_Y)
            ),
            new KeyFrame(Duration.millis(TOAST_SHOW_ANIMATION_MS),
                new KeyValue(toast.opacityProperty(), 1, Interpolator.EASE_OUT),
                new KeyValue(toast.translateYProperty(), 0, Interpolator.EASE_OUT)
            )
        );
        
        Timeline fadeOut = new Timeline(
            new KeyFrame(Duration.millis(durationMs)),
            new KeyFrame(Duration.millis(durationMs + TOAST_HIDE_ANIMATION_MS),
                new KeyValue(toast.opacityProperty(), 0, Interpolator.EASE_IN),
                new KeyValue(toast.translateYProperty(), TOAST_HIDE_END_Y, Interpolator.EASE_IN)
            )
        );
        
        fadeOut.setOnFinished(e -> {
            container.getChildren().remove(toast);
            onRemoved.run();
        });
        
        slideIn.setOnFinished(e -> fadeOut.play());
        slideIn.play();
    }
}
