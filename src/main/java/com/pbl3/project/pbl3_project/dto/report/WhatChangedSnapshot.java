package com.pbl3.project.pbl3_project.dto.report;

import java.util.List;

public record WhatChangedSnapshot(
    String currentRangeLabel,
    String baselineRangeLabel,
    List<WhatChangedInsight> insights
) {
}
