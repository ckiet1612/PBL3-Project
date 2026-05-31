package com.pbl3.project.pbl3_project.ui.component;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public final class FilterDisclosureSection {
    private final VBox root;
    private final Region contentRegion;
    private final StackPane contentWrapper;
    private final Polygon arrow;
    private final Timeline animationTimeline = new Timeline();
    private boolean expanded;

    public FilterDisclosureSection(CheckBox allCheckBox, Node contentNode) {
        this.contentRegion = contentNode instanceof Region region ? region : new StackPane(contentNode);
        this.arrow = new Polygon(0.0, 0.0, 10.0, 0.0, 5.0, 6.0);
        this.arrow.setFill(Color.web("#78909C"));

        StackPane arrowWrap = new StackPane(arrow);
        arrowWrap.setMinSize(12, 12);
        arrowWrap.setPrefSize(12, 12);
        arrowWrap.setMaxSize(12, 12);

        Button toggleButton = new Button();
        toggleButton.getStyleClass().setAll("filter-disclosure-button");
        toggleButton.setGraphic(arrowWrap);
        toggleButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        toggleButton.setFocusTraversable(false);
        toggleButton.setTooltip(new Tooltip("Show or hide filter options"));
        toggleButton.setOnAction(e -> setExpanded(!expanded, true));

        HBox header = new HBox(6, allCheckBox, toggleButton);
        header.setAlignment(Pos.CENTER_LEFT);

        this.contentRegion.setMaxWidth(Double.MAX_VALUE);
        this.contentWrapper = new StackPane(this.contentRegion);
        this.contentWrapper.setMaxWidth(Double.MAX_VALUE);
        this.contentWrapper.setMinHeight(0);
        StackPane.setAlignment(this.contentRegion, Pos.TOP_LEFT);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(contentWrapper.widthProperty());
        clip.heightProperty().bind(contentWrapper.heightProperty());
        this.contentWrapper.setClip(clip);

        this.root = new VBox(4, header, contentWrapper);
        this.root.setFillWidth(true);
        setExpanded(false, false);
    }

    public Node getNode() {
        return root;
    }

    public void setExpanded(boolean expanded) {
        setExpanded(expanded, true);
    }

    private void setExpanded(boolean expanded, boolean animate) {
        this.expanded = expanded;
        double targetRotation = expanded ? 180.0 : 0.0;
        animationTimeline.stop();

        if (!animate) {
            double targetHeight = expanded ? computeExpandedHeight() : 0.0;
            contentWrapper.setManaged(expanded);
            contentWrapper.setVisible(expanded);
            contentWrapper.setOpacity(expanded ? 1.0 : 0.0);
            contentWrapper.setPrefHeight(targetHeight);
            contentWrapper.setMaxHeight(targetHeight);
            arrow.setRotate(targetRotation);
            return;
        }

        if (expanded) {
            double targetHeight = computeExpandedHeight();
            contentWrapper.setManaged(true);
            contentWrapper.setVisible(true);
            if (contentWrapper.getPrefHeight() <= 0.0) {
                contentWrapper.setPrefHeight(0.0);
                contentWrapper.setMaxHeight(0.0);
                contentWrapper.setOpacity(0.0);
            }
            animationTimeline.getKeyFrames().setAll(
                new KeyFrame(
                    Duration.millis(180),
                    new KeyValue(arrow.rotateProperty(), targetRotation),
                    new KeyValue(contentWrapper.prefHeightProperty(), targetHeight),
                    new KeyValue(contentWrapper.maxHeightProperty(), targetHeight),
                    new KeyValue(contentWrapper.opacityProperty(), 1.0)
                )
            );
        } else {
            double currentHeight = contentWrapper.getHeight() > 0.0 ? contentWrapper.getHeight() : contentWrapper.getPrefHeight();
            contentWrapper.setPrefHeight(currentHeight);
            contentWrapper.setMaxHeight(currentHeight);
            animationTimeline.getKeyFrames().setAll(
                new KeyFrame(
                    Duration.millis(160),
                    new KeyValue(arrow.rotateProperty(), targetRotation),
                    new KeyValue(contentWrapper.prefHeightProperty(), 0.0),
                    new KeyValue(contentWrapper.maxHeightProperty(), 0.0),
                    new KeyValue(contentWrapper.opacityProperty(), 0.0)
                )
            );
            animationTimeline.setOnFinished(e -> {
                if (!this.expanded) {
                    contentWrapper.setManaged(false);
                    contentWrapper.setVisible(false);
                }
                animationTimeline.setOnFinished(null);
            });
        }
        animationTimeline.play();
    }

    private double computeExpandedHeight() {
        contentRegion.applyCss();
        double width = contentRegion.getWidth() > 0.0 ? contentRegion.getWidth() : 300.0;
        double prefHeight = contentRegion.prefHeight(width);
        double maxHeight = contentRegion.getMaxHeight();
        if (maxHeight > 0.0 && maxHeight < Double.MAX_VALUE) {
            prefHeight = Math.min(prefHeight, maxHeight);
        }
        return Math.max(0.0, prefHeight);
    }
}
