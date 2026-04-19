package com.pbl3.project.pbl3_project.dto.report;

import java.math.BigDecimal;

public record TopSellingProductRow(
    String productName,
    String categoryName,
    long netSoldQuantity,
    BigDecimal netRevenue,
    BigDecimal estimatedProfit,
    int onHandQuantity
) {
}
