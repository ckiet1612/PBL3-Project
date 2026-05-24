package com.pbl3.project.pbl3_project.ui.scene.pos;

import com.pbl3.project.pbl3_project.entity.Customer;
import com.pbl3.project.pbl3_project.ui.component.SidebarIconFactory;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public final class PosCustomerCardSupport {
    private PosCustomerCardSupport() {
    }

    public static Node createCustomerContextIcon() {
        javafx.scene.Node accountIcon = SidebarIconFactory.createMyAccountHeaderIcon();
        accountIcon.getStyleClass().add("header-account-button");

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(accountIcon);
        iconWrap.getStyleClass().add("pos-customer-icon-wrap");
        iconWrap.setPrefSize(36, 36);
        iconWrap.setMinSize(36, 36);
        iconWrap.setMaxSize(36, 36);
        iconWrap.setMouseTransparent(true);
        return iconWrap;
    }

    public static void updateCustomerCard(
        VBox customerCard,
        Label customerNameLabel,
        Label customerPhoneLabel,
        Label customerStateBadge,
        Button clearCustomerButton,
        Customer customer
    ) {
        if (customer == null) {
            customerNameLabel.setText("Guest");
            customerPhoneLabel.setText("");
            customerPhoneLabel.setManaged(false);
            customerPhoneLabel.setVisible(false);
            customerStateBadge.setText("Guest");
            customerStateBadge.getStyleClass().remove("pos-customer-badge-selected");
            if (!customerStateBadge.getStyleClass().contains("pos-customer-badge-guest")) {
                customerStateBadge.getStyleClass().add("pos-customer-badge-guest");
            }
            customerCard.getStyleClass().remove("pos-customer-card-selected");
            if (!customerCard.getStyleClass().contains("pos-customer-card-guest")) {
                customerCard.getStyleClass().add("pos-customer-card-guest");
            }
            clearCustomerButton.setDisable(true);
            return;
        }
        customerNameLabel.setText(customer.getFullName());
        customerPhoneLabel.setText(customer.getPhone());
        customerPhoneLabel.setManaged(true);
        customerPhoneLabel.setVisible(true);
        customerStateBadge.setText("Customers");
        customerStateBadge.getStyleClass().remove("pos-customer-badge-guest");
        if (!customerStateBadge.getStyleClass().contains("pos-customer-badge-selected")) {
            customerStateBadge.getStyleClass().add("pos-customer-badge-selected");
        }
        customerCard.getStyleClass().remove("pos-customer-card-guest");
        if (!customerCard.getStyleClass().contains("pos-customer-card-selected")) {
            customerCard.getStyleClass().add("pos-customer-card-selected");
        }
        clearCustomerButton.setDisable(false);
    }
}
