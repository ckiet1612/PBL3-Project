package com.pbl3.project.pbl3_project.entity;

public enum UiDensityMode {
    COMFORTABLE("Comfortable", null),
    COMPACT("Compact", "ui-density-compact");

    private final String label;
    private final String rootStyleClass;

    UiDensityMode(String label, String rootStyleClass) {
        this.label = label;
        this.rootStyleClass = rootStyleClass;
    }

    public String getLabel() {
        return label;
    }

    public String getRootStyleClass() {
        return rootStyleClass;
    }
}
