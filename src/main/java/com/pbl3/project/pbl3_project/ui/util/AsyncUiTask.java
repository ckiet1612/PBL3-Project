package com.pbl3.project.pbl3_project.ui.util;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableView;

public final class AsyncUiTask {

    private AsyncUiTask() {
    }

    public static <T> void runLatestTableLoad(
        TableView<?> table,
        Button previousButton,
        Button nextButton,
        AtomicLong loadVersion,
        Callable<T> work,
        Consumer<T> onSucceeded,
        Consumer<Throwable> onFailed,
        String loadingText,
        String failedText,
        String threadName
    ) {
        long version = loadVersion.incrementAndGet();
        boolean previousButtonWasDisabled = previousButton != null && previousButton.isDisabled();
        boolean nextButtonWasDisabled = nextButton != null && nextButton.isDisabled();
        moveFocusToTableIfPageControlIsFocused(table, previousButton, nextButton);
        if (previousButton != null) {
            previousButton.setDisable(true);
        }
        if (nextButton != null) {
            nextButton.setDisable(true);
        }
        table.setPlaceholder(createLoadingPlaceholder(loadingText));

        javafx.concurrent.Task<T> task = new javafx.concurrent.Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(event -> {
            if (version != loadVersion.get()) {
                return;
            }
            table.setPlaceholder(new Label("No data"));
            onSucceeded.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            if (version != loadVersion.get()) {
                return;
            }
            table.setPlaceholder(new Label(failedText == null || failedText.isBlank() ? "Could not load data" : failedText));
            if (previousButton != null) {
                previousButton.setDisable(previousButtonWasDisabled);
            }
            if (nextButton != null) {
                nextButton.setDisable(nextButtonWasDisabled);
            }
            if (onFailed != null) {
                onFailed.accept(task.getException());
            }
        });
        UiTaskExecutor.execute(task, threadName == null || threadName.isBlank() ? "table-page-loader" : threadName);
    }

    public static <T> void runButtonTask(
        Button primaryButton,
        Button secondaryButton,
        String busyText,
        Callable<T> work,
        Consumer<T> onSucceeded,
        Consumer<Throwable> onFailed,
        String threadName
    ) {
        String originalText = primaryButton != null ? primaryButton.getText() : null;
        boolean primaryWasDisabled = primaryButton != null && primaryButton.isDisabled();
        boolean secondaryWasDisabled = secondaryButton != null && secondaryButton.isDisabled();
        if (primaryButton != null) {
            primaryButton.setDisable(true);
            if (busyText != null && !busyText.isBlank()) {
                primaryButton.setText(busyText);
            }
        }
        if (secondaryButton != null) {
            secondaryButton.setDisable(true);
        }

        javafx.concurrent.Task<T> task = new javafx.concurrent.Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(event -> {
            restoreButtons(primaryButton, secondaryButton, originalText, primaryWasDisabled, secondaryWasDisabled);
            if (onSucceeded != null) {
                onSucceeded.accept(task.getValue());
            }
        });
        task.setOnFailed(event -> {
            restoreButtons(primaryButton, secondaryButton, originalText, primaryWasDisabled, secondaryWasDisabled);
            if (onFailed != null) {
                onFailed.accept(task.getException());
            }
        });
        UiTaskExecutor.execute(task, threadName == null || threadName.isBlank() ? "ui-button-task" : threadName);
    }

    public static <T> void runLatestCachedTableLoad(
        TableView<?> table,
        Button previousButton,
        Button nextButton,
        AtomicLong loadVersion,
        AsyncPageCache<T> pageCache,
        Object cacheKey,
        Callable<T> work,
        Consumer<T> onSucceeded,
        Consumer<Throwable> onFailed,
        String loadingText,
        String failedText,
        String threadName
    ) {
        if (pageCache == null || cacheKey == null) {
            runLatestTableLoad(
                table,
                previousButton,
                nextButton,
                loadVersion,
                work,
                onSucceeded,
                onFailed,
                loadingText,
                failedText,
                threadName
            );
            return;
        }

        T cachedValue = pageCache == null ? null : pageCache.get(cacheKey);
        if (cachedValue != null) {
            loadVersion.incrementAndGet();
            table.setPlaceholder(new Label("No data"));
            onSucceeded.accept(cachedValue);
            return;
        }

        long version = loadVersion.incrementAndGet();
        boolean previousButtonWasDisabled = previousButton != null && previousButton.isDisabled();
        boolean nextButtonWasDisabled = nextButton != null && nextButton.isDisabled();
        moveFocusToTableIfPageControlIsFocused(table, previousButton, nextButton);
        if (previousButton != null) {
            previousButton.setDisable(true);
        }
        if (nextButton != null) {
            nextButton.setDisable(true);
        }
        table.setPlaceholder(createLoadingPlaceholder(loadingText));

        CompletableFuture<T> future = pageCache.load(cacheKey, work, threadName);
        future.whenComplete((value, throwable) -> Platform.runLater(() -> {
            if (version != loadVersion.get()) {
                return;
            }
            if (throwable == null) {
                table.setPlaceholder(new Label("No data"));
                onSucceeded.accept(value);
                return;
            }
            table.setPlaceholder(new Label(failedText == null || failedText.isBlank() ? "Could not load data" : failedText));
            if (previousButton != null) {
                previousButton.setDisable(previousButtonWasDisabled);
            }
            if (nextButton != null) {
                nextButton.setDisable(nextButtonWasDisabled);
            }
            if (onFailed != null) {
                onFailed.accept(unwrapCompletionException(throwable));
            }
        }));
    }

    private static javafx.scene.layout.VBox createLoadingPlaceholder(String loadingText) {
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(30, 30);
        Label label = new Label(loadingText == null || loadingText.isBlank() ? "Loading..." : loadingText);
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: -app-text-secondary;");
        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(8, progressIndicator, label);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        return box;
    }

    private static void restoreButtons(
        Button primaryButton,
        Button secondaryButton,
        String originalText,
        boolean primaryWasDisabled,
        boolean secondaryWasDisabled
    ) {
        if (primaryButton != null) {
            primaryButton.setDisable(primaryWasDisabled);
            if (originalText != null) {
                primaryButton.setText(originalText);
            }
        }
        if (secondaryButton != null) {
            secondaryButton.setDisable(secondaryWasDisabled);
        }
    }

    private static void moveFocusToTableIfPageControlIsFocused(
        TableView<?> table,
        Button previousButton,
        Button nextButton
    ) {
        if (table == null || table.getScene() == null) {
            return;
        }
        javafx.scene.Node focusOwner = table.getScene().getFocusOwner();
        if (focusOwner == previousButton || focusOwner == nextButton) {
            table.requestFocus();
        }
    }

    private static Throwable unwrapCompletionException(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
