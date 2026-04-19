package com.pbl3.project.pbl3_project.dto.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExplainableReorderRow(
    Long productId,
    String productName,
    String categoryName,
    int onHandQuantity,
    int minStockLevel,
    BigDecimal avgDailyUnits14d,
    BigDecimal coverageDays,
    boolean coverageKnown,
    int suggestedReorderQty,
    LocalDateTime lastInboundAt,
    BigDecimal latestImportPrice,
    String latestSupplierName,
    String explanation
) {
}
