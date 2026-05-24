package com.pbl3.project.pbl3_project.ui.util;

import javafx.scene.control.TableColumn;

public record SortCriterion(String uiKey, TableColumn.SortType direction) {
}
