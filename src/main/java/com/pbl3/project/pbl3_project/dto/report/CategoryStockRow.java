package com.pbl3.project.pbl3_project.dto.report;

import java.math.BigDecimal;

public record CategoryStockRow(
    String categoryName,
    long skuCount,
    long totalQuantity,
    BigDecimal retailValue,
    BigDecimal costValue
) {
}
