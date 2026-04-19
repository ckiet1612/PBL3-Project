package com.pbl3.project.pbl3_project.dto.report;

public record WhatChangedInsight(
    WhatChangedType type,
    InsightSeverity severity,
    String headline,
    String detail,
    InsightDrilldownTarget drilldownTarget
) {
}
