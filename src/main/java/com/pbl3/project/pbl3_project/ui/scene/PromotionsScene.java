package com.pbl3.project.pbl3_project.ui.scene;

import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.ui.component.ActionTaskbarFactory;
import com.pbl3.project.pbl3_project.ui.component.ButtonFactory;
import com.pbl3.project.pbl3_project.ui.component.ExpandableSearchControl;
import com.pbl3.project.pbl3_project.ui.component.FilterControlFactory;
import com.pbl3.project.pbl3_project.ui.component.FilterDisclosureSection;
import com.pbl3.project.pbl3_project.ui.util.AsyncPageCache;
import com.pbl3.project.pbl3_project.ui.util.AsyncUiTask;
import com.pbl3.project.pbl3_project.ui.util.DialogSupport;
import com.pbl3.project.pbl3_project.ui.util.SortCriterion;
import com.pbl3.project.pbl3_project.ui.util.TableSortState;
import com.pbl3.project.pbl3_project.ui.util.TableViewSupport;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

public final class PromotionsScene {
    public record Options() {
    }

    private record PromotionPageResult(
        org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.Promotion> page,
        int pageIndex,
        Long restoreId,
        double previousScrollValue
    ) {
    }

    private PromotionsScene() {
    }

    public static Node create(SceneRuntimeContext context, User user, Options options) {
        Stage stage = context.owner();
        SceneUiSupport support = context.support();

        final String promotionSortStateKey = "promotions";
        TableSortState promotionSortState = support.getOrCreateTableSortState(
            promotionSortStateKey,
            new SortCriterion("createdAt", javafx.scene.control.TableColumn.SortType.DESCENDING),
            new SortCriterion("id", javafx.scene.control.TableColumn.SortType.DESCENDING)
        );
        java.util.LinkedHashMap<String, String> promotionSortProperties = new java.util.LinkedHashMap<>();
        promotionSortProperties.put("id", "id");
        promotionSortProperties.put("name", "name");
        promotionSortProperties.put("scope", "scope");
        promotionSortProperties.put("discountValue", "discountValue");
        promotionSortProperties.put("startsAt", "startsAt");
        promotionSortProperties.put("endsAt", "endsAt");
        promotionSortProperties.put("createdAt", "createdAt");
        promotionSortProperties.put("createdBy", "createdBy.fullName");
        promotionSortProperties.put("target", "targetProduct.name");

        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.Promotion> table = new javafx.scene.control.TableView<>();
        support.applyStandardTableSizing(table);
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        TableViewSupport.enableDragSelection(table);

        final int promotionPageSize = 20;
        final int[] promotionCurrentPage = {0};
        final int[] promotionTotalPages = {0};
        final long[] promotionTotalElements = {0L};
        java.util.concurrent.atomic.AtomicReference<String> promotionSearchRef = new java.util.concurrent.atomic.AtomicReference<>("");
        java.util.concurrent.atomic.AtomicReference<java.time.LocalDate> promotionStartDateRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<java.time.LocalDate> promotionEndDateRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<java.util.Set<com.pbl3.project.pbl3_project.entity.PromotionScope>> promotionScopesRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<Boolean> promotionEnabledRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<java.util.Set<com.pbl3.project.pbl3_project.entity.PromotionLifecycleStatus>> promotionStatusesRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicLong promotionPageLoadVersion = new java.util.concurrent.atomic.AtomicLong();
        AsyncPageCache<PromotionPageResult> promotionPageCache = new AsyncPageCache<>(80);

        Label promotionRowCountLabel = support.createStatusMetaLabel(java.text.MessageFormat.format("Showing {0}-{1} of {2} Row(s)", 0, 0, 0));
        Label promotionPageLabel = support.createStatusMetaLabel(java.text.MessageFormat.format("Page {0} / {1}", 0, 0));
        Button promotionPrevBtn = ButtonFactory.pageNav("Prev");
        Button promotionNextBtn = ButtonFactory.pageNav("Next");
        java.util.concurrent.atomic.AtomicReference<Long> promotionSelectionRestoreId = new java.util.concurrent.atomic.AtomicReference<>();

        Runnable[] refreshPromotionTableRef = new Runnable[1];
        Runnable updatePromotionStatusBar = () -> support.updatePagedStatus(
            table,
            promotionRowCountLabel,
            promotionPageLabel,
            promotionPrevBtn,
            promotionNextBtn,
            promotionTotalElements[0],
            promotionCurrentPage[0],
            promotionTotalPages[0],
            promotionPageSize
        );
        Runnable loadPromotionPage = () -> {
            Long restoreId = promotionSelectionRestoreId.getAndSet(null);
            double previousScrollValue = restoreId != null ? support.getTableVerticalScrollValue(table) : Double.NaN;
            int requestedPage = promotionCurrentPage[0];
            String search = promotionSearchRef.get();
            java.util.Set<com.pbl3.project.pbl3_project.entity.PromotionScope> scopes =
                new java.util.LinkedHashSet<>(promotionScopesRef.get());
            Boolean enabled = promotionEnabledRef.get();
            java.util.Set<com.pbl3.project.pbl3_project.entity.PromotionLifecycleStatus> statuses =
                new java.util.LinkedHashSet<>(promotionStatusesRef.get());
            java.time.LocalDate startDate = promotionStartDateRef.get();
            java.time.LocalDate endDate = promotionEndDateRef.get();
            java.util.List<SortCriterion> sortSnapshot = promotionSortState.snapshot();
            TableSortState sortForLoad = new TableSortState(sortSnapshot);
            java.util.function.IntFunction<Object> cacheKeyForPage = pageIndex -> java.util.Arrays.asList(
                pageIndex,
                search,
                scopes,
                enabled,
                statuses,
                startDate,
                endDate,
                sortSnapshot
            );
            java.util.function.IntFunction<PromotionPageResult> fetchPromotionPage = pageIndex -> {
                int resolvedPage = pageIndex;
                org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.Promotion> pageData =
                    context.promotionService().searchPromotions(
                        user,
                        search,
                        scopes,
                        enabled,
                        statuses,
                        startDate,
                        endDate,
                        support.createPageable(sortForLoad, promotionSortProperties, resolvedPage, promotionPageSize)
                    );
                if (pageData.getTotalPages() > 0 && resolvedPage >= pageData.getTotalPages()) {
                    resolvedPage = pageData.getTotalPages() - 1;
                    pageData = context.promotionService().searchPromotions(
                        user,
                        search,
                        scopes,
                        enabled,
                        statuses,
                        startDate,
                        endDate,
                        support.createPageable(sortForLoad, promotionSortProperties, resolvedPage, promotionPageSize)
                    );
                }
                return new PromotionPageResult(pageData, resolvedPage, null, Double.NaN);
            };

            AsyncUiTask.runLatestCachedTableLoad(
                table,
                promotionPrevBtn,
                promotionNextBtn,
                promotionPageLoadVersion,
                promotionPageCache,
                cacheKeyForPage.apply(requestedPage),
                () -> fetchPromotionPage.apply(requestedPage),
                result -> {
                    var pageData = result.page();
                    promotionCurrentPage[0] = result.pageIndex();
                    promotionTotalElements[0] = pageData.getTotalElements();
                    promotionTotalPages[0] = pageData.getTotalPages();
                    table.setItems(javafx.collections.FXCollections.observableArrayList(pageData.getContent()));
                    if (restoreId != null) {
                        support.restoreTableSelectionById(table, restoreId, com.pbl3.project.pbl3_project.entity.Promotion::getId);
                        support.restoreTableVerticalScrollValue(table, previousScrollValue);
                    }
                    updatePromotionStatusBar.run();
                    int nextPage = result.pageIndex() + 1;
                    if (nextPage < promotionTotalPages[0]) {
                        promotionPageCache.prefetch(
                            cacheKeyForPage.apply(nextPage),
                            () -> fetchPromotionPage.apply(nextPage),
                            null,
                            "promotions-next-page-prefetch"
                        );
                    }
                    int previousPage = result.pageIndex() - 1;
                    if (previousPage >= 0) {
                        promotionPageCache.prefetch(
                            cacheKeyForPage.apply(previousPage),
                            () -> fetchPromotionPage.apply(previousPage),
                            null,
                            "promotions-prev-page-prefetch"
                        );
                    }
                },
                context::showUserFacingError,
                "Loading promotions...",
                "Could not load promotions",
                "promotions-page-loader"
            );
        };
        refreshPromotionTableRef[0] = () -> {
            promotionPageCache.clear();
            loadPromotionPage.run();
        };

        promotionPrevBtn.setOnAction(e -> {
            if (promotionCurrentPage[0] > 0) {
                promotionCurrentPage[0]--;
                loadPromotionPage.run();
            }
        });
        promotionNextBtn.setOnAction(e -> {
            if (promotionCurrentPage[0] + 1 < promotionTotalPages[0]) {
                promotionCurrentPage[0]++;
                loadPromotionPage.run();
            }
        });

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Promotion, Long> idCol = new javafx.scene.control.TableColumn<>("ID");
        idCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));
        idCol.setMinWidth(68);
        idCol.setPrefWidth(72);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Promotion, String> nameCol = new javafx.scene.control.TableColumn<>("Title");
        nameCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("name"));
        nameCol.setMinWidth(150);
        nameCol.setPrefWidth(200);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Promotion, String> scopeCol = new javafx.scene.control.TableColumn<>("Scope");
        scopeCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(support.formatPromotionScopeLabel(cell.getValue().getScope())));
        scopeCol.setMinWidth(82);
        scopeCol.setPrefWidth(96);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Promotion, String> targetCol = new javafx.scene.control.TableColumn<>("Target");
        targetCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(support.formatPromotionTargetLabel(cell.getValue())));
        targetCol.setMinWidth(150);
        targetCol.setPrefWidth(190);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Promotion, String> discountCol = new javafx.scene.control.TableColumn<>("Disc.");
        discountCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(support.formatPromotionDiscountLabel(cell.getValue())));
        discountCol.setMinWidth(82);
        discountCol.setPrefWidth(92);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Promotion, String> scheduleCol = new javafx.scene.control.TableColumn<>("Schedule");
        scheduleCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(support.formatPromotionScheduleLabel(cell.getValue())));
        scheduleCol.setMinWidth(150);
        scheduleCol.setPrefWidth(180);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Promotion, String> statusCol = new javafx.scene.control.TableColumn<>("Status");
        statusCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
            support.formatPromotionLifecycleStatusLabel(cell.getValue().getLifecycleStatus())
        ));
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
                if (support.formatPromotionLifecycleStatusLabel(com.pbl3.project.pbl3_project.entity.PromotionLifecycleStatus.ACTIVE).equals(item)) {
                    textColor = "-app-success-hover";
                } else if (support.formatPromotionLifecycleStatusLabel(com.pbl3.project.pbl3_project.entity.PromotionLifecycleStatus.SCHEDULED).equals(item)) {
                    textColor = "-app-primary-hover";
                } else if (support.formatPromotionLifecycleStatusLabel(com.pbl3.project.pbl3_project.entity.PromotionLifecycleStatus.EXPIRED).equals(item)) {
                    textColor = "#fe9900";
                } else {
                    textColor = "-app-danger-hover";
                }
                setStyle("-fx-text-fill: " + textColor + "; -fx-font-weight: bold; -fx-alignment: CENTER;");
            }
        });
        statusCol.setMinWidth(96);
        statusCol.setPrefWidth(110);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Promotion, String> createdByCol = new javafx.scene.control.TableColumn<>("Owner");
        createdByCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(support.formatPromotionOwnerLabel(cell.getValue())));
        createdByCol.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                    return;
                }
                setText(item);
                com.pbl3.project.pbl3_project.entity.Promotion promotion = getTableRow() != null ? getTableRow().getItem() : null;
                String fullOwner = promotion != null ? promotion.getCreatedByDisplayName() : item;
                setTooltip(new javafx.scene.control.Tooltip(fullOwner));
            }
        });
        createdByCol.setMinWidth(92);
        createdByCol.setPrefWidth(115);

        table.getColumns().addAll(idCol, nameCol, scopeCol, targetCol, discountCol, scheduleCol, statusCol, createdByCol);

        java.util.LinkedHashMap<String, javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Promotion, ?>> promotionSortColumns =
            new java.util.LinkedHashMap<>();
        promotionSortColumns.put("id", idCol);
        promotionSortColumns.put("name", nameCol);
        promotionSortColumns.put("scope", scopeCol);
        promotionSortColumns.put("target", targetCol);
        promotionSortColumns.put("discountValue", discountCol);
        promotionSortColumns.put("startsAt", scheduleCol);
        promotionSortColumns.put("createdBy", createdByCol);
        support.installSortHeaderIndicators(promotionSortColumns);

        java.util.LinkedHashMap<String, String> promotionSortLabels = new java.util.LinkedHashMap<>();
        promotionSortLabels.put("id", "ID");
        promotionSortLabels.put("name", "Name");
        promotionSortLabels.put("scope", "Scope");
        promotionSortLabels.put("target", "Target");
        promotionSortLabels.put("discountValue", "Discount");
        promotionSortLabels.put("startsAt", "Schedule");
        promotionSortLabels.put("createdBy", "Owner");
        Label promotionSortStatusLabel = support.createSortStatusLabel(promotionSortState, promotionSortLabels);
        Runnable applyPromotionSortUi = () -> {
            support.applySortStateToTable(table, promotionSortColumns, promotionSortState);
            promotionSortStatusLabel.setText(support.buildSortStatusText(promotionSortState, promotionSortLabels));
        };
        applyPromotionSortUi.run();
        support.installManualServerSorting(
            table,
            promotionSortColumns,
            promotionSortState,
            () -> {
                applyPromotionSortUi.run();
                promotionCurrentPage[0] = 0;
                loadPromotionPage.run();
            }
        );

        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<com.pbl3.project.pbl3_project.entity.Promotion> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY
                    && event.getClickCount() == 2
                    && !row.isEmpty()) {
                    support.showPromotionDialog(stage, user, row.getItem(), refreshPromotionTableRef[0]);
                }
            });
            return row;
        });

        ExpandableSearchControl promotionSearchControl = ExpandableSearchControl.create(260, Color.web("#1d7df2"));
        javafx.animation.PauseTransition promotionSearchPause = new javafx.animation.PauseTransition(Duration.millis(220));
        promotionSearchPause.setOnFinished(e -> {
            promotionCurrentPage[0] = 0;
            promotionSearchRef.set(promotionSearchControl.field().getText());
            loadPromotionPage.run();
        });
        promotionSearchControl.field().textProperty().addListener((obs, oldV, newV) -> promotionSearchPause.playFromStart());

        javafx.scene.layout.HBox promotionFilterBox = new javafx.scene.layout.HBox();
        promotionFilterBox.setAlignment(Pos.CENTER);
        promotionFilterBox.getStyleClass().add("expandable-search-box");
        promotionFilterBox.setPrefSize(40, 40);
        promotionFilterBox.setMinSize(40, 40);
        promotionFilterBox.setMaxSize(40, 40);
        promotionFilterBox.setCursor(javafx.scene.Cursor.HAND);
        javafx.scene.shape.SVGPath promotionFilterIcon = new javafx.scene.shape.SVGPath();
        promotionFilterIcon.setContent("M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z");
        promotionFilterIcon.setFill(Color.web("#1d7df2"));
        promotionFilterBox.getChildren().add(promotionFilterIcon);
        javafx.scene.control.Tooltip.install(promotionFilterBox, new javafx.scene.control.Tooltip("Filter"));

        Runnable updatePromotionFilterAccent = () -> {
            boolean hasFilter = promotionStartDateRef.get() != null
                || promotionEndDateRef.get() != null
                || !promotionScopesRef.get().isEmpty()
                || promotionEnabledRef.get() != null
                || !promotionStatusesRef.get().isEmpty();
            promotionFilterBox.setStyle(hasFilter ? "-fx-border-color: -app-primary;" : "");
        };

        javafx.stage.Popup promotionFilterPopup = new javafx.stage.Popup();
        promotionFilterPopup.setAutoHide(true);
        promotionFilterBox.setOnMouseClicked(fev -> {
            fev.consume();
            try {
                if (promotionFilterPopup.isShowing()) {
                    promotionFilterPopup.hide();
                    return;
                }

                FilterControlFactory.Shell shell = FilterControlFactory.shell(360, 340);
                VBox scrollContent = shell.content();

                Label dateTitle = FilterControlFactory.sectionTitle("Date Range");
                javafx.scene.control.DatePicker startDatePicker = new javafx.scene.control.DatePicker(promotionStartDateRef.get());
                startDatePicker.setPromptText("Start Date");
                startDatePicker.setPrefWidth(140);
                javafx.scene.control.DatePicker endDatePicker = new javafx.scene.control.DatePicker(promotionEndDateRef.get());
                endDatePicker.setPromptText("End Date");
                endDatePicker.setPrefWidth(140);
                support.customizeDatePicker(startDatePicker);
                support.customizeDatePicker(endDatePicker);
                javafx.scene.layout.HBox dateBox = new javafx.scene.layout.HBox(5, startDatePicker, new Label("-"), endDatePicker);
                dateBox.setAlignment(Pos.CENTER_LEFT);

                javafx.scene.control.Separator sepDate = new javafx.scene.control.Separator();

                Label scopeTitle = FilterControlFactory.sectionTitle("Scope");
                javafx.scene.control.CheckBox allScopesCb = new javafx.scene.control.CheckBox("All Scopes");
                allScopesCb.setSelected(promotionScopesRef.get().isEmpty());
                allScopesCb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                VBox scopeScroll = new VBox(8);
                scopeScroll.setPadding(new Insets(5, 5, 5, 20));
                java.util.List<javafx.scene.control.CheckBox> scopeCbs = new java.util.ArrayList<>();
                java.util.Set<com.pbl3.project.pbl3_project.entity.PromotionScope> activeScopes = promotionScopesRef.get();
                for (com.pbl3.project.pbl3_project.entity.PromotionScope scope : com.pbl3.project.pbl3_project.entity.PromotionScope.values()) {
                    javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(support.formatPromotionScopeLabel(scope));
                    cb.setUserData(scope);
                    cb.setSelected(activeScopes.isEmpty() || activeScopes.contains(scope));
                    cb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                    cb.setOnAction(e -> {
                        if (!cb.isSelected()) {
                            allScopesCb.setSelected(false);
                        } else {
                            boolean all = true;
                            for (javafx.scene.control.CheckBox child : scopeCbs) {
                                if (!child.isSelected()) {
                                    all = false;
                                    break;
                                }
                            }
                            allScopesCb.setSelected(all);
                        }
                    });
                    scopeCbs.add(cb);
                    scopeScroll.getChildren().add(cb);
                }
                allScopesCb.setOnAction(e -> {
                    boolean selected = allScopesCb.isSelected();
                    for (javafx.scene.control.CheckBox cb : scopeCbs) {
                        cb.setSelected(selected);
                    }
                });
                FilterDisclosureSection scopeSection = new FilterDisclosureSection(allScopesCb, scopeScroll);

                javafx.scene.control.Separator sepScope = new javafx.scene.control.Separator();

                Label enabledTitle = FilterControlFactory.sectionTitle("Enabled State");
                javafx.scene.control.ComboBox<String> enabledCombo = new javafx.scene.control.ComboBox<>();
                enabledCombo.getItems().addAll("All", "Enabled", "Disabled");
                enabledCombo.setValue(
                    promotionEnabledRef.get() == null
                        ? "All"
                        : (promotionEnabledRef.get() ? "Enabled" : "Disabled")
                );
                enabledCombo.setPrefWidth(180);

                javafx.scene.control.Separator sepEnabled = new javafx.scene.control.Separator();

                Label statusTitle = FilterControlFactory.sectionTitle("Current Status");
                javafx.scene.control.CheckBox allStatusesCb = new javafx.scene.control.CheckBox("All Statuses");
                allStatusesCb.setSelected(promotionStatusesRef.get().isEmpty());
                allStatusesCb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                VBox statusScroll = new VBox(8);
                statusScroll.setPadding(new Insets(5, 5, 5, 20));
                java.util.List<javafx.scene.control.CheckBox> statusCbs = new java.util.ArrayList<>();
                java.util.Set<com.pbl3.project.pbl3_project.entity.PromotionLifecycleStatus> activeStatuses = promotionStatusesRef.get();
                for (com.pbl3.project.pbl3_project.entity.PromotionLifecycleStatus status : com.pbl3.project.pbl3_project.entity.PromotionLifecycleStatus.values()) {
                    javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(support.formatPromotionLifecycleStatusLabel(status));
                    cb.setUserData(status);
                    cb.setSelected(activeStatuses.isEmpty() || activeStatuses.contains(status));
                    cb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                    cb.setOnAction(e -> {
                        if (!cb.isSelected()) {
                            allStatusesCb.setSelected(false);
                        } else {
                            boolean all = true;
                            for (javafx.scene.control.CheckBox child : statusCbs) {
                                if (!child.isSelected()) {
                                    all = false;
                                    break;
                                }
                            }
                            allStatusesCb.setSelected(all);
                        }
                    });
                    statusCbs.add(cb);
                    statusScroll.getChildren().add(cb);
                }
                allStatusesCb.setOnAction(e -> {
                    boolean selected = allStatusesCb.isSelected();
                    for (javafx.scene.control.CheckBox cb : statusCbs) {
                        cb.setSelected(selected);
                    }
                });
                FilterDisclosureSection statusSection = new FilterDisclosureSection(allStatusesCb, statusScroll);

                scrollContent.getChildren().addAll(
                    dateTitle, dateBox, sepDate,
                    scopeTitle, scopeSection.getNode(), sepScope,
                    enabledTitle, enabledCombo, sepEnabled,
                    statusTitle, statusSection.getNode()
                );

                Button resetBtn = new Button("Reset");
                resetBtn.getStyleClass().add("filter-reset-btn");
                resetBtn.setOnAction(ae -> {
                    promotionStartDateRef.set(null);
                    promotionEndDateRef.set(null);
                    promotionScopesRef.set(new java.util.LinkedHashSet<>());
                    promotionEnabledRef.set(null);
                    promotionStatusesRef.set(new java.util.LinkedHashSet<>());
                    promotionCurrentPage[0] = 0;
                    updatePromotionFilterAccent.run();
                    loadPromotionPage.run();
                    promotionFilterPopup.hide();
                });

                Button applyBtn = new Button("Apply");
                applyBtn.getStyleClass().add("filter-apply-btn");
                applyBtn.setOnAction(ae -> {
                    java.util.Set<com.pbl3.project.pbl3_project.entity.PromotionScope> selectedScopes = new java.util.LinkedHashSet<>();
                    for (javafx.scene.control.CheckBox cb : scopeCbs) {
                        if (cb.isSelected()) {
                            selectedScopes.add((com.pbl3.project.pbl3_project.entity.PromotionScope) cb.getUserData());
                        }
                    }
                    java.util.Set<com.pbl3.project.pbl3_project.entity.PromotionLifecycleStatus> selectedStatuses = new java.util.LinkedHashSet<>();
                    for (javafx.scene.control.CheckBox cb : statusCbs) {
                        if (cb.isSelected()) {
                            selectedStatuses.add((com.pbl3.project.pbl3_project.entity.PromotionLifecycleStatus) cb.getUserData());
                        }
                    }
                    promotionStartDateRef.set(startDatePicker.getValue());
                    promotionEndDateRef.set(endDatePicker.getValue());
                    promotionScopesRef.set(allScopesCb.isSelected() ? new java.util.LinkedHashSet<>() : selectedScopes);
                    promotionStatusesRef.set(allStatusesCb.isSelected() ? new java.util.LinkedHashSet<>() : selectedStatuses);
                    promotionEnabledRef.set(switch (enabledCombo.getValue()) {
                        case "Enabled" -> Boolean.TRUE;
                        case "Disabled" -> Boolean.FALSE;
                        default -> null;
                    });
                    promotionCurrentPage[0] = 0;
                    updatePromotionFilterAccent.run();
                    loadPromotionPage.run();
                    promotionFilterPopup.hide();
                });

                shell.container().getChildren().add(FilterControlFactory.actionRow(resetBtn, applyBtn));
                promotionFilterPopup.getContent().clear();
                promotionFilterPopup.getContent().add(shell.container());
                support.showPopupBelow(promotionFilterPopup, promotionFilterBox, -300, 5);
            } catch (Exception ex) {
                context.showUserFacingError(ex);
            }
        });

        Button newPromotionBtn = ButtonFactory.expandableGreenAction("New Promotion", 195);
        newPromotionBtn.setOnAction(e -> support.showPromotionDialog(stage, user, null, refreshPromotionTableRef[0]));

        Button editPromotionBtn = ActionTaskbarFactory.createButton(
            ActionTaskbarFactory.icon("edit"),
            "Edit Promotion",
            "promotion-taskbar-button-edit"
        );
        editPromotionBtn.setDisable(true);
        editPromotionBtn.setOnAction(e -> {
            java.util.List<com.pbl3.project.pbl3_project.entity.Promotion> selectedPromotions =
                new java.util.ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selectedPromotions.size() != 1) {
                context.toastService().showWarning("Select exactly one promotion to edit.");
                return;
            }
            support.showPromotionDialog(stage, user, selectedPromotions.get(0), refreshPromotionTableRef[0]);
        });

        Button togglePromotionBtn = ActionTaskbarFactory.createButton(
            ActionTaskbarFactory.icon("power"),
            "Enable / Disable Promotion",
            "promotion-taskbar-button-toggle"
        );
        togglePromotionBtn.setDisable(true);
        togglePromotionBtn.setOnAction(e -> {
            java.util.List<com.pbl3.project.pbl3_project.entity.Promotion> selectedPromotions =
                new java.util.ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selectedPromotions.size() != 1) {
                context.toastService().showWarning("Select exactly one promotion first.");
                return;
            }
            com.pbl3.project.pbl3_project.entity.Promotion selectedPromotion = selectedPromotions.get(0);
            try {
                promotionSelectionRestoreId.set(selectedPromotion.getId());
                context.promotionService().setPromotionEnabled(user, selectedPromotion.getId(), !selectedPromotion.isEnabled());
                context.toastService().showSuccess(selectedPromotion.isEnabled() ? "Promotion disabled." : "Promotion enabled.");
                loadPromotionPage.run();
            } catch (Exception ex) {
                context.showUserFacingError(ex);
            }
        });

        Button deletePromotionBtn = ActionTaskbarFactory.createButton(
            ActionTaskbarFactory.icon("trash"),
            "Delete Promotion",
            "promotion-taskbar-button-delete"
        );
        deletePromotionBtn.setDisable(true);
        deletePromotionBtn.setOnAction(e -> {
            java.util.List<com.pbl3.project.pbl3_project.entity.Promotion> selectedPromotions =
                new java.util.ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selectedPromotions.isEmpty()) {
                context.toastService().showWarning("Select a promotion to delete.");
                return;
            }
            String confirmMessage = selectedPromotions.size() == 1
                ? java.text.MessageFormat.format("Delete promotion \"{0}\"?", selectedPromotions.get(0).getName())
                : java.text.MessageFormat.format("Delete {0} selected promotions?", selectedPromotions.size());
            boolean confirmed = selectedPromotions.size() >= 5
                ? DialogSupport.showTypedDangerConfirm(
                    stage,
                    "Delete Promotions",
                    confirmMessage + " This is a large delete operation.",
                    "DELETE"
                )
                : support.showConfirmDialog("Delete Promotions", confirmMessage);
            if (!confirmed) {
                return;
            }
            deletePromotionBtn.setDisable(true);
            editPromotionBtn.setDisable(true);
            togglePromotionBtn.setDisable(true);
            javafx.concurrent.Task<Integer> task = new javafx.concurrent.Task<>() {
                @Override
                protected Integer call() {
                    for (com.pbl3.project.pbl3_project.entity.Promotion promotion : selectedPromotions) {
                        context.promotionService().deletePromotion(user, promotion.getId());
                    }
                    return selectedPromotions.size();
                }
            };
            task.setOnSucceeded(event -> {
                int deletedCount = task.getValue();
                context.toastService().showSuccess(deletedCount == 1 ? "Promotion deleted." : deletedCount + " promotions deleted.");
                loadPromotionPage.run();
            });
            task.setOnFailed(event -> {
                int selectedCount = table.getSelectionModel().getSelectedItems().size();
                com.pbl3.project.pbl3_project.entity.Promotion selectedPromotion = selectedCount == 1
                    ? table.getSelectionModel().getSelectedItems().get(0)
                    : null;
                editPromotionBtn.setDisable(selectedCount != 1);
                togglePromotionBtn.setDisable(selectedCount != 1);
                deletePromotionBtn.setDisable(selectedCount == 0);
                togglePromotionBtn.setTooltip(new javafx.scene.control.Tooltip(
                    selectedPromotion == null ? "Enable / Disable Promotion" : (selectedPromotion.isEnabled() ? "Disable Promotion" : "Enable Promotion")
                ));
                context.showUserFacingError(task.getException());
            });
            Thread worker = new Thread(task, "promotions-delete");
            worker.setDaemon(true);
            worker.start();
        });

        Runnable updatePromotionSelectionActions = () -> {
            int selectedCount = table.getSelectionModel().getSelectedItems().size();
            com.pbl3.project.pbl3_project.entity.Promotion selectedPromotion = selectedCount == 1
                ? table.getSelectionModel().getSelectedItems().get(0)
                : null;
            editPromotionBtn.setDisable(selectedCount != 1);
            togglePromotionBtn.setDisable(selectedCount != 1);
            deletePromotionBtn.setDisable(selectedCount == 0);
            togglePromotionBtn.setTooltip(new javafx.scene.control.Tooltip(
                selectedPromotion == null ? "Enable / Disable Promotion" : (selectedPromotion.isEnabled() ? "Disable Promotion" : "Enable Promotion")
            ));
            updatePromotionStatusBar.run();
        };
        table.getSelectionModel().getSelectedItems().addListener(
            (javafx.collections.ListChangeListener<com.pbl3.project.pbl3_project.entity.Promotion>) change -> updatePromotionSelectionActions.run()
        );
        table.setOnKeyPressed(event -> {
            javafx.scene.input.KeyCode code = event.getCode();
            if (code == javafx.scene.input.KeyCode.DELETE || code == javafx.scene.input.KeyCode.BACK_SPACE) {
                if (!deletePromotionBtn.isDisabled()) {
                    deletePromotionBtn.fire();
                }
            } else if (code == javafx.scene.input.KeyCode.ENTER && !editPromotionBtn.isDisabled()) {
                editPromotionBtn.fire();
            }
        });

        HBox promotionActionTaskbar = ActionTaskbarFactory.create(editPromotionBtn, deletePromotionBtn, togglePromotionBtn);

        javafx.scene.layout.BorderPane promotionToolbar = new javafx.scene.layout.BorderPane();
        javafx.scene.layout.HBox promotionLeftBox = new javafx.scene.layout.HBox(
            15,
            promotionSearchControl.box(),
            promotionFilterBox,
            newPromotionBtn
        );
        promotionLeftBox.setAlignment(Pos.CENTER_LEFT);
        promotionToolbar.setLeft(promotionActionTaskbar);
        promotionToolbar.setRight(promotionLeftBox);

        javafx.scene.layout.HBox promotionStatusBar = new javafx.scene.layout.HBox(
            15,
            promotionSortStatusLabel,
            promotionRowCountLabel,
            promotionPageLabel,
            promotionPrevBtn,
            promotionNextBtn
        );
        support.applyStandardTableStatusBar(promotionStatusBar);

        VBox content = new VBox();
        support.applyStandardTablePageLayout(content);
        content.getChildren().addAll(promotionToolbar, table, promotionStatusBar);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);

        javafx.application.Platform.runLater(loadPromotionPage);
        updatePromotionFilterAccent.run();
        TableViewSupport.enableDeselectOnOutsideClick(content, table);
        return content;
    
    }
}
