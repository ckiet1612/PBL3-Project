package com.pbl3.project.pbl3_project.dto.report;

public record OperationalInsightBundle(
    WhatChangedSnapshot whatChanged,
    ActionCenterSnapshot actionCenter,
    ExplainableReorderSnapshot reorder
) {
}
