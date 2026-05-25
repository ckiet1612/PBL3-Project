package com.pbl3.project.pbl3_project.ui.bootstrap;

import com.pbl3.project.pbl3_project.ui.util.DialogSupport;
import com.pbl3.project.pbl3_project.ui.util.ResponsiveSceneSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public final class TenantBootstrapSceneFactory {

    private static boolean fontsLoaded;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build();

    public record Context(
        Stage stage,
        TenantBootstrapStore store,
        String provisioningApiBaseUrl,
        String provisioningApiKey,
        Consumer<TenantBootstrapStore.TenantConfig> onTenantReady
    ) {
    }

    public void show(Context context) {
        show(context, null, false);
    }

    public void show(Context context, String initialMessage, boolean initialMessageIsError) {
        loadBootstrapFonts();

        StackPane root = new StackPane();
        root.getStyleClass().addAll("login-root", "tenant-bootstrap-root");

        HBox shell = new HBox();
        shell.getStyleClass().add("tenant-bootstrap-shell");
        shell.setMaxWidth(Double.MAX_VALUE);
        shell.setMaxHeight(Double.MAX_VALUE);
        shell.setFillHeight(true);
        shell.prefWidthProperty().bind(root.widthProperty());
        shell.prefHeightProperty().bind(root.heightProperty());

        VBox brandPanel = createBrandPanel();
        VBox formPanel = createWorkspaceFormPanel(context, initialMessage, initialMessageIsError);
        shell.getChildren().addAll(brandPanel, formPanel);
        root.getChildren().add(shell);

        Scene scene = new Scene(root, 880, 620);
        applyApplicationStyles(scene);
        context.stage().setScene(scene);
        context.stage().setTitle("Sales Management System");
        context.stage().setMinWidth(760);
        context.stage().setMinHeight(500);
        context.stage().centerOnScreen();
        context.stage().show();
    }

    public void showStarting(Stage stage) {
        loadBootstrapFonts();

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.getStyleClass().add("tenant-bootstrap-loading-spinner");
        spinner.setPrefSize(64, 64);

        Label titleLabel = createLabel("Starting Workspace", "tenant-bootstrap-loading-title");
        Label subtitleLabel = createLabel("Preparing your workspace...", "tenant-bootstrap-loading-subtitle");

        VBox loadingContent = new VBox(16, spinner, titleLabel, subtitleLabel);
        loadingContent.getStyleClass().add("tenant-bootstrap-loading-content");
        loadingContent.setAlignment(Pos.CENTER);
        loadingContent.setPadding(new Insets(36));

        StackPane root = new StackPane(loadingContent);
        root.getStyleClass().addAll("login-root", "tenant-bootstrap-root", "tenant-bootstrap-loading-root");

        Scene scene = new Scene(root, 880, 620);
        applyApplicationStyles(scene);
        stage.setScene(scene);
        stage.setTitle("Sales Management System");
        stage.centerOnScreen();
        stage.show();
    }

    private VBox createBrandPanel() {
        VBox brandPanel = new VBox(18);
        brandPanel.getStyleClass().add("tenant-bootstrap-brand-panel");
        brandPanel.setPrefWidth(320);
        brandPanel.setMinWidth(290);
        brandPanel.setMaxHeight(Double.MAX_VALUE);

        Node brandMark = createBrandMark();
        Label brandTitle = createLabel("SALES MGR", "tenant-bootstrap-brand-title");
        Label brandSubtitle = createLabel("Business Workspace", "tenant-bootstrap-brand-subtitle");

        VBox featureList = new VBox(12);
        featureList.getChildren().addAll(
            createBrandFeature("Create tenant workspace"),
            createBrandFeature("Join with business code"),
            createBrandFeature("Use shared sales data")
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Label footer = createLabel("Configure once, then open the workspace directly.", "tenant-bootstrap-brand-footer");
        footer.setWrapText(true);

        brandPanel.getChildren().addAll(brandMark, brandTitle, brandSubtitle, featureList, spacer, footer);
        return brandPanel;
    }

    private HBox createBrandFeature(String text) {
        Region bullet = new Region();
        bullet.getStyleClass().add("tenant-bootstrap-feature-dot");
        Label label = createLabel(text, "tenant-bootstrap-feature-title");
        HBox row = new HBox(10, bullet, label);
        row.getStyleClass().add("tenant-bootstrap-feature-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
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
            Label fallback = createLabel("S", "tenant-bootstrap-brand-mark-fallback");
            mark.getChildren().add(fallback);
        }
        mark.getStyleClass().add("tenant-bootstrap-brand-mark");
        return mark;
    }

    private VBox createWorkspaceFormPanel(Context context, String initialMessage, boolean initialMessageIsError) {
        VBox formPanel = new VBox(14);
        formPanel.getStyleClass().addAll("tenant-bootstrap-form-panel", "tenant-bootstrap-workspace-form-panel");
        formPanel.setPrefWidth(540);
        formPanel.setMaxWidth(Double.MAX_VALUE);
        formPanel.setMaxHeight(Double.MAX_VALUE);
        HBox.setHgrow(formPanel, Priority.ALWAYS);

        Label title = createLabel("Connect your business", "tenant-bootstrap-form-title");
        VBox header = new VBox(title);

        ToggleButton joinToggle = createSegmentButton("Join Business");
        ToggleButton createToggle = createSegmentButton("Create Business");
        ToggleGroup toggleGroup = new ToggleGroup();
        joinToggle.setToggleGroup(toggleGroup);
        createToggle.setToggleGroup(toggleGroup);
        joinToggle.setSelected(true);
        toggleGroup.selectedToggleProperty().addListener((obs, previousToggle, selectedToggle) -> {
            if (selectedToggle == null) {
                toggleGroup.selectToggle(previousToggle != null ? previousToggle : joinToggle);
            }
        });
        Label joinMessage = createMessageLabel();
        Label createMessage = createMessageLabel();
        setMessage(joinMessage, initialMessage, initialMessageIsError);
        setMessage(createMessage, null, false);
        joinToggle.selectedProperty().addListener((obs, wasSelected, selected) -> {
            if (selected) {
                setMessage(createMessage, null, false);
            }
        });
        createToggle.selectedProperty().addListener((obs, wasSelected, selected) -> {
            if (selected) {
                setMessage(joinMessage, null, false);
            }
        });

        HBox segmentedSwitch = new HBox(4, joinToggle, createToggle);
        segmentedSwitch.getStyleClass().add("tenant-bootstrap-segmented");
        HBox.setHgrow(joinToggle, Priority.ALWAYS);
        HBox.setHgrow(createToggle, Priority.ALWAYS);

        VBox joinForm = createJoinBusinessForm(context, joinMessage);
        VBox createForm = createCreateBusinessForm(context, createMessage);
        joinForm.visibleProperty().bind(joinToggle.selectedProperty());
        joinForm.managedProperty().bind(joinToggle.selectedProperty());
        createForm.visibleProperty().bind(createToggle.selectedProperty());
        createForm.managedProperty().bind(createToggle.selectedProperty());

        StackPane formStack = new StackPane(joinForm, createForm);
        formStack.getStyleClass().add("tenant-bootstrap-form-stack");
        StackPane.setAlignment(joinForm, Pos.TOP_LEFT);
        StackPane.setAlignment(createForm, Pos.TOP_LEFT);

        formPanel.getChildren().addAll(header, segmentedSwitch, formStack);
        return formPanel;
    }

    private VBox createCreateBusinessForm(Context context, Label message) {
        TextField businessName = createInput("Business name");
        TextField adminFullName = createInput("Admin full name");
        TextField adminUsername = createInput("Admin username");
        PasswordField adminPassword = createPasswordInput("Admin password");

        GridPane grid = createFormGrid();
        grid.add(fieldBlock("Business Name", businessName), 0, 0, 2, 1);
        grid.add(fieldBlock("Admin Full Name", adminFullName), 0, 1);
        grid.add(fieldBlock("Admin Username", adminUsername), 1, 1);
        grid.add(fieldBlock("Admin Password", adminPassword), 0, 2, 2, 1);

        Button createButton = primaryButton("Create Business");
        createButton.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(createButton, Priority.NEVER);

        VBox form = new VBox(16, grid, createButton, message);
        form.getStyleClass().add("tenant-bootstrap-form-body");
        form.setMaxWidth(Double.MAX_VALUE);
        Runnable submitCreate = () -> Platform.runLater(() -> {
            if (createButton.isDisabled()) {
                return;
            }
            String validationError = validateCreateBusinessForm(businessName, adminUsername, adminPassword);
            if (validationError != null) {
                setMessage(message, validationError, true);
                return;
            }
            runAsync(createButton, form, message, "Creating...", () -> {
                JsonNode response = postJson(
                    context,
                    "/api/provisioning/businesses",
                    Map.of(
                        "businessName", safeText(businessName),
                        "adminFullName", safeText(adminFullName),
                        "adminUsername", safeText(adminUsername),
                        "adminPassword", adminPassword.getText() == null ? "" : adminPassword.getText()
                    )
                );
                TenantBootstrapStore.TenantConfig config = configFromJson(response);
                context.store().save(config);
                Platform.runLater(() -> showCreatedBusiness(context, config));
            });
        });
        createButton.setOnAction(event -> submitCreate.run());
        businessName.setOnAction(event -> submitCreate.run());
        adminFullName.setOnAction(event -> submitCreate.run());
        adminUsername.setOnAction(event -> submitCreate.run());
        adminPassword.setOnAction(event -> submitCreate.run());
        return form;
    }

    private VBox createJoinBusinessForm(Context context, Label message) {
        TextField businessCode = createInput("BIZ-ABC123");
        PasswordField joinPin = createPasswordInput("Join PIN");
        VBox field = fieldBlock("Business Code", businessCode);
        VBox pinField = fieldBlock("Join PIN", joinPin);

        Button joinButton = primaryButton("Join Business");
        joinButton.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(joinButton, Priority.NEVER);

        VBox form = new VBox(16, field, pinField, joinButton, message);
        form.getStyleClass().add("tenant-bootstrap-form-body");
        form.setMaxWidth(Double.MAX_VALUE);
        if (context.store().load().isPresent()) {
            Button forgetButton = secondaryButton("Use another workspace");
            forgetButton.setMaxWidth(Double.MAX_VALUE);
            forgetButton.setOnAction(event -> forgetSavedWorkspace(context, message));
            form.getChildren().add(forgetButton);
        }
        Runnable submitJoin = () -> Platform.runLater(() -> {
            if (joinButton.isDisabled()) {
                return;
            }
            if (safeText(businessCode).isBlank()) {
                setMessage(message, "Business code is required.", true);
                return;
            }
            if (safeText(joinPin).isBlank()) {
                setMessage(message, "Join PIN is required.", true);
                return;
            }
            String normalizedCode = safeText(businessCode).toUpperCase(Locale.ROOT);
            String enteredJoinPin = safeText(joinPin);
            runAsyncResult(
                joinButton,
                form,
                message,
                "Checking...",
                () -> previewFromJson(getJson(context, "/api/provisioning/businesses/" + normalizedCode + "/preview")),
                preview -> confirmAndConnectBusiness(context, form, joinButton, message, normalizedCode, enteredJoinPin, preview)
            );
        });
        joinButton.setOnAction(event -> submitJoin.run());
        businessCode.setOnAction(event -> submitJoin.run());
        joinPin.setOnAction(event -> submitJoin.run());
        return form;
    }

    private void showCreatedBusiness(Context context, TenantBootstrapStore.TenantConfig config) {
        loadBootstrapFonts();

        StackPane root = new StackPane();
        root.getStyleClass().addAll("login-root", "tenant-bootstrap-root");

        HBox shell = new HBox();
        shell.getStyleClass().add("tenant-bootstrap-shell");
        shell.setMaxWidth(Double.MAX_VALUE);
        shell.setMaxHeight(Double.MAX_VALUE);
        shell.setFillHeight(true);
        shell.prefWidthProperty().bind(root.widthProperty());
        shell.prefHeightProperty().bind(root.heightProperty());

        VBox brandPanel = createBrandPanel();
        VBox successPanel = createSuccessPanel(context, config);
        shell.getChildren().addAll(brandPanel, successPanel);
        root.getChildren().add(shell);

        Scene scene = new Scene(root, 880, 620);
        applyApplicationStyles(scene);
        context.stage().setScene(scene);
        context.stage().setTitle("Sales Management System");
        context.stage().setMinWidth(760);
        context.stage().setMinHeight(500);
        context.stage().centerOnScreen();
        context.stage().show();
    }

    private VBox createSuccessPanel(Context context, TenantBootstrapStore.TenantConfig config) {
        VBox panel = new VBox(16);
        panel.getStyleClass().addAll("tenant-bootstrap-form-panel", "tenant-bootstrap-success-panel");
        panel.setPrefWidth(540);
        panel.setMaxWidth(Double.MAX_VALUE);
        panel.setMaxHeight(Double.MAX_VALUE);
        HBox.setHgrow(panel, Priority.ALWAYS);

        Label eyebrow = createLabel("Workspace Ready", "tenant-bootstrap-form-eyebrow");
        Label title = createLabel("Business workspace created", "tenant-bootstrap-form-title");
        Label businessName = createLabel(config.businessName(), "tenant-bootstrap-success-business");
        businessName.setWrapText(true);

        Label codeLabel = createLabel("Business Code", "tenant-bootstrap-label");
        Label codeValue = createLabel(config.businessCode(), "tenant-bootstrap-code");
        VBox codeBlock = new VBox(8, codeLabel, codeValue);
        codeBlock.getStyleClass().add("tenant-bootstrap-code-block");

        VBox codeBlocks = new VBox(10, codeBlock);
        boolean hasJoinPin = config.joinPin() != null && !config.joinPin().isBlank();
        if (hasJoinPin) {
            Label pinLabel = createLabel("Join PIN", "tenant-bootstrap-label");
            Label pinValue = createLabel(config.joinPin(), "tenant-bootstrap-code");
            VBox pinBlock = new VBox(8, pinLabel, pinValue);
            pinBlock.getStyleClass().add("tenant-bootstrap-code-block");

            Label pinWarning = createLabel("Save this Join PIN now. It is shown only once and is required when adding another device.", "tenant-bootstrap-message");
            pinWarning.getStyleClass().add("tenant-bootstrap-info");
            pinWarning.setWrapText(true);
            codeBlocks.getChildren().addAll(pinBlock, pinWarning);
        }

        Button copyButton = secondaryButton("Copy Code");
        copyButton.setOnAction(event -> copyText(copyButton, config.businessCode()));

        Button copyPinButton = null;
        if (hasJoinPin) {
            copyPinButton = secondaryButton("Copy PIN");
            copyPinButton.setOnAction(event -> copyText((Button) event.getSource(), config.joinPin()));
        }

        Button openButton = primaryButton("Open Workspace");
        openButton.setOnAction(event -> context.onTenantReady().accept(config));

        HBox actions = new HBox(10);
        actions.getChildren().add(copyButton);
        if (copyPinButton != null) {
            actions.getChildren().add(copyPinButton);
        }
        actions.getChildren().add(openButton);
        actions.getStyleClass().add("tenant-bootstrap-success-actions");
        actions.setAlignment(Pos.CENTER_RIGHT);

        panel.getChildren().addAll(eyebrow, title, businessName, codeBlocks, actions);
        return panel;
    }

    private GridPane createFormGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setMaxWidth(Double.MAX_VALUE);
        return grid;
    }

    private VBox fieldBlock(String labelText, Node field) {
        Label label = createLabel(labelText, "tenant-bootstrap-label");
        VBox block = new VBox(6, label, field);
        block.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(block, Priority.ALWAYS);
        return block;
    }

    private TextField createInput(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.getStyleClass().add("tenant-bootstrap-input");
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private PasswordField createPasswordInput(String prompt) {
        PasswordField field = new PasswordField();
        field.setPromptText(prompt);
        field.getStyleClass().add("tenant-bootstrap-input");
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private ToggleButton createSegmentButton(String text) {
        ToggleButton button = new ToggleButton(text);
        button.getStyleClass().add("tenant-bootstrap-segment-button");
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }

    private Button primaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().addAll("primary-button", "tenant-bootstrap-primary-button");
        return button;
    }

    private Button secondaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("tenant-bootstrap-secondary-button");
        return button;
    }

    private Label createLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setMinHeight(Region.USE_PREF_SIZE);
        return label;
    }

    private Label createMessageLabel() {
        Label message = new Label();
        message.getStyleClass().add("tenant-bootstrap-message");
        message.setWrapText(true);
        message.setMaxWidth(Double.MAX_VALUE);
        message.setVisible(true);
        message.setManaged(true);
        message.setOpacity(0);
        return message;
    }

    private void runAsync(
        Button actionButton,
        Node busyScope,
        Label message,
        String busyButtonText,
        ThrowingRunnable task
    ) {
        runAsyncResult(actionButton, busyScope, message, busyButtonText, () -> {
            task.run();
            return null;
        }, ignored -> {
        });
    }

    private <T> void runAsyncResult(
        Button actionButton,
        Node busyScope,
        Label message,
        String busyButtonText,
        ThrowingSupplier<T> task,
        Consumer<T> onSuccess
    ) {
        String originalText = actionButton.getText();
        setMessage(message, null, false);
        actionButton.setText(busyButtonText);
        actionButton.setDisable(true);
        busyScope.setDisable(true);
        Thread worker = new Thread(() -> {
            try {
                T result = task.get();
                Platform.runLater(() -> {
                    actionButton.setText(originalText);
                    actionButton.setDisable(false);
                    busyScope.setDisable(false);
                    if (onSuccess != null) {
                        Platform.runLater(() -> onSuccess.accept(result));
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setMessage(message, userFriendlyError(ex), true);
                    actionButton.setText(originalText);
                    actionButton.setDisable(false);
                    busyScope.setDisable(false);
                });
            }
        }, "tenant-bootstrap");
        worker.setDaemon(true);
        worker.start();
    }

    private void confirmAndConnectBusiness(
        Context context,
        Node busyScope,
        Button joinButton,
        Label message,
        String businessCode,
        String joinPin,
        TenantPreview preview
    ) {
        String confirmMessage = "Found business: " + preview.businessName() + " (" + preview.businessCode()
            + "). Continue joining this workspace?";
        if (!DialogSupport.showConfirm(context.stage(), "Join Business", confirmMessage)) {
            setMessage(message, "Join cancelled.", false);
            return;
        }
        runAsync(joinButton, busyScope, message, "Joining...", () -> {
            JsonNode response = postJson(
                context,
                "/api/provisioning/businesses/" + businessCode + "/connect",
                Map.of("joinPin", joinPin)
            );
            TenantBootstrapStore.TenantConfig config = configFromJson(response);
            context.store().save(config);
            Platform.runLater(() -> context.onTenantReady().accept(config));
        });
    }

    private void forgetSavedWorkspace(Context context, Label message) {
        if (!DialogSupport.showConfirm(
            context.stage(),
            "Use another workspace",
            "Remove the saved local workspace configuration and return to workspace setup?"
        )) {
            return;
        }
        try {
            context.store().clear();
            show(context);
        } catch (IOException ex) {
            setMessage(message, "Could not remove saved workspace: " + ex.getMessage(), true);
        }
    }

    private void copyText(Button button, String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text == null ? "" : text);
        Clipboard.getSystemClipboard().setContent(content);

        String originalText = button.getText();
        button.setText("Copied");
        PauseTransition delay = new PauseTransition(javafx.util.Duration.millis(900));
        delay.setOnFinished(event -> button.setText(originalText));
        delay.play();
    }

    private String validateCreateBusinessForm(TextField businessName, TextField adminUsername, PasswordField adminPassword) {
        if (safeText(businessName).isBlank()) {
            return "Business name is required.";
        }
        if (safeText(adminUsername).isBlank()) {
            return "Admin username is required.";
        }
        String password = adminPassword.getText() == null ? "" : adminPassword.getText();
        if (password.length() < 4) {
            return "Admin password must contain at least 4 characters.";
        }
        return null;
    }

    private String userFriendlyError(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return "Could not complete provisioning. Please check the API server and try again.";
        }
        if (message.contains("Connection refused")) {
            return "Provisioning API is not running. Start the API server, then try again.";
        }
        if (message.contains("Provisioning API base URL is not configured")) {
            return "Provisioning API URL is not configured. Set PROVISIONING_API_BASE_URL to your HTTPS provisioning server.";
        }
        if (message.contains("Provisioning API must use HTTPS")) {
            return "Provisioning API must use HTTPS. Local HTTP is allowed only when PROVISIONING_API_ALLOW_LOCAL=true.";
        }
        if (message.contains("Failed to obtain JDBC Connection") || message.contains("Communications link failure")) {
            return "Provisioning API cannot connect to the business registry database. Check TiDB/network credentials, then try again.";
        }
        if (message.contains("Access denied")) {
            return "Provisioning API database username or password is not valid.";
        }
        if (message.contains("timed out") || message.contains("HttpTimeoutException")) {
            return "Provisioning API did not respond in time. Check the server log and network connection.";
        }
        return message;
    }

    private JsonNode postJson(Context context, String path, Map<String, String> body) throws IOException, InterruptedException {
        HttpRequest.Builder builder = requestBuilder(context, path)
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
            .header("Content-Type", "application/json");
        return send(builder.build());
    }

    private JsonNode getJson(Context context, String path) throws IOException, InterruptedException {
        return send(requestBuilder(context, path).GET().build());
    }

    private HttpRequest.Builder requestBuilder(Context context, String path) {
        String configuredBaseUrl = context.provisioningApiBaseUrl() == null ? "" : context.provisioningApiBaseUrl().trim();
        if (configuredBaseUrl.isBlank()) {
            throw new IllegalStateException("Provisioning API base URL is not configured");
        }
        String baseUrl = configuredBaseUrl.replaceAll("/+$", "");
        URI baseUri = URI.create(baseUrl);
        boolean https = "https".equalsIgnoreCase(baseUri.getScheme());
        boolean allowedLocalHttp = isLocalProvisioningHost(baseUri.getHost()) && allowLocalProvisioningApi();
        if (!https && !allowedLocalHttp) {
            throw new IllegalStateException("Provisioning API must use HTTPS");
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
            .timeout(Duration.ofMinutes(5))
            .header("Accept", "application/json");
        if (context.provisioningApiKey() != null && !context.provisioningApiKey().isBlank()) {
            builder.header("X-Provisioning-Key", context.provisioningApiKey());
        }
        return builder;
    }

    private boolean isLocalProvisioningHost(String host) {
        if (host == null) {
            return false;
        }
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        return "localhost".equals(normalizedHost)
            || "127.0.0.1".equals(normalizedHost)
            || "::1".equals(normalizedHost);
    }

    private boolean allowLocalProvisioningApi() {
        String value = System.getProperty("PROVISIONING_API_ALLOW_LOCAL");
        if (value == null || value.isBlank()) {
            value = System.getenv("PROVISIONING_API_ALLOW_LOCAL");
        }
        return Boolean.parseBoolean(value);
    }

    private JsonNode send(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(extractErrorMessage(response.body(), response.statusCode()));
        }
        return objectMapper.readTree(response.body());
    }

    private String extractErrorMessage(String body, int statusCode) {
        try {
            JsonNode json = objectMapper.readTree(body);
            if (json.hasNonNull("message")) {
                return json.get("message").asText();
            }
            if (json.hasNonNull("detail")) {
                return json.get("detail").asText();
            }
        } catch (Exception ignored) {
        }
        return "Provisioning request failed with HTTP " + statusCode;
    }

    private TenantBootstrapStore.TenantConfig configFromJson(JsonNode json) {
        return new TenantBootstrapStore.TenantConfig(
            text(json, "businessCode"),
            text(json, "businessName"),
            text(json, "tenantJdbcUrl"),
            firstText(json, "tenantDbUsername", "datasourceUsername"),
            firstText(json, "tenantDbPassword", "datasourcePassword"),
            text(json, "joinPin")
        );
    }

    private TenantPreview previewFromJson(JsonNode json) {
        return new TenantPreview(
            text(json, "businessCode"),
            text(json, "businessName"),
            text(json, "status")
        );
    }

    private String text(JsonNode json, String fieldName) {
        JsonNode node = json.get(fieldName);
        return node == null || node.isNull() ? "" : node.asText();
    }

    private String firstText(JsonNode json, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String value = text(json, fieldName);
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String safeText(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private void setMessage(Label message, String text, boolean error) {
        boolean visible = text != null && !text.isBlank();
        message.setText(visible ? text : " ");
        message.setVisible(true);
        message.setManaged(true);
        message.setOpacity(visible ? 1 : 0);
        message.getStyleClass().removeAll("tenant-bootstrap-error", "tenant-bootstrap-info");
        if (visible) {
            message.getStyleClass().add(error ? "tenant-bootstrap-error" : "tenant-bootstrap-info");
        }
    }

    private void applyApplicationStyles(Scene scene) {
        String stylesheet = getClass().getResource("/application.css").toExternalForm();
        if (!scene.getStylesheets().contains(stylesheet)) {
            scene.getStylesheets().add(stylesheet);
        }
        ResponsiveSceneSupport.install(scene);
    }

    private void loadBootstrapFonts() {
        if (fontsLoaded) {
            return;
        }
        loadFont("/fonts/BeVietnamPro-Regular.ttf", 14);
        loadFont("/fonts/BeVietnamPro-Bold.ttf", 14);
        fontsLoaded = true;
    }

    private void loadFont(String path, double size) {
        try (InputStream stream = getClass().getResourceAsStream(path)) {
            if (stream != null) {
                Font.loadFont(stream, size);
            }
        } catch (IOException ignored) {
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    private record TenantPreview(String businessCode, String businessName, String status) {
    }
}
