package com.pbl3.project.pbl3_project.ui.scene;

import com.pbl3.project.pbl3_project.entity.StocktakeScopeType;
import com.pbl3.project.pbl3_project.entity.StocktakeSession;
import com.pbl3.project.pbl3_project.entity.StocktakeSessionStatus;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.ui.component.ActionTaskbarFactory;
import com.pbl3.project.pbl3_project.ui.component.ButtonFactory;
import com.pbl3.project.pbl3_project.ui.component.ExpandableSearchControl;
import com.pbl3.project.pbl3_project.ui.component.FilterControlFactory;
import com.pbl3.project.pbl3_project.ui.dialog.StocktakeDialog;
import com.pbl3.project.pbl3_project.ui.util.AsyncPageCache;
import com.pbl3.project.pbl3_project.ui.util.AsyncUiTask;
import com.pbl3.project.pbl3_project.ui.util.DialogSupport;
import com.pbl3.project.pbl3_project.ui.util.FxFormatters;
import com.pbl3.project.pbl3_project.ui.util.SortCriterion;
import com.pbl3.project.pbl3_project.ui.util.TableSortState;
import com.pbl3.project.pbl3_project.ui.util.TableViewSupport;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
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

public final class StocktakeScene {
    private static final Color PRIMARY_COLOR = Color.web("#1d7df2");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public record Options() {
    }

    private record StocktakePageResult(Page<StocktakeSession> page, int pageIndex) {
    }

    private StocktakeScene() {
    }

    public static Node create(SceneRuntimeContext context, User user, Options options) {
        VBox root = new VBox();
        context.support().applyStandardTablePageLayout(root);

        TableSortState stocktakeSortState = context.support().getOrCreateTableSortState(
            "stocktake-sessions",
            new SortCriterion("createdAt", TableColumn.SortType.DESCENDING)
        );
        LinkedHashMap<String, String> stocktakeSortProperties = new LinkedHashMap<>();
        stocktakeSortProperties.put("createdAt", "createdAt");
        stocktakeSortProperties.put("status", "status");
        stocktakeSortProperties.put("scopeType", "scopeType");
        stocktakeSortProperties.put("createdBy", "createdBy.username");
        LinkedHashMap<String, String> stocktakeSortLabels = new LinkedHashMap<>();
        stocktakeSortLabels.put("createdAt", "Created At");
        stocktakeSortLabels.put("status", "Status");
        stocktakeSortLabels.put("scopeType", "Scope");
        stocktakeSortLabels.put("createdBy", "Created By");

        ExpandableSearchControl searchControl = ExpandableSearchControl.create(320, PRIMARY_COLOR);
        TextField searchField = searchControl.field();
        HBox filterBox = createFilterButton();

        Button newSessionButton = ButtonFactory.expandableGreenAction("New Stocktake Session", 160);
        Button openButton = ActionTaskbarFactory.createButton(ActionTaskbarFactory.icon("clipboard"), "Open Stocktake", "promotion-taskbar-button-view");
        Button applyButton = ActionTaskbarFactory.createButton(ActionTaskbarFactory.icon("check-circle"), "Apply Stocktake", "promotion-taskbar-button-toggle");
        Button cancelButton = ActionTaskbarFactory.createButton(ActionTaskbarFactory.icon("x-circle"), "Cancel Stocktake", "promotion-taskbar-button-delete");
        openButton.setDisable(true);
        applyButton.setDisable(true);
        cancelButton.setDisable(true);
        HBox stocktakeActionTaskbar = ActionTaskbarFactory.create(openButton, applyButton, cancelButton);

        HBox rightBox = new HBox(12, newSessionButton, filterBox, searchControl.box());
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        BorderPane topBar = new BorderPane();
        topBar.setLeft(stocktakeActionTaskbar);
        topBar.setRight(rightBox);

        TableView<StocktakeSession> table = new TableView<>();
        context.support().applyStandardTableSizing(table);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        TableViewSupport.enableDragSelection(table);

        TableColumn<StocktakeSession, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().getId()));

        TableColumn<StocktakeSession, String> createdAtCol = new TableColumn<>("Created At");
        createdAtCol.setCellValueFactory(data -> new SimpleStringProperty(formatDateTime(data.getValue().getCreatedAt())));

        TableColumn<StocktakeSession, String> scopeCol = new TableColumn<>("Scope");
        scopeCol.setCellValueFactory(data -> new SimpleStringProperty(formatStocktakeScopeLabel(data.getValue().getScopeType())));

        TableColumn<StocktakeSession, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getCategory() != null ? data.getValue().getCategory().getName() : "-"
        ));

        TableColumn<StocktakeSession, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(formatStocktakeStatusLabel(data.getValue().getStatus())));
        statusCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<StocktakeSession, String> createdByCol = new TableColumn<>("Created By");
        createdByCol.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getCreatedBy() != null ? data.getValue().getCreatedBy().getUsername() : "System"
        ));

        TableColumn<StocktakeSession, String> notesCol = new TableColumn<>("Notes");
        notesCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNotes() == null ? "" : data.getValue().getNotes()));

        table.getColumns().addAll(idCol, createdAtCol, scopeCol, categoryCol, statusCol, createdByCol, notesCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        LinkedHashMap<String, TableColumn<StocktakeSession, ?>> stocktakeSortColumns = new LinkedHashMap<>();
        stocktakeSortColumns.put("createdAt", createdAtCol);
        stocktakeSortColumns.put("status", statusCol);
        stocktakeSortColumns.put("scopeType", scopeCol);
        stocktakeSortColumns.put("createdBy", createdByCol);
        context.support().installSortHeaderIndicators(stocktakeSortColumns);

        final int pageSize = 20;
        final int[] currentPage = {0};
        final int[] totalPages = {0};
        final long[] totalElements = {0L};
        AtomicReference<String> searchRef = new AtomicReference<>("");
        AtomicReference<LocalDate> startDateRef = new AtomicReference<>(null);
        AtomicReference<LocalDate> endDateRef = new AtomicReference<>(null);
        AtomicReference<Set<StocktakeSessionStatus>> statusFiltersRef = new AtomicReference<>(new LinkedHashSet<>());
        AtomicReference<Set<StocktakeScopeType>> scopeFiltersRef = new AtomicReference<>(new LinkedHashSet<>());
        AtomicLong pageLoadVersion = new AtomicLong();
        AsyncPageCache<StocktakePageResult> pageCache = new AsyncPageCache<>(80);

        Label rowCountLabel = context.support().createStatusMetaLabel(MessageFormat.format("Showing {0}-{1} of {2} Row(s)", 0, 0, 0));
        Label pageLabel = context.support().createStatusMetaLabel(MessageFormat.format("Page {0} / {1}", 0, 0));
        Button prevBtn = ButtonFactory.pageNav("Prev");
        Button nextBtn = ButtonFactory.pageNav("Next");

        Runnable updateStocktakeActionState = () -> {
            List<StocktakeSession> selectedSessions = new ArrayList<>(table.getSelectionModel().getSelectedItems());
            boolean exactlyOneSelected = selectedSessions.size() == 1;
            StocktakeSession selectedSession = exactlyOneSelected ? selectedSessions.get(0) : null;
            boolean openSessionSelected = selectedSession != null && selectedSession.getStatus() == StocktakeSessionStatus.OPEN;
            openButton.setDisable(!exactlyOneSelected);
            applyButton.setDisable(!openSessionSelected);
            cancelButton.setDisable(!openSessionSelected);
        };
        table.getSelectionModel().getSelectedItems().addListener((ListChangeListener<StocktakeSession>) change -> updateStocktakeActionState.run());

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
            Set<StocktakeSessionStatus> statuses = new LinkedHashSet<>(statusFiltersRef.get());
            Set<StocktakeScopeType> scopes = new LinkedHashSet<>(scopeFiltersRef.get());
            List<SortCriterion> sortSnapshot = stocktakeSortState.snapshot();
            TableSortState sortForLoad = new TableSortState(sortSnapshot);
            java.util.function.IntFunction<Object> cacheKeyForPage = pageIndex -> java.util.Arrays.asList(
                pageIndex,
                search,
                startDate,
                endDate,
                statuses,
                scopes,
                sortSnapshot
            );
            java.util.function.IntFunction<StocktakePageResult> fetchStocktakePage = pageIndex -> {
                int resolvedPage = pageIndex;
                Page<StocktakeSession> pageData = context.stocktakeService().searchSessions(
                    user,
                    search,
                    startDate,
                    endDate,
                    statuses,
                    scopes,
                    context.support().createPageable(sortForLoad, stocktakeSortProperties, resolvedPage, pageSize)
                );
                if (pageData.getTotalPages() > 0 && resolvedPage >= pageData.getTotalPages()) {
                    resolvedPage = pageData.getTotalPages() - 1;
                    pageData = context.stocktakeService().searchSessions(
                        user,
                        search,
                        startDate,
                        endDate,
                        statuses,
                        scopes,
                        context.support().createPageable(sortForLoad, stocktakeSortProperties, resolvedPage, pageSize)
                    );
                }
                return new StocktakePageResult(pageData, resolvedPage);
            };

            AsyncUiTask.runLatestCachedTableLoad(
                table,
                prevBtn,
                nextBtn,
                pageLoadVersion,
                pageCache,
                cacheKeyForPage.apply(requestedPage),
                () -> fetchStocktakePage.apply(requestedPage),
                result -> {
                    Page<StocktakeSession> pageData = result.page();
                    currentPage[0] = result.pageIndex();
                    totalElements[0] = pageData.getTotalElements();
                    totalPages[0] = pageData.getTotalPages();
                    table.setItems(FXCollections.observableArrayList(pageData.getContent()));
                    updateStatusBar.run();
                    updateStocktakeActionState.run();
                    int nextPage = result.pageIndex() + 1;
                    if (nextPage < totalPages[0]) {
                        pageCache.prefetch(
                            cacheKeyForPage.apply(nextPage),
                            () -> fetchStocktakePage.apply(nextPage),
                            null,
                            "stocktake-next-page-prefetch"
                        );
                    }
                    int previousPage = result.pageIndex() - 1;
                    if (previousPage >= 0) {
                        pageCache.prefetch(
                            cacheKeyForPage.apply(previousPage),
                            () -> fetchStocktakePage.apply(previousPage),
                            null,
                            "stocktake-prev-page-prefetch"
                        );
                    }
                },
                context::showUserFacingError,
                "Loading stocktakes...",
                "Could not load stocktakes",
                "stocktake-page-loader"
            );
        };

        Label stocktakeSortStatusLabel = context.support().createSortStatusLabel(stocktakeSortState, stocktakeSortLabels);
        Runnable applyStocktakeSortUi = () -> {
            context.support().applySortStateToTable(table, stocktakeSortColumns, stocktakeSortState);
            stocktakeSortStatusLabel.setText(context.support().buildSortStatusText(stocktakeSortState, stocktakeSortLabels));
        };
        applyStocktakeSortUi.run();
        context.support().installManualServerSorting(table, stocktakeSortColumns, stocktakeSortState, () -> {
            applyStocktakeSortUi.run();
            currentPage[0] = 0;
            loadPage.run();
        });

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

        PauseTransition searchPause = new PauseTransition(Duration.millis(220));
        searchPause.setOnFinished(e -> {
            currentPage[0] = 0;
            searchRef.set(searchField.getText());
            loadPage.run();
        });
        searchField.textProperty().addListener((obs, oldV, newV) -> searchPause.playFromStart());

        HBox statusBar = new HBox(15, stocktakeSortStatusLabel, rowCountLabel, pageLabel, prevBtn, nextBtn);
        context.support().applyStandardTableStatusBar(statusBar);

        Runnable openSelectedSession = () -> {
            StocktakeSession session = table.getSelectionModel().getSelectedItem();
            if (session == null) {
                context.toastService().showWarning("Select a stocktake session");
                return;
            }
            showStocktakeSessionDialog(context, user, session.getId(), loadPage);
        };
        Runnable refreshAfterStocktakeChange = () -> {
            pageCache.clear();
            loadPage.run();
        };

        newSessionButton.setOnAction(e -> showCreateStocktakeDialog(context, user, created -> {
            refreshAfterStocktakeChange.run();
            showStocktakeSessionDialog(context, user, created.getId(), refreshAfterStocktakeChange);
        }));
        openButton.setOnAction(e -> openSelectedSession.run());
        applyButton.setOnAction(e -> {
            StocktakeSession session = table.getSelectionModel().getSelectedItem();
            if (session == null) {
                context.toastService().showWarning("Select a stocktake session");
                return;
            }
            if (session.getStatus() != StocktakeSessionStatus.OPEN) {
                context.toastService().showWarning("Only open stocktake sessions can be applied.");
                return;
            }
            int itemCount = session.getItems() == null ? 0 : session.getItems().size();
            boolean largeApply = session.getScopeType() == StocktakeScopeType.ALL_PRODUCTS || itemCount >= 5;
            boolean confirmed = largeApply
                ? DialogSupport.showTypedDangerConfirm(
                    context.owner(),
                    "Apply Stocktake",
                    MessageFormat.format("Apply stocktake #{0}? This can update many product quantities.", session.getId()),
                    "APPLY"
                )
                : context.support().showConfirmDialog("Apply Stocktake", MessageFormat.format("Apply stocktake #{0}?", session.getId()));
            if (!confirmed) {
                return;
            }
            try {
                context.stocktakeService().applySession(user, session.getId());
                context.toastService().showSuccess("Stocktake applied.");
                refreshAfterStocktakeChange.run();
            } catch (Exception ex) {
                context.showUserFacingError(ex);
            }
        });
        cancelButton.setOnAction(e -> {
            StocktakeSession session = table.getSelectionModel().getSelectedItem();
            if (session == null) {
                context.toastService().showWarning("Select a stocktake session");
                return;
            }
            if (session.getStatus() != StocktakeSessionStatus.OPEN) {
                context.toastService().showWarning("Only open stocktake sessions can be canceled.");
                return;
            }
            Optional<String> result = DialogSupport.promptText(
                context.owner(),
                "Cancel Stocktake",
                MessageFormat.format("Cancel stocktake #{0}", session.getId()),
                "Notes:"
            );
            if (result.isEmpty()) {
                return;
            }
            try {
                context.stocktakeService().cancelSession(user, session.getId(), result.get());
                context.toastService().showSuccess("Stocktake canceled.");
                refreshAfterStocktakeChange.run();
            } catch (Exception ex) {
                context.showUserFacingError(ex);
            }
        });

        table.setRowFactory(tv -> {
            TableRow<StocktakeSession> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY
                    && event.getClickCount() == 2
                    && !row.isEmpty()) {
                    showStocktakeSessionDialog(context, user, row.getItem().getId(), refreshAfterStocktakeChange);
                }
            });
            return row;
        });

        Popup filterPopup = new Popup();
        filterPopup.setAutoHide(true);
        filterBox.setOnMouseClicked(event -> {
            if (filterPopup.isShowing()) {
                filterPopup.hide();
                return;
            }
            showFilterPopup(
                context,
                filterPopup,
                filterBox,
                startDateRef,
                endDateRef,
                statusFiltersRef,
                scopeFiltersRef,
                currentPage,
                loadPage
            );
        });

        root.getChildren().addAll(topBar, table, statusBar);
        VBox.setVgrow(table, Priority.ALWAYS);
        javafx.application.Platform.runLater(loadPage);
        TableViewSupport.enableDeselectOnOutsideClick(root, table);
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
        Popup filterPopup,
        HBox filterBox,
        AtomicReference<LocalDate> startDateRef,
        AtomicReference<LocalDate> endDateRef,
        AtomicReference<Set<StocktakeSessionStatus>> statusFiltersRef,
        AtomicReference<Set<StocktakeScopeType>> scopeFiltersRef,
        int[] currentPage,
        Runnable loadPage
    ) {
        FilterControlFactory.Shell filterShell = FilterControlFactory.shell(340, 240);
        VBox popupContainer = filterShell.container();
        VBox scrollContent = filterShell.content();

        Label dateTitle = FilterControlFactory.sectionTitle("Date Range");
        DatePicker startDatePicker = new DatePicker(startDateRef.get());
        startDatePicker.setPromptText("Start Date");
        DatePicker endDatePicker = new DatePicker(endDateRef.get());
        endDatePicker.setPromptText("End Date");
        context.support().customizeDatePicker(startDatePicker);
        context.support().customizeDatePicker(endDatePicker);
        HBox dateBox = new HBox(5, startDatePicker, new Label("-"), endDatePicker);
        dateBox.setAlignment(Pos.CENTER_LEFT);

        Label statusTitle = FilterControlFactory.sectionTitle("Status");
        ComboBox<String> statusCombo = new ComboBox<>();
        Map<String, StocktakeSessionStatus> statusLookup = new LinkedHashMap<>();
        statusCombo.getItems().add("All Statuses");
        for (StocktakeSessionStatus status : StocktakeSessionStatus.values()) {
            String label = formatStocktakeStatusLabel(status);
            statusLookup.put(label, status);
            statusCombo.getItems().add(label);
        }
        statusCombo.setValue(statusFiltersRef.get().isEmpty() ? "All Statuses" : formatStocktakeStatusLabel(statusFiltersRef.get().iterator().next()));
        statusCombo.setMaxWidth(Double.MAX_VALUE);

        Label scopeTitle = FilterControlFactory.sectionTitle("Scope");
        ComboBox<String> scopeCombo = new ComboBox<>();
        Map<String, StocktakeScopeType> scopeLookup = new LinkedHashMap<>();
        scopeCombo.getItems().add("All Scopes");
        for (StocktakeScopeType scope : StocktakeScopeType.values()) {
            String label = formatStocktakeScopeLabel(scope);
            scopeLookup.put(label, scope);
            scopeCombo.getItems().add(label);
        }
        scopeCombo.setValue(scopeFiltersRef.get().isEmpty() ? "All Scopes" : formatStocktakeScopeLabel(scopeFiltersRef.get().iterator().next()));
        scopeCombo.setMaxWidth(Double.MAX_VALUE);

        Button resetBtn = new Button("Reset");
        resetBtn.getStyleClass().add("filter-reset-btn");
        resetBtn.setOnAction(e -> {
            filterBox.setStyle("");
            startDateRef.set(null);
            endDateRef.set(null);
            statusFiltersRef.set(new LinkedHashSet<>());
            scopeFiltersRef.set(new LinkedHashSet<>());
            currentPage[0] = 0;
            loadPage.run();
            filterPopup.hide();
        });

        Button applyBtn = new Button("Apply Filter");
        applyBtn.getStyleClass().add("filter-apply-btn");
        applyBtn.setOnAction(e -> {
            startDateRef.set(startDatePicker.getValue());
            endDateRef.set(endDatePicker.getValue());

            Set<StocktakeSessionStatus> statusFilters = new LinkedHashSet<>();
            if (!"All Statuses".equals(statusCombo.getValue())) {
                statusFilters.add(statusLookup.get(statusCombo.getValue()));
            }
            statusFiltersRef.set(statusFilters);

            Set<StocktakeScopeType> scopeFilters = new LinkedHashSet<>();
            if (!"All Scopes".equals(scopeCombo.getValue())) {
                scopeFilters.add(scopeLookup.get(scopeCombo.getValue()));
            }
            scopeFiltersRef.set(scopeFilters);

            currentPage[0] = 0;
            loadPage.run();
            boolean hasFilter = startDateRef.get() != null
                || endDateRef.get() != null
                || !statusFiltersRef.get().isEmpty()
                || !scopeFiltersRef.get().isEmpty();
            filterBox.setStyle(hasFilter ? "-fx-border-color: -app-primary;" : "");
            filterPopup.hide();
        });

        scrollContent.getChildren().addAll(
            dateTitle, dateBox,
            new javafx.scene.control.Separator(),
            statusTitle, statusCombo,
            scopeTitle, scopeCombo,
            new javafx.scene.control.Separator()
        );
        popupContainer.getChildren().add(FilterControlFactory.actionRow(resetBtn, applyBtn));
        filterPopup.getContent().clear();
        filterPopup.getContent().add(popupContainer);
        context.support().showPopupBelow(filterPopup, filterBox, -260, 5);
    }

    private static void showCreateStocktakeDialog(SceneRuntimeContext context, User user, java.util.function.Consumer<StocktakeSession> onSuccess) {
        StocktakeDialog.showCreate(context.owner(), user, onSuccess, stocktakeDialogContext(context));
    }

    private static void showStocktakeSessionDialog(SceneRuntimeContext context, User user, Long sessionId, Runnable onChanged) {
        StocktakeDialog.showManage(context.owner(), user, sessionId, onChanged, stocktakeDialogContext(context));
    }

    private static StocktakeDialog.Context stocktakeDialogContext(SceneRuntimeContext context) {
        return new StocktakeDialog.Context(
            context.stocktakeService(),
            context.categoryService(),
            context.toastService(),
            context::showUserFacingError
        );
    }

    private static String formatStocktakeScopeLabel(StocktakeScopeType scopeType) {
        return FxFormatters.enumText(scopeType);
    }

    private static String formatStocktakeStatusLabel(StocktakeSessionStatus status) {
        return FxFormatters.enumText(status);
    }

    private static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : "-";
    }
}
