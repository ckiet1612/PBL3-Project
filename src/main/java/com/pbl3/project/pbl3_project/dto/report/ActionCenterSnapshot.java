package com.pbl3.project.pbl3_project.dto.report;

import java.util.List;

public record ActionCenterSnapshot(
    List<ActionCenterItem> items,
    long criticalCount,
    long warningCount,
    long infoCount
) {
}
