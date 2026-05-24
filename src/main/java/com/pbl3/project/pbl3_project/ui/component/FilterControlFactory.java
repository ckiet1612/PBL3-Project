package com.pbl3.project.pbl3_project.ui.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class FilterControlFactory {

    private FilterControlFactory() {
    }

    public record Shell(VBox container, VBox content, ScrollPane scrollPane) {
    }

    public static void applyContainerStyle(Region container) {
        container.setStyle(
            "-fx-background-color: -app-surface; "
                + "-fx-background-radius: 18; "
                + "-fx-effect: dropshadow(three-pass-box, -app-shadow, 14, 0, 0, 5); "
                + "-fx-border-color: -app-border; "
                + "-fx-border-radius: 18;"
        );
    }

    public static Label sectionTitle(String text) {
        Label title = new Label(text);
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");
        return title;
    }

    public static VBox scrollContent() {
        VBox scrollContent = new VBox(10);
        scrollContent.setStyle("-fx-background-color: -app-surface;");
        scrollContent.setPadding(new Insets(5, 15, 5, 15));
        return scrollContent;
    }

    public static ScrollPane scrollPane(Node content, double viewportHeight) {
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: -app-surface; -fx-background-color: transparent; -fx-border-color: transparent;");
        scrollPane.setPrefViewportHeight(viewportHeight);
        return scrollPane;
    }

    public static Shell shell(double prefWidth, double viewportHeight) {
        VBox popupContainer = new VBox(10);
        popupContainer.setPadding(new Insets(15));
        applyContainerStyle(popupContainer);
        popupContainer.setPrefWidth(prefWidth);

        VBox scrollContent = scrollContent();
        ScrollPane scrollPane = scrollPane(scrollContent, viewportHeight);
        popupContainer.getChildren().add(scrollPane);
        return new Shell(popupContainer, scrollContent, scrollPane);
    }

    public static VBox loadingContainer(double prefWidth, String message) {
        VBox popupContainer = new VBox(12);
        popupContainer.setPadding(new Insets(18));
        popupContainer.setPrefWidth(prefWidth);
        popupContainer.setMinHeight(132);
        popupContainer.setAlignment(Pos.CENTER);
        applyContainerStyle(popupContainer);

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(34, 34);

        Label loadingLabel = new Label(message == null || message.isBlank() ? "Loading filters..." : message);
        loadingLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: -app-text-secondary;");
        popupContainer.getChildren().addAll(spinner, loadingLabel);
        return popupContainer;
    }

    public static HBox actionRow(Button resetButton, Button applyButton) {
        HBox buttonRow = new HBox(10, resetButton, applyButton);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);
        return buttonRow;
    }
}
