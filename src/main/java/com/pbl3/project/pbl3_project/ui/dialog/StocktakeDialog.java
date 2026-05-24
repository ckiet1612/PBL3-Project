package com.pbl3.project.pbl3_project.ui.dialog;

import com.pbl3.project.pbl3_project.entity.Category;
import com.pbl3.project.pbl3_project.entity.StocktakeItem;
import com.pbl3.project.pbl3_project.entity.StocktakeScopeType;
import com.pbl3.project.pbl3_project.entity.StocktakeSession;
import com.pbl3.project.pbl3_project.entity.StocktakeSessionStatus;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.service.CategoryService;
import com.pbl3.project.pbl3_project.service.MoneySupport;
import com.pbl3.project.pbl3_project.service.StocktakeService;
import com.pbl3.project.pbl3_project.service.ToastService;
import com.pbl3.project.pbl3_project.service.ValidationException;
import com.pbl3.project.pbl3_project.ui.component.DialogFormFactory;
import com.pbl3.project.pbl3_project.ui.component.StatusBadgeFactory;
import com.pbl3.project.pbl3_project.ui.util.AsyncUiTask;
import com.pbl3.project.pbl3_project.ui.util.DialogSupport;
import com.pbl3.project.pbl3_project.ui.util.FxFormatters;
import com.pbl3.project.pbl3_project.ui.util.TableViewSupport;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;
import javafx.util.converter.IntegerStringConverter;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class StocktakeDialog {

    private static final DateTimeFormatter DATE_TIME_SECONDS_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final double MANAGE_DIALOG_PREF_WIDTH = 1000;
    private static final double MANAGE_DIALOG_PREF_HEIGHT = 720;
    private static final double MANAGE_DIALOG_MIN_WIDTH = 900;
    private static final double MANAGE_DIALOG_MIN_HEIGHT = 620;
    private static final double MANAGE_DIALOG_SCREEN_MARGIN = 88;

    private StocktakeDialog() {
    }

    public record Context(
        StocktakeService stocktakeService,
        CategoryService categoryService,
        ToastService toastService,
        Consumer<Throwable> errorHandler
    ) {
    }

    private record CreateDialogData(List<Category> categories) {
    }

    public static void showCreate(
        Stage owner,
        User user,
        Consumer<StocktakeSession> onSuccess,
        Context context
    ) {
        Stage dialog = DialogSupport.showLoadingWindow(owner, "New Stocktake Session", "Loading stocktake form...", 420, 240);

        javafx.concurrent.Task<CreateDialogData> task = new javafx.concurrent.Task<>() {
            @Override
            protected CreateDialogData call() {
                return new CreateDialogData(context.categoryService().getAllCategories());
            }
        };
        task.setOnSucceeded(event -> {
            if (!dialog.isShowing()) {
                return;
            }
            populateCreateDialog(dialog, user, onSuccess, context, task.getValue());
        });
        task.setOnFailed(event -> {
            dialog.close();
            handleError(context, task.getException());
        });
        Thread worker = new Thread(task, "stocktake-create-dialog-options-loader");
        worker.setDaemon(true);
        worker.start();
    }

    private static void populateCreateDialog(
        Stage dialog,
        User user,
        Consumer<StocktakeSession> onSuccess,
        Context context,
        CreateDialogData data
    ) {
        dialog.setTitle("New Stocktake Session");

        VBox root = new VBox(16);
        root.getStyleClass().addAll("dialog-root", "product-dialog-root", "stocktake-dialog-root");
        root.setPadding(new Insets(24));

        VBox header = DialogFormFactory.header("New Stocktake Session", null);

        ComboBox<StocktakeScopeType> scopeCombo = new ComboBox<>();
        scopeCombo.getItems().addAll(StocktakeScopeType.values());
        scopeCombo.setValue(StocktakeScopeType.ALL_PRODUCTS);
        scopeCombo.setMaxWidth(Double.MAX_VALUE);
        scopeCombo.getStyleClass().addAll("product-dialog-combo-box", "promotion-dialog-combo-box");
        scopeCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(StocktakeScopeType value) {
                return formatStocktakeScopeLabel(value);
            }

            @Override
            public StocktakeScopeType fromString(String string) {
                return null;
            }
        });
        DialogFormFactory.installPopupCells(scopeCombo);

        ComboBox<Category> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll(data.categories());
        categoryCombo.setDisable(true);
        categoryCombo.setMaxWidth(Double.MAX_VALUE);
        categoryCombo.getStyleClass().addAll("product-dialog-combo-box", "promotion-dialog-combo-box");
        categoryCombo.setConverter(categoryConverter());
        DialogFormFactory.installPopupCells(categoryCombo);

        TextArea notesArea = DialogFormFactory.textArea("", "Optional notes", 3);
        notesArea.getStyleClass().add("product-dialog-text-area");

        Label categoryError = DialogFormFactory.errorLabel();

        GridPane scopeGrid = DialogFormFactory.grid();
        scopeGrid.add(DialogFormFactory.fieldBlock("Scope *", scopeCombo, null), 0, 0);
        scopeGrid.add(DialogFormFactory.fieldBlock("Category", categoryCombo, categoryError), 1, 0);

        VBox notesContent = new VBox(0, DialogFormFactory.fieldBlock("Notes", notesArea, null));
        notesContent.setFillWidth(true);

        VBox form = new VBox(
            14,
            DialogFormFactory.section("Counting Scope", scopeGrid),
            DialogFormFactory.section("Session Notes", notesContent)
        );
        form.setFillWidth(true);

        scopeCombo.setOnAction(event -> {
            boolean categoryScope = scopeCombo.getValue() == StocktakeScopeType.CATEGORY;
            categoryCombo.setDisable(!categoryScope);
            DialogFormFactory.setError(categoryError, null);
            if (!categoryScope) {
                categoryCombo.setValue(null);
            }
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("button", "product-dialog-secondary-button", "dialog-cancel-button");
        cancelBtn.setOnAction(event -> dialog.close());

        Button createBtn = new Button("Create Session");
        createBtn.getStyleClass().addAll("button", "primary-button", "product-dialog-primary-button");
        createBtn.setDefaultButton(true);
        createBtn.setOnAction(event -> {
            try {
                DialogFormFactory.setError(categoryError, null);
                if (scopeCombo.getValue() == StocktakeScopeType.CATEGORY && categoryCombo.getValue() == null) {
                    DialogFormFactory.setError(categoryError, "Category is required for category scope");
                    return;
                }
                StocktakeScopeType scope = scopeCombo.getValue();
                Category category = categoryCombo.getValue();
                String notes = notesArea.getText();
                AsyncUiTask.runButtonTask(
                    createBtn,
                    cancelBtn,
                    "Creating...",
                    () -> context.stocktakeService().createSession(user, scope, category, notes),
                    created -> {
                        context.toastService().showSuccess("Stocktake created.");
                        dialog.close();
                        if (onSuccess != null) {
                            onSuccess.accept(created);
                        }
                    },
                    ex -> handleError(context, ex),
                    "stocktake-create"
                );
            } catch (Exception ex) {
                handleError(context, ex);
            }
        });

        HBox actionRow = new HBox(10, cancelBtn, createBtn);
        actionRow.getStyleClass().add("product-dialog-footer");
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(header, form, actionRow);

        Scene scene = new Scene(root, 560, 510);
        applyApplicationStyles(scene);
        dialog.setScene(scene);
        dialog.setMinWidth(540);
        dialog.setMinHeight(510);
        DialogSupport.preventInitialFieldFocus(dialog, root);
        DialogSupport.centerWindowOnOwner(dialog);
        if (!dialog.isShowing()) {
            dialog.show();
        }
    }

    public static void showManage(
        Stage owner,
        User user,
        Long sessionId,
        Runnable onChanged,
        Context context
    ) {
        Stage dialog = DialogSupport.showLoadingWindow(
            owner,
            MessageFormat.format("Stocktake #{0}", sessionId),
            "Loading stocktake session...",
            420,
            240
        );

        javafx.concurrent.Task<StocktakeSession> task = new javafx.concurrent.Task<>() {
            @Override
            protected StocktakeSession call() {
                return context.stocktakeService().getSessionWithItems(user, sessionId);
            }
        };
        task.setOnSucceeded(event -> {
            if (!dialog.isShowing()) {
                return;
            }
            populateManageDialog(dialog, user, sessionId, onChanged, context, task.getValue());
        });
        task.setOnFailed(event -> {
            dialog.close();
            handleError(context, task.getException());
        });
        Thread worker = new Thread(task, "stocktake-manage-dialog-loader");
        worker.setDaemon(true);
        worker.start();
    }

    private static void populateManageDialog(
        Stage dialog,
        User user,
        Long sessionId,
        Runnable onChanged,
        Context context,
        StocktakeSession session
    ) {
        try {
            dialog.setTitle(MessageFormat.format("Stocktake #{0}", session.getId()));

            VBox root = new VBox(16);
            root.getStyleClass().addAll("dialog-root", "product-dialog-root", "stocktake-dialog-root", "stocktake-detail-dialog-root");
            root.setPadding(new Insets(24));

            VBox header = DialogFormFactory.header(
                MessageFormat.format("Stocktake #{0}", session.getId()),
                "Manage counted quantities and session notes"
            );

            boolean editable = session.getStatus() == StocktakeSessionStatus.OPEN;
            TextArea notesArea = DialogFormFactory.textArea(session.getNotes() == null ? "" : session.getNotes(), "Session notes", 2);
            notesArea.getStyleClass().add("product-dialog-text-area");
            notesArea.setDisable(!editable);

            Label statusBadge = StatusBadgeFactory.stocktake(session.getStatus());
            HBox statusBadgeWrap = new HBox(statusBadge);
            statusBadgeWrap.setAlignment(Pos.CENTER_LEFT);

            GridPane overviewGrid = DialogFormFactory.grid();
            overviewGrid.add(DialogFormFactory.fieldBlock(
                "Scope",
                createStocktakeDetailValue(formatStocktakeScopeLabel(session.getScopeType()), true),
                null
            ), 0, 0);
            overviewGrid.add(DialogFormFactory.fieldBlock("Status", statusBadgeWrap, null), 1, 0);
            overviewGrid.add(DialogFormFactory.fieldBlock(
                "Category",
                createStocktakeDetailValue(session.getCategory() != null ? session.getCategory().getName() : "All products", false),
                null
            ), 0, 1);
            overviewGrid.add(DialogFormFactory.fieldBlock(
                "Created By",
                createStocktakeDetailValue(formatUserDisplayName(session.getCreatedBy()), false),
                null
            ), 1, 1);
            overviewGrid.add(DialogFormFactory.fieldBlock(
                "Created At",
                createStocktakeDetailValue(formatDateTimeWithSeconds(session.getCreatedAt()), false),
                null
            ), 0, 2);
            overviewGrid.add(DialogFormFactory.fieldBlock(
                "Applied At",
                createStocktakeDetailValue(formatDateTimeWithSeconds(session.getAppliedAt()), false),
                null
            ), 1, 2);
            VBox overviewCard = DialogFormFactory.section("Overview", overviewGrid);

            VBox notesCard = DialogFormFactory.section(
                "Session Notes",
                DialogFormFactory.fieldBlock("Notes", notesArea, null)
            );

            ObservableList<StocktakeDraftRow> rows = FXCollections.observableArrayList(
                session.getItems().stream().map(StocktakeDraftRow::new).toList()
            );
            TableView<StocktakeDraftRow> table = createStocktakeItemsTable(rows, editable, context);

            if (editable) {
                notesArea.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event ->
                    table.getSelectionModel().clearSelection()
                );
                notesArea.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                    if (isFocused) {
                        table.getSelectionModel().clearSelection();
                    }
                });
            }

            Supplier<List<StocktakeService.StocktakeItemUpdate>> collectUpdates = () ->
                rows.stream()
                    .map(row -> new StocktakeService.StocktakeItemUpdate(row.getItemId(), row.getCountedQuantity(), row.getNotes()))
                    .toList();

            Button closeBtn = new Button("Close");
            closeBtn.getStyleClass().addAll("button", "product-dialog-secondary-button", "dialog-cancel-button");
            closeBtn.setOnAction(event -> dialog.close());

            Button saveBtn = new Button("Save Draft");
            saveBtn.getStyleClass().addAll("button", "primary-button", "product-dialog-primary-button");
            saveBtn.setDisable(!editable);
            saveBtn.setOnAction(event -> {
                try {
                    String notes = notesArea.getText();
                    List<StocktakeService.StocktakeItemUpdate> updates = collectUpdates.get();
                    AsyncUiTask.runButtonTask(
                        saveBtn,
                        closeBtn,
                        "Saving...",
                        () -> {
                            context.stocktakeService().updateSessionItems(user, sessionId, notes, updates);
                            return null;
                        },
                        ignored -> {
                            context.toastService().showSuccess("Stocktake draft saved.");
                            if (onChanged != null) {
                                onChanged.run();
                            }
                        },
                        ex -> handleError(context, ex),
                        "stocktake-save"
                    );
                } catch (Exception ex) {
                    handleError(context, ex);
                }
            });

            Button applyBtn = new Button("Apply Session");
            applyBtn.getStyleClass().addAll("button", "success-button", "product-dialog-primary-button");
            applyBtn.setDisable(!editable);
            applyBtn.setOnAction(event -> {
                try {
                    String notes = notesArea.getText();
                    List<StocktakeService.StocktakeItemUpdate> updates = collectUpdates.get();
                    AsyncUiTask.runButtonTask(
                        applyBtn,
                        closeBtn,
                        "Applying...",
                        () -> {
                            context.stocktakeService().updateSessionItems(user, sessionId, notes, updates);
                            context.stocktakeService().applySession(user, sessionId);
                            return null;
                        },
                        ignored -> {
                            context.toastService().showSuccess("Stocktake applied.");
                            dialog.close();
                            if (onChanged != null) {
                                onChanged.run();
                            }
                        },
                        ex -> handleError(context, ex),
                        "stocktake-apply"
                    );
                } catch (Exception ex) {
                    handleError(context, ex);
                }
            });

            Button cancelBtn = new Button("Cancel Session");
            cancelBtn.getStyleClass().addAll("button", "danger-button", "product-dialog-primary-button");
            cancelBtn.setDisable(!editable);
            cancelBtn.setOnAction(event -> {
                try {
                    String notes = notesArea.getText();
                    AsyncUiTask.runButtonTask(
                        cancelBtn,
                        closeBtn,
                        "Canceling...",
                        () -> {
                            context.stocktakeService().cancelSession(user, sessionId, notes);
                            return null;
                        },
                        ignored -> {
                            context.toastService().showSuccess("Stocktake canceled.");
                            dialog.close();
                            if (onChanged != null) {
                                onChanged.run();
                            }
                        },
                        ex -> handleError(context, ex),
                        "stocktake-cancel"
                    );
                } catch (Exception ex) {
                    handleError(context, ex);
                }
            });

            HBox actionRow = new HBox(10, closeBtn, saveBtn, applyBtn, cancelBtn);
            actionRow.getStyleClass().add("product-dialog-footer");
            actionRow.setAlignment(Pos.CENTER_RIGHT);

            table.setMinHeight(180);
            table.setPrefHeight(260);
            VBox itemsCard = DialogFormFactory.section("Stocktake Items", table);
            VBox.setVgrow(table, Priority.ALWAYS);
            VBox.setVgrow(itemsCard, Priority.ALWAYS);

            VBox content = new VBox(14, overviewCard, notesCard, itemsCard);
            content.setFillWidth(true);
            VBox.setVgrow(itemsCard, Priority.ALWAYS);

            ScrollPane contentScroll = new ScrollPane(content);
            contentScroll.setFitToWidth(true);
            contentScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            contentScroll.getStyleClass().add("product-dialog-scroll");
            VBox.setVgrow(contentScroll, Priority.ALWAYS);

            root.getChildren().addAll(header, contentScroll, actionRow);

            DialogSize dialogSize = resolveManageDialogSize(dialog);
            Scene scene = new Scene(root, dialogSize.width(), dialogSize.height());
            applyApplicationStyles(scene);
            dialog.setScene(scene);
            dialog.setMinWidth(Math.min(MANAGE_DIALOG_MIN_WIDTH, dialogSize.width()));
            dialog.setMinHeight(Math.min(MANAGE_DIALOG_MIN_HEIGHT, dialogSize.height()));
            TableViewSupport.enableDeselectOnOutsideClick(root, table);
            DialogSupport.preventInitialFieldFocus(dialog, root);
            DialogSupport.centerWindowOnOwner(dialog);
            if (!dialog.isShowing()) {
                dialog.show();
            }
        } catch (Exception ex) {
            dialog.close();
            handleError(context, ex);
        }
    }

    private static TableView<StocktakeDraftRow> createStocktakeItemsTable(
        ObservableList<StocktakeDraftRow> rows,
        boolean editable,
        Context context
    ) {
        TableView<StocktakeDraftRow> table = new TableView<>(rows);
        TableViewSupport.prepareNonReorderableTable(table);
        table.setEditable(editable);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.getStyleClass().add("stocktake-session-table");

        TableColumn<StocktakeDraftRow, String> productCol = new TableColumn<>("Product");
        productCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProductName()));

        TableColumn<StocktakeDraftRow, Number> systemCol = new TableColumn<>("System Qty");
        systemCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getSystemQuantity()));
        systemCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<StocktakeDraftRow, Integer> countedCol = new TableColumn<>("Counted Qty");
        countedCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getCountedQuantity()));
        countedCol.setStyle("-fx-alignment: CENTER;");
        countedCol.setEditable(editable);
        countedCol.setCellFactory(column -> {
            TextFieldTableCell<StocktakeDraftRow, Integer> cell =
                new TextFieldTableCell<>(new IntegerStringConverter()) {
                    @Override
                    public void updateItem(Integer item, boolean empty) {
                        super.updateItem(item, empty);
                        getStyleClass().removeAll("stocktake-editable-cell", "stocktake-placeholder-cell", "stocktake-note-cell");
                        setCursor(!empty && editable ? Cursor.TEXT : Cursor.DEFAULT);
                        if (!empty && editable && !isEditing()) {
                            getStyleClass().add("stocktake-editable-cell");
                        }
                    }
                };
            cell.setAlignment(Pos.CENTER);
            enableSingleClickEditing(cell, editable);
            return cell;
        });
        countedCol.setOnEditCommit(event -> {
            int value = event.getNewValue() == null ? 0 : event.getNewValue();
            if (value < 0) {
                handleError(context, new ValidationException("Counted quantity cannot be negative"));
                table.refresh();
                return;
            }
            event.getRowValue().setCountedQuantity(value);
            table.refresh();
        });

        TableColumn<StocktakeDraftRow, Number> varianceCol = new TableColumn<>("Variance");
        varianceCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getVariance()));
        varianceCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<StocktakeDraftRow, String> unitCostCol = new TableColumn<>("Unit Cost");
        unitCostCol.setCellValueFactory(data -> new SimpleStringProperty(formatVnd(data.getValue().getUnitCost())));

        TableColumn<StocktakeDraftRow, String> notesCol = new TableColumn<>("Item Notes");
        notesCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNotes() == null ? "" : data.getValue().getNotes()));
        notesCol.setCellFactory(column -> {
            TextFieldTableCell<StocktakeDraftRow, String> cell =
                new TextFieldTableCell<>(new DefaultStringConverter()) {
                    @Override
                    public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        getStyleClass().removeAll("stocktake-editable-cell", "stocktake-placeholder-cell", "stocktake-note-cell");
                        setCursor(!empty && editable ? Cursor.TEXT : Cursor.DEFAULT);
                        if (empty) {
                            return;
                        }
                        if (editable && !isEditing()) {
                            getStyleClass().add("stocktake-note-cell");
                            if (item == null || item.isBlank()) {
                                setText("Click to add note");
                                getStyleClass().add("stocktake-placeholder-cell");
                            }
                        } else if (!isEditing() && (item == null || item.isBlank())) {
                            setText("-");
                            getStyleClass().add("stocktake-placeholder-cell");
                        }
                    }
                };
            cell.setAlignment(Pos.CENTER_LEFT);
            enableSingleClickEditing(cell, editable);
            return cell;
        });
        notesCol.setOnEditCommit(event -> {
            event.getRowValue().setNotes(event.getNewValue());
            table.refresh();
        });
        notesCol.setEditable(editable);

        table.getColumns().addAll(productCol, systemCol, countedCol, varianceCol, unitCostCol, notesCol);
        VBox.setVgrow(table, Priority.ALWAYS);
        return table;
    }

    private static <S, T> void enableSingleClickEditing(TextFieldTableCell<S, T> cell, boolean editable) {
        if (!editable) {
            return;
        }
        cell.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
            if (!cell.isEmpty() && !cell.isEditing()) {
                cell.startEdit();
                event.consume();
            }
        });
    }

    private static Label createStocktakeDetailValue(String text, boolean strong) {
        Label label = new Label(text == null || text.isBlank() ? "-" : text);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        label.getStyleClass().add(strong ? "stocktake-detail-value-strong" : "stocktake-detail-value");
        return label;
    }

    private static StringConverter<Category> categoryConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Category category) {
                return category == null ? "" : category.getName();
            }

            @Override
            public Category fromString(String string) {
                return null;
            }
        };
    }

    private static String formatStocktakeScopeLabel(StocktakeScopeType scopeType) {
        return FxFormatters.enumText(scopeType);
    }

    private static String formatDateTimeWithSeconds(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_SECONDS_FORMATTER) : "-";
    }

    private static String formatUserDisplayName(User user) {
        if (user == null) {
            return "-";
        }
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return "-";
    }

    private static String formatVnd(BigDecimal amount) {
        return String.format("%,.0f VND", MoneySupport.normalize(amount).doubleValue());
    }

    private static void applyApplicationStyles(Scene scene) {
        scene.getStylesheets().add(Objects.requireNonNull(StocktakeDialog.class.getResource("/application.css")).toExternalForm());
    }

    private static DialogSize resolveManageDialogSize(Stage dialog) {
        Window owner = dialog == null ? null : dialog.getOwner();
        Rectangle2D bounds = owner != null && owner.isShowing()
            ? Screen.getScreensForRectangle(owner.getX(), owner.getY(), owner.getWidth(), owner.getHeight())
                .stream()
                .findFirst()
                .orElse(Screen.getPrimary())
                .getVisualBounds()
            : Screen.getPrimary().getVisualBounds();
        double width = clamp(MANAGE_DIALOG_PREF_WIDTH, MANAGE_DIALOG_MIN_WIDTH, bounds.getWidth() - MANAGE_DIALOG_SCREEN_MARGIN);
        double height = clamp(MANAGE_DIALOG_PREF_HEIGHT, MANAGE_DIALOG_MIN_HEIGHT, bounds.getHeight() - MANAGE_DIALOG_SCREEN_MARGIN);
        return new DialogSize(width, height);
    }

    private static double clamp(double value, double min, double max) {
        if (max < min) {
            return Math.max(420, max);
        }
        return Math.max(min, Math.min(value, max));
    }

    private static void handleError(Context context, Throwable throwable) {
        if (context != null && context.errorHandler() != null) {
            context.errorHandler().accept(throwable);
        } else if (throwable != null) {
            throw new RuntimeException(throwable);
        }
    }

    private record DialogSize(double width, double height) {
    }

    private static final class StocktakeDraftRow {
        private final Long itemId;
        private final String productName;
        private final int systemQuantity;
        private final BigDecimal unitCost;
        private int countedQuantity;
        private String notes;

        private StocktakeDraftRow(StocktakeItem item) {
            this.itemId = item.getId();
            this.productName = item.getProduct() != null ? item.getProduct().getName() : "Unknown";
            this.systemQuantity = item.getSystemQuantity() != null ? item.getSystemQuantity() : 0;
            this.unitCost = item.getUnitCostSnapshot();
            this.countedQuantity = item.getCountedQuantity() != null ? item.getCountedQuantity() : 0;
            this.notes = item.getNotes();
        }

        public Long getItemId() {
            return itemId;
        }

        public String getProductName() {
            return productName;
        }

        public int getSystemQuantity() {
            return systemQuantity;
        }

        public int getCountedQuantity() {
            return countedQuantity;
        }

        public void setCountedQuantity(int countedQuantity) {
            this.countedQuantity = countedQuantity;
        }

        public int getVariance() {
            return countedQuantity - systemQuantity;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        public BigDecimal getUnitCost() {
            return unitCost;
        }
    }
}
