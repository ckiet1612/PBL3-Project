package com.pbl3.project.pbl3_project.ui.util;

import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.InputMethodEvent;

public final class ImeInputSupport {

    private ImeInputSupport() {
    }

    public static AtomicBoolean trackComposition(TextInputControl control) {
        AtomicBoolean composing = new AtomicBoolean(false);
        if (control == null) {
            return composing;
        }

        control.addEventFilter(InputMethodEvent.INPUT_METHOD_TEXT_CHANGED, event -> {
            boolean hasComposedText = event.getComposed() != null && !event.getComposed().isEmpty();
            composing.set(hasComposedText);
            if (!hasComposedText || (event.getCommitted() != null && !event.getCommitted().isEmpty())) {
                Platform.runLater(() -> composing.set(false));
            }
        });
        control.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                composing.set(false);
            }
        });
        return composing;
    }
}
