package com.pbl3.project.pbl3_project.ui.shell;

import java.util.function.Consumer;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public final class PostLoginLoadingSceneFactory {

    public record Context(
        Stage stage,
        double width,
        double height,
        Consumer<Scene> sceneInitializer,
        Runnable onFinished
    ) {
    }

    public void show(Context context) {
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(64, 64);

        Label titleLabel = new Label("Signing In");
        titleLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: 700; -fx-text-fill: -app-text-primary;");

        Label subtitleLabel = new Label("Preparing your workspace...");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -app-text-secondary;");

        VBox loadingContent = new VBox(16, spinner, titleLabel, subtitleLabel);
        loadingContent.setAlignment(Pos.CENTER);
        loadingContent.setPadding(new Insets(36));

        StackPane root = new StackPane(loadingContent);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: -app-surface-muted;");

        Scene scene = new Scene(root, context.width(), context.height());
        if (context.sceneInitializer() != null) {
            context.sceneInitializer().accept(scene);
        }

        context.stage().setScene(scene);
        context.stage().setWidth(context.width());
        context.stage().setHeight(context.height());
        context.stage().centerOnScreen();

        PauseTransition delay = new PauseTransition(Duration.millis(180));
        delay.setOnFinished(event -> {
            if (context.onFinished() != null) {
                context.onFinished().run();
            }
        });
        delay.play();
    }
}
