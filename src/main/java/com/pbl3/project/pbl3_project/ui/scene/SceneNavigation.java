package com.pbl3.project.pbl3_project.ui.scene;

import com.pbl3.project.pbl3_project.ui.scene.model.ImportOrderPrefill;
import com.pbl3.project.pbl3_project.ui.scene.model.ProductViewPreset;
import com.pbl3.project.pbl3_project.ui.scene.model.ReportFocusTarget;
import java.time.LocalDate;

public interface SceneNavigation {
    void showDashboard();

    void showProducts(ProductViewPreset preset);

    void showImportGoods();

    void showImportGoods(ImportOrderPrefill prefill);

    void showSales();

    void showPromotions();

    void showStocktake();

    void showSettings();

    void showReports(LocalDate startDate, LocalDate endDate, ReportFocusTarget focusTarget);

    void showExpenses(LocalDate startDate, LocalDate endDate);

    void refreshCurrentScene();
}
