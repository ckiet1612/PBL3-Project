package com.pbl3.project.pbl3_project.ui.component;

import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

public final class ActionTaskbarFactory {

    private ActionTaskbarFactory() {
    }

    public static HBox create(Button... buttons) {
        double baseWidth = (buttons.length * 44) + 20;
        HBox taskbar = new HBox(2, buttons);
        taskbar.getStyleClass().add("promotion-action-taskbar");
        taskbar.setAlignment(Pos.CENTER);
        taskbar.setMinHeight(44);
        taskbar.setPrefHeight(44);
        taskbar.setMaxHeight(44);
        taskbar.setMinWidth(baseWidth);
        taskbar.setPrefWidth(baseWidth);
        taskbar.setMaxWidth(baseWidth);
        return taskbar;
    }

    public static Button createButton(Node icon, String tooltipText, String variantClass) {
        Button button = new Button();
        button.getStyleClass().setAll("promotion-taskbar-button", variantClass);
        button.setGraphic(icon);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setAlignment(Pos.CENTER);
        button.setFocusTraversable(false);
        button.setTooltip(new Tooltip(tooltipText));
        button.setMinSize(44, 36);
        button.setPrefSize(44, 36);
        button.setMaxSize(44, 36);
        return button;
    }

    public static Node icon(String type) {
        return switch (type) {
            case "eye" -> iconGroup(
                iconPath("M2.062 12.348a1 1 0 0 1 0-.696C3.423 8.86 6.285 5 12 5s8.577 3.86 9.938 6.652a1 1 0 0 1 0 .696C20.577 15.14 17.715 19 12 19s-8.577-3.86-9.938-6.652"),
                iconPath("M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0")
            );
            case "clipboard" -> iconGroup(
                iconPath("M9 5H7a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2"),
                iconPath("M9 3h6v4H9z"),
                iconPath("M9 12h6"),
                iconPath("M9 16h6")
            );
            case "edit" -> iconGroup(
                iconPath("M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z")
            );
            case "key" -> iconGroup(
                iconCircle(6.3, 16.2, 5.7),
                iconPath("m10.3 12.2 7.6-7.6"),
                iconPath("m17.9 4.6 2.7 2.7"),
                iconPath("m14.4 8.1 2.7 2.7")
            );
            case "check-circle" -> iconGroup(
                iconPath("M22 11.08V12a10 10 0 1 1-5.93-9.14"),
                iconPath("M9 11l3 3L22 4")
            );
            case "trash" -> iconGroup(
                iconPath("M3 6h18"),
                iconPath("M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"),
                iconPath("M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"),
                iconPath("M10 11v6"),
                iconPath("M14 11v6")
            );
            case "power" -> iconGroup(
                iconPath("M12 2v10"),
                iconPath("M18.4 6.6a9 9 0 1 1-12.77.04")
            );
            case "x-circle" -> iconGroup(
                iconPath("M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0"),
                iconPath("M15 9l-6 6"),
                iconPath("M9 9l6 6")
            );
            default -> throw new IllegalArgumentException("Unknown action taskbar icon: " + type);
        };
    }

    private static Group iconGroup(Node... paths) {
        Group group = new Group(paths);
        group.getStyleClass().add("promotion-taskbar-icon-group");
        return group;
    }

    private static SVGPath iconPath(String content) {
        SVGPath path = new SVGPath();
        path.getStyleClass().add("promotion-taskbar-icon");
        path.setContent(content);
        path.setFill(Color.TRANSPARENT);
        path.setStrokeWidth(1.7);
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        path.setStrokeLineJoin(StrokeLineJoin.ROUND);
        return path;
    }

    private static Circle iconCircle(double centerX, double centerY, double radius) {
        Circle circle = new Circle(centerX, centerY, radius);
        circle.getStyleClass().add("promotion-taskbar-icon");
        circle.setFill(Color.TRANSPARENT);
        circle.setStrokeWidth(1.5);
        return circle;
    }
}
