package com.pbl3.project.pbl3_project.dto.report;

public record ActionCenterItem(
    ActionCenterType type,
    InsightSeverity severity,
    String title,
    String description,
    String actionLabel,
    InsightDrilldownTarget drilldownTarget,
    Long productId,
    Integer suggestedQuantity,
    String impactLabel
) {
}
