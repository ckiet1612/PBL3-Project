package com.pbl3.project.pbl3_project.ui.scene;

import com.pbl3.project.pbl3_project.service.MoneySupport;
import com.pbl3.project.pbl3_project.ui.component.ButtonFactory;
import com.pbl3.project.pbl3_project.ui.component.FilterControlFactory;
import com.pbl3.project.pbl3_project.ui.component.FilterDisclosureSection;
import com.pbl3.project.pbl3_project.ui.component.RangeSlider;
import com.pbl3.project.pbl3_project.ui.util.AsyncPageCache;
import com.pbl3.project.pbl3_project.ui.util.AsyncUiTask;
import com.pbl3.project.pbl3_project.ui.util.SortCriterion;
import com.pbl3.project.pbl3_project.ui.util.TableSortState;
import com.pbl3.project.pbl3_project.ui.util.TableViewSupport;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.springframework.data.domain.Pageable;

public final class ReturnsRefundsScene {
    private static final Color PRIMARY_COLOR = Color.web("#1d7df2");

    public record Options() {
    }

    private record ReturnFilterOptions(
        java.util.List<com.pbl3.project.pbl3_project.dto.IdLabelOption> creatorOptions,
        BigDecimal maxTotal
    ) {
    }

    private record ReturnPageResult(
        org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.Order> page,
        int pageIndex
    ) {
    }

    private final SceneRuntimeContext context;
    private final com.pbl3.project.pbl3_project.entity.User user;

    private ReturnsRefundsScene(SceneRuntimeContext context, com.pbl3.project.pbl3_project.entity.User user) {
        this.context = context;
        this.user = user;
    }

    public static Node create(SceneRuntimeContext context, com.pbl3.project.pbl3_project.entity.User user, Options options) {
        return new ReturnsRefundsScene(context, user).createReturnsRefundsView();
    }

    private VBox createReturnsRefundsView() {
        final String returnSortStateKey = "returns-refunds";
        TableSortState returnSortState = getOrCreateTableSortState(
            returnSortStateKey,
            new SortCriterion("createdAt", javafx.scene.control.TableColumn.SortType.DESCENDING)
        );
        java.util.LinkedHashMap<String, String> returnSortProperties = new java.util.LinkedHashMap<>();
        returnSortProperties.put("id", "id");
        returnSortProperties.put("createdAt", "createdAt");
        returnSortProperties.put("customerName", "customerNameSnapshot");
        returnSortProperties.put("userFullName", "createdByNameSnapshot");
        returnSortProperties.put("status", "status");
        returnSortProperties.put("refundedAmount", "refundedAmount");
        returnSortProperties.put("grossTotal", "grossSubtotal");

        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.Order> table = new javafx.scene.control.TableView<>();
        applyStandardTableSizing(table);
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        TableViewSupport.enableDragSelection(table);

        final int returnPageSize = 20;
        final int[] returnCurrentPage = {0};
        final int[] returnTotalPages = {0};
        final long[] returnTotalElements = {0L};
        java.util.concurrent.atomic.AtomicReference<com.pbl3.project.pbl3_project.entity.ReturnRefundScope> scopeRef =
            new java.util.concurrent.atomic.AtomicReference<>(com.pbl3.project.pbl3_project.entity.ReturnRefundScope.PROCESSED);
        java.util.concurrent.atomic.AtomicReference<String> returnSearchRef = new java.util.concurrent.atomic.AtomicReference<>("");
        java.util.concurrent.atomic.AtomicReference<java.time.LocalDate> returnStartDateRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<java.time.LocalDate> returnEndDateRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<java.util.Set<Long>> returnUsersRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<java.util.Set<com.pbl3.project.pbl3_project.entity.PaymentMethod>> returnMethodsRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<java.util.Set<com.pbl3.project.pbl3_project.entity.OrderStatus>> returnStatusesRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<BigDecimal> returnMinTotalRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<BigDecimal> returnMaxTotalRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<ReturnFilterOptions> returnFilterOptionsCache = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicLong returnPageLoadVersion = new java.util.concurrent.atomic.AtomicLong();
        AsyncPageCache<ReturnPageResult> returnPageCache = new AsyncPageCache<>(80);

        Label returnRowCountLabel = createStatusMetaLabel(java.text.MessageFormat.format("Showing {0}-{1} of {2} Row(s)", 0, 0, 0));
        Label returnPageLabel = createStatusMetaLabel(java.text.MessageFormat.format("Page {0} / {1}", 0, 0));
        Button returnPrevBtn = ButtonFactory.pageNav("Prev");
        Button returnNextBtn = ButtonFactory.pageNav("Next");

        Runnable[] refreshReturnTableRef = new Runnable[1];
        Runnable updateReturnStatusBar = () -> updatePagedStatus(
            table,
            returnRowCountLabel,
            returnPageLabel,
            returnPrevBtn,
            returnNextBtn,
            returnTotalElements[0],
            returnCurrentPage[0],
            returnTotalPages[0],
            returnPageSize
        );
        Runnable loadReturnPage = () -> {
            int requestedPage = returnCurrentPage[0];
            com.pbl3.project.pbl3_project.entity.ReturnRefundScope scope = scopeRef.get();
            String search = returnSearchRef.get();
            java.time.LocalDate startDate = returnStartDateRef.get();
            java.time.LocalDate endDate = returnEndDateRef.get();
            java.util.Set<Long> users = new java.util.LinkedHashSet<>(returnUsersRef.get());
            java.util.Set<com.pbl3.project.pbl3_project.entity.PaymentMethod> methods =
                new java.util.LinkedHashSet<>(returnMethodsRef.get());
            java.util.Set<com.pbl3.project.pbl3_project.entity.OrderStatus> statuses =
                new java.util.LinkedHashSet<>(returnStatusesRef.get());
            BigDecimal minTotal = returnMinTotalRef.get();
            BigDecimal maxTotal = returnMaxTotalRef.get();
            java.util.List<SortCriterion> sortSnapshot = returnSortState.snapshot();
            TableSortState sortForLoad = new TableSortState(sortSnapshot);
            java.util.function.IntFunction<Object> cacheKeyForPage = pageIndex -> java.util.Arrays.asList(
                pageIndex,
                scope,
                search,
                startDate,
                endDate,
                users,
                methods,
                statuses,
                minTotal,
                maxTotal,
                sortSnapshot
            );
            java.util.function.IntFunction<ReturnPageResult> fetchReturnPage = pageIndex -> {
                int resolvedPage = pageIndex;
                org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.Order> pageData = context.orderService().searchReturnRefundOrders(
                    user,
                    scope,
                    search,
                    startDate,
                    endDate,
                    users,
                    methods,
                    statuses,
                    minTotal,
                    maxTotal,
                    createPageable(sortForLoad, returnSortProperties, resolvedPage, returnPageSize)
                );
                if (pageData.getTotalPages() > 0 && resolvedPage >= pageData.getTotalPages()) {
                    resolvedPage = pageData.getTotalPages() - 1;
                    pageData = context.orderService().searchReturnRefundOrders(
                        user,
                        scope,
                        search,
                        startDate,
                        endDate,
                        users,
                        methods,
                        statuses,
                        minTotal,
                        maxTotal,
                        createPageable(sortForLoad, returnSortProperties, resolvedPage, returnPageSize)
                    );
                }
                return new ReturnPageResult(pageData, resolvedPage);
            };

            AsyncUiTask.runLatestCachedTableLoad(
                table,
                returnPrevBtn,
                returnNextBtn,
                returnPageLoadVersion,
                returnPageCache,
                cacheKeyForPage.apply(requestedPage),
                () -> fetchReturnPage.apply(requestedPage),
                result -> {
                    org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.Order> pageData = result.page();
                    returnCurrentPage[0] = result.pageIndex();
                    returnTotalElements[0] = pageData.getTotalElements();
                    returnTotalPages[0] = pageData.getTotalPages();
                    table.setItems(javafx.collections.FXCollections.observableArrayList(pageData.getContent()));
                    updateReturnStatusBar.run();
                    int nextPage = result.pageIndex() + 1;
                    if (nextPage < returnTotalPages[0]) {
                        returnPageCache.prefetch(
                            cacheKeyForPage.apply(nextPage),
                            () -> fetchReturnPage.apply(nextPage),
                            null,
                            "returns-next-page-prefetch"
                        );
                    }
                    int previousPage = result.pageIndex() - 1;
                    if (previousPage >= 0) {
                        returnPageCache.prefetch(
                            cacheKeyForPage.apply(previousPage),
                            () -> fetchReturnPage.apply(previousPage),
                            null,
                            "returns-prev-page-prefetch"
                        );
                    }
                },
                context::showUserFacingError,
                "Loading returns...",
                "Could not load returns",
                "returns-refunds-page-loader"
            );
        };
        refreshReturnTableRef[0] = () -> {
            returnPageCache.clear();
            loadReturnPage.run();
        };
        returnPrevBtn.setOnAction(e -> {
            if (returnCurrentPage[0] > 0) {
                returnCurrentPage[0]--;
                loadReturnPage.run();
            }
        });
        returnNextBtn.setOnAction(e -> {
            if (returnCurrentPage[0] + 1 < returnTotalPages[0]) {
                returnCurrentPage[0]++;
                loadReturnPage.run();
            }
        });

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, Long> idCol = new javafx.scene.control.TableColumn<>("ID");
        idCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, String> dateCol = new javafx.scene.control.TableColumn<>("Created At");
        dateCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(context.support().formatDateTime(cell.getValue().getCreatedAt())));
        dateCol.setPrefWidth(185);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, String> customerCol = new javafx.scene.control.TableColumn<>("Customer");
        customerCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getCustomerDisplayName()));
        customerCol.setPrefWidth(170);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, String> userCol = new javafx.scene.control.TableColumn<>("Created By");
        userCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getCreatedByDisplayName()));
        userCol.setPrefWidth(170);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, String> statusCol = new javafx.scene.control.TableColumn<>("Status");
        statusCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(context.support().formatOrderStatus(cell.getValue().getStatus())));
        statusCol.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                String textColor;
                if (context.support().formatOrderStatus(com.pbl3.project.pbl3_project.entity.OrderStatus.CANCELED).equals(item)) {
                    textColor = "-app-danger-hover";
                } else if (context.support().formatOrderStatus(com.pbl3.project.pbl3_project.entity.OrderStatus.RETURNED).equals(item)
                    || context.support().formatOrderStatus(com.pbl3.project.pbl3_project.entity.OrderStatus.PARTIALLY_RETURNED).equals(item)) {
                    textColor = "-app-primary-hover";
                } else {
                    textColor = "-app-success-hover";
                }
                setStyle("-fx-text-fill: " + textColor + "; -fx-font-weight: bold; -fx-alignment: CENTER;");
            }
        });
        statusCol.setPrefWidth(150);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, String> refundedCol = new javafx.scene.control.TableColumn<>("Refunded Amount");
        refundedCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(context.support().formatVnd(cell.getValue().getRefundedAmount())));
        refundedCol.setPrefWidth(150);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, String> grossTotalCol = new javafx.scene.control.TableColumn<>("Gross Total");
        grossTotalCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(context.support().formatVnd(cell.getValue().getGrossSubtotalSnapshot())));
        grossTotalCol.setPrefWidth(145);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, String> netTotalCol = new javafx.scene.control.TableColumn<>("Net Paid");
        netTotalCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(context.support().formatVnd(cell.getValue().getTotalPrice())));
        netTotalCol.setPrefWidth(145);

        table.getColumns().addAll(idCol, dateCol, customerCol, userCol, statusCol, refundedCol, grossTotalCol, netTotalCol);
        java.util.LinkedHashMap<String, javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, ?>> returnSortColumns =
            new java.util.LinkedHashMap<>();
        returnSortColumns.put("id", idCol);
        returnSortColumns.put("createdAt", dateCol);
        returnSortColumns.put("customerName", customerCol);
        returnSortColumns.put("userFullName", userCol);
        returnSortColumns.put("status", statusCol);
        returnSortColumns.put("refundedAmount", refundedCol);
        returnSortColumns.put("grossTotal", grossTotalCol);
        installSortHeaderIndicators(returnSortColumns);
        java.util.LinkedHashMap<String, String> returnSortLabels = new java.util.LinkedHashMap<>();
        returnSortLabels.put("id", "ID");
        returnSortLabels.put("createdAt", "Created At");
        returnSortLabels.put("customerName", "Customer");
        returnSortLabels.put("userFullName", "Created By");
        returnSortLabels.put("status", "Status");
        returnSortLabels.put("refundedAmount", "Refunded Amount");
        returnSortLabels.put("grossTotal", "Gross Total");
        Label returnSortStatusLabel = createSortStatusLabel(returnSortState, returnSortLabels);
        Runnable applyReturnSortUi = () -> {
            applySortStateToTable(table, returnSortColumns, returnSortState);
            returnSortStatusLabel.setText(buildSortStatusText(returnSortState, returnSortLabels));
        };
        applyReturnSortUi.run();
        installManualServerSorting(
            table,
            returnSortColumns,
            returnSortState,
            () -> {
                applyReturnSortUi.run();
                returnCurrentPage[0] = 0;
                loadReturnPage.run();
            }
        );

        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<com.pbl3.project.pbl3_project.entity.Order> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY
                    && event.getClickCount() == 2
                    && !row.isEmpty()) {
                    showReturnOrderDetailsAsync(row.getItem().getId(), refreshReturnTableRef[0]);
                }
            });
            return row;
        });

        javafx.scene.layout.HBox returnSearchBox = new javafx.scene.layout.HBox(0);
        returnSearchBox.setAlignment(Pos.CENTER);
        returnSearchBox.getStyleClass().add("expandable-search-box");
        returnSearchBox.setPrefSize(40, 40);
        returnSearchBox.setMinSize(40, 40);
        returnSearchBox.setMaxSize(40, 40);
        javafx.scene.shape.SVGPath returnSearchIcon = new javafx.scene.shape.SVGPath();
        returnSearchIcon.setContent("M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
        returnSearchIcon.setFill(PRIMARY_COLOR);
        javafx.scene.layout.Region returnSpacer = new javafx.scene.layout.Region();
        returnSpacer.setMinWidth(0);
        returnSpacer.setPrefWidth(0);
        TextField returnField = new TextField();
        returnField.setPromptText("Search");
        returnField.getStyleClass().add("search-text-field");
        returnField.setMinWidth(0);
        returnField.setMaxWidth(0);
        returnField.setPrefWidth(0);
        returnField.setOpacity(0);
        returnSearchBox.getChildren().addAll(returnSearchIcon, returnSpacer, returnField);
        javafx.animation.Timeline returnExpand = new javafx.animation.Timeline(new javafx.animation.KeyFrame(Duration.millis(150),
            new javafx.animation.KeyValue(returnSearchBox.maxWidthProperty(), 250, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(returnSearchBox.prefWidthProperty(), 250, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(returnSpacer.minWidthProperty(), 8, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(returnField.minWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(returnField.maxWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(returnField.prefWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(returnField.opacityProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH)));
        javafx.animation.Timeline returnCollapse = new javafx.animation.Timeline(new javafx.animation.KeyFrame(Duration.millis(150),
            new javafx.animation.KeyValue(returnSearchBox.maxWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(returnSearchBox.prefWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(returnSpacer.minWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(returnField.minWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(returnField.maxWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(returnField.prefWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(returnField.opacityProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH)));

        javafx.animation.PauseTransition returnSearchPause = new javafx.animation.PauseTransition(Duration.millis(220));
        returnSearchPause.setOnFinished(e -> {
            returnCurrentPage[0] = 0;
            returnSearchRef.set(returnField.getText());
            loadReturnPage.run();
        });
        returnField.textProperty().addListener((obs, oldV, newV) -> returnSearchPause.playFromStart());

        VBox content = new VBox();
        applyStandardTablePageLayout(content);

        returnSearchBox.setOnMouseClicked(ev -> {
            if (returnSearchBox.getMaxWidth() == 40) {
                returnExpand.play();
                returnField.requestFocus();
            } else if (ev.getTarget() == returnSearchIcon || ev.getTarget() == returnSearchBox) {
                returnField.clear();
                content.requestFocus();
                returnCollapse.play();
            }
        });

        javafx.scene.layout.HBox returnFilterBox = new javafx.scene.layout.HBox();
        returnFilterBox.setAlignment(Pos.CENTER);
        returnFilterBox.getStyleClass().add("expandable-search-box");
        returnFilterBox.setPrefSize(40, 40);
        returnFilterBox.setMinSize(40, 40);
        returnFilterBox.setMaxSize(40, 40);
        returnFilterBox.setCursor(javafx.scene.Cursor.HAND);
        javafx.scene.shape.SVGPath returnFilterIcon = new javafx.scene.shape.SVGPath();
        returnFilterIcon.setContent("M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z");
        returnFilterIcon.setFill(PRIMARY_COLOR);
        returnFilterBox.getChildren().add(returnFilterIcon);
        javafx.scene.control.Tooltip.install(returnFilterBox, new javafx.scene.control.Tooltip("Filter"));

        Runnable updateReturnFilterAccent = () -> {
            boolean hasFilter = returnStartDateRef.get() != null
                || returnEndDateRef.get() != null
                || !returnUsersRef.get().isEmpty()
                || !returnMethodsRef.get().isEmpty()
                || !returnStatusesRef.get().isEmpty()
                || returnMinTotalRef.get() != null
                || returnMaxTotalRef.get() != null;
            returnFilterBox.setStyle(hasFilter ? "-fx-border-color: -app-primary;" : "");
        };

        javafx.scene.control.ComboBox<com.pbl3.project.pbl3_project.entity.ReturnRefundScope> scopeCombo = new javafx.scene.control.ComboBox<>();
        scopeCombo.getItems().addAll(com.pbl3.project.pbl3_project.entity.ReturnRefundScope.values());
        scopeCombo.setValue(scopeRef.get());
        scopeCombo.getStyleClass().add("return-scope-combo");
        scopeCombo.setPrefWidth(220);
        scopeCombo.setMinWidth(220);
        scopeCombo.setMaxWidth(220);
        javafx.util.StringConverter<com.pbl3.project.pbl3_project.entity.ReturnRefundScope> scopeConverter = new javafx.util.StringConverter<>() {
            @Override
            public String toString(com.pbl3.project.pbl3_project.entity.ReturnRefundScope scope) {
                return scope != null ? scope.getLabel() : "";
            }

            @Override
            public com.pbl3.project.pbl3_project.entity.ReturnRefundScope fromString(String string) {
                return java.util.Arrays.stream(com.pbl3.project.pbl3_project.entity.ReturnRefundScope.values())
                    .filter(scope -> scope.getLabel().equalsIgnoreCase(string))
                    .findFirst()
                    .orElse(com.pbl3.project.pbl3_project.entity.ReturnRefundScope.PROCESSED);
            }
        };
        scopeCombo.setConverter(scopeConverter);
        scopeCombo.setCellFactory(listView -> new javafx.scene.control.ListCell<>() {
            {
                getStyleClass().add("return-scope-popup-cell");
            }

            @Override
            protected void updateItem(com.pbl3.project.pbl3_project.entity.ReturnRefundScope item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : scopeConverter.toString(item));
            }
        });
        scopeCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null || newValue == oldValue) {
                return;
            }
            scopeRef.set(newValue);
            returnStatusesRef.set(new java.util.LinkedHashSet<>());
            returnCurrentPage[0] = 0;
            updateReturnFilterAccent.run();
            loadReturnPage.run();
        });
        Label scopeLabel = new Label("Scope");
        scopeLabel.getStyleClass().add("return-scope-label");
        javafx.scene.layout.HBox scopeBox = new javafx.scene.layout.HBox(10, scopeLabel, scopeCombo);
        scopeBox.getStyleClass().add("return-scope-control");
        scopeBox.setAlignment(Pos.CENTER_LEFT);

        javafx.stage.Popup returnFilterPopup = new javafx.stage.Popup();
        returnFilterPopup.setAutoHide(true);

        returnFilterBox.setOnMouseClicked(fev -> {
            fev.consume();
            try {
                if (returnFilterPopup.isShowing()) {
                    returnFilterPopup.hide();
                    return;
                }

                java.util.function.Consumer<ReturnFilterOptions> showFilterContent = filterOptions -> {
                VBox popupContainer = new VBox(10);
                popupContainer.setPadding(new Insets(15));
                FilterControlFactory.applyContainerStyle(popupContainer);
                popupContainer.setPrefWidth(350);

                VBox scrollContent = new VBox(10);
                scrollContent.setStyle("-fx-background-color: -app-surface;");
                scrollContent.setPadding(new Insets(5, 15, 5, 15));
                javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(scrollContent);
                scrollPane.setFitToWidth(true);
                scrollPane.setStyle("-fx-background: -app-surface; -fx-background-color: transparent; -fx-border-color: transparent;");
                scrollPane.setPrefViewportHeight(350);

                Label dateTitle = new Label("Date Range");
                dateTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");
                javafx.scene.control.DatePicker startDatePicker = new javafx.scene.control.DatePicker(returnStartDateRef.get());
                startDatePicker.setPromptText("Start Date");
                startDatePicker.setPrefWidth(140);
                startDatePicker.setStyle("-fx-font-size: 13px;");
                javafx.scene.control.DatePicker endDatePicker = new javafx.scene.control.DatePicker(returnEndDateRef.get());
                endDatePicker.setPromptText("End Date");
                endDatePicker.setPrefWidth(140);
                endDatePicker.setStyle("-fx-font-size: 13px;");
                context.support().customizeDatePicker(startDatePicker);
                context.support().customizeDatePicker(endDatePicker);
                javafx.scene.layout.HBox dateBox = new javafx.scene.layout.HBox(5, startDatePicker, new Label("-"), endDatePicker);
                dateBox.setAlignment(Pos.CENTER_LEFT);

                javafx.scene.control.Separator sepDate = new javafx.scene.control.Separator();

                Label userTitle = new Label("Created By");
                userTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");

                javafx.scene.control.CheckBox allUsersCb = new javafx.scene.control.CheckBox("All Users");
                allUsersCb.setSelected(returnUsersRef.get().isEmpty());
                allUsersCb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");

                VBox userScroll = new VBox(8);
                userScroll.setPadding(new Insets(5, 5, 5, 20));

                java.util.List<javafx.scene.control.CheckBox> userCbs = new java.util.ArrayList<>();
                java.util.List<com.pbl3.project.pbl3_project.dto.IdLabelOption> userOptions = filterOptions.creatorOptions();
                java.util.Set<Long> activeUserFilters = returnUsersRef.get();
                for (com.pbl3.project.pbl3_project.dto.IdLabelOption option : userOptions) {
                    if (option.label() == null || option.label().trim().isEmpty()) {
                        continue;
                    }
                    javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(option.label());
                    cb.setUserData(option.id());
                    cb.setSelected(activeUserFilters.isEmpty() || activeUserFilters.contains(option.id()));
                    cb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                    cb.setOnAction(e -> {
                        if (!cb.isSelected()) {
                            allUsersCb.setSelected(false);
                        } else {
                            boolean all = true;
                            for (javafx.scene.control.CheckBox c : userCbs) {
                                if (!c.isSelected()) {
                                    all = false;
                                }
                            }
                            allUsersCb.setSelected(all);
                        }
                    });
                    userCbs.add(cb);
                    userScroll.getChildren().add(cb);
                }
                allUsersCb.setOnAction(e -> {
                    boolean sel = allUsersCb.isSelected();
                    for (javafx.scene.control.CheckBox cb : userCbs) {
                        cb.setSelected(sel);
                    }
                });
                FilterDisclosureSection userSection = new FilterDisclosureSection(allUsersCb, userScroll);

                javafx.scene.control.Separator sepUser = new javafx.scene.control.Separator();

                Label methodTitle = new Label("Payment Method");
                methodTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");

                javafx.scene.control.CheckBox allMethodsCb = new javafx.scene.control.CheckBox("All Methods");
                allMethodsCb.setSelected(returnMethodsRef.get().isEmpty());
                allMethodsCb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");

                VBox methodScroll = new VBox(8);
                methodScroll.setPadding(new Insets(5, 5, 5, 20));

                java.util.List<javafx.scene.control.CheckBox> methodCbs = new java.util.ArrayList<>();
                java.util.Set<com.pbl3.project.pbl3_project.entity.PaymentMethod> activeMethodFilters = returnMethodsRef.get();
                for (com.pbl3.project.pbl3_project.entity.PaymentMethod paymentMethod : com.pbl3.project.pbl3_project.entity.PaymentMethod.values()) {
                    javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(context.support().formatPaymentMethodLabel(paymentMethod));
                    cb.setUserData(paymentMethod);
                    cb.setSelected(activeMethodFilters.isEmpty() || activeMethodFilters.contains(paymentMethod));
                    cb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                    cb.setOnAction(e -> {
                        if (!cb.isSelected()) {
                            allMethodsCb.setSelected(false);
                        } else {
                            boolean all = true;
                            for (javafx.scene.control.CheckBox c : methodCbs) {
                                if (!c.isSelected()) {
                                    all = false;
                                }
                            }
                            allMethodsCb.setSelected(all);
                        }
                    });
                    methodCbs.add(cb);
                    methodScroll.getChildren().add(cb);
                }
                allMethodsCb.setOnAction(e -> {
                    boolean sel = allMethodsCb.isSelected();
                    for (javafx.scene.control.CheckBox cb : methodCbs) {
                        cb.setSelected(sel);
                    }
                });
                FilterDisclosureSection methodSection = new FilterDisclosureSection(allMethodsCb, methodScroll);

                javafx.scene.control.Separator sepMethod = new javafx.scene.control.Separator();

                Label statusTitle = new Label("Return / Refund Status");
                statusTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");

                java.util.Set<com.pbl3.project.pbl3_project.entity.OrderStatus> scopeStatuses = scopeRef.get().getStatuses();
                java.util.Set<com.pbl3.project.pbl3_project.entity.OrderStatus> activeStatusFilters = returnStatusesRef.get();
                javafx.scene.control.CheckBox allStatusesCb = new javafx.scene.control.CheckBox("All In Scope");
                allStatusesCb.setSelected(activeStatusFilters.isEmpty());
                allStatusesCb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");

                VBox statusScroll = new VBox(8);
                statusScroll.setPadding(new Insets(5, 5, 5, 20));

                java.util.List<javafx.scene.control.CheckBox> statusCbs = new java.util.ArrayList<>();
                for (com.pbl3.project.pbl3_project.entity.OrderStatus status : com.pbl3.project.pbl3_project.entity.OrderStatus.values()) {
                    if (!scopeStatuses.contains(status)) {
                        continue;
                    }
                    javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(context.support().formatOrderStatus(status));
                    cb.setUserData(status);
                    cb.setSelected(activeStatusFilters.isEmpty() || activeStatusFilters.contains(status));
                    cb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                    cb.setOnAction(e -> {
                        if (!cb.isSelected()) {
                            allStatusesCb.setSelected(false);
                        } else {
                            boolean all = true;
                            for (javafx.scene.control.CheckBox c : statusCbs) {
                                if (!c.isSelected()) {
                                    all = false;
                                }
                            }
                            allStatusesCb.setSelected(all);
                        }
                    });
                    statusCbs.add(cb);
                    statusScroll.getChildren().add(cb);
                }
                allStatusesCb.setOnAction(e -> {
                    boolean sel = allStatusesCb.isSelected();
                    for (javafx.scene.control.CheckBox cb : statusCbs) {
                        cb.setSelected(sel);
                    }
                });
                FilterDisclosureSection statusSection = new FilterDisclosureSection(allStatusesCb, statusScroll);

                javafx.scene.control.Separator sepStatus = new javafx.scene.control.Separator();

                Label priceTitle = new Label("Net Paid Range");
                priceTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");

                BigDecimal maxPriceValue = filterOptions.maxTotal();
                double maxPrice = maxPriceValue == null ? 0.0 : maxPriceValue.doubleValue();
                if (maxPrice == 0) {
                    maxPrice = 1000;
                }

                double initialMinPrice = returnMinTotalRef.get() == null ? 0.0 : returnMinTotalRef.get().doubleValue();
                double initialMaxPrice = returnMaxTotalRef.get() == null ? maxPrice : Math.min(maxPrice, returnMaxTotalRef.get().doubleValue());
                Label priceLabel = new Label(String.format("%.0f - %.0f VND", initialMinPrice, initialMaxPrice));
                priceLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -app-primary; -fx-font-weight: bold;");

                RangeSlider priceSlider = new RangeSlider(0, maxPrice, initialMinPrice, initialMaxPrice, 280);
                priceSlider.minVal.addListener((o, ov, nv) ->
                    priceLabel.setText(String.format("%.0f - %.0f VND", nv.doubleValue(), priceSlider.maxVal.get()))
                );
                priceSlider.maxVal.addListener((o, ov, nv) ->
                    priceLabel.setText(String.format("%.0f - %.0f VND", priceSlider.minVal.get(), nv.doubleValue()))
                );

                scrollContent.getChildren().addAll(
                    dateTitle, dateBox, sepDate,
                    userTitle, userSection.getNode(), sepUser,
                    methodTitle, methodSection.getNode(), sepMethod,
                    statusTitle, statusSection.getNode(), sepStatus,
                    priceTitle, priceLabel, priceSlider
                );

                javafx.scene.layout.HBox btnRow = new javafx.scene.layout.HBox(10);
                btnRow.setAlignment(Pos.CENTER_RIGHT);

                final double finalMaxPrice = maxPrice;
                Button resetBtn = new Button("Reset");
                resetBtn.getStyleClass().add("filter-reset-btn");
                resetBtn.setOnAction(ae -> {
                    returnStartDateRef.set(null);
                    returnEndDateRef.set(null);
                    returnUsersRef.set(new java.util.LinkedHashSet<>());
                    returnMethodsRef.set(new java.util.LinkedHashSet<>());
                    returnStatusesRef.set(new java.util.LinkedHashSet<>());
                    returnMinTotalRef.set(null);
                    returnMaxTotalRef.set(null);
                    returnCurrentPage[0] = 0;
                    updateReturnFilterAccent.run();
                    loadReturnPage.run();
                    returnFilterPopup.hide();
                });

                Button applyBtn = new Button("Apply Filter");
                applyBtn.getStyleClass().add("filter-apply-btn");
                applyBtn.setOnAction(ae -> {
                    java.util.Set<Long> selectedUsers = new java.util.LinkedHashSet<>();
                    for (javafx.scene.control.CheckBox cb : userCbs) {
                        if (cb.isSelected() && cb.getUserData() instanceof Long userId) {
                            selectedUsers.add(userId);
                        }
                    }

                    java.util.Set<com.pbl3.project.pbl3_project.entity.PaymentMethod> selectedMethods = new java.util.LinkedHashSet<>();
                    for (javafx.scene.control.CheckBox cb : methodCbs) {
                        if (cb.isSelected() && cb.getUserData() instanceof com.pbl3.project.pbl3_project.entity.PaymentMethod method) {
                            selectedMethods.add(method);
                        }
                    }

                    java.util.Set<com.pbl3.project.pbl3_project.entity.OrderStatus> selectedStatuses = new java.util.LinkedHashSet<>();
                    for (javafx.scene.control.CheckBox cb : statusCbs) {
                        if (cb.isSelected()) {
                            selectedStatuses.add((com.pbl3.project.pbl3_project.entity.OrderStatus) cb.getUserData());
                        }
                    }

                    double pMin = priceSlider.minVal.get();
                    double pMax = priceSlider.maxVal.get();

                    returnStartDateRef.set(startDatePicker.getValue());
                    returnEndDateRef.set(endDatePicker.getValue());
                    returnUsersRef.set(allUsersCb.isSelected() ? new java.util.LinkedHashSet<>() : selectedUsers);
                    returnMethodsRef.set(allMethodsCb.isSelected() ? new java.util.LinkedHashSet<>() : selectedMethods);
                    returnStatusesRef.set(allStatusesCb.isSelected() ? new java.util.LinkedHashSet<>() : selectedStatuses);
                    returnMinTotalRef.set(pMin <= 0 ? null : MoneySupport.normalize(BigDecimal.valueOf(pMin)));
                    returnMaxTotalRef.set(pMax >= finalMaxPrice ? null : MoneySupport.normalize(BigDecimal.valueOf(pMax)));
                    returnCurrentPage[0] = 0;
                    updateReturnFilterAccent.run();
                    loadReturnPage.run();
                    returnFilterPopup.hide();
                });

                btnRow.getChildren().addAll(resetBtn, applyBtn);

                popupContainer.getChildren().addAll(scrollPane, btnRow);
                returnFilterPopup.getContent().clear();
                returnFilterPopup.getContent().add(popupContainer);

                context.support().showPopupBelow(returnFilterPopup, returnFilterBox, -290, 5);
                };

                ReturnFilterOptions cachedOptions = returnFilterOptionsCache.get();
                if (cachedOptions != null) {
                    showFilterContent.accept(cachedOptions);
                    return;
                }

                returnFilterPopup.getContent().setAll(FilterControlFactory.loadingContainer(350, "Loading filters..."));
                context.support().showPopupBelow(returnFilterPopup, returnFilterBox, -290, 5);

                javafx.concurrent.Task<ReturnFilterOptions> task = new javafx.concurrent.Task<>() {
                    @Override
                    protected ReturnFilterOptions call() {
                        return new ReturnFilterOptions(
                            context.orderService().getOrderCreatorOptions(user),
                            context.orderService().getOrderMaxTotalPrice(user)
                        );
                    }
                };
                task.setOnSucceeded(taskEvent -> {
                    ReturnFilterOptions optionsValue = task.getValue();
                    returnFilterOptionsCache.set(optionsValue);
                    if (returnFilterPopup.isShowing()) {
                        showFilterContent.accept(optionsValue);
                    }
                });
                task.setOnFailed(taskEvent -> {
                    returnFilterPopup.hide();
                    context.showUserFacingError(task.getException());
                });
                Thread worker = new Thread(task, "return-filter-options-loader");
                worker.setDaemon(true);
                worker.start();
            } catch (Exception ex) {
                context.showUserFacingError(ex);
            }
        });

        Button manageReturnBtn = ButtonFactory.expandableManageAction("Manage Return/Refund", 220);
        manageReturnBtn.setDisable(true);
        manageReturnBtn.setOnAction(e -> {
            java.util.List<com.pbl3.project.pbl3_project.entity.Order> selectedOrders = new java.util.ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selectedOrders.size() != 1) {
                context.toastService().showWarning("Select exactly one order.");
                return;
            }
            showReturnOrderDetailsAsync(selectedOrders.get(0).getId(), refreshReturnTableRef[0]);
        });
        table.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<com.pbl3.project.pbl3_project.entity.Order>) c ->
            manageReturnBtn.setDisable(table.getSelectionModel().getSelectedItems().size() != 1)
        );

        javafx.scene.layout.BorderPane returnToolbar = new javafx.scene.layout.BorderPane();
        javafx.scene.layout.HBox returnLeftBox = new javafx.scene.layout.HBox(15, manageReturnBtn);
        returnLeftBox.setAlignment(Pos.CENTER_LEFT);
        javafx.scene.layout.HBox returnRightBox = new javafx.scene.layout.HBox(15, scopeBox, returnFilterBox, returnSearchBox);
        returnRightBox.setAlignment(Pos.CENTER_RIGHT);
        returnToolbar.setLeft(returnLeftBox);
        returnToolbar.setRight(returnRightBox);

        javafx.scene.layout.HBox returnStatusBar = new javafx.scene.layout.HBox(
            15,
            returnSortStatusLabel,
            returnRowCountLabel,
            returnPageLabel,
            returnPrevBtn,
            returnNextBtn
        );
        applyStandardTableStatusBar(returnStatusBar);

        content.getChildren().addAll(returnToolbar, table, returnStatusBar);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);

        table.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<com.pbl3.project.pbl3_project.entity.Order>) c -> updateReturnStatusBar.run());
        javafx.application.Platform.runLater(loadReturnPage);
        updateReturnFilterAccent.run();
        TableViewSupport.enableDeselectOnOutsideClick(content, table);
        return content;
    }

    private void showReturnOrderDetailsAsync(Long orderId, Runnable onChanged) {
        if (orderId == null) {
            context.toastService().showWarning("Select exactly one order.");
            return;
        }
        javafx.stage.Stage loadingDialog = com.pbl3.project.pbl3_project.ui.util.DialogSupport.showLoadingWindow(
            context.owner(),
            java.text.MessageFormat.format("Order Details #{0}", orderId),
            "Loading order details...",
            420,
            240
        );

        javafx.concurrent.Task<com.pbl3.project.pbl3_project.entity.Order> task = new javafx.concurrent.Task<>() {
            @Override
            protected com.pbl3.project.pbl3_project.entity.Order call() {
                return context.orderService().getOrderWithItems(orderId, user);
            }
        };
        task.setOnSucceeded(event -> {
            if (!loadingDialog.isShowing()) {
                return;
            }
            loadingDialog.close();
            context.support().showOrderDetailsDialog(context.owner(), task.getValue(), user, onChanged);
        });
        task.setOnFailed(event -> {
            loadingDialog.close();
            Throwable ex = task.getException();
            context.toastService().showError(java.text.MessageFormat.format(
                "Could not load order details: {0}",
                ex != null && ex.getMessage() != null ? ex.getMessage() : "Unknown error"
            ));
        });
        Thread worker = new Thread(task, "returns-order-details-dialog-loader");
        worker.setDaemon(true);
        worker.start();
    }


    private void applyStandardTableSizing(TableView<?> table) { context.support().applyStandardTableSizing(table); }
    private void applyStandardTablePageLayout(VBox root) { context.support().applyStandardTablePageLayout(root); }
    private void applyStandardTableStatusBar(HBox statusBar) { context.support().applyStandardTableStatusBar(statusBar); }
    private Label createStatusMetaLabel(String text) { return context.support().createStatusMetaLabel(text); }
    private void updatePagedStatus(TableView<?> table, Label rowCountLabel, Label pageLabel, Button prevButton, Button nextButton, long totalElements, int currentPage, int totalPages, int pageSize) { context.support().updatePagedStatus(table, rowCountLabel, pageLabel, prevButton, nextButton, totalElements, currentPage, totalPages, pageSize); }
    private Pageable createPageable(TableSortState sortState, Map<String, String> propertyByUiKey, int page, int size) { return context.support().createPageable(sortState, propertyByUiKey, page, size); }
    private TableSortState getOrCreateTableSortState(String stateKey, SortCriterion... defaultCriteria) { return context.support().getOrCreateTableSortState(stateKey, defaultCriteria); }
    private <T> void installSortHeaderIndicators(LinkedHashMap<String, TableColumn<T, ?>> columnsByKey) { context.support().installSortHeaderIndicators(columnsByKey); }
    private <T> void applySortStateToTable(TableView<T> table, LinkedHashMap<String, TableColumn<T, ?>> columnsByKey, TableSortState sortState) { context.support().applySortStateToTable(table, columnsByKey, sortState); }
    private Label createSortStatusLabel(TableSortState sortState, Map<String, String> labelsByKey) { return context.support().createSortStatusLabel(sortState, labelsByKey); }
    private String buildSortStatusText(TableSortState sortState, Map<String, String> labelsByKey) { return context.support().buildSortStatusText(sortState, labelsByKey); }
    private <T> void installManualServerSorting(TableView<T> table, LinkedHashMap<String, TableColumn<T, ?>> columnsByKey, TableSortState sortState, Runnable onSortChanged) { context.support().installManualServerSorting(table, columnsByKey, sortState, onSortChanged); }
}
