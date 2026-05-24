package com.pbl3.project.pbl3_project.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class ApplicationVersionService {

    private static final String MINIMUM_CLIENT_VERSION_KEY = "minimum_client_version";

    private final JdbcTemplate jdbcTemplate;
    private final String currentClientVersion;
    private final boolean versionGateEnabled;

    public ApplicationVersionService(
        JdbcTemplate jdbcTemplate,
        @Value("${app.client.version:0.0.1-SNAPSHOT}") String currentClientVersion,
        @Value("${app.version-gate.enabled:true}") boolean versionGateEnabled
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.currentClientVersion = currentClientVersion == null || currentClientVersion.isBlank()
            ? "0.0.1-SNAPSHOT"
            : currentClientVersion.trim();
        this.versionGateEnabled = versionGateEnabled;
    }

    public VersionCheckResult checkClientCompatibility() {
        if (!versionGateEnabled) {
            return VersionCheckResult.compatible(currentClientVersion, null);
        }

        Optional<String> minimumVersion = readSetting(MINIMUM_CLIENT_VERSION_KEY);
        if (minimumVersion.isEmpty()) {
            return VersionCheckResult.blocked(
                currentClientVersion,
                null,
                "Database version control is not installed. Run the migration tool before using this client."
            );
        }

        String requiredVersion = minimumVersion.get();
        if (compareVersions(currentClientVersion, requiredVersion) < 0) {
            return VersionCheckResult.blocked(
                currentClientVersion,
                requiredVersion,
                "This app version is no longer allowed. Please update the desktop client."
            );
        }
        return VersionCheckResult.compatible(currentClientVersion, requiredVersion);
    }

    private Optional<String> readSetting(String key) {
        try {
            List<String> values = jdbcTemplate.query(
                "SELECT setting_value FROM system_settings WHERE setting_key = ?",
                (rs, rowNum) -> rs.getString("setting_value"),
                key
            );
            return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .findFirst();
        } catch (DataAccessException ex) {
            return Optional.empty();
        }
    }

    private int compareVersions(String actual, String required) {
        List<Integer> actualParts = numericParts(actual);
        List<Integer> requiredParts = numericParts(required);
        int max = Math.max(actualParts.size(), requiredParts.size());
        for (int i = 0; i < max; i++) {
            int actualPart = i < actualParts.size() ? actualParts.get(i) : 0;
            int requiredPart = i < requiredParts.size() ? requiredParts.get(i) : 0;
            if (actualPart != requiredPart) {
                return Integer.compare(actualPart, requiredPart);
            }
        }
        return 0;
    }

    private List<Integer> numericParts(String version) {
        List<Integer> parts = new ArrayList<>();
        if (version == null || version.isBlank()) {
            parts.add(0);
            return parts;
        }
        String normalized = version.toLowerCase(Locale.ROOT).replaceAll("[^0-9]+", ".");
        for (String token : normalized.split("\\.")) {
            if (token.isBlank()) {
                continue;
            }
            try {
                parts.add(Integer.parseInt(token));
            } catch (NumberFormatException ignored) {
                parts.add(0);
            }
        }
        if (parts.isEmpty()) {
            parts.add(0);
        }
        return parts;
    }

    public record VersionCheckResult(
        boolean compatible,
        String currentVersion,
        String requiredVersion,
        String message
    ) {
        static VersionCheckResult compatible(String currentVersion, String requiredVersion) {
            return new VersionCheckResult(true, currentVersion, requiredVersion, null);
        }

        static VersionCheckResult blocked(String currentVersion, String requiredVersion, String message) {
            return new VersionCheckResult(false, currentVersion, requiredVersion, message);
        }
    }
}
