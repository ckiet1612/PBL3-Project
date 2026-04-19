package com.pbl3.project.pbl3_project.dto.report;

import com.pbl3.project.pbl3_project.entity.PromotionScope;

import java.math.BigDecimal;

public record PromotionImpactRow(
    Long promotionId,
    String promotionName,
    PromotionScope scope,
    long usageCount,
    BigDecimal totalDiscount
) {
}
