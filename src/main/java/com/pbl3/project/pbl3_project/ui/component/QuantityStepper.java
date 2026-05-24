package com.pbl3.project.pbl3_project.ui.component;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;

public class QuantityStepper extends HBox {
    private static final PseudoClass FOCUSED_PSEUDO_CLASS = PseudoClass.getPseudoClass("focused");
    private static final Color PRIMARY_COLOR = Color.web("#087ff5");

    private final int minValue;
    private final int maxValue;
    private final IntegerProperty value = new SimpleIntegerProperty(0);
    private final TextField valueField = new TextField("0");
    private final Button minusButton = new Button("-");
    private final Button plusButton = new Button("+");

    public QuantityStepper(int minValue, int maxValue, Runnable onChanged) {
        this.minValue = minValue;
        this.maxValue = Math.max(minValue, maxValue);

        getStyleClass().add("qty-stepper");
        setAlignment(Pos.CENTER);

        minusButton.getStyleClass().setAll("qty-stepper-button");
        minusButton.setText(null);
        minusButton.setGraphic(createStepperIcon(false));
        minusButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        minusButton.setFocusTraversable(false);
        minusButton.setOnAction(event -> setValue(getValue() - 1, true, onChanged));

        valueField.getStyleClass().setAll("qty-stepper-field");
        valueField.setAlignment(Pos.CENTER);
        valueField.setFocusTraversable(false);
        valueField.setTextFormatter(new TextFormatter<>(change ->
            change.getControlNewText().matches("\\d*") ? change : null
        ));
        valueField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            pseudoClassStateChanged(FOCUSED_PSEUDO_CLASS, isFocused);
            if (!isFocused) {
                commitEditorText(onChanged);
            }
        });
        valueField.setOnAction(event -> commitEditorText(onChanged));

        plusButton.getStyleClass().setAll("qty-stepper-button");
        plusButton.setText(null);
        plusButton.setGraphic(createStepperIcon(true));
        plusButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        plusButton.setFocusTraversable(false);
        plusButton.setOnAction(event -> setValue(getValue() + 1, true, onChanged));

        getChildren().addAll(minusButton, valueField, plusButton);

        value.addListener((obs, oldValue, newValue) -> {
            String nextText = String.valueOf(newValue.intValue());
            if (!nextText.equals(valueField.getText())) {
                valueField.setText(nextText);
            }
            minusButton.setDisable(newValue.intValue() <= this.minValue);
            plusButton.setDisable(newValue.intValue() >= this.maxValue);
        });
        value.set(this.minValue);
    }

    public void commitEditorText(Runnable onChanged) {
        String editorText = valueField.getText();
        if (editorText == null || editorText.isBlank()) {
            setValue(minValue, true, onChanged);
            return;
        }
        try {
            setValue(Integer.parseInt(editorText.trim()), true, onChanged);
        } catch (NumberFormatException ignored) {
            valueField.setText(String.valueOf(getValue()));
        }
    }

    public void setValue(int nextValue, boolean notify, Runnable onChanged) {
        int clamped = Math.max(minValue, Math.min(maxValue, nextValue));
        if (value.get() == clamped) {
            valueField.setText(String.valueOf(clamped));
            return;
        }
        value.set(clamped);
        if (notify && onChanged != null) {
            onChanged.run();
        }
    }

    public int getValue() {
        return value.get();
    }

    public IntegerProperty valueProperty() {
        return value;
    }

    private static Node createStepperIcon(boolean plus) {
        Line horizontal = new Line(-4.0, 0.0, 4.0, 0.0);
        horizontal.setStroke(PRIMARY_COLOR);
        horizontal.setStrokeWidth(2.2);
        horizontal.setStrokeLineCap(StrokeLineCap.ROUND);

        StackPane iconPane;
        if (plus) {
            Line vertical = new Line(0.0, -4.0, 0.0, 4.0);
            vertical.setStroke(PRIMARY_COLOR);
            vertical.setStrokeWidth(2.2);
            vertical.setStrokeLineCap(StrokeLineCap.ROUND);
            iconPane = new StackPane(horizontal, vertical);
        } else {
            iconPane = new StackPane(horizontal);
        }
        iconPane.setMinSize(12, 12);
        iconPane.setPrefSize(12, 12);
        iconPane.setMaxSize(12, 12);
        return iconPane;
    }
}
