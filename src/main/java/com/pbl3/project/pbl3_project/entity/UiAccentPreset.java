package com.pbl3.project.pbl3_project.entity;

public enum UiAccentPreset {
    BLUE("Blue", "ui-accent-blue"),
    EMERALD("Emerald", "ui-accent-emerald"),
    AMBER("Amber", "ui-accent-amber");

    private final String label;
    private final String rootStyleClass;

    UiAccentPreset(String label, String rootStyleClass) {
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
