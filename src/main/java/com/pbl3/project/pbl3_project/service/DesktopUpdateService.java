package com.pbl3.project.pbl3_project.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DesktopUpdateService {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String provisioningApiBaseUrl;
    private final String provisioningApiKey;
    private final String currentClientVersion;

    public DesktopUpdateService(
        ObjectMapper objectMapper,
        @Value("${provisioning.api.base-url:}") String provisioningApiBaseUrl,
        @Value("${provisioning.api-key:}") String provisioningApiKey,
        @Value("${app.client.version:0.0.1-SNAPSHOT}") String currentClientVersion
    ) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();
        this.provisioningApiBaseUrl = normalize(provisioningApiBaseUrl);
        this.provisioningApiKey = normalize(provisioningApiKey);
        this.currentClientVersion = normalize(currentClientVersion).isBlank()
            ? "0.0.1-SNAPSHOT"
            : normalize(currentClientVersion);
    }

    public Optional<DesktopUpdate> checkForUpdate() {
        if (provisioningApiBaseUrl.isBlank()) {
            return Optional.empty();
        }

        try {
            JsonNode json = fetchUpdateJson();
            String latestVersion = normalize(json.path("latestVersion").asText(""));
            String minSupportedVersion = normalize(json.path("minSupportedVersion").asText(""));
            String downloadUrl = resolveDownloadUrl(json);
            String releaseNotes = normalize(json.path("releaseNotes").asText(""));
            if (latestVersion.isBlank()
                || downloadUrl.isBlank()
                || compareVersions(currentClientVersion, latestVersion) >= 0) {
                return Optional.empty();
            }
            return Optional.of(new DesktopUpdate(
                currentClientVersion,
                latestVersion,
                minSupportedVersion.isBlank() ? null : minSupportedVersion,
                downloadUrl,
                releaseNotes.isBlank() ? null : releaseNotes,
                !minSupportedVersion.isBlank() && compareVersions(currentClientVersion, minSupportedVersion) < 0
            ));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private JsonNode fetchUpdateJson() throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(provisioningApiBaseUrl + "/api/provisioning/app-update"))
            .timeout(REQUEST_TIMEOUT)
            .GET();
        if (!provisioningApiKey.isBlank()) {
            builder.header("X-Provisioning-Key", provisioningApiKey);
        }
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Update check failed with HTTP " + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }

    private String resolveDownloadUrl(JsonNode json) {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (osName.contains("mac")) {
            return firstNonBlank(json.path("downloadUrlMac").asText(""), json.path("downloadUrl").asText(""));
        }
        if (osName.contains("win")) {
            return firstNonBlank(json.path("downloadUrlWindows").asText(""), json.path("downloadUrl").asText(""));
        }
        return normalize(json.path("downloadUrl").asText(""));
    }

    public URI buildDownloadUri(DesktopUpdate update) {
        if (update == null || update.downloadUrl() == null || update.downloadUrl().isBlank()) {
            return URI.create(provisioningApiBaseUrl);
        }
        return URI.create(update.downloadUrl());
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

    private String firstNonBlank(String first, String second) {
        String normalizedFirst = normalize(first);
        return normalizedFirst.isBlank() ? normalize(second) : normalizedFirst;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public record DesktopUpdate(
        String currentVersion,
        String latestVersion,
        String minSupportedVersion,
        String downloadUrl,
        String releaseNotes,
        boolean mandatory
    ) {
    }
}
