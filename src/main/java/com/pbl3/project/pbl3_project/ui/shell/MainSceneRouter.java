package com.pbl3.project.pbl3_project.ui.shell;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

public final class MainSceneRouter {

    public record Route(
        Stage stage,
        String title,
        String navId,
        Node content,
        boolean expandContainingSidebarGroup,
        double defaultWidth,
        double defaultHeight,
        Supplier<BorderPane> mainLayoutFactory,
        Consumer<Scene> sceneInitializer,
        Runnable existingLayoutPreferenceApplier,
        BooleanSupplier reducedMotionEnabled
    ) {
    }

    public void switchScene(Route route) {
        Scene currentScene = route.stage().getScene();
        BorderPane root = MainShellFactory.resolveMainLayout(currentScene);

        if (root != null) {
            boolean reducedMotion = MainShellFactory.isReducedMotionEnabled(root);
            routeInsideExistingShell(route, root, reducedMotion);
            return;
        }

        boolean reducedMotion = route.reducedMotionEnabled() != null && route.reducedMotionEnabled().getAsBoolean();
        buildMainShell(route, reducedMotion);
    }

    public void rebuildMainShell(Route route) {
        BorderPane layout = route.mainLayoutFactory().get();
        layout.setUserData("MAIN_LAYOUT");

        WindowSize windowSize = fitToCurrentScreen(route.stage(), route.defaultWidth(), route.defaultHeight());
        Scene newScene = new Scene(layout, windowSize.width(), windowSize.height());
        if (route.sceneInitializer() != null) {
            route.sceneInitializer().accept(newScene);
        }

        route.stage().setScene(newScene);
        route.stage().setWidth(windowSize.width());
        route.stage().setHeight(windowSize.height());
        route.stage().centerOnScreen();
    }

    private void routeInsideExistingShell(Route route, BorderPane root, boolean reducedMotion) {
        Label pageTitle = (Label) root.lookup("#header-title");
        if (pageTitle != null) {
            pageTitle.setText(route.title());
        }
        MainShellFactory.updateSidebarState(root, route.navId(), route.expandContainingSidebarGroup());

        if (route.existingLayoutPreferenceApplier() != null) {
            route.existingLayoutPreferenceApplier().run();
        }

        root.setCenter(route.content());
        BorderPane.setMargin(route.content(), new Insets(15));

        if (reducedMotion) {
            resetMotionState(route.content());
            return;
        }
        playRouteTransition(route.content(), 30);
    }

    private void buildMainShell(Route route, boolean reducedMotion) {
        BorderPane layout = route.mainLayoutFactory().get();
        layout.setUserData("MAIN_LAYOUT");

        WindowSize windowSize = fitToCurrentScreen(route.stage(), route.defaultWidth(), route.defaultHeight());
        Scene newScene = new Scene(layout, windowSize.width(), windowSize.height());
        if (route.sceneInitializer() != null) {
            route.sceneInitializer().accept(newScene);
        }

        route.stage().setScene(newScene);
        route.stage().setWidth(windowSize.width());
        route.stage().setHeight(windowSize.height());
        route.stage().centerOnScreen();

        if (reducedMotion) {
            resetMotionState(layout);
            return;
        }
        playRouteTransition(layout, 50);
    }

    private void playRouteTransition(Node node, double fromY) {
        TranslateTransition translate = new TranslateTransition(Duration.millis(150), node);
        translate.setFromY(fromY);
        translate.setToY(0);
        translate.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fade = new FadeTransition(Duration.millis(150), node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        new ParallelTransition(translate, fade).play();
    }

    private void resetMotionState(Node node) {
        node.setTranslateY(0.0);
        node.setOpacity(1.0);
    }

    private WindowSize fitToCurrentScreen(Stage stage, double defaultWidth, double defaultHeight) {
        Rectangle2D visualBounds = resolveScreenBounds(stage);
        double width = Math.min(defaultWidth, Math.max(760, visualBounds.getWidth() - 48));
        double height = Math.min(defaultHeight, Math.max(560, visualBounds.getHeight() - 64));
        return new WindowSize(width, height);
    }

    private Rectangle2D resolveScreenBounds(Stage stage) {
        if (stage != null && stage.isShowing()) {
            return Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight())
                .stream()
                .findFirst()
                .orElse(Screen.getPrimary())
                .getVisualBounds();
        }
        return Screen.getPrimary().getVisualBounds();
    }

    private record WindowSize(double width, double height) {
    }
}
