package com.pbl3.project.pbl3_project.ui.dialog;

import com.pbl3.project.pbl3_project.entity.Role;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.service.ToastService;
import com.pbl3.project.pbl3_project.service.UserAccountService;
import com.pbl3.project.pbl3_project.ui.component.DialogFormFactory;
import com.pbl3.project.pbl3_project.ui.util.AsyncUiTask;
import com.pbl3.project.pbl3_project.ui.util.DialogSupport;
import com.pbl3.project.pbl3_project.ui.util.FxFormatters;
import java.text.MessageFormat;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public final class AccountDialog {

    private AccountDialog() {
    }

    public record Context(
        UserAccountService userAccountService,
        ToastService toastService,
        BiConsumer<User, User> sessionSynchronizer,
        Predicate<User> canAccessAccounts,
        BiConsumer<Stage, User> accountAccessFallback
    ) {
    }

    public static void showUpsert(Stage owner, User actor, User target, Runnable onSuccess, Context context) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle(target == null ? "Create Account" : "Edit Account");

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.getStyleClass().addAll("dialog-root", "product-dialog-root", "account-dialog-root");

        VBox header = DialogFormFactory.header(target == null ? "Create Account" : "Edit Account", null);

        TextField usernameField = DialogFormFactory.textField(target != null ? target.getUsername() : "", "Username");
        usernameField.getStyleClass().add("product-dialog-input");
        TextField fullNameField = DialogFormFactory.textField(target != null ? target.getFullName() : "", "Full Name");
        fullNameField.getStyleClass().add("product-dialog-input");

        ComboBox<Role> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll(Role.values());
        roleCombo.setValue(target != null ? target.getRole() : Role.STAFF);
        roleCombo.setMaxWidth(Double.MAX_VALUE);
        roleCombo.getStyleClass().addAll("product-dialog-combo-box", "promotion-dialog-combo-box");
        installEnumCells(roleCombo);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText(target == null ? "Temporary Password" : "Optional New Password");
        passwordField.getStyleClass().add("product-dialog-input");
        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText(target == null ? "Confirm Password" : "Confirm New Password");
        confirmField.getStyleClass().add("product-dialog-input");

        GridPane identityGrid = DialogFormFactory.grid();
        identityGrid.add(DialogFormFactory.fieldBlock("Username *", usernameField, null), 0, 0);
        identityGrid.add(DialogFormFactory.fieldBlock("Full Name *", fullNameField, null), 1, 0);
        VBox roleBlock = DialogFormFactory.fieldBlock("Role *", roleCombo, null);
        identityGrid.add(roleBlock, 0, 1, 2, 1);

        GridPane securityGrid = DialogFormFactory.grid();
        securityGrid.add(DialogFormFactory.fieldBlock(target == null ? "Temporary Password *" : "New Password", passwordField, null), 0, 0);
        securityGrid.add(DialogFormFactory.fieldBlock(target == null ? "Confirm Password *" : "Confirm New Password", confirmField, null), 1, 0);

        VBox form = new VBox(
            14,
            DialogFormFactory.section("Profile", identityGrid),
            DialogFormFactory.section("Security", securityGrid)
        );
        form.setFillWidth(true);

        javafx.scene.control.ScrollPane formScroll = new javafx.scene.control.ScrollPane(form);
        formScroll.setFitToWidth(true);
        formScroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        formScroll.getStyleClass().add("product-dialog-scroll");
        VBox.setVgrow(formScroll, Priority.ALWAYS);

        Button saveBtn = new Button(target == null ? "Create" : "Save");
        saveBtn.getStyleClass().addAll("button", "primary-button", "product-dialog-primary-button");
        saveBtn.setDefaultButton(true);
        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("button", "product-dialog-secondary-button", "dialog-cancel-button");
        cancelBtn.setOnAction(e -> dialog.close());
        saveBtn.setOnAction(e -> saveAccount(
            owner,
            dialog,
            saveBtn,
            cancelBtn,
            actor,
            target,
            usernameField,
            fullNameField,
            roleCombo,
            passwordField,
            confirmField,
            onSuccess,
            context
        ));

        HBox actions = new HBox(10, cancelBtn, saveBtn);
        actions.getStyleClass().add("product-dialog-footer");
        actions.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(header, formScroll, actions);

        Scene scene = new Scene(root, 560, 600);
        applyApplicationStyles(scene);
        dialog.setScene(scene);
        dialog.setMinWidth(560);
        dialog.setMinHeight(600);
        DialogSupport.preventInitialFieldFocus(dialog, root);
        dialog.showAndWait();
    }

    public static void showResetPassword(Stage owner, User actor, User target, Runnable onSuccess, Context context) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Reset Password");

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.getStyleClass().addAll("dialog-root", "product-dialog-root", "account-dialog-root");

        VBox header = DialogFormFactory.header(
            "Reset Password",
            MessageFormat.format("Account: {0}", target.getUsername())
        );

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("New Password");
        passwordField.getStyleClass().add("product-dialog-input");
        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Confirm New Password");
        confirmField.getStyleClass().add("product-dialog-input");

        GridPane credentialGrid = DialogFormFactory.grid();
        credentialGrid.add(DialogFormFactory.fieldBlock("New Password *", passwordField, null), 0, 0);
        credentialGrid.add(DialogFormFactory.fieldBlock("Confirm Password *", confirmField, null), 1, 0);

        VBox form = new VBox(14, DialogFormFactory.section("Credentials", credentialGrid));
        form.setFillWidth(true);

        Button resetBtn = new Button("Reset Password");
        resetBtn.getStyleClass().addAll("button", "primary-button", "product-dialog-primary-button");
        resetBtn.setDefaultButton(true);
        resetBtn.setOnAction(e -> {
            try {
                if (!passwordField.getText().equals(confirmField.getText())) {
                    throw new RuntimeException("Password confirmation does not match");
                }
                context.userAccountService().resetUserPassword(actor, target.getId(), passwordField.getText());
                context.toastService().showSuccess("Password reset.");
                dialog.close();
                onSuccess.run();
            } catch (Exception ex) {
                context.toastService().showError(ex.getMessage());
            }
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("button", "product-dialog-secondary-button", "dialog-cancel-button");
        cancelBtn.setOnAction(e -> dialog.close());

        HBox actions = new HBox(10, cancelBtn, resetBtn);
        actions.getStyleClass().add("product-dialog-footer");
        actions.setAlignment(Pos.CENTER_RIGHT);
        root.getChildren().addAll(header, form, actions);

        Scene scene = new Scene(root, 560, 330);
        applyApplicationStyles(scene);
        dialog.setScene(scene);
        dialog.setMinWidth(540);
        dialog.setMinHeight(320);
        DialogSupport.preventInitialFieldFocus(dialog, root);
        dialog.showAndWait();
    }

    private static void saveAccount(
        Stage owner,
        Stage dialog,
        Button saveButton,
        Button cancelButton,
        User actor,
        User target,
        TextField usernameField,
        TextField fullNameField,
        ComboBox<Role> roleCombo,
        PasswordField passwordField,
        PasswordField confirmField,
        Runnable onSuccess,
        Context context
    ) {
        try {
            String username = usernameField.getText();
            String fullName = fullNameField.getText();
            Role role = roleCombo.getValue();
            String password = passwordField.getText();
            String confirmPassword = confirmField.getText();
            if (target == null) {
                if (!password.equals(confirmPassword)) {
                    throw new RuntimeException("Password confirmation does not match");
                }
            } else if (!password.isBlank() || !confirmPassword.isBlank()) {
                if (!password.equals(confirmPassword)) {
                    throw new RuntimeException("Password confirmation does not match");
                }
            }
            Role previousRole = target != null ? target.getRole() : null;
            AsyncUiTask.runButtonTask(
                saveButton,
                cancelButton,
                target == null ? "Creating..." : "Saving...",
                () -> {
                    if (target == null) {
                        return context.userAccountService().createUser(actor, username, password, fullName, role);
                    }
                    User updatedUser = context.userAccountService().updateUserProfile(
                        actor,
                        target.getId(),
                        username,
                        fullName
                    );
                    if (previousRole != role) {
                        updatedUser = context.userAccountService().changeUserRole(actor, target.getId(), role);
                    }
                    if (!password.isBlank() || !confirmPassword.isBlank()) {
                        updatedUser = context.userAccountService().resetUserPassword(actor, target.getId(), password);
                    }
                    return updatedUser;
                },
                updatedUser -> {
                    if (target != null && updatedUser != null) {
                        context.sessionSynchronizer().accept(actor, updatedUser);
                    }
                    context.toastService().showSuccess(target == null ? "Account created." : "Account updated.");
                    dialog.close();
                    if (!context.canAccessAccounts().test(actor)) {
                        context.accountAccessFallback().accept(owner, actor);
                    } else if (onSuccess != null) {
                        onSuccess.run();
                    }
                },
                ex -> context.toastService().showError(ex.getMessage()),
                "account-save"
            );
        } catch (Exception ex) {
            context.toastService().showError(ex.getMessage());
        }
    }

    private static <T extends Enum<T>> void installEnumCells(ComboBox<T> comboBox) {
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(T value) {
                return FxFormatters.enumText(value);
            }

            @Override
            public T fromString(String string) {
                return null;
            }
        });
        comboBox.setCellFactory(listView -> new ListCell<>() {
            {
                getStyleClass().add("promotion-dialog-popup-cell");
            }

            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : FxFormatters.enumText(item));
            }
        });
    }

    private static void applyApplicationStyles(Scene scene) {
        String stylesheet = AccountDialog.class.getResource("/application.css").toExternalForm();
        if (!scene.getStylesheets().contains(stylesheet)) {
            scene.getStylesheets().add(stylesheet);
        }
    }
}
