package com.pbl3.project.pbl3_project.ui.scene;

import com.pbl3.project.pbl3_project.entity.DashboardSectionKey;
import com.pbl3.project.pbl3_project.entity.UiAccentPreset;
import com.pbl3.project.pbl3_project.entity.UiDensityMode;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.entity.UserUiPreferences;
import com.pbl3.project.pbl3_project.service.DataBackupService;
import com.pbl3.project.pbl3_project.service.ReceiptService;
import com.pbl3.project.pbl3_project.service.SePaySettingsService;
import com.pbl3.project.pbl3_project.ui.component.ButtonFactory;
import com.pbl3.project.pbl3_project.ui.util.DialogSupport;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

public final class SettingsScene {
    public record Options() {
    }

    private record SettingsData(
        UserUiPreferences preferences,
        List<DashboardSectionKey> dashboardOrder,
        Set<DashboardSectionKey> hiddenSections,
        File receiptOutputDirectory,
        ReceiptService.ReceiptSettings receiptSettings,
        SePaySettingsService.SePaySettings sePaySettings
    ) {
    }

    private record SePaySettingsControls(
        CheckBox enabledCheck,
        PasswordField apiTokenField,
        PasswordField webhookApiKeyField,
        TextField bankShortNameField,
        TextField accountNumberField,
        Label statusLabel,
        VBox section
    ) {
    }

    private SettingsScene() {
    }

    public static Node create(SceneRuntimeContext context, User user, Options options) {
        ScrollPane scrollPane = new ScrollPane(createSettingsStateContent(
            "Loading settings",
            "Preparing your workspace preferences...",
            null,
            true
        ));
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("settings-scroll-pane");

        VBox root = new VBox(scrollPane);
        root.getStyleClass().add("settings-root");
        root.setFillWidth(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        loadSettingsData(context, user, scrollPane);
        return root;
    }

    private static void loadSettingsData(SceneRuntimeContext context, User user, ScrollPane scrollPane) {
        scrollPane.setContent(createSettingsStateContent(
            "Loading settings",
            "Preparing your workspace preferences...",
            null,
            true
        ));

        javafx.concurrent.Task<SettingsData> task = new javafx.concurrent.Task<>() {
            @Override
            protected SettingsData call() {
                return new SettingsData(
                    context.userUiPreferencesService().getPreferences(user),
                    new ArrayList<>(context.userUiPreferencesService().resolveDashboardSectionOrder(user)),
                    new LinkedHashSet<>(context.userUiPreferencesService().resolveHiddenDashboardSections(user)),
                    context.receiptService().getReceiptOutputDirectory(),
                    context.receiptService().getReceiptSettings(),
                    context.authorizationService().canAccessDataBackup(user)
                        ? context.sePaySettingsService().getSettings(user)
                        : null
                );
            }
        };

        task.setOnSucceeded(event -> scrollPane.setContent(createSettingsContent(context, user, task.getValue())));
        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            Button retryButton = ButtonFactory.pageNav("Retry");
            retryButton.getStyleClass().add("settings-secondary-button");
            retryButton.setOnAction(e -> loadSettingsData(context, user, scrollPane));
            scrollPane.setContent(createSettingsStateContent(
                "Settings unavailable",
                ex != null && ex.getMessage() != null && !ex.getMessage().isBlank()
                    ? "Could not load settings: " + ex.getMessage()
                    : "Could not load settings.",
                retryButton,
                false
            ));
            if (ex != null) {
                context.showUserFacingError(ex);
            }
        });

        Thread worker = new Thread(task, "settings-loader");
        worker.setDaemon(true);
        worker.start();
    }

    private static VBox createSettingsContent(SceneRuntimeContext context, User user, SettingsData data) {
        UserUiPreferences preferences = data.preferences();

        ComboBox<UiAccentPreset> accentCombo = new ComboBox<>();
        accentCombo.getItems().addAll(UiAccentPreset.values());
        accentCombo.setValue(preferences.getAccentPreset());
        accentCombo.setConverter(createUiAccentPresetConverter());
        accentCombo.setButtonCell(createUiAccentPresetListCell());
        accentCombo.setCellFactory(list -> createUiAccentPresetListCell());
        accentCombo.setMaxWidth(Double.MAX_VALUE);
        accentCombo.getStyleClass().add("settings-combo-box");

        ComboBox<UiDensityMode> densityCombo = new ComboBox<>();
        densityCombo.getItems().addAll(UiDensityMode.values());
        densityCombo.setValue(preferences.getDensityMode());
        densityCombo.setConverter(createUiDensityModeConverter());
        densityCombo.setButtonCell(createUiDensityModeListCell());
        densityCombo.setCellFactory(list -> createUiDensityModeListCell());
        densityCombo.setMaxWidth(Double.MAX_VALUE);
        densityCombo.getStyleClass().add("settings-combo-box");

        CheckBox reducedMotionCheck = new CheckBox();
        reducedMotionCheck.setSelected(preferences.isReducedMotion());
        reducedMotionCheck.getStyleClass().add("settings-check-box");

        CheckBox collapseSidebarCheck = new CheckBox();
        collapseSidebarCheck.setSelected(preferences.isSidebarCollapsedByDefault());
        collapseSidebarCheck.getStyleClass().add("settings-check-box");

        AtomicReference<File> receiptOutputDirectory = new AtomicReference<>(data.receiptOutputDirectory());
        AtomicReference<File> receiptLogoFile = new AtomicReference<>(
            data.receiptSettings() != null ? data.receiptSettings().logoFile() : null
        );
        TextField storePhoneField = createSettingsTextField(
            data.receiptSettings() != null ? data.receiptSettings().storePhone() : "",
            "Store phone"
        );
        TextArea storeAddressArea = createSettingsTextArea(
            data.receiptSettings() != null ? data.receiptSettings().storeAddress() : "",
            "Store address",
            2
        );
        TextArea receiptFooterArea = createSettingsTextArea(
            data.receiptSettings() != null ? data.receiptSettings().footerNote() : "",
            "Receipt footer",
            2
        );
        Label receiptPathPreviewLabel = new Label(context.receiptService().previewReceiptFilePath(receiptOutputDirectory.get()));
        receiptPathPreviewLabel.getStyleClass().add("settings-path-preview-label");
        receiptPathPreviewLabel.setWrapText(true);
        receiptPathPreviewLabel.setMaxWidth(Double.MAX_VALUE);
        HBox receiptFolderPicker = createReceiptFolderPicker(
            context,
            receiptOutputDirectory,
            directory -> receiptPathPreviewLabel.setText(context.receiptService().previewReceiptFilePath(directory))
        );
        HBox receiptLogoPicker = createReceiptLogoPicker(context, receiptLogoFile);
        SePaySettingsControls sePayControls = data.sePaySettings() != null
            ? createSePaySettingsControls(context, user, data.sePaySettings())
            : null;

        List<DashboardSectionKey> dashboardOrder = new ArrayList<>(data.dashboardOrder());
        Set<DashboardSectionKey> hiddenSections = new LinkedHashSet<>(data.hiddenSections());

        VBox dashboardRows = new VBox(10);
        dashboardRows.setFillWidth(true);
        Runnable[] renderDashboardRowsRef = new Runnable[1];
        renderDashboardRowsRef[0] = () -> {
            dashboardRows.getChildren().clear();
            for (int index = 0; index < dashboardOrder.size(); index++) {
                DashboardSectionKey sectionKey = dashboardOrder.get(index);
                boolean visible = !hiddenSections.contains(sectionKey);

                Label label = new Label(sectionKey == null ? "-" : sectionKey.getLabel());
                label.getStyleClass().add("settings-dashboard-row-title");

                CheckBox visibleCheck = new CheckBox("Visible");
                visibleCheck.setSelected(visible);
                visibleCheck.getStyleClass().add("settings-dashboard-visible-check");
                visibleCheck.setOnAction(e -> {
                    if (visibleCheck.isSelected()) {
                        hiddenSections.remove(sectionKey);
                    } else {
                        hiddenSections.add(sectionKey);
                    }
                });

                Button moveUpButton = createDashboardMoveButton(
                    "Move Up",
                    "M12 19V5 M5 12l7-7 7 7"
                );
                moveUpButton.setDisable(index == 0);
                final int currentIndex = index;
                moveUpButton.setOnAction(e -> {
                    Collections.swap(dashboardOrder, currentIndex, currentIndex - 1);
                    renderDashboardRowsRef[0].run();
                });

                Button moveDownButton = createDashboardMoveButton(
                    "Move Down",
                    "M12 5v14 M5 12l7 7 7-7"
                );
                moveDownButton.setDisable(index == dashboardOrder.size() - 1);
                moveDownButton.setOnAction(e -> {
                    Collections.swap(dashboardOrder, currentIndex, currentIndex + 1);
                    renderDashboardRowsRef[0].run();
                });

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                HBox row = new HBox(10, label, spacer, visibleCheck, moveUpButton, moveDownButton);
                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("settings-dashboard-row");
                dashboardRows.getChildren().add(row);
            }
        };
        renderDashboardRowsRef[0].run();

        Button resetDashboardButton = ButtonFactory.pageNav("Reset to Default");
        resetDashboardButton.getStyleClass().add("settings-secondary-button");
        resetDashboardButton.setOnAction(e -> {
            dashboardOrder.clear();
            dashboardOrder.addAll(context.userUiPreferencesService().getDefaultDashboardOrder());
            hiddenSections.clear();
            renderDashboardRowsRef[0].run();
        });

        HBox dashboardActions = new HBox(resetDashboardButton);
        dashboardActions.setAlignment(Pos.CENTER_LEFT);

        VBox dashboardContent = new VBox(14, dashboardRows, dashboardActions);
        dashboardContent.setFillWidth(true);

        VBox appearanceContent = new VBox(10,
            createSettingRow(
                "Accent preset",
                "Controls highlight color for navigation, actions and selected table rows.",
                accentCombo
            ),
            createSettingRow(
                "Density mode",
                "Adjusts spacing for navigation, tables and high-frequency screens.",
                densityCombo
            ),
            createSettingRow(
                "Reduced motion",
                "Disables non-essential transitions and hover motion across the workspace.",
                reducedMotionCheck
            )
        );
        appearanceContent.setFillWidth(true);

        VBox layoutContent = new VBox(10,
            createSettingRow(
                "Collapse sidebar by default",
                "Opens the main workspace with the sidebar hidden for this account.",
                collapseSidebarCheck
            )
        );
        layoutContent.setFillWidth(true);

        VBox receiptContent = new VBox(10,
            createSettingRow(
                "Receipt output folder",
                "Folder used to save generated receipt PDFs. Use a mounted shared folder for multiple computers.",
                receiptFolderPicker
            ),
            createSettingRow(
                "Save path preview",
                "Receipts are found by order id in the configured folder when the saved absolute path is not available.",
                receiptPathPreviewLabel
            ),
            createSettingRow(
                "Store phone",
                "Printed below the store name on POS receipts.",
                storePhoneField
            ),
            createSettingRow(
                "Store address",
                "Printed below the store name. Vietnamese text is supported.",
                storeAddressArea
            ),
            createSettingRow(
                "Receipt footer",
                "Printed at the bottom of each receipt.",
                receiptFooterArea
            ),
            createSettingRow(
                "Logo",
                "Optional local image file printed above the store name.",
                receiptLogoPicker
            )
        );
        receiptContent.setFillWidth(true);

        VBox appearanceSection = createSettingsSection(
            "Appearance",
            "Choose how the application looks and moves for your own session.",
            appearanceContent
        );
        VBox layoutSection = createSettingsSection(
            "Layout",
            "Set how the main workspace opens for your account.",
            layoutContent
        );
        VBox receiptSection = createSettingsSection(
            "Receipts",
            "Choose where exported POS receipt PDFs are stored.",
            receiptContent
        );
        VBox dashboardSection = createSettingsSection(
            "Dashboard",
            "Show, hide and reorder the Dashboard sections shown for your account.",
            dashboardContent
        );
        VBox dataBackupSection = context.authorizationService().canAccessDataBackup(user)
            ? createSettingsSection(
                "Data Backup",
                "Export or restore the full tenant database on this workspace.",
                createDataBackupContent(context, user)
            )
            : null;

        Button saveButton = new Button("Save");
        saveButton.getStyleClass().addAll("button", "primary-button", "settings-save-button");
        saveButton.setOnAction(event -> {
            try {
                context.userUiPreferencesService().updatePreferences(
                    user,
                    user,
                    accentCombo.getValue(),
                    densityCombo.getValue(),
                    reducedMotionCheck.isSelected(),
                    collapseSidebarCheck.isSelected(),
                    dashboardOrder,
                    hiddenSections
                );
                context.receiptService().saveReceiptOutputDirectory(receiptOutputDirectory.get());
                context.receiptService().saveReceiptSettings(new ReceiptService.ReceiptSettings(
                    storePhoneField.getText(),
                    storeAddressArea.getText(),
                    receiptFooterArea.getText(),
                    receiptLogoFile.get()
                ));
                if (sePayControls != null) {
                    context.sePaySettingsService().saveSettings(user, collectSePaySettings(sePayControls));
                }
                context.applyUiPreferences(user, true);
                context.toastService().showSuccess("Settings saved");
            } catch (RuntimeException ex) {
                context.showUserFacingError(ex);
            }
        });

        Label pageTitle = new Label("Settings");
        pageTitle.getStyleClass().add("settings-page-title");
        Label pageSubtitle = new Label("Personalize workspace appearance, sidebar behavior, receipts and dashboard composition.");
        pageSubtitle.getStyleClass().add("settings-page-subtitle");
        pageSubtitle.setWrapText(true);
        VBox heading = new VBox(4, pageTitle, pageSubtitle);
        heading.setMinWidth(0);
        HBox.setHgrow(heading, Priority.ALWAYS);

        HBox pageHeader = new HBox(16, heading, saveButton);
        pageHeader.getStyleClass().add("settings-page-header");
        pageHeader.setAlignment(Pos.CENTER_LEFT);

        VBox pageContent = new VBox(18);
        pageContent.getChildren().addAll(pageHeader, appearanceSection, layoutSection, receiptSection, dashboardSection);
        if (sePayControls != null) {
            pageContent.getChildren().add(sePayControls.section());
        }
        if (dataBackupSection != null) {
            pageContent.getChildren().add(dataBackupSection);
        }
        pageContent.getStyleClass().addAll("reports-page", "settings-page");
        pageContent.setPadding(new Insets(20));
        pageContent.setFillWidth(true);
        return pageContent;
    }

    private static SePaySettingsControls createSePaySettingsControls(
        SceneRuntimeContext context,
        User user,
        SePaySettingsService.SePaySettings settings
    ) {
        CheckBox enabledCheck = new CheckBox();
        enabledCheck.setSelected(settings != null && settings.enabled());
        enabledCheck.getStyleClass().add("settings-check-box");

        PasswordField apiTokenField = createSettingsPasswordField(settings != null ? settings.apiToken() : "", "SePay API Token");
        PasswordField webhookApiKeyField = createSettingsPasswordField(settings != null ? settings.webhookApiKey() : "", "SePay Webhook API Key");
        TextField bankShortNameField = createSettingsTextField(settings != null ? settings.bankShortName() : "", "Vietcombank");
        TextField accountNumberField = createSettingsTextField(settings != null ? settings.accountNumber() : "", "Receiving account number");

        Label statusLabel = new Label("Credentials are stored in this tenant database. Backup files include these values.");
        statusLabel.getStyleClass().add("settings-row-description");
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(Double.MAX_VALUE);

        Button testButton = ButtonFactory.pageNav("Test Connection");
        testButton.getStyleClass().add("settings-secondary-button");

        SePaySettingsControls controls = new SePaySettingsControls(
            enabledCheck,
            apiTokenField,
            webhookApiKeyField,
            bankShortNameField,
            accountNumberField,
            statusLabel,
            null
        );
        testButton.setOnAction(event -> runSePayConnectionTest(context, user, controls, testButton));

        VBox paymentContent = new VBox(10,
            createSettingRow(
                "Enable SePay",
                "Use automatic QR payment confirmation for POS checkout.",
                enabledCheck
            ),
            createSettingRow(
                "API token",
                "Token from SePay Company Settings > API Access, used to poll transaction status.",
                apiTokenField
            ),
            createSettingRow(
                "Webhook API key",
                "Optional. Only needed if this app has a public webhook URL for SePay IPN.",
                webhookApiKeyField
            ),
            createSettingRow(
                "Bank",
                "SePay/VietQR bank short name, for example Vietcombank.",
                bankShortNameField
            ),
            createSettingRow(
                "Account number",
                "Receiving bank account number linked to SePay.",
                accountNumberField
            ),
            createSettingRow(
                "Connection",
                "Checks the SePay token and configured bank account.",
                new VBox(8, new HBox(8, testButton), statusLabel)
            )
        );
        paymentContent.setFillWidth(true);

        VBox section = createSettingsSection(
            "QR Payments",
            "Configure automatic QR confirmation through SePay once for this business.",
            paymentContent
        );
        return new SePaySettingsControls(enabledCheck, apiTokenField, webhookApiKeyField, bankShortNameField, accountNumberField, statusLabel, section);
    }

    private static SePaySettingsService.SePaySettings collectSePaySettings(SePaySettingsControls controls) {
        return new SePaySettingsService.SePaySettings(
            controls.enabledCheck().isSelected(),
            controls.apiTokenField().getText(),
            controls.webhookApiKeyField().getText(),
            controls.bankShortNameField().getText(),
            controls.accountNumberField().getText()
        );
    }

    private static void runSePayConnectionTest(
        SceneRuntimeContext context,
        User user,
        SePaySettingsControls controls,
        Button testButton
    ) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                context.sePaySettingsService().testConnection(user, collectSePaySettings(controls));
                return null;
            }
        };
        String originalText = testButton.getText();
        testButton.setDisable(true);
        testButton.setText("Testing...");
        controls.statusLabel().setText("Testing SePay connection...");
        task.setOnSucceeded(event -> {
            testButton.setDisable(false);
            testButton.setText(originalText);
            controls.statusLabel().setText("SePay settings look valid. Save settings to use them for checkout.");
            context.toastService().showSuccess("SePay connection verified");
        });
        task.setOnFailed(event -> {
            testButton.setDisable(false);
            testButton.setText(originalText);
            controls.statusLabel().setText("SePay connection test failed.");
            context.showUserFacingError(task.getException());
        });
        startSettingsTask(task, "sepay-settings-test");
    }

    private static VBox createDataBackupContent(SceneRuntimeContext context, User user) {
        Label statusLabel = new Label("Backup files are not encrypted. Keep exported files in a secure location.");
        statusLabel.getStyleClass().add("settings-row-description");
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(Double.MAX_VALUE);

        Button exportButton = ButtonFactory.pageNav("Export Backup");
        exportButton.getStyleClass().add("settings-secondary-button");
        Button restoreButton = ButtonFactory.pageNav("Restore Backup");
        restoreButton.getStyleClass().add("settings-secondary-button");

        HBox actions = new HBox(8, exportButton, restoreButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox controls = new VBox(8, actions, statusLabel);
        controls.setFillWidth(true);
        controls.setMinWidth(0);

        exportButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Export backup");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PBL3 Backup (*.pbl3backup)", "*.pbl3backup"));
            File initialDirectory = resolveDirectoryChooserInitialDirectory(
                context.dataBackupService().getDefaultBackupDirectory(),
                context.dataBackupService().getDefaultBackupDirectory()
            );
            if (initialDirectory != null) {
                chooser.setInitialDirectory(initialDirectory);
            }
            chooser.setInitialFileName(context.dataBackupService().buildDefaultBackupFileName());
            File selectedFile = chooser.showSaveDialog(context.owner());
            if (selectedFile != null) {
                runBackupExport(context, user, selectedFile, exportButton, restoreButton, statusLabel);
            }
        });

        restoreButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Restore backup");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PBL3 Backup (*.pbl3backup)", "*.pbl3backup"));
            File initialDirectory = resolveDirectoryChooserInitialDirectory(
                context.dataBackupService().getDefaultBackupDirectory(),
                context.dataBackupService().getDefaultBackupDirectory()
            );
            if (initialDirectory != null) {
                chooser.setInitialDirectory(initialDirectory);
            }
            File selectedFile = chooser.showOpenDialog(context.owner());
            if (selectedFile == null) {
                return;
            }

            DataBackupService.BackupPreview preview;
            try {
                preview = context.dataBackupService().previewBackup(selectedFile);
            } catch (RuntimeException ex) {
                context.showUserFacingError(ex);
                return;
            }
            long rowCount = preview.manifest().tables().stream()
                .mapToLong(DataBackupService.BackupTableSummary::rowCount)
                .sum();
            String message = MessageFormat.format(
                "Restore backup \"{0}\" with {1} row(s). Current tenant data will be replaced. A safety backup will be created before restore.",
                selectedFile.getName(),
                rowCount
            );
            if (!DialogSupport.showTypedDangerConfirm(context.owner(), "Restore Backup", message, "RESTORE")) {
                return;
            }
            runBackupRestore(context, user, selectedFile, exportButton, restoreButton, statusLabel);
        });

        VBox content = new VBox(10,
            createSettingRow(
                "Tenant backup",
                "Exports users, products, orders, stock, expenses, promotions, settings and audit logs.",
                controls
            )
        );
        content.setFillWidth(true);
        return content;
    }

    private static void runBackupExport(
        SceneRuntimeContext context,
        User user,
        File selectedFile,
        Button exportButton,
        Button restoreButton,
        Label statusLabel
    ) {
        Task<DataBackupService.BackupExportResult> task = new Task<>() {
            @Override
            protected DataBackupService.BackupExportResult call() {
                updateMessage("Exporting backup...");
                return context.dataBackupService().exportBackup(user, selectedFile, this::updateMessage);
            }
        };
        setBackupControlsBusy(exportButton, restoreButton, statusLabel, task, true);
        task.setOnSucceeded(event -> {
            setBackupControlsBusy(exportButton, restoreButton, statusLabel, task, false);
            DataBackupService.BackupExportResult result = task.getValue();
            statusLabel.setText(MessageFormat.format(
                "Exported {0} row(s) to {1}",
                result.totalRows(),
                result.file().getAbsolutePath()
            ));
            context.toastService().showSuccess("Backup exported");
        });
        task.setOnFailed(event -> {
            setBackupControlsBusy(exportButton, restoreButton, statusLabel, task, false);
            statusLabel.setText("Backup export failed.");
            context.showUserFacingError(task.getException());
        });
        startSettingsTask(task, "data-backup-export");
    }

    private static void runBackupRestore(
        SceneRuntimeContext context,
        User user,
        File selectedFile,
        Button exportButton,
        Button restoreButton,
        Label statusLabel
    ) {
        Task<DataBackupService.BackupRestoreResult> task = new Task<>() {
            @Override
            protected DataBackupService.BackupRestoreResult call() {
                updateMessage("Restoring backup...");
                return context.dataBackupService().restoreBackup(user, selectedFile, this::updateMessage);
            }
        };
        setBackupControlsBusy(exportButton, restoreButton, statusLabel, task, true);
        task.setOnSucceeded(event -> {
            setBackupControlsBusy(exportButton, restoreButton, statusLabel, task, false);
            DataBackupService.BackupRestoreResult result = task.getValue();
            statusLabel.setText(MessageFormat.format(
                "Restored {0} row(s). Safety backup: {1}. Restart the app or sign in again before continuing.",
                result.totalRows(),
                result.safetyBackupFile().getAbsolutePath()
            ));
            context.toastService().showSuccess("Backup restored. Restart the app or sign in again before continuing.");
            if (context.navigator() != null) {
                context.navigator().refreshCurrentScene();
            }
        });
        task.setOnFailed(event -> {
            setBackupControlsBusy(exportButton, restoreButton, statusLabel, task, false);
            statusLabel.setText("Backup restore failed. Current data was not replaced unless restore had already completed.");
            context.showUserFacingError(task.getException());
        });
        startSettingsTask(task, "data-backup-restore");
    }

    private static void setBackupControlsBusy(
        Button exportButton,
        Button restoreButton,
        Label statusLabel,
        Task<?> task,
        boolean busy
    ) {
        exportButton.setDisable(busy);
        restoreButton.setDisable(busy);
        if (busy) {
            statusLabel.textProperty().bind(task.messageProperty());
        } else {
            statusLabel.textProperty().unbind();
        }
    }

    private static void startSettingsTask(Task<?> task, String threadName) {
        Thread worker = new Thread(task, threadName);
        worker.setDaemon(true);
        worker.start();
    }

    private static HBox createReceiptFolderPicker(
        SceneRuntimeContext context,
        AtomicReference<File> receiptOutputDirectory,
        Consumer<File> onDirectoryChanged
    ) {
        TextField pathField = new TextField(formatReceiptFolderPath(receiptOutputDirectory.get()));
        pathField.setEditable(false);
        pathField.setFocusTraversable(false);
        pathField.getStyleClass().add("settings-path-field");
        pathField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(pathField, Priority.ALWAYS);

        Button browseButton = ButtonFactory.pageNav("Browse");
        browseButton.getStyleClass().add("settings-secondary-button");
        browseButton.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Choose receipt output folder");
            File initialDirectory = resolveDirectoryChooserInitialDirectory(
                receiptOutputDirectory.get(),
                context.receiptService().getDefaultReceiptOutputDirectory()
            );
            if (initialDirectory != null) {
                chooser.setInitialDirectory(initialDirectory);
            }
            File selectedDirectory = chooser.showDialog(context.owner());
            if (selectedDirectory != null) {
                File normalizedDirectory = selectedDirectory.toPath().toAbsolutePath().normalize().toFile();
                receiptOutputDirectory.set(normalizedDirectory);
                pathField.setText(formatReceiptFolderPath(normalizedDirectory));
                if (onDirectoryChanged != null) {
                    onDirectoryChanged.accept(normalizedDirectory);
                }
            }
        });

        Button resetButton = ButtonFactory.pageNav("Reset");
        resetButton.getStyleClass().add("settings-secondary-button");
        resetButton.setOnAction(event -> {
            File defaultDirectory = context.receiptService().getDefaultReceiptOutputDirectory();
            receiptOutputDirectory.set(defaultDirectory);
            pathField.setText(formatReceiptFolderPath(defaultDirectory));
            if (onDirectoryChanged != null) {
                onDirectoryChanged.accept(defaultDirectory);
            }
        });

        HBox picker = new HBox(8, pathField, browseButton, resetButton);
        picker.getStyleClass().add("settings-folder-picker");
        picker.setAlignment(Pos.CENTER_LEFT);
        picker.setMinWidth(0);
        picker.setPrefWidth(460);
        picker.setMaxWidth(Double.MAX_VALUE);
        return picker;
    }

    private static HBox createReceiptLogoPicker(
        SceneRuntimeContext context,
        AtomicReference<File> receiptLogoFile
    ) {
        TextField pathField = new TextField(formatReceiptFolderPath(receiptLogoFile.get()));
        pathField.setEditable(false);
        pathField.setFocusTraversable(false);
        pathField.getStyleClass().add("settings-path-field");
        pathField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(pathField, Priority.ALWAYS);

        Button browseButton = ButtonFactory.pageNav("Browse");
        browseButton.getStyleClass().add("settings-secondary-button");
        browseButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose receipt logo");
            chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("All files", "*.*")
            );
            File initialDirectory = resolveFileChooserInitialDirectory(receiptLogoFile.get());
            if (initialDirectory != null) {
                chooser.setInitialDirectory(initialDirectory);
            }
            File selectedFile = chooser.showOpenDialog(context.owner());
            if (selectedFile != null) {
                File normalizedFile = selectedFile.toPath().toAbsolutePath().normalize().toFile();
                receiptLogoFile.set(normalizedFile);
                pathField.setText(formatReceiptFolderPath(normalizedFile));
            }
        });

        Button clearButton = ButtonFactory.pageNav("Clear");
        clearButton.getStyleClass().add("settings-secondary-button");
        clearButton.setOnAction(event -> {
            receiptLogoFile.set(null);
            pathField.clear();
        });

        HBox picker = new HBox(8, pathField, browseButton, clearButton);
        picker.getStyleClass().add("settings-folder-picker");
        picker.setAlignment(Pos.CENTER_LEFT);
        picker.setMinWidth(0);
        picker.setPrefWidth(460);
        picker.setMaxWidth(Double.MAX_VALUE);
        return picker;
    }

    private static File resolveDirectoryChooserInitialDirectory(File selectedDirectory, File defaultDirectory) {
        if (selectedDirectory != null && selectedDirectory.isDirectory()) {
            return selectedDirectory;
        }
        if (defaultDirectory != null && defaultDirectory.isDirectory()) {
            return defaultDirectory;
        }
        File defaultParent = defaultDirectory != null ? defaultDirectory.getParentFile() : null;
        if (defaultParent != null && defaultParent.isDirectory()) {
            return defaultParent;
        }
        File homeDirectory = new File(System.getProperty("user.home"));
        return homeDirectory.isDirectory() ? homeDirectory : null;
    }

    private static File resolveFileChooserInitialDirectory(File selectedFile) {
        if (selectedFile != null) {
            File parent = selectedFile.isDirectory() ? selectedFile : selectedFile.getParentFile();
            if (parent != null && parent.isDirectory()) {
                return parent;
            }
        }
        File homeDirectory = new File(System.getProperty("user.home"));
        return homeDirectory.isDirectory() ? homeDirectory : null;
    }

    private static String formatReceiptFolderPath(File directory) {
        return directory == null ? "" : directory.toPath().toAbsolutePath().normalize().toString();
    }

    private static TextField createSettingsTextField(String value, String prompt) {
        TextField field = new TextField(value == null ? "" : value);
        field.setPromptText(prompt);
        field.getStyleClass().add("settings-text-field");
        field.setMaxWidth(Double.MAX_VALUE);
        field.setPrefWidth(460);
        return field;
    }

    private static PasswordField createSettingsPasswordField(String value, String prompt) {
        PasswordField field = new PasswordField();
        field.setText(value == null ? "" : value);
        field.setPromptText(prompt);
        field.getStyleClass().add("settings-text-field");
        field.setMaxWidth(Double.MAX_VALUE);
        field.setPrefWidth(460);
        return field;
    }

    private static TextArea createSettingsTextArea(String value, String prompt, int rowCount) {
        TextArea area = new TextArea(value == null ? "" : value);
        area.setPromptText(prompt);
        area.setWrapText(true);
        area.setPrefRowCount(rowCount);
        area.getStyleClass().add("settings-text-area");
        area.setMaxWidth(Double.MAX_VALUE);
        area.setPrefWidth(460);
        return area;
    }

    private static VBox createSettingsStateContent(String title, String message, Button actionButton, boolean loading) {
        VBox pageContent = new VBox(18);
        pageContent.getStyleClass().addAll("reports-page", "settings-page");
        pageContent.setPadding(new Insets(20));
        pageContent.setFillWidth(true);

        VBox card = new VBox(12);
        card.getStyleClass().add("settings-section-card");
        card.setAlignment(Pos.CENTER);
        card.setMinHeight(260);
        card.setMaxWidth(Double.MAX_VALUE);

        if (loading) {
            ProgressIndicator spinner = new ProgressIndicator();
            spinner.setPrefSize(44, 44);
            card.getChildren().add(spinner);
        }

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("settings-section-title");
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("settings-section-subtitle");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(520);

        card.getChildren().addAll(titleLabel, messageLabel);
        if (actionButton != null) {
            card.getChildren().add(actionButton);
        }

        pageContent.getChildren().add(card);
        return pageContent;
    }

    private static VBox createSettingsSection(String title, String subtitle, Node content) {
        VBox section = new VBox(16);
        section.getStyleClass().add("settings-section-card");
        section.setMinWidth(0);
        section.setFillWidth(true);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("settings-section-title");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(Double.MAX_VALUE);

        VBox titleBox = new VBox(4);
        titleBox.getChildren().add(titleLabel);
        if (subtitle != null && !subtitle.isBlank()) {
            Label subtitleLabel = new Label(subtitle);
            subtitleLabel.getStyleClass().add("settings-section-subtitle");
            subtitleLabel.setWrapText(true);
            subtitleLabel.setMaxWidth(Double.MAX_VALUE);
            subtitleLabel.prefWidthProperty().bind(titleBox.widthProperty());
            subtitleLabel.maxWidthProperty().bind(titleBox.widthProperty());
            titleBox.getChildren().add(subtitleLabel);
        }
        titleBox.setMinWidth(0);
        titleBox.setMaxWidth(Double.MAX_VALUE);
        titleBox.setFillWidth(true);

        titleLabel.prefWidthProperty().bind(titleBox.widthProperty());
        titleLabel.maxWidthProperty().bind(titleBox.widthProperty());
        VBox.setVgrow(content, Priority.ALWAYS);
        section.getChildren().addAll(titleBox, content);
        return section;
    }

    private static HBox createSettingRow(String title, String description, Node control) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("settings-row-title");
        titleLabel.setWrapText(true);

        Label descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("settings-row-description");
        descriptionLabel.setWrapText(true);

        VBox copy = new VBox(3, titleLabel, descriptionLabel);
        copy.setMinWidth(0);
        copy.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(copy, Priority.ALWAYS);

        if (control instanceof Label labelControl) {
            labelControl.setMinWidth(0);
            labelControl.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(labelControl, Priority.ALWAYS);
        } else if (control instanceof ComboBox<?> comboBox) {
            comboBox.setMinWidth(Region.USE_PREF_SIZE);
            comboBox.setPrefWidth(260);
        } else if (control instanceof HBox || control instanceof VBox) {
            Region region = (Region) control;
            region.setMinWidth(0);
            HBox.setHgrow(region, Priority.ALWAYS);
        } else if (control instanceof Region region) {
            region.setMinWidth(Region.USE_PREF_SIZE);
        }

        HBox row = new HBox(18, copy, control);
        row.getStyleClass().add("settings-control-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMinWidth(0);
        return row;
    }

    private static Button createDashboardMoveButton(String tooltipText, String pathContent) {
        SVGPath path = new SVGPath();
        path.setContent(pathContent);
        path.getStyleClass().add("settings-icon-stroke");
        path.setFill(javafx.scene.paint.Color.TRANSPARENT);
        path.setStrokeWidth(1.8);
        path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);

        Button button = new Button();
        button.getStyleClass().add("settings-icon-button");
        button.setGraphic(path);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setTooltip(new Tooltip(tooltipText));
        button.setFocusTraversable(false);
        button.setMinSize(34, 34);
        button.setPrefSize(34, 34);
        button.setMaxSize(34, 34);
        return button;
    }

    private static StringConverter<UiAccentPreset> createUiAccentPresetConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(UiAccentPreset preset) {
                return preset != null ? preset.getLabel() : "";
            }

            @Override
            public UiAccentPreset fromString(String string) {
                return java.util.Arrays.stream(UiAccentPreset.values())
                    .filter(preset -> preset.getLabel().equalsIgnoreCase(string))
                    .findFirst()
                    .orElse(UiAccentPreset.BLUE);
            }
        };
    }

    private static ListCell<UiAccentPreset> createUiAccentPresetListCell() {
        return new ListCell<>() {
            {
                getStyleClass().add("settings-combo-popup-cell");
            }

            @Override
            protected void updateItem(UiAccentPreset item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label label = new Label(item.getLabel());
                label.getStyleClass().add("settings-combo-cell-label");
                Region swatch = new Region();
                swatch.getStyleClass().add("settings-accent-swatch");
                swatch.setStyle("-fx-background-color: " + accentColor(item) + ";");
                HBox cell = new HBox(8, swatch, label);
                cell.setAlignment(Pos.CENTER_LEFT);
                setText(null);
                setGraphic(cell);
            }
        };
    }

    private static String accentColor(UiAccentPreset preset) {
        return switch (preset) {
            case EMERALD -> "#10b981";
            case AMBER -> "#f59e0b";
            case BLUE -> "#1d7df2";
        };
    }

    private static StringConverter<UiDensityMode> createUiDensityModeConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(UiDensityMode densityMode) {
                return densityMode != null ? densityMode.getLabel() : "";
            }

            @Override
            public UiDensityMode fromString(String string) {
                return java.util.Arrays.stream(UiDensityMode.values())
                    .filter(mode -> mode.getLabel().equalsIgnoreCase(string))
                    .findFirst()
                    .orElse(UiDensityMode.COMFORTABLE);
            }
        };
    }

    private static ListCell<UiDensityMode> createUiDensityModeListCell() {
        return new ListCell<>() {
            {
                getStyleClass().add("settings-combo-popup-cell");
            }

            @Override
            protected void updateItem(UiDensityMode item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label label = new Label(item.getLabel());
                label.getStyleClass().add("settings-combo-cell-label");
                setText(null);
                setGraphic(label);
            }
        };
    }
}
