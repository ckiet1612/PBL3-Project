package com.pbl3.project.pbl3_project.dto.report;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public record AgingStockRow(
    String productName,
    String categoryName,
    int onHandQuantity,
    LocalDateTime lastInboundAt,
    long ageDays,
    String agingBucket,
    BigDecimal retailValue,
    BigDecimal costValue
) {
}
