package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.entity.DashboardSectionKey;
import com.pbl3.project.pbl3_project.entity.UiAccentPreset;
import com.pbl3.project.pbl3_project.entity.UiDensityMode;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.entity.UserUiPreferences;
import com.pbl3.project.pbl3_project.repository.UserUiPreferencesRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserUiPreferencesService {

    private static final List<DashboardSectionKey> DEFAULT_DASHBOARD_ORDER = List.of(
        DashboardSectionKey.KPI_ROW,
        DashboardSectionKey.WHAT_CHANGED,
        DashboardSectionKey.ACTION_CENTER,
        DashboardSectionKey.EXPLAINABLE_REORDER,
        DashboardSectionKey.REVENUE_CHART,
        DashboardSectionKey.ORDERS_CHART,
        DashboardSectionKey.CANCELED_ORDERS_CHART,
        DashboardSectionKey.PAYMENT_METHOD_SHARE,
        DashboardSectionKey.TOP_SELLING
    );

    private final UserUiPreferencesRepository userUiPreferencesRepository;
    private final AuthorizationService authorizationService;

    public UserUiPreferencesService(
        UserUiPreferencesRepository userUiPreferencesRepository,
        AuthorizationService authorizationService
    ) {
        this.userUiPreferencesRepository = userUiPreferencesRepository;
        this.authorizationService = authorizationService;
    }

    public UserUiPreferences getPreferences(User user) {
        authorizationService.requireSettingsAccess(user);
        if (user.getId() == null) {
            throw new ValidationException("Cannot load preferences for unsaved user");
        }
        return userUiPreferencesRepository.findByUserId(user.getId())
            .orElseGet(() -> userUiPreferencesRepository.save(createDefaultPreferences(user)));
    }

    public UserUiPreferences updatePreferences(
        User actor,
        User targetUser,
        UiAccentPreset accentPreset,
        UiDensityMode densityMode,
        boolean reducedMotion,
        boolean sidebarCollapsedByDefault,
        List<DashboardSectionKey> dashboardOrder,
        Set<DashboardSectionKey> hiddenSections
    ) {
        authorizationService.requireSettingsAccess(actor);
        if (actor == null || targetUser == null || actor.getId() == null || targetUser.getId() == null
            || !actor.getId().equals(targetUser.getId())) {
            throw new AuthorizationException("You can only edit your own Settings");
        }

        List<DashboardSectionKey> normalizedOrder = normalizeDashboardOrder(dashboardOrder);
        Set<DashboardSectionKey> normalizedHidden = normalizeHiddenSections(hiddenSections);

        UserUiPreferences preferences = getPreferences(targetUser);
        preferences.setAccentPreset(accentPreset != null ? accentPreset : UiAccentPreset.BLUE);
        preferences.setDensityMode(densityMode != null ? densityMode : UiDensityMode.COMFORTABLE);
        preferences.setReducedMotion(reducedMotion);
        preferences.setSidebarCollapsedByDefault(sidebarCollapsedByDefault);
        preferences.setDashboardSectionOrder(serializeSections(normalizedOrder));
        preferences.setDashboardHiddenSections(serializeSections(orderSectionsByDefault(normalizedHidden)));
        return userUiPreferencesRepository.save(preferences);
    }

    public List<DashboardSectionKey> resolveDashboardSectionOrder(User user) {
        return normalizeDashboardOrder(parseSections(getPreferences(user).getDashboardSectionOrder()));
    }

    public Set<DashboardSectionKey> resolveHiddenDashboardSections(User user) {
        return normalizeHiddenSections(parseSections(getPreferences(user).getDashboardHiddenSections()));
    }

    public List<DashboardSectionKey> getDefaultDashboardOrder() {
        return List.copyOf(DEFAULT_DASHBOARD_ORDER);
    }

    private UserUiPreferences createDefaultPreferences(User user) {
        UserUiPreferences preferences = new UserUiPreferences();
        preferences.setUser(user);
        preferences.setAccentPreset(UiAccentPreset.BLUE);
        preferences.setDensityMode(UiDensityMode.COMFORTABLE);
        preferences.setReducedMotion(false);
        preferences.setSidebarCollapsedByDefault(false);
        preferences.setDashboardHiddenSections("");
        preferences.setDashboardSectionOrder("");
        return preferences;
    }

    private List<DashboardSectionKey> normalizeDashboardOrder(Collection<DashboardSectionKey> requestedOrder) {
        LinkedHashSet<DashboardSectionKey> normalized = new LinkedHashSet<>();
        if (requestedOrder != null) {
            requestedOrder.stream()
                .filter(section -> section != null && DEFAULT_DASHBOARD_ORDER.contains(section))
                .forEach(normalized::add);
        }
        DEFAULT_DASHBOARD_ORDER.forEach(normalized::add);
        return new ArrayList<>(normalized);
    }

    private Set<DashboardSectionKey> normalizeHiddenSections(Collection<DashboardSectionKey> hiddenSections) {
        LinkedHashSet<DashboardSectionKey> normalized = new LinkedHashSet<>();
        if (hiddenSections == null) {
            return normalized;
        }
        hiddenSections.stream()
            .filter(section -> section != null && DEFAULT_DASHBOARD_ORDER.contains(section))
            .forEach(normalized::add);
        return normalized;
    }

    private List<DashboardSectionKey> parseSections(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(token -> !token.isBlank())
            .map(this::parseSection)
            .filter(java.util.Objects::nonNull)
            .toList();
    }

    private DashboardSectionKey parseSection(String token) {
        try {
            return DashboardSectionKey.valueOf(token.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String serializeSections(Collection<DashboardSectionKey> sections) {
        if (sections == null || sections.isEmpty()) {
            return "";
        }
        return sections.stream()
            .map(Enum::name)
            .collect(Collectors.joining(","));
    }

    private List<DashboardSectionKey> orderSectionsByDefault(Set<DashboardSectionKey> sections) {
        return DEFAULT_DASHBOARD_ORDER.stream()
            .filter(sections::contains)
            .toList();
    }
}
