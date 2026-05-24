package com.pbl3.project.pbl3_project.ui.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Duration;
import javafx.util.StringConverter;

public final class SearchableComboBoxSupport {

    private SearchableComboBoxSupport() {
    }

    public static <T> void install(
        ComboBox<T> comboBox,
        List<T> sourceItems,
        Function<T, String> displayText,
        Function<T, String> searchText
    ) {
        List<T> safeItems = sourceItems == null ? List.of() : new ArrayList<>(sourceItems);
        ObservableList<T> observableItems = FXCollections.observableArrayList(safeItems);
        FilteredList<T> filteredItems = new FilteredList<>(observableItems, item -> true);
        Function<T, String> safeDisplayText = displayText != null ? displayText : String::valueOf;
        Function<T, String> safeSearchText = searchText != null ? searchText : safeDisplayText;

        comboBox.setItems(filteredItems);
        comboBox.setEditable(true);
        comboBox.setVisibleRowCount(Math.min(10, Math.max(1, safeItems.size())));
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(T value) {
                return value == null ? "" : safeDisplayText.apply(value);
            }

            @Override
            public T fromString(String text) {
                return resolveValue(comboBox, safeItems, safeDisplayText, safeSearchText, text);
            }
        });

        AtomicBoolean syncingSelection = new AtomicBoolean(false);
        AtomicBoolean imeComposing = ImeInputSupport.trackComposition(comboBox.getEditor());
        PauseTransition popupDelay = new PauseTransition(Duration.millis(90));
        popupDelay.setOnFinished(event -> {
            if (!imeComposing.get()
                && comboBox.getEditor() != null
                && comboBox.getEditor().isFocused()
                && !comboBox.isShowing()
                && !filteredItems.isEmpty()) {
                comboBox.show();
            }
        });

        Runnable syncVisibleRows = () -> {
            int visibleRows = Math.max(1, Math.min(10, filteredItems.size()));
            comboBox.setVisibleRowCount(visibleRows);
            Platform.runLater(() -> {
                if (comboBox.getSkin() instanceof ComboBoxListViewSkin<?> skin
                    && skin.getPopupContent() instanceof ListView<?> popupList) {
                    double rowHeight = 36;
                    double popupHeight = visibleRows * rowHeight + 8;
                    popupList.setFixedCellSize(rowHeight);
                    popupList.setPrefHeight(popupHeight);
                    popupList.setMaxHeight(popupHeight);
                }
            });
        };
        filteredItems.addListener((ListChangeListener<T>) change -> syncVisibleRows.run());
        syncVisibleRows.run();

        comboBox.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (comboBox.getEditor() == null || !comboBox.getEditor().isFocused()) {
                return;
            }
            if (event.getCode() == KeyCode.ENTER && !imeComposing.get()) {
                completeCurrentText(comboBox, safeItems, safeDisplayText, safeSearchText, syncingSelection);
                event.consume();
            }
        });

        comboBox.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (syncingSelection.get()) {
                return;
            }
            T currentValue = comboBox.getValue();
            if (currentValue != null && sameText(newText, safeDisplayText.apply(currentValue))) {
                filteredItems.setPredicate(item -> true);
                syncVisibleRows.run();
                return;
            }
            String query = newText == null ? "" : newText;
            filteredItems.setPredicate(item -> matches(item, query, safeDisplayText, safeSearchText));
            syncVisibleRows.run();
            if (!imeComposing.get() && comboBox.getEditor().isFocused() && !comboBox.isShowing()) {
                popupDelay.playFromStart();
            }
        });

        comboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            syncingSelection.set(true);
            Platform.runLater(() -> syncingSelection.set(false));
        });

        comboBox.setOnShowing(event -> {
            popupDelay.stop();
            T currentValue = comboBox.getValue();
            String currentText = editorText(comboBox);
            if (currentValue != null && sameText(currentText, safeDisplayText.apply(currentValue))) {
                filteredItems.setPredicate(item -> true);
                syncVisibleRows.run();
                return;
            }
            filteredItems.setPredicate(item -> matches(item, currentText, safeDisplayText, safeSearchText));
            syncVisibleRows.run();
        });
    }

    private static <T> void completeCurrentText(
        ComboBox<T> comboBox,
        List<T> sourceItems,
        Function<T, String> displayText,
        Function<T, String> searchText,
        AtomicBoolean syncingSelection
    ) {
        T resolvedValue = resolveValue(comboBox, sourceItems, displayText, searchText);
        if (resolvedValue == null) {
            return;
        }
        String resolvedText = displayText.apply(resolvedValue);
        syncingSelection.set(true);
        comboBox.setValue(resolvedValue);
        comboBox.getSelectionModel().select(resolvedValue);
        comboBox.getEditor().setText(resolvedText);
        comboBox.getEditor().positionCaret(resolvedText.length());
        comboBox.hide();
        Platform.runLater(() -> syncingSelection.set(false));
    }

    public static <T> T resolveValue(
        ComboBox<T> comboBox,
        List<T> sourceItems,
        Function<T, String> displayText,
        Function<T, String> searchText
    ) {
        return resolveValue(comboBox, sourceItems, displayText, searchText, editorText(comboBox));
    }

    private static <T> T resolveValue(
        ComboBox<T> comboBox,
        List<T> sourceItems,
        Function<T, String> displayText,
        Function<T, String> searchText,
        String rawText
    ) {
        String text = rawText == null ? "" : rawText.trim();
        if (text.isEmpty()) {
            return null;
        }
        T currentValue = comboBox.getValue();
        if (currentValue != null && sameText(text, displayText.apply(currentValue))) {
            return currentValue;
        }
        List<T> safeItems = sourceItems == null ? List.of() : sourceItems;
        Optional<T> exactMatch = safeItems.stream()
            .filter(item -> sameText(text, displayText.apply(item)))
            .findFirst();
        if (exactMatch.isPresent()) {
            return exactMatch.get();
        }
        List<T> queryMatches = safeItems.stream()
            .filter(item -> matches(item, text, displayText, searchText))
            .toList();
        return queryMatches.size() == 1 ? queryMatches.get(0) : null;
    }

    private static <T> void restoreSelection(
        ComboBox<T> comboBox,
        List<T> sourceItems,
        Function<T, String> displayText,
        Function<T, String> searchText,
        T fallbackValue,
        String fallbackText,
        AtomicBoolean syncingSelection
    ) {
        T restoredValue = null;
        if (fallbackValue != null) {
            String fallbackDisplay = displayText.apply(fallbackValue);
            restoredValue = sourceItems.stream()
                .filter(item -> sameText(fallbackDisplay, displayText.apply(item)))
                .findFirst()
                .orElse(fallbackValue);
        }
        if (restoredValue == null) {
            restoredValue = resolveValue(comboBox, sourceItems, displayText, searchText, fallbackText);
        }
        if (restoredValue == null) {
            return;
        }

        String text = displayText.apply(restoredValue);
        syncingSelection.set(true);
        comboBox.setValue(restoredValue);
        comboBox.getSelectionModel().select(restoredValue);
        if (!sameText(editorText(comboBox), text)) {
            comboBox.getEditor().setText(text);
            comboBox.getEditor().positionCaret(text.length());
        }
        Platform.runLater(() -> syncingSelection.set(false));
    }

    private static <T> boolean matches(
        T item,
        String query,
        Function<T, String> displayText,
        Function<T, String> searchText
    ) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isBlank()) {
            return true;
        }
        String haystack = normalize(displayText.apply(item) + " " + searchText.apply(item));
        for (String token : normalizedQuery.split("\\s+")) {
            if (!token.isBlank() && !haystack.contains(token)) {
                return false;
            }
        }
        return true;
    }

    private static <T> String editorText(ComboBox<T> comboBox) {
        if (comboBox == null) {
            return "";
        }
        if (comboBox.isEditable() && comboBox.getEditor() != null) {
            return comboBox.getEditor().getText();
        }
        T value = comboBox.getValue();
        return value == null ? "" : Objects.toString(value, "");
    }

    private static boolean sameText(String first, String second) {
        return normalize(first).equals(normalize(second));
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(text.trim(), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }
}
