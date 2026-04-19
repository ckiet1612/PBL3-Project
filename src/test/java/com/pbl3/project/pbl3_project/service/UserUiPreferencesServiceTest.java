package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.entity.DashboardSectionKey;
import com.pbl3.project.pbl3_project.entity.Role;
import com.pbl3.project.pbl3_project.entity.UiAccentPreset;
import com.pbl3.project.pbl3_project.entity.UiDensityMode;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.entity.UserUiPreferences;
import com.pbl3.project.pbl3_project.repository.UserUiPreferencesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserUiPreferencesServiceTest {

    @Mock
    private UserUiPreferencesRepository userUiPreferencesRepository;

    private UserUiPreferencesService userUiPreferencesService;

    @BeforeEach
    void setUp() {
        userUiPreferencesService = new UserUiPreferencesService(
            userUiPreferencesRepository,
            new AuthorizationService()
        );
    }

    @Test
    void getPreferencesCreatesDefaultRowWhenMissing() {
        User user = user(1L, Role.STAFF);

        when(userUiPreferencesRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(userUiPreferencesRepository.save(any(UserUiPreferences.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserUiPreferences preferences = userUiPreferencesService.getPreferences(user);

        assertEquals(UiAccentPreset.BLUE, preferences.getAccentPreset());
        assertEquals(UiDensityMode.COMFORTABLE, preferences.getDensityMode());
        assertFalse(preferences.isReducedMotion());
        assertFalse(preferences.isSidebarCollapsedByDefault());
        assertEquals("", preferences.getDashboardSectionOrder());
        assertEquals("", preferences.getDashboardHiddenSections());
        assertEquals(user, preferences.getUser());
    }

    @Test
    void updatePreferencesPersistsOwnPersonalization() {
        User user = user(2L, Role.MANAGER);
        UserUiPreferences existing = new UserUiPreferences();
        existing.setUser(user);

        when(userUiPreferencesRepository.findByUserId(user.getId())).thenReturn(Optional.of(existing));
        when(userUiPreferencesRepository.save(any(UserUiPreferences.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserUiPreferences saved = userUiPreferencesService.updatePreferences(
            user,
            user,
            UiAccentPreset.EMERALD,
            UiDensityMode.COMPACT,
            true,
            true,
            List.of(DashboardSectionKey.TOP_SELLING, DashboardSectionKey.KPI_ROW),
            new LinkedHashSet<>(Set.of(DashboardSectionKey.LOW_STOCK, DashboardSectionKey.WHAT_CHANGED))
        );

        assertEquals(UiAccentPreset.EMERALD, saved.getAccentPreset());
        assertEquals(UiDensityMode.COMPACT, saved.getDensityMode());
        assertTrue(saved.isReducedMotion());
        assertTrue(saved.isSidebarCollapsedByDefault());
        assertTrue(saved.getDashboardSectionOrder().startsWith("TOP_SELLING,KPI_ROW"));
        assertEquals("WHAT_CHANGED,LOW_STOCK", saved.getDashboardHiddenSections());

        ArgumentCaptor<UserUiPreferences> captor = ArgumentCaptor.forClass(UserUiPreferences.class);
        verify(userUiPreferencesRepository).save(captor.capture());
        assertEquals(UiAccentPreset.EMERALD, captor.getValue().getAccentPreset());
    }

    @Test
    void updatePreferencesRejectsEditingAnotherUsersSettings() {
        User actor = user(3L, Role.ADMIN);
        User target = user(4L, Role.STAFF);

        AuthorizationException ex = assertThrows(
            AuthorizationException.class,
            () -> userUiPreferencesService.updatePreferences(
                actor,
                target,
                UiAccentPreset.AMBER,
                UiDensityMode.COMFORTABLE,
                false,
                false,
                List.of(DashboardSectionKey.KPI_ROW),
                Set.of()
            )
        );

        assertEquals("You can only edit your own Settings", ex.getMessage());
    }

    @Test
    void resolveDashboardPreferencesIgnoreInvalidTokensAndAppendMissingDefaults() {
        User user = user(5L, Role.STAFF);
        UserUiPreferences preferences = new UserUiPreferences();
        preferences.setUser(user);
        preferences.setDashboardSectionOrder("TOP_SELLING,INVALID,WHAT_CHANGED");
        preferences.setDashboardHiddenSections("LOW_STOCK,NOPE,ACTION_CENTER");

        when(userUiPreferencesRepository.findByUserId(user.getId())).thenReturn(Optional.of(preferences));

        List<DashboardSectionKey> resolvedOrder = userUiPreferencesService.resolveDashboardSectionOrder(user);
        Set<DashboardSectionKey> resolvedHidden = userUiPreferencesService.resolveHiddenDashboardSections(user);

        assertEquals(DashboardSectionKey.TOP_SELLING, resolvedOrder.get(0));
        assertEquals(DashboardSectionKey.WHAT_CHANGED, resolvedOrder.get(1));
        assertEquals(
            userUiPreferencesService.getDefaultDashboardOrder().size(),
            new LinkedHashSet<>(resolvedOrder).size()
        );
        assertEquals(Set.of(DashboardSectionKey.LOW_STOCK, DashboardSectionKey.ACTION_CENTER), resolvedHidden);
    }

    private User user(Long id, Role role) {
        return new User(id, "user" + id, "secret", "User " + id, role, true);
    }
}
