package com.pbl3.project.pbl3_project.ui.scene;

import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.ui.component.DialogFormFactory;
import com.pbl3.project.pbl3_project.ui.util.FxFormatters;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.text.Text;

public final class MyAccountScene {
    public record Options() {
    }

    private MyAccountScene() {
    }

    public static Node create(SceneRuntimeContext context, User user, Options options) {
        VBox content = new VBox(18);
        content.getStyleClass().add("my-account-root");
        content.setPadding(new Insets(24));

        HBox workspace = new HBox(18);
        workspace.setAlignment(Pos.TOP_LEFT);
        workspace.getStyleClass().add("my-account-workspace");

        VBox profileCard = createProfileCard(user);
        VBox securityCard = createSecurityCard(context, user);
        HBox.setHgrow(securityCard, Priority.ALWAYS);
        workspace.getChildren().addAll(profileCard, securityCard);

        content.getChildren().add(workspace);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.getStyleClass().add("my-account-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scrollPane;
    }

    private static VBox createProfileCard(User user) {
        VBox card = new VBox(14);
        card.getStyleClass().addAll("report-section-card", "my-account-profile-card");
        card.setMinWidth(420);
        card.setPrefWidth(460);
        card.setMaxWidth(520);

        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);

        Text avatarInitial = new Text(accountInitial(user));
        avatarInitial.getStyleClass().add("my-account-avatar-initial");
        StackPane avatar = new StackPane(avatarInitial);
        avatar.getStyleClass().add("my-account-avatar");

        VBox identity = new VBox(4);
        Label name = new Label(displayValue(user.getFullName()));
        name.getStyleClass().add("my-account-name");
        name.setWrapText(true);
        name.setMaxWidth(Double.MAX_VALUE);
        Label username = new Label("@" + displayValue(user.getUsername()));
        username.getStyleClass().add("my-account-username");
        identity.getChildren().addAll(name, username);
        HBox.setHgrow(identity, Priority.ALWAYS);

        Label statusChip = new Label(FxFormatters.userStatus(user.isEnabled()));
        statusChip.getStyleClass().addAll("my-account-status-chip", user.isEnabled() ? "active" : "disabled");

        header.getChildren().addAll(avatar, identity, statusChip);

        GridPane details = new GridPane();
        details.getStyleClass().add("my-account-details-grid");
        details.setHgap(14);
        details.setVgap(12);
        ColumnConstraints firstColumn = new ColumnConstraints();
        firstColumn.setPercentWidth(50);
        ColumnConstraints secondColumn = new ColumnConstraints();
        secondColumn.setPercentWidth(50);
        details.getColumnConstraints().addAll(firstColumn, secondColumn);
        details.add(createDetailBlock("Username", displayValue(user.getUsername())), 0, 0);
        details.add(createDetailBlock("Full Name", displayValue(user.getFullName())), 1, 0);
        details.add(createDetailBlock("Role", FxFormatters.roleLabel(user.getRole())), 0, 1);
        details.add(createDetailBlock("Account Status", FxFormatters.userStatus(user.isEnabled())), 1, 1);

        card.getChildren().addAll(header, details);
        return card;
    }

    private static VBox createSecurityCard(SceneRuntimeContext context, User user) {
        VBox card = new VBox(14);
        card.getStyleClass().addAll("report-section-card", "my-account-security-card");
        card.setFillWidth(true);

        VBox heading = new VBox(4);
        Label title = new Label("Change Password");
        title.getStyleClass().add("my-account-section-title");
        Label subtitle = new Label("Use a new password that is different from your current one.");
        subtitle.getStyleClass().add("my-account-section-subtitle");
        subtitle.setWrapText(true);
        heading.getChildren().addAll(title, subtitle);

        Label errorBanner = new Label();
        errorBanner.getStyleClass().add("my-account-error-banner");
        setMessage(errorBanner, null);

        PasswordField currentField = passwordField("Current password");
        PasswordField newField = passwordField("New password");
        PasswordField confirmField = passwordField("Confirm new password");

        GridPane form = new GridPane();
        form.getStyleClass().add("my-account-password-grid");
        form.setHgap(14);
        form.setVgap(12);
        form.add(DialogFormFactory.fieldBlock("Current Password", currentField, null), 0, 0, 2, 1);
        form.add(DialogFormFactory.fieldBlock("New Password", newField, null), 0, 1);
        form.add(DialogFormFactory.fieldBlock("Confirm Password", confirmField, null), 1, 1);

        Button changeButton = new Button("Save Password");
        changeButton.getStyleClass().addAll("button", "primary-button", "my-account-primary-button");
        changeButton.setDefaultButton(true);

        changeButton.setOnAction(e -> {
            String validationMessage = validatePasswordInput(currentField, newField, confirmField);
            if (validationMessage != null) {
                setMessage(errorBanner, validationMessage);
                return;
            }
            setMessage(errorBanner, null);
            changeButton.setDisable(true);
            changeButton.setText("Saving...");
            try {
                User updatedUser = context.userAccountService()
                    .changeOwnPassword(user, currentField.getText(), newField.getText(), confirmField.getText());
                syncSessionUser(user, updatedUser);
                currentField.clear();
                newField.clear();
                confirmField.clear();
                context.toastService().showSuccess("Password updated.");
            } catch (Exception ex) {
                setMessage(errorBanner, ex.getMessage() == null || ex.getMessage().isBlank()
                    ? "Could not update password."
                    : ex.getMessage());
            } finally {
                changeButton.setDisable(false);
                changeButton.setText("Save Password");
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(spacer, changeButton);
        actions.getStyleClass().add("my-account-actions");
        actions.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(heading, errorBanner, form, actions);
        Platform.runLater(() -> card.requestFocus());
        return card;
    }

    private static VBox createDetailBlock(String labelText, String valueText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("my-account-detail-label");
        Label value = new Label(valueText);
        value.getStyleClass().add("my-account-detail-value");
        value.setWrapText(true);
        VBox block = new VBox(4, label, value);
        block.getStyleClass().add("my-account-detail-block");
        block.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(block, Priority.ALWAYS);
        GridPane.setFillWidth(block, true);
        return block;
    }

    private static PasswordField passwordField(String prompt) {
        PasswordField field = new PasswordField();
        field.setPromptText(prompt);
        field.getStyleClass().addAll("product-dialog-input", "my-account-input");
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private static String validatePasswordInput(
        PasswordField currentField,
        PasswordField newField,
        PasswordField confirmField
    ) {
        if (isBlank(currentField.getText())) {
            return "Current password is required.";
        }
        if (isBlank(newField.getText())) {
            return "New password is required.";
        }
        if (isBlank(confirmField.getText())) {
            return "Confirm the new password.";
        }
        if (!newField.getText().equals(confirmField.getText())) {
            return "New password and confirmation do not match.";
        }
        return null;
    }

    private static void setMessage(Label label, String message) {
        boolean visible = message != null && !message.isBlank();
        label.setText(visible ? message : "");
        label.setVisible(visible);
        label.setManaged(visible);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String accountInitial(User user) {
        String source = user != null && !isBlank(user.getFullName()) ? user.getFullName() : user != null ? user.getUsername() : null;
        if (isBlank(source)) {
            return "?";
        }
        return source.trim().substring(0, 1).toUpperCase();
    }

    private static String displayValue(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static void syncSessionUser(User sessionUser, User updatedUser) {
        if (sessionUser == null || updatedUser == null || sessionUser.getId() == null || updatedUser.getId() == null) {
            return;
        }
        if (!sessionUser.getId().equals(updatedUser.getId())) {
            return;
        }
        sessionUser.setUsername(updatedUser.getUsername());
        sessionUser.setFullName(updatedUser.getFullName());
        sessionUser.setRole(updatedUser.getRole());
        sessionUser.setEnabled(updatedUser.isEnabled());
        sessionUser.setPassword(updatedUser.getPassword());
    }
}
