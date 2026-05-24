package com.pbl3.project.pbl3_project.ui.scene;

import com.pbl3.project.pbl3_project.dto.CustomerOrderAggregate;
import com.pbl3.project.pbl3_project.entity.Customer;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.ui.component.ActionTaskbarFactory;
import com.pbl3.project.pbl3_project.ui.component.ButtonFactory;
import com.pbl3.project.pbl3_project.ui.component.ExpandableSearchControl;
import com.pbl3.project.pbl3_project.ui.component.FilterControlFactory;
import com.pbl3.project.pbl3_project.ui.component.FilterDisclosureSection;
import com.pbl3.project.pbl3_project.ui.dialog.CustomerDialog;
import com.pbl3.project.pbl3_project.ui.dialog.OrderDialog;
import com.pbl3.project.pbl3_project.ui.util.AsyncPageCache;
import com.pbl3.project.pbl3_project.ui.util.AsyncUiTask;
import com.pbl3.project.pbl3_project.ui.util.SortCriterion;
import com.pbl3.project.pbl3_project.ui.util.TableSortState;
import com.pbl3.project.pbl3_project.ui.util.TableViewSupport;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PopupControl;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import org.springframework.data.domain.Page;

public final class CustomersScene {
    private static final Color PRIMARY_COLOR = Color.web("#1d7df2");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public record Options() {
    }

    private record CustomerPageResult(
        Page<Customer> page,
        int pageIndex,
        Map<Long, CustomerOrderAggregate> aggregateMap,
        Long restoreId,
        double previousScrollValue
    ) {
    }

    private CustomersScene() {
    }

    public static Node create(SceneRuntimeContext context, User user, Options options) {
        VBox root = new VBox();
        context.support().applyStandardTablePageLayout(root);

        ExpandableSearchControl searchControl = ExpandableSearchControl.create(300, PRIMARY_COLOR);
        TextField searchField = searchControl.field();

        final int pageSize = 20;
        final int[] currentPage = {0};
        final int[] totalPages = {0};
        final long[] totalElements = {0L};
        Runnable[] loadPageRef = new Runnable[1];
        AtomicReference<Boolean> enabledFilterRef = new AtomicReference<>(null);
        AtomicReference<Map<Long, CustomerOrderAggregate>> aggregateMapRef = new AtomicReference<>(Map.of());
        AtomicLong pageLoadVersion = new AtomicLong();
        AsyncPageCache<CustomerPageResult> pageCache = new AsyncPageCache<>(80);

        HBox filterBox = createFilterButton();
        Popup filterPopup = new Popup();
        filterPopup.setAutoHide(true);

        FilterControlFactory.Shell filterShell = FilterControlFactory.shell(320, 220);
        VBox popupContainer = filterShell.container();
        VBox scrollContent = filterShell.content();

        Label statusLabel = FilterControlFactory.sectionTitle("Status");
        CheckBox allStatusesCb = new CheckBox("All Statuses");
        allStatusesCb.setSelected(true);
        styleFilterCheckBox(allStatusesCb);

        VBox statusScroll = new VBox(8);
        statusScroll.setPadding(new Insets(5, 5, 5, 20));
        List<CheckBox> statusCbs = new ArrayList<>();
        Map<String, Boolean> customerStatuses = new LinkedHashMap<>();
        customerStatuses.put(formatStatus(true), Boolean.TRUE);
        customerStatuses.put(formatStatus(false), Boolean.FALSE);
        for (Map.Entry<String, Boolean> entry : customerStatuses.entrySet()) {
            CheckBox cb = new CheckBox(entry.getKey());
            cb.setUserData(entry.getValue());
            cb.setSelected(true);
            styleFilterCheckBox(cb);
            cb.setOnAction(e -> syncAllCheckbox(allStatusesCb, statusCbs));
            statusCbs.add(cb);
            statusScroll.getChildren().add(cb);
        }
        allStatusesCb.setOnAction(e -> statusCbs.forEach(cb -> cb.setSelected(allStatusesCb.isSelected())));
        FilterDisclosureSection statusSection = new FilterDisclosureSection(allStatusesCb, statusScroll);

        Button resetFilterBtn = new Button("Reset");
        resetFilterBtn.getStyleClass().add("filter-reset-btn");
        resetFilterBtn.setOnAction(e -> {
            allStatusesCb.setSelected(true);
            statusCbs.forEach(cb -> cb.setSelected(true));
            statusSection.setExpanded(false);
            enabledFilterRef.set(null);
            currentPage[0] = 0;
            loadPageRef[0].run();
            filterBox.setStyle("");
            filterPopup.hide();
        });

        Button applyFilterBtn = new Button("Apply Filter");
        applyFilterBtn.getStyleClass().add("filter-apply-btn");
        applyFilterBtn.setOnAction(e -> {
            Set<Boolean> selectedStatuses = new LinkedHashSet<>();
            for (CheckBox cb : statusCbs) {
                if (cb.isSelected()) {
                    selectedStatuses.add((Boolean) cb.getUserData());
                }
            }
            enabledFilterRef.set(selectedStatuses.size() == 1 ? selectedStatuses.iterator().next() : null);
            currentPage[0] = 0;
            loadPageRef[0].run();
            boolean hasFilter = !allStatusesCb.isSelected();
            filterBox.setStyle(hasFilter ? "-fx-border-color: -app-primary;" : "");
            filterPopup.hide();
        });

        scrollContent.getChildren().addAll(statusLabel, statusSection.getNode());
        popupContainer.getChildren().add(FilterControlFactory.actionRow(resetFilterBtn, applyFilterBtn));
        filterPopup.getContent().add(popupContainer);
        filterBox.setOnMouseClicked(e -> togglePopup(context, filterPopup, filterBox, -200, 5));

        TableSortState customerSortState = context.support().getOrCreateTableSortState(
            "customers",
            new SortCriterion("fullName", TableColumn.SortType.ASCENDING)
        );
        LinkedHashMap<String, String> customerSortProperties = new LinkedHashMap<>();
        customerSortProperties.put("id", "id");
        customerSortProperties.put("fullName", "fullName");
        customerSortProperties.put("phone", "phone");
        customerSortProperties.put("enabled", "enabled");
        LinkedHashMap<String, String> customerSortLabels = new LinkedHashMap<>();
        customerSortLabels.put("id", "ID");
        customerSortLabels.put("fullName", "Name");
        customerSortLabels.put("phone", "Phone");
        customerSortLabels.put("enabled", "Status");

        Button createButton = ButtonFactory.expandableGreenAction("Add Customer", 170);
        Button detailsButton = ActionTaskbarFactory.createButton(ActionTaskbarFactory.icon("eye"), "View Customer", "promotion-taskbar-button-view");
        Button editButton = ActionTaskbarFactory.createButton(ActionTaskbarFactory.icon("edit"), "Edit Customer", "promotion-taskbar-button-edit");
        Button toggleStatusButton = ActionTaskbarFactory.createButton(ActionTaskbarFactory.icon("power"), "Enable / Disable Customer", "promotion-taskbar-button-toggle");
        detailsButton.setDisable(true);
        editButton.setDisable(true);
        toggleStatusButton.setDisable(true);

        TableView<Customer> table = new TableView<>();
        context.support().applyStandardTableSizing(table);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        TableViewSupport.enableDragSelection(table);

        TableColumn<Customer, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Customer, String> nameCol = new TableColumn<>("Title");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullName()));

        TableColumn<Customer, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPhone()));

        TableColumn<Customer, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(formatStatus(data.getValue().isEnabled())));
        statusCol.setCellFactory(enabledStatusCellFactory());

        TableColumn<Customer, String> ordersCol = new TableColumn<>("Orders");
        ordersCol.setSortable(false);
        ordersCol.setCellValueFactory(data -> new SimpleStringProperty(
            String.valueOf(aggregateFor(data.getValue(), aggregateMapRef.get()).orderCount())
        ));

        TableColumn<Customer, String> totalSpentCol = new TableColumn<>("Total Spent");
        totalSpentCol.setSortable(false);
        totalSpentCol.setCellValueFactory(data -> new SimpleStringProperty(
            context.support().formatVnd(aggregateFor(data.getValue(), aggregateMapRef.get()).totalSpent())
        ));

        TableColumn<Customer, String> lastPurchaseCol = new TableColumn<>("Last Purchase");
        lastPurchaseCol.setSortable(false);
        lastPurchaseCol.setCellValueFactory(data -> {
            CustomerOrderAggregate aggregate = aggregateFor(data.getValue(), aggregateMapRef.get());
            return new SimpleStringProperty(aggregate.lastPurchase() != null ? formatDateTime(aggregate.lastPurchase()) : "-");
        });

        table.getColumns().addAll(idCol, nameCol, phoneCol, statusCol, ordersCol, totalSpentCol, lastPurchaseCol);
        LinkedHashMap<String, TableColumn<Customer, ?>> customerSortColumns = new LinkedHashMap<>();
        customerSortColumns.put("id", idCol);
        customerSortColumns.put("fullName", nameCol);
        customerSortColumns.put("phone", phoneCol);
        customerSortColumns.put("enabled", statusCol);
        context.support().installSortHeaderIndicators(customerSortColumns);

        Label rowCountLabel = context.support().createStatusMetaLabel(MessageFormat.format("Showing {0}-{1} of {2} Row(s)", 0, 0, 0));
        Label pageLabel = context.support().createStatusMetaLabel(MessageFormat.format("Page {0} / {1}", 0, 0));
        Button prevBtn = ButtonFactory.pageNav("Prev");
        Button nextBtn = ButtonFactory.pageNav("Next");
        AtomicReference<Long> customerSelectionRestoreId = new AtomicReference<>();

        Runnable updateStatusBar = () -> context.support().updatePagedStatus(
            table,
            rowCountLabel,
            pageLabel,
            prevBtn,
            nextBtn,
            totalElements[0],
            currentPage[0],
            totalPages[0],
            pageSize
        );

        Runnable loadPage = () -> {
            Long restoreId = customerSelectionRestoreId.getAndSet(null);
            double previousScrollValue = restoreId != null ? context.support().getTableVerticalScrollValue(table) : Double.NaN;
            String searchText = searchField.getText();
            Boolean enabledFilter = enabledFilterRef.get();
            int requestedPage = currentPage[0];
            List<SortCriterion> sortSnapshot = customerSortState.snapshot();
            TableSortState sortForLoad = new TableSortState(sortSnapshot);
            java.util.function.IntFunction<Object> cacheKeyForPage = pageIndex -> java.util.Arrays.asList(
                pageIndex,
                searchText,
                enabledFilter,
                sortSnapshot
            );
            java.util.function.IntFunction<CustomerPageResult> fetchCustomerPage = pageIndex -> {
                int resolvedPage = pageIndex;
                Page<Customer> pageData = context.customerService().searchCustomers(
                    user,
                    searchText,
                    enabledFilter,
                    context.support().createPageable(sortForLoad, customerSortProperties, resolvedPage, pageSize)
                );
                if (pageData.getTotalPages() > 0 && resolvedPage >= pageData.getTotalPages()) {
                    resolvedPage = pageData.getTotalPages() - 1;
                    pageData = context.customerService().searchCustomers(
                        user,
                        searchText,
                        enabledFilter,
                        context.support().createPageable(sortForLoad, customerSortProperties, resolvedPage, pageSize)
                    );
                }
                Map<Long, CustomerOrderAggregate> aggregateMap =
                    context.customerService().getCustomerAggregates(user, pageData.getContent());
                return new CustomerPageResult(pageData, resolvedPage, aggregateMap, null, Double.NaN);
            };

            AsyncUiTask.runLatestCachedTableLoad(
                table,
                prevBtn,
                nextBtn,
                pageLoadVersion,
                pageCache,
                cacheKeyForPage.apply(requestedPage),
                () -> fetchCustomerPage.apply(requestedPage),
                result -> {
                    Page<Customer> pageData = result.page();
                    currentPage[0] = result.pageIndex();
                    totalElements[0] = pageData.getTotalElements();
                    totalPages[0] = pageData.getTotalPages();
                    aggregateMapRef.set(result.aggregateMap());
                    table.setItems(FXCollections.observableArrayList(pageData.getContent()));
                    if (restoreId != null) {
                        context.support().restoreTableSelectionById(table, restoreId, Customer::getId);
                        context.support().restoreTableVerticalScrollValue(table, previousScrollValue);
                    }
                    table.refresh();
                    updateStatusBar.run();
                    int nextPage = result.pageIndex() + 1;
                    if (nextPage < totalPages[0]) {
                        pageCache.prefetch(
                            cacheKeyForPage.apply(nextPage),
                            () -> fetchCustomerPage.apply(nextPage),
                            null,
                            "customers-next-page-prefetch"
                        );
                    }
                    int previousPage = result.pageIndex() - 1;
                    if (previousPage >= 0) {
                        pageCache.prefetch(
                            cacheKeyForPage.apply(previousPage),
                            () -> fetchCustomerPage.apply(previousPage),
                            null,
                            "customers-prev-page-prefetch"
                        );
                    }
                },
                context::showUserFacingError,
                "Loading customers...",
                "Could not load customers",
                "customers-page-loader"
            );
        };
        loadPageRef[0] = () -> {
            pageCache.clear();
            loadPage.run();
        };

        Label customerSortStatusLabel = context.support().createSortStatusLabel(customerSortState, customerSortLabels);
        Runnable applyCustomerSortUi = () -> {
            context.support().applySortStateToTable(table, customerSortColumns, customerSortState);
            customerSortStatusLabel.setText(context.support().buildSortStatusText(customerSortState, customerSortLabels));
        };
        applyCustomerSortUi.run();
        context.support().installManualServerSorting(table, customerSortColumns, customerSortState, () -> {
            applyCustomerSortUi.run();
            currentPage[0] = 0;
            loadPage.run();
        });

        PauseTransition searchPause = new PauseTransition(Duration.millis(220));
        searchPause.setOnFinished(e -> {
            currentPage[0] = 0;
            loadPage.run();
        });
        searchField.textProperty().addListener((obs, oldV, newV) -> searchPause.playFromStart());

        prevBtn.setOnAction(e -> {
            if (currentPage[0] > 0) {
                currentPage[0]--;
                loadPage.run();
            }
        });
        nextBtn.setOnAction(e -> {
            if (currentPage[0] + 1 < totalPages[0]) {
                currentPage[0]++;
                loadPage.run();
            }
        });

        table.getSelectionModel().getSelectedItems().addListener((ListChangeListener<Customer>) c -> {
            boolean single = table.getSelectionModel().getSelectedItems().size() == 1;
            detailsButton.setDisable(!single);
            editButton.setDisable(!single);
            toggleStatusButton.setDisable(!single);
            Customer selected = single ? table.getSelectionModel().getSelectedItem() : null;
            toggleStatusButton.setTooltip(new Tooltip(
                selected == null ? "Enable / Disable Customer" : (selected.isEnabled() ? "Disable Customer" : "Enable Customer")
            ));
        });

        table.setRowFactory(tv -> {
            TableRow<Customer> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY
                    && event.getClickCount() == 2
                    && !row.isEmpty()) {
                    showCustomerDetailsDialog(context, user, row.getItem(), loadPageRef[0]);
                }
            });
            return row;
        });

        createButton.setOnAction(e -> showCustomerUpsertDialog(context, user, null, loadPageRef[0]));
        detailsButton.setOnAction(e -> {
            Customer selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showCustomerDetailsDialog(context, user, selected, loadPageRef[0]);
            }
        });
        editButton.setOnAction(e -> {
            Customer selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showCustomerUpsertDialog(context, user, selected, loadPageRef[0]);
            }
        });
        toggleStatusButton.setOnAction(e -> {
            Customer selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            try {
                customerSelectionRestoreId.set(selected.getId());
                Customer updated = context.customerService().setCustomerEnabled(user, selected.getId(), !selected.isEnabled());
                context.toastService().showSuccess(updated.isEnabled() ? "Customer enabled" : "Customer disabled");
                loadPageRef[0].run();
            } catch (Exception ex) {
                context.showUserFacingError(ex);
            }
        });

        HBox customerActionTaskbar = ActionTaskbarFactory.create(detailsButton, editButton, toggleStatusButton);

        BorderPane toolbar = new BorderPane();
        HBox rightBox = new HBox(12, filterBox, searchControl.box(), createButton);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        toolbar.setLeft(customerActionTaskbar);
        toolbar.setRight(rightBox);

        HBox statusBar = new HBox(15, customerSortStatusLabel, rowCountLabel, pageLabel, prevBtn, nextBtn);
        context.support().applyStandardTableStatusBar(statusBar);

        root.getChildren().addAll(toolbar, table, statusBar);
        VBox.setVgrow(table, Priority.ALWAYS);
        TableViewSupport.enableDeselectOnOutsideClick(root, table);
        javafx.application.Platform.runLater(loadPage);
        return root;
    }

    private static HBox createFilterButton() {
        HBox filterBox = new HBox();
        filterBox.setAlignment(Pos.CENTER);
        filterBox.getStyleClass().add("expandable-search-box");
        filterBox.setPrefSize(40, 40);
        filterBox.setMinSize(40, 40);
        filterBox.setMaxSize(40, 40);
        filterBox.setCursor(Cursor.HAND);
        SVGPath filterIcon = new SVGPath();
        filterIcon.setContent("M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z");
        filterIcon.setFill(PRIMARY_COLOR);
        filterBox.getChildren().add(filterIcon);
        javafx.scene.control.Tooltip.install(filterBox, new javafx.scene.control.Tooltip("Filter"));
        return filterBox;
    }

    private static void togglePopup(SceneRuntimeContext context, Popup popup, Node owner, double xOffset, double yOffset) {
        if (popup.isShowing()) {
            popup.hide();
            return;
        }
        context.support().showPopupBelow(popup, owner, xOffset, yOffset);
    }

    private static void styleFilterCheckBox(CheckBox checkBox) {
        checkBox.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
    }

    private static void syncAllCheckbox(CheckBox allCheckBox, List<CheckBox> checkBoxes) {
        if (!checkBoxes.stream().allMatch(CheckBox::isSelected)) {
            allCheckBox.setSelected(false);
        } else {
            allCheckBox.setSelected(true);
        }
    }

    private static CustomerOrderAggregate aggregateFor(Customer customer, Map<Long, CustomerOrderAggregate> aggregateMap) {
        if (customer == null || customer.getId() == null) {
            return CustomerOrderAggregate.empty(null);
        }
        if (aggregateMap == null) {
            return CustomerOrderAggregate.empty(customer.getId());
        }
        return aggregateMap.getOrDefault(customer.getId(), CustomerOrderAggregate.empty(customer.getId()));
    }

    private static String formatStatus(boolean enabled) {
        return enabled ? "Active" : "Disabled";
    }

    private static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : "-";
    }

    private static Callback<TableColumn<Customer, String>, TableCell<Customer, String>> enabledStatusCellFactory() {
        return col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                String color = formatStatus(true).equals(item) ? "-app-success-hover" : "-app-danger-hover";
                setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-alignment: CENTER;");
            }
        };
    }

    private static void showCustomerUpsertDialog(SceneRuntimeContext context, User actor, Customer target, Runnable onSuccess) {
        CustomerDialog.showUpsert(context.owner(), actor, target, onSuccess, customerDialogContext(context));
    }

    private static void showCustomerDetailsDialog(SceneRuntimeContext context, User actor, Customer customer, Runnable onChanged) {
        CustomerDialog.showDetails(context.owner(), actor, customer, onChanged, customerDialogContext(context));
    }

    private static CustomerDialog.Context customerDialogContext(SceneRuntimeContext context) {
        return new CustomerDialog.Context(
            context.customerService(),
            context.orderService(),
            context.toastService(),
            context::showUserFacingError,
            (owner, order, actor, onChanged) -> OrderDialog.showDetails(
                owner,
                order,
                actor,
                onChanged,
                new OrderDialog.Context(
                    context.orderService(),
                    context.receiptService(),
                    context.toastService(),
                    context::showUserFacingError
                )
            )
        );
    }
}
