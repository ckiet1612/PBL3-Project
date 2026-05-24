package com.pbl3.project.pbl3_project.ui.scene;

import com.pbl3.project.pbl3_project.dto.IdLabelOption;
import com.pbl3.project.pbl3_project.entity.ImportOrder;
import com.pbl3.project.pbl3_project.entity.ImportOrderStatus;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.service.MoneySupport;
import com.pbl3.project.pbl3_project.ui.component.ButtonFactory;
import com.pbl3.project.pbl3_project.ui.component.ExpandableSearchControl;
import com.pbl3.project.pbl3_project.ui.component.FilterControlFactory;
import com.pbl3.project.pbl3_project.ui.component.FilterDisclosureSection;
import com.pbl3.project.pbl3_project.ui.component.RangeSlider;
import com.pbl3.project.pbl3_project.ui.dialog.ImportOrderDialog;
import com.pbl3.project.pbl3_project.ui.scene.model.ImportOrderPrefill;
import com.pbl3.project.pbl3_project.ui.util.AsyncPageCache;
import com.pbl3.project.pbl3_project.ui.util.AsyncUiTask;
import com.pbl3.project.pbl3_project.ui.util.SortCriterion;
import com.pbl3.project.pbl3_project.ui.util.TableSortState;
import com.pbl3.project.pbl3_project.ui.util.TableViewSupport;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Popup;
import javafx.util.Duration;
import org.springframework.data.domain.Page;

public final class ImportGoodsScene {
    private static final Color PRIMARY_COLOR = Color.web("#1d7df2");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public record Options(ImportOrderPrefill prefill) {
    }

    private record ImportFilterOptions(List<IdLabelOption> supplierOptions, BigDecimal maxTotalCost) {
    }

    private record ImportPageResult(Page<ImportOrder> page, int pageIndex) {
    }

    private ImportGoodsScene() {
    }

    public static Node create(SceneRuntimeContext context, User user, Options options) {
        ImportOrderPrefill prefill = options == null ? null : options.prefill();

        TableSortState importSortState = context.support().getOrCreateTableSortState(
            "import-goods",
            new SortCriterion("createdAt", TableColumn.SortType.DESCENDING)
        );
        LinkedHashMap<String, String> importSortProperties = new LinkedHashMap<>();
        importSortProperties.put("id", "id");
        importSortProperties.put("supplier", "supplierNameSnapshot");
        importSortProperties.put("createdAt", "createdAt");
        importSortProperties.put("totalCost", "totalCost");
        importSortProperties.put("status", "status");

        VBox root = new VBox();
        context.support().applyStandardTablePageLayout(root);
        root.setStyle("-fx-background-color: transparent;");

        BorderPane toolbar = new BorderPane();
        ExpandableSearchControl searchControl = ExpandableSearchControl.create(250, PRIMARY_COLOR);
        TextField searchField = searchControl.field();
        HBox filterBox = createFilterButton();

        TableView<ImportOrder> table = new TableView<>();
        context.support().applyStandardTableSizing(table);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        TableViewSupport.enableDragSelection(table);

        final int pageSize = 20;
        final int[] currentPage = {0};
        final int[] totalPages = {0};
        final long[] totalElements = {0L};
        AtomicReference<String> searchRef = new AtomicReference<>("");
        AtomicReference<LocalDate> startDateRef = new AtomicReference<>(null);
        AtomicReference<LocalDate> endDateRef = new AtomicReference<>(null);
        AtomicReference<Set<Long>> suppliersRef = new AtomicReference<>(new LinkedHashSet<>());
        AtomicReference<Set<ImportOrderStatus>> statusesRef = new AtomicReference<>(new LinkedHashSet<>());
        AtomicReference<BigDecimal> minTotalRef = new AtomicReference<>(null);
        AtomicReference<BigDecimal> maxTotalRef = new AtomicReference<>(null);
        AtomicReference<ImportFilterOptions> filterOptionsCache = new AtomicReference<>();
        java.util.concurrent.atomic.AtomicLong pageLoadVersion = new java.util.concurrent.atomic.AtomicLong();
        AsyncPageCache<ImportPageResult> pageCache = new AsyncPageCache<>(80);

        Label rowCountLabel = context.support().createStatusMetaLabel(MessageFormat.format("Showing {0}-{1} of {2} Row(s)", 0, 0, 0));
        Label pageLabel = context.support().createStatusMetaLabel(MessageFormat.format("Page {0} / {1}", 0, 0));
        Button prevBtn = ButtonFactory.pageNav("Prev");
        Button nextBtn = ButtonFactory.pageNav("Next");

        Runnable[] refreshTableRef = new Runnable[1];
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
            int requestedPage = currentPage[0];
            String search = searchRef.get();
            LocalDate startDate = startDateRef.get();
            LocalDate endDate = endDateRef.get();
            Set<Long> suppliers = new LinkedHashSet<>(suppliersRef.get());
            Set<ImportOrderStatus> statuses = new LinkedHashSet<>(statusesRef.get());
            BigDecimal minTotal = minTotalRef.get();
            BigDecimal maxTotal = maxTotalRef.get();
            List<SortCriterion> sortSnapshot = importSortState.snapshot();
            TableSortState sortForLoad = new TableSortState(sortSnapshot);
            java.util.function.IntFunction<Object> cacheKeyForPage = pageIndex -> java.util.Arrays.asList(
                pageIndex,
                search,
                startDate,
                endDate,
                suppliers,
                statuses,
                minTotal,
                maxTotal,
                sortSnapshot
            );
            java.util.function.IntFunction<ImportPageResult> fetchImportPage = pageIndex -> {
                    int resolvedPage = requestedPage;
                    if (pageIndex >= 0) {
                        resolvedPage = pageIndex;
                    }
                    Page<ImportOrder> pageData = context.importOrderService().searchImportOrders(
                        search,
                        startDate,
                        endDate,
                        suppliers,
                        statuses,
                        minTotal,
                        maxTotal,
                        context.support().createPageable(sortForLoad, importSortProperties, resolvedPage, pageSize)
                    );
                    if (pageData.getTotalPages() > 0 && resolvedPage >= pageData.getTotalPages()) {
                        resolvedPage = pageData.getTotalPages() - 1;
                        pageData = context.importOrderService().searchImportOrders(
                            search,
                            startDate,
                            endDate,
                            suppliers,
                            statuses,
                            minTotal,
                            maxTotal,
                            context.support().createPageable(sortForLoad, importSortProperties, resolvedPage, pageSize)
                        );
                    }
                    return new ImportPageResult(pageData, resolvedPage);
            };

            AsyncUiTask.runLatestCachedTableLoad(
                table,
                prevBtn,
                nextBtn,
                pageLoadVersion,
                pageCache,
                cacheKeyForPage.apply(requestedPage),
                () -> fetchImportPage.apply(requestedPage),
                result -> {
                Page<ImportOrder> pageData = result.page();
                currentPage[0] = result.pageIndex();
                totalElements[0] = pageData.getTotalElements();
                totalPages[0] = pageData.getTotalPages();
                table.setItems(FXCollections.observableArrayList(pageData.getContent()));
                updateStatusBar.run();
                    int nextPage = result.pageIndex() + 1;
                    if (nextPage < totalPages[0]) {
                        pageCache.prefetch(
                            cacheKeyForPage.apply(nextPage),
                            () -> fetchImportPage.apply(nextPage),
                            null,
                            "import-goods-next-page-prefetch"
                        );
                    }
                    int previousPage = result.pageIndex() - 1;
                    if (previousPage >= 0) {
                        pageCache.prefetch(
                            cacheKeyForPage.apply(previousPage),
                            () -> fetchImportPage.apply(previousPage),
                            null,
                            "import-goods-prev-page-prefetch"
                        );
                    }
                },
                ex -> {
                    updateStatusBar.run();
                    context.showUserFacingError(ex);
                },
                "Loading imports...",
                "Could not load imports",
                "import-goods-page-loader"
            );
        };
        refreshTableRef[0] = () -> {
            pageCache.clear();
            loadPage.run();
        };
        Runnable refreshAfterImportChange = () -> {
            filterOptionsCache.set(null);
            pageCache.clear();
            loadPage.run();
        };

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

        Button manageImportBtn = ButtonFactory.expandableManageAction("Manage Import", 170);
        manageImportBtn.setDisable(true);
        manageImportBtn.setOnAction(e -> {
            List<ImportOrder> selectedImports = new ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selectedImports.size() != 1) {
                context.toastService().showWarning("Select an import order.");
                return;
            }
            openImportDetails(context, user, selectedImports.get(0).getId(), refreshAfterImportChange);
        });

        Button createBtn = ButtonFactory.expandableGreenAction("New Import", 140);
        createBtn.setOnAction(e -> showCreateImportDialog(context, user, refreshAfterImportChange, null));

        HBox leftBox = new HBox(15, manageImportBtn);
        leftBox.setAlignment(Pos.CENTER_LEFT);
        HBox rightBox = new HBox(15, filterBox, searchControl.box(), createBtn);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        toolbar.setLeft(leftBox);
        toolbar.setRight(rightBox);

        TableColumn<ImportOrder, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new SimpleStringProperty("IMP-" + data.getValue().getId()));

        TableColumn<ImportOrder, String> supplierCol = new TableColumn<>("Supplier");
        supplierCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSupplierDisplayName()));

        TableColumn<ImportOrder, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> new SimpleStringProperty(formatDateTime(data.getValue().getCreatedAt())));

        TableColumn<ImportOrder, String> costCol = new TableColumn<>("Total Cost");
        costCol.setCellValueFactory(data -> new SimpleStringProperty(context.support().formatVnd(data.getValue().getTotalCost())));

        TableColumn<ImportOrder, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(formatImportOrderStatus(data.getValue().getStatus())));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                ImportOrder order = getTableRow() != null ? getTableRow().getItem() : null;
                setStyle("-fx-text-fill: " + getImportOrderStatusColor(order != null ? order.getStatus() : null)
                    + "; -fx-font-weight: bold; -fx-alignment: CENTER;");
            }
        });

        table.getColumns().addAll(idCol, supplierCol, dateCol, costCol, statusCol);
        LinkedHashMap<String, TableColumn<ImportOrder, ?>> importSortColumns = new LinkedHashMap<>();
        importSortColumns.put("id", idCol);
        importSortColumns.put("supplier", supplierCol);
        importSortColumns.put("createdAt", dateCol);
        importSortColumns.put("totalCost", costCol);
        importSortColumns.put("status", statusCol);
        context.support().installSortHeaderIndicators(importSortColumns);

        LinkedHashMap<String, String> importSortLabels = new LinkedHashMap<>();
        importSortLabels.put("id", "ID");
        importSortLabels.put("supplier", "Supplier");
        importSortLabels.put("createdAt", "Date");
        importSortLabels.put("totalCost", "Total Cost");
        importSortLabels.put("status", "Status");
        Label importSortStatusLabel = context.support().createSortStatusLabel(importSortState, importSortLabels);
        Runnable applyImportSortUi = () -> {
            context.support().applySortStateToTable(table, importSortColumns, importSortState);
            importSortStatusLabel.setText(context.support().buildSortStatusText(importSortState, importSortLabels));
        };
        applyImportSortUi.run();
        context.support().installManualServerSorting(table, importSortColumns, importSortState, () -> {
            applyImportSortUi.run();
            currentPage[0] = 0;
            loadPage.run();
        });

        table.getSelectionModel().getSelectedItems().addListener((ListChangeListener<ImportOrder>) c -> {
            manageImportBtn.setDisable(table.getSelectionModel().getSelectedItems().size() != 1);
            updateStatusBar.run();
        });

        table.setRowFactory(tv -> {
            TableRow<ImportOrder> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY
                    && event.getClickCount() == 2
                    && !row.isEmpty()) {
                    openImportDetails(context, user, row.getItem().getId(), refreshAfterImportChange);
                }
            });
            return row;
        });

        PauseTransition searchPause = new PauseTransition(Duration.millis(220));
        searchPause.setOnFinished(e -> {
            currentPage[0] = 0;
            searchRef.set(searchField.getText());
            loadPage.run();
        });
        searchField.textProperty().addListener((obs, oldV, newV) -> searchPause.playFromStart());

        HBox statusBar = new HBox(15, importSortStatusLabel, rowCountLabel, pageLabel, prevBtn, nextBtn);
        context.support().applyStandardTableStatusBar(statusBar);

        Popup filterPopup = new Popup();
        filterPopup.setAutoHide(true);
        filterBox.setOnMouseClicked(event -> {
            event.consume();
            if (filterPopup.isShowing()) {
                filterPopup.hide();
                return;
            }
            try {
                showFilterPopup(
                    context,
                    user,
                    filterPopup,
                    filterBox,
                    filterOptionsCache,
                    startDateRef,
                    endDateRef,
                    suppliersRef,
                    statusesRef,
                    minTotalRef,
                    maxTotalRef,
                    currentPage,
                    loadPage
                );
            } catch (Exception ex) {
                context.showUserFacingError(ex);
            }
        });

        root.getChildren().addAll(toolbar, table, statusBar);
        VBox.setVgrow(table, Priority.ALWAYS);
        Platform.runLater(loadPage);
        TableViewSupport.enableDeselectOnOutsideClick(root, table);

        if (prefill != null) {
            Platform.runLater(() -> showCreateImportDialog(context, user, refreshAfterImportChange, prefill));
        }
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

    private static void showFilterPopup(
        SceneRuntimeContext context,
        User user,
        Popup popup,
        HBox filterBox,
        AtomicReference<ImportFilterOptions> filterOptionsCache,
        AtomicReference<LocalDate> startDateRef,
        AtomicReference<LocalDate> endDateRef,
        AtomicReference<Set<Long>> suppliersRef,
        AtomicReference<Set<ImportOrderStatus>> statusesRef,
        AtomicReference<BigDecimal> minTotalRef,
        AtomicReference<BigDecimal> maxTotalRef,
        int[] currentPage,
        Runnable loadPage
    ) {
        ImportFilterOptions filterOptions = filterOptionsCache.get();
        if (filterOptions == null) {
            popup.getContent().setAll(FilterControlFactory.loadingContainer(350, "Loading filters..."));
            context.support().showPopupBelow(popup, filterBox, -290, 5);
            javafx.concurrent.Task<ImportFilterOptions> task = new javafx.concurrent.Task<>() {
                @Override
                protected ImportFilterOptions call() {
                    return new ImportFilterOptions(
                        context.importOrderService().getImportSupplierOptions(user),
                        context.importOrderService().getImportMaxTotalCost(user)
                    );
                }
            };
            task.setOnSucceeded(event -> {
                filterOptionsCache.set(task.getValue());
                if (popup.isShowing()) {
                    showFilterPopup(
                        context,
                        user,
                        popup,
                        filterBox,
                        filterOptionsCache,
                        startDateRef,
                        endDateRef,
                        suppliersRef,
                        statusesRef,
                        minTotalRef,
                        maxTotalRef,
                        currentPage,
                        loadPage
                    );
                }
            });
            task.setOnFailed(event -> {
                popup.hide();
                context.showUserFacingError(task.getException());
            });
            Thread worker = new Thread(task, "import-filter-options-loader");
            worker.setDaemon(true);
            worker.start();
            return;
        }

        VBox popupContainer = new VBox(10);
        popupContainer.setPadding(new Insets(15));
        FilterControlFactory.applyContainerStyle(popupContainer);
        popupContainer.setPrefWidth(350);

        VBox scrollContent = new VBox(10);
        scrollContent.setStyle("-fx-background-color: -app-surface;");
        scrollContent.setPadding(new Insets(5, 15, 5, 15));
        ScrollPane scrollPane = new ScrollPane(scrollContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: -app-surface; -fx-background-color: transparent; -fx-border-color: transparent;");
        scrollPane.setPrefViewportHeight(350);

        Label dateTitle = FilterControlFactory.sectionTitle("Date Range");
        DatePicker startDatePicker = new DatePicker(startDateRef.get());
        startDatePicker.setPromptText("Start Date");
        startDatePicker.setPrefWidth(140);
        DatePicker endDatePicker = new DatePicker(endDateRef.get());
        endDatePicker.setPromptText("End Date");
        endDatePicker.setPrefWidth(140);
        context.support().customizeDatePicker(startDatePicker);
        context.support().customizeDatePicker(endDatePicker);
        HBox dateBox = new HBox(5, startDatePicker, new Label("-"), endDatePicker);
        dateBox.setAlignment(Pos.CENTER_LEFT);

        Label supplierTitle = FilterControlFactory.sectionTitle("Suppliers");
        CheckBox allSuppliersCb = new CheckBox("All Suppliers");
        allSuppliersCb.setSelected(suppliersRef.get().isEmpty());
        styleFilterCheckBox(allSuppliersCb);
        ScrollPane supplierScroll = new ScrollPane();
        VBox supplierBox = new VBox(8);
        supplierBox.setPadding(new Insets(5, 5, 5, 20));
        supplierScroll.setContent(supplierBox);
        supplierScroll.setFitToWidth(true);
        supplierScroll.setMaxHeight(140);
        supplierScroll.setStyle("-fx-background-color: transparent; -fx-background: -app-surface; -fx-border-color: -app-border; -fx-border-radius: 4;");

        List<CheckBox> supplierCbs = new ArrayList<>();
        for (IdLabelOption option : filterOptions.supplierOptions()) {
            if (option.label() == null || option.label().trim().isEmpty()) {
                continue;
            }
            CheckBox cb = new CheckBox(option.label());
            cb.setUserData(option.id());
            cb.setSelected(suppliersRef.get().isEmpty() || suppliersRef.get().contains(option.id()));
            styleFilterCheckBox(cb);
            cb.setOnAction(e -> allSuppliersCb.setSelected(supplierCbs.stream().allMatch(CheckBox::isSelected)));
            supplierCbs.add(cb);
            supplierBox.getChildren().add(cb);
        }
        allSuppliersCb.setOnAction(e -> supplierCbs.forEach(cb -> cb.setSelected(allSuppliersCb.isSelected())));
        FilterDisclosureSection supplierSection = new FilterDisclosureSection(allSuppliersCb, supplierScroll);

        Label statusTitle = FilterControlFactory.sectionTitle("Status");
        CheckBox allStatusesCb = new CheckBox("All Statuses");
        allStatusesCb.setSelected(statusesRef.get().isEmpty());
        styleFilterCheckBox(allStatusesCb);
        VBox statusBox = new VBox(8);
        statusBox.setPadding(new Insets(5, 5, 5, 20));
        List<CheckBox> statusCbs = new ArrayList<>();
        for (ImportOrderStatus status : ImportOrderStatus.values()) {
            CheckBox cb = new CheckBox(formatImportOrderStatus(status));
            cb.setUserData(status);
            cb.setSelected(statusesRef.get().isEmpty() || statusesRef.get().contains(status));
            styleFilterCheckBox(cb);
            cb.setOnAction(e -> allStatusesCb.setSelected(statusCbs.stream().allMatch(CheckBox::isSelected)));
            statusCbs.add(cb);
            statusBox.getChildren().add(cb);
        }
        allStatusesCb.setOnAction(e -> statusCbs.forEach(cb -> cb.setSelected(allStatusesCb.isSelected())));
        FilterDisclosureSection statusSection = new FilterDisclosureSection(allStatusesCb, statusBox);

        Label priceTitle = FilterControlFactory.sectionTitle("Total Cost Range");
        BigDecimal maxPriceValue = filterOptions.maxTotalCost();
        double maxPrice = maxPriceValue == null ? 0.0 : maxPriceValue.doubleValue();
        if (maxPrice == 0) {
            maxPrice = 1000;
        }
        double selectedMin = minTotalRef.get() == null ? 0 : minTotalRef.get().doubleValue();
        double selectedMax = maxTotalRef.get() == null ? maxPrice : maxTotalRef.get().doubleValue();
        Label priceLabel = new Label(String.format("%.0f - %.0f VND", selectedMin, selectedMax));
        priceLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -app-primary; -fx-font-weight: bold;");
        RangeSlider priceSlider = new RangeSlider(0, maxPrice, selectedMin, selectedMax, 290);
        priceSlider.minVal.addListener((o, ov, nv) -> priceLabel.setText(String.format("%.0f - %.0f VND", nv.doubleValue(), priceSlider.maxVal.get())));
        priceSlider.maxVal.addListener((o, ov, nv) -> priceLabel.setText(String.format("%.0f - %.0f VND", priceSlider.minVal.get(), nv.doubleValue())));
        final double finalMaxPrice = maxPrice;

        Button resetBtn = new Button("Reset");
        resetBtn.getStyleClass().add("filter-reset-btn");
        resetBtn.setOnAction(e -> {
            filterBox.setStyle("");
            startDateRef.set(null);
            endDateRef.set(null);
            suppliersRef.set(new LinkedHashSet<>());
            statusesRef.set(new LinkedHashSet<>());
            minTotalRef.set(null);
            maxTotalRef.set(null);
            currentPage[0] = 0;
            loadPage.run();
            popup.hide();
        });

        Button applyBtn = new Button("Apply Filter");
        applyBtn.getStyleClass().add("filter-apply-btn");
        applyBtn.setOnAction(e -> {
            Set<Long> selectedSuppliers = new HashSet<>();
            for (CheckBox cb : supplierCbs) {
                if (cb.isSelected() && cb.getUserData() instanceof Long supplierId) {
                    selectedSuppliers.add(supplierId);
                }
            }
            Set<ImportOrderStatus> selectedStatuses = new HashSet<>();
            for (CheckBox cb : statusCbs) {
                if (cb.isSelected()) {
                    selectedStatuses.add((ImportOrderStatus) cb.getUserData());
                }
            }
            double min = priceSlider.minVal.get();
            double max = priceSlider.maxVal.get();
            LocalDate start = startDatePicker.getValue();
            LocalDate end = endDatePicker.getValue();

            startDateRef.set(start);
            endDateRef.set(end);
            suppliersRef.set(allSuppliersCb.isSelected() ? new LinkedHashSet<>() : selectedSuppliers);
            statusesRef.set(allStatusesCb.isSelected() ? new LinkedHashSet<>() : selectedStatuses);
            minTotalRef.set(min <= 0 ? null : MoneySupport.normalize(BigDecimal.valueOf(min)));
            maxTotalRef.set(max >= finalMaxPrice ? null : MoneySupport.normalize(BigDecimal.valueOf(max)));
            currentPage[0] = 0;
            loadPage.run();

            boolean hasFilter = !allSuppliersCb.isSelected()
                || !allStatusesCb.isSelected()
                || min > 0
                || max < finalMaxPrice
                || start != null
                || end != null;
            filterBox.setStyle(hasFilter ? "-fx-border-color: -app-primary;" : "");
            popup.hide();
        });

        scrollContent.getChildren().addAll(
            dateTitle, dateBox, new Separator(),
            supplierTitle, supplierSection.getNode(), new Separator(),
            statusTitle, statusSection.getNode(), new Separator(),
            priceTitle, priceLabel, priceSlider
        );
        HBox actionRow = FilterControlFactory.actionRow(resetBtn, applyBtn);
        popupContainer.getChildren().addAll(scrollPane, actionRow);
        popup.getContent().clear();
        popup.getContent().add(popupContainer);
        context.support().showPopupBelow(popup, filterBox, -290, 5);
    }

    private static void styleFilterCheckBox(CheckBox checkBox) {
        checkBox.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
    }

    private static void openImportDetails(SceneRuntimeContext context, User user, Long importOrderId, Runnable onChanged) {
        if (importOrderId == null) {
            context.toastService().showWarning("Select an import order.");
            return;
        }
        javafx.stage.Stage loadingDialog = com.pbl3.project.pbl3_project.ui.util.DialogSupport.showLoadingWindow(
            context.owner(),
            MessageFormat.format("Import Order #{0}", importOrderId),
            "Loading import order details...",
            420,
            240
        );
        javafx.concurrent.Task<ImportOrder> task = new javafx.concurrent.Task<>() {
            @Override
            protected ImportOrder call() {
                return context.importOrderService().getImportOrderWithItems(importOrderId);
            }
        };
        task.setOnSucceeded(event -> {
            if (!loadingDialog.isShowing()) {
                return;
            }
            loadingDialog.close();
            ImportOrderDialog.showDetails(context.owner(), task.getValue(), user, onChanged, importDialogContext(context));
        });
        task.setOnFailed(event -> {
            loadingDialog.close();
            Throwable ex = task.getException();
            context.toastService().showError(MessageFormat.format(
                "Could not load import order details: {0}",
                ex != null && ex.getMessage() != null ? ex.getMessage() : "Unknown error"
            ));
        });
        Thread worker = new Thread(task, "import-details-dialog-loader");
        worker.setDaemon(true);
        worker.start();
    }

    private static void showCreateImportDialog(SceneRuntimeContext context, User user, Runnable onSuccess, ImportOrderPrefill prefill) {
        try {
            ImportOrderDialog.showCreate(
                context.owner(),
                user,
                onSuccess,
                prefill == null ? null : new ImportOrderDialog.Prefill(prefill.productId(), prefill.quantity()),
                importDialogContext(context)
            );
        } catch (Exception ex) {
            context.toastService().showError(MessageFormat.format("Could not open New Import: {0}", ex.getMessage()));
        }
    }

    private static ImportOrderDialog.Context importDialogContext(SceneRuntimeContext context) {
        return new ImportOrderDialog.Context(
            context.importOrderService(),
            context.supplierService(),
            context.categoryService(),
            context.productService(),
            context.toastService(),
            context::showUserFacingError
        );
    }

    private static String formatImportOrderStatus(ImportOrderStatus status) {
        ImportOrderStatus safeStatus = status != null ? status : ImportOrderStatus.COMPLETED;
        return switch (safeStatus) {
            case COMPLETED -> "Completed";
            case CANCELED -> "Canceled";
        };
    }

    private static String getImportOrderStatusColor(ImportOrderStatus status) {
        ImportOrderStatus safeStatus = status != null ? status : ImportOrderStatus.COMPLETED;
        return switch (safeStatus) {
            case COMPLETED -> "-app-success-hover";
            case CANCELED -> "-app-danger-hover";
        };
    }

    private static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : "-";
    }
}
