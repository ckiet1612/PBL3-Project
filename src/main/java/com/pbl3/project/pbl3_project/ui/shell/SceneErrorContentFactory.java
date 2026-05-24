package com.pbl3.project.pbl3_project.ui.shell;

import com.pbl3.project.pbl3_project.ui.component.ButtonFactory;
import java.text.MessageFormat;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public final class SceneErrorContentFactory {

    private SceneErrorContentFactory() {
    }

    public static Node createContentOrError(
        String title,
        Supplier<Node> contentFactory,
        Runnable retryAction,
        Consumer<String> errorNotifier
    ) {
        try {
            return contentFactory.get();
        } catch (Throwable ex) {
            ex.printStackTrace();
            if (errorNotifier != null) {
                errorNotifier.accept(MessageFormat.format("Could not open {0}: {1}", title, ex.getMessage()));
            }
            return create(title, ex, retryAction);
        }
    }

    public static Node create(String title, Throwable ex, Runnable retryAction) {
        VBox root = new VBox(14);
        root.setPadding(new Insets(24));
        root.setFillWidth(true);
        root.getStyleClass().add("reports-page");

        VBox card = new VBox(12);
        card.getStyleClass().add("report-section-card");
        card.setPadding(new Insets(22));
        card.setMaxWidth(Double.MAX_VALUE);

        Label errorTitle = new Label(MessageFormat.format("Could not open {0}", title));
        errorTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: -app-text-primary;");

        Label errorMessage = new Label(ex.getMessage() == null || ex.getMessage().isBlank()
            ? "An unexpected error occurred while loading this tab."
            : ex.getMessage());
        errorMessage.setWrapText(true);
        errorMessage.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");

        Button retryButton = ButtonFactory.pageNav("Retry");
        retryButton.setOnAction(event -> {
            if (retryAction != null) {
                retryAction.run();
            }
        });

        card.getChildren().addAll(errorTitle, errorMessage, retryButton);
        root.getChildren().add(card);
        return root;
    }
}
