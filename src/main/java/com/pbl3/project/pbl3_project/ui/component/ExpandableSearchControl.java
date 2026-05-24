package com.pbl3.project.pbl3_project.ui.component;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

public record ExpandableSearchControl(HBox box, TextField field) {

    public static ExpandableSearchControl create(double expandedWidth, Color iconColor) {
        HBox searchBox = new HBox(0);
        searchBox.setAlignment(Pos.CENTER);
        searchBox.getStyleClass().add("expandable-search-box");
        searchBox.setPrefSize(40, 40);
        searchBox.setMinSize(40, 40);
        searchBox.setMaxSize(40, 40);
        Tooltip.install(searchBox, new Tooltip("Search"));

        SVGPath searchIcon = new SVGPath();
        searchIcon.setContent("M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
        searchIcon.setFill(iconColor);

        Region searchSpacer = new Region();
        searchSpacer.setMinWidth(0);
        searchSpacer.setPrefWidth(0);

        TextField searchField = new TextField();
        searchField.setPromptText("Search");
        searchField.getStyleClass().add("search-text-field");
        searchField.setMinWidth(0);
        searchField.setMaxWidth(0);
        searchField.setPrefWidth(0);
        searchField.setOpacity(0);

        searchBox.getChildren().addAll(searchIcon, searchSpacer, searchField);

        double targetFieldWidth = Math.max(0, expandedWidth - 60);
        Timeline searchExpand = new Timeline(
            new KeyFrame(Duration.millis(150),
                new KeyValue(searchBox.maxWidthProperty(), expandedWidth, Interpolator.EASE_BOTH),
                new KeyValue(searchBox.prefWidthProperty(), expandedWidth, Interpolator.EASE_BOTH),
                new KeyValue(searchSpacer.minWidthProperty(), 8, Interpolator.EASE_BOTH),
                new KeyValue(searchField.minWidthProperty(), targetFieldWidth, Interpolator.EASE_BOTH),
                new KeyValue(searchField.maxWidthProperty(), targetFieldWidth, Interpolator.EASE_BOTH),
                new KeyValue(searchField.prefWidthProperty(), targetFieldWidth, Interpolator.EASE_BOTH),
                new KeyValue(searchField.opacityProperty(), 1.0, Interpolator.EASE_BOTH)
            )
        );
        Timeline searchCollapse = new Timeline(
            new KeyFrame(Duration.millis(150),
                new KeyValue(searchBox.maxWidthProperty(), 40, Interpolator.EASE_BOTH),
                new KeyValue(searchBox.prefWidthProperty(), 40, Interpolator.EASE_BOTH),
                new KeyValue(searchSpacer.minWidthProperty(), 0, Interpolator.EASE_BOTH),
                new KeyValue(searchField.minWidthProperty(), 0, Interpolator.EASE_BOTH),
                new KeyValue(searchField.maxWidthProperty(), 0, Interpolator.EASE_BOTH),
                new KeyValue(searchField.prefWidthProperty(), 0, Interpolator.EASE_BOTH),
                new KeyValue(searchField.opacityProperty(), 0.0, Interpolator.EASE_BOTH)
            )
        );

        searchBox.setOnMouseClicked(event -> {
            if (searchBox.getMaxWidth() == 40) {
                searchExpand.play();
                searchField.requestFocus();
            } else if (event.getTarget() == searchIcon || event.getTarget() == searchBox) {
                searchField.clear();
                if (searchBox.getParent() != null) {
                    searchBox.getParent().requestFocus();
                }
                searchCollapse.play();
            }
        });

        return new ExpandableSearchControl(searchBox, searchField);
    }
}
