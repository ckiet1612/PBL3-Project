package com.pbl3.project.pbl3_project.ui.scene;

import com.pbl3.project.pbl3_project.dto.IdLabelOption;
import com.pbl3.project.pbl3_project.entity.SalesShiftStatus;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.service.MoneySupport;
import com.pbl3.project.pbl3_project.service.SalesShiftService;
import com.pbl3.project.pbl3_project.ui.component.SidebarIconFactory;
import com.pbl3.project.pbl3_project.ui.scene.model.ImportOrderPrefill;
import com.pbl3.project.pbl3_project.ui.scene.model.ProductViewPreset;
import com.pbl3.project.pbl3_project.ui.scene.model.ReportFocusTarget;
import com.pbl3.project.pbl3_project.ui.util.TableViewSupport;
import com.pbl3.project.pbl3_project.ui.util.UiTaskExecutor;
import java.math.BigDecimal;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

final class DashboardReportsContentBuilder {
    private record ReportSectionsBundle(
        java.util.List<Node> nodes,
        java.util.Map<ReportFocusTarget, Node> anchors
    ) {
    }

    private record ShiftCloseInput(BigDecimal amount, String note) {
    }

    private record ShiftReportLoadResult(
        java.util.List<IdLabelOption> userOptions,
        java.util.List<SalesShiftService.ShiftReportRow> rows
    ) {
    }

    private record ReportPdfFonts(
        com.itextpdf.text.Font title,
        com.itextpdf.text.Font metaBold,
        com.itextpdf.text.Font meta,
        com.itextpdf.text.Font header,
        com.itextpdf.text.Font body
    ) {
    }

    private record ShiftDetailLabels(
        Label title,
        Label status,
        Label opened,
        Label closed,
        Label orders,
        Label openingCash,
        Label sales,
        Label refunds,
        Label expenses,
        Label expectedCash,
        Label actualCash,
        Label variance,
        Label closedBy,
        Label closeNote
    ) {
    }

    private static final String PRIMARY_HEX = "#1d7df2";
    private static final String SUCCESS_HEX = "#22c55e";
    private static final String PRIMARY_BAR_FILL = "-app-primary";
    private static final String SUCCESS_BAR_FILL = "-app-success";
    private static final String DANGER_BAR_FILL = "-app-danger";
    private static final java.time.format.DateTimeFormatter FILE_DATE_FORMATTER =
        java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final java.time.format.DateTimeFormatter DISPLAY_DATE_FORMATTER =
        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final javafx.scene.paint.Color PRIMARY_COLOR = javafx.scene.paint.Color.web(PRIMARY_HEX);
    private static final javafx.scene.paint.Color TEXT_MUTED_COLOR = javafx.scene.paint.Color.web("#78909C");

    private final SceneRuntimeContext context;

    DashboardReportsContentBuilder(SceneRuntimeContext context) {
        this.context = context;
    }


    private void enableScrollPerfCache(Node node) {
        if (node == null) {
            return;
        }
        node.setCache(true);
        node.setCacheHint(javafx.scene.CacheHint.SPEED);
    }

    private boolean isReducedMotionEnabled(Node node) {
        Node current = node;
        while (current != null) {
            if (current.getStyleClass().contains("ui-reduced-motion")) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private String formatExpenseCategoryLabel(com.pbl3.project.pbl3_project.entity.ExpenseCategory category) {
        return context.support().formatExpenseCategoryLabel(category);
    }

    private String formatPromotionScopeLabel(com.pbl3.project.pbl3_project.entity.PromotionScope scope) {
        return context.support().formatPromotionScopeLabel(scope);
    }

    Node createDashboard(User user) {
        return createOverviewView(context.owner(), user);
    }

    Node createReports(User user, java.time.LocalDate startDate, java.time.LocalDate endDate, ReportFocusTarget focusTarget) {
        return createOperationalReportsView(context.owner(), user, startDate, endDate, focusTarget);
    }

    private VBox createOverviewView(Stage stage, User user) {
        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(
            createDashboardStateContent(
                "Loading dashboard",
                "Preparing today's snapshot and the last 7 days sales mix...",
                null,
                true
            )
        );
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        javafx.beans.binding.DoubleBinding dashboardViewportWidth = javafx.beans.binding.Bindings.createDoubleBinding(
            () -> Math.max(420.0, scrollPane.getViewportBounds().getWidth()),
            scrollPane.viewportBoundsProperty()
        );

        VBox root = new VBox(scrollPane);
        VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);
        Runnable[] loadDashboardRef = new Runnable[1];
        loadDashboardRef[0] = () -> {
            scrollPane.setContent(createDashboardStateContent(
                "Loading dashboard",
                "Preparing today's snapshot and the last 7 days sales mix...",
                null,
                true
            ));

            javafx.concurrent.Task<com.pbl3.project.pbl3_project.dto.report.DashboardOverviewData> task =
                new javafx.concurrent.Task<>() {
                    @Override
                    protected com.pbl3.project.pbl3_project.dto.report.DashboardOverviewData call() {
                        return context.reportService().getDashboardOverviewData(user);
                    }
                };

            task.setOnSucceeded(event -> {
                VBox loadedContent = buildOverviewContent(stage, user, task.getValue(), dashboardViewportWidth);
                loadedContent.setOpacity(0.0);
                scrollPane.setContent(loadedContent);
                javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(140), loadedContent);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });

            task.setOnFailed(event -> {
                Throwable ex = task.getException();
                Button retryButton = new Button("Retry");
                retryButton.getStyleClass().addAll("button", "dashboard-report-secondary-button");
                retryButton.setOnAction(e -> loadDashboardRef[0].run());
                scrollPane.setContent(createDashboardStateContent(
                    "Dashboard unavailable",
                    ex != null && ex.getMessage() != null && !ex.getMessage().isBlank()
                        ? java.text.MessageFormat.format("Could not load dashboard: {0}", ex.getMessage())
                        : "Could not load dashboard data.",
                    retryButton,
                    false
                ));
                if (ex != null && ex.getMessage() != null && !ex.getMessage().isBlank()) {
                    context.toastService().showError("Failed to load dashboard" + ": " + ex.getMessage());
                } else {
                    context.toastService().showError("Failed to load dashboard");
                }
            });

            UiTaskExecutor.execute(task, "dashboard-overview-loader");
        };

        loadDashboardRef[0].run();
        return root;
    }

    private VBox buildOverviewContent(
        Stage stage,
        User user,
        com.pbl3.project.pbl3_project.dto.report.DashboardOverviewData dashboardData,
        javafx.beans.value.ObservableNumberValue viewportWidthSource
    ) {
        VBox content = new VBox(20);
        content.getStyleClass().add("dashboard-page");
        content.setPadding(new Insets(20));
        content.setFillWidth(true);
        content.setMaxWidth(Double.MAX_VALUE);

        javafx.beans.binding.DoubleBinding dashboardContentWidth = javafx.beans.binding.Bindings.createDoubleBinding(
            () -> Math.max(420.0, viewportWidthSource.doubleValue() - 40.0),
            viewportWidthSource
        );
        boolean showInventorySnapshot = context.authorizationService().canViewAllOrders(user);
        boolean canOpenReports = context.authorizationService().canAccessReports(user);
        boolean canAccessExpenses = context.authorizationService().canAccessExpenses(user);

        VBox revenueCard = createDashboardMetricCard(
            "Today's Revenue",
            context.support().formatVnd(dashboardData.todayRevenue()),
            "-app-success",
            formatDashboardCurrencyDelta(dashboardData.revenueDeltaVsYesterday()),
            getDashboardDeltaColor(dashboardData.revenueDeltaVsYesterday(), true),
            createRevenuePanelIcon()
        );
        VBox ordersCard = createDashboardMetricCard(
            "Orders Today",
            String.valueOf(dashboardData.todayOrders()),
            "-app-primary",
            formatDashboardCountDelta(dashboardData.ordersDeltaVsYesterday(), "orders"),
            getDashboardDeltaColor(dashboardData.ordersDeltaVsYesterday(), true),
            createOrdersPanelIcon()
        );
        VBox expensesCard = createDashboardMetricCard(
            "Today's Expenses",
            context.support().formatVnd(dashboardData.todayExpenses()),
            "#fe9900",
            formatDashboardCurrencyDelta(dashboardData.expenseDeltaVsYesterday()),
            getDashboardDeltaColor(dashboardData.expenseDeltaVsYesterday(), false),
            createExpensesPanelIcon()
        );
        installDashboardPaneHover(revenueCard);
        installDashboardPaneHover(ordersCard);
        installDashboardPaneHover(expensesCard);
        javafx.scene.layout.GridPane statsRow;
        if (showInventorySnapshot) {
            VBox lowStockCard = createDashboardMetricCard(
                "Low Stock Items",
                String.valueOf(dashboardData.lowStockCount()),
                dashboardData.lowStockCount() > 0 ? "-app-danger" : "-app-text-muted",
                formatDashboardCountDelta(dashboardData.lowStockDeltaVsYesterday(), "items"),
                getDashboardDeltaColor(dashboardData.lowStockDeltaVsYesterday(), false),
                createLowStockPanelIcon()
            );
            installDashboardPaneHover(lowStockCard);
            if (canAccessExpenses) {
                statsRow = createResponsiveDashboardQuadRow(
                    revenueCard,
                    ordersCard,
                    expensesCard,
                    lowStockCard,
                    dashboardContentWidth,
                    1180.0,
                    760.0
                );
            } else {
                statsRow = createResponsiveDashboardKpiRow(
                    revenueCard,
                    ordersCard,
                    lowStockCard,
                    dashboardContentWidth,
                    980.0
                );
            }
            if (context.authorizationService().canAccessProducts(user)) {
                makeDashboardDrillDown(
                    lowStockCard,
                    dashboardData.lowStockCount() > 0
                        ? "Open low-stock items in Products"
                        : "Open Products",
                    () -> context.navigator().showProducts(ProductViewPreset.LOW_STOCK)
                );
            }
            if (canAccessExpenses) {
                makeDashboardDrillDown(
                    expensesCard,
                    "Open today's expenses",
                    () -> context.navigator().showExpenses(java.time.LocalDate.now(), java.time.LocalDate.now())
                );
            }
        } else {
            statsRow = createResponsiveDashboardPairRow(
                revenueCard,
                ordersCard,
                dashboardContentWidth,
                720.0
            );
        }

        java.util.function.Function<com.pbl3.project.pbl3_project.dto.report.InsightDrilldownTarget, Runnable> dashboardInsightActionResolver =
            target -> createDashboardInsightAction(stage, user, target);

        VBox whatChangedSection = createReportSection(
            "What Changed Today",
            dashboardData.whatChanged() != null
                ? java.text.MessageFormat.format("Compared with {0}", dashboardData.whatChanged().baselineRangeLabel())
                : "Compared with yesterday",
            createWhatChangedContent(
                dashboardData.whatChanged(),
                dashboardInsightActionResolver,
                "No prior comparison available yet",
                dashboardContentWidth
            ),
            null,
            null
        );
        bindReportSectionFullWidth(whatChangedSection, dashboardContentWidth);
        enableScrollPerfCache(whatChangedSection);

        VBox actionCenterSection = null;
        if (dashboardData.actionCenter() != null) {
            actionCenterSection = createReportSection(
                "Action Center",
                "Prioritized actions from stock and sales signals",
                createActionCenterContent(stage, user, dashboardData.actionCenter(), 5, dashboardInsightActionResolver),
                null,
                null
            );
            bindReportSectionFullWidth(actionCenterSection, dashboardContentWidth);
            enableScrollPerfCache(actionCenterSection);
        }

        VBox reorderSection = null;
        if (dashboardData.reorder() != null) {
            reorderSection = createReportSection(
                "Explainable Reorder",
                "Suggested replenishment using the last 14 days of demand",
                createExplainableReorderContent(stage, user, dashboardData.reorder(), 5, false),
                null,
                null
            );
            bindReportSectionFullWidth(reorderSection, dashboardContentWidth);
            enableScrollPerfCache(reorderSection);
        }

        javafx.scene.chart.LineChart<String, Number> revenueChart = createDashboardLineChart("Date", "Revenue (VND)", false);
        if (revenueChart.getXAxis() instanceof javafx.scene.chart.CategoryAxis revenueXAxis) {
            configureDashboardCategoryAxis(revenueXAxis, new java.util.ArrayList<>(dashboardData.salesMix().revenueSeries().keySet()));
        }
        javafx.scene.chart.XYChart.Series<String, Number> revenueSeries = new javafx.scene.chart.XYChart.Series<>();
        dashboardData.salesMix().revenueSeries().forEach((label, value) -> revenueSeries.getData().add(
            createDashboardLineData(label, value, SUCCESS_BAR_FILL, label + ": " + context.support().formatVnd(value))
        ));
        applyLineSeriesStyling(revenueSeries, SUCCESS_BAR_FILL);
        revenueChart.getData().add(revenueSeries);
        configureDashboardVerticalValueAxis(revenueChart, dashboardData.salesMix().revenueSeries().values(), false);

        javafx.scene.chart.LineChart<String, Number> ordersChart = createDashboardLineChart("Date", "Orders", false);
        if (ordersChart.getXAxis() instanceof javafx.scene.chart.CategoryAxis ordersXAxis) {
            configureDashboardCategoryAxis(ordersXAxis, new java.util.ArrayList<>(dashboardData.salesMix().orderSeries().keySet()));
        }
        javafx.scene.chart.XYChart.Series<String, Number> ordersSeries = new javafx.scene.chart.XYChart.Series<>();
        dashboardData.salesMix().orderSeries().forEach((label, value) -> ordersSeries.getData().add(
            createDashboardLineData(label, value, PRIMARY_BAR_FILL, java.text.MessageFormat.format("{0}: {1} orders", label, value))
        ));
        applyLineSeriesStyling(ordersSeries, PRIMARY_BAR_FILL);
        ordersChart.getData().add(ordersSeries);
        configureDashboardVerticalValueAxis(ordersChart, dashboardData.salesMix().orderSeries().values(), true);

        javafx.scene.chart.LineChart<String, Number> canceledOrdersChart = createDashboardLineChart("Date", "Canceled Orders", false);
        if (canceledOrdersChart.getXAxis() instanceof javafx.scene.chart.CategoryAxis canceledOrdersXAxis) {
            configureDashboardCategoryAxis(canceledOrdersXAxis, new java.util.ArrayList<>(dashboardData.salesMix().canceledOrderSeries().keySet()));
        }
        javafx.scene.chart.XYChart.Series<String, Number> canceledOrdersSeries = new javafx.scene.chart.XYChart.Series<>();
        dashboardData.salesMix().canceledOrderSeries().forEach((label, value) -> canceledOrdersSeries.getData().add(
            createDashboardLineData(label, value, DANGER_BAR_FILL, java.text.MessageFormat.format("{0}: {1} canceled orders", label, value))
        ));
        applyLineSeriesStyling(canceledOrdersSeries, DANGER_BAR_FILL);
        canceledOrdersChart.getData().add(canceledOrdersSeries);
        configureDashboardVerticalValueAxis(canceledOrdersChart, dashboardData.salesMix().canceledOrderSeries().values(), true);

        javafx.scene.Node paymentChartContent = createPaymentMethodShareContent(
            dashboardData.salesMix().paymentMethodShare(),
            "No sales in the last 7 days"
        );
        javafx.scene.Node topSellingChartContent = createTopSellingChartContent(dashboardData.salesMix().topSellingProducts());

        VBox revenueSection = createReportSection(
            "Revenue - Last 7 Days",
            null,
            revenueChart,
            null,
            null
        );
        VBox ordersSection = createReportSection(
            "Orders - Last 7 Days",
            null,
            ordersChart,
            null,
            null
        );
        VBox canceledOrdersSection = createReportSection(
            "Canceled Orders - Last 7 Days",
            null,
            canceledOrdersChart,
            null,
            null
        );
        VBox paymentSection = createReportSection(
            "Payment Method Share - Last 7 Days",
            null,
            paymentChartContent,
            null,
            null
        );
        VBox topSellingSection = createReportSection(
            "Top Selling Products - Last 7 Days",
            null,
            topSellingChartContent,
            null,
            null
        );

        enableScrollPerfCache(revenueChart);
        enableScrollPerfCache(ordersChart);
        enableScrollPerfCache(canceledOrdersChart);
        enableScrollPerfCache(paymentChartContent);
        enableScrollPerfCache(topSellingChartContent);
        enableScrollPerfCache(revenueSection);
        enableScrollPerfCache(ordersSection);
        enableScrollPerfCache(canceledOrdersSection);
        enableScrollPerfCache(paymentSection);
        enableScrollPerfCache(topSellingSection);
        canceledOrdersSection.setMaxWidth(Double.MAX_VALUE);
        canceledOrdersSection.prefWidthProperty().bind(dashboardContentWidth);

        java.util.Map<com.pbl3.project.pbl3_project.entity.DashboardSectionKey, javafx.scene.Node> availableDashboardSections =
            new java.util.EnumMap<>(com.pbl3.project.pbl3_project.entity.DashboardSectionKey.class);
        availableDashboardSections.put(com.pbl3.project.pbl3_project.entity.DashboardSectionKey.KPI_ROW, statsRow);
        availableDashboardSections.put(com.pbl3.project.pbl3_project.entity.DashboardSectionKey.WHAT_CHANGED, whatChangedSection);
        if (actionCenterSection != null) {
            availableDashboardSections.put(com.pbl3.project.pbl3_project.entity.DashboardSectionKey.ACTION_CENTER, actionCenterSection);
        }
        if (reorderSection != null) {
            availableDashboardSections.put(com.pbl3.project.pbl3_project.entity.DashboardSectionKey.EXPLAINABLE_REORDER, reorderSection);
        }
        availableDashboardSections.put(com.pbl3.project.pbl3_project.entity.DashboardSectionKey.REVENUE_CHART, revenueSection);
        availableDashboardSections.put(com.pbl3.project.pbl3_project.entity.DashboardSectionKey.ORDERS_CHART, ordersSection);
        availableDashboardSections.put(com.pbl3.project.pbl3_project.entity.DashboardSectionKey.CANCELED_ORDERS_CHART, canceledOrdersSection);
        availableDashboardSections.put(com.pbl3.project.pbl3_project.entity.DashboardSectionKey.PAYMENT_METHOD_SHARE, paymentSection);
        availableDashboardSections.put(com.pbl3.project.pbl3_project.entity.DashboardSectionKey.TOP_SELLING, topSellingSection);

        java.time.LocalDate dashboardSalesStart = dashboardData.salesMix().startDate();
        java.time.LocalDate dashboardSalesEnd = dashboardData.salesMix().endDate();
        if (canOpenReports) {
            makeDashboardDrillDown(
                revenueCard,
                "Open today's sales summary in Reports",
                () -> context.navigator().showReports(java.time.LocalDate.now(), java.time.LocalDate.now(), ReportFocusTarget.SUMMARY)
            );
            makeDashboardDrillDown(
                ordersCard,
                "Open today's orders summary in Reports",
                () -> context.navigator().showReports(java.time.LocalDate.now(), java.time.LocalDate.now(), ReportFocusTarget.SUMMARY)
            );
            makeDashboardDrillDown(
                revenueSection,
                "Open revenue chart in Reports",
                () -> context.navigator().showReports(dashboardSalesStart, dashboardSalesEnd, ReportFocusTarget.REVENUE)
            );
            makeDashboardDrillDown(
                ordersSection,
                "Open orders chart in Reports",
                () -> context.navigator().showReports(dashboardSalesStart, dashboardSalesEnd, ReportFocusTarget.ORDERS)
            );
            makeDashboardDrillDown(
                canceledOrdersSection,
                "Open canceled orders chart in Reports",
                () -> context.navigator().showReports(dashboardSalesStart, dashboardSalesEnd, ReportFocusTarget.CANCELED_ORDERS)
            );
            makeDashboardDrillDown(
                paymentSection,
                "Open payment method share in Reports",
                () -> context.navigator().showReports(dashboardSalesStart, dashboardSalesEnd, ReportFocusTarget.PAYMENT_METHOD_SHARE)
            );
            makeDashboardDrillDown(
                topSellingSection,
                "Open top selling products in Reports",
                () -> context.navigator().showReports(dashboardSalesStart, dashboardSalesEnd, ReportFocusTarget.TOP_SELLING)
            );
        }

        content.getChildren().setAll(assembleDashboardNodes(user, null, availableDashboardSections, dashboardContentWidth));
        return content;
    }

    private VBox createDashboardStateContent(String title, String message, Button actionButton, boolean loading) {
        VBox content = new VBox(20);
        content.getStyleClass().add("dashboard-page");
        content.setPadding(new Insets(20));
        content.setFillWidth(true);
        content.setMaxWidth(Double.MAX_VALUE);

        VBox stateCard = new VBox(14);
        stateCard.getStyleClass().add("report-section-card");
        stateCard.setPadding(new Insets(22));
        stateCard.setMaxWidth(Double.MAX_VALUE);

        Label stateTitle = new Label(title);
        stateTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: -app-text-primary;");

        Label stateMessage = new Label(message);
        stateMessage.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
        stateMessage.setWrapText(true);

        if (loading) {
            javafx.scene.control.ProgressIndicator indicator = new javafx.scene.control.ProgressIndicator();
            indicator.setPrefSize(38, 38);
            indicator.setMaxSize(38, 38);
            stateCard.getChildren().addAll(stateTitle, stateMessage, indicator);
        } else {
            stateCard.getChildren().addAll(stateTitle, stateMessage);
        }

        if (actionButton != null) {
            stateCard.getChildren().add(actionButton);
        }

        content.getChildren().add(stateCard);
        return content;
    }

    private VBox createOperationalReportsStateContent(String title, String message, Button actionButton, boolean loading) {
        VBox content = new VBox(20);
        content.setFillWidth(true);
        content.setMaxWidth(Double.MAX_VALUE);

        VBox stateCard = new VBox(14);
        stateCard.getStyleClass().add("report-section-card");
        stateCard.setPadding(new Insets(22));
        stateCard.setMaxWidth(Double.MAX_VALUE);

        Label stateTitle = new Label(title);
        stateTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: -app-text-primary;");

        Label stateMessage = new Label(message);
        stateMessage.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-secondary;");
        stateMessage.setWrapText(true);

        if (loading) {
            javafx.scene.control.ProgressIndicator indicator = new javafx.scene.control.ProgressIndicator();
            indicator.setPrefSize(38, 38);
            indicator.setMaxSize(38, 38);
            stateCard.getChildren().addAll(stateTitle, stateMessage, indicator);
        } else {
            stateCard.getChildren().addAll(stateTitle, stateMessage);
        }

        if (actionButton != null) {
            stateCard.getChildren().add(actionButton);
        }

        content.getChildren().add(stateCard);
        return content;
    }

    private VBox createOperationalReportsView(
        Stage stage,
        User user,
        java.time.LocalDate initialStartDate,
        java.time.LocalDate initialEndDate,
        ReportFocusTarget initialFocusTarget
    ) {
        VBox pageContent = new VBox(20);
        pageContent.getStyleClass().add("reports-page");
        pageContent.setPadding(new Insets(20));

        Label filterLabel = new Label("Date Range");
        filterLabel.getStyleClass().add("report-date-range-label");

        javafx.scene.control.DatePicker startDatePicker = new javafx.scene.control.DatePicker();
        startDatePicker.setPromptText("Start Date");
        startDatePicker.setPrefWidth(140);
        startDatePicker.getStyleClass().add("report-date-picker");

        javafx.scene.control.DatePicker endDatePicker = new javafx.scene.control.DatePicker();
        endDatePicker.setPromptText("End Date");
        endDatePicker.setPrefWidth(140);
        endDatePicker.getStyleClass().add("report-date-picker");

        context.support().customizeDatePicker(startDatePicker);
        context.support().customizeDatePicker(endDatePicker);

        Button applyFilterButton = new Button("Apply");
        applyFilterButton.getStyleClass().addAll("button", "primary-button");
        applyFilterButton.setStyle("-fx-padding: 8 18; -fx-background-radius: 999;");

        Button resetFilterButton = new Button("Reset");
        resetFilterButton.getStyleClass().addAll("button", "dashboard-report-secondary-button");
        resetFilterButton.setStyle("-fx-padding: 8 18; -fx-background-radius: 999;");

        Button todayPresetButton = createReportPresetButton("Today");
        Button last7DaysPresetButton = createReportPresetButton("Last 7 Days");
        Button thisMonthPresetButton = createReportPresetButton("This Month");
        Button allTimePresetButton = createReportPresetButton("All Time");

        Label activeRangeLabel = new Label();
        activeRangeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -app-text-muted;");
        activeRangeLabel.setWrapText(true);

        Label dateSeparator = new Label("-");
        dateSeparator.getStyleClass().add("report-date-range-separator");

        javafx.scene.layout.HBox filterBar = new javafx.scene.layout.HBox(
            10,
            filterLabel,
            startDatePicker,
            dateSeparator,
            endDatePicker,
            applyFilterButton,
            resetFilterButton
        );
        filterBar.getStyleClass().add("report-date-range-bar");
        filterBar.setAlignment(Pos.CENTER_LEFT);

        javafx.scene.layout.HBox presetBar = new javafx.scene.layout.HBox(
            8,
            todayPresetButton,
            last7DaysPresetButton,
            thisMonthPresetButton,
            allTimePresetButton
        );
        presetBar.setAlignment(Pos.CENTER_LEFT);

        VBox headerBox = new VBox(8, filterBar, presetBar, activeRangeLabel);

        VBox reportSections = new VBox(20);
        reportSections.setFillWidth(true);
        reportSections.setMaxWidth(Double.MAX_VALUE);
        final boolean[] initialFocusPending = {initialFocusTarget != null};
        final long[] reportLoadVersion = {0L};
        startDatePicker.setValue(initialStartDate);
        endDatePicker.setValue(initialEndDate);

        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(pageContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        javafx.beans.binding.DoubleBinding reportViewportWidth = javafx.beans.binding.Bindings.createDoubleBinding(
            () -> Math.max(420.0, scrollPane.getViewportBounds().getWidth()),
            scrollPane.viewportBoundsProperty()
        );

        Runnable[] refreshReportsRef = new Runnable[1];
        refreshReportsRef[0] = () -> {
            java.time.LocalDate startDate = startDatePicker.getValue();
            java.time.LocalDate endDate = endDatePicker.getValue();
            long loadVersion = ++reportLoadVersion[0];

            filterBar.setMouseTransparent(true);
            presetBar.setMouseTransparent(true);
            activeRangeLabel.setText("Loading report data...");
            reportSections.setOpacity(1.0);
            reportSections.getChildren().setAll(createOperationalReportsStateContent(
                "Loading reports",
                "Preparing operational reports and sales summaries...",
                null,
                true
            ));

            javafx.concurrent.Task<com.pbl3.project.pbl3_project.dto.report.OperationalReportData> task =
                new javafx.concurrent.Task<>() {
                    @Override
                    protected com.pbl3.project.pbl3_project.dto.report.OperationalReportData call() {
                        return context.reportService().getOperationalReportData(startDate, endDate);
                    }
                };

            task.setOnSucceeded(event -> {
                if (loadVersion != reportLoadVersion[0]) {
                    return;
                }
                filterBar.setMouseTransparent(false);
                presetBar.setMouseTransparent(false);

                com.pbl3.project.pbl3_project.dto.report.OperationalReportData reportData = task.getValue();
                activeRangeLabel.setText(buildOperationalReportContextLabel(reportData));
                ReportSectionsBundle sectionsBundle = createOperationalReportSections(
                    stage,
                    user,
                    pageContent,
                    scrollPane,
                    reportData,
                    startDate,
                    endDate,
                    initialFocusTarget,
                    reportViewportWidth
                );
                reportSections.getChildren().setAll(sectionsBundle.nodes());

                if (initialFocusPending[0]) {
                    initialFocusPending[0] = false;
                    javafx.scene.Node focusNode = sectionsBundle.anchors().get(initialFocusTarget);
                    if (focusNode != null) {
                        revealReportSection(scrollPane, focusNode);
                    }
                }
            });

            task.setOnFailed(event -> {
                if (loadVersion != reportLoadVersion[0]) {
                    return;
                }
                filterBar.setMouseTransparent(false);
                presetBar.setMouseTransparent(false);

                Throwable ex = task.getException();
                Button retryButton = new Button("Retry");
                retryButton.getStyleClass().addAll("button", "dashboard-report-secondary-button");
                retryButton.setOnAction(e -> refreshReportsRef[0].run());

                activeRangeLabel.setText("Could not load report data.");
                reportSections.setOpacity(1.0);
                reportSections.getChildren().setAll(createOperationalReportsStateContent(
                    "Reports unavailable",
                    ex != null && ex.getMessage() != null && !ex.getMessage().isBlank()
                        ? "Failed to load reports" + ": " + ex.getMessage()
                        : "Could not load report data.",
                    retryButton,
                    false
                ));

                if (ex != null && ex.getMessage() != null && !ex.getMessage().isBlank()) {
                    context.toastService().showError("Failed to load reports" + ": " + ex.getMessage());
                } else {
                    context.toastService().showError("Failed to load reports");
                }
            });

            UiTaskExecutor.execute(task, "operational-reports-loader");
        };

        applyFilterButton.setOnAction(e -> {
            java.time.LocalDate startDate = startDatePicker.getValue();
            java.time.LocalDate endDate = endDatePicker.getValue();
            if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
                context.toastService().showWarning("End date must be after start date.");
                return;
            }
            refreshReportsRef[0].run();
        });

        resetFilterButton.setOnAction(e -> {
            startDatePicker.setValue(null);
            endDatePicker.setValue(null);
            refreshReportsRef[0].run();
        });

        todayPresetButton.setOnAction(e -> {
            java.time.LocalDate today = java.time.LocalDate.now();
            startDatePicker.setValue(today);
            endDatePicker.setValue(today);
            refreshReportsRef[0].run();
        });

        last7DaysPresetButton.setOnAction(e -> {
            java.time.LocalDate today = java.time.LocalDate.now();
            startDatePicker.setValue(today.minusDays(6));
            endDatePicker.setValue(today);
            refreshReportsRef[0].run();
        });

        thisMonthPresetButton.setOnAction(e -> {
            java.time.LocalDate today = java.time.LocalDate.now();
            startDatePicker.setValue(today.withDayOfMonth(1));
            endDatePicker.setValue(today);
            refreshReportsRef[0].run();
        });

        allTimePresetButton.setOnAction(e -> {
            startDatePicker.setValue(null);
            endDatePicker.setValue(null);
            refreshReportsRef[0].run();
        });

        activeRangeLabel.setText("Loading report data...");
        reportSections.getChildren().setAll(createOperationalReportsStateContent(
            "Loading reports",
            "Preparing operational reports and sales summaries...",
            null,
            true
        ));
        pageContent.getChildren().addAll(headerBox, reportSections);

        VBox root = new VBox(scrollPane);
        VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);
        final javafx.beans.value.ChangeListener<javafx.scene.Scene>[] sceneListenerRef = new javafx.beans.value.ChangeListener[1];
        sceneListenerRef[0] = (obs, oldScene, newScene) -> {
            if (newScene == null) {
                return;
            }
            root.sceneProperty().removeListener(sceneListenerRef[0]);
            javafx.animation.PauseTransition initialLoadDelay =
                new javafx.animation.PauseTransition(javafx.util.Duration.millis(190));
            initialLoadDelay.setOnFinished(event -> refreshReportsRef[0].run());
            initialLoadDelay.play();
        };
        root.sceneProperty().addListener(sceneListenerRef[0]);
        return root;
    }

    private ReportSectionsBundle createOperationalReportSections(
        Stage stage,
        User user,
        VBox interactionRoot,
        javafx.scene.control.ScrollPane scrollPane,
        com.pbl3.project.pbl3_project.dto.report.OperationalReportData reportData,
        java.time.LocalDate startDate,
        java.time.LocalDate endDate,
        ReportFocusTarget activeFocusTarget,
        javafx.beans.value.ObservableNumberValue widthSource
    ) {
        String rangeLabel = formatOperationalReportRangeLabel(startDate, endDate);
        java.util.Map<ReportFocusTarget, javafx.scene.Node> anchors = new java.util.EnumMap<>(ReportFocusTarget.class);

        VBox netRevenueCard = createDashboardCard(
            "Net Revenue",
            context.support().formatVnd(reportData.summary().netRevenue()),
            "-app-success",
            createRevenuePanelIcon()
        );
        VBox estimatedCostCard = createDashboardCard(
            "Estimated Cost (COGS)",
            context.support().formatVnd(reportData.summary().estimatedCost()),
            "#fe9900",
            createExpensesPanelIcon()
        );
        boolean grossProfitPositive = MoneySupport.normalize(reportData.summary().grossProfit()).signum() >= 0;
        VBox grossProfitCard = createDashboardCard(
            "Gross Profit",
            context.support().formatVnd(reportData.summary().grossProfit()),
            grossProfitPositive ? "-app-primary" : "-app-danger",
            createEstimatedProfitPanelIcon(grossProfitPositive)
        );
        VBox operatingExpensesCard = createDashboardCard(
            "Operating Expenses",
            context.support().formatVnd(reportData.summary().operatingExpenses()),
            "#fe9900",
            createExpensesPanelIcon()
        );
        boolean netProfitPositive = MoneySupport.normalize(reportData.summary().netProfit()).signum() >= 0;
        VBox netProfitCard = createDashboardCard(
            "Net Profit",
            context.support().formatVnd(reportData.summary().netProfit()),
            netProfitPositive ? "-app-success" : "-app-danger",
            createEstimatedProfitPanelIcon(netProfitPositive)
        );
        VBox unitsSoldCard = createDashboardCard(
            "Net Units Sold",
            String.valueOf(reportData.summary().netUnitsSold()),
            "#fe9900",
            createNetUnitsPanelIcon()
        );
        javafx.scene.layout.GridPane summaryPrimaryRow = createResponsiveDashboardKpiRow(
            netRevenueCard,
            estimatedCostCard,
            grossProfitCard,
            widthSource,
            1180.0
        );
        javafx.scene.layout.GridPane summarySecondaryRow = createResponsiveDashboardKpiRow(
            operatingExpensesCard,
            netProfitCard,
            unitsSoldCard,
            widthSource,
            1180.0
        );
        VBox summaryContent = new VBox(20, summaryPrimaryRow, summarySecondaryRow);

        Button exportSummaryBtn = createReportExportButton("Export Summary CSV");
        exportSummaryBtn.setOnAction(e -> exportCsv(
            stage,
            buildReportCsvFileName("operational-summary", startDate, endDate),
            java.util.List.of("Metric", "Value"),
            java.util.List.of(
                java.util.List.<String>of("Date Range", rangeLabel),
                java.util.List.<String>of("Net Revenue", context.support().formatVnd(reportData.summary().netRevenue())),
                java.util.List.<String>of("Estimated Cost", context.support().formatVnd(reportData.summary().estimatedCost())),
                java.util.List.<String>of("Gross Profit", context.support().formatVnd(reportData.summary().grossProfit())),
                java.util.List.<String>of("Operating Expenses", context.support().formatVnd(reportData.summary().operatingExpenses())),
                java.util.List.<String>of("Net Profit", context.support().formatVnd(reportData.summary().netProfit())),
                java.util.List.<String>of("Net Units Sold", String.valueOf(reportData.summary().netUnitsSold())),
                java.util.List.<String>of("Active SKUs", String.valueOf(reportData.summary().activeSkuCount())),
                java.util.List.<String>of("Low Stock SKUs", String.valueOf(reportData.summary().lowStockSkuCount())),
                java.util.List.<String>of("Refunded Amount", context.support().formatVnd(reportData.summary().refundedAmount()))
            )
        ));

        VBox summarySection = createReportSection(
            "Summary",
            null,
            summaryContent,
            exportSummaryBtn,
            activeFocusTarget == ReportFocusTarget.SUMMARY ? "From Dashboard" : null
        );
        enableScrollPerfCache(summarySection);
        anchors.put(ReportFocusTarget.SUMMARY, summarySection);

        java.util.function.Function<com.pbl3.project.pbl3_project.dto.report.InsightDrilldownTarget, Runnable> reportInsightActionResolver =
            target -> createReportInsightAction(scrollPane, anchors, target);

        Button exportActionCenterBtn = createReportExportButton("Export CSV");
        exportActionCenterBtn.setOnAction(e -> exportCsv(
            stage,
            buildReportCsvFileName("action-center", startDate, endDate),
            java.util.List.of("Type", "Severity", "Title", "Description", "Action", "Impact"),
            reportData.actionCenter() == null || reportData.actionCenter().items() == null
                ? java.util.List.of()
                : reportData.actionCenter().items().stream().map(item -> java.util.List.<String>of(
                    item.type().name(),
                    item.severity().name(),
                    item.title(),
                    item.description(),
                    item.actionLabel() != null ? item.actionLabel() : "",
                    item.impactLabel() != null ? item.impactLabel() : ""
                )).toList()
        ));

        VBox actionCenterSection = createReportSection(
            "Action Center",
            "Prioritized actions from stock and sales signals",
            createActionCenterContent(stage, user, reportData.actionCenter(), Integer.MAX_VALUE, reportInsightActionResolver),
            exportActionCenterBtn,
            activeFocusTarget == ReportFocusTarget.ACTION_CENTER ? "From Dashboard" : null
        );
        bindReportSectionFullWidth(actionCenterSection, widthSource);
        enableScrollPerfCache(actionCenterSection);
        anchors.put(ReportFocusTarget.ACTION_CENTER, actionCenterSection);

        VBox whatChangedSection = createReportSection(
            "What Changed",
            reportData.whatChanged() != null
                ? java.text.MessageFormat.format("Compared with {0}", reportData.whatChanged().baselineRangeLabel())
                : "Compared with previous period",
            createWhatChangedContent(
                reportData.whatChanged(),
                reportInsightActionResolver,
                "No prior comparison available for the selected range",
                widthSource
            ),
            null,
            activeFocusTarget == ReportFocusTarget.WHAT_CHANGED ? "From Dashboard" : null
        );
        bindReportSectionFullWidth(whatChangedSection, widthSource);
        enableScrollPerfCache(whatChangedSection);
        anchors.put(ReportFocusTarget.WHAT_CHANGED, whatChangedSection);

        Button exportReorderBtn = createReportExportButton("Export CSV");
        exportReorderBtn.setOnAction(e -> exportCsv(
            stage,
            buildReportCsvFileName("explainable-reorder", startDate, endDate),
            java.util.List.of("Product", "Category", "On Hand", "Min Stock", "Avg Daily Units 14d", "Coverage Days", "Suggested Qty", "Last Inbound", "Latest Import Price", "Latest Supplier", "Explanation"),
            reportData.reorder() == null || reportData.reorder().rows() == null
                ? java.util.List.of()
                : reportData.reorder().rows().stream().map(row -> java.util.List.<String>of(
                    row.productName(),
                    row.categoryName(),
                    String.valueOf(row.onHandQuantity()),
                    String.valueOf(row.minStockLevel()),
                    formatCompactDecimal(row.avgDailyUnits14d()),
                    row.coverageKnown() && row.coverageDays() != null ? formatCompactDecimal(row.coverageDays()) : "",
                    String.valueOf(row.suggestedReorderQty()),
                    context.support().formatDateTime(row.lastInboundAt()),
                    row.latestImportPrice() != null ? context.support().formatVnd(row.latestImportPrice()) : "",
                    row.latestSupplierName() != null ? row.latestSupplierName() : "",
                    row.explanation()
                )).toList()
        ));

        VBox reorderSection = createReportSection(
            "Explainable Reorder",
            "Suggested replenishment using the last 14 days of demand",
            createExplainableReorderContent(stage, user, reportData.reorder(), Integer.MAX_VALUE, true),
            exportReorderBtn,
            activeFocusTarget == ReportFocusTarget.REORDER ? "From Dashboard" : null
        );
        bindReportSectionFullWidth(reorderSection, widthSource);
        anchors.put(ReportFocusTarget.REORDER, reorderSection);

        javafx.scene.chart.LineChart<String, Number> revenueChart = createReportSeriesLineChart("Date", "Revenue (VND)");
        if (revenueChart.getXAxis() instanceof javafx.scene.chart.CategoryAxis revenueXAxis) {
            configureDashboardCategoryAxis(revenueXAxis, new java.util.ArrayList<>(reportData.salesMix().revenueSeries().keySet()));
        }
        javafx.scene.chart.XYChart.Series<String, Number> revenueSeries = new javafx.scene.chart.XYChart.Series<>();
        reportData.salesMix().revenueSeries().forEach((label, value) -> revenueSeries.getData().add(
            createDashboardLineData(label, value, SUCCESS_BAR_FILL, label + ": " + context.support().formatVnd(value))
        ));
        applyLineSeriesStyling(revenueSeries, SUCCESS_BAR_FILL);
        revenueChart.getData().add(revenueSeries);
        configureDashboardVerticalValueAxis(revenueChart, reportData.salesMix().revenueSeries().values(), false);

        java.util.List<java.util.List<String>> revenueExportRows = buildRevenueExportRows(reportData.salesMix().revenueSeries());
        Button exportRevenueBtn = createReportExportButton("Export CSV");
        exportRevenueBtn.setOnAction(e -> exportCsv(
            stage,
            buildReportCsvFileName("revenue-series", reportData.salesMix().startDate(), reportData.salesMix().endDate()),
            java.util.List.of("Label", "Revenue"),
            revenueExportRows
        ));
        Button exportRevenuePdfBtn = createReportExportButton("Export PDF");
        exportRevenuePdfBtn.setOnAction(e -> exportPdf(
            stage,
            "Revenue Report",
            buildReportPdfFileName("revenue-series", reportData.salesMix().startDate(), reportData.salesMix().endDate()),
            java.util.List.of(
                java.util.List.of("Range", formatOperationalReportRangeLabel(reportData.salesMix().startDate(), reportData.salesMix().endDate())),
                java.util.List.of("Net Revenue", context.support().formatVnd(reportData.summary().netRevenue())),
                java.util.List.of("Orders", String.valueOf(reportData.salesMix().orderSeries().values().stream().mapToLong(Long::longValue).sum()))
            ),
            java.util.List.of("Label", "Revenue"),
            revenueExportRows
        ));

        VBox revenueSection = createReportSection(
            "Revenue",
            null,
            revenueChart,
            createReportActionGroup(exportRevenueBtn, exportRevenuePdfBtn),
            activeFocusTarget == ReportFocusTarget.REVENUE ? "From Dashboard" : null
        );
        bindReportSectionWidth(revenueSection, widthSource);
        enableScrollPerfCache(revenueChart);
        enableScrollPerfCache(revenueSection);
        anchors.put(ReportFocusTarget.REVENUE, revenueSection);

        javafx.scene.chart.LineChart<String, Number> ordersChart = createReportSeriesLineChart("Date", "Orders");
        if (ordersChart.getXAxis() instanceof javafx.scene.chart.CategoryAxis ordersXAxis) {
            configureDashboardCategoryAxis(ordersXAxis, new java.util.ArrayList<>(reportData.salesMix().orderSeries().keySet()));
        }
        javafx.scene.chart.XYChart.Series<String, Number> ordersSeries = new javafx.scene.chart.XYChart.Series<>();
        reportData.salesMix().orderSeries().forEach((label, value) -> ordersSeries.getData().add(
            createDashboardLineData(label, value, PRIMARY_BAR_FILL, java.text.MessageFormat.format("{0}: {1} orders", label, value))
        ));
        applyLineSeriesStyling(ordersSeries, PRIMARY_BAR_FILL);
        ordersChart.getData().add(ordersSeries);
        configureDashboardVerticalValueAxis(ordersChart, reportData.salesMix().orderSeries().values(), true);

        Button exportOrdersBtn = createReportExportButton("Export CSV");
        exportOrdersBtn.setOnAction(e -> exportCsv(
            stage,
            buildReportCsvFileName("orders-series", reportData.salesMix().startDate(), reportData.salesMix().endDate()),
            java.util.List.of("Label", "Orders"),
            reportData.salesMix().orderSeries().entrySet().stream()
                .map(entry -> java.util.List.of(entry.getKey(), String.valueOf(entry.getValue())))
                .toList()
        ));

        VBox ordersSection = createReportSection(
            "Orders",
            null,
            ordersChart,
            exportOrdersBtn,
            activeFocusTarget == ReportFocusTarget.ORDERS ? "From Dashboard" : null
        );
        bindReportSectionWidth(ordersSection, widthSource);
        enableScrollPerfCache(ordersChart);
        enableScrollPerfCache(ordersSection);
        anchors.put(ReportFocusTarget.ORDERS, ordersSection);

        javafx.scene.chart.LineChart<String, Number> canceledOrdersChart = createReportSeriesLineChart("Date", "Canceled Orders");
        if (canceledOrdersChart.getXAxis() instanceof javafx.scene.chart.CategoryAxis canceledOrdersXAxis) {
            configureDashboardCategoryAxis(canceledOrdersXAxis, new java.util.ArrayList<>(reportData.salesMix().canceledOrderSeries().keySet()));
        }
        javafx.scene.chart.XYChart.Series<String, Number> canceledOrdersSeries = new javafx.scene.chart.XYChart.Series<>();
        reportData.salesMix().canceledOrderSeries().forEach((label, value) -> canceledOrdersSeries.getData().add(
            createDashboardLineData(label, value, DANGER_BAR_FILL, java.text.MessageFormat.format("{0}: {1} canceled orders", label, value))
        ));
        applyLineSeriesStyling(canceledOrdersSeries, DANGER_BAR_FILL);
        canceledOrdersChart.getData().add(canceledOrdersSeries);
        configureDashboardVerticalValueAxis(canceledOrdersChart, reportData.salesMix().canceledOrderSeries().values(), true);

        Button exportCanceledOrdersBtn = createReportExportButton("Export CSV");
        exportCanceledOrdersBtn.setOnAction(e -> exportCsv(
            stage,
            buildReportCsvFileName("canceled-orders-series", reportData.salesMix().startDate(), reportData.salesMix().endDate()),
            java.util.List.of("Label", "Canceled Orders"),
            reportData.salesMix().canceledOrderSeries().entrySet().stream()
                .map(entry -> java.util.List.of(entry.getKey(), String.valueOf(entry.getValue())))
                .toList()
        ));

        VBox canceledOrdersSection = createReportSection(
            "Canceled Orders",
            null,
            canceledOrdersChart,
            exportCanceledOrdersBtn,
            activeFocusTarget == ReportFocusTarget.CANCELED_ORDERS ? "From Dashboard" : null
        );
        bindReportSectionFullWidth(canceledOrdersSection, widthSource);
        enableScrollPerfCache(canceledOrdersChart);
        enableScrollPerfCache(canceledOrdersSection);
        anchors.put(ReportFocusTarget.CANCELED_ORDERS, canceledOrdersSection);

        javafx.scene.layout.GridPane salesChartsRow = createResponsiveReportPairRow(
            revenueSection,
            ordersSection,
            widthSource,
            980.0
        );
        enableScrollPerfCache(salesChartsRow);

        javafx.scene.Node paymentChartContent = createPaymentMethodShareContent(
            reportData.salesMix().paymentMethodShare(),
            buildNoSalesRangeText(reportData.salesMix().startDate(), reportData.salesMix().endDate())
        );
        Button exportPaymentBtn = createReportExportButton("Export CSV");
        exportPaymentBtn.setOnAction(e -> exportCsv(
            stage,
            buildReportCsvFileName("payment-method-share", reportData.salesMix().startDate(), reportData.salesMix().endDate()),
            java.util.List.of("Payment Method", "Orders"),
            reportData.salesMix().paymentMethodShare().entrySet().stream()
                .map(entry -> java.util.List.of(formatPaymentMethodLabel(entry.getKey()), String.valueOf(entry.getValue())))
                .toList()
        ));

        VBox paymentSection = createReportSection(
            "Payment Method Share",
            null,
            paymentChartContent,
            exportPaymentBtn,
            activeFocusTarget == ReportFocusTarget.PAYMENT_METHOD_SHARE ? "From Dashboard" : null
        );
        enableScrollPerfCache(paymentChartContent);
        enableScrollPerfCache(paymentSection);
        anchors.put(ReportFocusTarget.PAYMENT_METHOD_SHARE, paymentSection);

        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.dto.report.ExpenseCategorySummaryRow> expenseCategoryTable =
            new javafx.scene.control.TableView<>();
        TableViewSupport.prepareNonReorderableTable(expenseCategoryTable);
        expenseCategoryTable.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        expenseCategoryTable.setItems(javafx.collections.FXCollections.observableArrayList(reportData.expenseCategorySummaries()));
        expenseCategoryTable.setPrefHeight(240);

        BigDecimal totalOperatingExpenses = MoneySupport.normalize(reportData.summary().operatingExpenses());
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.ExpenseCategorySummaryRow, String> expenseCategoryNameCol =
            new javafx.scene.control.TableColumn<>("Category");
        expenseCategoryNameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            formatExpenseCategoryLabel(data.getValue().category())
        ));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.ExpenseCategorySummaryRow, Number> expenseEntriesCol =
            new javafx.scene.control.TableColumn<>("Entries");
        expenseEntriesCol.setCellValueFactory(data -> new javafx.beans.property.SimpleLongProperty(data.getValue().entryCount()));
        expenseEntriesCol.setStyle("-fx-alignment: CENTER;");
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.ExpenseCategorySummaryRow, String> expenseAmountCol =
            new javafx.scene.control.TableColumn<>("Total Amount");
        expenseAmountCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(context.support().formatVnd(data.getValue().totalAmount())));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.ExpenseCategorySummaryRow, String> expenseShareCol =
            new javafx.scene.control.TableColumn<>("Share");
        expenseShareCol.setCellValueFactory(data -> {
            BigDecimal totalAmount = MoneySupport.normalize(data.getValue().totalAmount());
            if (MoneySupport.isZero(totalOperatingExpenses)) {
                return new javafx.beans.property.SimpleStringProperty("0.0%");
            }
            double share = totalAmount.doubleValue() * 100.0 / totalOperatingExpenses.doubleValue();
            return new javafx.beans.property.SimpleStringProperty(String.format("%.1f%%", share));
        });
        expenseShareCol.setStyle("-fx-alignment: CENTER;");
        expenseCategoryTable.getColumns().addAll(expenseCategoryNameCol, expenseEntriesCol, expenseAmountCol, expenseShareCol);

        Button exportExpensesBtn = createReportExportButton("Export CSV");
        exportExpensesBtn.setOnAction(e -> exportCsv(
            stage,
            buildReportCsvFileName("expenses-by-category", startDate, endDate),
            java.util.List.of("Category", "Entries", "Total Amount", "Share"),
            reportData.expenseCategorySummaries().stream().map(row -> {
                BigDecimal rowTotal = MoneySupport.normalize(row.totalAmount());
                String share = MoneySupport.isZero(totalOperatingExpenses)
                    ? "0.0%"
                    : String.format("%.1f%%", rowTotal.doubleValue() * 100.0 / totalOperatingExpenses.doubleValue());
                return java.util.List.<String>of(
                    formatExpenseCategoryLabel(row.category()),
                    String.valueOf(row.entryCount()),
                    context.support().formatVnd(row.totalAmount()),
                    share
                );
            }).toList()
        ));

        VBox expensesSection = createReportSection(
            "Expenses by Category",
            null,
            expenseCategoryTable,
            exportExpensesBtn,
            null
        );
        bindReportSectionFullWidth(expensesSection, widthSource);

        VBox promotionDiscountCard = createDashboardCard(
            "Promotion Discount",
            context.support().formatVnd(reportData.promotionReport().totalDiscount()),
            "-app-primary",
            SidebarIconFactory.createPromotionsNavIcon()
        );
        VBox promotedOrdersCard = createDashboardCard(
            "Promoted Orders",
            String.valueOf(reportData.promotionReport().promotedOrderCount()),
            "-app-success",
            SidebarIconFactory.createPromotionsNavIcon()
        );
        VBox activePromotionsCard = createDashboardCard(
            "Active Promotions",
            String.valueOf(reportData.promotionReport().activePromotions().size()),
            "#fe9900",
            SidebarIconFactory.createPromotionsNavIcon()
        );
        javafx.scene.layout.GridPane promotionSummaryRow = createResponsiveDashboardKpiRow(
            promotionDiscountCard,
            promotedOrdersCard,
            activePromotionsCard,
            widthSource,
            1180.0
        );

        Label promotionImpactTitle = new Label("Top Promotions");
        promotionImpactTitle.getStyleClass().add("header-label");
        promotionImpactTitle.setStyle("-fx-font-size: 16px;");
        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.dto.report.PromotionImpactRow> promotionImpactTable =
            new javafx.scene.control.TableView<>();
        TableViewSupport.prepareNonReorderableTable(promotionImpactTable);
        promotionImpactTable.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        promotionImpactTable.setItems(javafx.collections.FXCollections.observableArrayList(reportData.promotionReport().topPromotions()));
        promotionImpactTable.setPrefHeight(240);
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.PromotionImpactRow, String> impactNameCol =
            new javafx.scene.control.TableColumn<>("Promotion");
        impactNameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().promotionName()));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.PromotionImpactRow, String> impactScopeCol =
            new javafx.scene.control.TableColumn<>("Scope");
        impactScopeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatPromotionScopeLabel(data.getValue().scope())));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.PromotionImpactRow, Number> impactUsageCol =
            new javafx.scene.control.TableColumn<>("Usage");
        impactUsageCol.setCellValueFactory(data -> new javafx.beans.property.SimpleLongProperty(data.getValue().usageCount()));
        impactUsageCol.setStyle("-fx-alignment: CENTER;");
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.PromotionImpactRow, String> impactDiscountCol =
            new javafx.scene.control.TableColumn<>("Discount Given");
        impactDiscountCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(context.support().formatVnd(data.getValue().totalDiscount())));
        promotionImpactTable.getColumns().addAll(impactNameCol, impactScopeCol, impactUsageCol, impactDiscountCol);

        Label activePromotionTitle = new Label("Active Promotions");
        activePromotionTitle.getStyleClass().add("header-label");
        activePromotionTitle.setStyle("-fx-font-size: 16px;");
        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.dto.report.ActivePromotionRow> activePromotionTable =
            new javafx.scene.control.TableView<>();
        TableViewSupport.prepareNonReorderableTable(activePromotionTable);
        activePromotionTable.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        activePromotionTable.setItems(javafx.collections.FXCollections.observableArrayList(reportData.promotionReport().activePromotions()));
        activePromotionTable.setPrefHeight(220);
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.ActivePromotionRow, String> activePromotionNameCol =
            new javafx.scene.control.TableColumn<>("Promotion");
        activePromotionNameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().promotionName()));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.ActivePromotionRow, String> activePromotionScopeCol =
            new javafx.scene.control.TableColumn<>("Scope");
        activePromotionScopeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatPromotionScopeLabel(data.getValue().scope())));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.ActivePromotionRow, String> activePromotionTargetCol =
            new javafx.scene.control.TableColumn<>("Target");
        activePromotionTargetCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().targetLabel()));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.ActivePromotionRow, String> activePromotionDiscountCol =
            new javafx.scene.control.TableColumn<>("Discount");
        activePromotionDiscountCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().discountLabel()));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.ActivePromotionRow, String> activePromotionStatusCol =
            new javafx.scene.control.TableColumn<>("Status");
        activePromotionStatusCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().statusLabel()));
        activePromotionTable.getColumns().addAll(
            activePromotionNameCol,
            activePromotionScopeCol,
            activePromotionTargetCol,
            activePromotionDiscountCol,
            activePromotionStatusCol
        );

        VBox promotionSectionContent = new VBox(
            16,
            promotionSummaryRow,
            promotionImpactTitle,
            promotionImpactTable,
            activePromotionTitle,
            activePromotionTable
        );
        VBox promotionSection = createReportSection(
            "Promotion Impact",
            "Discount usage and currently active promotions",
            promotionSectionContent,
            null,
            null
        );
        bindReportSectionFullWidth(promotionSection, widthSource);

        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.dto.report.TopSellingProductRow> topSellingTable = new javafx.scene.control.TableView<>();
        TableViewSupport.prepareNonReorderableTable(topSellingTable);
        topSellingTable.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        topSellingTable.setItems(javafx.collections.FXCollections.observableArrayList(reportData.topSellingProducts()));
        topSellingTable.setPrefHeight(280);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.TopSellingProductRow, String> topProductCol = new javafx.scene.control.TableColumn<>("Product");
        topProductCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().productName()));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.TopSellingProductRow, String> topCategoryCol = new javafx.scene.control.TableColumn<>("Category");
        topCategoryCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().categoryName()));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.TopSellingProductRow, Number> topQtyCol = new javafx.scene.control.TableColumn<>("Net Sold");
        topQtyCol.setCellValueFactory(data -> new javafx.beans.property.SimpleLongProperty(data.getValue().netSoldQuantity()));
        topQtyCol.setStyle("-fx-alignment: CENTER;");
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.TopSellingProductRow, String> topRevenueCol = new javafx.scene.control.TableColumn<>("Revenue");
        topRevenueCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(context.support().formatVnd(data.getValue().netRevenue())));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.TopSellingProductRow, String> topProfitCol = new javafx.scene.control.TableColumn<>("Est. Profit");
        topProfitCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(context.support().formatVnd(data.getValue().estimatedProfit())));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.TopSellingProductRow, Number> topOnHandCol = new javafx.scene.control.TableColumn<>("On Hand");
        topOnHandCol.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().onHandQuantity()));
        topOnHandCol.setStyle("-fx-alignment: CENTER;");
        topSellingTable.getColumns().addAll(topProductCol, topCategoryCol, topQtyCol, topRevenueCol, topProfitCol, topOnHandCol);

        Button exportTopSellingBtn = createReportExportButton("Export CSV");
        exportTopSellingBtn.setOnAction(e -> exportCsv(
            stage,
            buildReportCsvFileName("top-selling-products", startDate, endDate),
            java.util.List.of("Product", "Category", "Net Sold", "Revenue", "Estimated Profit", "On Hand"),
            reportData.topSellingProducts().stream().map(row -> java.util.List.<String>of(
                row.productName(),
                row.categoryName(),
                String.valueOf(row.netSoldQuantity()),
                context.support().formatVnd(row.netRevenue()),
                context.support().formatVnd(row.estimatedProfit()),
                String.valueOf(row.onHandQuantity())
            )).toList()
        ));

        VBox topSellingSection = createReportSection(
            "Top Selling Products",
            null,
            topSellingTable,
            exportTopSellingBtn,
            activeFocusTarget == ReportFocusTarget.TOP_SELLING ? "From Dashboard" : null
        );
        bindReportSectionFullWidth(topSellingSection, widthSource);
        anchors.put(ReportFocusTarget.TOP_SELLING, topSellingSection);

        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.dto.report.CategoryStockRow> categoryStockTable = new javafx.scene.control.TableView<>();
        TableViewSupport.prepareNonReorderableTable(categoryStockTable);
        categoryStockTable.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        categoryStockTable.setItems(javafx.collections.FXCollections.observableArrayList(reportData.categoryStocks()));
        categoryStockTable.setPrefHeight(280);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.CategoryStockRow, String> categoryNameCol = new javafx.scene.control.TableColumn<>("Category");
        categoryNameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().categoryName()));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.CategoryStockRow, Number> categorySkuCol = new javafx.scene.control.TableColumn<>("SKUs");
        categorySkuCol.setCellValueFactory(data -> new javafx.beans.property.SimpleLongProperty(data.getValue().skuCount()));
        categorySkuCol.setStyle("-fx-alignment: CENTER;");
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.CategoryStockRow, Number> categoryQtyCol = new javafx.scene.control.TableColumn<>("On Hand Qty");
        categoryQtyCol.setCellValueFactory(data -> new javafx.beans.property.SimpleLongProperty(data.getValue().totalQuantity()));
        categoryQtyCol.setStyle("-fx-alignment: CENTER;");
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.CategoryStockRow, String> categoryRetailCol = new javafx.scene.control.TableColumn<>("Retail Value");
        categoryRetailCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(context.support().formatVnd(data.getValue().retailValue())));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.CategoryStockRow, String> categoryCostCol = new javafx.scene.control.TableColumn<>("Cost Value");
        categoryCostCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(context.support().formatVnd(data.getValue().costValue())));
        categoryStockTable.getColumns().addAll(categoryNameCol, categorySkuCol, categoryQtyCol, categoryRetailCol, categoryCostCol);

        Button exportCategoryStockBtn = createReportExportButton("Export CSV");
        exportCategoryStockBtn.setOnAction(e -> exportCsv(
            stage,
            buildReportCsvFileName("stock-by-category", startDate, endDate),
            java.util.List.of("Category", "SKUs", "On Hand Qty", "Retail Value", "Cost Value"),
            reportData.categoryStocks().stream().map(row -> java.util.List.<String>of(
                row.categoryName(),
                String.valueOf(row.skuCount()),
                String.valueOf(row.totalQuantity()),
                context.support().formatVnd(row.retailValue()),
                context.support().formatVnd(row.costValue())
            )).toList()
        ));

        VBox categoryStockSection = createReportSection(
            "Inventory by Category",
            null,
            categoryStockTable,
            exportCategoryStockBtn,
            activeFocusTarget == ReportFocusTarget.CATEGORY_STOCK ? "From Dashboard" : null
        );
        bindReportSectionFullWidth(categoryStockSection, widthSource);
        anchors.put(ReportFocusTarget.CATEGORY_STOCK, categoryStockSection);

        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.dto.report.AgingStockRow> agingTable = new javafx.scene.control.TableView<>();
        TableViewSupport.prepareNonReorderableTable(agingTable);
        agingTable.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        agingTable.setItems(javafx.collections.FXCollections.observableArrayList(reportData.agingStocks()));
        agingTable.setPrefHeight(320);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.AgingStockRow, String> agingProductCol = new javafx.scene.control.TableColumn<>("Product");
        agingProductCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().productName()));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.AgingStockRow, String> agingCategoryCol = new javafx.scene.control.TableColumn<>("Category");
        agingCategoryCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().categoryName()));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.AgingStockRow, Number> agingQtyCol = new javafx.scene.control.TableColumn<>("On Hand");
        agingQtyCol.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().onHandQuantity()));
        agingQtyCol.setStyle("-fx-alignment: CENTER;");
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.AgingStockRow, String> agingInboundCol = new javafx.scene.control.TableColumn<>("Last Inbound");
        agingInboundCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(context.support().formatDateTime(data.getValue().lastInboundAt())));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.AgingStockRow, String> agingDaysCol = new javafx.scene.control.TableColumn<>("Age");
        agingDaysCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().ageDays() >= 0 ? java.text.MessageFormat.format("{0} days", data.getValue().ageDays()) : "Unknown"
        ));
        agingDaysCol.setStyle("-fx-alignment: CENTER;");
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.AgingStockRow, String> agingBucketCol = new javafx.scene.control.TableColumn<>("Bucket");
        agingBucketCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().agingBucket()));
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.AgingStockRow, String> agingCostCol = new javafx.scene.control.TableColumn<>("Cost Value");
        agingCostCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(context.support().formatVnd(data.getValue().costValue())));
        agingTable.getColumns().addAll(agingProductCol, agingCategoryCol, agingQtyCol, agingInboundCol, agingDaysCol, agingBucketCol, agingCostCol);

        Button exportAgingBtn = createReportExportButton("Export CSV");
        exportAgingBtn.setOnAction(e -> exportCsv(
            stage,
            buildReportCsvFileName("aging-stock", startDate, endDate),
            java.util.List.of("Product", "Category", "On Hand", "Last Inbound", "Age Days", "Bucket", "Cost Value", "Retail Value"),
            reportData.agingStocks().stream().map(row -> java.util.List.<String>of(
                row.productName(),
                row.categoryName(),
                String.valueOf(row.onHandQuantity()),
                context.support().formatDateTime(row.lastInboundAt()),
                row.ageDays() >= 0 ? String.valueOf(row.ageDays()) : "",
                row.agingBucket(),
                context.support().formatVnd(row.costValue()),
                context.support().formatVnd(row.retailValue())
            )).toList()
        ));

        VBox agingSection = createReportSection(
            "Aging Stock",
            null,
            agingTable,
            exportAgingBtn,
            activeFocusTarget == ReportFocusTarget.AGING_STOCK ? "From Dashboard" : null
        );
        anchors.put(ReportFocusTarget.AGING_STOCK, agingSection);

        VBox shiftsSection = createSalesShiftReportSection(
            stage,
            user,
            interactionRoot,
            startDate,
            endDate,
            activeFocusTarget
        );
        bindReportSectionFullWidth(shiftsSection, widthSource);
        anchors.put(ReportFocusTarget.SHIFTS, shiftsSection);

        TableViewSupport.enableDeselectOnOutsideClick(interactionRoot, expenseCategoryTable);
        TableViewSupport.enableDeselectOnOutsideClick(interactionRoot, promotionImpactTable);
        TableViewSupport.enableDeselectOnOutsideClick(interactionRoot, activePromotionTable);
        TableViewSupport.enableDeselectOnOutsideClick(interactionRoot, topSellingTable);
        TableViewSupport.enableDeselectOnOutsideClick(interactionRoot, categoryStockTable);
        TableViewSupport.enableDeselectOnOutsideClick(interactionRoot, agingTable);

        return new ReportSectionsBundle(
            java.util.List.of(
                summarySection,
                actionCenterSection,
                whatChangedSection,
                reorderSection,
                shiftsSection,
                salesChartsRow,
                canceledOrdersSection,
                paymentSection,
                expensesSection,
                promotionSection,
                topSellingSection,
                categoryStockSection,
                agingSection
            ),
            anchors
        );
    }

    private VBox createSalesShiftReportSection(
        Stage stage,
        User user,
        VBox interactionRoot,
        java.time.LocalDate startDate,
        java.time.LocalDate endDate,
        ReportFocusTarget activeFocusTarget
    ) {
        boolean canManageAllShifts = context.authorizationService().canManageAllSalesShifts(user);

        javafx.scene.control.ComboBox<IdLabelOption> userFilter = new javafx.scene.control.ComboBox<>();
        userFilter.setPrefWidth(220);
        userFilter.setPromptText(canManageAllShifts ? "All employees" : "My shifts");
        userFilter.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(IdLabelOption option) {
                return option == null ? "" : option.label();
            }

            @Override
            public IdLabelOption fromString(String string) {
                return null;
            }
        });
        userFilter.setDisable(!canManageAllShifts);
        userFilter.getStyleClass().add("shift-report-combo-box");
        userFilter.setButtonCell(createShiftReportComboCell(option -> option == null ? "" : option.label()));
        userFilter.setCellFactory(list -> createShiftReportComboCell(option -> option == null ? "" : option.label()));

        javafx.scene.control.ComboBox<String> statusFilter = new javafx.scene.control.ComboBox<>(
            javafx.collections.FXCollections.observableArrayList("All statuses", "Open", "Closed")
        );
        statusFilter.getSelectionModel().selectFirst();
        statusFilter.setPrefWidth(150);
        statusFilter.getStyleClass().add("shift-report-combo-box");
        statusFilter.setButtonCell(createShiftReportComboCell(value -> value == null ? "" : value));
        statusFilter.setCellFactory(list -> createShiftReportComboCell(value -> value == null ? "" : value));

        TextField shiftIdFilter = new TextField();
        shiftIdFilter.setPromptText("Shift ID");
        shiftIdFilter.setPrefWidth(110);

        Button applyButton = new Button("Apply");
        applyButton.getStyleClass().addAll("button", "primary-button");
        applyButton.setStyle("-fx-padding: 8 18; -fx-background-radius: 999;");

        Button closeSelectedButton = new Button("Close Selected");
        closeSelectedButton.getStyleClass().addAll("button", "dashboard-report-secondary-button", "no-hover-button");
        closeSelectedButton.setStyle("-fx-padding: 8 18; -fx-background-radius: 999;");
        closeSelectedButton.setVisible(canManageAllShifts);
        closeSelectedButton.setManaged(canManageAllShifts);
        closeSelectedButton.setDisable(true);

        Label stateLabel = new Label("Loading shifts...");
        stateLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: -app-text-muted;");

        HBox filterBar = new HBox(10, userFilter, statusFilter, shiftIdFilter, applyButton, closeSelectedButton, new Region(), stateLabel);
        HBox.setHgrow(filterBar.getChildren().get(5), Priority.ALWAYS);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setMinWidth(0);
        filterBar.setMaxWidth(Double.MAX_VALUE);

        javafx.scene.control.TableView<SalesShiftService.ShiftReportRow> table = new javafx.scene.control.TableView<>();
        TableViewSupport.prepareNonReorderableTable(table);
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(360);
        table.setPlaceholder(new Label("Loading shifts..."));

        table.getColumns().addAll(
            createShiftTextColumn("Shift", row -> "#" + row.shiftId(), 90),
            createShiftTextColumn("Employee", this::formatShiftEmployee, 240),
            createShiftTextColumn("Opened", row -> formatShiftDateTime(row.openedAt()), 170),
            createShiftTextColumn("Closed", row -> formatShiftDateTime(row.closedAt()), 170),
            createShiftTextColumn("Status", row -> formatShiftStatus(row.status()), 110),
            createShiftTextColumn("Orders", row -> String.valueOf(row.orderCount()), 90),
            createShiftTextColumn("Sales", row -> context.support().formatVnd(row.salesRevenue()), 160),
            createShiftTextColumn("Variance", row -> formatSignedVnd(row.cashVarianceAmount()), 150)
        );

        Button exportShiftsCsvButton = createReportExportButton("Export CSV");
        exportShiftsCsvButton.setDisable(true);
        exportShiftsCsvButton.setOnAction(event -> exportCsv(
            stage,
            buildReportCsvFileName("sales-shifts", startDate, endDate),
            shiftExportHeaders(),
            buildShiftExportRows(table.getItems())
        ));
        Button exportShiftsPdfButton = createReportExportButton("Export PDF");
        exportShiftsPdfButton.setDisable(true);
        exportShiftsPdfButton.setOnAction(event -> exportPdf(
            stage,
            "Sales Shift Report",
            buildReportPdfFileName("sales-shifts", startDate, endDate),
            buildShiftExportMetadata(startDate, endDate, userFilter, statusFilter, shiftIdFilter),
            shiftExportHeaders(),
            buildShiftExportRows(table.getItems())
        ));

        ShiftDetailLabels detailLabels = new ShiftDetailLabels(
            new Label(),
            new Label(),
            new Label(),
            new Label(),
            new Label(),
            new Label(),
            new Label(),
            new Label(),
            new Label(),
            new Label(),
            new Label(),
            new Label(),
            new Label(),
            new Label()
        );
        VBox detailPanel = createShiftDetailPanel(detailLabels);
        updateShiftDetailPanel(detailLabels, null);

        Runnable[] loadShiftsRef = new Runnable[1];
        loadShiftsRef[0] = () -> {
            Long selectedUserId = userFilter.getSelectionModel().getSelectedItem() != null
                ? userFilter.getSelectionModel().getSelectedItem().id()
                : null;
            SalesShiftStatus selectedStatus = parseShiftStatusFilter(statusFilter.getValue());
            Long selectedShiftId;
            try {
                selectedShiftId = parseShiftIdFilter(shiftIdFilter.getText());
            } catch (IllegalArgumentException ex) {
                context.toastService().showWarning(ex.getMessage());
                return;
            }

            filterBar.setMouseTransparent(true);
            stateLabel.setText("Loading shifts...");
            table.setPlaceholder(new Label("Loading shifts..."));
            applyButton.setDisable(true);
            closeSelectedButton.setDisable(true);
            exportShiftsCsvButton.setDisable(true);
            exportShiftsPdfButton.setDisable(true);

            javafx.concurrent.Task<ShiftReportLoadResult> task = new javafx.concurrent.Task<>() {
                @Override
                protected ShiftReportLoadResult call() {
                    java.util.List<IdLabelOption> options = context.salesShiftService().getShiftUserOptions(user);
                    java.util.List<SalesShiftService.ShiftReportRow> rows = context.salesShiftService().searchShifts(
                        user,
                        startDate,
                        endDate,
                        selectedUserId,
                        selectedStatus,
                        selectedShiftId
                    );
                    return new ShiftReportLoadResult(options, rows);
                }
            };

            task.setOnSucceeded(event -> {
                filterBar.setMouseTransparent(false);
                applyButton.setDisable(false);
                installShiftUserOptions(userFilter, task.getValue().userOptions(), canManageAllShifts, selectedUserId);
                table.setItems(javafx.collections.FXCollections.observableArrayList(task.getValue().rows()));
                table.setPlaceholder(new Label("No shifts match the current filters"));
                stateLabel.setText(java.text.MessageFormat.format("{0} shifts", task.getValue().rows().size()));
                table.getSelectionModel().clearSelection();
                updateShiftDetailPanel(detailLabels, null);
                closeSelectedButton.setDisable(true);
                exportShiftsCsvButton.setDisable(false);
                exportShiftsPdfButton.setDisable(false);
            });

            task.setOnFailed(event -> {
                filterBar.setMouseTransparent(false);
                applyButton.setDisable(false);
                exportShiftsCsvButton.setDisable(false);
                exportShiftsPdfButton.setDisable(false);
                Throwable ex = task.getException();
                String message = ex != null && ex.getMessage() != null && !ex.getMessage().isBlank()
                    ? ex.getMessage()
                    : "Could not load sales shifts";
                stateLabel.setText("Shift report unavailable");
                table.setPlaceholder(new Label("Could not load shifts"));
                context.toastService().showError("Could not load sales shifts: " + message);
            });

            UiTaskExecutor.execute(task, "sales-shift-report-loader");
        };

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            updateShiftDetailPanel(detailLabels, newSelection);
            closeSelectedButton.setDisable(!canCloseShiftFromReport(canManageAllShifts, newSelection));
        });
        applyButton.setOnAction(event -> loadShiftsRef[0].run());
        closeSelectedButton.setOnAction(event -> {
            SalesShiftService.ShiftReportRow selected = table.getSelectionModel().getSelectedItem();
            if (!canCloseShiftFromReport(canManageAllShifts, selected)) {
                context.toastService().showWarning("Select an open shift to close");
                return;
            }
            java.util.Optional<ShiftCloseInput> input = showManagerCloseShiftDialog(stage, selected);
            if (input.isEmpty()) {
                return;
            }

            filterBar.setMouseTransparent(true);
            closeSelectedButton.setDisable(true);
            stateLabel.setText("Closing shift...");
            javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
                @Override
                protected Void call() {
                    context.salesShiftService().closeShiftAsManager(
                        user,
                        selected.shiftId(),
                        input.get().amount(),
                        input.get().note()
                    );
                    return null;
                }
            };

            task.setOnSucceeded(closeEvent -> {
                context.toastService().showSuccess("Shift closed");
                loadShiftsRef[0].run();
            });
            task.setOnFailed(closeEvent -> {
                filterBar.setMouseTransparent(false);
                applyButton.setDisable(false);
                closeSelectedButton.setDisable(!canCloseShiftFromReport(canManageAllShifts, table.getSelectionModel().getSelectedItem()));
                Throwable ex = task.getException();
                context.showUserFacingError(ex != null ? ex : new RuntimeException("Could not close shift"));
                stateLabel.setText("Close shift failed");
            });

            UiTaskExecutor.execute(task, "sales-shift-report-close");
        });

        VBox content = new VBox(12, filterBar, table, detailPanel);
        content.setFillWidth(true);
        VBox section = createReportSection(
            "Shifts",
            "Open and closed sales shifts by employee",
            content,
            createReportActionGroup(exportShiftsCsvButton, exportShiftsPdfButton),
            activeFocusTarget == ReportFocusTarget.SHIFTS ? "From Dashboard" : null
        );
        TableViewSupport.enableDeselectOnOutsideClick(interactionRoot, table);
        loadShiftsRef[0].run();
        return section;
    }

    private VBox createShiftDetailPanel(ShiftDetailLabels labels) {
        Label detailCaption = new Label("Details");
        detailCaption.setStyle("-fx-font-size: 12px; -fx-font-weight: 900; -fx-text-fill: -app-text-muted;");
        labels.title().setStyle("-fx-font-size: 15px; -fx-font-weight: 900; -fx-text-fill: -app-text-primary;");
        labels.title().setWrapText(true);
        labels.status().setStyle("-fx-font-size: 12px; -fx-font-weight: 900; -fx-padding: 4 10; -fx-background-radius: 999;");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox header = new HBox(10, detailCaption, labels.title(), headerSpacer, labels.status());
        header.setAlignment(Pos.CENTER_LEFT);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(16);
        grid.setVgap(8);
        grid.setMaxWidth(Double.MAX_VALUE);
        grid.add(createShiftDetailMetric("Opened", labels.opened()), 0, 0);
        grid.add(createShiftDetailMetric("Closed", labels.closed()), 1, 0);
        grid.add(createShiftDetailMetric("Activity", labels.orders()), 2, 0);
        grid.add(createShiftDetailMetric("Opening Cash", labels.openingCash()), 0, 1);
        grid.add(createShiftDetailMetric("Sales", labels.sales()), 1, 1);
        grid.add(createShiftDetailMetric("Refunds", labels.refunds()), 2, 1);
        grid.add(createShiftDetailMetric("Expenses", labels.expenses()), 0, 2);
        grid.add(createShiftDetailMetric("Expected Cash", labels.expectedCash()), 1, 2);
        grid.add(createShiftDetailMetric("Actual Cash", labels.actualCash()), 2, 2);
        grid.add(createShiftDetailMetric("Variance", labels.variance()), 0, 3);
        grid.add(createShiftDetailMetric("Closed By", labels.closedBy()), 1, 3);
        grid.add(createShiftDetailMetric("Close Note", labels.closeNote()), 2, 3);
        for (int i = 0; i < 3; i++) {
            javafx.scene.layout.ColumnConstraints constraints = new javafx.scene.layout.ColumnConstraints();
            constraints.setPercentWidth(33.333);
            constraints.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(constraints);
        }

        VBox panel = new VBox(10, header, grid);
        panel.setPadding(new Insets(12));
        panel.setFillWidth(true);
        panel.setStyle("""
            -fx-background-color: derive(-app-surface-muted, 35%);
            -fx-background-radius: 14;
            -fx-border-color: -app-border;
            -fx-border-radius: 14;
            """);
        return panel;
    }

    private VBox createShiftDetailMetric(String labelText, Label valueLabel) {
        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: -app-text-muted;");
        valueLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 900; -fx-text-fill: -app-text-primary;");
        valueLabel.setWrapText(true);
        valueLabel.setMaxWidth(Double.MAX_VALUE);
        VBox metric = new VBox(2, label, valueLabel);
        metric.setMinWidth(0);
        metric.setMaxWidth(Double.MAX_VALUE);
        return metric;
    }

    private void updateShiftDetailPanel(
        ShiftDetailLabels labels,
        SalesShiftService.ShiftReportRow row
    ) {
        if (row == null) {
            labels.title().setText("No shift selected");
            labels.status().setText("Select a row");
            labels.status().setStyle("-fx-font-size: 12px; -fx-font-weight: 900; -fx-padding: 4 10; -fx-background-radius: 999; -fx-background-color: -app-surface; -fx-text-fill: -app-text-muted;");
            labels.opened().setText("-");
            labels.closed().setText("-");
            labels.orders().setText("-");
            labels.openingCash().setText("-");
            labels.sales().setText("-");
            labels.refunds().setText("-");
            labels.expenses().setText("-");
            labels.expectedCash().setText("-");
            labels.actualCash().setText("-");
            labels.variance().setText("-");
            labels.closedBy().setText("-");
            labels.closeNote().setText("-");
            return;
        }

        labels.title().setText(java.text.MessageFormat.format("#{0} · {1}", row.shiftId(), formatShiftEmployee(row)));
        labels.status().setText(formatShiftStatus(row.status()));
        labels.status().setStyle(row.status() == SalesShiftStatus.OPEN
            ? "-fx-font-size: 12px; -fx-font-weight: 900; -fx-padding: 4 10; -fx-background-radius: 999; -fx-background-color: -app-success-soft; -fx-text-fill: -app-success-hover;"
            : "-fx-font-size: 12px; -fx-font-weight: 900; -fx-padding: 4 10; -fx-background-radius: 999; -fx-background-color: -app-surface; -fx-text-fill: -app-text-muted;"
        );
        labels.opened().setText(formatShiftDateTime(row.openedAt()));
        labels.closed().setText(formatShiftDateTime(row.closedAt()));
        labels.orders().setText(java.text.MessageFormat.format(
            "{0} order(s), {1} refund event(s)",
            row.orderCount(),
            row.refundCount()
        ));
        labels.openingCash().setText(context.support().formatVnd(row.openingCashAmount()));
        labels.sales().setText(context.support().formatVnd(row.salesRevenue()));
        labels.refunds().setText(context.support().formatVnd(row.refundAmount()));
        labels.expenses().setText(context.support().formatVnd(row.expenseAmount()));
        labels.expectedCash().setText(context.support().formatVnd(row.expectedCashAmount()));
        labels.actualCash().setText(row.closingCashActual() != null ? context.support().formatVnd(row.closingCashActual()) : "-");
        labels.variance().setText(formatSignedVnd(row.cashVarianceAmount()));
        labels.closedBy().setText(row.closedByName() != null && !row.closedByName().isBlank() ? row.closedByName() : "-");
        labels.closeNote().setText(row.closeNote() != null && !row.closeNote().isBlank() ? row.closeNote() : "-");
    }

    private javafx.scene.control.TableColumn<SalesShiftService.ShiftReportRow, String> createShiftTextColumn(
        String title,
        java.util.function.Function<SalesShiftService.ShiftReportRow, String> valueFactory,
        double prefWidth
    ) {
        javafx.scene.control.TableColumn<SalesShiftService.ShiftReportRow, String> column =
            new javafx.scene.control.TableColumn<>(title);
        column.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(valueFactory.apply(data.getValue())));
        column.setPrefWidth(prefWidth);
        return column;
    }

    private void installShiftUserOptions(
        javafx.scene.control.ComboBox<IdLabelOption> userFilter,
        java.util.List<IdLabelOption> options,
        boolean canManageAllShifts,
        Long selectedUserId
    ) {
        IdLabelOption allOption = new IdLabelOption(null, canManageAllShifts ? "All employees" : "My shifts");
        java.util.List<IdLabelOption> nextOptions = new java.util.ArrayList<>();
        nextOptions.add(allOption);
        if (options != null) {
            nextOptions.addAll(options);
        }
        userFilter.setItems(javafx.collections.FXCollections.observableArrayList(nextOptions));
        IdLabelOption selected = nextOptions.stream()
            .filter(option -> java.util.Objects.equals(option.id(), selectedUserId))
            .findFirst()
            .orElse(allOption);
        userFilter.getSelectionModel().select(selected);
    }

    private static <T> javafx.scene.control.ListCell<T> createShiftReportComboCell(
        java.util.function.Function<T, String> labelFactory
    ) {
        return new javafx.scene.control.ListCell<>() {
            {
                getStyleClass().add("shift-report-popup-cell");
            }

            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : labelFactory.apply(item));
                setGraphic(null);
            }
        };
    }

    private boolean canCloseShiftFromReport(
        boolean canManageAllShifts,
        SalesShiftService.ShiftReportRow row
    ) {
        return canManageAllShifts && row != null && row.status() == SalesShiftStatus.OPEN;
    }

    private SalesShiftStatus parseShiftStatusFilter(String selectedValue) {
        if ("Open".equals(selectedValue)) {
            return SalesShiftStatus.OPEN;
        }
        if ("Closed".equals(selectedValue)) {
            return SalesShiftStatus.CLOSED;
        }
        return null;
    }

    private Long parseShiftIdFilter(String rawValue) {
        String value = rawValue == null ? "" : rawValue.trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            long parsed = Long.parseLong(value);
            if (parsed <= 0) {
                throw new NumberFormatException("non-positive");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Shift ID must be a positive number");
        }
    }

    private String formatShiftEmployee(SalesShiftService.ShiftReportRow row) {
        String name = row.openedByName() == null || row.openedByName().isBlank()
            ? "Unknown"
            : row.openedByName();
        if (row.openedByUsername() == null || row.openedByUsername().isBlank()) {
            return name;
        }
        return name + " @" + row.openedByUsername();
    }

    private String formatShiftDateTime(java.time.LocalDateTime dateTime) {
        return dateTime == null ? "-" : context.support().formatDateTime(dateTime);
    }

    private String formatShiftStatus(SalesShiftStatus status) {
        return status == SalesShiftStatus.OPEN ? "Open" : status == SalesShiftStatus.CLOSED ? "Closed" : "-";
    }

    private String formatSignedVnd(BigDecimal value) {
        BigDecimal normalized = MoneySupport.normalize(value);
        if (normalized.signum() == 0) {
            return context.support().formatVnd(normalized);
        }
        String sign = normalized.signum() > 0 ? "+" : "-";
        return sign + context.support().formatVnd(normalized.abs());
    }

    private java.util.Optional<ShiftCloseInput> showManagerCloseShiftDialog(
        Stage owner,
        SalesShiftService.ShiftReportRow row
    ) {
        javafx.scene.control.Dialog<ShiftCloseInput> dialog = new javafx.scene.control.Dialog<>();
        if (owner != null) {
            dialog.initOwner(owner);
            dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
        }
        dialog.setTitle("Close Shift");
        dialog.setHeaderText(null);

        javafx.scene.control.ButtonType confirmType =
            new javafx.scene.control.ButtonType("Close Shift", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmType, javafx.scene.control.ButtonType.CANCEL);

        Label contextLabel = new Label(java.text.MessageFormat.format(
            "Shift #{0} · {1} · Expected cash {2}",
            row.shiftId(),
            formatShiftEmployee(row),
            context.support().formatVnd(row.expectedCashAmount())
        ));
        contextLabel.setWrapText(true);
        contextLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: -app-text-muted;");

        Label amountCaption = new Label("Actual closing cash");
        amountCaption.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: -app-text-secondary;");
        TextField amountField = new TextField(formatPlainMoney(row.expectedCashAmount()));
        amountField.setPromptText("0");

        Label noteCaption = new Label("Manager note *");
        noteCaption.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: -app-text-secondary;");
        TextField noteField = new TextField();
        noteField.setPromptText("Reason for closing this employee's shift");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: -app-danger;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        VBox content = new VBox(8, contextLabel, amountCaption, amountField, noteCaption, noteField, errorLabel);
        content.setPadding(new Insets(4));
        content.setFillWidth(true);
        dialog.getDialogPane().setContent(content);

        Button confirmButton = (Button) dialog.getDialogPane().lookupButton(confirmType);
        Runnable validate = () -> {
            try {
                BigDecimal amount = parseReportMoneyInput(amountField.getText());
                boolean invalidNote = noteField.getText() == null || noteField.getText().trim().isEmpty();
                confirmButton.setDisable(amount.signum() < 0 || invalidNote);
                errorLabel.setVisible(false);
                errorLabel.setManaged(false);
            } catch (IllegalArgumentException ex) {
                confirmButton.setDisable(true);
                errorLabel.setText(ex.getMessage());
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
            }
        };
        amountField.textProperty().addListener((obs, oldValue, newValue) -> validate.run());
        noteField.textProperty().addListener((obs, oldValue, newValue) -> validate.run());
        validate.run();

        dialog.setOnShown(event -> {
            amountField.requestFocus();
            amountField.selectAll();
            javafx.application.Platform.runLater(amountField::requestFocus);
        });
        dialog.setResultConverter(buttonType -> {
            if (buttonType != confirmType) {
                return null;
            }
            return new ShiftCloseInput(parseReportMoneyInput(amountField.getText()), noteField.getText());
        });
        return dialog.showAndWait();
    }

    private String formatPlainMoney(BigDecimal amount) {
        return MoneySupport.normalize(amount).setScale(0, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private BigDecimal parseReportMoneyInput(String input) {
        String normalized = input == null
            ? ""
            : input.trim().toUpperCase(java.util.Locale.ROOT).replace("VND", "").replace(" ", "");
        if (normalized.isBlank()) {
            return MoneySupport.ZERO;
        }
        boolean hasComma = normalized.contains(",");
        boolean hasDot = normalized.contains(".");
        if (hasComma) {
            normalized = normalized.replace(",", "");
        } else if (hasDot) {
            int dotIndex = normalized.lastIndexOf('.');
            if (normalized.length() - dotIndex - 1 == 3) {
                normalized = normalized.replace(".", "");
            }
        }
        try {
            return MoneySupport.normalize(new BigDecimal(normalized));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Enter a valid cash amount");
        }
    }

    private VBox createDashboardCard(String title, String value, String colorHex) {
        return createDashboardCard(title, value, colorHex, null);
    }

    private VBox createDashboardCard(String title, String value, String colorHex, javafx.scene.Node headerIcon) {
        VBox card = new VBox(8);
        card.getStyleClass().add("dashboard-summary-card");
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: -app-surface; -fx-background-radius: 15; " +
            "-fx-effect: dropshadow(three-pass-box, -app-shadow, 5, 0, 0, 1);");
        card.setMinHeight(100);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dashboard-summary-title");
        titleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-muted;");

        javafx.scene.layout.HBox titleRow = new javafx.scene.layout.HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.getChildren().add(titleLabel);
        if (headerIcon != null) {
            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
            javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            titleRow.getChildren().addAll(spacer, headerIcon);
        }

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("dashboard-summary-value");
        valueLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + colorHex + ";");

        card.getChildren().addAll(titleRow, valueLabel);
        return card;
    }

    private VBox createDashboardMetricCard(String title, String value, String valueColorStyle, String deltaText, String deltaColorStyle) {
        return createDashboardMetricCard(title, value, valueColorStyle, deltaText, deltaColorStyle, null);
    }

    private VBox createDashboardMetricCard(
        String title,
        String value,
        String valueColorStyle,
        String deltaText,
        String deltaColorStyle,
        javafx.scene.Node headerIcon
    ) {
        VBox card = new VBox(10);
        card.getStyleClass().add("dashboard-metric-card");
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMinHeight(118);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dashboard-metric-title");
        titleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-muted;");

        javafx.scene.layout.HBox titleRow = new javafx.scene.layout.HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.getChildren().add(titleLabel);
        if (headerIcon != null) {
            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
            javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            titleRow.getChildren().addAll(spacer, headerIcon);
        }

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("dashboard-metric-value");
        valueLabel.setStyle("-fx-font-size: 30px; -fx-font-weight: 700; -fx-text-fill: " + valueColorStyle + ";");

        Label deltaLabel = new Label(deltaText);
        deltaLabel.getStyleClass().add("dashboard-metric-delta");
        deltaLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: " + deltaColorStyle + ";");
        deltaLabel.setWrapText(true);

        card.getChildren().addAll(titleRow, valueLabel, deltaLabel);
        return card;
    }

    private javafx.scene.Node createRevenuePanelIcon() {
        javafx.scene.shape.Circle outerCircle = new javafx.scene.shape.Circle(12);
        outerCircle.getStyleClass().add("dashboard-card-icon-stroke");
        outerCircle.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.shape.SVGPath amountPath = new javafx.scene.shape.SVGPath();
        amountPath.setContent("M16 8h-6a2 2 0 1 0 0 4h4a2 2 0 1 1 0 4H8");
        amountPath.getStyleClass().add("dashboard-card-icon-stroke");
        amountPath.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.shape.SVGPath dividerPath = new javafx.scene.shape.SVGPath();
        dividerPath.setContent("M12 18V6");
        dividerPath.getStyleClass().add("dashboard-card-icon-stroke");
        dividerPath.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.layout.StackPane iconPane = new javafx.scene.layout.StackPane(outerCircle, amountPath, dividerPath);
        iconPane.getStyleClass().add("dashboard-card-icon-wrap");
        iconPane.setMinSize(28, 28);
        iconPane.setPrefSize(28, 28);
        iconPane.setMaxSize(28, 28);
        iconPane.setScaleX(0.92);
        iconPane.setScaleY(0.92);
        return iconPane;
    }

    private javafx.scene.Node createOrdersPanelIcon() {
        javafx.scene.shape.SVGPath cartPath = new javafx.scene.shape.SVGPath();
        cartPath.setContent(
            "M9 21A1 1 0 1 1 7 21A1 1 0 1 1 9 21Z "
                + "M20 21A1 1 0 1 1 18 21A1 1 0 1 1 20 21Z "
                + "M2.05 2.05h2l2.66 12.42a2 2 0 0 0 2 1.58h9.78a2 2 0 0 0 1.95-1.57l1.65-7.43H5.12"
        );
        cartPath.getStyleClass().add("dashboard-card-icon-stroke-primary");
        cartPath.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.layout.StackPane iconPane = new javafx.scene.layout.StackPane(cartPath);
        iconPane.getStyleClass().add("dashboard-card-icon-wrap");
        iconPane.setMinSize(28, 28);
        iconPane.setPrefSize(28, 28);
        iconPane.setMaxSize(28, 28);
        iconPane.setScaleX(0.92);
        iconPane.setScaleY(0.92);
        return iconPane;
    }

    private javafx.scene.Node createLowStockPanelIcon() {
        javafx.scene.shape.SVGPath packagePath = new javafx.scene.shape.SVGPath();
        packagePath.setContent(
            "M16 16h6 "
                + "M21 10V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l2-1.14 "
                + "M7.5 4.27 16.5 9.42 "
                + "M3.29 7 12 12 20.71 7 "
                + "M12 22V12"
        );
        packagePath.getStyleClass().add("dashboard-card-icon-stroke-danger");
        packagePath.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.layout.StackPane iconPane = new javafx.scene.layout.StackPane(packagePath);
        iconPane.getStyleClass().add("dashboard-card-icon-wrap");
        iconPane.setMinSize(28, 28);
        iconPane.setPrefSize(28, 28);
        iconPane.setMaxSize(28, 28);
        iconPane.setScaleX(0.92);
        iconPane.setScaleY(0.92);
        return iconPane;
    }

    private javafx.scene.Node createEstimatedProfitPanelIcon(boolean positive) {
        javafx.scene.shape.SVGPath trendPath = new javafx.scene.shape.SVGPath();
        trendPath.setContent(
            "M22 7 13.5 15.5 8.5 10.5 2 17 "
                + "M16 7H22V13"
        );
        trendPath.getStyleClass().add(positive ? "dashboard-card-icon-stroke-primary" : "dashboard-card-icon-stroke-danger");
        trendPath.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.layout.StackPane iconPane = new javafx.scene.layout.StackPane(trendPath);
        iconPane.getStyleClass().add("dashboard-card-icon-wrap");
        iconPane.setMinSize(28, 28);
        iconPane.setPrefSize(28, 28);
        iconPane.setMaxSize(28, 28);
        iconPane.setScaleX(0.92);
        iconPane.setScaleY(0.92);
        return iconPane;
    }

    private javafx.scene.Node createExpensesPanelIcon() {
        java.util.function.Function<String, javafx.scene.shape.SVGPath> pathFactory = content -> {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(content);
            path.getStyleClass().add("dashboard-card-icon-stroke-accent");
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            return path;
        };

        javafx.scene.layout.Pane receiptIcon = new javafx.scene.layout.Pane(
            pathFactory.apply("M6 3h9l4 4v14a1 1 0 0 1-1.4.91L15 20l-2 2-2-2-2 2-2-2-2 2A1 1 0 0 1 4 21V5a2 2 0 0 1 2-2"),
            pathFactory.apply("M9 9h5"),
            pathFactory.apply("M9 13h6"),
            pathFactory.apply("M9 17h4")
        );
        receiptIcon.setMinSize(24, 24);
        receiptIcon.setPrefSize(24, 24);
        receiptIcon.setMaxSize(24, 24);
        receiptIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconPane = new javafx.scene.layout.StackPane(receiptIcon);
        iconPane.getStyleClass().add("dashboard-card-icon-wrap");
        iconPane.setMinSize(28, 28);
        iconPane.setPrefSize(28, 28);
        iconPane.setMaxSize(28, 28);
        iconPane.setScaleX(0.92);
        iconPane.setScaleY(0.92);
        return iconPane;
    }

    private javafx.scene.Node createNetUnitsPanelIcon() {
        java.util.function.Function<String, javafx.scene.shape.SVGPath> pathFactory = content -> {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(content);
            path.getStyleClass().add("dashboard-card-icon-stroke-accent");
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            return path;
        };

        javafx.scene.layout.Pane bagIcon = new javafx.scene.layout.Pane(
            pathFactory.apply("M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4Z"),
            pathFactory.apply("M3 6h18"),
            pathFactory.apply("M16 10a4 4 0 0 1-8 0")
        );
        bagIcon.setMinSize(24, 24);
        bagIcon.setPrefSize(24, 24);
        bagIcon.setMaxSize(24, 24);
        bagIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconPane = new javafx.scene.layout.StackPane(bagIcon);
        iconPane.getStyleClass().add("dashboard-card-icon-wrap");
        iconPane.setMinSize(28, 28);
        iconPane.setPrefSize(28, 28);
        iconPane.setMaxSize(28, 28);
        iconPane.setScaleX(0.92);
        iconPane.setScaleY(0.92);
        return iconPane;
    }

    private javafx.scene.Node createCancelSignalPanelIcon() {
        javafx.scene.shape.SVGPath firstStroke = new javafx.scene.shape.SVGPath();
        firstStroke.setContent("M7 7 17 17");
        firstStroke.getStyleClass().add("dashboard-card-icon-stroke-danger");
        firstStroke.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.shape.SVGPath secondStroke = new javafx.scene.shape.SVGPath();
        secondStroke.setContent("M17 7 7 17");
        secondStroke.getStyleClass().add("dashboard-card-icon-stroke-danger");
        secondStroke.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.Group iconGroup = new javafx.scene.Group(firstStroke, secondStroke);
        javafx.scene.layout.StackPane iconPane = new javafx.scene.layout.StackPane(iconGroup);
        iconPane.getStyleClass().add("dashboard-card-icon-wrap");
        iconPane.setMinSize(28, 28);
        iconPane.setPrefSize(28, 28);
        iconPane.setMaxSize(28, 28);
        iconPane.setScaleX(0.92);
        iconPane.setScaleY(0.92);
        return iconPane;
    }

    private javafx.scene.Node createWhatChangedInsightIcon(
        com.pbl3.project.pbl3_project.dto.report.WhatChangedType type,
        com.pbl3.project.pbl3_project.dto.report.InsightSeverity severity
    ) {
        if (type == null) {
            return createEstimatedProfitPanelIcon(severity != com.pbl3.project.pbl3_project.dto.report.InsightSeverity.CRITICAL);
        }
        return switch (type) {
            case REVENUE_CHANGE -> createRevenuePanelIcon();
            case ORDER_COUNT_CHANGE -> createOrdersPanelIcon();
            case AVERAGE_ORDER_VALUE_CHANGE -> createEstimatedProfitPanelIcon(true);
            case CANCEL_RATE_CHANGE -> createCancelSignalPanelIcon();
            case TOP_DRIVER_PRODUCT -> createNetUnitsPanelIcon();
        };
    }

    private javafx.scene.Node createActionCenterInsightIcon(
        com.pbl3.project.pbl3_project.dto.report.ActionCenterType type,
        com.pbl3.project.pbl3_project.dto.report.InsightSeverity severity
    ) {
        if (type == null) {
            return createEstimatedProfitPanelIcon(severity != com.pbl3.project.pbl3_project.dto.report.InsightSeverity.CRITICAL);
        }
        return switch (type) {
            case REORDER_NOW, LOW_COVERAGE, AGED_STOCK -> createLowStockPanelIcon();
            case REVENUE_DROP -> createEstimatedProfitPanelIcon(false);
            case CANCEL_SPIKE -> createCancelSignalPanelIcon();
        };
    }

    private javafx.scene.layout.GridPane createResponsiveDashboardKpiRow(
        VBox firstCard,
        VBox secondCard,
        VBox thirdCard,
        javafx.beans.value.ObservableNumberValue widthSource,
        double threeColumnBreakpoint
    ) {
        javafx.scene.layout.GridPane row = new javafx.scene.layout.GridPane();
        row.setHgap(20);
        row.setVgap(20);
        row.setMaxWidth(Double.MAX_VALUE);
        row.prefWidthProperty().bind(widthSource);

        java.util.List<VBox> cards = java.util.List.of(firstCard, secondCard, thirdCard);
        cards.forEach(card -> {
            card.setMinWidth(0);
            card.setMaxWidth(Double.MAX_VALUE);
            javafx.scene.layout.GridPane.setHgrow(card, javafx.scene.layout.Priority.ALWAYS);
            javafx.scene.layout.GridPane.setFillWidth(card, true);
        });

        final boolean[] threeColumnMode = {false};
        Runnable syncLayout = () -> {
            boolean shouldUseThreeColumns = widthSource.doubleValue() >= threeColumnBreakpoint;
            if (!row.getChildren().isEmpty() && threeColumnMode[0] == shouldUseThreeColumns) {
                return;
            }
            threeColumnMode[0] = shouldUseThreeColumns;
            row.getChildren().clear();
            row.getColumnConstraints().clear();
            row.getRowConstraints().clear();

            if (shouldUseThreeColumns) {
                for (int i = 0; i < 3; i++) {
                    javafx.scene.layout.ColumnConstraints column = new javafx.scene.layout.ColumnConstraints();
                    column.setPercentWidth(100.0 / 3.0);
                    column.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                    column.setFillWidth(true);
                    row.getColumnConstraints().add(column);
                }
                row.add(firstCard, 0, 0);
                row.add(secondCard, 1, 0);
                row.add(thirdCard, 2, 0);
            } else {
                javafx.scene.layout.ColumnConstraints singleColumn = new javafx.scene.layout.ColumnConstraints();
                singleColumn.setPercentWidth(100);
                singleColumn.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                singleColumn.setFillWidth(true);
                row.getColumnConstraints().add(singleColumn);
                row.add(firstCard, 0, 0);
                row.add(secondCard, 0, 1);
                row.add(thirdCard, 0, 2);
            }
        };

        syncLayout.run();
        widthSource.addListener((obs, oldValue, newValue) -> syncLayout.run());
        return row;
    }

    private javafx.scene.layout.GridPane createResponsiveDashboardPairRow(
        VBox firstCard,
        VBox secondCard,
        javafx.beans.value.ObservableNumberValue widthSource,
        double twoColumnBreakpoint
    ) {
        javafx.scene.layout.GridPane row = new javafx.scene.layout.GridPane();
        row.setHgap(20);
        row.setVgap(20);
        row.setMaxWidth(Double.MAX_VALUE);
        row.prefWidthProperty().bind(widthSource);

        java.util.List<VBox> cards = java.util.List.of(firstCard, secondCard);
        cards.forEach(card -> {
            card.setMinWidth(0);
            card.setMaxWidth(Double.MAX_VALUE);
            javafx.scene.layout.GridPane.setHgrow(card, javafx.scene.layout.Priority.ALWAYS);
            javafx.scene.layout.GridPane.setFillWidth(card, true);
        });

        final boolean[] twoColumnMode = {false};
        Runnable syncLayout = () -> {
            boolean shouldUseTwoColumns = widthSource.doubleValue() >= twoColumnBreakpoint;
            if (!row.getChildren().isEmpty() && twoColumnMode[0] == shouldUseTwoColumns) {
                return;
            }
            twoColumnMode[0] = shouldUseTwoColumns;
            row.getChildren().clear();
            row.getColumnConstraints().clear();
            row.getRowConstraints().clear();

            if (shouldUseTwoColumns) {
                for (int i = 0; i < 2; i++) {
                    javafx.scene.layout.ColumnConstraints column = new javafx.scene.layout.ColumnConstraints();
                    column.setPercentWidth(50);
                    column.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                    column.setFillWidth(true);
                    row.getColumnConstraints().add(column);
                }
                row.add(firstCard, 0, 0);
                row.add(secondCard, 1, 0);
            } else {
                javafx.scene.layout.ColumnConstraints singleColumn = new javafx.scene.layout.ColumnConstraints();
                singleColumn.setPercentWidth(100);
                singleColumn.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                singleColumn.setFillWidth(true);
                row.getColumnConstraints().add(singleColumn);
                row.add(firstCard, 0, 0);
                row.add(secondCard, 0, 1);
            }
        };

        syncLayout.run();
        widthSource.addListener((obs, oldValue, newValue) -> syncLayout.run());
        return row;
    }

    private javafx.scene.layout.GridPane createResponsiveDashboardQuadRow(
        VBox firstCard,
        VBox secondCard,
        VBox thirdCard,
        VBox fourthCard,
        javafx.beans.value.ObservableNumberValue widthSource,
        double fourColumnBreakpoint,
        double twoColumnBreakpoint
    ) {
        javafx.scene.layout.GridPane row = new javafx.scene.layout.GridPane();
        row.setHgap(20);
        row.setVgap(20);
        row.setMaxWidth(Double.MAX_VALUE);
        row.prefWidthProperty().bind(widthSource);

        java.util.List<VBox> cards = java.util.List.of(firstCard, secondCard, thirdCard, fourthCard);
        cards.forEach(card -> {
            card.setMinWidth(0);
            card.setMaxWidth(Double.MAX_VALUE);
            javafx.scene.layout.GridPane.setHgrow(card, javafx.scene.layout.Priority.ALWAYS);
            javafx.scene.layout.GridPane.setFillWidth(card, true);
        });

        final int[] modeRef = {-1};
        Runnable syncLayout = () -> {
            int mode;
            if (widthSource.doubleValue() >= fourColumnBreakpoint) {
                mode = 4;
            } else if (widthSource.doubleValue() >= twoColumnBreakpoint) {
                mode = 2;
            } else {
                mode = 1;
            }
            if (!row.getChildren().isEmpty() && modeRef[0] == mode) {
                return;
            }
            modeRef[0] = mode;
            row.getChildren().clear();
            row.getColumnConstraints().clear();
            row.getRowConstraints().clear();

            if (mode == 4) {
                for (int i = 0; i < 4; i++) {
                    javafx.scene.layout.ColumnConstraints column = new javafx.scene.layout.ColumnConstraints();
                    column.setPercentWidth(25);
                    column.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                    column.setFillWidth(true);
                    row.getColumnConstraints().add(column);
                }
                row.add(firstCard, 0, 0);
                row.add(secondCard, 1, 0);
                row.add(thirdCard, 2, 0);
                row.add(fourthCard, 3, 0);
                return;
            }

            if (mode == 2) {
                for (int i = 0; i < 2; i++) {
                    javafx.scene.layout.ColumnConstraints column = new javafx.scene.layout.ColumnConstraints();
                    column.setPercentWidth(50);
                    column.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                    column.setFillWidth(true);
                    row.getColumnConstraints().add(column);
                }
                row.add(firstCard, 0, 0);
                row.add(secondCard, 1, 0);
                row.add(thirdCard, 0, 1);
                row.add(fourthCard, 1, 1);
                return;
            }

            javafx.scene.layout.ColumnConstraints singleColumn = new javafx.scene.layout.ColumnConstraints();
            singleColumn.setPercentWidth(100);
            singleColumn.setHgrow(javafx.scene.layout.Priority.ALWAYS);
            singleColumn.setFillWidth(true);
            row.getColumnConstraints().add(singleColumn);
            row.add(firstCard, 0, 0);
            row.add(secondCard, 0, 1);
            row.add(thirdCard, 0, 2);
            row.add(fourthCard, 0, 3);
        };

        syncLayout.run();
        widthSource.addListener((obs, oldValue, newValue) -> syncLayout.run());
        return row;
    }

    private java.util.List<javafx.scene.Node> assembleDashboardNodes(
        User user,
        javafx.scene.Node headerRow,
        java.util.Map<com.pbl3.project.pbl3_project.entity.DashboardSectionKey, javafx.scene.Node> availableSections,
        javafx.beans.value.ObservableNumberValue widthSource
    ) {
        java.util.List<javafx.scene.Node> nodes = new java.util.ArrayList<>();
        if (headerRow != null) {
            nodes.add(headerRow);
        }

        java.util.List<com.pbl3.project.pbl3_project.entity.DashboardSectionKey> orderedSections =
            context.userUiPreferencesService().resolveDashboardSectionOrder(user);
        java.util.Set<com.pbl3.project.pbl3_project.entity.DashboardSectionKey> hiddenSections =
            context.userUiPreferencesService().resolveHiddenDashboardSections(user);
        java.util.List<VBox> gridBuffer = new java.util.ArrayList<>();

        for (com.pbl3.project.pbl3_project.entity.DashboardSectionKey sectionKey : orderedSections) {
            javafx.scene.Node section = availableSections.get(sectionKey);
            if (section == null || hiddenSections.contains(sectionKey)) {
                continue;
            }
            if (sectionKey.isGridEligible() && section instanceof VBox gridSection) {
                gridBuffer.add(gridSection);
                continue;
            }
            flushDashboardGridBuffer(nodes, gridBuffer, widthSource);
            nodes.add(section);
        }

        flushDashboardGridBuffer(nodes, gridBuffer, widthSource);
        return nodes;
    }

    private void flushDashboardGridBuffer(
        java.util.List<javafx.scene.Node> nodes,
        java.util.List<VBox> gridBuffer,
        javafx.beans.value.ObservableNumberValue widthSource
    ) {
        if (gridBuffer.isEmpty()) {
            return;
        }
        if (gridBuffer.size() == 1) {
            nodes.add(gridBuffer.get(0));
            gridBuffer.clear();
            return;
        }
        javafx.scene.layout.GridPane grid = createResponsiveDashboardSectionGrid(gridBuffer, widthSource, 1120.0);
        enableScrollPerfCache(grid);
        nodes.add(grid);
        gridBuffer.clear();
    }

    private javafx.scene.layout.GridPane createResponsiveDashboardSectionGrid(
        java.util.List<VBox> sections,
        javafx.beans.value.ObservableNumberValue widthSource,
        double twoColumnBreakpoint
    ) {
        java.util.List<VBox> layoutSections = java.util.List.copyOf(sections);
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setMaxWidth(Double.MAX_VALUE);
        grid.prefWidthProperty().bind(widthSource);

        layoutSections.forEach(section -> {
            section.setMinWidth(0);
            section.setMaxWidth(Double.MAX_VALUE);
            javafx.scene.layout.GridPane.setHgrow(section, javafx.scene.layout.Priority.ALWAYS);
            javafx.scene.layout.GridPane.setFillWidth(section, true);
            javafx.scene.layout.GridPane.setFillHeight(section, true);
        });

        final boolean[] twoColumnMode = {false};
        Runnable syncLayout = () -> {
            boolean shouldUseTwoColumns = widthSource.doubleValue() >= twoColumnBreakpoint;
            if (!grid.getChildren().isEmpty() && twoColumnMode[0] == shouldUseTwoColumns) {
                return;
            }
            twoColumnMode[0] = shouldUseTwoColumns;
            grid.getChildren().clear();
            grid.getColumnConstraints().clear();

            if (shouldUseTwoColumns) {
                for (int i = 0; i < 2; i++) {
                    javafx.scene.layout.ColumnConstraints column = new javafx.scene.layout.ColumnConstraints();
                    column.setPercentWidth(50);
                    column.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                    column.setFillWidth(true);
                    grid.getColumnConstraints().add(column);
                }
                for (int index = 0; index < layoutSections.size(); index++) {
                    grid.add(layoutSections.get(index), index % 2, index / 2);
                }
                return;
            }

            javafx.scene.layout.ColumnConstraints singleColumn = new javafx.scene.layout.ColumnConstraints();
            singleColumn.setPercentWidth(100);
            singleColumn.setHgrow(javafx.scene.layout.Priority.ALWAYS);
            singleColumn.setFillWidth(true);
            grid.getColumnConstraints().add(singleColumn);
            for (int index = 0; index < layoutSections.size(); index++) {
                grid.add(layoutSections.get(index), 0, index);
            }
        };

        syncLayout.run();
        widthSource.addListener((obs, oldValue, newValue) -> syncLayout.run());
        return grid;
    }

    private javafx.scene.layout.GridPane createResponsiveDashboardChartGrid(
        VBox firstSection,
        VBox secondSection,
        VBox thirdSection,
        VBox fourthSection,
        javafx.beans.value.ObservableNumberValue widthSource,
        double twoColumnBreakpoint
    ) {
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setMaxWidth(Double.MAX_VALUE);
        grid.prefWidthProperty().bind(widthSource);

        java.util.List<VBox> sections = java.util.List.of(firstSection, secondSection, thirdSection, fourthSection);
        sections.forEach(section -> {
            section.setMinWidth(0);
            section.setMaxWidth(Double.MAX_VALUE);
            javafx.scene.layout.GridPane.setHgrow(section, javafx.scene.layout.Priority.ALWAYS);
            javafx.scene.layout.GridPane.setFillWidth(section, true);
            javafx.scene.layout.GridPane.setFillHeight(section, true);
        });

        final boolean[] twoColumnMode = {false};
        Runnable syncLayout = () -> {
            boolean shouldUseTwoColumns = widthSource.doubleValue() >= twoColumnBreakpoint;
            if (!grid.getChildren().isEmpty() && twoColumnMode[0] == shouldUseTwoColumns) {
                return;
            }
            twoColumnMode[0] = shouldUseTwoColumns;
            grid.getChildren().clear();
            grid.getColumnConstraints().clear();
            grid.getRowConstraints().clear();

            if (shouldUseTwoColumns) {
                for (int i = 0; i < 2; i++) {
                    javafx.scene.layout.ColumnConstraints column = new javafx.scene.layout.ColumnConstraints();
                    column.setPercentWidth(50);
                    column.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                    column.setFillWidth(true);
                    grid.getColumnConstraints().add(column);
                }
                grid.add(firstSection, 0, 0);
                grid.add(secondSection, 1, 0);
                grid.add(thirdSection, 0, 1);
                grid.add(fourthSection, 1, 1);
            } else {
                javafx.scene.layout.ColumnConstraints singleColumn = new javafx.scene.layout.ColumnConstraints();
                singleColumn.setPercentWidth(100);
                singleColumn.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                singleColumn.setFillWidth(true);
                grid.getColumnConstraints().add(singleColumn);
                grid.add(firstSection, 0, 0);
                grid.add(secondSection, 0, 1);
                grid.add(thirdSection, 0, 2);
                grid.add(fourthSection, 0, 3);
            }
        };

        syncLayout.run();
        widthSource.addListener((obs, oldValue, newValue) -> syncLayout.run());
        return grid;
    }

    private void bindDashboardKpiCardWidth(VBox card, javafx.beans.value.ObservableNumberValue widthSource) {
        card.setMinWidth(0);
        card.setMaxWidth(Double.MAX_VALUE);
        card.prefWidthProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(() -> {
            double width = widthSource.doubleValue();
            if (width <= 0) {
                return 280.0;
            }
            if (width >= 1040) {
                return Math.max(240.0, (width - 60.0) / 3.0);
            }
            if (width >= 720) {
                return Math.max(240.0, (width - 40.0) / 2.0);
            }
            return Math.max(260.0, width - 40.0);
        }, widthSource));
    }

    private void bindDashboardChartSectionWidth(VBox section, javafx.beans.value.ObservableNumberValue widthSource) {
        section.setMinWidth(0);
        section.setMaxWidth(Double.MAX_VALUE);
        section.prefWidthProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(() -> {
            double width = widthSource.doubleValue();
            if (width <= 0) {
                return 420.0;
            }
            if (width >= 1120) {
                return Math.max(380.0, (width - 60.0) / 2.0);
            }
            return Math.max(320.0, width - 40.0);
        }, widthSource));
    }

    private void bindReportSectionWidth(VBox section, javafx.beans.value.ObservableNumberValue widthSource) {
        section.setMinWidth(0);
        section.setMaxWidth(Double.MAX_VALUE);
        section.prefWidthProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(() -> {
            double width = widthSource.doubleValue();
            if (width <= 0) {
                return 420.0;
            }
            if (width >= 980) {
                return Math.max(360.0, (width - 60.0) / 2.0);
            }
            return Math.max(420.0, width - 40.0);
        }, widthSource));
    }

    private void bindReportSectionFullWidth(VBox section, javafx.beans.value.ObservableNumberValue widthSource) {
        section.setMinWidth(0);
        section.setMaxWidth(Double.MAX_VALUE);
        section.prefWidthProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(() -> {
            double width = widthSource.doubleValue();
            if (width <= 0) {
                return 420.0;
            }
            return Math.max(420.0, width - 40.0);
        }, widthSource));
    }

    private javafx.scene.layout.GridPane createResponsiveReportPairRow(
        VBox firstSection,
        VBox secondSection,
        javafx.beans.value.ObservableNumberValue widthSource,
        double twoColumnBreakpoint
    ) {
        javafx.scene.layout.GridPane row = new javafx.scene.layout.GridPane();
        row.setHgap(20);
        row.setVgap(20);
        row.setMaxWidth(Double.MAX_VALUE);

        javafx.scene.layout.GridPane.setHgrow(firstSection, javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.layout.GridPane.setHgrow(secondSection, javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.layout.GridPane.setFillWidth(firstSection, true);
        javafx.scene.layout.GridPane.setFillWidth(secondSection, true);
        javafx.scene.layout.GridPane.setFillHeight(firstSection, true);
        javafx.scene.layout.GridPane.setFillHeight(secondSection, true);
        firstSection.setMaxHeight(Double.MAX_VALUE);
        secondSection.setMaxHeight(Double.MAX_VALUE);

        final boolean[] twoColumnMode = {false};
        Runnable syncLayout = () -> {
            boolean shouldUseTwoColumns = widthSource.doubleValue() >= twoColumnBreakpoint;
            if (!row.getChildren().isEmpty() && twoColumnMode[0] == shouldUseTwoColumns) {
                return;
            }
            twoColumnMode[0] = shouldUseTwoColumns;
            row.getChildren().clear();
            row.getColumnConstraints().clear();
            row.getRowConstraints().clear();

            if (shouldUseTwoColumns) {
                javafx.scene.layout.ColumnConstraints leftColumn = new javafx.scene.layout.ColumnConstraints();
                leftColumn.setPercentWidth(50);
                leftColumn.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                leftColumn.setFillWidth(true);

                javafx.scene.layout.ColumnConstraints rightColumn = new javafx.scene.layout.ColumnConstraints();
                rightColumn.setPercentWidth(50);
                rightColumn.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                rightColumn.setFillWidth(true);

                row.getColumnConstraints().addAll(leftColumn, rightColumn);
                row.add(firstSection, 0, 0);
                row.add(secondSection, 1, 0);
            } else {
                javafx.scene.layout.ColumnConstraints singleColumn = new javafx.scene.layout.ColumnConstraints();
                singleColumn.setPercentWidth(100);
                singleColumn.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                singleColumn.setFillWidth(true);

                row.getColumnConstraints().add(singleColumn);
                row.add(firstSection, 0, 0);
                row.add(secondSection, 0, 1);
            }
        };

        syncLayout.run();
        widthSource.addListener((obs, oldValue, newValue) -> syncLayout.run());
        return row;
    }

    private javafx.scene.layout.GridPane createResponsiveReportSummaryRow(
        VBox firstCard,
        VBox secondCard,
        VBox thirdCard,
        VBox fourthCard,
        javafx.beans.value.ObservableNumberValue widthSource,
        double fourColumnBreakpoint,
        double twoColumnBreakpoint
    ) {
        javafx.scene.layout.GridPane row = new javafx.scene.layout.GridPane();
        row.setHgap(20);
        row.setVgap(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);

        java.util.List<VBox> cards = java.util.List.of(firstCard, secondCard, thirdCard, fourthCard);
        cards.forEach(card -> {
            card.setMinWidth(0);
            card.setMaxWidth(Double.MAX_VALUE);
            javafx.scene.layout.GridPane.setHgrow(card, javafx.scene.layout.Priority.ALWAYS);
            javafx.scene.layout.GridPane.setFillWidth(card, true);
        });

        final int[] layoutMode = {-1};
        Runnable syncLayout = () -> {
            double width = widthSource.doubleValue();
            int nextMode = width >= fourColumnBreakpoint ? 4 : width >= twoColumnBreakpoint ? 2 : 1;
            if (!row.getChildren().isEmpty() && layoutMode[0] == nextMode) {
                return;
            }
            layoutMode[0] = nextMode;
            row.getChildren().clear();
            row.getColumnConstraints().clear();
            row.getRowConstraints().clear();

            if (nextMode == 4) {
                for (int i = 0; i < 4; i++) {
                    javafx.scene.layout.ColumnConstraints column = new javafx.scene.layout.ColumnConstraints();
                    column.setPercentWidth(25);
                    column.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                    column.setFillWidth(true);
                    row.getColumnConstraints().add(column);
                }
                row.add(firstCard, 0, 0);
                row.add(secondCard, 1, 0);
                row.add(thirdCard, 2, 0);
                row.add(fourthCard, 3, 0);
            } else if (nextMode == 2) {
                for (int i = 0; i < 2; i++) {
                    javafx.scene.layout.ColumnConstraints column = new javafx.scene.layout.ColumnConstraints();
                    column.setPercentWidth(50);
                    column.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                    column.setFillWidth(true);
                    row.getColumnConstraints().add(column);
                }
                row.add(firstCard, 0, 0);
                row.add(secondCard, 1, 0);
                row.add(thirdCard, 0, 1);
                row.add(fourthCard, 1, 1);
            } else {
                javafx.scene.layout.ColumnConstraints singleColumn = new javafx.scene.layout.ColumnConstraints();
                singleColumn.setPercentWidth(100);
                singleColumn.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                singleColumn.setFillWidth(true);
                row.getColumnConstraints().add(singleColumn);
                row.add(firstCard, 0, 0);
                row.add(secondCard, 0, 1);
                row.add(thirdCard, 0, 2);
                row.add(fourthCard, 0, 3);
            }
        };

        syncLayout.run();
        widthSource.addListener((obs, oldValue, newValue) -> syncLayout.run());
        return row;
    }

    private javafx.scene.chart.LineChart<String, Number> createDashboardLineChart(String xLabel, String yLabel, boolean showLegend) {
        javafx.scene.chart.CategoryAxis xAxis = new javafx.scene.chart.CategoryAxis();
        xAxis.setLabel(xLabel);
        xAxis.setAnimated(false);
        xAxis.setGapStartAndEnd(true);
        xAxis.setStartMargin(18);
        xAxis.setEndMargin(18);
        xAxis.setTickLabelFill(TEXT_MUTED_COLOR);
        xAxis.setTickLabelFont(javafx.scene.text.Font.font("Be Vietnam Pro", javafx.scene.text.FontWeight.MEDIUM, 11));

        javafx.scene.chart.NumberAxis yAxis = new javafx.scene.chart.NumberAxis();
        yAxis.setLabel(yLabel);
        yAxis.setAnimated(false);
        yAxis.setForceZeroInRange(true);
        yAxis.setMinorTickVisible(false);
        yAxis.setTickLabelFill(TEXT_MUTED_COLOR);
        yAxis.setTickLabelFont(javafx.scene.text.Font.font("Be Vietnam Pro", javafx.scene.text.FontWeight.MEDIUM, 11));

        javafx.scene.chart.LineChart<String, Number> chart = new javafx.scene.chart.LineChart<>(xAxis, yAxis);
        chart.getStyleClass().addAll("dashboard-bar-chart", "dashboard-line-chart");
        chart.setTitle(null);
        chart.setLegendVisible(showLegend);
        chart.setCreateSymbols(true);
        chart.setAnimated(false);
        chart.setAlternativeColumnFillVisible(false);
        chart.setAlternativeRowFillVisible(false);
        chart.setHorizontalGridLinesVisible(true);
        chart.setVerticalGridLinesVisible(false);
        chart.setPrefHeight(280);
        chart.setMinHeight(280);
        chart.setMaxHeight(280);
        return chart;
    }

    private javafx.scene.chart.LineChart<String, Number> createReportSeriesLineChart(String xLabel, String yLabel) {
        javafx.scene.chart.CategoryAxis xAxis = new javafx.scene.chart.CategoryAxis();
        xAxis.setLabel(xLabel);
        xAxis.setAnimated(false);
        xAxis.setGapStartAndEnd(true);
        xAxis.setStartMargin(18);
        xAxis.setEndMargin(18);
        xAxis.setTickLabelFill(TEXT_MUTED_COLOR);
        xAxis.setTickLabelFont(javafx.scene.text.Font.font("Be Vietnam Pro", javafx.scene.text.FontWeight.MEDIUM, 11));

        javafx.scene.chart.NumberAxis yAxis = new javafx.scene.chart.NumberAxis();
        yAxis.setLabel(yLabel);
        yAxis.setAnimated(false);
        yAxis.setForceZeroInRange(true);
        yAxis.setMinorTickVisible(false);
        yAxis.setTickLabelFill(TEXT_MUTED_COLOR);
        yAxis.setTickLabelFont(javafx.scene.text.Font.font("Be Vietnam Pro", javafx.scene.text.FontWeight.MEDIUM, 11));

        javafx.scene.chart.LineChart<String, Number> chart = new javafx.scene.chart.LineChart<>(xAxis, yAxis);
        chart.getStyleClass().addAll("dashboard-bar-chart", "dashboard-line-chart");
        chart.setTitle(null);
        chart.setLegendVisible(false);
        chart.setCreateSymbols(true);
        chart.setAnimated(false);
        chart.setAlternativeColumnFillVisible(false);
        chart.setAlternativeRowFillVisible(false);
        chart.setHorizontalGridLinesVisible(true);
        chart.setVerticalGridLinesVisible(false);
        chart.setPrefHeight(280);
        chart.setMinHeight(280);
        chart.setMaxHeight(280);
        return chart;
    }

    private javafx.scene.chart.BarChart<Number, String> createDashboardHorizontalBarChart(String xLabel, String yLabel) {
        javafx.scene.chart.NumberAxis xAxis = new javafx.scene.chart.NumberAxis();
        xAxis.setLabel(xLabel);
        xAxis.setAnimated(false);
        xAxis.setForceZeroInRange(true);
        xAxis.setMinorTickVisible(false);
        xAxis.setTickLabelFill(TEXT_MUTED_COLOR);
        xAxis.setTickLabelFont(javafx.scene.text.Font.font("Be Vietnam Pro", javafx.scene.text.FontWeight.MEDIUM, 11));

        javafx.scene.chart.CategoryAxis yAxis = new javafx.scene.chart.CategoryAxis();
        yAxis.setLabel(yLabel);
        yAxis.setAnimated(false);
        yAxis.setGapStartAndEnd(true);
        yAxis.setStartMargin(18);
        yAxis.setEndMargin(18);
        yAxis.setTickLabelFill(TEXT_MUTED_COLOR);
        yAxis.setTickLabelFont(javafx.scene.text.Font.font("Be Vietnam Pro", javafx.scene.text.FontWeight.MEDIUM, 11));

        javafx.scene.chart.BarChart<Number, String> chart = new javafx.scene.chart.BarChart<>(xAxis, yAxis) {
            @Override
            protected void layoutPlotChildren() {
                super.layoutPlotChildren();
                alignHorizontalBarCenters(this);
            }
        };
        chart.getStyleClass().addAll("dashboard-bar-chart", "horizontal-bar-chart");
        chart.setTitle(null);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setAlternativeColumnFillVisible(false);
        chart.setAlternativeRowFillVisible(false);
        chart.setHorizontalGridLinesVisible(true);
        chart.setVerticalGridLinesVisible(false);
        chart.setCategoryGap(12);
        chart.setBarGap(4);
        chart.setPrefHeight(260);
        chart.setMinHeight(260);
        chart.setMaxHeight(260);
        return chart;
    }

    private void configureDashboardCategoryAxis(javafx.scene.chart.CategoryAxis axis, java.util.List<String> categories) {
        axis.setCategories(javafx.collections.FXCollections.observableArrayList(categories));
        int maxLabelLength = categories.stream()
            .filter(java.util.Objects::nonNull)
            .mapToInt(String::length)
            .max()
            .orElse(0);
        boolean denseMonthlyLabels = categories.size() >= 10 || maxLabelLength >= 6;
        axis.setTickLabelRotation(denseMonthlyLabels ? -32 : 0);
        axis.setTickLabelGap(denseMonthlyLabels ? 8 : 4);
        axis.setTickLabelFont(javafx.scene.text.Font.font(
            "Be Vietnam Pro",
            javafx.scene.text.FontWeight.MEDIUM,
            denseMonthlyLabels ? 10 : 11
        ));
    }

    private void alignVerticalBarCenters(javafx.scene.chart.BarChart<String, Number> chart) {
        if (!(chart.getXAxis() instanceof javafx.scene.chart.CategoryAxis xAxis)) {
            return;
        }
        for (javafx.scene.chart.XYChart.Series<String, Number> series : chart.getData()) {
            for (javafx.scene.chart.XYChart.Data<String, Number> data : series.getData()) {
                javafx.scene.Node node = data.getNode();
                String category = data.getXValue();
                if (node == null || category == null) {
                    continue;
                }
                javafx.geometry.Bounds bounds = node.getBoundsInParent();
                double desiredCenterX = xAxis.getDisplayPosition(category);
                double currentCenterX = bounds.getMinX() + bounds.getWidth() / 2.0;
                node.setTranslateX(node.getTranslateX() + (desiredCenterX - currentCenterX));
            }
        }
    }

    private void alignHorizontalBarCenters(javafx.scene.chart.BarChart<Number, String> chart) {
        if (!(chart.getYAxis() instanceof javafx.scene.chart.CategoryAxis yAxis)) {
            return;
        }
        for (javafx.scene.chart.XYChart.Series<Number, String> series : chart.getData()) {
            for (javafx.scene.chart.XYChart.Data<Number, String> data : series.getData()) {
                javafx.scene.Node node = data.getNode();
                String category = data.getYValue();
                if (node == null || category == null) {
                    continue;
                }
                javafx.geometry.Bounds bounds = node.getBoundsInParent();
                double desiredCenterY = yAxis.getDisplayPosition(category);
                double currentCenterY = bounds.getMinY() + bounds.getHeight() / 2.0;
                node.setTranslateY(node.getTranslateY() + (desiredCenterY - currentCenterY));
            }
        }
    }

    private javafx.scene.chart.XYChart.Data<String, Number> createDashboardLineData(String category, Number value, String strokeValue, String tooltipText) {
        javafx.scene.chart.XYChart.Data<String, Number> data = new javafx.scene.chart.XYChart.Data<>(category, value);
        data.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) {
                newNode.setStyle(
                    "-fx-background-color: " + strokeValue + ", -app-surface; " +
                    "-fx-background-insets: 0, 2; " +
                    "-fx-background-radius: 5, 5; " +
                    "-fx-padding: 5;"
                );
                installTooltip(newNode, tooltipText);
            }
        });
        return data;
    }

    private void applyLineSeriesStyling(javafx.scene.chart.XYChart.Series<String, Number> series, String strokeValue) {
        series.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) {
                newNode.setStyle(
                    "-fx-stroke: " + strokeValue + "; " +
                    "-fx-stroke-width: 2.4px;"
                );
            }
        });
    }

    private javafx.scene.chart.XYChart.Data<Number, String> createDashboardHorizontalBarData(Number value, String category, String barFillValue, String tooltipText) {
        javafx.scene.chart.XYChart.Data<Number, String> data = new javafx.scene.chart.XYChart.Data<>(value, category);
        data.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) {
                newNode.setStyle("-fx-bar-fill: " + barFillValue + ";");
                installTooltip(newNode, tooltipText);
            }
        });
        return data;
    }

    private void configureDashboardVerticalValueAxis(javafx.scene.chart.XYChart<String, Number> chart, java.util.Collection<? extends Number> values, boolean wholeNumbers) {
        if (!(chart.getYAxis() instanceof javafx.scene.chart.NumberAxis yAxis)) {
            return;
        }
        double maxValue = values.stream()
            .filter(java.util.Objects::nonNull)
            .mapToDouble(Number::doubleValue)
            .max()
            .orElse(0.0);
        double upperBound;
        double tickUnit;
        if (wholeNumbers) {
            upperBound = maxValue <= 0 ? 1.0 : Math.max(1.0, Math.ceil(maxValue * 1.4));
            tickUnit = Math.max(1.0, Math.ceil(upperBound / 4.0));
        } else {
            upperBound = maxValue <= 0 ? 100.0 : Math.max(1.0, Math.ceil(maxValue * 1.15));
            tickUnit = Math.max(1.0, Math.ceil(upperBound / 5.0));
        }
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0.0);
        yAxis.setUpperBound(upperBound);
        yAxis.setTickUnit(tickUnit);
    }

    private void configureDashboardHorizontalValueAxis(javafx.scene.chart.BarChart<Number, String> chart, java.util.Collection<? extends Number> values, boolean wholeNumbers) {
        if (!(chart.getXAxis() instanceof javafx.scene.chart.NumberAxis xAxis)) {
            return;
        }
        double maxValue = values.stream()
            .filter(java.util.Objects::nonNull)
            .mapToDouble(Number::doubleValue)
            .max()
            .orElse(0.0);
        double upperBound;
        double tickUnit;
        if (wholeNumbers) {
            upperBound = maxValue <= 0 ? 1.0 : Math.max(1.0, Math.ceil(maxValue * 1.4));
            tickUnit = Math.max(1.0, Math.ceil(upperBound / 4.0));
        } else {
            upperBound = maxValue <= 0 ? 100.0 : Math.max(1.0, Math.ceil(maxValue * 1.15));
            tickUnit = Math.max(1.0, Math.ceil(upperBound / 5.0));
        }
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(0.0);
        xAxis.setUpperBound(upperBound);
        xAxis.setTickUnit(tickUnit);
    }

    private javafx.scene.Node createPaymentMethodShareContent(
        java.util.Map<com.pbl3.project.pbl3_project.entity.PaymentMethod, Long> paymentCounts,
        String emptyText
    ) {
        long total = paymentCounts.values().stream().mapToLong(Long::longValue).sum();
        if (total <= 0) {
            return createDashboardPlaceholder(emptyText);
        }

        javafx.scene.chart.PieChart pieChart = new javafx.scene.chart.PieChart();
        pieChart.getStyleClass().add("dashboard-payment-chart");
        pieChart.setLegendVisible(false);
        pieChart.setLabelsVisible(paymentCounts.values().stream().filter(count -> count != null && count > 0).count() > 1);
        pieChart.setClockwise(true);
        pieChart.setPrefHeight(260);
        pieChart.setMinHeight(260);
        pieChart.setMaxHeight(260);

        java.util.Map<com.pbl3.project.pbl3_project.entity.PaymentMethod, String> sliceColors = new java.util.LinkedHashMap<>();
        sliceColors.put(com.pbl3.project.pbl3_project.entity.PaymentMethod.CASH, PRIMARY_HEX);
        sliceColors.put(com.pbl3.project.pbl3_project.entity.PaymentMethod.CARD, SUCCESS_HEX);
        sliceColors.put(com.pbl3.project.pbl3_project.entity.PaymentMethod.QR, "#f59e0b");

        javafx.scene.layout.FlowPane legendPane = new javafx.scene.layout.FlowPane();
        legendPane.getStyleClass().add("dashboard-chart-legend");
        legendPane.setHgap(14);
        legendPane.setVgap(8);
        legendPane.setAlignment(Pos.CENTER);

        for (com.pbl3.project.pbl3_project.entity.PaymentMethod method : java.util.List.of(
            com.pbl3.project.pbl3_project.entity.PaymentMethod.CASH,
            com.pbl3.project.pbl3_project.entity.PaymentMethod.CARD,
            com.pbl3.project.pbl3_project.entity.PaymentMethod.QR
        )) {
            long count = paymentCounts.getOrDefault(method, 0L);
            if (count <= 0) {
                continue;
            }
            String color = sliceColors.getOrDefault(method, PRIMARY_HEX);
            javafx.scene.chart.PieChart.Data data = new javafx.scene.chart.PieChart.Data(formatPaymentMethodLabel(method), count);
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    newNode.setStyle("-fx-pie-color: " + color + ";");
                    double percentage = (count * 100.0) / total;
                    installTooltip(newNode, formatPaymentMethodLabel(method) + ": " + count + " orders (" + String.format("%.1f%%", percentage) + ")");
                }
            });
            pieChart.getData().add(data);

            javafx.scene.layout.HBox legendItem = new javafx.scene.layout.HBox(8);
            legendItem.setAlignment(Pos.CENTER_LEFT);
            javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(5, javafx.scene.paint.Color.web(color));
            javafx.scene.control.Label legendLabel = new javafx.scene.control.Label(
                formatPaymentMethodLabel(method) + " (" + count + ", " + String.format("%.1f%%", (count * 100.0) / total) + ")"
            );
            legendLabel.getStyleClass().add("dashboard-chart-legend-label");
            legendLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -app-text-secondary;");
            legendItem.getChildren().addAll(dot, legendLabel);
            legendPane.getChildren().add(legendItem);
        }

        if (pieChart.getData().isEmpty()) {
            return createDashboardPlaceholder(emptyText);
        }

        javafx.scene.layout.VBox wrapper = new javafx.scene.layout.VBox(12, pieChart, legendPane);
        wrapper.setAlignment(Pos.CENTER);
        return wrapper;
    }

    private javafx.scene.Node createTopSellingChartContent(java.util.List<com.pbl3.project.pbl3_project.dto.report.TopSellingProductRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return createDashboardPlaceholder("No top-selling data in the last 7 days");
        }

        javafx.scene.chart.BarChart<Number, String> chart = createDashboardHorizontalBarChart("Net Sold", "Product");
        if (chart.getYAxis() instanceof javafx.scene.chart.CategoryAxis topSellingYAxis) {
            java.util.List<String> categories = rows.stream()
                .map(row -> abbreviateLabel(row.productName(), 24))
                .toList();
            topSellingYAxis.setCategories(javafx.collections.FXCollections.observableArrayList(categories));
            topSellingYAxis.setTickLabelRotation(0);
            topSellingYAxis.setTickLabelGap(6);
            topSellingYAxis.setTickLabelFont(javafx.scene.text.Font.font(
                "Be Vietnam Pro",
                javafx.scene.text.FontWeight.MEDIUM,
                11
            ));
        }
        double chartHeight = Math.max(260, rows.size() * 44 + 80);
        chart.setPrefHeight(chartHeight);
        chart.setMinHeight(chartHeight);
        chart.setMaxHeight(chartHeight);
        javafx.scene.chart.XYChart.Series<Number, String> series = new javafx.scene.chart.XYChart.Series<>();
        for (com.pbl3.project.pbl3_project.dto.report.TopSellingProductRow row : rows) {
            String shortLabel = abbreviateLabel(row.productName(), 24);
            String tooltipText = row.productName()
                + "\nCategory: " + row.categoryName()
                + "\nNet Sold: " + row.netSoldQuantity()
                + "\nRevenue: " + context.support().formatVnd(row.netRevenue());
            series.getData().add(createDashboardHorizontalBarData(row.netSoldQuantity(), shortLabel, PRIMARY_BAR_FILL, tooltipText));
        }
        chart.getData().add(series);
        configureDashboardHorizontalValueAxis(chart, rows.stream().map(com.pbl3.project.pbl3_project.dto.report.TopSellingProductRow::netSoldQuantity).toList(), true);
        return chart;
    }

    private javafx.scene.Node createDashboardPlaceholder(String text) {
        Label placeholder = new Label(text);
        placeholder.getStyleClass().add("dashboard-placeholder-label");
        placeholder.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-muted;");

        javafx.scene.layout.StackPane wrapper = new javafx.scene.layout.StackPane(placeholder);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setMinHeight(260);
        wrapper.setPrefHeight(260);
        wrapper.setMaxHeight(260);
        wrapper.setStyle("-fx-background-color: derive(-app-surface-muted, 8%); -fx-background-radius: 12;");
        return wrapper;
    }

    private void makeDashboardDrillDown(javafx.scene.Node node, String tooltipText, Runnable action) {
        if (node == null || action == null) {
            return;
        }
        if (!node.getStyleClass().contains("dashboard-drilldown-target")) {
            node.getStyleClass().add("dashboard-drilldown-target");
        }
        node.setCursor(javafx.scene.Cursor.HAND);
        installTooltip(node, tooltipText);
        node.setOnMouseClicked(event -> {
            if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                action.run();
            }
        });
    }

    private void installDashboardPaneHover(javafx.scene.Node node) {
        if (node == null) {
            return;
        }
        final javafx.animation.Timeline[] hoverTimelineRef = new javafx.animation.Timeline[1];
        java.util.function.BiConsumer<Double, Double> animateTo = (scale, translateY) -> {
            if (isReducedMotionEnabled(node)) {
                if (hoverTimelineRef[0] != null) {
                    hoverTimelineRef[0].stop();
                }
                node.setScaleX(1.0);
                node.setScaleY(1.0);
                node.setTranslateY(0.0);
                return;
            }
            if (hoverTimelineRef[0] != null) {
                hoverTimelineRef[0].stop();
            }
            hoverTimelineRef[0] = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                    javafx.util.Duration.millis(170),
                    new javafx.animation.KeyValue(node.scaleXProperty(), scale, javafx.animation.Interpolator.EASE_BOTH),
                    new javafx.animation.KeyValue(node.scaleYProperty(), scale, javafx.animation.Interpolator.EASE_BOTH),
                    new javafx.animation.KeyValue(node.translateYProperty(), translateY, javafx.animation.Interpolator.EASE_BOTH)
                )
            );
            hoverTimelineRef[0].play();
        };

        node.setOnMouseEntered(e -> animateTo.accept(1.012, -4.0));
        node.setOnMouseExited(e -> animateTo.accept(1.0, 0.0));
        node.setOnMousePressed(e -> {
            if (!node.isHover()) {
                return;
            }
            animateTo.accept(1.006, -2.0);
        });
        node.setOnMouseReleased(e -> {
            if (node.isHover()) {
                animateTo.accept(1.012, -4.0);
            } else {
                animateTo.accept(1.0, 0.0);
            }
        });
    }

    private void scrollNodeIntoView(javafx.scene.control.ScrollPane scrollPane, javafx.scene.Node node) {
        if (scrollPane == null || node == null || scrollPane.getContent() == null) {
            return;
        }
        javafx.application.Platform.runLater(() -> {
            scrollPane.applyCss();
            scrollPane.layout();
            scrollPane.getContent().applyCss();
            if (scrollPane.getContent() instanceof javafx.scene.Parent contentParent) {
                contentParent.layout();
            }

            javafx.geometry.Bounds contentBounds = scrollPane.getContent().getLayoutBounds();
            javafx.geometry.Bounds viewportBounds = scrollPane.getViewportBounds();
            javafx.geometry.Bounds nodeBoundsInScene = node.localToScene(node.getBoundsInLocal());
            javafx.geometry.Bounds contentBoundsInScene = scrollPane.getContent().localToScene(scrollPane.getContent().getLayoutBounds());
            if (nodeBoundsInScene == null || contentBoundsInScene == null) {
                return;
            }
            double nodeMinYInContent = nodeBoundsInScene.getMinY() - contentBoundsInScene.getMinY();
            double availableHeight = contentBounds.getHeight() - viewportBounds.getHeight();
            if (availableHeight <= 0) {
                scrollPane.setVvalue(0);
                return;
            }
            double topOffset = 12.0;
            double targetVvalue = Math.max(0.0, Math.min(1.0, (nodeMinYInContent - topOffset) / availableHeight));
            scrollPane.setVvalue(targetVvalue);
        });
    }

    private void revealReportSection(javafx.scene.control.ScrollPane scrollPane, javafx.scene.Node node) {
        if (scrollPane == null || node == null) {
            return;
        }
        scrollNodeIntoView(scrollPane, node);

        javafx.animation.PauseTransition secondPass = new javafx.animation.PauseTransition(javafx.util.Duration.millis(90));
        secondPass.setOnFinished(event -> scrollNodeIntoView(scrollPane, node));
        secondPass.play();

        javafx.animation.PauseTransition thirdPass = new javafx.animation.PauseTransition(javafx.util.Duration.millis(190));
        thirdPass.setOnFinished(event -> scrollNodeIntoView(scrollPane, node));
        thirdPass.play();
    }

    private String buildOperationalReportContextLabel(com.pbl3.project.pbl3_project.dto.report.OperationalReportData reportData) {
        return "Sales: "
            + formatOperationalReportRangeLabel(reportData.salesMix().startDate(), reportData.salesMix().endDate())
            + " | Inventory: Current snapshot";
    }

    private void installTooltip(javafx.scene.Node node, String text) {
        javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip(text);
        tooltip.setShowDelay(javafx.util.Duration.millis(120));
        javafx.scene.control.Tooltip.install(node, tooltip);
    }

    private String formatDashboardCurrencyDelta(BigDecimal delta) {
        BigDecimal normalizedDelta = MoneySupport.normalize(delta);
        if (normalizedDelta.signum() == 0) {
            return "No change vs yesterday";
        }
        String sign = normalizedDelta.signum() > 0 ? "+" : "-";
        return sign + context.support().formatVnd(normalizedDelta.abs()) + " vs yesterday";
    }

    private String formatDashboardCountDelta(long delta, String noun) {
        if (delta == 0) {
            return "No change vs yesterday";
        }
        String sign = delta > 0 ? "+" : "-";
        return sign + Math.abs(delta) + " " + noun + " vs yesterday";
    }

    private String getDashboardDeltaColor(BigDecimal delta, boolean higherIsBetter) {
        BigDecimal normalizedDelta = MoneySupport.normalize(delta);
        if (normalizedDelta.signum() == 0) {
            return "-app-text-muted";
        }
        boolean positiveDirection = higherIsBetter ? normalizedDelta.signum() > 0 : normalizedDelta.signum() < 0;
        return positiveDirection ? "-app-success-hover" : "-app-danger-hover";
    }

    private String getDashboardDeltaColor(long delta, boolean higherIsBetter) {
        if (delta == 0) {
            return "-app-text-muted";
        }
        boolean positiveDirection = higherIsBetter ? delta > 0 : delta < 0;
        return positiveDirection ? "-app-success-hover" : "-app-danger-hover";
    }

    private String formatPaymentMethodLabel(com.pbl3.project.pbl3_project.entity.PaymentMethod method) {
        return context.support().formatPaymentMethodLabel(method);
    }

    private String abbreviateLabel(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text != null ? text : "-";
        }
        return text.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private Button createReportExportButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().addAll("primary-button", "report-export-button");
        button.setFocusTraversable(false);
        button.setStyle("-fx-background-radius: 999; -fx-padding: 8 16; -fx-font-size: 13px;");
        return button;
    }

    private HBox createReportActionGroup(Button... buttons) {
        HBox group = new HBox(8);
        group.setAlignment(Pos.CENTER_RIGHT);
        if (buttons != null) {
            group.getChildren().addAll(buttons);
        }
        return group;
    }

    private Button createReportPresetButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().addAll("button", "report-preset-button", "dashboard-report-secondary-button");
        button.setFocusTraversable(false);
        button.setStyle("-fx-background-radius: 999; -fx-padding: 6 14; -fx-font-size: 12px;");
        return button;
    }

    private VBox createReportSection(String title, String subtitle, javafx.scene.Node content, javafx.scene.Node actionNode, String badgeText) {
        VBox section = new VBox(14);
        section.getStyleClass().add("report-section-card");
        if (badgeText != null && !badgeText.isBlank()) {
            section.getStyleClass().add("report-section-active");
        }
        section.setPadding(new Insets(18));
        section.setMinWidth(0);
        section.setFillWidth(true);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("report-section-title");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -app-text-primary;");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(Double.MAX_VALUE);

        VBox titleBox = new VBox(4);
        titleBox.getChildren().add(titleLabel);
        if (subtitle != null && !subtitle.isBlank()) {
            Label subtitleLabel = new Label(subtitle);
            subtitleLabel.getStyleClass().add("report-section-subtitle");
            subtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -app-text-muted;");
            subtitleLabel.setWrapText(true);
            subtitleLabel.setMaxWidth(Double.MAX_VALUE);
            subtitleLabel.prefWidthProperty().bind(titleBox.widthProperty());
            subtitleLabel.maxWidthProperty().bind(titleBox.widthProperty());
            titleBox.getChildren().add(subtitleLabel);
        }
        titleBox.setMinWidth(0);
        titleBox.setMaxWidth(Double.MAX_VALUE);
        titleBox.setFillWidth(true);

        javafx.scene.layout.HBox headerActions = new javafx.scene.layout.HBox(10);
        headerActions.setAlignment(Pos.TOP_RIGHT);
        headerActions.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        headerActions.setPrefWidth(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
        if (badgeText != null && !badgeText.isBlank()) {
            Label badgeLabel = new Label(badgeText);
            badgeLabel.getStyleClass().add("report-section-focus-badge");
            headerActions.getChildren().add(badgeLabel);
        }
        if (actionNode != null) {
            headerActions.getChildren().add(actionNode);
        }

        javafx.scene.layout.HBox.setHgrow(titleBox, javafx.scene.layout.Priority.ALWAYS);

        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(12);
        header.setAlignment(Pos.TOP_LEFT);
        header.setMinWidth(0);
        header.setMaxWidth(Double.MAX_VALUE);
        header.getChildren().add(titleBox);
        if (!headerActions.getChildren().isEmpty()) {
            header.getChildren().add(headerActions);
        }

        titleLabel.prefWidthProperty().bind(titleBox.widthProperty());
        titleLabel.maxWidthProperty().bind(titleBox.widthProperty());

        VBox.setVgrow(content, javafx.scene.layout.Priority.ALWAYS);
        section.getChildren().addAll(header, content);
        return section;
    }

    private ReportFocusTarget mapInsightDrilldownTarget(com.pbl3.project.pbl3_project.dto.report.InsightDrilldownTarget target) {
        if (target == null) {
            return null;
        }
        return switch (target) {
            case SUMMARY -> ReportFocusTarget.SUMMARY;
            case ACTION_CENTER -> ReportFocusTarget.ACTION_CENTER;
            case REORDER -> ReportFocusTarget.REORDER;
            case WHAT_CHANGED -> ReportFocusTarget.WHAT_CHANGED;
            case REVENUE -> ReportFocusTarget.REVENUE;
            case ORDERS -> ReportFocusTarget.ORDERS;
            case CANCELED_ORDERS -> ReportFocusTarget.CANCELED_ORDERS;
            case TOP_SELLING -> ReportFocusTarget.TOP_SELLING;
            case AGING_STOCK -> ReportFocusTarget.AGING_STOCK;
        };
    }

    private Runnable createDashboardInsightAction(
        Stage stage,
        User user,
        com.pbl3.project.pbl3_project.dto.report.InsightDrilldownTarget target
    ) {
        if (target == null || !context.authorizationService().canAccessReports(user)) {
            return null;
        }
        ReportFocusTarget focusTarget = mapInsightDrilldownTarget(target);
        if (focusTarget == null) {
            return null;
        }

        java.time.LocalDate startDate = null;
        java.time.LocalDate endDate = null;
        if (target == com.pbl3.project.pbl3_project.dto.report.InsightDrilldownTarget.REVENUE
            || target == com.pbl3.project.pbl3_project.dto.report.InsightDrilldownTarget.ORDERS
            || target == com.pbl3.project.pbl3_project.dto.report.InsightDrilldownTarget.CANCELED_ORDERS
            || target == com.pbl3.project.pbl3_project.dto.report.InsightDrilldownTarget.TOP_SELLING
            || target == com.pbl3.project.pbl3_project.dto.report.InsightDrilldownTarget.WHAT_CHANGED
            || target == com.pbl3.project.pbl3_project.dto.report.InsightDrilldownTarget.SUMMARY) {
            startDate = java.time.LocalDate.now();
            endDate = java.time.LocalDate.now();
        }

        java.time.LocalDate finalStartDate = startDate;
        java.time.LocalDate finalEndDate = endDate;
        return () -> context.navigator().showReports(finalStartDate, finalEndDate, focusTarget);
    }

    private Runnable createReportInsightAction(
        javafx.scene.control.ScrollPane scrollPane,
        java.util.Map<ReportFocusTarget, javafx.scene.Node> anchors,
        com.pbl3.project.pbl3_project.dto.report.InsightDrilldownTarget target
    ) {
        ReportFocusTarget focusTarget = mapInsightDrilldownTarget(target);
        if (focusTarget == null) {
            return null;
        }
        return () -> {
            javafx.scene.Node node = anchors.get(focusTarget);
            if (node != null) {
                revealReportSection(scrollPane, node);
            }
        };
    }

    private Runnable createActionCenterItemAction(
        Stage stage,
        User user,
        com.pbl3.project.pbl3_project.dto.report.ActionCenterItem item,
        java.util.function.Function<com.pbl3.project.pbl3_project.dto.report.InsightDrilldownTarget, Runnable> drilldownResolver
    ) {
        if (item == null) {
            return null;
        }
        if (item.type() == com.pbl3.project.pbl3_project.dto.report.ActionCenterType.REORDER_NOW
            && item.productId() != null
            && item.suggestedQuantity() != null
            && item.suggestedQuantity() > 0) {
            ImportOrderPrefill prefill = new ImportOrderPrefill(item.productId(), item.suggestedQuantity());
            return () -> context.navigator().showImportGoods(prefill);
        }
        return drilldownResolver != null ? drilldownResolver.apply(item.drilldownTarget()) : null;
    }

    private Runnable createReorderAction(
        Stage stage,
        User user,
        com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow row
    ) {
        if (row == null || row.productId() == null || row.suggestedReorderQty() <= 0) {
            return null;
        }
        ImportOrderPrefill prefill = new ImportOrderPrefill(row.productId(), row.suggestedReorderQty());
        return () -> context.navigator().showImportGoods(prefill);
    }

    private javafx.scene.Node createWhatChangedContent(
        com.pbl3.project.pbl3_project.dto.report.WhatChangedSnapshot snapshot,
        java.util.function.Function<com.pbl3.project.pbl3_project.dto.report.InsightDrilldownTarget, Runnable> actionResolver,
        String emptyText,
        javafx.beans.value.ObservableNumberValue widthSource
    ) {
        if (snapshot == null || snapshot.insights() == null || snapshot.insights().isEmpty()) {
            return createCompactInsightPlaceholder(emptyText);
        }

        java.util.List<VBox> cards = new java.util.ArrayList<>();

        for (com.pbl3.project.pbl3_project.dto.report.WhatChangedInsight insight : snapshot.insights()) {
            Runnable action = actionResolver != null ? actionResolver.apply(insight.drilldownTarget()) : null;
            cards.add(createInsightDigestCard(
                createWhatChangedInsightIcon(insight.type(), insight.severity()),
                formatWhatChangedType(insight.type()),
                insight.headline(),
                insight.detail(),
                insight.severity(),
                extractWhatChangedDeltaChip(insight),
                action != null ? "Open" : null,
                action,
                true
            ));
        }
        return createResponsiveInsightGrid(cards, widthSource, 860.0);
    }

    private javafx.scene.Node createActionCenterContent(
        Stage stage,
        User user,
        com.pbl3.project.pbl3_project.dto.report.ActionCenterSnapshot snapshot,
        int maxItems,
        java.util.function.Function<com.pbl3.project.pbl3_project.dto.report.InsightDrilldownTarget, Runnable> drilldownResolver
    ) {
        if (snapshot == null || snapshot.items() == null || snapshot.items().isEmpty()) {
            return createCompactInsightPlaceholder("No action items right now");
        }

        VBox content = new VBox(10);
        content.setFillWidth(true);

        int limit = Math.min(maxItems, snapshot.items().size());
        java.util.List<com.pbl3.project.pbl3_project.dto.report.ActionCenterItem> visibleItems =
            snapshot.items().subList(0, limit);
        java.util.List<com.pbl3.project.pbl3_project.dto.report.InsightSeverity> severityOrder = java.util.List.of(
            com.pbl3.project.pbl3_project.dto.report.InsightSeverity.CRITICAL,
            com.pbl3.project.pbl3_project.dto.report.InsightSeverity.WARNING,
            com.pbl3.project.pbl3_project.dto.report.InsightSeverity.INFO
        );

        for (com.pbl3.project.pbl3_project.dto.report.InsightSeverity severity : severityOrder) {
            java.util.List<com.pbl3.project.pbl3_project.dto.report.ActionCenterItem> severityItems = visibleItems.stream()
                .filter(item -> item.severity() == severity)
                .toList();
            if (severityItems.isEmpty()) {
                continue;
            }

            VBox groupBox = new VBox(10);
            groupBox.setFillWidth(true);
            for (com.pbl3.project.pbl3_project.dto.report.ActionCenterItem item : severityItems) {
                Runnable action = createActionCenterItemAction(stage, user, item, drilldownResolver);
                groupBox.getChildren().add(createInsightDigestCard(
                    createActionCenterInsightIcon(item.type(), item.severity()),
                    formatActionCenterType(item.type()),
                    item.title(),
                    item.description(),
                    item.severity(),
                    createActionCenterImpactChip(item),
                    formatInsightActionLabel(item.actionLabel()),
                    action,
                    true
                ));
            }
            boolean collapsible = severity == com.pbl3.project.pbl3_project.dto.report.InsightSeverity.CRITICAL
                || severity == com.pbl3.project.pbl3_project.dto.report.InsightSeverity.WARNING;
            content.getChildren().add(createInsightSeverityGroup(
                severity,
                severityItems.size(),
                groupBox,
                collapsible,
                collapsible
            ));
        }
        return content;
    }

    private javafx.scene.Node createExplainableReorderContent(
        Stage stage,
        User user,
        com.pbl3.project.pbl3_project.dto.report.ExplainableReorderSnapshot snapshot,
        int maxItems,
        boolean detailed
    ) {
        java.util.List<com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow> rows =
            snapshot != null && snapshot.rows() != null ? snapshot.rows() : java.util.List.of();
        if (rows.isEmpty()) {
            return createCompactInsightPlaceholder("No reorder candidates right now");
        }

        java.util.List<com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow> limitedRows =
            rows.subList(0, Math.min(maxItems, rows.size()));

        return detailed
            ? createExplainableReorderDetailedContent(stage, user, limitedRows)
            : createExplainableReorderSummaryContent(stage, user, limitedRows);
    }

    private void configureExplainableReorderColumn(
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow, ?> column,
        double minWidth,
        double prefWidth,
        boolean resizable
    ) {
        column.setMinWidth(minWidth);
        column.setPrefWidth(prefWidth);
        column.setResizable(resizable);
    }

    private javafx.scene.Node createExplainableReorderSummaryContent(
        Stage stage,
        User user,
        java.util.List<com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow> rows
    ) {
        VBox content = new VBox(10);
        content.setFillWidth(true);
        for (com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow row : rows) {
            content.getChildren().add(createExplainableReorderSummaryCard(stage, user, row));
        }
        return content;
    }

    private javafx.scene.Node createExplainableReorderDetailedContent(
        Stage stage,
        User user,
        java.util.List<com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow> rows
    ) {
        javafx.scene.control.TableView<com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow> table =
            new javafx.scene.control.TableView<>();
        TableViewSupport.enableDragSelection(table);
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setItems(javafx.collections.FXCollections.observableArrayList(rows));
        double visibleRowCount = Math.max(1, Math.min(6, rows.size()));
        double targetTableHeight = 58 + visibleRowCount * 44;
        table.setMinHeight(targetTableHeight);
        table.setPrefHeight(targetTableHeight);
        table.setMaxHeight(targetTableHeight);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow, String> productCol =
            new javafx.scene.control.TableColumn<>("Product");
        productCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().productName()));
        productCol.setCellFactory(createExplainableReorderTextCell(Pos.CENTER_LEFT));
        configureExplainableReorderColumn(productCol, 220, 260, true);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow, Number> onHandCol =
            new javafx.scene.control.TableColumn<>("On Hand");
        onHandCol.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().onHandQuantity()));
        onHandCol.setStyle("-fx-alignment: CENTER;");
        configureExplainableReorderColumn(onHandCol, 86, 96, false);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow, String> avgCol =
            new javafx.scene.control.TableColumn<>("Avg/Day");
        avgCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            formatCompactDecimal(data.getValue().avgDailyUnits14d())
        ));
        avgCol.setStyle("-fx-alignment: CENTER;");
        configureExplainableReorderColumn(avgCol, 94, 104, false);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow, String> coverageCol =
            new javafx.scene.control.TableColumn<>("Coverage");
        coverageCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatExplainableCoverageShort(data.getValue())));
        coverageCol.setCellFactory(createExplainableCoverageCell());
        coverageCol.setStyle("-fx-alignment: CENTER;");
        configureExplainableReorderColumn(coverageCol, 112, 126, false);

        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow, Number> suggestedCol =
            new javafx.scene.control.TableColumn<>("Suggested");
        suggestedCol.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().suggestedReorderQty()));
        suggestedCol.setStyle("-fx-alignment: CENTER;");
        configureExplainableReorderColumn(suggestedCol, 98, 108, false);

        table.getColumns().addAll(productCol, onHandCol, avgCol, coverageCol, suggestedCol);

        VBox detailPane = new VBox();
        detailPane.setFillWidth(true);
        detailPane.setMinWidth(0);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldRow, newRow) ->
            updateExplainableReorderDetailPane(detailPane, stage, user, newRow)
        );

        updateExplainableReorderDetailPane(detailPane, stage, user, null);

        VBox content = new VBox(12, table, detailPane);
        content.setFillWidth(true);
        TableViewSupport.enableDeselectOnOutsideClick(content, table);
        return content;
    }

    private void updateExplainableReorderDetailPane(
        VBox detailPane,
        Stage stage,
        User user,
        com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow row
    ) {
        detailPane.getChildren().setAll(
            row != null
                ? createExplainableReorderDetailCard(stage, user, row)
                : createCompactInsightPlaceholder("Select a product to view reorder details")
        );
    }

    private VBox createExplainableReorderSummaryCard(
        Stage stage,
        User user,
        com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow row
    ) {
        VBox card = new VBox(12);
        card.getStyleClass().addAll("insight-card", "reorder-summary-card");
        card.setFillWidth(true);

        Label titleLabel = new Label(row.productName());
        titleLabel.getStyleClass().add("insight-card-title");
        titleLabel.setWrapText(true);

        VBox titleBox = new VBox(4);
        titleBox.setFillWidth(true);
        titleBox.getChildren().add(titleLabel);
        if (row.categoryName() != null && !row.categoryName().isBlank()) {
            Label categoryLabel = new Label(row.categoryName());
            categoryLabel.getStyleClass().add("reorder-summary-subtitle");
            titleBox.getChildren().add(categoryLabel);
        }

        javafx.scene.layout.FlowPane metrics = createExplainableReorderMetricFlow(row);

        Label explanationLabel = new Label(row.explanation());
        explanationLabel.getStyleClass().add("insight-card-detail");
        explanationLabel.setWrapText(true);

        javafx.scene.layout.HBox footer = new javafx.scene.layout.HBox(12);
        footer.setAlignment(Pos.CENTER_LEFT);

        String supportText = buildExplainableReorderSupportText(row);
        if (!supportText.isBlank()) {
            Label supportLabel = new Label(supportText);
            supportLabel.getStyleClass().add("reorder-summary-support");
            supportLabel.setWrapText(true);
            supportLabel.setMaxWidth(Double.MAX_VALUE);
            javafx.scene.layout.HBox.setHgrow(supportLabel, javafx.scene.layout.Priority.ALWAYS);
            footer.getChildren().add(supportLabel);
        }

        Runnable action = createReorderAction(stage, user, row);
        Button actionButton = createImportActionButton(action, row.suggestedReorderQty() <= 0);
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        footer.getChildren().addAll(spacer, actionButton);

        card.getChildren().addAll(titleBox, metrics, explanationLabel, footer);
        return card;
    }

    private VBox createExplainableReorderDetailCard(
        Stage stage,
        User user,
        com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow row
    ) {
        VBox card = new VBox(14);
        card.getStyleClass().addAll("insight-card", "reorder-detail-card");
        card.setFillWidth(true);

        Label titleLabel = new Label(row.productName());
        titleLabel.getStyleClass().add("insight-card-title");
        titleLabel.setWrapText(true);

        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getChildren().add(titleLabel);
        if (row.categoryName() != null && !row.categoryName().isBlank()) {
            Label categoryChip = new Label(row.categoryName());
            categoryChip.getStyleClass().add("reorder-category-chip");
            header.getChildren().add(categoryChip);
        }

        javafx.scene.layout.FlowPane metrics = createExplainableReorderMetricFlow(row);
        metrics.getChildren().add(createExplainableReorderMetricChip("Min", String.valueOf(row.minStockLevel()), false));

        javafx.scene.layout.FlowPane supportFlow = new javafx.scene.layout.FlowPane();
        supportFlow.setHgap(10);
        supportFlow.setVgap(10);
        supportFlow.setPrefWrapLength(720);
        supportFlow.getChildren().addAll(
            createExplainableReorderSupportCard("Last Inbound", row.lastInboundAt() != null ? context.support().formatDateTime(row.lastInboundAt()) : "-"),
            createExplainableReorderSupportCard("Latest Supplier", row.latestSupplierName() != null && !row.latestSupplierName().isBlank() ? row.latestSupplierName() : "-"),
            createExplainableReorderSupportCard("Latest Price", row.latestImportPrice() != null ? context.support().formatVnd(row.latestImportPrice()) : "-")
        );

        Label explanationTitle = new Label("Explainable Reorder");
        explanationTitle.getStyleClass().add("reorder-detail-heading");

        Label explanationLabel = new Label(row.explanation());
        explanationLabel.getStyleClass().add("insight-card-detail");
        explanationLabel.setWrapText(true);

        Runnable action = createReorderAction(stage, user, row);
        Button actionButton = createImportActionButton(action, row.suggestedReorderQty() <= 0);

        javafx.scene.layout.HBox footer = new javafx.scene.layout.HBox(actionButton);
        footer.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(header, metrics, supportFlow, explanationTitle, explanationLabel, footer);
        return card;
    }

    private javafx.scene.layout.FlowPane createExplainableReorderMetricFlow(
        com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow row
    ) {
        javafx.scene.layout.FlowPane metrics = new javafx.scene.layout.FlowPane();
        metrics.setHgap(8);
        metrics.setVgap(8);
        metrics.setPrefWrapLength(720);
        metrics.getChildren().addAll(
            createExplainableReorderMetricChip("On Hand", String.valueOf(row.onHandQuantity()), false),
            createExplainableReorderMetricChip("Avg/Day", formatCompactDecimal(row.avgDailyUnits14d()), false),
            createExplainableCoverageChip(row),
            createExplainableReorderMetricChip("Suggested", String.valueOf(row.suggestedReorderQty()), true)
        );
        return metrics;
    }

    private Label createExplainableReorderMetricChip(String label, String value, boolean emphasized) {
        Label chip = new Label(label + ": " + value);
        chip.getStyleClass().add(emphasized ? "reorder-metric-chip-strong" : "reorder-metric-chip");
        return chip;
    }

    private Label createExplainableCoverageChip(
        com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow row
    ) {
        String tone = getExplainableCoverageTone(row);
        Label chip = new Label(java.text.MessageFormat.format("Coverage: {0}", formatExplainableCoverageShort(row)));
        chip.getStyleClass().addAll("reorder-metric-chip", "reorder-metric-chip-coverage", "reorder-metric-chip-coverage-" + tone);
        return chip;
    }

    private VBox createExplainableReorderSupportCard(String label, String value) {
        VBox card = new VBox(4);
        card.getStyleClass().add("reorder-support-card");

        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("reorder-support-label");

        Label valueNode = new Label(value);
        valueNode.getStyleClass().add("reorder-support-value");
        valueNode.setWrapText(true);

        card.getChildren().addAll(labelNode, valueNode);
        return card;
    }

    private String buildExplainableReorderSupportText(
        com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow row
    ) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        if (row.lastInboundAt() != null) {
            parts.add(java.text.MessageFormat.format("Last inbound {0}", DISPLAY_DATE_FORMATTER.format(row.lastInboundAt().toLocalDate())));
        }
        if (row.latestSupplierName() != null && !row.latestSupplierName().isBlank()) {
            parts.add(java.text.MessageFormat.format("Supplier {0}", row.latestSupplierName()));
        }
        return String.join(" • ", parts);
    }

    private String formatExplainableCoverage(
        com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow row
    ) {
        return row != null && row.coverageKnown() && row.coverageDays() != null
            ? formatCompactDecimal(row.coverageDays()) + " days"
            : "Unknown";
    }

    private String formatExplainableCoverageShort(
        com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow row
    ) {
        return row != null && row.coverageKnown() && row.coverageDays() != null
            ? formatCompactDecimal(row.coverageDays()) + "d"
            : "Unknown";
    }

    private String getExplainableCoverageTone(
        com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow row
    ) {
        if (row == null || !row.coverageKnown() || row.coverageDays() == null) {
            return "unknown";
        }
        java.math.BigDecimal coverage = row.coverageDays();
        if (row.onHandQuantity() <= row.minStockLevel() || coverage.compareTo(BigDecimal.valueOf(3)) < 0) {
            return "critical";
        }
        if (coverage.compareTo(BigDecimal.valueOf(7)) < 0) {
            return "warning";
        }
        if (coverage.compareTo(BigDecimal.valueOf(14)) < 0) {
            return "watch";
        }
        return "stable";
    }

    private double getExplainableCoverageProgress(
        com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow row
    ) {
        if (row == null || !row.coverageKnown() || row.coverageDays() == null) {
            return 0.16;
        }
        return Math.max(0.08, Math.min(1.0, row.coverageDays().doubleValue() / 14.0));
    }

    private javafx.scene.Node createExplainableCoverageMetric(
        com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow row
    ) {
        String tone = getExplainableCoverageTone(row);

        Label titleLabel = new Label("Coverage");
        titleLabel.getStyleClass().add("reorder-coverage-label");

        Label valueLabel = new Label(formatExplainableCoverageShort(row));
        valueLabel.getStyleClass().addAll("reorder-coverage-value", "reorder-coverage-value-" + tone);

        javafx.scene.layout.Region track = new javafx.scene.layout.Region();
        track.getStyleClass().add("reorder-coverage-track");
        track.setMinSize(56, 6);
        track.setPrefSize(56, 6);
        track.setMaxSize(56, 6);

        javafx.scene.layout.Region fill = new javafx.scene.layout.Region();
        fill.getStyleClass().addAll("reorder-coverage-fill", "reorder-coverage-fill-" + tone);
        double fillWidth = 56 * getExplainableCoverageProgress(row);
        fill.setMinSize(fillWidth, 6);
        fill.setPrefSize(fillWidth, 6);
        fill.setMaxSize(fillWidth, 6);

        javafx.scene.layout.StackPane bar = new javafx.scene.layout.StackPane(track, fill);
        bar.setAlignment(Pos.CENTER_LEFT);
        javafx.scene.layout.StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        bar.setMinSize(javafx.scene.layout.Region.USE_PREF_SIZE, javafx.scene.layout.Region.USE_PREF_SIZE);
        bar.setPrefSize(javafx.scene.layout.Region.USE_COMPUTED_SIZE, javafx.scene.layout.Region.USE_COMPUTED_SIZE);

        VBox signal = new VBox(4, titleLabel, valueLabel, bar);
        signal.getStyleClass().add("reorder-coverage-signal");
        signal.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        return signal;
    }

    private com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow rowValue(
        javafx.scene.control.TableColumn.CellDataFeatures<
            com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow,
            String
        > data
    ) {
        return data != null ? data.getValue() : null;
    }

    private javafx.util.Callback<
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow, String>,
        javafx.scene.control.TableCell<com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow, String>
    > createExplainableCoverageCell() {
        return col -> new javafx.scene.control.TableCell<>() {
            private final Label badge = new Label();

            {
                setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
                setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setTooltip(null);
                    return;
                }
                com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow row =
                    getTableRow() != null ? getTableRow().getItem() : null;
                String tone = getExplainableCoverageTone(row);
                badge.setText(item);
                badge.getStyleClass().setAll("reorder-coverage-badge", "reorder-coverage-badge-" + tone);
                setTooltip(new javafx.scene.control.Tooltip(formatExplainableCoverage(row)));
                setGraphic(badge);
            }
        };
    }

    private javafx.util.Callback<
        javafx.scene.control.TableColumn<com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow, String>,
        javafx.scene.control.TableCell<com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow, String>
    > createExplainableReorderTextCell(Pos alignment) {
        return col -> new javafx.scene.control.TableCell<>() {
            private final Label label = new Label();
            private final javafx.scene.layout.StackPane wrapper = new javafx.scene.layout.StackPane(label);

            {
                label.setMaxWidth(Double.MAX_VALUE);
                label.setWrapText(false);
                label.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
                wrapper.setPadding(Insets.EMPTY);
                wrapper.setMaxWidth(Double.MAX_VALUE);
                wrapper.prefWidthProperty().bind(javafx.beans.binding.Bindings.max(0.0, widthProperty().subtract(16)));
                javafx.scene.layout.StackPane.setAlignment(label, alignment);
                setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
                setAlignment(alignment);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setTooltip(null);
                    return;
                }
                label.setText(item);
                setTooltip(item.isBlank() ? null : new javafx.scene.control.Tooltip(item));
                setGraphic(wrapper);
            }
        };
    }

    private VBox createInsightDigestCard(
        javafx.scene.Node leadingIcon,
        String typeLabel,
        String title,
        String detail,
        com.pbl3.project.pbl3_project.dto.report.InsightSeverity severity,
        javafx.scene.Node secondaryChip,
        String actionLabel,
        Runnable action,
        boolean iconOnlyAction
    ) {
        com.pbl3.project.pbl3_project.dto.report.InsightSeverity safeSeverity =
            severity != null ? severity : com.pbl3.project.pbl3_project.dto.report.InsightSeverity.INFO;
        VBox card = new VBox();
        card.getStyleClass().addAll("insight-card", "insight-digest-card");
        card.setFillWidth(true);
        card.setMaxWidth(Double.MAX_VALUE);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().addAll("insight-card-title", "insight-digest-title");
        titleLabel.setWrapText(true);

        javafx.scene.Node severityBadge = createInsightSeverityBadgeIcon(safeSeverity);
        Button actionButton = null;
        if (actionLabel != null && !actionLabel.isBlank() && action != null) {
            actionButton = iconOnlyAction
                ? createInsightIconActionButton(actionLabel, action)
                : createInsightTextActionButton(actionLabel, action);
        }
        javafx.scene.layout.HBox trailingActions = new javafx.scene.layout.HBox(6);
        trailingActions.getStyleClass().add(iconOnlyAction ? "insight-digest-trailing-icons" : "insight-digest-trailing-actions");
        trailingActions.setAlignment(Pos.CENTER_RIGHT);
        trailingActions.getChildren().add(severityBadge);
        if (actionButton != null) {
            trailingActions.getChildren().add(actionButton);
        }

        javafx.scene.layout.HBox metaRow = new javafx.scene.layout.HBox(8);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        metaRow.setMaxWidth(Double.MAX_VALUE);
        if (typeLabel != null && !typeLabel.isBlank()) {
            metaRow.getChildren().add(createInsightMetaChip(typeLabel, false));
        }
        if (secondaryChip != null) {
            metaRow.getChildren().add(secondaryChip);
        }
        javafx.scene.layout.Region metaSpacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(metaSpacer, javafx.scene.layout.Priority.ALWAYS);
        metaRow.getChildren().addAll(metaSpacer, trailingActions);

        Label detailLabel = new Label(detail);
        detailLabel.getStyleClass().addAll("insight-card-detail", "insight-digest-detail");
        detailLabel.setWrapText(false);
        detailLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        detailLabel.setText(createInsightPreviewText(detail, 116));
        if (detail != null && !detail.isBlank()) {
            installTooltip(detailLabel, detail);
        }

        VBox body = new VBox(8, metaRow, titleLabel, detailLabel);
        body.getStyleClass().add("insight-digest-body");
        if (leadingIcon != null) {
            body.getStyleClass().add("insight-digest-body-leading");
        }
        body.setFillWidth(true);
        body.setMinWidth(0);
        body.setMaxWidth(Double.MAX_VALUE);

        javafx.scene.layout.StackPane row = new javafx.scene.layout.StackPane();
        row.setAlignment(Pos.TOP_LEFT);
        row.setMinWidth(0);
        row.setMaxWidth(Double.MAX_VALUE);
        applyRoundedRegionClip(row, 16);
        javafx.scene.layout.StackPane.setAlignment(body, Pos.TOP_LEFT);
        row.getChildren().add(body);
        if (leadingIcon != null) {
            javafx.scene.layout.StackPane iconWrap = wrapInsightDigestIcon(leadingIcon, safeSeverity);
            javafx.scene.layout.StackPane.setAlignment(iconWrap, Pos.TOP_LEFT);
            javafx.scene.layout.StackPane.setMargin(iconWrap, new Insets(2, 0, 0, 2));
            row.getChildren().add(iconWrap);
        }

        card.getChildren().add(row);
        return card;
    }

    private Button createInsightTextActionButton(String actionLabel, Runnable action) {
        Button actionButton = new Button(actionLabel);
        actionButton.getStyleClass().addAll(
            "button",
            "dashboard-report-secondary-button",
            "insight-card-action-button",
            "insight-digest-action-button"
        );
        actionButton.setOnAction(e -> action.run());
        return actionButton;
    }

    private Button createInsightIconActionButton(String actionLabel, Runnable action) {
        Button actionButton = new Button();
        actionButton.getStyleClass().add("insight-icon-action-button");
        actionButton.setGraphic(createInsightActionIcon(actionLabel));
        actionButton.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        actionButton.setFocusTraversable(false);
        actionButton.setOnAction(e -> action.run());
        installTooltip(actionButton, actionLabel);
        return actionButton;
    }

    private javafx.scene.Node createInsightActionIcon(String actionLabel) {
        String normalized = actionLabel != null ? actionLabel.trim().toLowerCase(java.util.Locale.ROOT) : "";
        if ("import".equals(normalized) || normalized.contains("import")) {
            return createImportActionIcon();
        }
        return createInsightOpenActionIcon();
    }

    private Button createImportActionButton(Runnable action, boolean disabled) {
        Button actionButton = new Button();
        actionButton.getStyleClass().add("insight-icon-action-button");
        actionButton.setGraphic(createImportActionIcon());
        actionButton.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        actionButton.setFocusTraversable(false);
        actionButton.setDisable(disabled);
        actionButton.setOnAction(e -> {
            if (action != null) {
                action.run();
            }
        });
        installTooltip(actionButton, "Open Import");
        return actionButton;
    }

    private javafx.scene.Node createInsightOpenActionIcon() {
        javafx.scene.shape.SVGPath elbow = new javafx.scene.shape.SVGPath();
        elbow.setContent("M7 7h10v10");
        elbow.getStyleClass().add("insight-open-action-icon-stroke");
        elbow.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.shape.SVGPath arrow = new javafx.scene.shape.SVGPath();
        arrow.setContent("M7 17 17 7");
        arrow.getStyleClass().add("insight-open-action-icon-stroke");
        arrow.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.Group iconGroup = new javafx.scene.Group(elbow, arrow);
        javafx.scene.layout.StackPane iconPane = new javafx.scene.layout.StackPane(iconGroup);
        iconPane.setAlignment(Pos.CENTER);
        iconPane.setMinSize(18, 18);
        iconPane.setPrefSize(18, 18);
        iconPane.setMaxSize(18, 18);
        return iconPane;
    }

    private javafx.scene.Node createImportActionIcon() {
        java.util.function.Function<String, javafx.scene.shape.SVGPath> pathFactory = content -> {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(content);
            path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            path.setSmooth(true);
            path.getStyleClass().add("insight-open-action-icon-stroke");
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            return path;
        };

        javafx.scene.shape.Circle rearWheel = new javafx.scene.shape.Circle(7, 18, 2);
        rearWheel.getStyleClass().add("insight-open-action-icon-stroke");
        rearWheel.setFill(javafx.scene.paint.Color.TRANSPARENT);
        rearWheel.setSmooth(true);

        javafx.scene.shape.Circle frontWheel = new javafx.scene.shape.Circle(17, 18, 2);
        frontWheel.getStyleClass().add("insight-open-action-icon-stroke");
        frontWheel.setFill(javafx.scene.paint.Color.TRANSPARENT);
        frontWheel.setSmooth(true);

        javafx.scene.layout.Pane truckIcon = new javafx.scene.layout.Pane(
            pathFactory.apply("M14 18V6a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v11a1 1 0 0 0 1 1h2"),
            pathFactory.apply("M15 18H9"),
            pathFactory.apply("M19 18h2a1 1 0 0 0 1-1v-3.65a1 1 0 0 0-.22-.624l-3.48-4.35A1 1 0 0 0 17.52 8H14"),
            rearWheel,
            frontWheel
        );
        truckIcon.setMinSize(24, 24);
        truckIcon.setPrefSize(24, 24);
        truckIcon.setMaxSize(24, 24);
        truckIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconPane = new javafx.scene.layout.StackPane(truckIcon);
        iconPane.setAlignment(Pos.CENTER);
        iconPane.setMinSize(20, 20);
        iconPane.setPrefSize(20, 20);
        iconPane.setMaxSize(20, 20);
        iconPane.setScaleX(0.90);
        iconPane.setScaleY(0.90);
        return iconPane;
    }

    private void applyRoundedRegionClip(javafx.scene.layout.Region region, double radius) {
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.setArcWidth(radius * 2);
        clip.setArcHeight(radius * 2);
        clip.widthProperty().bind(region.widthProperty());
        clip.heightProperty().bind(region.heightProperty());
        region.setClip(clip);
    }

    private Label createInsightMetaChip(String text, boolean emphasized) {
        Label chip = new Label(text);
        chip.getStyleClass().add(emphasized ? "insight-meta-chip-strong" : "insight-meta-chip");
        return chip;
    }

    private javafx.scene.Node createInsightSeverityBadgeIcon(
        com.pbl3.project.pbl3_project.dto.report.InsightSeverity severity
    ) {
        javafx.scene.Node icon = createInsightSeverityGroupIcon(severity);
        javafx.scene.layout.StackPane wrap = new javafx.scene.layout.StackPane(icon);
        wrap.getStyleClass().add("insight-severity-badge-icon");
        wrap.setMinSize(26, 26);
        wrap.setPrefSize(26, 26);
        wrap.setMaxSize(26, 26);
        return wrap;
    }

    private javafx.scene.layout.StackPane wrapInsightDigestIcon(
        javafx.scene.Node icon,
        com.pbl3.project.pbl3_project.dto.report.InsightSeverity severity
    ) {
        javafx.scene.layout.StackPane wrap = new javafx.scene.layout.StackPane(icon);
        wrap.getStyleClass().add("insight-digest-icon-wrap");
        if (severity != null) {
            wrap.getStyleClass().add("insight-digest-icon-wrap-" + severity.name().toLowerCase(java.util.Locale.ROOT));
        }
        wrap.setMinSize(38, 38);
        wrap.setPrefSize(38, 38);
        wrap.setMaxSize(38, 38);
        return wrap;
    }

    private javafx.scene.Node createCompactInsightPlaceholder(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("dashboard-placeholder-label");
        label.setStyle("-fx-font-size: 13px; -fx-text-fill: -app-text-muted;");
        javafx.scene.layout.StackPane wrapper = new javafx.scene.layout.StackPane(label);
        wrapper.setAlignment(Pos.CENTER_LEFT);
        wrapper.setMinHeight(72);
        wrapper.setPrefHeight(72);
        wrapper.getStyleClass().add("insight-placeholder");
        return wrapper;
    }

    private String formatInsightSeverity(com.pbl3.project.pbl3_project.dto.report.InsightSeverity severity) {
        if (severity == null) {
            return "Info";
        }
        return switch (severity) {
            case CRITICAL -> "Critical";
            case WARNING -> "Warning";
            case INFO -> "Info";
        };
    }

    private String formatWhatChangedType(com.pbl3.project.pbl3_project.dto.report.WhatChangedType type) {
        if (type == null) {
            return "Insight";
        }
        return switch (type) {
            case REVENUE_CHANGE -> "Revenue";
            case ORDER_COUNT_CHANGE -> "Orders";
            case AVERAGE_ORDER_VALUE_CHANGE -> "AOV";
            case CANCEL_RATE_CHANGE -> "Cancel Rate";
            case TOP_DRIVER_PRODUCT -> "Driver Product";
        };
    }

    private javafx.scene.Node extractWhatChangedDeltaChip(com.pbl3.project.pbl3_project.dto.report.WhatChangedInsight insight) {
        if (insight == null || insight.headline() == null || insight.headline().isBlank()) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
            .compile("\\b(up|down)\\s+([0-9]+(?:\\.[0-9]+)?%?)\\b", java.util.regex.Pattern.CASE_INSENSITIVE)
            .matcher(insight.headline());
        if (matcher.find()) {
            return createInsightDeltaChip(
                matcher.group(1).toLowerCase(java.util.Locale.ROOT),
                matcher.group(2)
            );
        }
        return insight.type() == com.pbl3.project.pbl3_project.dto.report.WhatChangedType.TOP_DRIVER_PRODUCT
            ? createInsightMetaChip("Driver", false)
            : null;
    }

    private javafx.scene.Node createActionCenterImpactChip(
        com.pbl3.project.pbl3_project.dto.report.ActionCenterItem item
    ) {
        if (item == null || item.impactLabel() == null || item.impactLabel().isBlank()) {
            return null;
        }
        return createInsightMetaChip(createInsightPreviewText(item.impactLabel(), 28), true);
    }

    private javafx.scene.Node createInsightDeltaChip(String direction, String value) {
        String safeDirection = "up".equals(direction) ? "up" : "down";
        Label arrowLabel = new Label("up".equals(safeDirection) ? "\u2191" : "\u2193");
        arrowLabel.getStyleClass().addAll("insight-delta-arrow", "insight-delta-arrow-" + safeDirection);

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().addAll("insight-delta-label", "insight-delta-label-" + safeDirection);

        javafx.scene.layout.HBox chip = new javafx.scene.layout.HBox(4, arrowLabel, valueLabel);
        chip.getStyleClass().addAll("insight-delta-chip", "insight-delta-chip-" + safeDirection);
        chip.setAlignment(Pos.CENTER_LEFT);
        return chip;
    }

    private javafx.scene.Node createInsightSeverityGroup(
        com.pbl3.project.pbl3_project.dto.report.InsightSeverity severity,
        int count,
        VBox groupContent,
        boolean collapsible,
        boolean collapsedInitially
    ) {
        com.pbl3.project.pbl3_project.dto.report.InsightSeverity safeSeverity =
            severity != null ? severity : com.pbl3.project.pbl3_project.dto.report.InsightSeverity.INFO;
        String severityTone = safeSeverity.name().toLowerCase(java.util.Locale.ROOT);

        javafx.scene.Node severityIcon = createInsightSeverityGroupIcon(safeSeverity);

        Label titleLabel = new Label(formatInsightSeverity(safeSeverity));
        titleLabel.getStyleClass().add("insight-severity-group-label");

        Label countLabel = new Label(count + (count == 1 ? " item" : " items"));
        countLabel.getStyleClass().addAll("insight-severity-group-count", "insight-severity-group-count-" + severityTone);

        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(8);
        header.getStyleClass().addAll("insight-severity-group", collapsible ? "insight-severity-group-toggle" : "insight-severity-group-static");
        header.setAlignment(Pos.CENTER_LEFT);

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.layout.HBox trailingWrap = new javafx.scene.layout.HBox(8);
        trailingWrap.getStyleClass().add("insight-severity-group-trailing");
        trailingWrap.setAlignment(Pos.CENTER_RIGHT);
        trailingWrap.getChildren().add(countLabel);
        header.getChildren().addAll(severityIcon, titleLabel, spacer, trailingWrap);

        final boolean[] expanded = { !collapsedInitially };
        javafx.scene.shape.SVGPath chevron = collapsible
            ? createInsightSeverityChevron(expanded[0])
            : null;
        if (collapsible) {
            header.setCursor(javafx.scene.Cursor.HAND);
            header.setFocusTraversable(true);
            trailingWrap.getChildren().add(wrapInsightSeverityChevron(chevron));
            header.setOnMouseClicked(event -> {
                if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                    expanded[0] = !expanded[0];
                    applyInsightSeverityExpandedState(header, groupContent, chevron, expanded[0]);
                }
            });
            header.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ENTER || event.getCode() == javafx.scene.input.KeyCode.SPACE) {
                    expanded[0] = !expanded[0];
                    applyInsightSeverityExpandedState(header, groupContent, chevron, expanded[0]);
                    event.consume();
                }
            });
        }

        VBox wrapper = new VBox(8);
        wrapper.getStyleClass().add("insight-severity-group-wrapper");
        wrapper.setFillWidth(true);
        wrapper.getChildren().addAll(header, groupContent);

        applyInsightSeverityExpandedState(header, groupContent, chevron, expanded[0]);
        return wrapper;
    }

    private javafx.scene.Node createInsightSeverityGroupIcon(
        com.pbl3.project.pbl3_project.dto.report.InsightSeverity severity
    ) {
        com.pbl3.project.pbl3_project.dto.report.InsightSeverity safeSeverity =
            severity != null ? severity : com.pbl3.project.pbl3_project.dto.report.InsightSeverity.INFO;
        return switch (safeSeverity) {
            case CRITICAL -> createCriticalInsightSeverityIcon();
            case WARNING -> createWarningInsightSeverityIcon();
            case INFO -> createInfoInsightSeverityIcon();
        };
    }

    private javafx.scene.Node createCriticalInsightSeverityIcon() {
        javafx.scene.shape.SVGPath outline = new javafx.scene.shape.SVGPath();
        outline.setContent("M8.27 3h7.46L21 8.27v7.46L15.73 21H8.27L3 15.73V8.27Z");
        outline.getStyleClass().add("insight-severity-icon-stroke-critical");
        outline.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.shape.SVGPath mark = new javafx.scene.shape.SVGPath();
        mark.setContent("M12 8v4");
        mark.getStyleClass().add("insight-severity-icon-stroke-critical");
        mark.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(12, 16.3, 0.85);
        dot.getStyleClass().add("insight-severity-icon-fill-critical");

        javafx.scene.layout.Pane iconPane = new javafx.scene.layout.Pane(outline, mark, dot);
        iconPane.getStyleClass().add("insight-severity-group-icon-wrap");
        iconPane.setMinSize(24, 24);
        iconPane.setPrefSize(24, 24);
        iconPane.setMaxSize(24, 24);
        return iconPane;
    }

    private javafx.scene.Node createWarningInsightSeverityIcon() {
        javafx.scene.shape.SVGPath outline = new javafx.scene.shape.SVGPath();
        outline.setContent("M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z");
        outline.getStyleClass().add("insight-severity-icon-stroke-warning");
        outline.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.shape.SVGPath mark = new javafx.scene.shape.SVGPath();
        mark.setContent("M12 9v4");
        mark.getStyleClass().add("insight-severity-icon-stroke-warning");
        mark.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(12, 16.7, 0.85);
        dot.getStyleClass().add("insight-severity-icon-fill-warning");

        javafx.scene.layout.Pane iconPane = new javafx.scene.layout.Pane(outline, mark, dot);
        iconPane.getStyleClass().add("insight-severity-group-icon-wrap");
        iconPane.setMinSize(24, 24);
        iconPane.setPrefSize(24, 24);
        iconPane.setMaxSize(24, 24);
        return iconPane;
    }

    private javafx.scene.Node createInfoInsightSeverityIcon() {
        javafx.scene.shape.SVGPath outline = new javafx.scene.shape.SVGPath();
        outline.setContent("M12 2 A10 10 0 1 1 12 22 A10 10 0 1 1 12 2");
        outline.getStyleClass().add("insight-severity-icon-stroke-info");
        outline.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.shape.SVGPath mark = new javafx.scene.shape.SVGPath();
        mark.setContent("M12 10.4v5.1");
        mark.getStyleClass().add("insight-severity-icon-stroke-info");
        mark.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.shape.SVGPath dot = new javafx.scene.shape.SVGPath();
        dot.setContent("M12 7.1 L12 7.1");
        dot.getStyleClass().add("insight-severity-icon-dot-info");
        dot.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.scene.layout.Pane iconPane = new javafx.scene.layout.Pane(outline, mark, dot);
        iconPane.getStyleClass().add("insight-severity-group-icon-wrap");
        iconPane.setMinSize(24, 24);
        iconPane.setPrefSize(24, 24);
        iconPane.setMaxSize(24, 24);
        return iconPane;
    }

    private javafx.scene.shape.SVGPath createInsightSeverityChevron(boolean expanded) {
        javafx.scene.shape.SVGPath chevron = new javafx.scene.shape.SVGPath();
        chevron.setContent("M8 6 L14 12 L8 18");
        chevron.getStyleClass().add("insight-severity-group-chevron");
        chevron.setStrokeWidth(2.0);
        chevron.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        chevron.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        chevron.setFill(javafx.scene.paint.Color.TRANSPARENT);
        chevron.setRotate(expanded ? 90.0 : 0.0);
        return chevron;
    }

    private javafx.scene.layout.StackPane wrapInsightSeverityChevron(javafx.scene.shape.SVGPath chevron) {
        javafx.scene.layout.StackPane wrap = new javafx.scene.layout.StackPane(chevron);
        wrap.setMinSize(18, 18);
        wrap.setPrefSize(18, 18);
        wrap.setMaxSize(18, 18);
        return wrap;
    }

    private void applyInsightSeverityExpandedState(
        javafx.scene.layout.HBox header,
        javafx.scene.Node content,
        javafx.scene.shape.SVGPath chevron,
        boolean expanded
    ) {
        if (content != null) {
            content.setManaged(expanded);
            content.setVisible(expanded);
        }
        if (header != null) {
            if (expanded) {
                if (!header.getStyleClass().contains("expanded")) {
                    header.getStyleClass().add("expanded");
                }
            } else {
                header.getStyleClass().remove("expanded");
            }
        }
        if (chevron != null) {
            chevron.setRotate(expanded ? 90.0 : 0.0);
        }
    }

    private String formatActionCenterType(com.pbl3.project.pbl3_project.dto.report.ActionCenterType type) {
        if (type == null) {
            return "Action";
        }
        return switch (type) {
            case REORDER_NOW -> "Reorder Now";
            case LOW_COVERAGE -> "Low Coverage";
            case AGED_STOCK -> "Aged Stock";
            case REVENUE_DROP -> "Revenue Drop";
            case CANCEL_SPIKE -> "Cancel Spike";
        };
    }

    private String formatInsightActionLabel(String actionLabel) {
        if (actionLabel == null || actionLabel.isBlank()) {
            return null;
        }
        String normalized = actionLabel.trim();
        if ("Open Import Goods".equalsIgnoreCase(normalized)) {
            return "Import";
        }
        if ("Review Reorder".equalsIgnoreCase(normalized)) {
            return "Review";
        }
        return normalized;
    }

    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        if (text.length() == 1) {
            return text.toUpperCase(java.util.Locale.ROOT);
        }
        return text.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + text.substring(1);
    }

    private String createInsightPreviewText(String detail, int maxLength) {
        if (detail == null) {
            return "";
        }
        String normalized = detail.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        if (maxLength <= 1) {
            return normalized.substring(0, 1);
        }
        return normalized.substring(0, maxLength - 1).trim() + "…";
    }

    private javafx.scene.layout.GridPane createResponsiveInsightGrid(
        java.util.List<VBox> cards,
        javafx.beans.value.ObservableNumberValue widthSource,
        double twoColumnBreakpoint
    ) {
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        grid.setMaxWidth(Double.MAX_VALUE);
        if (widthSource != null) {
            grid.prefWidthProperty().bind(widthSource);
        }

        cards.forEach(card -> {
            card.setMinWidth(0);
            card.setMaxWidth(Double.MAX_VALUE);
            javafx.scene.layout.GridPane.setHgrow(card, javafx.scene.layout.Priority.ALWAYS);
            javafx.scene.layout.GridPane.setFillWidth(card, true);
        });

        final boolean[] twoColumnMode = {false};
        Runnable syncLayout = () -> {
            boolean shouldUseTwoColumns = widthSource != null
                && widthSource.doubleValue() >= twoColumnBreakpoint
                && cards.size() > 1;
            if (!grid.getChildren().isEmpty() && twoColumnMode[0] == shouldUseTwoColumns) {
                return;
            }
            twoColumnMode[0] = shouldUseTwoColumns;
            grid.getChildren().clear();
            grid.getColumnConstraints().clear();
            grid.getRowConstraints().clear();

            if (shouldUseTwoColumns) {
                for (int i = 0; i < 2; i++) {
                    javafx.scene.layout.ColumnConstraints column = new javafx.scene.layout.ColumnConstraints();
                    column.setPercentWidth(50);
                    column.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                    column.setFillWidth(true);
                    grid.getColumnConstraints().add(column);
                }
                for (int i = 0; i < cards.size(); i++) {
                    grid.add(cards.get(i), i % 2, i / 2);
                }
            } else {
                javafx.scene.layout.ColumnConstraints column = new javafx.scene.layout.ColumnConstraints();
                column.setPercentWidth(100);
                column.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                column.setFillWidth(true);
                grid.getColumnConstraints().add(column);
                for (int i = 0; i < cards.size(); i++) {
                    grid.add(cards.get(i), 0, i);
                }
            }
        };

        syncLayout.run();
        if (widthSource != null) {
            widthSource.addListener((obs, oldValue, newValue) -> syncLayout.run());
        }
        return grid;
    }

    private String formatCompactDecimal(BigDecimal value) {
        if (value == null) {
            return "-";
        }
        BigDecimal normalized = MoneySupport.normalize(value).stripTrailingZeros();
        return normalized.scale() < 0 ? normalized.setScale(0).toPlainString() : normalized.toPlainString();
    }

    private java.util.List<java.util.List<String>> buildRevenueExportRows(java.util.Map<String, BigDecimal> revenueSeries) {
        if (revenueSeries == null || revenueSeries.isEmpty()) {
            return java.util.List.of();
        }
        return revenueSeries.entrySet().stream()
            .map(entry -> java.util.List.<String>of(entry.getKey(), context.support().formatVnd(entry.getValue())))
            .toList();
    }

    private java.util.List<String> shiftExportHeaders() {
        return java.util.List.of(
            "Shift",
            "Employee",
            "Opened",
            "Closed",
            "Status",
            "Orders",
            "Sales",
            "Refunds",
            "Expenses",
            "Expected Cash",
            "Actual Cash",
            "Variance"
        );
    }

    private java.util.List<java.util.List<String>> buildShiftExportRows(
        java.util.List<SalesShiftService.ShiftReportRow> rows
    ) {
        if (rows == null || rows.isEmpty()) {
            return java.util.List.of();
        }
        return rows.stream()
            .map(row -> java.util.List.<String>of(
                "#" + row.shiftId(),
                formatShiftEmployee(row),
                formatShiftDateTime(row.openedAt()),
                formatShiftDateTime(row.closedAt()),
                formatShiftStatus(row.status()),
                String.valueOf(row.orderCount()),
                context.support().formatVnd(row.salesRevenue()),
                context.support().formatVnd(row.refundAmount()),
                context.support().formatVnd(row.expenseAmount()),
                context.support().formatVnd(row.expectedCashAmount()),
                row.closingCashActual() != null ? context.support().formatVnd(row.closingCashActual()) : "",
                formatSignedVnd(row.cashVarianceAmount())
            ))
            .toList();
    }

    private java.util.List<java.util.List<String>> buildShiftExportMetadata(
        java.time.LocalDate startDate,
        java.time.LocalDate endDate,
        javafx.scene.control.ComboBox<IdLabelOption> userFilter,
        javafx.scene.control.ComboBox<String> statusFilter,
        TextField shiftIdFilter
    ) {
        IdLabelOption selectedUser = userFilter.getSelectionModel().getSelectedItem();
        String shiftId = shiftIdFilter.getText() == null || shiftIdFilter.getText().isBlank()
            ? "All"
            : shiftIdFilter.getText().trim();
        return java.util.List.of(
            java.util.List.of("Range", formatOperationalReportRangeLabel(startDate, endDate)),
            java.util.List.of("Employee", selectedUser != null && selectedUser.label() != null ? selectedUser.label() : "All"),
            java.util.List.of("Status", statusFilter.getValue() != null ? statusFilter.getValue() : "All statuses"),
            java.util.List.of("Shift ID", shiftId)
        );
    }

    private void exportCsv(Stage owner, String defaultFileName, java.util.List<String> headers, java.util.List<java.util.List<String>> rows) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Export CSV");
        fileChooser.setInitialFileName(defaultFileName);
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        java.io.File file = fileChooser.showSaveDialog(owner);
        if (file == null) {
            return;
        }

        try (java.io.BufferedWriter writer = java.nio.file.Files.newBufferedWriter(
            file.toPath(),
            java.nio.charset.StandardCharsets.UTF_8,
            java.nio.file.StandardOpenOption.CREATE,
            java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
            java.nio.file.StandardOpenOption.WRITE
        )) {
            writer.write('\uFEFF');
            writer.write(toCsvLine(headers));
            for (java.util.List<String> row : rows) {
                writer.newLine();
                writer.write(toCsvLine(row));
            }
            context.toastService().showSuccess(java.text.MessageFormat.format("Exported {0}", file.getName()));
        } catch (Exception ex) {
            context.toastService().showError(java.text.MessageFormat.format("Export failed: {0}", ex.getMessage()));
        }
    }

    private void exportPdf(
        Stage owner,
        String title,
        String defaultFileName,
        java.util.List<java.util.List<String>> metadataRows,
        java.util.List<String> headers,
        java.util.List<java.util.List<String>> rows
    ) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Export PDF");
        fileChooser.setInitialFileName(defaultFileName);
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        java.io.File file = fileChooser.showSaveDialog(owner);
        if (file == null) {
            return;
        }

        try (java.io.FileOutputStream outputStream = new java.io.FileOutputStream(file)) {
            boolean landscape = headers != null && headers.size() > 6;
            com.itextpdf.text.Document document = new com.itextpdf.text.Document(
                landscape ? com.itextpdf.text.PageSize.A4.rotate() : com.itextpdf.text.PageSize.A4,
                28,
                28,
                28,
                28
            );
            com.itextpdf.text.pdf.PdfWriter.getInstance(document, outputStream);
            document.open();

            ReportPdfFonts fonts = createReportPdfFonts();
            com.itextpdf.text.Paragraph titleParagraph = new com.itextpdf.text.Paragraph(title, fonts.title());
            titleParagraph.setSpacingAfter(8);
            document.add(titleParagraph);

            com.itextpdf.text.Paragraph generatedParagraph = new com.itextpdf.text.Paragraph(
                "Generated at " + context.support().formatDateTime(java.time.LocalDateTime.now()),
                fonts.meta()
            );
            generatedParagraph.setSpacingAfter(12);
            document.add(generatedParagraph);

            if (metadataRows != null && !metadataRows.isEmpty()) {
                com.itextpdf.text.pdf.PdfPTable metadataTable = new com.itextpdf.text.pdf.PdfPTable(2);
                metadataTable.setWidthPercentage(100);
                metadataTable.setSpacingAfter(12);
                for (java.util.List<String> row : metadataRows) {
                    addReportPdfCell(metadataTable, valueAt(row, 0), fonts.metaBold(), false);
                    addReportPdfCell(metadataTable, valueAt(row, 1), fonts.meta(), false);
                }
                document.add(metadataTable);
            }

            com.itextpdf.text.pdf.PdfPTable table = new com.itextpdf.text.pdf.PdfPTable(headers == null || headers.isEmpty() ? 1 : headers.size());
            table.setWidthPercentage(100);
            if (headers != null && !headers.isEmpty()) {
                for (String header : headers) {
                    addReportPdfCell(table, header, fonts.header(), true);
                }
            }
            if (rows != null) {
                for (java.util.List<String> row : rows) {
                    for (int i = 0; i < (headers == null || headers.isEmpty() ? row.size() : headers.size()); i++) {
                        addReportPdfCell(table, valueAt(row, i), fonts.body(), false);
                    }
                }
            }
            document.add(table);
            document.close();
            context.toastService().showSuccess(java.text.MessageFormat.format("Exported {0}", file.getName()));
        } catch (Exception ex) {
            context.toastService().showError(java.text.MessageFormat.format("PDF export failed: {0}", ex.getMessage()));
        }
    }

    private void addReportPdfCell(
        com.itextpdf.text.pdf.PdfPTable table,
        String text,
        com.itextpdf.text.Font font,
        boolean header
    ) {
        com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(text == null ? "" : text, font));
        cell.setPadding(4);
        cell.setBorderColor(new com.itextpdf.text.BaseColor(207, 216, 220));
        if (header) {
            cell.setBackgroundColor(new com.itextpdf.text.BaseColor(232, 238, 242));
        }
        table.addCell(cell);
    }

    private ReportPdfFonts createReportPdfFonts() throws Exception {
        com.itextpdf.text.pdf.BaseFont regular = createReportBaseFont("/fonts/BeVietnamPro-Regular.ttf");
        com.itextpdf.text.pdf.BaseFont bold = createReportBaseFont("/fonts/BeVietnamPro-Bold.ttf");
        return new ReportPdfFonts(
            new com.itextpdf.text.Font(bold, 16),
            new com.itextpdf.text.Font(bold, 8),
            new com.itextpdf.text.Font(regular, 8),
            new com.itextpdf.text.Font(bold, 9),
            new com.itextpdf.text.Font(regular, 9)
        );
    }

    private com.itextpdf.text.pdf.BaseFont createReportBaseFont(String resourcePath) throws Exception {
        java.net.URL fontUrl = getClass().getResource(resourcePath);
        if (fontUrl != null) {
            return com.itextpdf.text.pdf.BaseFont.createFont(
                fontUrl.toExternalForm(),
                com.itextpdf.text.pdf.BaseFont.IDENTITY_H,
                com.itextpdf.text.pdf.BaseFont.EMBEDDED
            );
        }
        return com.itextpdf.text.pdf.BaseFont.createFont(
            com.itextpdf.text.pdf.BaseFont.HELVETICA,
            com.itextpdf.text.pdf.BaseFont.WINANSI,
            com.itextpdf.text.pdf.BaseFont.NOT_EMBEDDED
        );
    }

    private String valueAt(java.util.List<String> row, int index) {
        if (row == null || index < 0 || index >= row.size()) {
            return "";
        }
        return row.get(index);
    }

    private String toCsvLine(java.util.List<String> values) {
        return values.stream()
            .map(this::escapeCsvCell)
            .collect(java.util.stream.Collectors.joining(","));
    }

    private String escapeCsvCell(String value) {
        String safeValue = value != null ? value : "";
        String escapedValue = safeValue.replace("\"", "\"\"");
        boolean requiresQuotes = escapedValue.contains(",")
            || escapedValue.contains("\"")
            || escapedValue.contains("\n")
            || escapedValue.contains("\r");
        return requiresQuotes ? "\"" + escapedValue + "\"" : escapedValue;
    }

    private String buildReportCsvFileName(String baseName, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return baseName + "-" + buildReportRangeFileSuffix(startDate, endDate) + ".csv";
    }

    private String buildReportPdfFileName(String baseName, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return baseName + "-" + buildReportRangeFileSuffix(startDate, endDate) + ".pdf";
    }

    private String buildReportRangeFileSuffix(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return "all-time";
        }
        if (startDate != null && endDate != null) {
            if (startDate.equals(endDate)) {
                return startDate.format(FILE_DATE_FORMATTER);
            }
            return startDate.format(FILE_DATE_FORMATTER) + "-to-" + endDate.format(FILE_DATE_FORMATTER);
        }
        if (startDate != null) {
            return "from-" + startDate.format(FILE_DATE_FORMATTER);
        }
        return "until-" + endDate.format(FILE_DATE_FORMATTER);
    }






    private String formatOperationalReportRangeLabel(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return "All Time";
        }
        if (startDate != null && endDate != null) {
            return context.support().formatDate(startDate) + " - " + context.support().formatDate(endDate);
        }
        if (startDate != null) {
            return "From " + context.support().formatDate(startDate);
        }
        return "Until " + context.support().formatDate(endDate);
    }


    private String buildNoSalesRangeText(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return "No sales in all time";
        }
        if (startDate != null && endDate != null) {
            if (startDate.equals(endDate)) {
                return "No sales on " + context.support().formatDate(startDate);
            }
            return "No sales from " + context.support().formatDate(startDate) + " to " + context.support().formatDate(endDate);
        }
        if (startDate != null) {
            return "No sales from " + context.support().formatDate(startDate);
        }
        return "No sales until " + context.support().formatDate(endDate);
    }
}
