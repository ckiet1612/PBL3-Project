package com.pbl3.project.pbl3_project.dto.report;

import java.math.BigDecimal;

public record OperationalSummary(
    BigDecimal netRevenue,
    BigDecimal estimatedCost,
    BigDecimal grossProfit,
    BigDecimal operatingExpenses,
    BigDecimal netProfit,
    long netUnitsSold,
    long activeSkuCount,
    long lowStockSkuCount,
    BigDecimal refundedAmount,
    long legacyCostUnavailableItems
) {
}
