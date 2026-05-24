package com.pbl3.project.pbl3_project.ui.scene;

import com.pbl3.project.pbl3_project.ui.component.ButtonFactory;
import com.pbl3.project.pbl3_project.ui.component.ExpandableSearchControl;
import com.pbl3.project.pbl3_project.ui.component.FilterControlFactory;
import com.pbl3.project.pbl3_project.ui.component.FilterDisclosureSection;
import com.pbl3.project.pbl3_project.ui.component.RangeSlider;
import com.pbl3.project.pbl3_project.ui.util.AsyncPageCache;
import com.pbl3.project.pbl3_project.ui.util.AsyncUiTask;
import com.pbl3.project.pbl3_project.ui.util.FxFormatters;
import com.pbl3.project.pbl3_project.ui.util.SortCriterion;
import com.pbl3.project.pbl3_project.ui.util.TableSortState;
import com.pbl3.project.pbl3_project.ui.util.TableViewSupport;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.springframework.data.domain.Pageable;

public final class AuditLogScene {
    private static final Color PRIMARY_COLOR = Color.web("#1d7df2");

    public record Options() {
    }

    private record InventoryAuditFilterOptions(java.util.List<String> usernames, double maxAbsoluteQuantity) {
    }

    private record InventoryAuditPageResult(
        org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.InventoryTransaction> page,
        int pageIndex
    ) {
    }

    private record OperationalAuditPageResult(
        org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.OperationalAuditLog> page,
        int pageIndex
    ) {
    }

    private record AccountAuditPageResult(
        org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.AccountAuditLog> page,
        int pageIndex
    ) {
    }

    private final SceneRuntimeContext context;
    private final com.pbl3.project.pbl3_project.entity.User user;

    private AuditLogScene(SceneRuntimeContext context, com.pbl3.project.pbl3_project.entity.User user) {
        this.context = context;
        this.user = user;
    }

    public static Node create(SceneRuntimeContext context, com.pbl3.project.pbl3_project.entity.User user, Options options) {
        return new AuditLogScene(context, user).createStockHistoryView();
    }

    private VBox createStockHistoryView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));

        javafx.scene.layout.StackPane contentArea = new javafx.scene.layout.StackPane();
        javafx.scene.layout.BorderPane auditHeader = new javafx.scene.layout.BorderPane();
        auditHeader.setMaxWidth(Double.MAX_VALUE);
        java.util.List<java.util.function.Supplier<VBox>> viewFactories = java.util.List.of(
            () -> createInventoryAuditView(),
            () -> createOperationalAuditView(),
            () -> createAccountAuditView()
        );
        VBox[] views = new VBox[viewFactories.size()];
        javafx.scene.Node[] auditToolbars = new javafx.scene.Node[viewFactories.size()];

        java.util.function.IntFunction<VBox> ensureViewLoaded = index -> {
            if (views[index] != null) {
                return views[index];
            }
            VBox view = viewFactories.get(index).get();
            views[index] = view;
            javafx.scene.control.TableView<?> table = TableViewSupport.findFirstTableView(view);
            if (table != null) {
                TableViewSupport.enableDeselectOnOutsideClick(root, table);
            }
            return view;
        };

        java.util.function.IntConsumer showView = index -> {
            VBox targetView = ensureViewLoaded.apply(index);
            javafx.scene.Node toolbar = auditToolbars[index];
            if (toolbar == null) {
                toolbar = extractAuditToolbar(targetView);
                auditToolbars[index] = toolbar;
            }
            auditHeader.setRight(toolbar);
            if (toolbar != null) {
                javafx.scene.layout.BorderPane.setAlignment(toolbar, Pos.CENTER_RIGHT);
            }
            contentArea.getChildren().setAll(targetView);
        };

        javafx.scene.Node slidingMenu = createSlidingMenu(
            new String[]{"Inventory Audit", "Operational Audit", "Account Audit"},
            showView::accept
        );
        auditHeader.setLeft(slidingMenu);
        javafx.scene.layout.BorderPane.setAlignment(slidingMenu, Pos.CENTER_LEFT);

        showView.accept(0);

        VBox.setVgrow(contentArea, javafx.scene.layout.Priority.ALWAYS);
        root.getChildren().addAll(auditHeader, contentArea);

        javafx.animation.PauseTransition preloadOperational = new javafx.animation.PauseTransition(Duration.millis(120));
        preloadOperational.setOnFinished(event -> {
            ensureViewLoaded.apply(1);
            javafx.animation.PauseTransition preloadAccount = new javafx.animation.PauseTransition(Duration.millis(120));
            preloadAccount.setOnFinished(nextEvent -> ensureViewLoaded.apply(2));
            preloadAccount.play();
        });
        javafx.application.Platform.runLater(preloadOperational::play);

        return root;
    }

    private javafx.scene.Node extractAuditToolbar(VBox auditView) {
        if (auditView == null || auditView.getChildren().isEmpty()) {
            return null;
        }
        javafx.scene.Node firstChild = auditView.getChildren().get(0);
        if (!(firstChild instanceof javafx.scene.layout.BorderPane toolbar)) {
            return null;
        }
        auditView.getChildren().remove(toolbar);
        return toolbar;
    }

    private VBox createInventoryAuditView() {
        VBox root = new VBox();
        applyStandardTablePageLayout(root, Insets.EMPTY);
        final String inventoryAuditSortStateKey = "inventory-audit";
        TableSortState inventoryAuditSortState = getOrCreateTableSortState(
            inventoryAuditSortStateKey,
            new SortCriterion("createdAt", javafx.scene.control.TableColumn.SortType.DESCENDING)
        );
        java.util.LinkedHashMap<String, String> inventoryAuditSortProperties = new java.util.LinkedHashMap<>();
        inventoryAuditSortProperties.put("createdAt", "createdAt");
        inventoryAuditSortProperties.put("transactionType", "transactionType");
        inventoryAuditSortProperties.put("productName", "product.name");
        inventoryAuditSortProperties.put("quantityChange", "quantityChange");
        inventoryAuditSortProperties.put("username", "user.username");
        java.util.LinkedHashMap<String, String> inventoryAuditSortLabels = new java.util.LinkedHashMap<>();
        inventoryAuditSortLabels.put("createdAt", "Date");
        inventoryAuditSortLabels.put("transactionType", "Action");
        inventoryAuditSortLabels.put("productName", "Product");
        inventoryAuditSortLabels.put("quantityChange", "Change");
        inventoryAuditSortLabels.put("username", "Username");

        ExpandableSearchControl inventorySearchControl = createExpandableSearchControl(320);
        TextField hField = inventorySearchControl.field();

        javafx.scene.layout.BorderPane topBar = new javafx.scene.layout.BorderPane();
        javafx.scene.layout.HBox hFilterBox = new javafx.scene.layout.HBox();
        hFilterBox.setAlignment(Pos.CENTER);
        hFilterBox.getStyleClass().add("expandable-search-box");
        hFilterBox.setPrefSize(40, 40); hFilterBox.setMinSize(40, 40); hFilterBox.setMaxSize(40, 40);
        hFilterBox.setCursor(javafx.scene.Cursor.HAND);
        javafx.scene.shape.SVGPath hFilterIcon = new javafx.scene.shape.SVGPath();
        hFilterIcon.setContent("M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z");
        hFilterIcon.setFill(PRIMARY_COLOR);
        hFilterBox.getChildren().add(hFilterIcon);
        javafx.scene.control.Tooltip.install(hFilterBox, new javafx.scene.control.Tooltip("Filter"));

        javafx.scene.layout.HBox hRightBox = new javafx.scene.layout.HBox(12, hFilterBox, inventorySearchControl.box());
        hRightBox.setAlignment(Pos.CENTER_RIGHT);
        topBar.setRight(hRightBox);

        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.InventoryTransaction> table = new javafx.scene.control.TableView<>();
        applyStandardTableSizing(table);
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        TableViewSupport.enableDragSelection(table);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.InventoryTransaction, String> dateCol = new javafx.scene.control.TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> {
            java.time.LocalDateTime dt = data.getValue().getCreatedAt();
            return new javafx.beans.property.SimpleStringProperty(context.support().formatDateTime(dt));
        });

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.InventoryTransaction, String> typeCol = new javafx.scene.control.TableColumn<>("Action");
        typeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatTransactionTypeLabel(data.getValue().getTransactionType())));
        typeCol.setStyle("-fx-alignment: CENTER;");
        typeCol.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                com.pbl3.project.pbl3_project.entity.InventoryTransaction tx = getTableRow() != null ? getTableRow().getItem() : null;
                String textColor = getTransactionTypeColor(tx != null ? tx.getTransactionType() : null);
                setStyle("-fx-text-fill: " + textColor + "; -fx-font-weight: 700; -fx-alignment: CENTER;");
            }
        });

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.InventoryTransaction, String> productCol = new javafx.scene.control.TableColumn<>("Product");
        productCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getProduct() != null ? data.getValue().getProduct().getName() : "Unknown"
        ));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.InventoryTransaction, String> qtyCol = new javafx.scene.control.TableColumn<>("Change");
        qtyCol.setCellValueFactory(data -> {
            Integer changeParam = data.getValue().getQuantityChange();
            int change = changeParam != null ? changeParam : 0;
            String prefix = change > 0 ? "+" : "";
            return new javafx.beans.property.SimpleStringProperty(prefix + change);
        });
        qtyCol.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.InventoryTransaction, String> userCol = new javafx.scene.control.TableColumn<>("Created By");
        userCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getUser() != null ? data.getValue().getUser().getUsername() : "System"
        ));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.InventoryTransaction, String> notesCol = new javafx.scene.control.TableColumn<>("Notes/Ref");
        notesCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getNotes()));

        table.getColumns().addAll(dateCol, typeCol, productCol, qtyCol, userCol, notesCol);
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<com.pbl3.project.pbl3_project.entity.InventoryTransaction> row =
                new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY
                    && event.getClickCount() == 2
                    && !row.isEmpty()) {
                    showInventoryAuditDetails(row.getItem());
                }
            });
            return row;
        });
        java.util.LinkedHashMap<String, javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.InventoryTransaction, ?>> inventoryAuditSortColumns =
            new java.util.LinkedHashMap<>();
        inventoryAuditSortColumns.put("createdAt", dateCol);
        inventoryAuditSortColumns.put("transactionType", typeCol);
        inventoryAuditSortColumns.put("productName", productCol);
        inventoryAuditSortColumns.put("quantityChange", qtyCol);
        inventoryAuditSortColumns.put("username", userCol);
        installSortHeaderIndicators(inventoryAuditSortColumns);

        final int txPageSize = 25;
        final int[] txCurrentPage = {0};
        final int[] txTotalPages = {0};
        final long[] txTotalElements = {0L};
        java.util.concurrent.atomic.AtomicReference<String> txSearchRef = new java.util.concurrent.atomic.AtomicReference<>("");
        java.util.concurrent.atomic.AtomicReference<java.time.LocalDate> txStartDateRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<java.time.LocalDate> txEndDateRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<java.util.Set<String>> txUsersRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<java.util.Set<String>> txTypesRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<Double> txMinAbsQtyRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<Double> txMaxAbsQtyRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicLong txPageLoadVersion = new java.util.concurrent.atomic.AtomicLong();
        AsyncPageCache<InventoryAuditPageResult> txPageCache = new AsyncPageCache<>(80);

        Label txRowCountLabel = createStatusMetaLabel(java.text.MessageFormat.format("Showing {0}-{1} of {2} Row(s)", 0, 0, 0));
        Label txPageLabel = createStatusMetaLabel(java.text.MessageFormat.format("Page {0} / {1}", 0, 0));
        Button txPrevBtn = ButtonFactory.pageNav("Prev");
        Button txNextBtn = ButtonFactory.pageNav("Next");

        Runnable updateTxStatusBar = () -> updatePagedStatus(
            table,
            txRowCountLabel,
            txPageLabel,
            txPrevBtn,
            txNextBtn,
            txTotalElements[0],
            txCurrentPage[0],
            txTotalPages[0],
            txPageSize
        );
        Runnable loadTransactionPage = () -> {
            int requestedPage = txCurrentPage[0];
            String search = txSearchRef.get();
            java.time.LocalDate startDate = txStartDateRef.get();
            java.time.LocalDate endDate = txEndDateRef.get();
            java.util.Set<String> usernames = new java.util.LinkedHashSet<>(txUsersRef.get());
            java.util.Set<String> types = new java.util.LinkedHashSet<>(txTypesRef.get());
            Double minAbsQty = txMinAbsQtyRef.get();
            Double maxAbsQty = txMaxAbsQtyRef.get();
            java.util.List<SortCriterion> sortSnapshot = inventoryAuditSortState.snapshot();
            TableSortState sortForLoad = new TableSortState(sortSnapshot);
            java.util.function.IntFunction<Object> cacheKeyForPage = pageIndex -> java.util.Arrays.asList(
                pageIndex,
                search,
                startDate,
                endDate,
                usernames,
                types,
                minAbsQty,
                maxAbsQty,
                sortSnapshot
            );
            java.util.function.IntFunction<InventoryAuditPageResult> fetchInventoryAuditPage = pageIndex -> {
                int resolvedPage = pageIndex;
                org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.InventoryTransaction> pageData =
                    context.transactionService().searchTransactions(
                        search,
                        startDate,
                        endDate,
                        usernames,
                        types,
                        minAbsQty,
                        maxAbsQty,
                        createPageable(sortForLoad, inventoryAuditSortProperties, resolvedPage, txPageSize)
                    );
                if (pageData.getTotalPages() > 0 && resolvedPage >= pageData.getTotalPages()) {
                    resolvedPage = pageData.getTotalPages() - 1;
                    pageData = context.transactionService().searchTransactions(
                        search,
                        startDate,
                        endDate,
                        usernames,
                        types,
                        minAbsQty,
                        maxAbsQty,
                        createPageable(sortForLoad, inventoryAuditSortProperties, resolvedPage, txPageSize)
                    );
                }
                return new InventoryAuditPageResult(pageData, resolvedPage);
            };

            AsyncUiTask.runLatestCachedTableLoad(
                table,
                txPrevBtn,
                txNextBtn,
                txPageLoadVersion,
                txPageCache,
                cacheKeyForPage.apply(requestedPage),
                () -> fetchInventoryAuditPage.apply(requestedPage),
                result -> {
                    org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.InventoryTransaction> pageData = result.page();
                    txCurrentPage[0] = result.pageIndex();
                    txTotalElements[0] = pageData.getTotalElements();
                    txTotalPages[0] = pageData.getTotalPages();
                    table.setItems(javafx.collections.FXCollections.observableArrayList(pageData.getContent()));
                    updateTxStatusBar.run();
                    int nextPage = result.pageIndex() + 1;
                    if (nextPage < txTotalPages[0]) {
                        txPageCache.prefetch(
                            cacheKeyForPage.apply(nextPage),
                            () -> fetchInventoryAuditPage.apply(nextPage),
                            null,
                            "inventory-audit-next-page-prefetch"
                        );
                    }
                    int previousPage = result.pageIndex() - 1;
                    if (previousPage >= 0) {
                        txPageCache.prefetch(
                            cacheKeyForPage.apply(previousPage),
                            () -> fetchInventoryAuditPage.apply(previousPage),
                            null,
                            "inventory-audit-prev-page-prefetch"
                        );
                    }
                },
                context::showUserFacingError,
                "Loading inventory audit...",
                "Could not load inventory audit",
                "inventory-audit-page-loader"
            );
        };
        Label inventoryAuditSortStatusLabel = createSortStatusLabel(inventoryAuditSortState, inventoryAuditSortLabels);
        Runnable applyInventoryAuditSortUi = () -> {
            applySortStateToTable(table, inventoryAuditSortColumns, inventoryAuditSortState);
            inventoryAuditSortStatusLabel.setText(buildSortStatusText(inventoryAuditSortState, inventoryAuditSortLabels));
        };
        applyInventoryAuditSortUi.run();
        installManualServerSorting(
            table,
            inventoryAuditSortColumns,
            inventoryAuditSortState,
            () -> {
                applyInventoryAuditSortUi.run();
                txCurrentPage[0] = 0;
                loadTransactionPage.run();
            }
        );
        txPrevBtn.setOnAction(e -> {
            if (txCurrentPage[0] > 0) {
                txCurrentPage[0]--;
                loadTransactionPage.run();
            }
        });
        txNextBtn.setOnAction(e -> {
            if (txCurrentPage[0] + 1 < txTotalPages[0]) {
                txCurrentPage[0]++;
                loadTransactionPage.run();
            }
        });

        javafx.animation.PauseTransition txSearchPause = new javafx.animation.PauseTransition(Duration.millis(220));
        txSearchPause.setOnFinished(e -> {
            txCurrentPage[0] = 0;
            txSearchRef.set(hField.getText());
            loadTransactionPage.run();
        });
        hField.textProperty().addListener((obs, oldV, newV) -> txSearchPause.playFromStart());

        javafx.scene.layout.HBox txStatusBar = new javafx.scene.layout.HBox(15, inventoryAuditSortStatusLabel, txRowCountLabel, txPageLabel, txPrevBtn, txNextBtn);
        applyStandardTableStatusBar(txStatusBar);
        javafx.stage.Popup hFilterPopup = new javafx.stage.Popup();
        hFilterPopup.setAutoHide(true);
        java.util.concurrent.atomic.AtomicReference<InventoryAuditFilterOptions> txFilterOptionsCache = new java.util.concurrent.atomic.AtomicReference<>();

        hFilterBox.setOnMouseClicked(fev -> {
            if (hFilterPopup.isShowing()) {
                hFilterPopup.hide();
                return;
            }

            java.util.function.Consumer<InventoryAuditFilterOptions> showFilterContent = filterOptions -> {
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
            javafx.scene.control.DatePicker startDatePicker = new javafx.scene.control.DatePicker();
            startDatePicker.setPromptText("Start Date");
            startDatePicker.setPrefWidth(140);
            startDatePicker.setStyle("-fx-font-size: 13px;");
            javafx.scene.control.DatePicker endDatePicker = new javafx.scene.control.DatePicker();
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
            allUsersCb.setSelected(true);
            allUsersCb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");

            VBox userScroll = new VBox(8);
            userScroll.setPadding(new Insets(5, 5, 5, 20));

            java.util.List<javafx.scene.control.CheckBox> userCbs = new java.util.ArrayList<>();
            java.util.Set<String> userNames = new java.util.LinkedHashSet<>(filterOptions.usernames());

            for (String uName : userNames) {
                if (uName.trim().isEmpty()) continue;
                javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(uName);
                cb.setSelected(true);
                cb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                cb.setOnAction(e -> {
                    if (!cb.isSelected()) allUsersCb.setSelected(false);
                    else {
                        boolean all = true;
                        for (javafx.scene.control.CheckBox c : userCbs) if (!c.isSelected()) all = false;
                        allUsersCb.setSelected(all);
                    }
                });
                userCbs.add(cb);
                userScroll.getChildren().add(cb);
            }
            allUsersCb.setOnAction(e -> {
                boolean sel = allUsersCb.isSelected();
                for (javafx.scene.control.CheckBox cb : userCbs) cb.setSelected(sel);
            });
            FilterDisclosureSection userSection = new FilterDisclosureSection(allUsersCb, userScroll);

            javafx.scene.control.Separator sepUser = new javafx.scene.control.Separator();
            Label typeTitle = new Label("Transaction Type");
            typeTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");

            javafx.scene.control.CheckBox allTypesCb = new javafx.scene.control.CheckBox("All Types");
            allTypesCb.setSelected(true);
            allTypesCb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");

            VBox typeScroll = new VBox(8);
            typeScroll.setPadding(new Insets(5, 5, 5, 20));

            java.util.List<javafx.scene.control.CheckBox> typeCbs = new java.util.ArrayList<>();
            String[] types = {"IMPORT", "CANCEL_IMPORT", "SALE", "CANCEL_SALE", "RETURN", "MANUAL_ADJUST", "REVALUE", "DELETE"};
            for (String type : types) {
                javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(formatTransactionTypeLabel(type));
                cb.setUserData(type);
                cb.setSelected(true);
                cb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                cb.setOnAction(e -> {
                    if (!cb.isSelected()) allTypesCb.setSelected(false);
                    else {
                        boolean all = true;
                        for (javafx.scene.control.CheckBox c : typeCbs) if (!c.isSelected()) all = false;
                        allTypesCb.setSelected(all);
                    }
                });
                typeCbs.add(cb);
                typeScroll.getChildren().add(cb);
            }

            allTypesCb.setOnAction(e -> {
                boolean sel = allTypesCb.isSelected();
                for (javafx.scene.control.CheckBox cb : typeCbs) cb.setSelected(sel);
            });
            FilterDisclosureSection typeSection = new FilterDisclosureSection(allTypesCb, typeScroll);

            javafx.scene.control.Separator sepType = new javafx.scene.control.Separator();
            Label qtyTitle = new Label("Quantity Change Range");
            qtyTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");

            double maxQty = filterOptions.maxAbsoluteQuantity();
            if (maxQty == 0) maxQty = 100;

            Label qtyLabel = new Label("0 - " + String.format("%.0f", maxQty));
            qtyLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -app-primary; -fx-font-weight: bold;");

            RangeSlider qtySlider = new RangeSlider(0, maxQty, 0, maxQty, 280);
            qtySlider.minVal.addListener((o, ov, nv) -> qtyLabel.setText(String.format("%.0f - %.0f", nv.doubleValue(), qtySlider.maxVal.get())));
            qtySlider.maxVal.addListener((o, ov, nv) -> qtyLabel.setText(String.format("%.0f - %.0f", qtySlider.minVal.get(), nv.doubleValue())));

            scrollContent.getChildren().addAll(
                dateTitle, dateBox, sepDate,
                userTitle, userSection.getNode(), sepUser,
                typeTitle, typeSection.getNode(), sepType,
                qtyTitle, qtyLabel, qtySlider
            );
            javafx.scene.layout.HBox btnRow = new javafx.scene.layout.HBox(10);
            btnRow.setAlignment(Pos.CENTER_RIGHT);

            final double fMaxQty = maxQty;

            Button resetBtn = new Button("Reset");
            resetBtn.getStyleClass().add("filter-reset-btn");
            resetBtn.setOnAction(ae -> {
                hFilterBox.setStyle("");
                startDatePicker.setValue(null);
                endDatePicker.setValue(null);
                allUsersCb.setSelected(true);
                for (javafx.scene.control.CheckBox cb : userCbs) cb.setSelected(true);
                userSection.setExpanded(false);
                allTypesCb.setSelected(true);
                for (javafx.scene.control.CheckBox cb : typeCbs) cb.setSelected(true);
                typeSection.setExpanded(false);
                qtySlider.minVal.set(0); qtySlider.maxVal.set(fMaxQty);
                txStartDateRef.set(null);
                txEndDateRef.set(null);
                txUsersRef.set(new java.util.LinkedHashSet<>());
                txTypesRef.set(new java.util.LinkedHashSet<>());
                txMinAbsQtyRef.set(null);
                txMaxAbsQtyRef.set(null);
                txCurrentPage[0] = 0;
                loadTransactionPage.run();
                hFilterPopup.hide();
            });

            Button applyBtn = new Button("Apply Filter");
            applyBtn.getStyleClass().add("filter-apply-btn");
            applyBtn.setOnAction(ae -> {
                java.util.Set<String> selectedUsers = new java.util.HashSet<>();
                for (javafx.scene.control.CheckBox cb : userCbs) {
                    if (cb.isSelected()) selectedUsers.add(cb.getText());
                }

                java.util.Set<String> selectedTypes = new java.util.HashSet<>();
                for (javafx.scene.control.CheckBox cb : typeCbs) {
                    if (cb.isSelected()) selectedTypes.add((String) cb.getUserData());
                }
                double qMin = qtySlider.minVal.get();
                double qMax = qtySlider.maxVal.get();
                java.time.LocalDate sDate = startDatePicker.getValue();
                java.time.LocalDate eDate = endDatePicker.getValue();

                txStartDateRef.set(sDate);
                txEndDateRef.set(eDate);
                txUsersRef.set(allUsersCb.isSelected() ? new java.util.LinkedHashSet<>() : selectedUsers);
                txTypesRef.set(allTypesCb.isSelected() ? new java.util.LinkedHashSet<>() : selectedTypes);
                txMinAbsQtyRef.set(qMin <= 0 ? null : qMin);
                txMaxAbsQtyRef.set(qMax >= fMaxQty ? null : qMax);
                txCurrentPage[0] = 0;
                loadTransactionPage.run();

                boolean hasFilter = !allTypesCb.isSelected() || !allUsersCb.isSelected() || qMin > 0 || qMax < fMaxQty || sDate != null || eDate != null;
                hFilterBox.setStyle(hasFilter ? "-fx-border-color: -app-primary;" : "");
                hFilterPopup.hide();
            });

            btnRow.getChildren().addAll(resetBtn, applyBtn);

            popupContainer.getChildren().addAll(scrollPane, btnRow);
            hFilterPopup.getContent().clear();
            hFilterPopup.getContent().add(popupContainer);

            context.support().showPopupBelow(hFilterPopup, hFilterBox, -290, 5);
            };

            InventoryAuditFilterOptions cachedOptions = txFilterOptionsCache.get();
            if (cachedOptions != null) {
                showFilterContent.accept(cachedOptions);
                return;
            }

            hFilterPopup.getContent().setAll(FilterControlFactory.loadingContainer(350, "Loading filters..."));
            context.support().showPopupBelow(hFilterPopup, hFilterBox, -290, 5);

            javafx.concurrent.Task<InventoryAuditFilterOptions> task = new javafx.concurrent.Task<>() {
                @Override
                protected InventoryAuditFilterOptions call() {
                    return new InventoryAuditFilterOptions(
                        context.transactionService().getTransactionUsernames(user),
                        context.transactionService().getTransactionMaxAbsoluteQuantity(user)
                    );
                }
            };
            task.setOnSucceeded(taskEvent -> {
                InventoryAuditFilterOptions optionsValue = task.getValue();
                txFilterOptionsCache.set(optionsValue);
                if (hFilterPopup.isShowing()) {
                    showFilterContent.accept(optionsValue);
                }
            });
            task.setOnFailed(taskEvent -> {
                hFilterPopup.hide();
                context.showUserFacingError(task.getException());
            });
            Thread worker = new Thread(task, "inventory-audit-filter-options-loader");
            worker.setDaemon(true);
            worker.start();
        });

        root.getChildren().addAll(topBar, table, txStatusBar);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);

        table.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<com.pbl3.project.pbl3_project.entity.InventoryTransaction>) c -> updateTxStatusBar.run());
        javafx.application.Platform.runLater(loadTransactionPage);
        TableViewSupport.enableDeselectOnOutsideClick(root, table);
        return root;
    }

    private VBox createOperationalAuditView() {
        VBox root = new VBox();
        applyStandardTablePageLayout(root, Insets.EMPTY);
        final String operationalAuditSortStateKey = "operational-audit";
        TableSortState operationalAuditSortState = getOrCreateTableSortState(
            operationalAuditSortStateKey,
            new SortCriterion("createdAt", javafx.scene.control.TableColumn.SortType.DESCENDING)
        );
        java.util.LinkedHashMap<String, String> operationalAuditSortProperties = new java.util.LinkedHashMap<>();
        operationalAuditSortProperties.put("createdAt", "createdAt");
        operationalAuditSortProperties.put("actorUsername", "actor.username");
        operationalAuditSortProperties.put("action", "action");
        operationalAuditSortProperties.put("subjectType", "subjectType");
        operationalAuditSortProperties.put("subjectLabel", "subjectLabel");
        java.util.LinkedHashMap<String, String> operationalAuditSortLabels = new java.util.LinkedHashMap<>();
        operationalAuditSortLabels.put("createdAt", "Date");
        operationalAuditSortLabels.put("actorUsername", "Actor");
        operationalAuditSortLabels.put("action", "Action");
        operationalAuditSortLabels.put("subjectType", "Subject");
        operationalAuditSortLabels.put("subjectLabel", "Label");

        ExpandableSearchControl searchControl = createExpandableSearchControl(340);
        TextField searchField = searchControl.field();

        javafx.scene.layout.HBox filterBox = new javafx.scene.layout.HBox();
        filterBox.setAlignment(Pos.CENTER);
        filterBox.getStyleClass().add("expandable-search-box");
        filterBox.setPrefSize(40, 40);
        filterBox.setMinSize(40, 40);
        filterBox.setMaxSize(40, 40);
        filterBox.setCursor(javafx.scene.Cursor.HAND);
        javafx.scene.shape.SVGPath filterIcon = new javafx.scene.shape.SVGPath();
        filterIcon.setContent("M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z");
        filterIcon.setFill(PRIMARY_COLOR);
        filterBox.getChildren().add(filterIcon);
        javafx.scene.control.Tooltip.install(filterBox, new javafx.scene.control.Tooltip("Filter"));

        javafx.scene.layout.HBox rightBox = new javafx.scene.layout.HBox(12, filterBox, searchControl.box());
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        javafx.scene.layout.BorderPane topBar = new javafx.scene.layout.BorderPane();
        topBar.setRight(rightBox);

        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.OperationalAuditLog> table = new javafx.scene.control.TableView<>();
        applyStandardTableSizing(table);
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        TableViewSupport.enableDragSelection(table);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.OperationalAuditLog, String> dateCol = new javafx.scene.control.TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(context.support().formatDateTime(data.getValue().getCreatedAt())));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.OperationalAuditLog, String> actorCol = new javafx.scene.control.TableColumn<>("Actor");
        actorCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getActor() != null ? data.getValue().getActor().getUsername() : "System"
        ));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.OperationalAuditLog, String> actionCol = new javafx.scene.control.TableColumn<>("Action");
        actionCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatOperationalAuditActionLabel(data.getValue().getAction())));
        actionCol.setStyle("-fx-alignment: CENTER;");

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.OperationalAuditLog, String> subjectTypeCol = new javafx.scene.control.TableColumn<>("Subject");
        subjectTypeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatOperationalSubjectTypeLabel(data.getValue().getSubjectType())));
        subjectTypeCol.setStyle("-fx-alignment: CENTER;");

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.OperationalAuditLog, String> subjectLabelCol = new javafx.scene.control.TableColumn<>("Label");
        subjectLabelCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getSubjectLabel() == null ? "" : data.getValue().getSubjectLabel()
        ));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.OperationalAuditLog, String> detailsCol = new javafx.scene.control.TableColumn<>("Details");
        detailsCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getDetails() == null ? "" : data.getValue().getDetails()
        ));

        table.getColumns().addAll(dateCol, actorCol, actionCol, subjectTypeCol, subjectLabelCol, detailsCol);
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<com.pbl3.project.pbl3_project.entity.OperationalAuditLog> row =
                new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY
                    && event.getClickCount() == 2
                    && !row.isEmpty()) {
                    showOperationalAuditDetails(row.getItem());
                }
            });
            return row;
        });

        java.util.LinkedHashMap<String, javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.OperationalAuditLog, ?>> operationalAuditSortColumns =
            new java.util.LinkedHashMap<>();
        operationalAuditSortColumns.put("createdAt", dateCol);
        operationalAuditSortColumns.put("actorUsername", actorCol);
        operationalAuditSortColumns.put("action", actionCol);
        operationalAuditSortColumns.put("subjectType", subjectTypeCol);
        operationalAuditSortColumns.put("subjectLabel", subjectLabelCol);
        installSortHeaderIndicators(operationalAuditSortColumns);

        final int pageSize = 25;
        final int[] currentPage = {0};
        final int[] totalPages = {0};
        final long[] totalElements = {0L};
        java.util.concurrent.atomic.AtomicReference<String> searchRef = new java.util.concurrent.atomic.AtomicReference<>("");
        java.util.concurrent.atomic.AtomicReference<java.time.LocalDate> startDateRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<java.time.LocalDate> endDateRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<java.util.Set<String>> actorUsernamesRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<java.util.Set<com.pbl3.project.pbl3_project.entity.OperationalAuditAction>> actionsRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<java.util.Set<com.pbl3.project.pbl3_project.entity.OperationalSubjectType>> subjectTypesRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicLong pageLoadVersion = new java.util.concurrent.atomic.AtomicLong();
        AsyncPageCache<OperationalAuditPageResult> pageCache = new AsyncPageCache<>(80);

        Label rowCountLabel = createStatusMetaLabel(java.text.MessageFormat.format("Showing {0}-{1} of {2} Row(s)", 0, 0, 0));
        Label pageLabel = createStatusMetaLabel(java.text.MessageFormat.format("Page {0} / {1}", 0, 0));
        Button prevBtn = ButtonFactory.pageNav("Prev");
        Button nextBtn = ButtonFactory.pageNav("Next");

        Runnable updateStatusBar = () -> updatePagedStatus(
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
            java.time.LocalDate startDate = startDateRef.get();
            java.time.LocalDate endDate = endDateRef.get();
            java.util.Set<String> actorUsernames = new java.util.LinkedHashSet<>(actorUsernamesRef.get());
            java.util.Set<com.pbl3.project.pbl3_project.entity.OperationalAuditAction> actions =
                new java.util.LinkedHashSet<>(actionsRef.get());
            java.util.Set<com.pbl3.project.pbl3_project.entity.OperationalSubjectType> subjectTypes =
                new java.util.LinkedHashSet<>(subjectTypesRef.get());
            java.util.List<SortCriterion> sortSnapshot = operationalAuditSortState.snapshot();
            TableSortState sortForLoad = new TableSortState(sortSnapshot);
            java.util.function.IntFunction<Object> cacheKeyForPage = pageIndex -> java.util.Arrays.asList(
                pageIndex,
                search,
                startDate,
                endDate,
                actorUsernames,
                actions,
                subjectTypes,
                sortSnapshot
            );
            java.util.function.IntFunction<OperationalAuditPageResult> fetchOperationalAuditPage = pageIndex -> {
                int resolvedPage = pageIndex;
                org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.OperationalAuditLog> pageData =
                    context.operationalAuditLogService().searchOperationalAuditLogs(
                        user,
                        search,
                        startDate,
                        endDate,
                        actorUsernames,
                        actions,
                        subjectTypes,
                        createPageable(sortForLoad, operationalAuditSortProperties, resolvedPage, pageSize)
                    );
                if (pageData.getTotalPages() > 0 && resolvedPage >= pageData.getTotalPages()) {
                    resolvedPage = pageData.getTotalPages() - 1;
                    pageData = context.operationalAuditLogService().searchOperationalAuditLogs(
                        user,
                        search,
                        startDate,
                        endDate,
                        actorUsernames,
                        actions,
                        subjectTypes,
                        createPageable(sortForLoad, operationalAuditSortProperties, resolvedPage, pageSize)
                    );
                }
                return new OperationalAuditPageResult(pageData, resolvedPage);
            };

            AsyncUiTask.runLatestCachedTableLoad(
                table,
                prevBtn,
                nextBtn,
                pageLoadVersion,
                pageCache,
                cacheKeyForPage.apply(requestedPage),
                () -> fetchOperationalAuditPage.apply(requestedPage),
                result -> {
                    org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.OperationalAuditLog> pageData = result.page();
                    currentPage[0] = result.pageIndex();
                    totalElements[0] = pageData.getTotalElements();
                    totalPages[0] = pageData.getTotalPages();
                    table.setItems(javafx.collections.FXCollections.observableArrayList(pageData.getContent()));
                    updateStatusBar.run();
                    int nextPage = result.pageIndex() + 1;
                    if (nextPage < totalPages[0]) {
                        pageCache.prefetch(
                            cacheKeyForPage.apply(nextPage),
                            () -> fetchOperationalAuditPage.apply(nextPage),
                            null,
                            "operational-audit-next-page-prefetch"
                        );
                    }
                    int previousPage = result.pageIndex() - 1;
                    if (previousPage >= 0) {
                        pageCache.prefetch(
                            cacheKeyForPage.apply(previousPage),
                            () -> fetchOperationalAuditPage.apply(previousPage),
                            null,
                            "operational-audit-prev-page-prefetch"
                        );
                    }
                },
                context::showUserFacingError,
                "Loading audit logs...",
                "Could not load audit logs",
                "operational-audit-page-loader"
            );
        };
        Label operationalAuditSortStatusLabel = createSortStatusLabel(operationalAuditSortState, operationalAuditSortLabels);
        Runnable applyOperationalAuditSortUi = () -> {
            applySortStateToTable(table, operationalAuditSortColumns, operationalAuditSortState);
            operationalAuditSortStatusLabel.setText(buildSortStatusText(operationalAuditSortState, operationalAuditSortLabels));
        };
        applyOperationalAuditSortUi.run();
        installManualServerSorting(
            table,
            operationalAuditSortColumns,
            operationalAuditSortState,
            () -> {
                applyOperationalAuditSortUi.run();
                currentPage[0] = 0;
                loadPage.run();
            }
        );

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

        javafx.animation.PauseTransition searchPause = new javafx.animation.PauseTransition(Duration.millis(220));
        searchPause.setOnFinished(e -> {
            currentPage[0] = 0;
            searchRef.set(searchField.getText());
            loadPage.run();
        });
        searchField.textProperty().addListener((obs, oldV, newV) -> searchPause.playFromStart());

        javafx.scene.layout.HBox statusBar = new javafx.scene.layout.HBox(15, operationalAuditSortStatusLabel, rowCountLabel, pageLabel, prevBtn, nextBtn);
        applyStandardTableStatusBar(statusBar);

        javafx.stage.Popup filterPopup = new javafx.stage.Popup();
        filterPopup.setAutoHide(true);
        filterBox.setOnMouseClicked(event -> {
            if (filterPopup.isShowing()) {
                filterPopup.hide();
                return;
            }

            FilterControlFactory.Shell filterShell = FilterControlFactory.shell(360, 250);
            VBox popupContainer = filterShell.container();
            VBox scrollContent = filterShell.content();

            Label dateTitle = FilterControlFactory.sectionTitle("Date Range");
            javafx.scene.control.DatePicker startDatePicker = new javafx.scene.control.DatePicker(startDateRef.get());
            startDatePicker.setPromptText("Start Date");
            startDatePicker.setPrefWidth(140);
            javafx.scene.control.DatePicker endDatePicker = new javafx.scene.control.DatePicker(endDateRef.get());
            endDatePicker.setPromptText("End Date");
            endDatePicker.setPrefWidth(140);
            context.support().customizeDatePicker(startDatePicker);
            context.support().customizeDatePicker(endDatePicker);
            javafx.scene.layout.HBox dateBox = new javafx.scene.layout.HBox(5, startDatePicker, new Label("-"), endDatePicker);
            dateBox.setAlignment(Pos.CENTER_LEFT);

            Label actorTitle = FilterControlFactory.sectionTitle("Actor");
            javafx.scene.control.ComboBox<String> actorCombo = new javafx.scene.control.ComboBox<>();
            actorCombo.getItems().add("All Actors");
            actorCombo.getItems().addAll(context.operationalAuditLogService().getActorUsernames(user));
            actorCombo.setValue(actorUsernamesRef.get().isEmpty() ? "All Actors" : actorUsernamesRef.get().iterator().next());
            actorCombo.setMaxWidth(Double.MAX_VALUE);

            Label actionTitle = FilterControlFactory.sectionTitle("Action");
            javafx.scene.control.ComboBox<String> actionCombo = new javafx.scene.control.ComboBox<>();
            java.util.Map<String, com.pbl3.project.pbl3_project.entity.OperationalAuditAction> actionLookup = new java.util.LinkedHashMap<>();
            actionCombo.getItems().add("All Actions");
            for (com.pbl3.project.pbl3_project.entity.OperationalAuditAction action : com.pbl3.project.pbl3_project.entity.OperationalAuditAction.values()) {
                String label = formatOperationalAuditActionLabel(action);
                actionLookup.put(label, action);
                actionCombo.getItems().add(label);
            }
            actionCombo.setValue(actionsRef.get().isEmpty() ? "All Actions" : formatOperationalAuditActionLabel(actionsRef.get().iterator().next()));
            actionCombo.setMaxWidth(Double.MAX_VALUE);

            Label subjectTitle = FilterControlFactory.sectionTitle("Subject");
            javafx.scene.control.ComboBox<String> subjectCombo = new javafx.scene.control.ComboBox<>();
            java.util.Map<String, com.pbl3.project.pbl3_project.entity.OperationalSubjectType> subjectLookup = new java.util.LinkedHashMap<>();
            subjectCombo.getItems().add("All Subjects");
            for (com.pbl3.project.pbl3_project.entity.OperationalSubjectType type : com.pbl3.project.pbl3_project.entity.OperationalSubjectType.values()) {
                String label = formatOperationalSubjectTypeLabel(type);
                subjectLookup.put(label, type);
                subjectCombo.getItems().add(label);
            }
            subjectCombo.setValue(subjectTypesRef.get().isEmpty() ? "All Subjects" : formatOperationalSubjectTypeLabel(subjectTypesRef.get().iterator().next()));
            subjectCombo.setMaxWidth(Double.MAX_VALUE);

            Button resetBtn = new Button("Reset");
            resetBtn.getStyleClass().add("filter-reset-btn");
            resetBtn.setOnAction(e -> {
                filterBox.setStyle("");
                startDateRef.set(null);
                endDateRef.set(null);
                actorUsernamesRef.set(new java.util.LinkedHashSet<>());
                actionsRef.set(new java.util.LinkedHashSet<>());
                subjectTypesRef.set(new java.util.LinkedHashSet<>());
                currentPage[0] = 0;
                loadPage.run();
                filterPopup.hide();
            });

            Button applyBtn = new Button("Apply Filter");
            applyBtn.getStyleClass().add("filter-apply-btn");
            applyBtn.setOnAction(e -> {
                startDateRef.set(startDatePicker.getValue());
                endDateRef.set(endDatePicker.getValue());

                java.util.Set<String> actorFilters = new java.util.LinkedHashSet<>();
                if (!"All Actors".equals(actorCombo.getValue())) {
                    actorFilters.add(actorCombo.getValue());
                }
                actorUsernamesRef.set(actorFilters);

                java.util.Set<com.pbl3.project.pbl3_project.entity.OperationalAuditAction> actionFilters = new java.util.LinkedHashSet<>();
                if (!"All Actions".equals(actionCombo.getValue())) {
                    actionFilters.add(actionLookup.get(actionCombo.getValue()));
                }
                actionsRef.set(actionFilters);

                java.util.Set<com.pbl3.project.pbl3_project.entity.OperationalSubjectType> subjectFilters = new java.util.LinkedHashSet<>();
                if (!"All Subjects".equals(subjectCombo.getValue())) {
                    subjectFilters.add(subjectLookup.get(subjectCombo.getValue()));
                }
                subjectTypesRef.set(subjectFilters);

                currentPage[0] = 0;
                loadPage.run();
                boolean hasFilter = startDateRef.get() != null
                    || endDateRef.get() != null
                    || !actorUsernamesRef.get().isEmpty()
                    || !actionsRef.get().isEmpty()
                    || !subjectTypesRef.get().isEmpty();
                filterBox.setStyle(hasFilter ? "-fx-border-color: -app-primary;" : "");
                filterPopup.hide();
            });

            javafx.scene.layout.HBox buttonRow = FilterControlFactory.actionRow(resetBtn, applyBtn);

            scrollContent.getChildren().addAll(
                dateTitle, dateBox,
                new javafx.scene.control.Separator(),
                actorTitle, actorCombo,
                actionTitle, actionCombo,
                subjectTitle, subjectCombo,
                new javafx.scene.control.Separator()
            );
            popupContainer.getChildren().add(buttonRow);
            filterPopup.getContent().clear();
            filterPopup.getContent().add(popupContainer);

            context.support().showPopupBelow(filterPopup, filterBox, -280, 5);
        });

        root.getChildren().addAll(topBar, table, statusBar);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        javafx.application.Platform.runLater(loadPage);
        TableViewSupport.enableDeselectOnOutsideClick(root, table);
        return root;
    }





    private VBox createAccountAuditView() {
        VBox root = new VBox();
        applyStandardTablePageLayout(root, Insets.EMPTY);
        final String accountAuditSortStateKey = "account-audit";
        TableSortState accountAuditSortState = getOrCreateTableSortState(
            accountAuditSortStateKey,
            new SortCriterion("createdAt", javafx.scene.control.TableColumn.SortType.DESCENDING)
        );
        java.util.LinkedHashMap<String, String> accountAuditSortProperties = new java.util.LinkedHashMap<>();
        accountAuditSortProperties.put("createdAt", "createdAt");
        accountAuditSortProperties.put("actorUsername", "actor.username");
        accountAuditSortProperties.put("targetUsername", "targetUser.username");
        accountAuditSortProperties.put("action", "action");
        java.util.LinkedHashMap<String, String> accountAuditSortLabels = new java.util.LinkedHashMap<>();
        accountAuditSortLabels.put("createdAt", "Date");
        accountAuditSortLabels.put("actorUsername", "Actor");
        accountAuditSortLabels.put("targetUsername", "Target User");
        accountAuditSortLabels.put("action", "Action");

        ExpandableSearchControl searchControl = createExpandableSearchControl(320);
        TextField searchField = searchControl.field();

        javafx.scene.layout.HBox filterBox = new javafx.scene.layout.HBox();
        filterBox.setAlignment(Pos.CENTER);
        filterBox.getStyleClass().add("expandable-search-box");
        filterBox.setPrefSize(40, 40);
        filterBox.setMinSize(40, 40);
        filterBox.setMaxSize(40, 40);
        filterBox.setCursor(javafx.scene.Cursor.HAND);
        javafx.scene.shape.SVGPath filterIcon = new javafx.scene.shape.SVGPath();
        filterIcon.setContent("M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z");
        filterIcon.setFill(PRIMARY_COLOR);
        filterBox.getChildren().add(filterIcon);
        javafx.scene.control.Tooltip.install(filterBox, new javafx.scene.control.Tooltip("Filter"));

        javafx.scene.layout.HBox rightBox = new javafx.scene.layout.HBox(12, filterBox, searchControl.box());
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        javafx.scene.layout.BorderPane topBar = new javafx.scene.layout.BorderPane();
        topBar.setRight(rightBox);

        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.AccountAuditLog> table = new javafx.scene.control.TableView<>();
        applyStandardTableSizing(table);
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        TableViewSupport.enableDragSelection(table);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.AccountAuditLog, String> dateCol = new javafx.scene.control.TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(context.support().formatDateTime(data.getValue().getCreatedAt())));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.AccountAuditLog, String> actorCol = new javafx.scene.control.TableColumn<>("Actor");
        actorCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getActor() != null ? data.getValue().getActor().getUsername() : "System"
        ));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.AccountAuditLog, String> targetCol = new javafx.scene.control.TableColumn<>("Target User");
        targetCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getTargetUser() != null ? data.getValue().getTargetUser().getUsername() : "-"
        ));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.AccountAuditLog, String> actionCol = new javafx.scene.control.TableColumn<>("Action");
        actionCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatAccountAuditActionLabel(data.getValue().getAction())));
        actionCol.setStyle("-fx-alignment: CENTER;");

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.AccountAuditLog, String> detailsCol = new javafx.scene.control.TableColumn<>("Details");
        detailsCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getDetails() == null ? "" : data.getValue().getDetails()
        ));

        table.getColumns().addAll(dateCol, actorCol, targetCol, actionCol, detailsCol);
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<com.pbl3.project.pbl3_project.entity.AccountAuditLog> row =
                new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY
                    && event.getClickCount() == 2
                    && !row.isEmpty()) {
                    showAccountAuditDetails(row.getItem());
                }
            });
            return row;
        });
        java.util.LinkedHashMap<String, javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.AccountAuditLog, ?>> accountAuditSortColumns =
            new java.util.LinkedHashMap<>();
        accountAuditSortColumns.put("createdAt", dateCol);
        accountAuditSortColumns.put("actorUsername", actorCol);
        accountAuditSortColumns.put("targetUsername", targetCol);
        accountAuditSortColumns.put("action", actionCol);
        installSortHeaderIndicators(accountAuditSortColumns);

        final int pageSize = 25;
        final int[] currentPage = {0};
        final int[] totalPages = {0};
        final long[] totalElements = {0L};
        java.util.concurrent.atomic.AtomicReference<String> searchRef = new java.util.concurrent.atomic.AtomicReference<>("");
        java.util.concurrent.atomic.AtomicReference<java.time.LocalDate> startDateRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<java.time.LocalDate> endDateRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<java.util.Set<String>> actorUsernamesRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<java.util.Set<String>> targetUsernamesRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<java.util.Set<com.pbl3.project.pbl3_project.entity.AccountAuditAction>> actionsRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicLong pageLoadVersion = new java.util.concurrent.atomic.AtomicLong();
        AsyncPageCache<AccountAuditPageResult> pageCache = new AsyncPageCache<>(80);

        Label rowCountLabel = createStatusMetaLabel(java.text.MessageFormat.format("Showing {0}-{1} of {2} Row(s)", 0, 0, 0));
        Label pageLabel = createStatusMetaLabel(java.text.MessageFormat.format("Page {0} / {1}", 0, 0));
        Button prevBtn = ButtonFactory.pageNav("Prev");
        Button nextBtn = ButtonFactory.pageNav("Next");

        Runnable updateStatusBar = () -> updatePagedStatus(
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
            java.time.LocalDate startDate = startDateRef.get();
            java.time.LocalDate endDate = endDateRef.get();
            java.util.Set<String> actorUsernames = new java.util.LinkedHashSet<>(actorUsernamesRef.get());
            java.util.Set<String> targetUsernames = new java.util.LinkedHashSet<>(targetUsernamesRef.get());
            java.util.Set<com.pbl3.project.pbl3_project.entity.AccountAuditAction> actions =
                new java.util.LinkedHashSet<>(actionsRef.get());
            java.util.List<SortCriterion> sortSnapshot = accountAuditSortState.snapshot();
            TableSortState sortForLoad = new TableSortState(sortSnapshot);
            java.util.function.IntFunction<Object> cacheKeyForPage = pageIndex -> java.util.Arrays.asList(
                pageIndex,
                search,
                startDate,
                endDate,
                actorUsernames,
                targetUsernames,
                actions,
                sortSnapshot
            );
            java.util.function.IntFunction<AccountAuditPageResult> fetchAccountAuditPage = pageIndex -> {
                int resolvedPage = pageIndex;
                org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.AccountAuditLog> pageData =
                    context.accountAuditLogService().searchAccountAuditLogs(
                        user,
                        search,
                        startDate,
                        endDate,
                        actorUsernames,
                        targetUsernames,
                        actions,
                        createPageable(sortForLoad, accountAuditSortProperties, resolvedPage, pageSize)
                    );
                if (pageData.getTotalPages() > 0 && resolvedPage >= pageData.getTotalPages()) {
                    resolvedPage = pageData.getTotalPages() - 1;
                    pageData = context.accountAuditLogService().searchAccountAuditLogs(
                        user,
                        search,
                        startDate,
                        endDate,
                        actorUsernames,
                        targetUsernames,
                        actions,
                        createPageable(sortForLoad, accountAuditSortProperties, resolvedPage, pageSize)
                    );
                }
                return new AccountAuditPageResult(pageData, resolvedPage);
            };

            AsyncUiTask.runLatestCachedTableLoad(
                table,
                prevBtn,
                nextBtn,
                pageLoadVersion,
                pageCache,
                cacheKeyForPage.apply(requestedPage),
                () -> fetchAccountAuditPage.apply(requestedPage),
                result -> {
                    org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.AccountAuditLog> pageData = result.page();
                    currentPage[0] = result.pageIndex();
                    totalElements[0] = pageData.getTotalElements();
                    totalPages[0] = pageData.getTotalPages();
                    table.setItems(javafx.collections.FXCollections.observableArrayList(pageData.getContent()));
                    updateStatusBar.run();
                    int nextPage = result.pageIndex() + 1;
                    if (nextPage < totalPages[0]) {
                        pageCache.prefetch(
                            cacheKeyForPage.apply(nextPage),
                            () -> fetchAccountAuditPage.apply(nextPage),
                            null,
                            "account-audit-next-page-prefetch"
                        );
                    }
                    int previousPage = result.pageIndex() - 1;
                    if (previousPage >= 0) {
                        pageCache.prefetch(
                            cacheKeyForPage.apply(previousPage),
                            () -> fetchAccountAuditPage.apply(previousPage),
                            null,
                            "account-audit-prev-page-prefetch"
                        );
                    }
                },
                context::showUserFacingError,
                "Loading account audit...",
                "Could not load account audit",
                "account-audit-page-loader"
            );
        };
        Label accountAuditSortStatusLabel = createSortStatusLabel(accountAuditSortState, accountAuditSortLabels);
        Runnable applyAccountAuditSortUi = () -> {
            applySortStateToTable(table, accountAuditSortColumns, accountAuditSortState);
            accountAuditSortStatusLabel.setText(buildSortStatusText(accountAuditSortState, accountAuditSortLabels));
        };
        applyAccountAuditSortUi.run();
        installManualServerSorting(
            table,
            accountAuditSortColumns,
            accountAuditSortState,
            () -> {
                applyAccountAuditSortUi.run();
                currentPage[0] = 0;
                loadPage.run();
            }
        );

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

        javafx.animation.PauseTransition searchPause = new javafx.animation.PauseTransition(Duration.millis(220));
        searchPause.setOnFinished(e -> {
            currentPage[0] = 0;
            searchRef.set(searchField.getText());
            loadPage.run();
        });
        searchField.textProperty().addListener((obs, oldV, newV) -> searchPause.playFromStart());

        javafx.scene.layout.HBox statusBar = new javafx.scene.layout.HBox(15, accountAuditSortStatusLabel, rowCountLabel, pageLabel, prevBtn, nextBtn);
        applyStandardTableStatusBar(statusBar);

        javafx.stage.Popup filterPopup = new javafx.stage.Popup();
        filterPopup.setAutoHide(true);

        filterBox.setOnMouseClicked(event -> {
            if (filterPopup.isShowing()) {
                filterPopup.hide();
                return;
            }

            FilterControlFactory.Shell filterShell = FilterControlFactory.shell(360, 250);
            VBox popupContainer = filterShell.container();
            VBox scrollContent = filterShell.content();

            Label dateTitle = FilterControlFactory.sectionTitle("Date Range");
            javafx.scene.control.DatePicker startDatePicker = new javafx.scene.control.DatePicker(startDateRef.get());
            startDatePicker.setPromptText("Start Date");
            startDatePicker.setPrefWidth(140);
            startDatePicker.setStyle("-fx-font-size: 13px;");
            javafx.scene.control.DatePicker endDatePicker = new javafx.scene.control.DatePicker(endDateRef.get());
            endDatePicker.setPromptText("End Date");
            endDatePicker.setPrefWidth(140);
            endDatePicker.setStyle("-fx-font-size: 13px;");
            context.support().customizeDatePicker(startDatePicker);
            context.support().customizeDatePicker(endDatePicker);
            javafx.scene.layout.HBox dateBox = new javafx.scene.layout.HBox(5, startDatePicker, new Label("-"), endDatePicker);
            dateBox.setAlignment(Pos.CENTER_LEFT);

            Label actorTitle = FilterControlFactory.sectionTitle("Actor");
            javafx.scene.control.ComboBox<String> actorCombo = new javafx.scene.control.ComboBox<>();
            actorCombo.getItems().add("All Actors");
            actorCombo.getItems().addAll(context.accountAuditLogService().getActorUsernames(user));
            actorCombo.setValue(actorUsernamesRef.get().isEmpty() ? "All Actors" : actorUsernamesRef.get().iterator().next());
            actorCombo.setMaxWidth(Double.MAX_VALUE);

            Label targetTitle = FilterControlFactory.sectionTitle("Target User");
            javafx.scene.control.ComboBox<String> targetCombo = new javafx.scene.control.ComboBox<>();
            targetCombo.getItems().add("All Targets");
            targetCombo.getItems().addAll(context.accountAuditLogService().getTargetUsernames(user));
            targetCombo.setValue(targetUsernamesRef.get().isEmpty() ? "All Targets" : targetUsernamesRef.get().iterator().next());
            targetCombo.setMaxWidth(Double.MAX_VALUE);

            Label actionTitle = FilterControlFactory.sectionTitle("Action");
            javafx.scene.control.ComboBox<String> actionCombo = new javafx.scene.control.ComboBox<>();
            java.util.Map<String, com.pbl3.project.pbl3_project.entity.AccountAuditAction> actionLookup = new java.util.LinkedHashMap<>();
            actionCombo.getItems().add("All Actions");
            for (com.pbl3.project.pbl3_project.entity.AccountAuditAction action : com.pbl3.project.pbl3_project.entity.AccountAuditAction.values()) {
                String label = formatAccountAuditActionLabel(action);
                actionLookup.put(label, action);
                actionCombo.getItems().add(label);
            }
            actionCombo.setValue(actionsRef.get().isEmpty()
                ? "All Actions"
                : formatAccountAuditActionLabel(actionsRef.get().iterator().next()));
            actionCombo.setMaxWidth(Double.MAX_VALUE);

            Button resetBtn = new Button("Reset");
            resetBtn.getStyleClass().add("filter-reset-btn");
            resetBtn.setOnAction(ae -> {
                filterBox.setStyle("");
                startDateRef.set(null);
                endDateRef.set(null);
                actorUsernamesRef.set(new java.util.LinkedHashSet<>());
                targetUsernamesRef.set(new java.util.LinkedHashSet<>());
                actionsRef.set(new java.util.LinkedHashSet<>());
                currentPage[0] = 0;
                loadPage.run();
                filterPopup.hide();
            });

            Button applyBtn = new Button("Apply Filter");
            applyBtn.getStyleClass().add("filter-apply-btn");
            applyBtn.setOnAction(ae -> {
                java.time.LocalDate startDate = startDatePicker.getValue();
                java.time.LocalDate endDate = endDatePicker.getValue();
                if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
                    context.toastService().showWarning("End date must be after start date.");
                    return;
                }

                java.util.LinkedHashSet<String> selectedActors = new java.util.LinkedHashSet<>();
                java.util.LinkedHashSet<String> selectedTargets = new java.util.LinkedHashSet<>();
                java.util.LinkedHashSet<com.pbl3.project.pbl3_project.entity.AccountAuditAction> selectedActions = new java.util.LinkedHashSet<>();

                if (actorCombo.getValue() != null && !"All Actors".equals(actorCombo.getValue())) {
                    selectedActors.add(actorCombo.getValue());
                }
                if (targetCombo.getValue() != null && !"All Targets".equals(targetCombo.getValue())) {
                    selectedTargets.add(targetCombo.getValue());
                }
                if (actionCombo.getValue() != null && !"All Actions".equals(actionCombo.getValue())) {
                    com.pbl3.project.pbl3_project.entity.AccountAuditAction selectedAction = actionLookup.get(actionCombo.getValue());
                    if (selectedAction != null) {
                        selectedActions.add(selectedAction);
                    }
                }

                startDateRef.set(startDate);
                endDateRef.set(endDate);
                actorUsernamesRef.set(selectedActors);
                targetUsernamesRef.set(selectedTargets);
                actionsRef.set(selectedActions);
                currentPage[0] = 0;
                loadPage.run();

                boolean hasFilter = startDate != null
                    || endDate != null
                    || !selectedActors.isEmpty()
                    || !selectedTargets.isEmpty()
                    || !selectedActions.isEmpty();
                filterBox.setStyle(hasFilter ? "-fx-border-color: -app-primary;" : "");
                filterPopup.hide();
            });

            javafx.scene.layout.HBox buttonRow = FilterControlFactory.actionRow(resetBtn, applyBtn);
            scrollContent.getChildren().addAll(
                dateTitle, dateBox,
                new javafx.scene.control.Separator(),
                actorTitle, actorCombo,
                targetTitle, targetCombo,
                actionTitle, actionCombo,
                new javafx.scene.control.Separator()
            );
            popupContainer.getChildren().add(buttonRow);

            filterPopup.getContent().clear();
            filterPopup.getContent().add(popupContainer);
            context.support().showPopupBelow(filterPopup, filterBox, -280, 5);
        });

        root.getChildren().addAll(topBar, table, statusBar);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);

        table.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<com.pbl3.project.pbl3_project.entity.AccountAuditLog>) c -> updateStatusBar.run());
        javafx.application.Platform.runLater(loadPage);
        TableViewSupport.enableDeselectOnOutsideClick(root, table);
        return root;
    }

    private String formatAccountAuditActionLabel(com.pbl3.project.pbl3_project.entity.AccountAuditAction action) {
        return formatEnumWords(action == null ? null : action.name());
    }

    private void showInventoryAuditDetails(com.pbl3.project.pbl3_project.entity.InventoryTransaction tx) {
        if (tx == null) {
            return;
        }
        java.util.LinkedHashMap<String, String> fields = new java.util.LinkedHashMap<>();
        fields.put("ID", auditValue(tx.getId()));
        fields.put("Date", context.support().formatDateTime(tx.getCreatedAt()));
        fields.put("Action", formatTransactionTypeLabel(tx.getTransactionType()));
        fields.put("Product", tx.getProduct() != null ? auditValue(tx.getProduct().getName()) : "-");
        fields.put("Quantity change", formatSignedInteger(tx.getQuantityChange()));
        fields.put("Created by", formatAuditUser(tx.getUser()));
        fields.put("Reference ID", auditValue(tx.getReferenceId()));
        fields.put("Unit cost", tx.getUnitCostSnapshot() != null ? context.support().formatVnd(tx.getUnitCostSnapshot()) : "-");
        fields.put("Inventory value change", tx.getInventoryValueChange() != null ? context.support().formatVnd(tx.getInventoryValueChange()) : "-");
        showAuditDetailDialog(
            java.text.MessageFormat.format("Inventory Audit #{0}", tx.getId()),
            "Inventory Audit",
            fields,
            "Notes",
            tx.getNotes()
        );
    }

    private void showOperationalAuditDetails(com.pbl3.project.pbl3_project.entity.OperationalAuditLog log) {
        if (log == null) {
            return;
        }
        java.util.LinkedHashMap<String, String> fields = new java.util.LinkedHashMap<>();
        fields.put("ID", auditValue(log.getId()));
        fields.put("Date", context.support().formatDateTime(log.getCreatedAt()));
        fields.put("Actor", formatAuditUser(log.getActor()));
        fields.put("Action", formatOperationalAuditActionLabel(log.getAction()));
        fields.put("Subject", formatOperationalSubjectTypeLabel(log.getSubjectType()));
        fields.put("Subject ID", auditValue(log.getSubjectId()));
        fields.put("Subject label", auditValue(log.getSubjectLabel()));
        LinkedHashMap<String, String> detailSections = new LinkedHashMap<>();
        detailSections.put("Details", log.getDetails());
        if (hasAuditText(log.getBeforeJson())) {
            detailSections.put("Before JSON", prettyAuditJson(log.getBeforeJson()));
        }
        if (hasAuditText(log.getAfterJson())) {
            detailSections.put("After JSON", prettyAuditJson(log.getAfterJson()));
        }
        showAuditDetailDialog(
            java.text.MessageFormat.format("Operational Audit #{0}", log.getId()),
            "Operational Audit",
            fields,
            detailSections
        );
    }

    private void showAccountAuditDetails(com.pbl3.project.pbl3_project.entity.AccountAuditLog log) {
        if (log == null) {
            return;
        }
        java.util.LinkedHashMap<String, String> fields = new java.util.LinkedHashMap<>();
        fields.put("ID", auditValue(log.getId()));
        fields.put("Date", context.support().formatDateTime(log.getCreatedAt()));
        fields.put("Actor", formatAuditUser(log.getActor()));
        fields.put("Target user", formatAuditUser(log.getTargetUser()));
        fields.put("Action", formatAccountAuditActionLabel(log.getAction()));
        LinkedHashMap<String, String> detailSections = new LinkedHashMap<>();
        detailSections.put("Details", log.getDetails());
        if (hasAuditText(log.getBeforeJson())) {
            detailSections.put("Before JSON", prettyAuditJson(log.getBeforeJson()));
        }
        if (hasAuditText(log.getAfterJson())) {
            detailSections.put("After JSON", prettyAuditJson(log.getAfterJson()));
        }
        showAuditDetailDialog(
            java.text.MessageFormat.format("Account Audit #{0}", log.getId()),
            "Account Audit",
            fields,
            detailSections
        );
    }

    private void showAuditDetailDialog(
        String windowTitle,
        String title,
        java.util.LinkedHashMap<String, String> fields,
        String detailTitle,
        String detailText
    ) {
        LinkedHashMap<String, String> detailSections = new LinkedHashMap<>();
        detailSections.put(detailTitle, detailText);
        showAuditDetailDialog(windowTitle, title, fields, detailSections);
    }

    private void showAuditDetailDialog(
        String windowTitle,
        String title,
        java.util.LinkedHashMap<String, String> fields,
        java.util.LinkedHashMap<String, String> detailSections
    ) {
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initOwner(context.owner());
        dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
        dialog.setTitle(windowTitle);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("audit-detail-title");
        titleLabel.setPadding(Insets.EMPTY);

        Label idChip = new Label("#" + auditValue(fields.get("ID")));
        idChip.getStyleClass().add("audit-detail-id-chip");
        javafx.scene.layout.Region headerSpacer = new javafx.scene.layout.Region();
        HBox.setHgrow(headerSpacer, javafx.scene.layout.Priority.ALWAYS);
        HBox header = new HBox(12, titleLabel, headerSpacer, idChip);
        header.getStyleClass().add("audit-detail-header");
        header.setAlignment(Pos.CENTER_LEFT);

        Label overviewTitle = new Label("Overview");
        overviewTitle.getStyleClass().add("audit-detail-section-title");

        VBox detailRows = new VBox(8);
        detailRows.getStyleClass().add("audit-detail-row-list");
        for (java.util.Map.Entry<String, String> entry : fields.entrySet()) {
            if ("ID".equals(entry.getKey())) {
                continue;
            }
            detailRows.getChildren().add(createAuditDetailRow(entry.getKey(), entry.getValue()));
        }

        VBox overviewCard = new VBox(12, overviewTitle, detailRows);
        overviewCard.getStyleClass().add("audit-detail-card");

        VBox contentBox = new VBox(16, header, overviewCard);
        if (detailSections != null) {
            for (Map.Entry<String, String> entry : detailSections.entrySet()) {
                contentBox.getChildren().add(createAuditDetailTextCard(entry.getKey(), entry.getValue()));
            }
        }
        javafx.scene.control.ScrollPane contentScroll = new javafx.scene.control.ScrollPane(contentBox);
        contentScroll.setFitToWidth(true);
        contentScroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        contentScroll.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        contentScroll.getStyleClass().addAll("product-dialog-scroll", "import-dialog-scroll");
        VBox.setVgrow(contentScroll, javafx.scene.layout.Priority.ALWAYS);

        Button closeButton = new Button("Close");
        closeButton.getStyleClass().addAll("button", "dialog-cancel-button");
        closeButton.setOnAction(event -> dialog.close());
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox actionRow = new HBox(14, spacer, closeButton);
        actionRow.setAlignment(Pos.CENTER_RIGHT);
        actionRow.getStyleClass().add("import-dialog-footer");
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        VBox root = new VBox(16, contentScroll, actionRow);
        root.getStyleClass().addAll("dialog-root", "product-dialog-root", "audit-detail-dialog-root");

        javafx.scene.Scene scene = new javafx.scene.Scene(root, 600, 780);
        java.net.URL stylesheet = AuditLogScene.class.getResource("/application.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }
        dialog.setScene(scene);
        dialog.setMinWidth(600);
        dialog.setMinHeight(600);
        com.pbl3.project.pbl3_project.ui.util.DialogSupport.preventInitialFieldFocus(dialog, root);
        dialog.show();
        com.pbl3.project.pbl3_project.ui.util.DialogSupport.centerWindowOnOwner(dialog);
    }

    private VBox createAuditDetailTextCard(String title, String text) {
        Label detailLabel = new Label(title == null || title.isBlank() ? "Details" : title);
        detailLabel.getStyleClass().add("audit-detail-section-title");
        javafx.scene.control.TextArea detailArea = new javafx.scene.control.TextArea(auditValue(text));
        detailArea.setEditable(false);
        detailArea.setWrapText(true);
        detailArea.setPrefRowCount(title != null && title.contains("JSON") ? 9 : 4);
        detailArea.setMaxWidth(Double.MAX_VALUE);
        detailArea.getStyleClass().add("audit-detail-text-area");

        VBox detailCard = new VBox(10, detailLabel, detailArea);
        detailCard.getStyleClass().add("audit-detail-card");
        return detailCard;
    }

    private HBox createAuditDetailRow(String label, String value) {
        Label keyLabel = new Label(label);
        keyLabel.getStyleClass().add("audit-detail-key");
        keyLabel.setMinWidth(135);
        keyLabel.setPrefWidth(135);

        Label valueLabel = new Label(auditValue(value));
        valueLabel.getStyleClass().add("audit-detail-value");
        valueLabel.setWrapText(true);
        valueLabel.setMaxWidth(Double.MAX_VALUE);

        HBox row = new HBox(12, keyLabel, valueLabel);
        row.getStyleClass().add("audit-detail-row");
        row.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(valueLabel, javafx.scene.layout.Priority.ALWAYS);
        return row;
    }

    private String formatAuditUser(com.pbl3.project.pbl3_project.entity.User value) {
        if (value == null) {
            return "System";
        }
        String displayName = context.support().formatUserDisplayName(value);
        String username = value.getUsername();
        if (username == null || username.isBlank() || displayName.equals(username)) {
            return displayName;
        }
        return displayName + " (" + username + ")";
    }

    private String formatSignedInteger(Integer value) {
        int safeValue = value != null ? value : 0;
        return safeValue > 0 ? "+" + safeValue : String.valueOf(safeValue);
    }

    private String auditValue(Object value) {
        if (value == null) {
            return "-";
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? "-" : text;
    }

    private boolean hasAuditText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String prettyAuditJson(String value) {
        if (!hasAuditText(value)) {
            return "-";
        }
        try {
            Object json = new com.fasterxml.jackson.databind.ObjectMapper().readValue(value, Object.class);
            return new com.fasterxml.jackson.databind.ObjectMapper()
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(json);
        } catch (Exception ex) {
            return value;
        }
    }

    private String formatOperationalAuditActionLabel(com.pbl3.project.pbl3_project.entity.OperationalAuditAction action) {
        return formatEnumWords(action == null ? null : action.name());
    }

    private String formatOperationalSubjectTypeLabel(com.pbl3.project.pbl3_project.entity.OperationalSubjectType subjectType) {
        return formatEnumWords(subjectType == null ? null : subjectType.name());
    }


    private Node createSlidingMenu(String[] tabNames, Consumer<Integer> onSelect) { return context.support().createSlidingMenu(tabNames, onSelect); }
    private ExpandableSearchControl createExpandableSearchControl(double expandedWidth) { return ExpandableSearchControl.create(expandedWidth, PRIMARY_COLOR); }
    private void applyStandardTableSizing(TableView<?> table) { context.support().applyStandardTableSizing(table); }
    private void applyStandardTablePageLayout(VBox root, Insets padding) { context.support().applyStandardTablePageLayout(root, padding); }
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

    private String formatEnumWords(String token) { return FxFormatters.humanizeEnumToken(token); }
    private String formatTransactionTypeLabel(com.pbl3.project.pbl3_project.entity.InventoryTransactionType type) { return formatTransactionTypeLabel(type != null ? type.name() : null); }
    private String formatTransactionTypeLabel(String type) {
        if (type == null || type.isBlank()) return "Unknown";
        return switch (type) {
            case "IMPORT" -> "Import Goods";
            case "CANCEL_IMPORT" -> "Import Canceled";
            case "SALE" -> "Sale";
            case "CANCEL_SALE" -> "Sale Canceled";
            case "RETURN" -> "Customer Return";
            case "MANUAL_ADJUST" -> "Manual Adjustment";
            case "REVALUE" -> "Inventory Revalued";
            case "DELETE" -> "Product Deleted";
            default -> FxFormatters.humanizeEnumToken(type);
        };
    }
    private String getTransactionTypeColor(com.pbl3.project.pbl3_project.entity.InventoryTransactionType type) { return getTransactionTypeColor(type != null ? type.name() : null); }
    private String getTransactionTypeColor(String type) {
        if (type == null || type.isBlank()) return "-app-text-secondary";
        return switch (type) {
            case "IMPORT" -> "-app-success-hover";
            case "CANCEL_IMPORT" -> "-app-danger-hover";
            case "SALE" -> "-app-primary-hover";
            case "RETURN" -> "-app-info-hover";
            case "MANUAL_ADJUST", "REVALUE" -> "-app-warning-hover";
            case "DELETE", "CANCEL_SALE" -> "-app-danger-hover";
            default -> "-app-text-primary";
        };
    }
}
