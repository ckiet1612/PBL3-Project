package com.pbl3.project.pbl3_project.ui.component;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class DialogFormFactory {

    private DialogFormFactory() {
    }

    public static VBox header(String titleText, String contextText) {
        Label title = new Label(titleText);
        title.getStyleClass().add("product-dialog-title");

        VBox header = new VBox(6, title);
        header.getStyleClass().add("product-dialog-header");
        header.setFillWidth(true);

        if (contextText != null && !contextText.isBlank()) {
            Label context = new Label(contextText);
            context.getStyleClass().add("product-dialog-context-chip");
            header.getChildren().add(context);
        }
        return header;
    }

    public static GridPane grid() {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);
        ColumnConstraints firstColumn = new ColumnConstraints();
        firstColumn.setPercentWidth(50);
        ColumnConstraints secondColumn = new ColumnConstraints();
        secondColumn.setPercentWidth(50);
        grid.getColumnConstraints().addAll(firstColumn, secondColumn);
        return grid;
    }

    public static VBox section(String titleText, Node content) {
        Label title = new Label(titleText);
        title.getStyleClass().add("product-dialog-section-title");
        VBox section = new VBox(10, title, content);
        section.getStyleClass().add("product-dialog-section");
        section.setFillWidth(true);
        return section;
    }

    public static VBox fieldBlock(String labelText, Node input, Label errorLabel) {
        Label label = formLabel(labelText);
        VBox block = new VBox(5, label, input);
        block.getStyleClass().add("product-dialog-field-block");
        if (input instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        if (errorLabel != null) {
            block.getChildren().add(errorLabel);
        }
        return block;
    }

    public static Label errorLabel() {
        Label error = new Label();
        error.getStyleClass().add("product-dialog-error");
        error.setVisible(false);
        error.setManaged(false);
        return error;
    }

    public static void setError(Label errorLabel, String message) {
        if (errorLabel == null) {
            return;
        }
        boolean hasError = message != null && !message.isBlank();
        errorLabel.setText(hasError ? message : "");
        errorLabel.setVisible(hasError);
        errorLabel.setManaged(hasError);
    }

    public static TextField textField(String value, String prompt) {
        TextField field = new TextField(value);
        field.setPromptText(prompt);
        field.getStyleClass().add("text-field");
        return field;
    }

    public static TextArea textArea(String value, String prompt, int prefRowCount) {
        TextArea area = new TextArea(value);
        area.setPromptText(prompt);
        area.getStyleClass().add("text-area");
        area.setWrapText(true);
        area.setPrefRowCount(prefRowCount);
        return area;
    }

    public static void applyReadOnly(TextInputControl control) {
        control.setEditable(false);
        control.setFocusTraversable(false);
    }

    public static Label formLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("form-label");
        return label;
    }

    public static <T> void installPopupCells(ComboBox<T> comboBox) {
        comboBox.setCellFactory(listView -> new ListCell<>() {
            {
                getStyleClass().add("promotion-dialog-popup-cell");
            }

            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null
                    ? null
                    : comboBox.getConverter() != null ? comboBox.getConverter().toString(item) : String.valueOf(item));
            }
        });
    }
}
