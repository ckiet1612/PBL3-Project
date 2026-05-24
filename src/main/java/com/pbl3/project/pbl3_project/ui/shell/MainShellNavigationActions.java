package com.pbl3.project.pbl3_project.ui.shell;

public record MainShellNavigationActions(
    Runnable showDashboard,
    Runnable showReports,
    Runnable showProducts,
    Runnable showImportGoods,
    Runnable showSales,
    Runnable showPromotions,
    Runnable showMasterData,
    Runnable showOrderHistory,
    Runnable showReturns,
    Runnable showExpenses,
    Runnable showCustomers,
    Runnable showStocktake,
    Runnable showAuditLog,
    Runnable showAccounts,
    Runnable showSettings,
    Runnable showMyAccount,
    Runnable showNotifications,
    Runnable showLogin
) {
}
