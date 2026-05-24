package com.pbl3.project.pbl3_project.ui.component;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Cursor;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

public class RangeSlider extends Pane {
    public final DoubleProperty minVal = new SimpleDoubleProperty(0);
    public final DoubleProperty maxVal = new SimpleDoubleProperty(1);

    public RangeSlider(double minBound, double maxBound, double initialMin, double initialMax, double sliderWidth) {
        setPrefHeight(24);
        setPrefWidth(sliderWidth);
        Rectangle bgTrack = new Rectangle(0, 10, sliderWidth, 4);
        bgTrack.setFill(Color.web("#CFD8DC"));
        bgTrack.setArcWidth(4);
        bgTrack.setArcHeight(4);

        Rectangle activeTrack = new Rectangle(0, 10, sliderWidth, 4);
        activeTrack.setFill(Color.web("#1d7df2"));
        activeTrack.setArcWidth(4);
        activeTrack.setArcHeight(4);

        Circle minThumb = new Circle(8, Color.WHITE);
        minThumb.setStroke(Color.web("#CFD8DC"));
        minThumb.setStrokeWidth(1);
        minThumb.setCenterY(12);
        minThumb.setCursor(Cursor.HAND);
        minThumb.setStyle("-fx-effect: dropshadow(three-pass-box, -app-shadow, 3, 0, 0, 1);");

        Circle maxThumb = new Circle(8, Color.WHITE);
        maxThumb.setStroke(Color.web("#CFD8DC"));
        maxThumb.setStrokeWidth(1);
        maxThumb.setCenterY(12);
        maxThumb.setCursor(Cursor.HAND);
        maxThumb.setStyle("-fx-effect: dropshadow(three-pass-box, -app-shadow, 3, 0, 0, 1);");

        getChildren().addAll(bgTrack, activeTrack, minThumb, maxThumb);

        Runnable updateLayout = () -> {
            double range = maxBound - minBound;
            if (range == 0) {
                return;
            }
            double minX = ((minVal.get() - minBound) / range) * sliderWidth;
            double maxX = ((maxVal.get() - minBound) / range) * sliderWidth;
            minThumb.setCenterX(minX);
            maxThumb.setCenterX(maxX);
            activeTrack.setX(minX);
            activeTrack.setWidth(maxX - minX);
        };

        minVal.addListener((obs, ov, nv) -> updateLayout.run());
        maxVal.addListener((obs, ov, nv) -> updateLayout.run());

        minVal.set(initialMin);
        maxVal.set(initialMax);

        javafx.event.EventHandler<javafx.scene.input.MouseEvent> dragMin = e -> {
            double newX = Math.max(0, Math.min(e.getX(), maxThumb.getCenterX() - 12));
            minVal.set(minBound + (newX / sliderWidth) * (maxBound - minBound));
        };
        minThumb.setOnMouseDragged(dragMin);
        minThumb.setOnMousePressed(dragMin);

        javafx.event.EventHandler<javafx.scene.input.MouseEvent> dragMax = e -> {
            double newX = Math.max(minThumb.getCenterX() + 12, Math.min(e.getX(), sliderWidth));
            maxVal.set(minBound + (newX / sliderWidth) * (maxBound - minBound));
        };
        maxThumb.setOnMouseDragged(dragMax);
        maxThumb.setOnMousePressed(dragMax);
    }
}
