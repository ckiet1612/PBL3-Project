package com.pbl3.project.pbl3_project.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "user_ui_preferences",
    uniqueConstraints = @UniqueConstraint(name = "uk_user_ui_preferences_user", columnNames = "user_id")
)
public class UserUiPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "accent_preset", nullable = false, length = 20)
    private UiAccentPreset accentPreset = UiAccentPreset.BLUE;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "density_mode", nullable = false, length = 20)
    private UiDensityMode densityMode = UiDensityMode.COMFORTABLE;

    @Column(name = "reduced_motion", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean reducedMotion = false;

    @Column(name = "sidebar_collapsed_by_default", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean sidebarCollapsedByDefault = false;

    @Column(name = "dashboard_hidden_sections", nullable = false, columnDefinition = "TEXT")
    private String dashboardHiddenSections = "";

    @Column(name = "dashboard_section_order", nullable = false, columnDefinition = "TEXT")
    private String dashboardSectionOrder = "";

    public UserUiPreferences() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public UiAccentPreset getAccentPreset() {
        return accentPreset;
    }

    public void setAccentPreset(UiAccentPreset accentPreset) {
        this.accentPreset = accentPreset;
    }

    public UiDensityMode getDensityMode() {
        return densityMode;
    }

    public void setDensityMode(UiDensityMode densityMode) {
        this.densityMode = densityMode;
    }

    public boolean isReducedMotion() {
        return reducedMotion;
    }

    public void setReducedMotion(boolean reducedMotion) {
        this.reducedMotion = reducedMotion;
    }

    public boolean isSidebarCollapsedByDefault() {
        return sidebarCollapsedByDefault;
    }

    public void setSidebarCollapsedByDefault(boolean sidebarCollapsedByDefault) {
        this.sidebarCollapsedByDefault = sidebarCollapsedByDefault;
    }

    public String getDashboardHiddenSections() {
        return dashboardHiddenSections;
    }

    public void setDashboardHiddenSections(String dashboardHiddenSections) {
        this.dashboardHiddenSections = dashboardHiddenSections;
    }

    public String getDashboardSectionOrder() {
        return dashboardSectionOrder;
    }

    public void setDashboardSectionOrder(String dashboardSectionOrder) {
        this.dashboardSectionOrder = dashboardSectionOrder;
    }
}
