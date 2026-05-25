package com.pbl3.project.pbl3_project.ui.util;

import java.util.List;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;

public final class ResponsiveSceneSupport {

    private static final String INSTALLED_KEY = "pbl3ResponsiveSceneSupportInstalled";
    private static final String SHORT_HEIGHT_CLASS = "app-viewport-short";
    private static final String VERY_SHORT_HEIGHT_CLASS = "app-viewport-very-short";
    private static final String NARROW_WIDTH_CLASS = "app-viewport-narrow";
    private static final List<String> RESPONSIVE_CLASSES = List.of(
        SHORT_HEIGHT_CLASS,
        VERY_SHORT_HEIGHT_CLASS,
        NARROW_WIDTH_CLASS
    );

    private ResponsiveSceneSupport() {
    }

    public static void install(Scene scene) {
        if (scene == null || Boolean.TRUE.equals(scene.getProperties().get(INSTALLED_KEY))) {
            return;
        }
        scene.getProperties().put(INSTALLED_KEY, Boolean.TRUE);
        Runnable refresh = () -> apply(scene);
        scene.widthProperty().addListener((obs, oldValue, newValue) -> refresh.run());
        scene.heightProperty().addListener((obs, oldValue, newValue) -> refresh.run());
        scene.rootProperty().addListener((obs, oldRoot, newRoot) -> refresh.run());
        Platform.runLater(refresh);
    }

    private static void apply(Scene scene) {
        Parent root = scene.getRoot();
        if (root == null) {
            return;
        }
        root.getStyleClass().removeAll(RESPONSIVE_CLASSES);
        double height = scene.getHeight();
        double width = scene.getWidth();
        if (height > 0 && height <= 780) {
            root.getStyleClass().add(SHORT_HEIGHT_CLASS);
        }
        if (height > 0 && height <= 700) {
            root.getStyleClass().add(VERY_SHORT_HEIGHT_CLASS);
        }
        if (width > 0 && width <= 1180) {
            root.getStyleClass().add(NARROW_WIDTH_CLASS);
        }
    }
}
