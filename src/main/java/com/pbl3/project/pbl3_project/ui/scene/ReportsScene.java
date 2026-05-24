package com.pbl3.project.pbl3_project.ui.scene;

import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.ui.scene.model.ReportFocusTarget;
import java.time.LocalDate;
import javafx.scene.Node;

public final class ReportsScene {
    public record Options(LocalDate startDate, LocalDate endDate, ReportFocusTarget focusTarget) {
    }

    private ReportsScene() {
    }

    public static Node create(SceneRuntimeContext context, User user, Options options) {
        LocalDate startDate = options == null ? null : options.startDate();
        LocalDate endDate = options == null ? null : options.endDate();
        ReportFocusTarget focusTarget = options == null ? null : options.focusTarget();
        return new DashboardReportsContentBuilder(context).createReports(user, startDate, endDate, focusTarget);
    }
}
