package com.pbl3.project.pbl3_project.dto.report;

import com.pbl3.project.pbl3_project.entity.Product;

import java.math.BigDecimal;
import java.util.List;

public record DashboardOverviewData(
    BigDecimal todayRevenue,
    BigDecimal revenueDeltaVsYesterday,
    long todayOrders,
    long ordersDeltaVsYesterday,
    BigDecimal todayExpenses,
    BigDecimal expenseDeltaVsYesterday,
    long lowStockCount,
    long lowStockDeltaVsYesterday,
    SalesMixSnapshot salesMix,
    List<Product> lowStockProducts,
    WhatChangedSnapshot whatChanged,
    ActionCenterSnapshot actionCenter,
    ExplainableReorderSnapshot reorder
) {
}
