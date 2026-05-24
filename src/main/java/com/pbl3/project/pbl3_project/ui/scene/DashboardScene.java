package com.pbl3.project.pbl3_project.ui.scene;

import com.pbl3.project.pbl3_project.entity.User;
import javafx.scene.Node;

public final class DashboardScene {
    public record Options() {
    }

    private DashboardScene() {
    }

    public static Node create(SceneRuntimeContext context, User user, Options options) {
        return new DashboardReportsContentBuilder(context).createDashboard(user);
    }
}
