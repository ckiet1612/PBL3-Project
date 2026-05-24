package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.dto.report.ActionCenterItem;
import com.pbl3.project.pbl3_project.dto.report.ActionCenterSnapshot;
import com.pbl3.project.pbl3_project.dto.report.ActionCenterType;
import com.pbl3.project.pbl3_project.dto.report.ExplainableReorderRow;
import com.pbl3.project.pbl3_project.dto.report.ExplainableReorderSnapshot;
import com.pbl3.project.pbl3_project.dto.report.InsightDrilldownTarget;
import com.pbl3.project.pbl3_project.dto.report.InsightSeverity;
import com.pbl3.project.pbl3_project.dto.report.OperationalInsightBundle;
import com.pbl3.project.pbl3_project.dto.report.WhatChangedInsight;
import com.pbl3.project.pbl3_project.dto.report.WhatChangedSnapshot;
import com.pbl3.project.pbl3_project.dto.report.WhatChangedType;
import com.pbl3.project.pbl3_project.entity.ImportOrderItem;
import com.pbl3.project.pbl3_project.entity.ImportOrderStatus;
import com.pbl3.project.pbl3_project.entity.InventoryTransaction;
import com.pbl3.project.pbl3_project.entity.Order;
import com.pbl3.project.pbl3_project.entity.OrderItem;
import com.pbl3.project.pbl3_project.entity.OrderStatus;
import com.pbl3.project.pbl3_project.entity.Product;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.repository.ImportOrderItemRepository;
import com.pbl3.project.pbl3_project.repository.InventoryTransactionRepository;
import com.pbl3.project.pbl3_project.repository.OrderItemRepository;
import com.pbl3.project.pbl3_project.repository.OrderRepository;
import com.pbl3.project.pbl3_project.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class OperationalInsightService {
    private static final int DEMAND_WINDOW_DAYS = 14;
    private static final int TARGET_COVERAGE_DAYS = 14;
    private static final int REORDER_THRESHOLD_DAYS = 7;
    private static final int AGED_STOCK_THRESHOLD_DAYS = 30;
    private static final BigDecimal MIN_MEANINGFUL_CHANGE_PERCENT = new BigDecimal("5.00");
    private static final BigDecimal REVENUE_DROP_ACTION_PERCENT = new BigDecimal("20.00");
    private static final BigDecimal REVENUE_DROP_CRITICAL_PERCENT = new BigDecimal("35.00");
    private static final BigDecimal CANCEL_RATE_SPIKE_PERCENTAGE_POINTS = new BigDecimal("5.00");
    private static final BigDecimal CANCEL_RATE_CRITICAL_PERCENTAGE_POINTS = new BigDecimal("10.00");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final ImportOrderItemRepository importOrderItemRepository;
    private final AuthorizationService authorizationService;

    public OperationalInsightService(
        OrderRepository orderRepository,
        OrderItemRepository orderItemRepository,
        ProductRepository productRepository,
        InventoryTransactionRepository inventoryTransactionRepository,
        ImportOrderItemRepository importOrderItemRepository,
        AuthorizationService authorizationService
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.importOrderItemRepository = importOrderItemRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public OperationalInsightBundle getDashboardInsights(User viewer) {
        LocalDate today = LocalDate.now();
        InsightRange currentRange = new InsightRange(today, today, "Today");
        InsightRange baselineRange = new InsightRange(today.minusDays(1), today.minusDays(1), "Yesterday");

        LocalDateTime queryStart = baselineRange.start().atStartOfDay();
        LocalDateTime queryEnd = currentRange.end().atTime(LocalTime.MAX);

        WhatChangedComputation whatChanged;
        if (viewer != null && !authorizationService.canViewAllOrders(viewer)) {
            List<Order> orders = orderRepository.findAllWithinDateRangeAndUserId(queryStart, queryEnd, viewer.getId());
            List<OrderItem> orderItems = orderItemRepository.findAllWithOrderAndProductWithinDateRangeAndUserId(queryStart, queryEnd, viewer.getId());
            whatChanged = buildWhatChangedComputation(currentRange, baselineRange, orders, orderItems);
            return new OperationalInsightBundle(whatChanged.snapshot(), null, null);
        }

        List<Order> orders = orderRepository.findAllWithinDateRange(queryStart, queryEnd);
        List<OrderItem> orderItems = orderItemRepository.findAllWithOrderAndProductWithinDateRange(queryStart, queryEnd);
        WhatChangedComputation comparison = buildWhatChangedComputation(currentRange, baselineRange, orders, orderItems);

        InventoryInsightContext inventoryContext = buildInventoryInsightContext();
        ExplainableReorderSnapshot reorderSnapshot = buildExplainableReorderSnapshot(inventoryContext);
        ActionCenterSnapshot actionCenterSnapshot = buildActionCenterSnapshot(comparison, reorderSnapshot, inventoryContext.agingCandidates());

        return new OperationalInsightBundle(comparison.snapshot(), actionCenterSnapshot, reorderSnapshot);
    }

    @Transactional(readOnly = true)
    public OperationalInsightBundle getOperationalReportInsights(LocalDate startDate, LocalDate endDate) {
        InsightRange currentRange = resolveReportCurrentRange(startDate, endDate);
        InsightRange baselineRange = toPreviousAdjacentRange(currentRange);

        LocalDateTime queryStart = baselineRange.start().atStartOfDay();
        LocalDateTime queryEnd = currentRange.end().atTime(LocalTime.MAX);

        List<Order> orders = orderRepository.findAllWithinDateRange(queryStart, queryEnd);
        List<OrderItem> orderItems = orderItemRepository.findAllWithOrderAndProductWithinDateRange(queryStart, queryEnd);
        WhatChangedComputation comparison = buildWhatChangedComputation(currentRange, baselineRange, orders, orderItems);

        InventoryInsightContext inventoryContext = buildInventoryInsightContext();
        ExplainableReorderSnapshot reorderSnapshot = buildExplainableReorderSnapshot(inventoryContext);
        ActionCenterSnapshot actionCenterSnapshot = buildActionCenterSnapshot(comparison, reorderSnapshot, inventoryContext.agingCandidates());

        return new OperationalInsightBundle(comparison.snapshot(), actionCenterSnapshot, reorderSnapshot);
    }

    private InsightRange resolveReportCurrentRange(LocalDate requestedStart, LocalDate requestedEnd) {
        if (requestedStart != null || requestedEnd != null) {
            LocalDate resolvedStart = requestedStart != null ? requestedStart : requestedEnd;
            LocalDate resolvedEnd = requestedEnd != null ? requestedEnd : LocalDate.now();
            if (resolvedEnd.isBefore(resolvedStart)) {
                resolvedEnd = resolvedStart;
            }
            return new InsightRange(resolvedStart, resolvedEnd, formatRangeLabel(resolvedStart, resolvedEnd));
        }

        List<Order> orders = orderRepository.findAllWithinDateRange(null, null);
        LocalDate start = orders.stream()
            .filter(this::isOrderEligible)
            .map(Order::getCreatedAt)
            .filter(Objects::nonNull)
            .map(LocalDateTime::toLocalDate)
            .min(LocalDate::compareTo)
            .orElse(LocalDate.now());
        LocalDate end = LocalDate.now();
        return new InsightRange(start, end, formatRangeLabel(start, end));
    }

    private InsightRange toPreviousAdjacentRange(InsightRange currentRange) {
        long lengthDays = ChronoUnit.DAYS.between(currentRange.start(), currentRange.end()) + 1L;
        LocalDate baselineEnd = currentRange.start().minusDays(1);
        LocalDate baselineStart = baselineEnd.minusDays(Math.max(0L, lengthDays - 1L));
        return new InsightRange(baselineStart, baselineEnd, currentRange.label().equals("Today") ? "Yesterday" : formatRangeLabel(baselineStart, baselineEnd));
    }

    private WhatChangedComputation buildWhatChangedComputation(
        InsightRange currentRange,
        InsightRange baselineRange,
        List<Order> orders,
        List<OrderItem> orderItems
    ) {
        PeriodMetrics currentMetrics = computePeriodMetrics(orders, orderItems, currentRange.start(), currentRange.end());
        PeriodMetrics baselineMetrics = computePeriodMetrics(orders, orderItems, baselineRange.start(), baselineRange.end());

        ProductDelta topDriver = resolveTopDriver(currentMetrics.productRevenueByProduct(), baselineMetrics.productRevenueByProduct());
        List<WhatChangedInsight> insights = new ArrayList<>();

        if (baselineMetrics.totalOrderCount() == 0L && baselineMetrics.revenue().compareTo(MoneySupport.ZERO) == 0) {
            return new WhatChangedComputation(
                new WhatChangedSnapshot(currentRange.label(), baselineRange.label(), List.of()),
                currentMetrics,
                baselineMetrics,
                topDriver,
                currentRange,
                baselineRange
            );
        }

        WhatChangedInsight revenueInsight = buildRevenueInsight(currentRange, baselineRange, currentMetrics, baselineMetrics);
        if (revenueInsight != null) {
            insights.add(revenueInsight);
        }

        WhatChangedInsight orderInsight = buildOrderCountInsight(currentRange, baselineRange, currentMetrics, baselineMetrics);
        if (orderInsight != null) {
            insights.add(orderInsight);
        }

        WhatChangedInsight averageOrderValueInsight = buildAverageOrderValueInsight(currentRange, baselineRange, currentMetrics, baselineMetrics);
        if (averageOrderValueInsight != null) {
            insights.add(averageOrderValueInsight);
        }

        WhatChangedInsight cancelRateInsight = buildCancelRateInsight(currentRange, baselineRange, currentMetrics, baselineMetrics);
        if (cancelRateInsight != null) {
            insights.add(cancelRateInsight);
        }

        WhatChangedInsight topDriverInsight = buildTopDriverInsight(currentRange, baselineRange, topDriver);
        if (topDriverInsight != null) {
            insights.add(topDriverInsight);
        }

        return new WhatChangedComputation(
            new WhatChangedSnapshot(currentRange.label(), baselineRange.label(), insights),
            currentMetrics,
            baselineMetrics,
            topDriver,
            currentRange,
            baselineRange
        );
    }

    private PeriodMetrics computePeriodMetrics(List<Order> orders, List<OrderItem> orderItems, LocalDate startDate, LocalDate endDate) {
        BigDecimal revenue = MoneySupport.ZERO;
        long totalOrderCount = 0L;
        long completedOrderCount = 0L;
        long canceledOrderCount = 0L;

        for (Order order : orders) {
            if (!isWithinDateRange(order != null ? order.getCreatedAt() : null, startDate, endDate)) {
                continue;
            }
            totalOrderCount++;
            if (isCanceledOrder(order)) {
                canceledOrderCount++;
                continue;
            }
            completedOrderCount++;
            revenue = MoneySupport.add(revenue, getNetOrderRevenue(order));
        }

        Map<ProductKey, BigDecimal> revenueByProduct = new HashMap<>();
        for (OrderItem item : orderItems) {
            if (item == null || item.getOrder() == null || !isOrderEligible(item.getOrder()) || !isWithinDateRange(item.getOrder().getCreatedAt(), startDate, endDate)) {
                continue;
            }
            int orderedQty = item.getQuantity() != null ? item.getQuantity() : 0;
            int returnedQty = item.getReturnedQuantity() != null ? item.getReturnedQuantity() : 0;
            int netQty = Math.max(0, orderedQty - returnedQty);
            if (netQty <= 0) {
                continue;
            }
            BigDecimal netRevenue = item.getNetRevenueForQuantity(netQty);
            ProductKey key = new ProductKey(
                item.getProduct() != null ? item.getProduct().getId() : null,
                item.getProductDisplayName(),
                item.getCategoryDisplayName()
            );
            revenueByProduct.merge(key, netRevenue, MoneySupport::add);
        }

        BigDecimal averageOrderValue = completedOrderCount > 0
            ? revenue.divide(BigDecimal.valueOf(completedOrderCount), MoneySupport.MONEY_SCALE, MoneySupport.MONEY_ROUNDING)
            : MoneySupport.ZERO;
        BigDecimal cancelRate = totalOrderCount > 0
            ? BigDecimal.valueOf(canceledOrderCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalOrderCount), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return new PeriodMetrics(
            revenue,
            completedOrderCount,
            totalOrderCount,
            canceledOrderCount,
            averageOrderValue,
            cancelRate,
            revenueByProduct
        );
    }

    private WhatChangedInsight buildRevenueInsight(
        InsightRange currentRange,
        InsightRange baselineRange,
        PeriodMetrics currentMetrics,
        PeriodMetrics baselineMetrics
    ) {
        BigDecimal baselineRevenue = baselineMetrics.revenue();
        BigDecimal currentRevenue = currentMetrics.revenue();
        if (baselineRevenue.compareTo(MoneySupport.ZERO) == 0 && currentRevenue.compareTo(MoneySupport.ZERO) == 0) {
            return null;
        }

        BigDecimal delta = MoneySupport.subtract(currentRevenue, baselineRevenue);
        BigDecimal deltaPercent = computePercentageChange(currentRevenue, baselineRevenue);
        if (deltaPercent != null && deltaPercent.abs().compareTo(MIN_MEANINGFUL_CHANGE_PERCENT) < 0) {
            return null;
        }

        String direction = delta.signum() >= 0 ? "up" : "down";
        String headline;
        if (deltaPercent == null) {
            headline = "Revenue " + direction + " from no sales in " + baselineRange.label().toLowerCase(Locale.ROOT);
        } else {
            headline = "Revenue " + direction + " " + formatPercent(deltaPercent.abs()) + " vs " + baselineRange.label().toLowerCase(Locale.ROOT);
        }
        String detail = buildRevenueDetail(currentRange, baselineRange, currentMetrics, baselineMetrics);
        return new WhatChangedInsight(
            WhatChangedType.REVENUE_CHANGE,
            delta.signum() < 0 ? InsightSeverity.WARNING : InsightSeverity.INFO,
            headline,
            detail,
            InsightDrilldownTarget.REVENUE
        );
    }

    private String buildRevenueDetail(
        InsightRange currentRange,
        InsightRange baselineRange,
        PeriodMetrics currentMetrics,
        PeriodMetrics baselineMetrics
    ) {
        String orderPhrase = describeCountMovement("Orders", baselineMetrics.completedOrderCount(), currentMetrics.completedOrderCount());
        BigDecimal aovDeltaPercent = computePercentageChange(currentMetrics.averageOrderValue(), baselineMetrics.averageOrderValue());
        String aovPhrase;
        if (baselineMetrics.completedOrderCount() == 0L || aovDeltaPercent == null || aovDeltaPercent.abs().compareTo(MIN_MEANINGFUL_CHANGE_PERCENT) < 0) {
            aovPhrase = "average order value stayed flat";
        } else if (aovDeltaPercent.signum() >= 0) {
            aovPhrase = "average order value improved from " + formatVnd(baselineMetrics.averageOrderValue()) + " to " + formatVnd(currentMetrics.averageOrderValue());
        } else {
            aovPhrase = "average order value fell from " + formatVnd(baselineMetrics.averageOrderValue()) + " to " + formatVnd(currentMetrics.averageOrderValue());
        }
        return orderPhrase + " while " + aovPhrase + ".";
    }

    private WhatChangedInsight buildOrderCountInsight(
        InsightRange currentRange,
        InsightRange baselineRange,
        PeriodMetrics currentMetrics,
        PeriodMetrics baselineMetrics
    ) {
        if (currentMetrics.completedOrderCount() == baselineMetrics.completedOrderCount()) {
            return null;
        }
        BigDecimal deltaPercent = computePercentageChange(
            BigDecimal.valueOf(currentMetrics.completedOrderCount()),
            BigDecimal.valueOf(baselineMetrics.completedOrderCount())
        );
        if (deltaPercent != null && deltaPercent.abs().compareTo(MIN_MEANINGFUL_CHANGE_PERCENT) < 0) {
            return null;
        }
        long delta = currentMetrics.completedOrderCount() - baselineMetrics.completedOrderCount();
        String direction = delta >= 0 ? "up" : "down";
        String headline = deltaPercent == null
            ? "Orders " + direction + " from no orders in " + baselineRange.label().toLowerCase(Locale.ROOT)
            : "Orders " + direction + " " + formatPercent(deltaPercent.abs()) + " vs " + baselineRange.label().toLowerCase(Locale.ROOT);
        String detail = "Completed orders moved from " + baselineMetrics.completedOrderCount()
            + " to " + currentMetrics.completedOrderCount() + " in " + currentRange.label().toLowerCase(Locale.ROOT) + ".";
        return new WhatChangedInsight(
            WhatChangedType.ORDER_COUNT_CHANGE,
            delta < 0 ? InsightSeverity.WARNING : InsightSeverity.INFO,
            headline,
            detail,
            InsightDrilldownTarget.ORDERS
        );
    }

    private WhatChangedInsight buildAverageOrderValueInsight(
        InsightRange currentRange,
        InsightRange baselineRange,
        PeriodMetrics currentMetrics,
        PeriodMetrics baselineMetrics
    ) {
        if (currentMetrics.completedOrderCount() == 0L || baselineMetrics.completedOrderCount() == 0L) {
            return null;
        }
        BigDecimal deltaPercent = computePercentageChange(currentMetrics.averageOrderValue(), baselineMetrics.averageOrderValue());
        if (deltaPercent == null || deltaPercent.abs().compareTo(MIN_MEANINGFUL_CHANGE_PERCENT) < 0) {
            return null;
        }
        String direction = deltaPercent.signum() >= 0 ? "up" : "down";
        String headline = "Average order value " + direction + " " + formatPercent(deltaPercent.abs())
            + " vs " + baselineRange.label().toLowerCase(Locale.ROOT);
        String detail = "Average order value moved from " + formatVnd(baselineMetrics.averageOrderValue())
            + " to " + formatVnd(currentMetrics.averageOrderValue()) + ".";
        return new WhatChangedInsight(
            WhatChangedType.AVERAGE_ORDER_VALUE_CHANGE,
            deltaPercent.signum() < 0 ? InsightSeverity.WARNING : InsightSeverity.INFO,
            headline,
            detail,
            InsightDrilldownTarget.ORDERS
        );
    }

    private WhatChangedInsight buildCancelRateInsight(
        InsightRange currentRange,
        InsightRange baselineRange,
        PeriodMetrics currentMetrics,
        PeriodMetrics baselineMetrics
    ) {
        if (currentMetrics.totalOrderCount() == 0L && baselineMetrics.totalOrderCount() == 0L) {
            return null;
        }
        BigDecimal percentagePointDelta = currentMetrics.cancelRate().subtract(baselineMetrics.cancelRate()).setScale(2, RoundingMode.HALF_UP);
        if (percentagePointDelta.abs().compareTo(CANCEL_RATE_SPIKE_PERCENTAGE_POINTS) < 0
            && Math.abs(currentMetrics.canceledOrderCount() - baselineMetrics.canceledOrderCount()) < 1) {
            return null;
        }
        String direction = percentagePointDelta.signum() >= 0 ? "up" : "down";
        String headline = "Cancel rate " + direction + " " + formatPercent(percentagePointDelta.abs())
            + " vs " + baselineRange.label().toLowerCase(Locale.ROOT);
        String detail = "Canceled orders moved from "
            + baselineMetrics.canceledOrderCount() + "/" + baselineMetrics.totalOrderCount()
            + " to " + currentMetrics.canceledOrderCount() + "/" + currentMetrics.totalOrderCount() + ".";
        return new WhatChangedInsight(
            WhatChangedType.CANCEL_RATE_CHANGE,
            percentagePointDelta.signum() > 0 ? InsightSeverity.WARNING : InsightSeverity.INFO,
            headline,
            detail,
            InsightDrilldownTarget.CANCELED_ORDERS
        );
    }

    private WhatChangedInsight buildTopDriverInsight(
        InsightRange currentRange,
        InsightRange baselineRange,
        ProductDelta topDriver
    ) {
        if (topDriver == null || topDriver.delta().compareTo(MoneySupport.ZERO) == 0) {
            return null;
        }
        String direction = topDriver.delta().signum() >= 0 ? "lift" : "drag";
        String headline = topDriver.productName() + " was the top revenue " + direction + " vs " + baselineRange.label().toLowerCase(Locale.ROOT);
        String detail = topDriver.productName() + " changed by " + formatSignedVnd(topDriver.delta())
            + (topDriver.categoryName() != null && !topDriver.categoryName().isBlank()
            ? " in " + topDriver.categoryName() + "."
            : ".");
        return new WhatChangedInsight(
            WhatChangedType.TOP_DRIVER_PRODUCT,
            topDriver.delta().signum() < 0 ? InsightSeverity.WARNING : InsightSeverity.INFO,
            headline,
            detail,
            InsightDrilldownTarget.TOP_SELLING
        );
    }

    private InventoryInsightContext buildInventoryInsightContext() {
        List<Product> activeProducts = productRepository.findAllActiveWithCategory();
        if (activeProducts.isEmpty()) {
            return new InventoryInsightContext(List.of(), Map.of(), Map.of(), Map.of(), List.of());
        }

        LocalDateTime recentSalesStart = LocalDate.now().minusDays(DEMAND_WINDOW_DAYS - 1L).atStartOfDay();
        LocalDateTime recentSalesEnd = LocalDate.now().atTime(LocalTime.MAX);

        List<OrderItem> recentOrderItems = orderItemRepository.findAllWithOrderAndProductWithinDateRange(recentSalesStart, recentSalesEnd);
        Map<Long, Integer> netSold14dByProductId = new HashMap<>();
        for (OrderItem item : recentOrderItems) {
            if (item == null || item.getOrder() == null || !isOrderEligible(item.getOrder()) || item.getProduct() == null || item.getProduct().getId() == null) {
                continue;
            }
            int orderedQty = item.getQuantity() != null ? item.getQuantity() : 0;
            int returnedQty = item.getReturnedQuantity() != null ? item.getReturnedQuantity() : 0;
            int netQty = Math.max(0, orderedQty - returnedQty);
            if (netQty <= 0) {
                continue;
            }
            netSold14dByProductId.merge(item.getProduct().getId(), netQty, Integer::sum);
        }

        List<InventoryTransaction> transactions = inventoryTransactionRepository.findInboundWithProductOrderByCreatedAtDesc();
        Map<Long, LocalDateTime> lastInboundAtByProductId = new HashMap<>();
        List<AgingCandidate> agingCandidates = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (InventoryTransaction transaction : transactions) {
            if (transaction.getProduct() == null || transaction.getProduct().getId() == null) {
                continue;
            }
            int quantityChange = transaction.getQuantityChange() != null ? transaction.getQuantityChange() : 0;
            if (quantityChange <= 0) {
                continue;
            }
            lastInboundAtByProductId.putIfAbsent(transaction.getProduct().getId(), transaction.getCreatedAt());
        }

        Collection<Long> productIds = activeProducts.stream()
            .map(Product::getId)
            .filter(Objects::nonNull)
            .toList();
        Map<Long, LatestImportSnapshot> latestImportByProductId = new HashMap<>();
        if (!productIds.isEmpty()) {
            List<ImportOrderItem> latestImportItems = importOrderItemRepository.findCompletedImportSnapshotsForProducts(productIds, ImportOrderStatus.COMPLETED);
            for (ImportOrderItem item : latestImportItems) {
                if (item.getProduct() == null || item.getProduct().getId() == null) {
                    continue;
                }
                latestImportByProductId.putIfAbsent(
                    item.getProduct().getId(),
                    new LatestImportSnapshot(
                        item.getImportOrder() != null ? item.getImportOrder().getCreatedAt() : null,
                        MoneySupport.normalize(item.getImportPrice()),
                        item.getImportOrder() != null ? item.getImportOrder().getSupplierDisplayName() : null
                    )
                );
            }
        }

        for (Product product : activeProducts) {
            int onHand = product.getQuantity() != null ? product.getQuantity() : 0;
            if (onHand <= 0) {
                continue;
            }
            LocalDateTime lastInboundAt = lastInboundAtByProductId.get(product.getId());
            long ageDays = lastInboundAt != null ? ChronoUnit.DAYS.between(lastInboundAt.toLocalDate(), today) : -1L;
            agingCandidates.add(new AgingCandidate(
                product.getId(),
                product.getName(),
                product.getCategory() != null ? product.getCategory().getName() : "Uncategorized",
                onHand,
                ageDays
            ));
        }

        return new InventoryInsightContext(activeProducts, netSold14dByProductId, lastInboundAtByProductId, latestImportByProductId, agingCandidates);
    }

    private ExplainableReorderSnapshot buildExplainableReorderSnapshot(InventoryInsightContext context) {
        List<ExplainableReorderRow> rows = new ArrayList<>();

        for (Product product : context.activeProducts()) {
            if (product.getId() == null) {
                continue;
            }
            int onHand = product.getQuantity() != null ? product.getQuantity() : 0;
            int minStock = product.getMinStockLevel() != null ? product.getMinStockLevel() : 0;
            int netSold14d = context.netSold14dByProductId().getOrDefault(product.getId(), 0);
            BigDecimal avgDailyUnits = MoneySupport.divide(BigDecimal.valueOf(netSold14d), DEMAND_WINDOW_DAYS);
            boolean coverageKnown = avgDailyUnits.compareTo(MoneySupport.ZERO) > 0;
            BigDecimal coverageDays = coverageKnown
                ? BigDecimal.valueOf(onHand).divide(avgDailyUnits, 2, RoundingMode.HALF_UP)
                : null;

            BigDecimal targetUnits = avgDailyUnits.multiply(BigDecimal.valueOf(TARGET_COVERAGE_DAYS));
            int baseSuggestedQty = targetUnits
                .subtract(BigDecimal.valueOf(onHand))
                .setScale(0, RoundingMode.CEILING)
                .intValue();
            int suggestedQty = Math.max(Math.max(baseSuggestedQty, minStock - onHand), 0);

            boolean include = onHand <= minStock || (coverageKnown && coverageDays.compareTo(BigDecimal.valueOf(REORDER_THRESHOLD_DAYS)) < 0);
            if (!include) {
                continue;
            }

            LocalDateTime lastInboundAt = context.lastInboundAtByProductId().get(product.getId());
            LatestImportSnapshot latestImport = context.latestImportByProductId().get(product.getId());
            String explanation = buildReorderExplanation(avgDailyUnits, onHand, coverageDays, coverageKnown, suggestedQty);

            rows.add(new ExplainableReorderRow(
                product.getId(),
                product.getName(),
                product.getCategory() != null ? product.getCategory().getName() : "Uncategorized",
                onHand,
                minStock,
                avgDailyUnits,
                coverageDays,
                coverageKnown,
                suggestedQty,
                lastInboundAt,
                latestImport != null ? latestImport.importPrice() : null,
                latestImport != null ? latestImport.supplierName() : null,
                explanation
            ));
        }

        rows.sort(Comparator
            .comparing((ExplainableReorderRow row) -> row.onHandQuantity() <= row.minStockLevel() ? 0 : 1)
            .thenComparing(row -> row.coverageKnown() ? 0 : 1)
            .thenComparing(row -> row.coverageKnown() && row.coverageDays() != null ? row.coverageDays() : BigDecimal.valueOf(Double.MAX_VALUE))
            .thenComparing(ExplainableReorderRow::suggestedReorderQty, Comparator.reverseOrder())
            .thenComparing(ExplainableReorderRow::productName, String.CASE_INSENSITIVE_ORDER));

        return new ExplainableReorderSnapshot(rows);
    }

    private String buildReorderExplanation(BigDecimal avgDailyUnits, int onHand, BigDecimal coverageDays, boolean coverageKnown, int suggestedQty) {
        StringBuilder builder = new StringBuilder();
        builder.append("Sold avg ")
            .append(formatDecimal(avgDailyUnits))
            .append("/day in last ")
            .append(DEMAND_WINDOW_DAYS)
            .append(" days; on hand ")
            .append(onHand);
        if (coverageKnown && coverageDays != null) {
            builder.append(" covers ~").append(formatDecimal(coverageDays)).append(" days");
        } else {
            builder.append(" has unknown coverage");
        }
        builder.append("; suggest reorder ").append(suggestedQty)
            .append(" to restore ")
            .append(TARGET_COVERAGE_DAYS)
            .append("-day coverage.");
        return builder.toString();
    }

    private ActionCenterSnapshot buildActionCenterSnapshot(
        WhatChangedComputation comparison,
        ExplainableReorderSnapshot reorderSnapshot,
        List<AgingCandidate> agingCandidates
    ) {
        List<ActionCenterItem> items = new ArrayList<>();

        for (ExplainableReorderRow row : reorderSnapshot.rows()) {
            if (row.suggestedReorderQty() <= 0) {
                continue;
            }
            if (row.onHandQuantity() <= row.minStockLevel()) {
                items.add(new ActionCenterItem(
                    ActionCenterType.REORDER_NOW,
                    InsightSeverity.CRITICAL,
                    "Reorder now: " + row.productName(),
                    row.explanation(),
                    "Open Import Goods",
                    InsightDrilldownTarget.REORDER,
                    row.productId(),
                    row.suggestedReorderQty(),
                    "Suggest " + row.suggestedReorderQty() + " units"
                ));
            } else if (row.coverageKnown() && row.coverageDays() != null && row.coverageDays().compareTo(BigDecimal.valueOf(REORDER_THRESHOLD_DAYS)) < 0) {
                items.add(new ActionCenterItem(
                    ActionCenterType.LOW_COVERAGE,
                    InsightSeverity.WARNING,
                    "Low coverage: " + row.productName(),
                    row.explanation(),
                    "Review Reorder",
                    InsightDrilldownTarget.REORDER,
                    row.productId(),
                    null,
                    "~" + formatDecimal(row.coverageDays()) + " days left"
                ));
            }
        }

        for (AgingCandidate candidate : agingCandidates) {
            if (candidate.ageDays() < AGED_STOCK_THRESHOLD_DAYS) {
                continue;
            }
            items.add(new ActionCenterItem(
                ActionCenterType.AGED_STOCK,
                InsightSeverity.WARNING,
                "Aged stock: " + candidate.productName(),
                candidate.productName() + " has " + candidate.onHandQuantity() + " units on hand and has not received inbound stock for "
                    + candidate.ageDays() + " days.",
                "Open Aging Stock",
                InsightDrilldownTarget.AGING_STOCK,
                candidate.productId(),
                null,
                candidate.ageDays() + " days"
            ));
        }

        ActionCenterItem revenueDropItem = buildRevenueDropActionItem(comparison);
        if (revenueDropItem != null) {
            items.add(revenueDropItem);
        }

        ActionCenterItem cancelSpikeItem = buildCancelSpikeActionItem(comparison);
        if (cancelSpikeItem != null) {
            items.add(cancelSpikeItem);
        }

        items.sort(Comparator
            .comparing((ActionCenterItem item) -> severityRank(item.severity()))
            .thenComparing(item -> isInventoryAction(item.type()) ? 0 : 1)
            .thenComparing(ActionCenterItem::impactLabel, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
            .thenComparing(ActionCenterItem::title, String.CASE_INSENSITIVE_ORDER));

        long criticalCount = items.stream().filter(item -> item.severity() == InsightSeverity.CRITICAL).count();
        long warningCount = items.stream().filter(item -> item.severity() == InsightSeverity.WARNING).count();
        long infoCount = items.stream().filter(item -> item.severity() == InsightSeverity.INFO).count();

        return new ActionCenterSnapshot(items, criticalCount, warningCount, infoCount);
    }

    private ActionCenterItem buildRevenueDropActionItem(WhatChangedComputation comparison) {
        BigDecimal baselineRevenue = comparison.baselineMetrics().revenue();
        BigDecimal currentRevenue = comparison.currentMetrics().revenue();
        if (baselineRevenue.compareTo(MoneySupport.ZERO) <= 0) {
            return null;
        }

        BigDecimal deltaPercent = computePercentageChange(currentRevenue, baselineRevenue);
        if (deltaPercent == null || deltaPercent.signum() >= 0 || deltaPercent.abs().compareTo(REVENUE_DROP_ACTION_PERCENT) < 0) {
            return null;
        }

        InsightSeverity severity = deltaPercent.abs().compareTo(REVENUE_DROP_CRITICAL_PERCENT) >= 0
            ? InsightSeverity.CRITICAL
            : InsightSeverity.WARNING;

        return new ActionCenterItem(
            ActionCenterType.REVENUE_DROP,
            severity,
            "Revenue down " + formatPercent(deltaPercent.abs()) + " vs " + comparison.baselineRange().label().toLowerCase(Locale.ROOT),
            buildRevenueDetail(comparison.currentRange(), comparison.baselineRange(), comparison.currentMetrics(), comparison.baselineMetrics()),
            "Open Revenue",
            InsightDrilldownTarget.REVENUE,
            null,
            null,
            formatSignedVnd(MoneySupport.subtract(currentRevenue, baselineRevenue))
        );
    }

    private ActionCenterItem buildCancelSpikeActionItem(WhatChangedComputation comparison) {
        long currentCanceled = comparison.currentMetrics().canceledOrderCount();
        long baselineCanceled = comparison.baselineMetrics().canceledOrderCount();
        BigDecimal percentagePointDelta = comparison.currentMetrics().cancelRate()
            .subtract(comparison.baselineMetrics().cancelRate())
            .setScale(2, RoundingMode.HALF_UP);

        boolean isSpike = (baselineCanceled == 0L && currentCanceled >= 2L)
            || (currentCanceled - baselineCanceled >= 2L && percentagePointDelta.compareTo(CANCEL_RATE_SPIKE_PERCENTAGE_POINTS) >= 0);
        if (!isSpike) {
            return null;
        }

        InsightSeverity severity = (currentCanceled - baselineCanceled >= 5L
            || percentagePointDelta.compareTo(CANCEL_RATE_CRITICAL_PERCENTAGE_POINTS) >= 0)
            ? InsightSeverity.CRITICAL
            : InsightSeverity.WARNING;

        return new ActionCenterItem(
            ActionCenterType.CANCEL_SPIKE,
            severity,
            "Canceled orders up vs " + comparison.baselineRange().label().toLowerCase(Locale.ROOT),
            "Canceled orders moved from " + baselineCanceled + " to " + currentCanceled
                + " and cancel rate rose to " + formatPercent(comparison.currentMetrics().cancelRate()) + ".",
            "Open Canceled Orders",
            InsightDrilldownTarget.CANCELED_ORDERS,
            null,
            null,
            "+" + (currentCanceled - baselineCanceled) + " canceled"
        );
    }

    private ProductDelta resolveTopDriver(Map<ProductKey, BigDecimal> current, Map<ProductKey, BigDecimal> baseline) {
        Map<ProductKey, BigDecimal> deltas = new HashMap<>();
        for (Map.Entry<ProductKey, BigDecimal> entry : baseline.entrySet()) {
            deltas.merge(entry.getKey(), MoneySupport.normalize(entry.getValue()).negate(), BigDecimal::add);
        }
        for (Map.Entry<ProductKey, BigDecimal> entry : current.entrySet()) {
            deltas.merge(entry.getKey(), MoneySupport.normalize(entry.getValue()), BigDecimal::add);
        }

        return deltas.entrySet().stream()
            .filter(entry -> entry.getValue().compareTo(MoneySupport.ZERO) != 0)
            .max(Comparator.comparing(entry -> entry.getValue().abs()))
            .map(entry -> new ProductDelta(
                entry.getKey().productId(),
                entry.getKey().productName(),
                entry.getKey().categoryName(),
                entry.getValue()
            ))
            .orElse(null);
    }

    private boolean isWithinDateRange(LocalDateTime createdAt, LocalDate startDate, LocalDate endDate) {
        if (createdAt == null) {
            return false;
        }
        LocalDate createdDate = createdAt.toLocalDate();
        if (startDate != null && createdDate.isBefore(startDate)) {
            return false;
        }
        return endDate == null || !createdDate.isAfter(endDate);
    }

    private boolean isOrderEligible(Order order) {
        if (order == null) {
            return false;
        }
        OrderStatus status = order.getStatus() != null ? order.getStatus() : OrderStatus.COMPLETED;
        return status != OrderStatus.CANCELED;
    }

    private boolean isCanceledOrder(Order order) {
        return order != null && order.getStatus() == OrderStatus.CANCELED;
    }

    private BigDecimal getNetOrderRevenue(Order order) {
        if (!isOrderEligible(order)) {
            return MoneySupport.ZERO;
        }
        return MoneySupport.subtract(order.getTotalPrice(), order.getRefundedAmount());
    }

    private BigDecimal computePercentageChange(BigDecimal current, BigDecimal baseline) {
        BigDecimal normalizedBaseline = MoneySupport.normalize(baseline);
        if (normalizedBaseline.compareTo(MoneySupport.ZERO) == 0) {
            return null;
        }
        return MoneySupport.normalize(current)
            .subtract(normalizedBaseline)
            .multiply(BigDecimal.valueOf(100))
            .divide(normalizedBaseline, 2, RoundingMode.HALF_UP);
    }

    private String formatPercent(BigDecimal value) {
        return MoneySupport.normalize(value).stripTrailingZeros().toPlainString() + "%";
    }

    private String formatDecimal(BigDecimal value) {
        return MoneySupport.normalize(value).stripTrailingZeros().toPlainString();
    }

    private String formatVnd(BigDecimal value) {
        return String.format(Locale.US, "%,.0f VND", MoneySupport.normalize(value));
    }

    private String formatSignedVnd(BigDecimal value) {
        BigDecimal normalized = MoneySupport.normalize(value);
        String prefix = normalized.signum() > 0 ? "+" : "";
        return prefix + formatVnd(normalized);
    }

    private String describeCountMovement(String label, long baseline, long current) {
        if (current > baseline) {
            return label + " rose from " + baseline + " to " + current;
        }
        if (current < baseline) {
            return label + " fell from " + baseline + " to " + current;
        }
        return label + " held at " + current;
    }

    private String formatRangeLabel(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return "Selected range";
        }
        if (startDate.equals(endDate)) {
            return startDate.toString();
        }
        return startDate + " to " + endDate;
    }

    private int severityRank(InsightSeverity severity) {
        if (severity == InsightSeverity.CRITICAL) {
            return 0;
        }
        if (severity == InsightSeverity.WARNING) {
            return 1;
        }
        return 2;
    }

    private boolean isInventoryAction(ActionCenterType type) {
        return type == ActionCenterType.REORDER_NOW
            || type == ActionCenterType.LOW_COVERAGE
            || type == ActionCenterType.AGED_STOCK;
    }

    private record InsightRange(LocalDate start, LocalDate end, String label) {
    }

    private record ProductKey(Long productId, String productName, String categoryName) {
    }

    private record ProductDelta(Long productId, String productName, String categoryName, BigDecimal delta) {
    }

    private record PeriodMetrics(
        BigDecimal revenue,
        long completedOrderCount,
        long totalOrderCount,
        long canceledOrderCount,
        BigDecimal averageOrderValue,
        BigDecimal cancelRate,
        Map<ProductKey, BigDecimal> productRevenueByProduct
    ) {
    }

    private record WhatChangedComputation(
        WhatChangedSnapshot snapshot,
        PeriodMetrics currentMetrics,
        PeriodMetrics baselineMetrics,
        ProductDelta topDriver,
        InsightRange currentRange,
        InsightRange baselineRange
    ) {
    }

    private record LatestImportSnapshot(LocalDateTime createdAt, BigDecimal importPrice, String supplierName) {
    }

    private record AgingCandidate(Long productId, String productName, String categoryName, int onHandQuantity, long ageDays) {
    }

    private record InventoryInsightContext(
        List<Product> activeProducts,
        Map<Long, Integer> netSold14dByProductId,
        Map<Long, LocalDateTime> lastInboundAtByProductId,
        Map<Long, LatestImportSnapshot> latestImportByProductId,
        List<AgingCandidate> agingCandidates
    ) {
    }
}
