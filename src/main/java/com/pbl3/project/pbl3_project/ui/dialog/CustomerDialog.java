package com.pbl3.project.pbl3_project.ui.dialog;

import com.pbl3.project.pbl3_project.dto.CustomerOrderAggregate;
import com.pbl3.project.pbl3_project.entity.Customer;
import com.pbl3.project.pbl3_project.entity.Order;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.service.CustomerService;
import com.pbl3.project.pbl3_project.service.MoneySupport;
import com.pbl3.project.pbl3_project.service.OrderService;
import com.pbl3.project.pbl3_project.service.ToastService;
import com.pbl3.project.pbl3_project.ui.component.ButtonFactory;
import com.pbl3.project.pbl3_project.ui.component.DialogFormFactory;
import com.pbl3.project.pbl3_project.ui.util.AsyncPageCache;
import com.pbl3.project.pbl3_project.ui.util.AsyncUiTask;
import com.pbl3.project.pbl3_project.ui.util.DialogSupport;
import com.pbl3.project.pbl3_project.ui.util.FxFormatters;
import com.pbl3.project.pbl3_project.ui.util.TableViewSupport;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class CustomerDialog {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_TIME_SECONDS_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private CustomerDialog() {
    }

    public record Context(
        CustomerService customerService,
        OrderService orderService,
        ToastService toastService,
        Consumer<Throwable> errorHandler,
        OrderDetailsOpener orderDetailsOpener
    ) {
    }

    @FunctionalInterface
    public interface OrderDetailsOpener {
        void open(Stage owner, Order order, User actor, Runnable onChanged);
    }

    private record CustomerDetailsData(Customer customer, CustomerOrderAggregate aggregate) {
    }

    private record CustomerOrdersPage(Page<Order> page, int pageIndex) {
    }

    private record CustomerPickerPage(Page<Customer> page, int pageIndex) {
    }

    public static void showUpsert(
        Stage owner,
        User actor,
        Customer target,
        Runnable onSuccess,
        Context context
    ) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle(target == null ? "Add Customer" : "Edit Customer");

        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("dialog-root");
        root.setFillWidth(true);

        Label titleLabel = new Label(target == null ? "Add Customer" : "Edit Customer");
        titleLabel.getStyleClass().add("dialog-title");

        TextField nameField = DialogFormFactory.textField(target != null ? target.getFullName() : "", "Full Name");
        TextField phoneField = DialogFormFactory.textField(target != null ? target.getPhone() : "", "Phone");
        configureFormField(nameField);
        configureFormField(phoneField);

        GridPane form = new GridPane();
        form.setHgap(14);
        form.setVgap(14);
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(78);
        labelColumn.setPrefWidth(78);
        labelColumn.setMaxWidth(78);
        ColumnConstraints fieldColumn = new ColumnConstraints();
        fieldColumn.setHgrow(Priority.ALWAYS);
        fieldColumn.setFillWidth(true);
        form.getColumnConstraints().setAll(labelColumn, fieldColumn);
        form.add(DialogFormFactory.formLabel("Full Name *"), 0, 0);
        form.add(nameField, 1, 0);
        form.add(DialogFormFactory.formLabel("Phone *"), 0, 1);
        form.add(phoneField, 1, 1);

        Button saveBtn = new Button(target == null ? "Create" : "Save");
        saveBtn.getStyleClass().addAll("button", "primary-button");
        saveBtn.setPrefHeight(36);
        saveBtn.setMinHeight(36);
        saveBtn.setPrefWidth(104);
        saveBtn.setMinWidth(104);
        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("filter-reset-btn", "dialog-cancel-button");
        cancelBtn.setPrefHeight(40);
        cancelBtn.setMinHeight(40);
        cancelBtn.setPrefWidth(104);
        cancelBtn.setMinWidth(104);
        cancelBtn.setOnAction(event -> dialog.close());
        saveBtn.setOnAction(event -> {
            try {
                String name = nameField.getText();
                String phone = phoneField.getText();
                AsyncUiTask.runButtonTask(
                    saveBtn,
                    cancelBtn,
                    target == null ? "Creating..." : "Saving...",
                    () -> {
                        if (target == null) {
                            return context.customerService().createCustomer(actor, name, phone);
                        }
                        return context.customerService().updateCustomer(actor, target.getId(), name, phone);
                    },
                    saved -> {
                        context.toastService().showSuccess(target == null ? "Customer created." : "Customer updated.");
                        dialog.close();
                        if (onSuccess != null) {
                            onSuccess.run();
                        }
                    },
                    ex -> handleError(context, ex),
                    "customer-save"
                );
            } catch (Exception ex) {
                handleError(context, ex);
            }
        });

        HBox actions = new HBox(10, cancelBtn, saveBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(2, 0, 0, 0));

        root.getChildren().addAll(titleLabel, form, actions);

        Scene scene = new Scene(root, 500, 260);
        applyApplicationStyles(scene);
        dialog.setScene(scene);
        dialog.setMinWidth(500);
        dialog.setMinHeight(260);
        DialogSupport.preventInitialFieldFocus(dialog, root);
        dialog.showAndWait();
    }

    public static void showDetails(
        Stage owner,
        User actor,
        Customer customer,
        Runnable onChanged,
        Context context
    ) {
        Stage dialog = DialogSupport.showLoadingWindow(
            owner,
            MessageFormat.format("Customer #{0}", customer.getId()),
            "Loading customer details...",
            420,
            240
        );

        javafx.concurrent.Task<CustomerDetailsData> task = new javafx.concurrent.Task<>() {
            @Override
            protected CustomerDetailsData call() {
                Customer managedCustomer = context.customerService().getCustomerById(actor, customer.getId());
                return new CustomerDetailsData(
                    managedCustomer,
                    context.customerService().getCustomerAggregate(actor, managedCustomer.getId())
                );
            }
        };
        task.setOnSucceeded(event -> {
            if (!dialog.isShowing()) {
                return;
            }
            populateDetailsDialog(dialog, actor, onChanged, context, task.getValue());
        });
        task.setOnFailed(event -> {
            dialog.close();
            handleError(context, task.getException());
        });
        Thread worker = new Thread(task, "customer-details-dialog-loader");
        worker.setDaemon(true);
        worker.start();
    }

    private static void populateDetailsDialog(
        Stage dialog,
        User actor,
        Runnable onChanged,
        Context context,
        CustomerDetailsData data
    ) {
        try {
            Customer managedCustomer = data.customer();
            CustomerOrderAggregate aggregate = data.aggregate();
            dialog.setTitle(MessageFormat.format("Customer #{0}", managedCustomer.getId()));

            VBox root = new VBox(15);
            root.getStyleClass().add("dialog-root");

            Label titleLabel = new Label(MessageFormat.format("Customer #{0}", managedCustomer.getId()));
            titleLabel.getStyleClass().add("dialog-title");
            titleLabel.setMaxWidth(Double.MAX_VALUE);

            VBox metaBox = new VBox(
                8,
                createDetailMetaRow("Name", createDetailMetaValueLabel(managedCustomer.getFullName(), "-fx-font-size: 14px; -fx-text-fill: -app-text-secondary;")),
                createDetailMetaRow("Phone", createDetailMetaValueLabel(managedCustomer.getPhone(), "-fx-font-size: 14px; -fx-text-fill: -app-text-secondary;")),
                createDetailMetaRow("Status", createDetailMetaValueLabel(
                    formatCustomerStatus(managedCustomer.isEnabled()),
                    "-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: " + (managedCustomer.isEnabled() ? "-app-success-hover" : "-app-danger-hover") + ";"
                )),
                createDetailMetaRow("Created At", createDetailMetaValueLabel(formatDateTimeWithSeconds(managedCustomer.getCreatedAt()), "-fx-font-size: 14px; -fx-text-fill: -app-text-secondary;")),
                createDetailMetaRow("Updated At", createDetailMetaValueLabel(formatDateTimeWithSeconds(managedCustomer.getUpdatedAt()), "-fx-font-size: 14px; -fx-text-fill: -app-text-secondary;"))
            );

            HBox summaryRow = new HBox(
                12,
                createCustomerSummaryCard("Total Orders", String.valueOf(aggregate.orderCount()), "-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: -app-primary;"),
                createCustomerSummaryCard("Total Spent", formatVnd(aggregate.totalSpent()), "-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: -app-success-hover;"),
                createCustomerSummaryCard(
                    "Last Purchase",
                    aggregate.lastPurchase() != null ? formatDateTime(aggregate.lastPurchase()) : "-",
                    "-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: -app-text-primary;"
                )
            );

            Label historyLabel = new Label("Purchase History");
            historyLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: -app-text-primary;");

            TableView<Order> table = new TableView<>();
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            TableViewSupport.prepareNonReorderableTable(table);

            TableColumn<Order, Long> idCol = new TableColumn<>("ID");
            idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

            TableColumn<Order, String> dateCol = new TableColumn<>("Created At");
            dateCol.setCellValueFactory(cell -> new SimpleStringProperty(formatDateTime(cell.getValue().getCreatedAt())));

            TableColumn<Order, String> totalCol = new TableColumn<>("Total");
            totalCol.setCellValueFactory(cell -> new SimpleStringProperty(formatVnd(cell.getValue().getTotalPrice())));

            TableColumn<Order, String> userCol = new TableColumn<>("Created By");
            userCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCreatedByDisplayName()));

            TableColumn<Order, String> statusCol = new TableColumn<>("Status");
            statusCol.setCellValueFactory(cell -> new SimpleStringProperty(formatOrderStatus(cell.getValue().getStatus())));

            table.getColumns().add(idCol);
            table.getColumns().add(dateCol);
            table.getColumns().add(totalCol);
            table.getColumns().add(userCol);
            table.getColumns().add(statusCol);

            PagedTableState<Order> tableState = new PagedTableState<>(table, 8);
            AsyncPageCache<CustomerOrdersPage> pageCache = new AsyncPageCache<>(24);
            tableState.loadPage = () -> {
                int requestedPage = tableState.currentPage;
                java.util.function.IntFunction<Object> cacheKeyForPage = pageIndex -> java.util.Arrays.asList(managedCustomer.getId(), pageIndex);
                java.util.function.IntFunction<CustomerOrdersPage> fetchOrdersPage = pageIndex -> {
                    int resolvedPage = pageIndex;
                    Page<Order> pageData = context.customerService().searchCustomerOrders(
                        actor,
                        managedCustomer.getId(),
                        PageRequest.of(resolvedPage, tableState.pageSize, Sort.by(Sort.Direction.DESC, "createdAt"))
                    );
                    if (pageData.getTotalPages() > 0 && resolvedPage >= pageData.getTotalPages()) {
                        resolvedPage = pageData.getTotalPages() - 1;
                        pageData = context.customerService().searchCustomerOrders(
                            actor,
                            managedCustomer.getId(),
                            PageRequest.of(resolvedPage, tableState.pageSize, Sort.by(Sort.Direction.DESC, "createdAt"))
                        );
                    }
                    return new CustomerOrdersPage(pageData, resolvedPage);
                };
                AsyncUiTask.runLatestCachedTableLoad(
                    table,
                    tableState.prevBtn,
                    tableState.nextBtn,
                    tableState.loadVersion,
                    pageCache,
                    cacheKeyForPage.apply(requestedPage),
                    () -> fetchOrdersPage.apply(requestedPage),
                    result -> {
                        tableState.currentPage = result.pageIndex();
                        tableState.setPage(result.page());
                        int nextPage = result.pageIndex() + 1;
                        if (nextPage < tableState.totalPages) {
                            pageCache.prefetch(
                                cacheKeyForPage.apply(nextPage),
                                () -> fetchOrdersPage.apply(nextPage),
                                null,
                                "customer-orders-next-page-prefetch"
                            );
                        }
                        int previousPage = result.pageIndex() - 1;
                        if (previousPage >= 0) {
                            pageCache.prefetch(
                                cacheKeyForPage.apply(previousPage),
                                () -> fetchOrdersPage.apply(previousPage),
                                null,
                                "customer-orders-prev-page-prefetch"
                            );
                        }
                    },
                    ex -> handleError(context, ex),
                    "Loading purchases...",
                    "Could not load purchases",
                    "customer-orders-page-loader"
                );
            };

            tableState.prevBtn.setOnAction(event -> {
                if (tableState.currentPage > 0) {
                    tableState.currentPage--;
                    tableState.loadPage.run();
                }
            });
            tableState.nextBtn.setOnAction(event -> {
                if (tableState.currentPage + 1 < tableState.totalPages) {
                    tableState.currentPage++;
                    tableState.loadPage.run();
                }
            });
            table.setRowFactory(view -> {
                TableRow<Order> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY
                        && event.getClickCount() == 2
                        && !row.isEmpty()
                        && context.orderDetailsOpener() != null) {
                        openOrderDetailsAsync(dialog, row.getItem().getId(), actor, onChanged, context);
                    }
                });
                return row;
            });
            TableViewSupport.enableDeselectOnOutsideClick(root, table);

            Button closeButton = new Button("Close");
            closeButton.getStyleClass().addAll("button", "close-button");
            closeButton.setOnAction(event -> dialog.close());

            HBox statusBar = new HBox(15, tableState.rowCountLabel, tableState.pageLabel, tableState.prevBtn, tableState.nextBtn);
            statusBar.setAlignment(Pos.CENTER_RIGHT);

            root.getChildren().addAll(titleLabel, metaBox, summaryRow, historyLabel, table, statusBar, closeButton);
            VBox.setVgrow(table, Priority.ALWAYS);

            Scene scene = new Scene(root, 860, 720);
            applyApplicationStyles(scene);
            dialog.setScene(scene);
            DialogSupport.preventInitialFieldFocus(dialog, root);
            DialogSupport.centerWindowOnOwner(dialog);
            if (!dialog.isShowing()) {
                dialog.show();
            }
            tableState.loadPage.run();
        } catch (Exception ex) {
            dialog.close();
            handleError(context, ex);
        }
    }

    private static void openOrderDetailsAsync(
        Stage owner,
        Long orderId,
        User actor,
        Runnable onChanged,
        Context context
    ) {
        Stage loadingDialog = DialogSupport.showLoadingWindow(
            owner,
            MessageFormat.format("Order Details #{0}", orderId),
            "Loading order details...",
            420,
            240
        );
        javafx.concurrent.Task<Order> task = new javafx.concurrent.Task<>() {
            @Override
            protected Order call() {
                return context.orderService().getOrderWithItems(orderId, actor);
            }
        };
        task.setOnSucceeded(event -> {
            if (!loadingDialog.isShowing()) {
                return;
            }
            loadingDialog.close();
            context.orderDetailsOpener().open(owner, task.getValue(), actor, onChanged);
        });
        task.setOnFailed(event -> {
            loadingDialog.close();
            handleError(context, task.getException());
        });
        Thread worker = new Thread(task, "customer-order-details-loader");
        worker.setDaemon(true);
        worker.start();
    }

    public static Customer showPicker(Stage owner, User actor, Context context) {
        AtomicReference<Customer> selectedCustomerRef = new AtomicReference<>(null);

        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Select Customer");

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setFillWidth(true);
        root.getStyleClass().addAll("dialog-root", "product-dialog-root", "customer-picker-root");

        Label titleLabel = new Label("Select Customer");
        titleLabel.getStyleClass().add("product-dialog-title");

        TextField searchField = DialogFormFactory.textField("", "Search name or phone");
        searchField.getStyleClass().addAll("product-dialog-input", "customer-picker-search");
        searchField.setMaxWidth(Double.MAX_VALUE);
        VBox searchCard = new VBox(
            8,
            DialogFormFactory.formLabel("Search Customer"),
            searchField
        );
        searchCard.getStyleClass().add("customer-picker-search-card");

        TableView<Customer> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableViewSupport.prepareNonReorderableTable(table);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        TableColumn<Customer, String> nameCol = new TableColumn<>("Customer");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullName()));
        TableColumn<Customer, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPhone()));
        table.getColumns().add(nameCol);
        table.getColumns().add(phoneCol);
        table.getStyleClass().add("customer-picker-table");

        PagedTableState<Customer> tableState = new PagedTableState<>(table, 20);
        AsyncPageCache<CustomerPickerPage> pageCache = new AsyncPageCache<>(24);
        tableState.loadPage = () -> {
            int requestedPage = tableState.currentPage;
            String search = normalizeCustomerSearch(searchField.getText());
            java.util.function.IntFunction<Object> cacheKeyForPage = pageIndex -> java.util.Arrays.asList(search, pageIndex);
            java.util.function.IntFunction<CustomerPickerPage> fetchCustomersPage = pageIndex -> {
                int resolvedPage = pageIndex;
                Page<Customer> pageData = context.customerService().searchActiveCustomersForSales(
                    actor,
                    search,
                    PageRequest.of(resolvedPage, tableState.pageSize, Sort.by(Sort.Direction.ASC, "fullName"))
                );
                if (pageData.getTotalPages() > 0 && resolvedPage >= pageData.getTotalPages()) {
                    resolvedPage = pageData.getTotalPages() - 1;
                    pageData = context.customerService().searchActiveCustomersForSales(
                        actor,
                        search,
                        PageRequest.of(resolvedPage, tableState.pageSize, Sort.by(Sort.Direction.ASC, "fullName"))
                    );
                }
                return new CustomerPickerPage(pageData, resolvedPage);
            };
            AsyncUiTask.runLatestCachedTableLoad(
                table,
                tableState.prevBtn,
                tableState.nextBtn,
                tableState.loadVersion,
                pageCache,
                cacheKeyForPage.apply(requestedPage),
                () -> fetchCustomersPage.apply(requestedPage),
                result -> {
                    tableState.currentPage = result.pageIndex();
                    tableState.setPage(result.page());
                    int nextPage = result.pageIndex() + 1;
                    if (nextPage < tableState.totalPages) {
                        pageCache.prefetch(
                            cacheKeyForPage.apply(nextPage),
                            () -> fetchCustomersPage.apply(nextPage),
                            null,
                            "customer-picker-next-page-prefetch"
                        );
                    }
                    int previousPage = result.pageIndex() - 1;
                    if (previousPage >= 0) {
                        pageCache.prefetch(
                            cacheKeyForPage.apply(previousPage),
                            () -> fetchCustomersPage.apply(previousPage),
                            null,
                            "customer-picker-prev-page-prefetch"
                        );
                    }
                },
                ex -> handleError(context, ex),
                "Loading customers...",
                "Could not load customers",
                "customer-picker-page-loader"
            );
        };

        PauseTransition searchPause = new PauseTransition(javafx.util.Duration.millis(220));
        searchPause.setOnFinished(event -> {
            tableState.currentPage = 0;
            tableState.loadPage.run();
        });
        searchField.textProperty().addListener((obs, oldValue, newValue) -> searchPause.playFromStart());
        searchField.setOnAction(event -> {
            searchPause.stop();
            tableState.currentPage = 0;
            tableState.loadPage.run();
        });

        tableState.prevBtn.setOnAction(event -> {
            if (tableState.currentPage > 0) {
                tableState.currentPage--;
                tableState.loadPage.run();
            }
        });
        tableState.nextBtn.setOnAction(event -> {
            if (tableState.currentPage + 1 < tableState.totalPages) {
                tableState.currentPage++;
                tableState.loadPage.run();
            }
        });

        Button selectBtn = new Button("Select Customer");
        selectBtn.getStyleClass().addAll("product-dialog-primary-button", "primary-button");
        selectBtn.setDisable(true);
        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().addAll("product-dialog-secondary-button", "dialog-cancel-button");
        closeBtn.setOnAction(event -> dialog.close());

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> selectBtn.setDisable(newValue == null));
        selectBtn.setOnAction(event -> {
            selectedCustomerRef.set(table.getSelectionModel().getSelectedItem());
            dialog.close();
        });
        table.setRowFactory(view -> {
            TableRow<Customer> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY
                    && event.getClickCount() == 2
                    && !row.isEmpty()) {
                    selectedCustomerRef.set(row.getItem());
                    dialog.close();
                }
            });
            return row;
        });

        VBox tableCard = new VBox(10, table);
        tableCard.getStyleClass().add("customer-picker-table-card");
        VBox.setVgrow(table, Priority.ALWAYS);

        HBox statusBar = new HBox(15, tableState.rowCountLabel, tableState.pageLabel, tableState.prevBtn, tableState.nextBtn);
        statusBar.getStyleClass().add("customer-picker-status-bar");
        statusBar.setAlignment(Pos.CENTER_RIGHT);
        HBox actions = new HBox(10, closeBtn, selectBtn);
        actions.getStyleClass().add("customer-picker-actions");
        actions.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(closeBtn, Priority.ALWAYS);
        HBox.setHgrow(selectBtn, Priority.ALWAYS);
        closeBtn.setMaxWidth(Double.MAX_VALUE);
        selectBtn.setMaxWidth(Double.MAX_VALUE);

        root.getChildren().addAll(titleLabel, searchCard, tableCard, statusBar, actions);
        VBox.setVgrow(tableCard, Priority.ALWAYS);
        TableViewSupport.enableDeselectOnOutsideClick(root, table);
        tableState.loadPage.run();

        Scene scene = new Scene(root, 620, 620);
        applyApplicationStyles(scene);
        dialog.setScene(scene);
        dialog.setMinWidth(580);
        dialog.setMinHeight(560);
        dialog.setResizable(true);
        DialogSupport.preventInitialFieldFocus(dialog, root);
        dialog.showAndWait();
        return selectedCustomerRef.get();
    }

    private static void configureFormField(TextField field) {
        field.setPrefWidth(220);
        field.setMaxWidth(Double.MAX_VALUE);
    }

    private static String normalizeCustomerSearch(String value) {
        return value == null ? "" : value.trim();
    }

    private static VBox createCustomerSummaryCard(String labelText, String valueText, String valueStyle) {
        Label keyLabel = new Label(labelText);
        keyLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -app-text-muted; -fx-font-weight: 600;");
        Label valueLabel = new Label(valueText);
        valueLabel.setWrapText(true);
        valueLabel.setStyle(valueStyle);

        VBox card = new VBox(6, keyLabel, valueLabel);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: -app-surface-muted; -fx-background-radius: 16; -fx-border-color: -app-border; -fx-border-radius: 16;");
        card.setPrefWidth(180);
        return card;
    }

    private static Label createDetailMetaValueLabel(String value, String style) {
        Label label = new Label(value != null ? value : "-");
        label.setWrapText(true);
        label.setStyle(style);
        return label;
    }

    private static HBox createDetailMetaRow(String labelText, Label valueLabel) {
        Label label = new Label(labelText);
        label.setMinWidth(92);
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: -app-text-muted; -fx-font-weight: 600;");
        HBox row = new HBox(10, label, valueLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static Label createStatusMetaLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: -app-text-muted; -fx-font-weight: 600;");
        return label;
    }

    private static String formatCustomerStatus(boolean enabled) {
        return enabled ? "Active" : "Disabled";
    }

    private static String formatOrderStatus(Enum<?> status) {
        return FxFormatters.enumText(status);
    }

    private static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : "-";
    }

    private static String formatDateTimeWithSeconds(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_SECONDS_FORMATTER) : "-";
    }

    private static String formatVnd(BigDecimal amount) {
        return String.format("%,.0f VND", MoneySupport.normalize(amount).doubleValue());
    }

    private static void applyApplicationStyles(Scene scene) {
        scene.getStylesheets().add(Objects.requireNonNull(CustomerDialog.class.getResource("/application.css")).toExternalForm());
    }

    private static void handleError(Context context, Throwable throwable) {
        if (context != null && context.errorHandler() != null) {
            context.errorHandler().accept(throwable);
        } else if (throwable != null) {
            throw new RuntimeException(throwable);
        }
    }

    private static final class PagedTableState<T> {
        private final TableView<T> table;
        private final int pageSize;
        private final Label rowCountLabel;
        private final Label pageLabel;
        private final Button prevBtn;
        private final Button nextBtn;
        private int currentPage;
        private int totalPages;
        private long totalElements;
        private final AtomicLong loadVersion = new AtomicLong();
        private Runnable loadPage;

        private PagedTableState(TableView<T> table, int pageSize) {
            this.table = table;
            this.pageSize = pageSize;
            this.rowCountLabel = createStatusMetaLabel(MessageFormat.format("Showing {0}-{1} of {2} Row(s)", 0, 0, 0));
            this.pageLabel = createStatusMetaLabel(MessageFormat.format("Page {0} / {1}", 0, 0));
            this.prevBtn = ButtonFactory.pageNav("Prev");
            this.nextBtn = ButtonFactory.pageNav("Next");
        }

        private void setPage(Page<T> pageData) {
            totalElements = pageData.getTotalElements();
            totalPages = pageData.getTotalPages();
            table.setItems(FXCollections.observableArrayList(pageData.getContent()));
            updateStatusBar();
        }

        private void updateStatusBar() {
            int from = totalElements == 0 ? 0 : currentPage * pageSize + 1;
            int to = totalElements == 0 ? 0 : Math.min((currentPage + 1) * pageSize, (int) totalElements);
            rowCountLabel.setText(MessageFormat.format("Showing {0}-{1} of {2} Row(s)", from, to, totalElements));
            pageLabel.setText(MessageFormat.format("Page {0} / {1}", totalPages == 0 ? 0 : currentPage + 1, totalPages));
            prevBtn.setDisable(currentPage <= 0);
            nextBtn.setDisable(currentPage + 1 >= totalPages);
        }
    }
}
