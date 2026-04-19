package com.pbl3.project.pbl3_project.dto.report;

import java.math.BigDecimal;
import java.util.List;

public record PromotionReportSnapshot(
    BigDecimal totalDiscount,
    long promotedOrderCount,
    List<PromotionImpactRow> topPromotions,
    List<ActivePromotionRow> activePromotions
) {
}
