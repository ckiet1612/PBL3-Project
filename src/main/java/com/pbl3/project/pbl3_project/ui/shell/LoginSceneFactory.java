package com.pbl3.project.pbl3_project.ui.shell;

import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.service.AuthService;
import com.pbl3.project.pbl3_project.service.ToastService;
import java.net.URL;
import java.util.function.Consumer;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.stage.Stage;
import javafx.util.Duration;

public final class LoginSceneFactory {

    public record Context(
        Stage stage,
        AuthService authService,
        ToastService toastService,
        Consumer<User> onAuthenticated,
        Runnable onUseAnotherWorkspace
    ) {
    }

    public void show(Context context) {
        StackPane mainRoot = new StackPane();
        mainRoot.getStyleClass().add("login-root");

        HBox loginShell = new HBox();
        loginShell.getStyleClass().add("login-shell");
        loginShell.setMaxWidth(Double.MAX_VALUE);
        loginShell.setMaxHeight(Double.MAX_VALUE);
        loginShell.setFillHeight(true);

        VBox brandPanel = createBrandPanel();
        StackPane formPanel = createFormPanel(context, mainRoot);
        loginShell.getChildren().addAll(brandPanel, formPanel);
        mainRoot.getChildren().add(loginShell);

        Scene scene = new Scene(mainRoot, 880, 620);
        applyApplicationStyles(scene);
        context.toastService().setScene(scene);
        context.stage().setScene(scene);
        context.stage().setMinWidth(760);
        context.stage().setMinHeight(500);
        context.stage().centerOnScreen();
        TextField usernameField = (TextField) scene.lookup("#login-username-field");
        Platform.runLater(() -> {
            if (usernameField != null) {
                usernameField.requestFocus();
            }
        });
    }

    private VBox createBrandPanel() {
        VBox brandPanel = new VBox(18);
        brandPanel.getStyleClass().add("login-brand-panel");
        brandPanel.setPrefWidth(340);
        brandPanel.setMinWidth(300);

        Label brandTitle = new Label("SALES MGR");
        brandTitle.getStyleClass().add("login-brand-title");
        Label brandSubtitle = new Label("Inventory • Sales • Reports");
        brandSubtitle.getStyleClass().add("login-brand-subtitle");

        VBox brandFeatureList = new VBox(12);
        brandFeatureList.getChildren().addAll(
            createBrandFeature("Fast checkout", "Close sales quickly at the counter"),
            createBrandFeature("Stock-aware operations", "Keep product movement visible"),
            createBrandFeature("Actionable reports", "Track revenue, profit, and alerts")
        );

        Region brandSpacer = new Region();
        VBox.setVgrow(brandSpacer, Priority.ALWAYS);
        Label brandFooter = new Label("Daily store operations in one workspace");
        brandFooter.getStyleClass().add("login-brand-footer");
        brandPanel.getChildren().addAll(createBrandMark(), brandTitle, brandSubtitle, brandFeatureList, brandSpacer, brandFooter);
        return brandPanel;
    }

    private StackPane createFormPanel(Context context, StackPane mainRoot) {
        StackPane formPanel = new StackPane();
        formPanel.getStyleClass().add("login-form-panel");
        formPanel.setPrefWidth(480);
        formPanel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(formPanel, Priority.ALWAYS);

        VBox formContent = new VBox(16);
        formContent.setFillWidth(true);
        formContent.setMaxWidth(Double.MAX_VALUE);
        formContent.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane.setAlignment(formContent, Pos.CENTER_LEFT);

        Label titleLabel = new Label("Welcome back");
        titleLabel.getStyleClass().add("login-title");
        Label subtitleLabel = new Label("Sign in to continue");
        subtitleLabel.getStyleClass().add("login-subtitle");

        Label errorText = new Label();
        errorText.getStyleClass().add("login-error-text");
        errorText.setWrapText(true);
        HBox errorBanner = createErrorBanner(errorText);
        setLoginError(errorBanner, errorText, null);

        TextField usernameField = createLoginInput("Enter username");
        usernameField.setId("login-username-field");
        VBox usernameBlock = createFieldBlock("Username", usernameField);

        PasswordField passwordField = new PasswordField();
        TextField visiblePasswordField = new TextField();
        passwordField.textProperty().bindBidirectional(visiblePasswordField.textProperty());
        configureEmbeddedPasswordField(passwordField, "Enter password");
        configureEmbeddedPasswordField(visiblePasswordField, "Enter password");

        BooleanProperty passwordVisible = new SimpleBooleanProperty(false);
        passwordField.visibleProperty().bind(passwordVisible.not());
        passwordField.managedProperty().bind(passwordVisible.not());
        visiblePasswordField.visibleProperty().bind(passwordVisible);
        visiblePasswordField.managedProperty().bind(passwordVisible);

        StackPane passwordFieldStack = new StackPane(passwordField, visiblePasswordField);
        HBox.setHgrow(passwordFieldStack, Priority.ALWAYS);

        Button passwordToggle = new Button();
        passwordToggle.getStyleClass().add("login-password-toggle");
        passwordToggle.setCursor(Cursor.HAND);
        passwordToggle.setFocusTraversable(false);
        passwordToggle.setGraphic(createEyeIcon(false));
        passwordVisible.addListener((obs, oldValue, visible) -> passwordToggle.setGraphic(createEyeIcon(visible)));
        passwordToggle.setOnAction(event -> {
            passwordVisible.set(!passwordVisible.get());
            Platform.runLater(() -> {
                TextField activePasswordField = passwordVisible.get() ? visiblePasswordField : passwordField;
                activePasswordField.requestFocus();
                activePasswordField.positionCaret(activePasswordField.getText() == null ? 0 : activePasswordField.getText().length());
            });
        });

        HBox passwordShell = new HBox(8, passwordFieldStack, passwordToggle);
        passwordShell.getStyleClass().add("login-password-shell");
        passwordShell.setAlignment(Pos.CENTER_LEFT);
        passwordShell.setMaxWidth(Double.MAX_VALUE);
        Runnable syncPasswordFocus = () -> setStyleClassActive(
            passwordShell,
            "focused",
            passwordField.isFocused() || visiblePasswordField.isFocused()
        );
        passwordField.focusedProperty().addListener((obs, oldValue, newValue) -> syncPasswordFocus.run());
        visiblePasswordField.focusedProperty().addListener((obs, oldValue, newValue) -> syncPasswordFocus.run());
        VBox passwordBlock = createFieldBlock("Password", passwordShell);

        Button loginButton = createSubmitButton(mainRoot);
        Runnable clearError = () -> {
            setLoginError(errorBanner, errorText, null);
            setStyleClassActive(usernameField, "login-input-error", false);
            setStyleClassActive(passwordShell, "login-input-error", false);
        };
        usernameField.textProperty().addListener((obs, oldValue, newValue) -> clearError.run());
        passwordField.textProperty().addListener((obs, oldValue, newValue) -> clearError.run());
        loginButton.setOnAction(e -> handleLogin(
            context,
            formContent,
            errorBanner,
            errorText,
            usernameField,
            passwordField,
            visiblePasswordField,
            passwordShell,
            passwordToggle,
            loginButton,
            clearError
        ));

        formContent.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                return;
            }
            newScene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.ESCAPE && errorBanner.isVisible()) {
                    clearError.run();
                    event.consume();
                }
            });
        });
        formContent.getChildren().addAll(titleLabel, subtitleLabel, errorBanner, usernameBlock, passwordBlock, loginButton);
        formPanel.getChildren().add(formContent);
        if (context.onUseAnotherWorkspace() != null) {
            Button changeWorkspaceButton = createChangeWorkspaceButton(context.onUseAnotherWorkspace());
            formPanel.getChildren().add(changeWorkspaceButton);
            StackPane.setAlignment(changeWorkspaceButton, Pos.BOTTOM_RIGHT);
        }
        return formPanel;
    }

    private void handleLogin(
        Context context,
        VBox formPanel,
        HBox errorBanner,
        Label errorText,
        TextField usernameField,
        PasswordField passwordField,
        TextField visiblePasswordField,
        HBox passwordShell,
        Button passwordToggle,
        Button loginButton,
        Runnable clearError
    ) {
        clearError.run();
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();
        if (username.isBlank() || password.isBlank()) {
            setLoginError(errorBanner, errorText, "Enter both username and password.");
            setStyleClassActive(usernameField, "login-input-error", username.isBlank());
            setStyleClassActive(passwordShell, "login-input-error", password.isBlank());
            if (username.isBlank()) {
                usernameField.requestFocus();
            } else {
                passwordField.requestFocus();
            }
            playFailureMotion(formPanel);
            return;
        }

        setLoginBusy(loginButton, usernameField, passwordField, visiblePasswordField, passwordToggle, true);
        boolean authenticated = false;
        try {
            User user = context.authService().login(username, password);
            if (user != null) {
                authenticated = true;
                context.stage().setTitle("Sales Management System");
                context.onAuthenticated().accept(user);
                return;
            }
            setLoginError(errorBanner, errorText, "Invalid username or password.");
            setStyleClassActive(usernameField, "login-input-error", true);
            setStyleClassActive(passwordShell, "login-input-error", true);
            playFailureMotion(formPanel);
        } catch (Exception ex) {
            ex.printStackTrace();
            setLoginError(errorBanner, errorText, "Could not sign in. Please try again.");
            setStyleClassActive(usernameField, "login-input-error", true);
            setStyleClassActive(passwordShell, "login-input-error", true);
            playFailureMotion(formPanel);
        } finally {
            if (!authenticated) {
                setLoginBusy(loginButton, usernameField, passwordField, visiblePasswordField, passwordToggle, false);
            }
        }
    }

    private Button createSubmitButton(StackPane mainRoot) {
        Button loginButton = new Button("Sign in");
        loginButton.getStyleClass().add("login-submit-button");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setDefaultButton(true);
        loginButton.setCursor(Cursor.HAND);
        loginButton.setOnMousePressed(e -> {
            if (isReducedMotionNode(mainRoot)) {
                return;
            }
            ScaleTransition transition = new ScaleTransition(Duration.millis(100), loginButton);
            transition.setToX(0.95);
            transition.setToY(0.95);
            transition.play();
        });
        loginButton.setOnMouseReleased(e -> {
            if (isReducedMotionNode(mainRoot)) {
                return;
            }
            ScaleTransition transition = new ScaleTransition(Duration.millis(100), loginButton);
            transition.setToX(1.0);
            transition.setToY(1.0);
            transition.play();
        });
        return loginButton;
    }

    private Button createChangeWorkspaceButton(Runnable onUseAnotherWorkspace) {
        Button button = new Button("Use another workspace");
        button.getStyleClass().add("login-change-workspace-button");
        button.setCursor(Cursor.HAND);
        button.setFocusTraversable(false);
        button.setOnAction(event -> onUseAnotherWorkspace.run());
        return button;
    }

    private Node createBrandMark() {
        StackPane mark = new StackPane();
        URL iconUrl = getClass().getResource("/images/AppIcon.png");
        if (iconUrl == null) {
            iconUrl = getClass().getResource("/AppIcon/AppIcon.png");
        }
        if (iconUrl != null) {
            ImageView icon = new ImageView(new Image(iconUrl.toExternalForm()));
            icon.setFitWidth(64);
            icon.setFitHeight(64);
            icon.setPreserveRatio(true);
            icon.setSmooth(true);
            mark.getChildren().add(icon);
        } else {
            Label fallback = new Label("S");
            fallback.getStyleClass().add("login-brand-mark-fallback");
            mark.getChildren().add(fallback);
        }
        mark.getStyleClass().add("login-brand-mark");
        return mark;
    }

    private HBox createBrandFeature(String titleText, String detailText) {
        SVGPath checkIcon = new SVGPath();
        checkIcon.setContent("M5 12l4 4L19 6");
        checkIcon.setFill(Color.TRANSPARENT);
        checkIcon.setStrokeWidth(2.2);
        checkIcon.setStrokeLineCap(StrokeLineCap.ROUND);
        checkIcon.setStrokeLineJoin(StrokeLineJoin.ROUND);
        checkIcon.getStyleClass().add("login-feature-icon");
        StackPane iconWrap = new StackPane(checkIcon);
        iconWrap.getStyleClass().add("login-feature-icon-wrap");
        Label title = new Label(titleText);
        title.getStyleClass().add("login-feature-title");
        Label detail = new Label(detailText);
        detail.getStyleClass().add("login-feature-detail");
        detail.setWrapText(true);
        VBox textBox = new VBox(2, title, detail);
        HBox.setHgrow(textBox, Priority.ALWAYS);
        HBox row = new HBox(10, iconWrap, textBox);
        row.getStyleClass().add("login-feature-row");
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    private TextField createLoginInput(String promptText) {
        TextField field = new TextField();
        field.setPromptText(promptText);
        field.getStyleClass().add("login-input");
        field.setMinHeight(44);
        field.setPrefHeight(44);
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private void configureEmbeddedPasswordField(TextInputControl field, String promptText) {
        field.setPromptText(promptText);
        field.getStyleClass().add("login-input-embedded");
        field.setMinHeight(42);
        field.setPrefHeight(42);
        field.setMaxWidth(Double.MAX_VALUE);
    }

    private VBox createFieldBlock(String labelText, Node input) {
        Label label = new Label(labelText);
        label.getStyleClass().add("login-field-label");
        VBox block = new VBox(7, label, input);
        block.getStyleClass().add("login-field-block");
        block.setFillWidth(true);
        if (input instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        return block;
    }

    private HBox createErrorBanner(Label errorText) {
        SVGPath icon = new SVGPath();
        icon.setContent("M12 8v5 M12 17h.01 M10.3 4.4 2.4 18a2 2 0 0 0 1.7 3h15.8a2 2 0 0 0 1.7-3L13.7 4.4a2 2 0 0 0-3.4 0z");
        icon.setFill(Color.TRANSPARENT);
        icon.setStrokeWidth(1.8);
        icon.setStrokeLineCap(StrokeLineCap.ROUND);
        icon.setStrokeLineJoin(StrokeLineJoin.ROUND);
        icon.getStyleClass().add("login-error-icon");
        HBox banner = new HBox(10, icon, errorText);
        banner.getStyleClass().add("login-error-banner");
        banner.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(errorText, Priority.ALWAYS);
        return banner;
    }

    private Node createEyeIcon(boolean hidden) {
        SVGPath icon = new SVGPath();
        icon.setContent(hidden
            ? "M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z M12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6z M4 4l16 16"
            : "M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z M12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6z");
        icon.setFill(Color.TRANSPARENT);
        icon.setStrokeWidth(1.8);
        icon.setStrokeLineCap(StrokeLineCap.ROUND);
        icon.setStrokeLineJoin(StrokeLineJoin.ROUND);
        icon.getStyleClass().add("login-eye-icon");
        return icon;
    }

    private void setLoginError(HBox errorBanner, Label errorText, String message) {
        boolean hasError = message != null && !message.isBlank();
        errorText.setText(hasError ? message : "");
        errorBanner.setVisible(hasError);
        errorBanner.setManaged(hasError);
    }

    private void setLoginBusy(
        Button loginButton,
        TextInputControl usernameField,
        TextInputControl passwordField,
        TextInputControl visiblePasswordField,
        Button passwordToggle,
        boolean busy
    ) {
        loginButton.setDisable(busy);
        loginButton.setText(busy ? "Signing in..." : "Sign in");
        usernameField.setDisable(busy);
        passwordField.setDisable(busy);
        visiblePasswordField.setDisable(busy);
        passwordToggle.setDisable(busy);
    }

    private void playFailureMotion(Node node) {
        if (isReducedMotionNode(node)) {
            return;
        }
        TranslateTransition shake = new TranslateTransition(Duration.millis(45), node);
        shake.setFromX(0);
        shake.setByX(6f);
        shake.setCycleCount(4);
        shake.setAutoReverse(true);
        shake.setOnFinished(event -> node.setTranslateX(0));
        shake.playFromStart();
    }

    private boolean isReducedMotionNode(Node node) {
        Scene scene = node == null ? null : node.getScene();
        return scene != null && scene.getRoot() != null && scene.getRoot().getStyleClass().contains("ui-reduced-motion");
    }

    private void setStyleClassActive(Node node, String styleClass, boolean active) {
        if (node == null || styleClass == null || styleClass.isBlank()) {
            return;
        }
        if (active) {
            if (!node.getStyleClass().contains(styleClass)) {
                node.getStyleClass().add(styleClass);
            }
        } else {
            node.getStyleClass().remove(styleClass);
        }
    }

    private void applyApplicationStyles(Scene scene) {
        if (scene == null) {
            return;
        }
        String stylesheet = getClass().getResource("/application.css").toExternalForm();
        if (!scene.getStylesheets().contains(stylesheet)) {
            scene.getStylesheets().add(stylesheet);
        }
    }
}
