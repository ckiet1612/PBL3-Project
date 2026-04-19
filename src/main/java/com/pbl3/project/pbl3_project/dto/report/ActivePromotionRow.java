package com.pbl3.project.pbl3_project.dto.report;

import com.pbl3.project.pbl3_project.entity.PromotionScope;

public record ActivePromotionRow(
    Long promotionId,
    String promotionName,
    PromotionScope scope,
    String targetLabel,
    String discountLabel,
    String statusLabel
) {
}
