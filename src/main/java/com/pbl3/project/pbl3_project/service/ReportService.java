package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.dto.report.AgingStockRow;
import com.pbl3.project.pbl3_project.dto.report.CategoryStockRow;
import com.pbl3.project.pbl3_project.dto.report.DashboardOverviewData;
import com.pbl3.project.pbl3_project.dto.report.ExpenseCategorySummaryRow;
import com.pbl3.project.pbl3_project.dto.report.ActivePromotionRow;
import com.pbl3.project.pbl3_project.dto.report.OperationalReportData;
import com.pbl3.project.pbl3_project.dto.report.OperationalSummary;
import com.pbl3.project.pbl3_project.dto.report.PromotionImpactRow;
import com.pbl3.project.pbl3_project.dto.report.PromotionReportSnapshot;
import com.pbl3.project.pbl3_project.dto.report.SalesMixSnapshot;
import com.pbl3.project.pbl3_project.dto.report.TopSellingProductRow;
import com.pbl3.project.pbl3_project.entity.InventoryTransaction;
import com.pbl3.project.pbl3_project.entity.Order;
import com.pbl3.project.pbl3_project.entity.OrderItem;
import com.pbl3.project.pbl3_project.entity.OrderStatus;
import com.pbl3.project.pbl3_project.entity.PaymentMethod;
import com.pbl3.project.pbl3_project.entity.Product;
import com.pbl3.project.pbl3_project.entity.Promotion;
import com.pbl3.project.pbl3_project.entity.PromotionDiscountType;
import com.pbl3.project.pbl3_project.entity.PromotionScope;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.repository.ExpenseRepository;
import com.pbl3.project.pbl3_project.repository.InventoryTransactionRepository;
import com.pbl3.project.pbl3_project.repository.OrderItemRepository;
import com.pbl3.project.pbl3_project.repository.OrderRepository;
import com.pbl3.project.pbl3_project.repository.ProductRepository;
import com.pbl3.project.pbl3_project.repository.PromotionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {
    private static final DateTimeFormatter MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MM/yy");
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final PromotionRepository promotionRepository;
    private final ExpenseRepository expenseRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final AuthorizationService authorizationService;
    private final OperationalInsightService operationalInsightService;

    public ReportService(OrderRepository orderRepository,
                         OrderItemRepository orderItemRepository,
                         ProductRepository productRepository,
                         PromotionRepository promotionRepository,
                         ExpenseRepository expenseRepository,
                         InventoryTransactionRepository inventoryTransactionRepository,
                         AuthorizationService authorizationService,
                         OperationalInsightService operationalInsightService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.promotionRepository = promotionRepository;
        this.expenseRepository = expenseRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.authorizationService = authorizationService;
        this.operationalInsightService = operationalInsightService;
    }

    public Map<String, Object> getDailyStats() {
        LocalDateTime start = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime end = LocalDateTime.now().with(LocalTime.MAX);
        return getStats(start, end);
    }

    public Map<String, Object> getMonthlyStats() {
        LocalDateTime start = LocalDateTime.now().withDayOfMonth(1).with(LocalTime.MIN);
        LocalDateTime end = LocalDateTime.now().withDayOfMonth(LocalDateTime.now().toLocalDate().lengthOfMonth()).with(LocalTime.MAX);
        return getStats(start, end);
    }

    private Map<String, Object> getStats(LocalDateTime start, LocalDateTime end) {
        BigDecimal revenue = MoneySupport.normalize(orderRepository.sumRevenueBetween(start, end));
        Long count = orderRepository.countOrdersBetween(start, end);

        Map<String, Object> stats = new HashMap<>();
        stats.put("revenue", revenue);
        stats.put("orders", count != null ? count : 0L);
        return stats;
    }

    public long getLowStockCount(int threshold) {
        return productRepository.countByQuantityLessThanAndIsDeletedFalse(threshold);
    }

    public List<Product> getLowStockProducts() {
        return productRepository.findLowStockProducts();
    }

    public long countLowStockProducts() {
        return productRepository.countLowStockProducts();
    }

    public Map<String, BigDecimal> getLast7DaysRevenue() {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(LocalTime.MAX);

            BigDecimal revenue = MoneySupport.normalize(orderRepository.sumRevenueBetween(start, end));
            String label = date.getDayOfMonth() + "/" + date.getMonthValue();
            result.put(label, revenue);
        }

        return result;
    }

    @Transactional(readOnly = true)
    public DashboardOverviewData getDashboardOverviewData() {
        return getDashboardOverviewData(null);
    }

    @Transactional(readOnly = true)
    public DashboardOverviewData getDashboardOverviewData(User viewer) {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate last7DaysStart = today.minusDays(6);
        var insights = operationalInsightService.getDashboardInsights(viewer);

        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);
        LocalDateTime yesterdayStart = yesterday.atStartOfDay();
        LocalDateTime yesterdayEnd = yesterday.atTime(LocalTime.MAX);
        BigDecimal todayExpenses = MoneySupport.normalize(expenseRepository.sumAmountOn(today));
        BigDecimal yesterdayExpenses = MoneySupport.normalize(expenseRepository.sumAmountOn(yesterday));

        if (viewer != null && !authorizationService.canViewAllOrders(viewer)) {
            LocalDateTime last7DaysRangeStart = last7DaysStart.atStartOfDay();
            List<Order> orders = orderRepository.findAllWithinDateRangeAndUserId(last7DaysRangeStart, todayEnd, viewer.getId());
            List<OrderItem> orderItems = orderItemRepository.findAllWithOrderAndProductWithinDateRangeAndUserId(last7DaysRangeStart, todayEnd, viewer.getId());
            SalesMixSnapshot salesMix = buildSalesMixSnapshot(orders, orderItems, last7DaysStart, today, 5);
            BigDecimal todayRevenue = calculateRevenueForDate(orders, today);
            BigDecimal yesterdayRevenue = calculateRevenueForDate(orders, yesterday);
            long todayOrders = countOrdersForDate(orders, today);
            long yesterdayOrders = countOrdersForDate(orders, yesterday);

            return new DashboardOverviewData(
                todayRevenue,
                MoneySupport.subtract(todayRevenue, yesterdayRevenue),
                todayOrders,
                todayOrders - yesterdayOrders,
                MoneySupport.ZERO,
                MoneySupport.ZERO,
                0,
                0,
                salesMix,
                List.of(),
                insights.whatChanged(),
                insights.actionCenter(),
                insights.reorder()
            );
        }

        BigDecimal todayRevenue = MoneySupport.normalize(orderRepository.sumRevenueBetween(todayStart, todayEnd));
        BigDecimal yesterdayRevenue = MoneySupport.normalize(orderRepository.sumRevenueBetween(yesterdayStart, yesterdayEnd));
        long todayOrders = safeLong(orderRepository.countOrdersBetween(todayStart, todayEnd));
        long yesterdayOrders = safeLong(orderRepository.countOrdersBetween(yesterdayStart, yesterdayEnd));

        LocalDateTime last7DaysRangeStart = last7DaysStart.atStartOfDay();
        List<Product> activeProducts = productRepository.findAllActiveWithCategory();
        List<Product> lowStockProducts = productRepository.findLowStockProducts();
        List<InventoryTransaction> transactionsSinceYesterday =
            inventoryTransactionRepository.findAllWithProductAfterOrderByCreatedAtDesc(yesterdayEnd);
        List<OrderItem> orderItems = orderItemRepository.findAllWithOrderAndProductWithinDateRange(last7DaysRangeStart, todayEnd);
        List<Order> orders = orderRepository.findAllWithinDateRange(last7DaysRangeStart, todayEnd);

        long currentLowStockCount = lowStockProducts.size();
        long previousLowStockCount = calculateLowStockCountAt(activeProducts, transactionsSinceYesterday, yesterdayEnd);

        SalesMixSnapshot salesMix = buildSalesMixSnapshot(orders, orderItems, last7DaysStart, today, 5);

        return new DashboardOverviewData(
            todayRevenue,
            MoneySupport.subtract(todayRevenue, yesterdayRevenue),
            todayOrders,
            todayOrders - yesterdayOrders,
            todayExpenses,
            MoneySupport.subtract(todayExpenses, yesterdayExpenses),
            currentLowStockCount,
            currentLowStockCount - previousLowStockCount,
            salesMix,
            lowStockProducts,
            insights.whatChanged(),
            insights.actionCenter(),
            insights.reorder()
        );
    }

    @Transactional(readOnly = true)
    public OperationalReportData getOperationalReportData() {
        return getOperationalReportData(null, null);
    }

    @Transactional(readOnly = true)
    public OperationalReportData getOperationalReportData(LocalDate startDate, LocalDate endDate) {
        var insights = operationalInsightService.getOperationalReportInsights(startDate, endDate);
        LocalDateTime rangeStart = toRangeStart(startDate);
        LocalDateTime rangeEnd = toRangeEnd(endDate);

        List<Order> orders = orderRepository.findAllWithinDateRange(rangeStart, rangeEnd);
        List<OrderItem> orderItems = orderItemRepository.findAllWithOrderAndProductWithinDateRange(rangeStart, rangeEnd);
        List<Product> activeProducts = productRepository.findAllActiveWithCategory();
        List<InventoryTransaction> transactions = inventoryTransactionRepository.findInboundWithProductOrderByCreatedAtDesc();
        List<ExpenseCategorySummaryRow> expenseCategorySummaries = expenseRepository.findCategorySummariesBetween(startDate, endDate).stream()
            .sorted(Comparator
                .comparing(ExpenseCategorySummaryRow::totalAmount, Comparator.nullsLast(BigDecimal::compareTo))
                .reversed()
                .thenComparing(row -> row.category() != null ? row.category().name() : ""))
            .toList();
        PromotionReportSnapshot promotionReport = buildPromotionReportSnapshot(orders, orderItems, startDate, endDate);
        SalesMixSnapshot salesMix = buildSalesMixSnapshot(orders, orderItems, startDate, endDate, 10);

        BigDecimal netRevenue = MoneySupport.ZERO;
        BigDecimal estimatedCost = MoneySupport.ZERO;
        long netUnitsSold = 0L;
        BigDecimal refundedAmount = MoneySupport.ZERO;
        long legacyCostUnavailableItems = 0L;
        java.util.Set<Long> refundOrderIds = new java.util.HashSet<>();

        for (OrderItem item : orderItems) {
            if (item.getOrder() == null || !isWithinDateRange(item.getOrder().getCreatedAt(), startDate, endDate)) {
                continue;
            }
            OrderStatus status = item.getOrder().getStatus() != null ? item.getOrder().getStatus() : OrderStatus.COMPLETED;
            if (status == OrderStatus.CANCELED) {
                continue;
            }

            int orderedQty = item.getQuantity() != null ? item.getQuantity() : 0;
            int returnedQty = item.getReturnedQuantity() != null ? item.getReturnedQuantity() : 0;
            int netQty = Math.max(0, orderedQty - returnedQty);
            if (netQty <= 0) {
                if (returnedQty > 0 && item.getOrder().getId() != null && refundOrderIds.add(item.getOrder().getId())) {
                    refundedAmount = MoneySupport.add(refundedAmount, item.getOrder().getRefundedAmount());
                }
                continue;
            }

            BigDecimal itemRevenue = item.getNetRevenueForQuantity(netQty);
            netRevenue = MoneySupport.add(netRevenue, itemRevenue);
            netUnitsSold += netQty;

            if (item.getCostAtSale() != null) {
                estimatedCost = MoneySupport.add(estimatedCost, MoneySupport.multiply(item.getCostAtSale(), netQty));
            } else {
                legacyCostUnavailableItems++;
            }

            if (returnedQty > 0 && item.getOrder().getId() != null && refundOrderIds.add(item.getOrder().getId())) {
                refundedAmount = MoneySupport.add(refundedAmount, item.getOrder().getRefundedAmount());
            }
        }

        List<TopSellingProductRow> topSellingRows = salesMix.topSellingProducts();

        Map<String, CategoryAccumulator> categoryStocks = new LinkedHashMap<>();
        for (Product product : activeProducts) {
            String categoryName = product.getCategory() != null ? product.getCategory().getName() : "Uncategorized";
            CategoryAccumulator accumulator = categoryStocks.computeIfAbsent(categoryName, ignored -> new CategoryAccumulator());
            int quantity = product.getQuantity() != null ? product.getQuantity() : 0;

            accumulator.skuCount += 1;
            accumulator.totalQuantity += quantity;
            accumulator.retailValue = MoneySupport.add(accumulator.retailValue, MoneySupport.multiply(product.getPrice(), quantity));
            accumulator.costValue = MoneySupport.add(accumulator.costValue, MoneySupport.multiply(product.getImportPrice(), quantity));
        }

        List<CategoryStockRow> categoryStockRows = categoryStocks.entrySet().stream()
            .map(entry -> new CategoryStockRow(
                entry.getKey(),
                entry.getValue().skuCount,
                entry.getValue().totalQuantity,
                entry.getValue().retailValue,
                entry.getValue().costValue
            ))
            .sorted(Comparator.comparingLong(CategoryStockRow::totalQuantity).reversed())
            .toList();

        Map<Long, LocalDateTime> lastInboundByProductId = new HashMap<>();
        for (InventoryTransaction transaction : transactions) {
            if (transaction.getProduct() == null || transaction.getProduct().getId() == null) {
                continue;
            }
            int quantityChange = transaction.getQuantityChange() != null ? transaction.getQuantityChange() : 0;
            if (quantityChange <= 0) {
                continue;
            }
            lastInboundByProductId.putIfAbsent(transaction.getProduct().getId(), transaction.getCreatedAt());
        }

        List<AgingStockRow> agingStockRows = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (Product product : activeProducts) {
            int quantity = product.getQuantity() != null ? product.getQuantity() : 0;
            if (quantity <= 0) {
                continue;
            }

            LocalDateTime lastInboundAt = lastInboundByProductId.get(product.getId());
            long ageDays = lastInboundAt != null ? ChronoUnit.DAYS.between(lastInboundAt.toLocalDate(), today) : -1;
            agingStockRows.add(new AgingStockRow(
                product.getName(),
                product.getCategory() != null ? product.getCategory().getName() : "Uncategorized",
                quantity,
                lastInboundAt,
                ageDays,
                toAgingBucket(ageDays),
                MoneySupport.multiply(product.getPrice(), quantity),
                MoneySupport.multiply(product.getImportPrice(), quantity)
            ));
        }

        agingStockRows.sort(Comparator
            .comparingLong((AgingStockRow row) -> row.ageDays() < 0 ? Long.MIN_VALUE : row.ageDays())
            .reversed()
            .thenComparing(AgingStockRow::productName));

        BigDecimal operatingExpenses = MoneySupport.normalize(expenseRepository.sumAmountBetween(startDate, endDate));
        BigDecimal grossProfit = MoneySupport.subtract(netRevenue, estimatedCost);
        BigDecimal netProfit = MoneySupport.subtract(grossProfit, operatingExpenses);

        OperationalSummary summary = new OperationalSummary(
            netRevenue,
            estimatedCost,
            grossProfit,
            operatingExpenses,
            netProfit,
            netUnitsSold,
            activeProducts.size(),
            activeProducts.stream().filter(product -> {
                int quantity = product.getQuantity() != null ? product.getQuantity() : 0;
                int minStock = product.getMinStockLevel() != null ? product.getMinStockLevel() : 0;
                return quantity <= minStock;
            }).count(),
            refundedAmount,
            legacyCostUnavailableItems
        );

        return new OperationalReportData(
            summary,
            salesMix,
            topSellingRows,
            promotionReport,
            expenseCategorySummaries,
            categoryStockRows,
            agingStockRows,
            insights.whatChanged(),
            insights.actionCenter(),
            insights.reorder()
        );
    }

    private SalesMixSnapshot buildSalesMixSnapshot(List<Order> orders, List<OrderItem> orderItems, LocalDate startDate, LocalDate endDate, int topLimit) {
        LocalDate resolvedEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate resolvedStart = resolveSalesMixStartDate(orders, startDate, resolvedEnd);
        return new SalesMixSnapshot(
            resolvedStart,
            resolvedEnd,
            buildRevenueSeries(orders, resolvedStart, resolvedEnd),
            buildOrderSeries(orders, resolvedStart, resolvedEnd),
            buildCanceledOrderSeries(orders, resolvedStart, resolvedEnd),
            buildPaymentMethodShare(orders, resolvedStart, resolvedEnd),
            buildTopSellingRows(orderItems, resolvedStart, resolvedEnd, topLimit)
        );
    }

    private LocalDate resolveSalesMixStartDate(List<Order> orders, LocalDate startDate, LocalDate endDate) {
        if (startDate != null) {
            return startDate;
        }
        if (orders == null || orders.isEmpty()) {
            return endDate != null ? endDate.minusDays(6) : LocalDate.now().minusDays(6);
        }
        return orders.stream()
            .filter(this::isOrderEligible)
            .map(Order::getCreatedAt)
            .filter(java.util.Objects::nonNull)
            .map(LocalDateTime::toLocalDate)
            .min(LocalDate::compareTo)
            .orElse(endDate != null ? endDate.minusDays(6) : LocalDate.now().minusDays(6));
    }

    private Map<String, BigDecimal> buildRevenueSeries(List<Order> orders, LocalDate startDate, LocalDate endDate) {
        if (orders == null || startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return new LinkedHashMap<>();
        }
        if (ChronoUnit.DAYS.between(startDate, endDate) > 31) {
            return buildMonthlyRevenueSeries(orders, startDate, endDate);
        }
        Map<LocalDate, BigDecimal> dailyRevenue = new LinkedHashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dailyRevenue.put(date, MoneySupport.ZERO);
        }
        for (Order order : orders) {
            if (!isOrderEligible(order) || !isWithinDateRange(order.getCreatedAt(), startDate, endDate)) {
                continue;
            }
            LocalDate orderDate = order.getCreatedAt().toLocalDate();
            dailyRevenue.merge(orderDate, getNetOrderRevenue(order), MoneySupport::add);
        }
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        dailyRevenue.forEach((date, revenue) -> result.put(formatDayLabel(date), revenue));
        return result;
    }

    private Map<String, BigDecimal> buildMonthlyRevenueSeries(List<Order> orders, LocalDate startDate, LocalDate endDate) {
        Map<YearMonth, BigDecimal> monthlyRevenue = new LinkedHashMap<>();
        YearMonth startMonth = YearMonth.from(startDate);
        YearMonth endMonth = YearMonth.from(endDate);
        for (YearMonth month = startMonth; !month.isAfter(endMonth); month = month.plusMonths(1)) {
            monthlyRevenue.put(month, MoneySupport.ZERO);
        }
        for (Order order : orders) {
            if (!isOrderEligible(order) || !isWithinDateRange(order.getCreatedAt(), startDate, endDate)) {
                continue;
            }
            YearMonth month = YearMonth.from(order.getCreatedAt().toLocalDate());
            monthlyRevenue.merge(month, getNetOrderRevenue(order), MoneySupport::add);
        }
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        monthlyRevenue.forEach((month, revenue) -> result.put(MONTH_LABEL_FORMATTER.format(month.atDay(1)), revenue));
        return result;
    }

    private Map<String, Long> buildOrderSeries(List<Order> orders, LocalDate startDate, LocalDate endDate) {
        if (orders == null || startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return new LinkedHashMap<>();
        }
        if (ChronoUnit.DAYS.between(startDate, endDate) > 31) {
            return buildMonthlyOrderSeries(orders, startDate, endDate);
        }
        Map<LocalDate, Long> dailyOrders = new LinkedHashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dailyOrders.put(date, 0L);
        }
        for (Order order : orders) {
            if (!isOrderEligible(order) || !isWithinDateRange(order.getCreatedAt(), startDate, endDate)) {
                continue;
            }
            LocalDate orderDate = order.getCreatedAt().toLocalDate();
            dailyOrders.merge(orderDate, 1L, Long::sum);
        }
        Map<String, Long> result = new LinkedHashMap<>();
        dailyOrders.forEach((date, count) -> result.put(formatDayLabel(date), count));
        return result;
    }

    private Map<String, Long> buildMonthlyOrderSeries(List<Order> orders, LocalDate startDate, LocalDate endDate) {
        Map<YearMonth, Long> monthlyOrders = new LinkedHashMap<>();
        YearMonth startMonth = YearMonth.from(startDate);
        YearMonth endMonth = YearMonth.from(endDate);
        for (YearMonth month = startMonth; !month.isAfter(endMonth); month = month.plusMonths(1)) {
            monthlyOrders.put(month, 0L);
        }
        for (Order order : orders) {
            if (!isOrderEligible(order) || !isWithinDateRange(order.getCreatedAt(), startDate, endDate)) {
                continue;
            }
            YearMonth month = YearMonth.from(order.getCreatedAt().toLocalDate());
            monthlyOrders.merge(month, 1L, Long::sum);
        }
        Map<String, Long> result = new LinkedHashMap<>();
        monthlyOrders.forEach((month, count) -> result.put(MONTH_LABEL_FORMATTER.format(month.atDay(1)), count));
        return result;
    }

    private Map<String, Long> buildCanceledOrderSeries(List<Order> orders, LocalDate startDate, LocalDate endDate) {
        if (orders == null || startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return new LinkedHashMap<>();
        }
        if (ChronoUnit.DAYS.between(startDate, endDate) > 31) {
            return buildMonthlyCanceledOrderSeries(orders, startDate, endDate);
        }
        Map<LocalDate, Long> dailyCanceledOrders = new LinkedHashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dailyCanceledOrders.put(date, 0L);
        }
        for (Order order : orders) {
            if (!isCanceledOrder(order) || !isWithinDateRange(order.getCreatedAt(), startDate, endDate)) {
                continue;
            }
            LocalDate orderDate = order.getCreatedAt().toLocalDate();
            dailyCanceledOrders.merge(orderDate, 1L, Long::sum);
        }
        Map<String, Long> result = new LinkedHashMap<>();
        dailyCanceledOrders.forEach((date, count) -> result.put(formatDayLabel(date), count));
        return result;
    }

    private Map<String, Long> buildMonthlyCanceledOrderSeries(List<Order> orders, LocalDate startDate, LocalDate endDate) {
        Map<YearMonth, Long> monthlyCanceledOrders = new LinkedHashMap<>();
        YearMonth startMonth = YearMonth.from(startDate);
        YearMonth endMonth = YearMonth.from(endDate);
        for (YearMonth month = startMonth; !month.isAfter(endMonth); month = month.plusMonths(1)) {
            monthlyCanceledOrders.put(month, 0L);
        }
        for (Order order : orders) {
            if (!isCanceledOrder(order) || !isWithinDateRange(order.getCreatedAt(), startDate, endDate)) {
                continue;
            }
            YearMonth month = YearMonth.from(order.getCreatedAt().toLocalDate());
            monthlyCanceledOrders.merge(month, 1L, Long::sum);
        }
        Map<String, Long> result = new LinkedHashMap<>();
        monthlyCanceledOrders.forEach((month, count) -> result.put(MONTH_LABEL_FORMATTER.format(month.atDay(1)), count));
        return result;
    }

    private Map<PaymentMethod, Long> buildPaymentMethodShare(List<Order> orders, LocalDate startDate, LocalDate endDate) {
        Map<PaymentMethod, Long> counts = new LinkedHashMap<>();
        counts.put(PaymentMethod.CASH, 0L);
        counts.put(PaymentMethod.CARD, 0L);
        counts.put(PaymentMethod.QR, 0L);

        for (Order order : orders) {
            if (!isOrderEligible(order) || !isWithinDateRange(order.getCreatedAt(), startDate, endDate) || order.getPaymentMethod() == null) {
                continue;
            }
            counts.merge(order.getPaymentMethod(), 1L, Long::sum);
        }
        return counts;
    }

    private List<TopSellingProductRow> buildTopSellingRows(List<OrderItem> orderItems, LocalDate startDate, LocalDate endDate, int limit) {
        Map<String, TopSellingAccumulator> topSelling = new LinkedHashMap<>();

        for (OrderItem item : orderItems) {
            if (item.getOrder() == null || !isOrderEligible(item.getOrder()) || !isWithinDateRange(item.getOrder().getCreatedAt(), startDate, endDate)) {
                continue;
            }

            int orderedQty = item.getQuantity() != null ? item.getQuantity() : 0;
            int returnedQty = item.getReturnedQuantity() != null ? item.getReturnedQuantity() : 0;
            int netQty = Math.max(0, orderedQty - returnedQty);
            if (netQty <= 0) {
                continue;
            }

            String productKey = item.getProduct() != null && item.getProduct().getId() != null
                ? "P:" + item.getProduct().getId()
                : "L:" + item.getProductDisplayName() + "|" + (item.getSkuSnapshot() != null ? item.getSkuSnapshot() : "");

            TopSellingAccumulator accumulator = topSelling.computeIfAbsent(
                productKey,
                ignored -> new TopSellingAccumulator(
                    item.getProductDisplayName(),
                    item.getCategoryDisplayName(),
                    item.getProduct() != null && item.getProduct().getQuantity() != null ? item.getProduct().getQuantity() : 0
                )
            );
            accumulator.netSoldQuantity += netQty;
            BigDecimal netRevenue = item.getNetRevenueForQuantity(netQty);
            accumulator.netRevenue = MoneySupport.add(accumulator.netRevenue, netRevenue);
            if (item.getCostAtSale() != null) {
                accumulator.estimatedProfit = MoneySupport.add(
                    accumulator.estimatedProfit,
                    MoneySupport.subtract(
                        netRevenue,
                        MoneySupport.multiply(item.getCostAtSale(), netQty)
                    )
                );
            }
        }

        return topSelling.values().stream()
            .sorted(Comparator
                .comparingLong(TopSellingAccumulator::getNetSoldQuantity).reversed()
                .thenComparing(TopSellingAccumulator::getNetRevenue).reversed())
            .limit(limit)
            .map(acc -> new TopSellingProductRow(
                acc.productName,
                acc.categoryName,
                acc.netSoldQuantity,
                acc.netRevenue,
                acc.estimatedProfit,
                acc.onHandQuantity
            ))
            .toList();
    }

    private BigDecimal calculateRevenueForDate(List<Order> orders, LocalDate date) {
        if (orders == null || date == null) {
            return MoneySupport.ZERO;
        }
        return orders.stream()
            .filter(this::isOrderEligible)
            .filter(order -> order.getCreatedAt() != null && date.equals(order.getCreatedAt().toLocalDate()))
            .map(this::getNetOrderRevenue)
            .reduce(MoneySupport.ZERO, MoneySupport::add);
    }

    private long countOrdersForDate(List<Order> orders, LocalDate date) {
        if (orders == null || date == null) {
            return 0L;
        }
        return orders.stream()
            .filter(this::isOrderEligible)
            .filter(order -> order.getCreatedAt() != null && date.equals(order.getCreatedAt().toLocalDate()))
            .count();
    }

    private long calculateLowStockCountAt(List<Product> activeProducts, List<InventoryTransaction> transactions, LocalDateTime snapshotTime) {
        Map<Long, Integer> quantityDeltaSinceSnapshot = new HashMap<>();
        for (InventoryTransaction transaction : transactions) {
            if (transaction.getProduct() == null || transaction.getProduct().getId() == null || transaction.getCreatedAt() == null) {
                continue;
            }
            if (!transaction.getCreatedAt().isAfter(snapshotTime)) {
                continue;
            }
            quantityDeltaSinceSnapshot.merge(
                transaction.getProduct().getId(),
                transaction.getQuantityChange() != null ? transaction.getQuantityChange() : 0,
                Integer::sum
            );
        }

        return activeProducts.stream().filter(product -> {
            int currentQuantity = product.getQuantity() != null ? product.getQuantity() : 0;
            int minStock = product.getMinStockLevel() != null ? product.getMinStockLevel() : 0;
            int priorQuantity = currentQuantity - quantityDeltaSinceSnapshot.getOrDefault(product.getId(), 0);
            return priorQuantity <= minStock;
        }).count();
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

    private PromotionReportSnapshot buildPromotionReportSnapshot(List<Order> orders, List<OrderItem> orderItems, LocalDate startDate, LocalDate endDate) {
        BigDecimal totalDiscount = MoneySupport.ZERO;
        long promotedOrderCount = 0L;
        Map<String, PromotionImpactAccumulator> impactAccumulators = new LinkedHashMap<>();

        for (Order order : orders) {
            if (!isOrderEligible(order) || !isWithinDateRange(order.getCreatedAt(), startDate, endDate)) {
                continue;
            }
            BigDecimal orderDiscount = order.getDiscountTotalSnapshot();
            totalDiscount = MoneySupport.add(totalDiscount, orderDiscount);
            if (MoneySupport.isPositive(orderDiscount)) {
                promotedOrderCount++;
            }
            if (order.getAppliedOrderPromotionIdSnapshot() != null && MoneySupport.isPositive(order.getOrderLevelDiscountTotalSnapshot())) {
                String key = "ORDER:" + order.getAppliedOrderPromotionIdSnapshot();
                PromotionImpactAccumulator accumulator = impactAccumulators.computeIfAbsent(
                    key,
                    ignored -> new PromotionImpactAccumulator(
                        order.getAppliedOrderPromotionIdSnapshot(),
                        order.getAppliedOrderPromotionNameSnapshot() != null ? order.getAppliedOrderPromotionNameSnapshot() : "Order Promotion",
                        PromotionScope.ORDER
                    )
                );
                accumulator.usageCount++;
                accumulator.totalDiscount = MoneySupport.add(accumulator.totalDiscount, order.getOrderLevelDiscountTotalSnapshot());
            }
        }

        for (OrderItem item : orderItems) {
            if (item == null || item.getOrder() == null || !isOrderEligible(item.getOrder()) || !isWithinDateRange(item.getOrder().getCreatedAt(), startDate, endDate)) {
                continue;
            }
            if (item.getAppliedProductPromotionIdSnapshot() == null || !MoneySupport.isPositive(item.getLinePromotionDiscountAmountSnapshot())) {
                continue;
            }
            String key = "PRODUCT:" + item.getAppliedProductPromotionIdSnapshot();
            PromotionImpactAccumulator accumulator = impactAccumulators.computeIfAbsent(
                key,
                ignored -> new PromotionImpactAccumulator(
                    item.getAppliedProductPromotionIdSnapshot(),
                    item.getAppliedProductPromotionNameSnapshot() != null ? item.getAppliedProductPromotionNameSnapshot() : "Product Promotion",
                    PromotionScope.PRODUCT
                )
            );
            accumulator.usageCount++;
            accumulator.totalDiscount = MoneySupport.add(accumulator.totalDiscount, item.getLinePromotionDiscountAmountSnapshot());
        }

        List<PromotionImpactRow> topPromotions = impactAccumulators.values().stream()
            .sorted(Comparator
                .comparing(PromotionImpactAccumulator::getTotalDiscount, Comparator.nullsLast(BigDecimal::compareTo)).reversed()
                .thenComparing(PromotionImpactAccumulator::getUsageCount, Comparator.reverseOrder())
                .thenComparing(acc -> acc.promotionName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
            .limit(8)
            .map(acc -> new PromotionImpactRow(
                acc.promotionId,
                acc.promotionName,
                acc.scope,
                acc.usageCount,
                acc.totalDiscount
            ))
            .toList();

        List<ActivePromotionRow> activePromotions = promotionRepository.findAllActiveAt(LocalDateTime.now()).stream()
            .sorted(Comparator
                .comparing(Promotion::getScope)
                .thenComparing(Promotion::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
            .map(promotion -> new ActivePromotionRow(
                promotion.getId(),
                promotion.getName(),
                promotion.getScope(),
                promotion.getScope() == PromotionScope.PRODUCT
                    ? (promotion.getTargetProduct() != null ? promotion.getTargetProduct().getName() : "Specific Product")
                    : (promotion.getMinOrderTotal() != null ? "Min order " + formatVnd(promotion.getMinOrderTotal()) : "Any order"),
                formatPromotionDiscount(promotion),
                promotion.getLifecycleStatus(LocalDateTime.now()).name()
            ))
            .toList();

        return new PromotionReportSnapshot(totalDiscount, promotedOrderCount, topPromotions, activePromotions);
    }

    private String formatPromotionDiscount(Promotion promotion) {
        if (promotion == null) {
            return "-";
        }
        if (promotion.getDiscountType() == PromotionDiscountType.PERCENT) {
            return MoneySupport.normalize(promotion.getDiscountValue()).stripTrailingZeros().toPlainString() + "%";
        }
        return formatVnd(promotion.getDiscountValue());
    }

    private String formatVnd(BigDecimal amount) {
        return String.format(java.util.Locale.US, "%,.0f VND", MoneySupport.normalize(amount));
    }

    private boolean isWithinDateRange(LocalDateTime createdAt, LocalDate startDate, LocalDate endDate) {
        if (createdAt == null) {
            return startDate == null && endDate == null;
        }
        LocalDate createdDate = createdAt.toLocalDate();
        if (startDate != null && createdDate.isBefore(startDate)) {
            return false;
        }
        return endDate == null || !createdDate.isAfter(endDate);
    }

    private LocalDateTime toRangeStart(LocalDate startDate) {
        return startDate != null ? startDate.atStartOfDay() : null;
    }

    private LocalDateTime toRangeEnd(LocalDate endDate) {
        return endDate != null ? endDate.atTime(LocalTime.MAX) : null;
    }

    private long safeLong(Long value) {
        return value != null ? value : 0L;
    }

    private String formatDayLabel(LocalDate date) {
        return date.getDayOfMonth() + "/" + date.getMonthValue();
    }

    private String toAgingBucket(long ageDays) {
        if (ageDays < 0) {
            return "Unknown";
        }
        if (ageDays <= 30) {
            return "0-30 Days";
        }
        if (ageDays <= 60) {
            return "31-60 Days";
        }
        if (ageDays <= 90) {
            return "61-90 Days";
        }
        return ">90 Days";
    }

    private static final class TopSellingAccumulator {
        private final String productName;
        private final String categoryName;
        private final int onHandQuantity;
        private long netSoldQuantity;
        private BigDecimal netRevenue = MoneySupport.ZERO;
        private BigDecimal estimatedProfit = MoneySupport.ZERO;

        private TopSellingAccumulator(String productName, String categoryName, int onHandQuantity) {
            this.productName = productName;
            this.categoryName = categoryName;
            this.onHandQuantity = onHandQuantity;
        }

        private long getNetSoldQuantity() {
            return netSoldQuantity;
        }

        private BigDecimal getNetRevenue() {
            return netRevenue;
        }
    }

    private static final class CategoryAccumulator {
        private long skuCount;
        private long totalQuantity;
        private BigDecimal retailValue = MoneySupport.ZERO;
        private BigDecimal costValue = MoneySupport.ZERO;
    }

    private static final class PromotionImpactAccumulator {
        private final Long promotionId;
        private final String promotionName;
        private final PromotionScope scope;
        private long usageCount;
        private BigDecimal totalDiscount = MoneySupport.ZERO;

        private PromotionImpactAccumulator(Long promotionId, String promotionName, PromotionScope scope) {
            this.promotionId = promotionId;
            this.promotionName = promotionName;
            this.scope = scope;
        }

        private BigDecimal getTotalDiscount() {
            return totalDiscount;
        }

        private Long getUsageCount() {
            return usageCount;
        }
    }
}
