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

public final class OrderHistoryScene {
    private static final Color PRIMARY_COLOR = Color.web("#1d7df2");

    public record Options() {
    }

    private record OrderFilterOptions(
        java.util.List<com.pbl3.project.pbl3_project.dto.IdLabelOption> creatorOptions,
        BigDecimal maxTotal
    ) {
    }

    private record OrderPageResult(
        org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.Order> page,
        int pageIndex
    ) {
    }

    private final SceneRuntimeContext context;
    private final com.pbl3.project.pbl3_project.entity.User user;

    private OrderHistoryScene(SceneRuntimeContext context, com.pbl3.project.pbl3_project.entity.User user) {
        this.context = context;
        this.user = user;
    }

    public static Node create(SceneRuntimeContext context, com.pbl3.project.pbl3_project.entity.User user, Options options) {
        return new OrderHistoryScene(context, user).createOrderHistoryView();
    }

    private VBox createOrderHistoryView() {
        final String orderSortStateKey = "order-history";
        TableSortState orderSortState = getOrCreateTableSortState(
            orderSortStateKey,
            new SortCriterion("createdAt", javafx.scene.control.TableColumn.SortType.DESCENDING)
        );
        java.util.LinkedHashMap<String, String> orderSortProperties = new java.util.LinkedHashMap<>();
        orderSortProperties.put("id", "id");
        orderSortProperties.put("createdAt", "createdAt");
        orderSortProperties.put("totalPrice", "totalPrice");
        orderSortProperties.put("userFullName", "createdByNameSnapshot");
        orderSortProperties.put("customerName", "customerNameSnapshot");
        orderSortProperties.put("status", "status");

        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.entity.Order> table = new javafx.scene.control.TableView<>();
        applyStandardTableSizing(table);
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        TableViewSupport.enableDragSelection(table);

        final int orderPageSize = 20;
        final int[] orderCurrentPage = {0};
        final int[] orderTotalPages = {0};
        final long[] orderTotalElements = {0L};
        java.util.concurrent.atomic.AtomicReference<String> orderSearchRef = new java.util.concurrent.atomic.AtomicReference<>("");
        java.util.concurrent.atomic.AtomicReference<java.time.LocalDate> orderStartDateRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<java.time.LocalDate> orderEndDateRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<java.util.Set<Long>> orderUsersRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<java.util.Set<com.pbl3.project.pbl3_project.entity.PaymentMethod>> orderMethodsRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<java.util.Set<com.pbl3.project.pbl3_project.entity.OrderStatus>> orderStatusesRef =
            new java.util.concurrent.atomic.AtomicReference<>(new java.util.LinkedHashSet<>());
        java.util.concurrent.atomic.AtomicReference<BigDecimal> orderMinTotalRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<BigDecimal> orderMaxTotalRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<OrderFilterOptions> orderFilterOptionsCache = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicLong orderPageLoadVersion = new java.util.concurrent.atomic.AtomicLong();
        AsyncPageCache<OrderPageResult> orderPageCache = new AsyncPageCache<>(80);

        Label orderRowCountLabel = createStatusMetaLabel(java.text.MessageFormat.format("Showing {0}-{1} of {2} Row(s)", 0, 0, 0));
        Label orderPageLabel = createStatusMetaLabel(java.text.MessageFormat.format("Page {0} / {1}", 0, 0));
        Button orderPrevBtn = ButtonFactory.pageNav("Prev");
        Button orderNextBtn = ButtonFactory.pageNav("Next");

        Runnable[] refreshOrderTableRef = new Runnable[1];
        Runnable updateOrderStatusBar = () -> updatePagedStatus(
            table,
            orderRowCountLabel,
            orderPageLabel,
            orderPrevBtn,
            orderNextBtn,
            orderTotalElements[0],
            orderCurrentPage[0],
            orderTotalPages[0],
            orderPageSize
        );
        Runnable loadOrderPage = () -> {
            int requestedPage = orderCurrentPage[0];
            String search = orderSearchRef.get();
            java.time.LocalDate startDate = orderStartDateRef.get();
            java.time.LocalDate endDate = orderEndDateRef.get();
            java.util.Set<Long> users = new java.util.LinkedHashSet<>(orderUsersRef.get());
            java.util.Set<com.pbl3.project.pbl3_project.entity.PaymentMethod> methods =
                new java.util.LinkedHashSet<>(orderMethodsRef.get());
            java.util.Set<com.pbl3.project.pbl3_project.entity.OrderStatus> statuses =
                new java.util.LinkedHashSet<>(orderStatusesRef.get());
            BigDecimal minTotal = orderMinTotalRef.get();
            BigDecimal maxTotal = orderMaxTotalRef.get();
            java.util.List<SortCriterion> sortSnapshot = orderSortState.snapshot();
            TableSortState sortForLoad = new TableSortState(sortSnapshot);
            java.util.function.IntFunction<Object> cacheKeyForPage = pageIndex -> java.util.Arrays.asList(
                pageIndex,
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
            java.util.function.IntFunction<OrderPageResult> fetchOrderPage = pageIndex -> {
                    int resolvedPage = requestedPage;
                    if (pageIndex >= 0) {
                        resolvedPage = pageIndex;
                    }
                    org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.Order> pageData = context.orderService().searchOrders(
                        user,
                        search,
                        startDate,
                        endDate,
                        users,
                        methods,
                        statuses,
                        minTotal,
                        maxTotal,
                        createPageable(sortForLoad, orderSortProperties, resolvedPage, orderPageSize)
                    );
                    if (pageData.getTotalPages() > 0 && resolvedPage >= pageData.getTotalPages()) {
                        resolvedPage = pageData.getTotalPages() - 1;
                        pageData = context.orderService().searchOrders(
                            user,
                            search,
                            startDate,
                            endDate,
                            users,
                            methods,
                            statuses,
                            minTotal,
                            maxTotal,
                            createPageable(sortForLoad, orderSortProperties, resolvedPage, orderPageSize)
                        );
                    }
                    return new OrderPageResult(pageData, resolvedPage);
            };

            AsyncUiTask.runLatestCachedTableLoad(
                table,
                orderPrevBtn,
                orderNextBtn,
                orderPageLoadVersion,
                orderPageCache,
                cacheKeyForPage.apply(requestedPage),
                () -> fetchOrderPage.apply(requestedPage),
                result -> {
                org.springframework.data.domain.Page<com.pbl3.project.pbl3_project.entity.Order> pageData = result.page();
                orderCurrentPage[0] = result.pageIndex();
                orderTotalElements[0] = pageData.getTotalElements();
                orderTotalPages[0] = pageData.getTotalPages();
                table.setItems(javafx.collections.FXCollections.observableArrayList(pageData.getContent()));
                updateOrderStatusBar.run();
                    int nextPage = result.pageIndex() + 1;
                    if (nextPage < orderTotalPages[0]) {
                        orderPageCache.prefetch(
                            cacheKeyForPage.apply(nextPage),
                            () -> fetchOrderPage.apply(nextPage),
                            null,
                            "order-history-next-page-prefetch"
                        );
                    }
                    int previousPage = result.pageIndex() - 1;
                    if (previousPage >= 0) {
                        orderPageCache.prefetch(
                            cacheKeyForPage.apply(previousPage),
                            () -> fetchOrderPage.apply(previousPage),
                            null,
                            "order-history-prev-page-prefetch"
                        );
                    }
                },
                ex -> {
                    updateOrderStatusBar.run();
                    context.showUserFacingError(ex);
                },
                "Loading orders...",
                "Could not load orders",
                "order-history-page-loader"
            );
        };
        refreshOrderTableRef[0] = () -> {
            orderPageCache.clear();
            loadOrderPage.run();
        };
        orderPrevBtn.setOnAction(e -> {
            if (orderCurrentPage[0] > 0) {
                orderCurrentPage[0]--;
                loadOrderPage.run();
            }
        });
        orderNextBtn.setOnAction(e -> {
            if (orderCurrentPage[0] + 1 < orderTotalPages[0]) {
                orderCurrentPage[0]++;
                loadOrderPage.run();
            }
        });

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, Long> idCol = new javafx.scene.control.TableColumn<>("ID");
        idCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, String> dateCol = new javafx.scene.control.TableColumn<>("Created At");
        dateCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(context.support().formatDateTime(cell.getValue().getCreatedAt())));
        dateCol.setPrefWidth(200);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, String> totalCol = new javafx.scene.control.TableColumn<>("Net Paid");
        totalCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(context.support().formatVnd(cell.getValue().getTotalPrice())));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, String> userCol = new javafx.scene.control.TableColumn<>("Created By");
        userCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getCreatedByDisplayName()));

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, String> customerCol = new javafx.scene.control.TableColumn<>("Customer");
        customerCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getCustomerDisplayName()));

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

        table.getColumns().addAll(idCol, dateCol, totalCol, userCol, customerCol, statusCol);
        java.util.LinkedHashMap<String, javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.entity.Order, ?>> orderSortColumns =
            new java.util.LinkedHashMap<>();
        orderSortColumns.put("id", idCol);
        orderSortColumns.put("createdAt", dateCol);
        orderSortColumns.put("totalPrice", totalCol);
        orderSortColumns.put("userFullName", userCol);
        orderSortColumns.put("customerName", customerCol);
        orderSortColumns.put("status", statusCol);
        installSortHeaderIndicators(orderSortColumns);
        java.util.LinkedHashMap<String, String> orderSortLabels = new java.util.LinkedHashMap<>();
        orderSortLabels.put("id", "ID");
        orderSortLabels.put("createdAt", "Created At");
        orderSortLabels.put("totalPrice", "Net Paid");
        orderSortLabels.put("userFullName", "Created By");
        orderSortLabels.put("customerName", "Customer");
        orderSortLabels.put("status", "Status");
        Label orderSortStatusLabel = createSortStatusLabel(orderSortState, orderSortLabels);
        Runnable applyOrderSortUi = () -> {
            applySortStateToTable(table, orderSortColumns, orderSortState);
            orderSortStatusLabel.setText(buildSortStatusText(orderSortState, orderSortLabels));
        };
        applyOrderSortUi.run();
        installManualServerSorting(
            table,
            orderSortColumns,
            orderSortState,
            () -> {
                applyOrderSortUi.run();
                orderCurrentPage[0] = 0;
                loadOrderPage.run();
            }
        );

        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<com.pbl3.project.pbl3_project.entity.Order> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY
                    && event.getClickCount() == 2
                    && !row.isEmpty()) {
                    showOrderDetailsAsync(row.getItem().getId(), refreshOrderTableRef[0]);
                }
            });
            return row;
        });
        javafx.scene.layout.HBox oSearchBox = new javafx.scene.layout.HBox(0);
        oSearchBox.setAlignment(Pos.CENTER);
        oSearchBox.getStyleClass().add("expandable-search-box");
        oSearchBox.setPrefSize(40, 40); oSearchBox.setMinSize(40, 40); oSearchBox.setMaxSize(40, 40);
        javafx.scene.shape.SVGPath oIcon = new javafx.scene.shape.SVGPath();
        oIcon.setContent("M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
        oIcon.setFill(PRIMARY_COLOR);
        javafx.scene.layout.Region oSpacer = new javafx.scene.layout.Region();
        oSpacer.setMinWidth(0); oSpacer.setPrefWidth(0);
        TextField oField = new TextField();
        oField.setPromptText("Search"); oField.getStyleClass().add("search-text-field");
        oField.setMinWidth(0); oField.setMaxWidth(0); oField.setPrefWidth(0); oField.setOpacity(0);
        oSearchBox.getChildren().addAll(oIcon, oSpacer, oField);
        javafx.animation.Timeline oExpand = new javafx.animation.Timeline(new javafx.animation.KeyFrame(Duration.millis(150),
            new javafx.animation.KeyValue(oSearchBox.maxWidthProperty(), 250, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oSearchBox.prefWidthProperty(), 250, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oSpacer.minWidthProperty(), 8, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oField.minWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oField.maxWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oField.prefWidthProperty(), 190, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oField.opacityProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH)));
        javafx.animation.Timeline oCollapse = new javafx.animation.Timeline(new javafx.animation.KeyFrame(Duration.millis(150),
            new javafx.animation.KeyValue(oSearchBox.maxWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oSearchBox.prefWidthProperty(), 40, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oSpacer.minWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oField.minWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oField.maxWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oField.prefWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
            new javafx.animation.KeyValue(oField.opacityProperty(), 0.0, javafx.animation.Interpolator.EASE_BOTH)));

        javafx.animation.PauseTransition orderSearchPause = new javafx.animation.PauseTransition(Duration.millis(220));
        orderSearchPause.setOnFinished(e -> {
            orderCurrentPage[0] = 0;
            orderSearchRef.set(oField.getText());
            loadOrderPage.run();
        });
        oField.textProperty().addListener((obs, oldV, newV) -> orderSearchPause.playFromStart());

        VBox content = new VBox();
        applyStandardTablePageLayout(content);

        oSearchBox.setOnMouseClicked(ev -> {
            if (oSearchBox.getMaxWidth() == 40) { oExpand.play(); oField.requestFocus(); }
            else if (ev.getTarget() == oIcon || ev.getTarget() == oSearchBox) { oField.clear(); content.requestFocus(); oCollapse.play(); }
        });
        javafx.scene.layout.HBox oFilterBox = new javafx.scene.layout.HBox();
        oFilterBox.setAlignment(Pos.CENTER);
        oFilterBox.getStyleClass().add("expandable-search-box");
        oFilterBox.setPrefSize(40, 40); oFilterBox.setMinSize(40, 40); oFilterBox.setMaxSize(40, 40);
        oFilterBox.setCursor(javafx.scene.Cursor.HAND);
        javafx.scene.shape.SVGPath oFilterIcon = new javafx.scene.shape.SVGPath();
        oFilterIcon.setContent("M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z");
        oFilterIcon.setFill(PRIMARY_COLOR);
        oFilterBox.getChildren().add(oFilterIcon);
        javafx.scene.control.Tooltip.install(oFilterBox, new javafx.scene.control.Tooltip("Filter"));

        javafx.stage.Popup oFilterPopup = new javafx.stage.Popup();
        oFilterPopup.setAutoHide(true);

        oFilterBox.setOnMouseClicked(fev -> {
            fev.consume();
            try {
                if (oFilterPopup.isShowing()) {
                    oFilterPopup.hide();
                    return;
                }

                java.util.function.Consumer<OrderFilterOptions> showFilterContent = filterOptions -> {
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
            java.util.List<com.pbl3.project.pbl3_project.dto.IdLabelOption> userOptions = filterOptions.creatorOptions();

            for (com.pbl3.project.pbl3_project.dto.IdLabelOption option : userOptions) {
                if (option.label() == null || option.label().trim().isEmpty()) continue;
                javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(option.label());
                cb.setUserData(option.id());
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
            Label methodTitle = new Label("Payment Method");
            methodTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");

            javafx.scene.control.CheckBox allMethodsCb = new javafx.scene.control.CheckBox("All Methods");
            allMethodsCb.setSelected(true);
            allMethodsCb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");

            VBox methodScroll = new VBox(8);
            methodScroll.setPadding(new Insets(5, 5, 5, 20));

            java.util.List<javafx.scene.control.CheckBox> methodCbs = new java.util.ArrayList<>();
            for (com.pbl3.project.pbl3_project.entity.PaymentMethod pm : com.pbl3.project.pbl3_project.entity.PaymentMethod.values()) {
                javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(context.support().formatPaymentMethodLabel(pm));
                cb.setUserData(pm);
                cb.setSelected(true);
                cb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                cb.setOnAction(e -> {
                    if (!cb.isSelected()) allMethodsCb.setSelected(false);
                    else {
                        boolean all = true;
                        for (javafx.scene.control.CheckBox c : methodCbs) if (!c.isSelected()) all = false;
                        allMethodsCb.setSelected(all);
                    }
                });
                methodCbs.add(cb);
                methodScroll.getChildren().add(cb);
            }

            allMethodsCb.setOnAction(e -> {
                boolean sel = allMethodsCb.isSelected();
                for (javafx.scene.control.CheckBox cb : methodCbs) cb.setSelected(sel);
            });
            FilterDisclosureSection methodSection = new FilterDisclosureSection(allMethodsCb, methodScroll);

            javafx.scene.control.Separator sepMethod = new javafx.scene.control.Separator();
            Label statusTitle = new Label("Order Status");
            statusTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");

            javafx.scene.control.CheckBox allStatusesCb = new javafx.scene.control.CheckBox("All Statuses");
            allStatusesCb.setSelected(true);
            allStatusesCb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");

            VBox statusScroll = new VBox(8);
            statusScroll.setPadding(new Insets(5, 5, 5, 20));

            java.util.List<javafx.scene.control.CheckBox> statusCbs = new java.util.ArrayList<>();
            for (com.pbl3.project.pbl3_project.entity.OrderStatus status : com.pbl3.project.pbl3_project.entity.OrderStatus.values()) {
                javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(context.support().formatOrderStatus(status));
                cb.setUserData(status);
                cb.setSelected(true);
                cb.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
                cb.setOnAction(e -> {
                    if (!cb.isSelected()) {
                        allStatusesCb.setSelected(false);
                    } else {
                        boolean all = true;
                        for (javafx.scene.control.CheckBox c : statusCbs) {
                            if (!c.isSelected()) all = false;
                        }
                        allStatusesCb.setSelected(all);
                    }
                });
                statusCbs.add(cb);
                statusScroll.getChildren().add(cb);
            }

            allStatusesCb.setOnAction(e -> {
                boolean sel = allStatusesCb.isSelected();
                for (javafx.scene.control.CheckBox cb : statusCbs) cb.setSelected(sel);
            });
            FilterDisclosureSection statusSection = new FilterDisclosureSection(allStatusesCb, statusScroll);

            javafx.scene.control.Separator sepStatus = new javafx.scene.control.Separator();
            Label priceTitle = new Label("Net Paid Range");
            priceTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");

            BigDecimal maxPriceValue = filterOptions.maxTotal();
            double maxPrice = maxPriceValue == null ? 0.0 : maxPriceValue.doubleValue();
            if (maxPrice == 0) maxPrice = 1000;

            Label priceLabel = new Label("0 - " + String.format("%.0f", maxPrice) + " VND");
            priceLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -app-primary; -fx-font-weight: bold;");

            RangeSlider priceSlider = new RangeSlider(0, maxPrice, 0, maxPrice, 280);
            priceSlider.minVal.addListener((o, ov, nv) -> priceLabel.setText(String.format("%.0f - %.0f VND", nv.doubleValue(), priceSlider.maxVal.get())));
            priceSlider.maxVal.addListener((o, ov, nv) -> priceLabel.setText(String.format("%.0f - %.0f VND", priceSlider.minVal.get(), nv.doubleValue())));

            scrollContent.getChildren().addAll(
                dateTitle, dateBox, sepDate,
                userTitle, userSection.getNode(), sepUser,
                methodTitle, methodSection.getNode(), sepMethod,
                statusTitle, statusSection.getNode(), sepStatus,
                priceTitle, priceLabel, priceSlider
            );
            javafx.scene.layout.HBox btnRow = new javafx.scene.layout.HBox(10);
            btnRow.setAlignment(Pos.CENTER_RIGHT);

            final double fMaxPrice = maxPrice;

            Button resetBtn = new Button("Reset");
            resetBtn.getStyleClass().add("filter-reset-btn");
            resetBtn.setOnAction(ae -> {
                oFilterBox.setStyle("");
                startDatePicker.setValue(null);
                endDatePicker.setValue(null);
                allUsersCb.setSelected(true);
                for (javafx.scene.control.CheckBox cb : userCbs) cb.setSelected(true);
                userSection.setExpanded(false);
                allMethodsCb.setSelected(true);
                for (javafx.scene.control.CheckBox cb : methodCbs) cb.setSelected(true);
                methodSection.setExpanded(false);
                allStatusesCb.setSelected(true);
                for (javafx.scene.control.CheckBox cb : statusCbs) cb.setSelected(true);
                statusSection.setExpanded(false);
                priceSlider.minVal.set(0); priceSlider.maxVal.set(fMaxPrice);
                orderStartDateRef.set(null);
                orderEndDateRef.set(null);
                orderUsersRef.set(new java.util.LinkedHashSet<>());
                orderMethodsRef.set(new java.util.LinkedHashSet<>());
                orderStatusesRef.set(new java.util.LinkedHashSet<>());
                orderMinTotalRef.set(null);
                orderMaxTotalRef.set(null);
                orderCurrentPage[0] = 0;
                loadOrderPage.run();
                oFilterPopup.hide();
            });

            Button applyBtn = new Button("Apply Filter");
            applyBtn.getStyleClass().add("filter-apply-btn");
            applyBtn.setOnAction(ae -> {
                java.util.Set<Long> selectedUsers = new java.util.HashSet<>();
                for (javafx.scene.control.CheckBox cb : userCbs) {
                    if (cb.isSelected() && cb.getUserData() instanceof Long userId) {
                        selectedUsers.add(userId);
                    }
                }

                java.util.Set<com.pbl3.project.pbl3_project.entity.PaymentMethod> selectedMethods = new java.util.HashSet<>();
                for (javafx.scene.control.CheckBox cb : methodCbs) {
                    if (cb.isSelected() && cb.getUserData() instanceof com.pbl3.project.pbl3_project.entity.PaymentMethod method) {
                        selectedMethods.add(method);
                    }
                }

                java.util.Set<com.pbl3.project.pbl3_project.entity.OrderStatus> selectedStatuses = new java.util.HashSet<>();
                for (javafx.scene.control.CheckBox cb : statusCbs) {
                    if (cb.isSelected()) {
                        selectedStatuses.add((com.pbl3.project.pbl3_project.entity.OrderStatus) cb.getUserData());
                    }
                }

                double pMin = priceSlider.minVal.get();
                double pMax = priceSlider.maxVal.get();
                java.time.LocalDate sDate = startDatePicker.getValue();
                java.time.LocalDate eDate = endDatePicker.getValue();

                orderStartDateRef.set(sDate);
                orderEndDateRef.set(eDate);
                orderUsersRef.set(allUsersCb.isSelected() ? new java.util.LinkedHashSet<>() : selectedUsers);
                java.util.Set<com.pbl3.project.pbl3_project.entity.PaymentMethod> methodFilters = new java.util.LinkedHashSet<>();
                if (!allMethodsCb.isSelected()) {
                    methodFilters.addAll(selectedMethods);
                }
                orderMethodsRef.set(methodFilters);
                orderStatusesRef.set(allStatusesCb.isSelected() ? new java.util.LinkedHashSet<>() : selectedStatuses);
                orderMinTotalRef.set(pMin <= 0 ? null : MoneySupport.normalize(BigDecimal.valueOf(pMin)));
                orderMaxTotalRef.set(pMax >= fMaxPrice ? null : MoneySupport.normalize(BigDecimal.valueOf(pMax)));
                orderCurrentPage[0] = 0;
                loadOrderPage.run();

                boolean hasFilter = !allMethodsCb.isSelected()
                    || !allUsersCb.isSelected()
                    || !allStatusesCb.isSelected()
                    || pMin > 0
                    || pMax < fMaxPrice
                    || sDate != null
                    || eDate != null;
                oFilterBox.setStyle(hasFilter ? "-fx-border-color: -app-primary;" : "");
                oFilterPopup.hide();
            });

            btnRow.getChildren().addAll(resetBtn, applyBtn);

                popupContainer.getChildren().addAll(scrollPane, btnRow);
                oFilterPopup.getContent().clear();
                oFilterPopup.getContent().add(popupContainer);

                context.support().showPopupBelow(oFilterPopup, oFilterBox, -290, 5);
                };

                OrderFilterOptions cachedOptions = orderFilterOptionsCache.get();
                if (cachedOptions != null) {
                    showFilterContent.accept(cachedOptions);
                    return;
                }

                oFilterPopup.getContent().setAll(FilterControlFactory.loadingContainer(350, "Loading filters..."));
                context.support().showPopupBelow(oFilterPopup, oFilterBox, -290, 5);

                javafx.concurrent.Task<OrderFilterOptions> task = new javafx.concurrent.Task<>() {
                    @Override
                    protected OrderFilterOptions call() {
                        return new OrderFilterOptions(
                            context.orderService().getOrderCreatorOptions(user),
                            context.orderService().getOrderMaxTotalPrice(user)
                        );
                    }
                };
                task.setOnSucceeded(taskEvent -> {
                    OrderFilterOptions optionsValue = task.getValue();
                    orderFilterOptionsCache.set(optionsValue);
                    if (oFilterPopup.isShowing()) {
                        showFilterContent.accept(optionsValue);
                    }
                });
                task.setOnFailed(taskEvent -> {
                    oFilterPopup.hide();
                    context.showUserFacingError(task.getException());
                });
                Thread worker = new Thread(task, "order-filter-options-loader");
                worker.setDaemon(true);
                worker.start();
            } catch (Exception ex) {
                context.showUserFacingError(ex);
            }
        });

        Button manageOrderBtn = ButtonFactory.expandableManageAction("Manage Order", 164);
        manageOrderBtn.setDisable(true);
        manageOrderBtn.setOnAction(e -> {
            java.util.List<com.pbl3.project.pbl3_project.entity.Order> selectedOrders = new java.util.ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selectedOrders.size() != 1) {
                context.toastService().showWarning("Select exactly one order.");
                return;
            }
            showOrderDetailsAsync(selectedOrders.get(0).getId(), refreshOrderTableRef[0]);
        });
        table.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<com.pbl3.project.pbl3_project.entity.Order>) c ->
            manageOrderBtn.setDisable(table.getSelectionModel().getSelectedItems().size() != 1)
        );

        javafx.scene.layout.BorderPane orderToolbar = new javafx.scene.layout.BorderPane();
        javafx.scene.layout.HBox oLeftBox = new javafx.scene.layout.HBox(15, manageOrderBtn);
        oLeftBox.setAlignment(Pos.CENTER_LEFT);
        javafx.scene.layout.HBox oRightBox = new javafx.scene.layout.HBox(15, oFilterBox, oSearchBox);
        oRightBox.setAlignment(Pos.CENTER_RIGHT);
        orderToolbar.setLeft(oLeftBox);
        orderToolbar.setRight(oRightBox);

        javafx.scene.layout.HBox orderStatusBar = new javafx.scene.layout.HBox(15, orderSortStatusLabel, orderRowCountLabel, orderPageLabel, orderPrevBtn, orderNextBtn);
        applyStandardTableStatusBar(orderStatusBar);

        content.getChildren().addAll(orderToolbar, table, orderStatusBar);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);

        table.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<com.pbl3.project.pbl3_project.entity.Order>) c -> updateOrderStatusBar.run());
        javafx.application.Platform.runLater(loadOrderPage);
        TableViewSupport.enableDeselectOnOutsideClick(content, table);
        return content;
    }

    private void showOrderDetailsAsync(Long orderId, Runnable onChanged) {
        if (orderId == null) {
            context.toastService().showWarning("Select an order.");
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
        Thread worker = new Thread(task, "order-details-dialog-loader");
        worker.setDaemon(true);
        worker.start();
    }


    private void applyStandardTableSizing(TableView<?> table) {
        context.support().applyStandardTableSizing(table);
    }

    private void applyStandardTablePageLayout(VBox root) {
        context.support().applyStandardTablePageLayout(root);
    }

    private void applyStandardTableStatusBar(HBox statusBar) {
        context.support().applyStandardTableStatusBar(statusBar);
    }

    private Label createStatusMetaLabel(String text) {
        return context.support().createStatusMetaLabel(text);
    }

    private void updatePagedStatus(TableView<?> table, Label rowCountLabel, Label pageLabel, Button prevButton, Button nextButton, long totalElements, int currentPage, int totalPages, int pageSize) {
        context.support().updatePagedStatus(table, rowCountLabel, pageLabel, prevButton, nextButton, totalElements, currentPage, totalPages, pageSize);
    }

    private Pageable createPageable(TableSortState sortState, Map<String, String> propertyByUiKey, int page, int size) {
        return context.support().createPageable(sortState, propertyByUiKey, page, size);
    }

    private TableSortState getOrCreateTableSortState(String stateKey, SortCriterion... defaultCriteria) {
        return context.support().getOrCreateTableSortState(stateKey, defaultCriteria);
    }

    private <T> void installSortHeaderIndicators(LinkedHashMap<String, TableColumn<T, ?>> columnsByKey) {
        context.support().installSortHeaderIndicators(columnsByKey);
    }

    private <T> void applySortStateToTable(TableView<T> table, LinkedHashMap<String, TableColumn<T, ?>> columnsByKey, TableSortState sortState) {
        context.support().applySortStateToTable(table, columnsByKey, sortState);
    }

    private Label createSortStatusLabel(TableSortState sortState, Map<String, String> labelsByKey) {
        return context.support().createSortStatusLabel(sortState, labelsByKey);
    }

    private String buildSortStatusText(TableSortState sortState, Map<String, String> labelsByKey) {
        return context.support().buildSortStatusText(sortState, labelsByKey);
    }

    private <T> void installManualServerSorting(TableView<T> table, LinkedHashMap<String, TableColumn<T, ?>> columnsByKey, TableSortState sortState, Runnable onSortChanged) {
        context.support().installManualServerSorting(table, columnsByKey, sortState, onSortChanged);
    }
}
