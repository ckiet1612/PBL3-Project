package com.pbl3.project.pbl3_project.dto.report;

import java.util.List;

public record OperationalReportData(
    OperationalSummary summary,
    SalesMixSnapshot salesMix,
    List<TopSellingProductRow> topSellingProducts,
    PromotionReportSnapshot promotionReport,
    List<ExpenseCategorySummaryRow> expenseCategorySummaries,
    List<CategoryStockRow> categoryStocks,
    List<AgingStockRow> agingStocks,
    WhatChangedSnapshot whatChanged,
    ActionCenterSnapshot actionCenter,
    ExplainableReorderSnapshot reorder
) {
}
