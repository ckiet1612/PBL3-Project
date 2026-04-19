package com.pbl3.project.pbl3_project.entity;

public enum DashboardSectionKey {
    KPI_ROW("KPI Row", false),
    WHAT_CHANGED("What Changed", false),
    ACTION_CENTER("Action Center", false),
    EXPLAINABLE_REORDER("Explainable Reorder", false),
    REVENUE_CHART("Revenue Chart", true),
    ORDERS_CHART("Orders Chart", true),
    CANCELED_ORDERS_CHART("Canceled Orders Chart", false),
    PAYMENT_METHOD_SHARE("Payment Method Share", true),
    TOP_SELLING("Top Selling Products", true),
    LOW_STOCK("Low Stock", false);

    private final String label;
    private final boolean gridEligible;

    DashboardSectionKey(String label, boolean gridEligible) {
        this.label = label;
        this.gridEligible = gridEligible;
    }

    public String getLabel() {
        return label;
    }

    public boolean isGridEligible() {
        return gridEligible;
    }
}
