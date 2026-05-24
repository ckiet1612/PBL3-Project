package com.pbl3.project.pbl3_project.ui.component;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

public final class ButtonFactory {

    private static final Interpolator SPRING_BOUNCE = new Interpolator() {
        @Override
        protected double curve(double t) {
            double tension = 0.4;
            t -= 1.0;
            return t * t * ((tension + 1) * t + tension) + 1.0;
        }
    };
    private static final Color SURFACE_COLOR = Color.web("#FFFFFF");

    private ButtonFactory() {
    }

    public static Button pageNav(String text) {
        Button button = new Button(text);
        button.getStyleClass().addAll("button", "dashboard-report-secondary-button");
        button.setStyle("-fx-padding: 4 12; -fx-background-radius: 999;");
        button.setFocusTraversable(false);
        return button;
    }

    public static Button expandableGreenAction(String labelText, double expandedWidth) {
        String baseStyle = "-fx-background-color: -app-success; -fx-background-radius: 20; -fx-padding: 0;";
        String hoverStyle = "-fx-background-color: -app-success-hover; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, -app-shadow, 15, 0, 0, 6); -fx-padding: 0;";

        Button actionButton = new Button();
        actionButton.setStyle(baseStyle);
        actionButton.setPrefSize(40, 40);
        actionButton.setMinSize(40, 40);
        actionButton.setMaxSize(40, 40);
        actionButton.setCursor(Cursor.HAND);
        actionButton.setFocusTraversable(false);
        actionButton.setTooltip(new Tooltip(labelText));

        SVGPath plusIcon = new SVGPath();
        plusIcon.setContent("M12 5v14M5 12h14");
        plusIcon.setStroke(SURFACE_COLOR);
        plusIcon.setStrokeWidth(2.5);
        plusIcon.setStrokeLineCap(StrokeLineCap.ROUND);

        final double iconWrapperSize = 28;
        final double iconTextGap = 8;
        final double collapsedIconX = 6;

        StackPane iconWrapper = new StackPane(plusIcon);
        iconWrapper.setPrefSize(iconWrapperSize, 40);
        iconWrapper.setMinSize(iconWrapperSize, 40);
        iconWrapper.setMaxSize(iconWrapperSize, 40);
        iconWrapper.setTranslateX(collapsedIconX);

        Label actionLabel = new Label(labelText);
        actionLabel.setStyle("-fx-font-family: 'Be Vietnam Pro', 'Inter', sans-serif; -fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: -app-surface; -fx-padding: 0;");
        actionLabel.setMinHeight(40);
        actionLabel.setPrefHeight(40);
        actionLabel.setAlignment(Pos.CENTER_LEFT);
        actionLabel.setOpacity(0);
        actionLabel.setScaleX(0.8);
        actionLabel.setScaleY(0.8);
        actionLabel.setTranslateX(21);

        GaussianBlur labelBlur = new GaussianBlur(4.0);
        actionLabel.setEffect(labelBlur);

        StackPane buttonContent = new StackPane(iconWrapper, actionLabel);
        StackPane.setAlignment(iconWrapper, Pos.CENTER_LEFT);
        StackPane.setAlignment(actionLabel, Pos.CENTER_LEFT);

        Text labelMeasure = new Text(labelText);
        labelMeasure.setFont(Font.font("Be Vietnam Pro", FontWeight.BOLD, 15));
        double labelWidth = Math.ceil(labelMeasure.getLayoutBounds().getWidth());
        double expandedContentWidth = iconWrapperSize + iconTextGap + labelWidth;
        double effectiveExpandedWidth = Math.max(expandedWidth, Math.ceil(expandedContentWidth + 44));
        double expandedContentStart = Math.max(8, Math.floor((effectiveExpandedWidth - expandedContentWidth) / 2));
        double expandedLabelX = expandedContentStart + iconWrapperSize + iconTextGap;

        Rectangle buttonClip = new Rectangle();
        buttonClip.setArcWidth(40);
        buttonClip.setArcHeight(40);
        buttonClip.widthProperty().bind(actionButton.widthProperty());
        buttonClip.heightProperty().bind(actionButton.heightProperty());
        buttonContent.setClip(buttonClip);

        actionButton.setGraphic(buttonContent);
        actionButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        actionButton.setAlignment(Pos.CENTER_LEFT);

        Timeline hoverInAnimation = new Timeline(
            new KeyFrame(Duration.millis(210),
                new KeyValue(actionButton.minWidthProperty(), effectiveExpandedWidth, SPRING_BOUNCE),
                new KeyValue(actionButton.prefWidthProperty(), effectiveExpandedWidth, SPRING_BOUNCE),
                new KeyValue(actionButton.maxWidthProperty(), effectiveExpandedWidth, SPRING_BOUNCE),
                new KeyValue(plusIcon.rotateProperty(), 90, SPRING_BOUNCE),
                new KeyValue(actionLabel.opacityProperty(), 1.0, Interpolator.EASE_BOTH),
                new KeyValue(iconWrapper.translateXProperty(), expandedContentStart, SPRING_BOUNCE),
                new KeyValue(actionLabel.translateXProperty(), expandedLabelX, SPRING_BOUNCE),
                new KeyValue(actionLabel.scaleXProperty(), 1.0, SPRING_BOUNCE),
                new KeyValue(actionLabel.scaleYProperty(), 1.0, SPRING_BOUNCE),
                new KeyValue(labelBlur.radiusProperty(), 0.0, Interpolator.EASE_BOTH)
            )
        );

        Timeline hoverOutAnimation = new Timeline(
            new KeyFrame(Duration.millis(210),
                new KeyValue(actionButton.minWidthProperty(), 40, Interpolator.EASE_BOTH),
                new KeyValue(actionButton.prefWidthProperty(), 40, Interpolator.EASE_BOTH),
                new KeyValue(actionButton.maxWidthProperty(), 40, Interpolator.EASE_BOTH),
                new KeyValue(plusIcon.rotateProperty(), 0, Interpolator.EASE_BOTH),
                new KeyValue(actionLabel.opacityProperty(), 0.0, Interpolator.EASE_BOTH),
                new KeyValue(iconWrapper.translateXProperty(), collapsedIconX, Interpolator.EASE_BOTH),
                new KeyValue(actionLabel.translateXProperty(), 21, Interpolator.EASE_BOTH),
                new KeyValue(actionLabel.scaleXProperty(), 0.8, Interpolator.EASE_BOTH),
                new KeyValue(actionLabel.scaleYProperty(), 0.8, Interpolator.EASE_BOTH),
                new KeyValue(labelBlur.radiusProperty(), 4.0, Interpolator.EASE_BOTH)
            )
        );

        actionButton.setOnMouseEntered(e -> {
            actionButton.setStyle(hoverStyle);
            hoverOutAnimation.stop();
            hoverInAnimation.play();
        });
        actionButton.setOnMouseExited(e -> {
            actionButton.setStyle(baseStyle);
            hoverInAnimation.stop();
            hoverOutAnimation.play();
        });
        actionButton.setOnMousePressed(e -> {
            actionButton.setScaleX(0.95);
            actionButton.setScaleY(0.95);
        });
        actionButton.setOnMouseReleased(e -> {
            actionButton.setScaleX(1.0);
            actionButton.setScaleY(1.0);
        });
        return actionButton;
    }

    public static Button expandableManageAction(String labelText, double expandedWidth) {
        final double collapsedSize = 40;
        final double durationMs = 210;
        String baseStyle = "-fx-background-color: -app-primary; -fx-background-radius: 20; -fx-padding: 0; "
            + "-fx-effect: dropshadow(three-pass-box, rgba(29,125,242,0.30), 12, 0, 0, 4);";
        String hoverStyle = "-fx-background-color: -app-primary-hover; -fx-background-radius: 20; -fx-padding: 0; "
            + "-fx-effect: dropshadow(three-pass-box, rgba(29,125,242,0.40), 18, 0, 0, 6);";
        String disabledStyle = "-fx-background-color: derive(-app-primary, 20%); -fx-background-radius: 20; -fx-padding: 0;";

        Button actionButton = new Button();
        actionButton.getStyleClass().clear();
        actionButton.setStyle(baseStyle);
        actionButton.setPrefSize(collapsedSize, collapsedSize);
        actionButton.setMinSize(collapsedSize, collapsedSize);
        actionButton.setMaxSize(collapsedSize, collapsedSize);
        actionButton.setCursor(Cursor.HAND);
        actionButton.setFocusTraversable(false);
        actionButton.setTooltip(new Tooltip(labelText));

        final double iconWrapperSize = 28;
        final double iconTextGap = 8;
        final double collapsedIconX = (collapsedSize - iconWrapperSize) / 2;
        Node clipboardIcon = manageClipboardIcon();
        StackPane iconWrapper = new StackPane(clipboardIcon);
        iconWrapper.setPrefSize(iconWrapperSize, collapsedSize);
        iconWrapper.setMinSize(iconWrapperSize, collapsedSize);
        iconWrapper.setMaxSize(iconWrapperSize, collapsedSize);
        iconWrapper.setTranslateX(collapsedIconX);

        Label actionLabel = new Label(labelText);
        actionLabel.setStyle("-fx-font-family: 'Be Vietnam Pro', 'Inter', sans-serif; -fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: -app-surface; -fx-padding: 0;");
        actionLabel.setMinHeight(collapsedSize);
        actionLabel.setPrefHeight(collapsedSize);
        actionLabel.setAlignment(Pos.CENTER_LEFT);
        actionLabel.setOpacity(0);
        actionLabel.setTranslateX(21);

        StackPane buttonContent = new StackPane(iconWrapper, actionLabel);
        StackPane.setAlignment(iconWrapper, Pos.CENTER_LEFT);
        StackPane.setAlignment(actionLabel, Pos.CENTER_LEFT);

        Text labelMeasure = new Text(labelText);
        labelMeasure.setFont(Font.font("Be Vietnam Pro", FontWeight.BOLD, 15));
        double labelWidth = Math.ceil(labelMeasure.getLayoutBounds().getWidth());
        double expandedContentWidth = iconWrapperSize + iconTextGap + labelWidth;
        double effectiveExpandedWidth = Math.max(expandedWidth, Math.ceil(expandedContentWidth + 44));
        double expandedContentStart = Math.max(8, Math.floor((effectiveExpandedWidth - expandedContentWidth) / 2));
        double expandedLabelX = expandedContentStart + iconWrapperSize + iconTextGap;

        Rectangle buttonClip = new Rectangle();
        buttonClip.setArcWidth(collapsedSize);
        buttonClip.setArcHeight(collapsedSize);
        buttonClip.widthProperty().bind(actionButton.widthProperty());
        buttonClip.heightProperty().bind(actionButton.heightProperty());
        buttonContent.setClip(buttonClip);
        actionButton.setGraphic(buttonContent);
        actionButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        actionButton.setAlignment(Pos.CENTER_LEFT);

        Timeline hoverInAnimation = new Timeline(
            new KeyFrame(Duration.millis(durationMs),
                new KeyValue(actionButton.minWidthProperty(), effectiveExpandedWidth, SPRING_BOUNCE),
                new KeyValue(actionButton.prefWidthProperty(), effectiveExpandedWidth, SPRING_BOUNCE),
                new KeyValue(actionButton.maxWidthProperty(), effectiveExpandedWidth, SPRING_BOUNCE),
                new KeyValue(iconWrapper.translateXProperty(), expandedContentStart, SPRING_BOUNCE),
                new KeyValue(actionLabel.opacityProperty(), 1.0, Interpolator.EASE_BOTH),
                new KeyValue(actionLabel.translateXProperty(), expandedLabelX, SPRING_BOUNCE)
            )
        );
        Timeline hoverOutAnimation = new Timeline(
            new KeyFrame(Duration.millis(durationMs),
                new KeyValue(actionButton.minWidthProperty(), collapsedSize, Interpolator.EASE_BOTH),
                new KeyValue(actionButton.prefWidthProperty(), collapsedSize, Interpolator.EASE_BOTH),
                new KeyValue(actionButton.maxWidthProperty(), collapsedSize, Interpolator.EASE_BOTH),
                new KeyValue(iconWrapper.translateXProperty(), collapsedIconX, Interpolator.EASE_BOTH),
                new KeyValue(actionLabel.opacityProperty(), 0.0, Interpolator.EASE_BOTH),
                new KeyValue(actionLabel.translateXProperty(), 21, Interpolator.EASE_BOTH)
            )
        );

        actionButton.setOnMouseEntered(e -> {
            if (actionButton.isDisabled()) {
                return;
            }
            actionButton.setStyle(hoverStyle);
            hoverOutAnimation.stop();
            hoverInAnimation.play();
        });
        actionButton.setOnMouseExited(e -> {
            if (actionButton.isDisabled()) {
                return;
            }
            actionButton.setStyle(baseStyle);
            hoverInAnimation.stop();
            hoverOutAnimation.play();
        });
        actionButton.setOnMousePressed(e -> {
            if (actionButton.isDisabled()) {
                return;
            }
            actionButton.setScaleX(0.95);
            actionButton.setScaleY(0.95);
        });
        actionButton.setOnMouseReleased(e -> {
            actionButton.setScaleX(1.0);
            actionButton.setScaleY(1.0);
        });
        actionButton.disabledProperty().addListener((obs, wasDisabled, isDisabled) -> {
            hoverInAnimation.stop();
            hoverOutAnimation.stop();
            actionButton.setScaleX(1.0);
            actionButton.setScaleY(1.0);
            actionButton.setMinWidth(collapsedSize);
            actionButton.setPrefWidth(collapsedSize);
            actionButton.setMaxWidth(collapsedSize);
            iconWrapper.setTranslateX(collapsedIconX);
            actionLabel.setOpacity(0.0);
            actionLabel.setTranslateX(21);
            actionButton.setOpacity(isDisabled ? 0.55 : 1.0);
            actionButton.setStyle(isDisabled ? disabledStyle : baseStyle);
        });
        return actionButton;
    }

    private static Node manageClipboardIcon() {
        Rectangle board = new Rectangle(-5.5, -4.5, 11, 14);
        board.setArcWidth(4);
        board.setArcHeight(4);
        board.setFill(Color.TRANSPARENT);
        board.setStroke(SURFACE_COLOR);
        board.setStrokeWidth(1.6);

        Rectangle clip = new Rectangle(-3.0, -7.5, 6.0, 3.8);
        clip.setArcWidth(3.5);
        clip.setArcHeight(3.5);
        clip.setFill(Color.TRANSPARENT);
        clip.setStroke(SURFACE_COLOR);
        clip.setStrokeWidth(1.6);

        Line line1 = new Line(-2.8, -0.5, 2.8, -0.5);
        line1.setStroke(SURFACE_COLOR);
        line1.setStrokeWidth(1.5);
        line1.setStrokeLineCap(StrokeLineCap.ROUND);
        Line line2 = new Line(-2.8, 2.8, 2.8, 2.8);
        line2.setStroke(SURFACE_COLOR);
        line2.setStrokeWidth(1.5);
        line2.setStrokeLineCap(StrokeLineCap.ROUND);
        Line line3 = new Line(-1.8, 6.1, 1.8, 6.1);
        line3.setStroke(SURFACE_COLOR);
        line3.setStrokeWidth(1.5);
        line3.setStrokeLineCap(StrokeLineCap.ROUND);

        javafx.scene.Group iconGroup = new javafx.scene.Group(board, clip, line1, line2, line3);
        StackPane iconPane = new StackPane(iconGroup);
        iconPane.setPrefSize(20, 20);
        iconPane.setMinSize(20, 20);
        iconPane.setMaxSize(20, 20);
        return iconPane;
    }
}
