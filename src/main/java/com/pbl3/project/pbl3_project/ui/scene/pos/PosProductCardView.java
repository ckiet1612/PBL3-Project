package com.pbl3.project.pbl3_project.ui.scene.pos;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public record PosProductCardView(VBox card, Label inCartBadge) {
    public void setInCartQuantity(int quantity) {
        int normalizedQuantity = Math.max(0, quantity);
        inCartBadge.setText(java.text.MessageFormat.format("In cart: {0}", normalizedQuantity));
        inCartBadge.setVisible(normalizedQuantity > 0);
        inCartBadge.setManaged(normalizedQuantity > 0);
    }
}
