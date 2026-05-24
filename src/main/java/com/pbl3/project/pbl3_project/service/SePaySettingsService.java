package com.pbl3.project.pbl3_project.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.pbl3.project.pbl3_project.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class SePaySettingsService {
    private static final String ENABLED_KEY = "payment.sepay.enabled";
    private static final String API_TOKEN_KEY = "payment.sepay.api_token";
    private static final String WEBHOOK_API_KEY_KEY = "payment.sepay.webhook_api_key";
    private static final String BANK_SHORT_NAME_KEY = "payment.sepay.bank_short_name";
    private static final String ACCOUNT_NUMBER_KEY = "payment.sepay.account_number";
    private static final Duration CHECKOUT_AVAILABILITY_CACHE_TTL = Duration.ofSeconds(60);

    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final AuthorizationService authorizationService;
    private final RestClient restClient;
    private final String apiBaseUrl;
    private final String defaultApiToken;
    private final String defaultWebhookApiKey;
    private final String defaultBankShortName;
    private final String defaultAccountNumber;
    private volatile CheckoutAvailability cachedCheckoutAvailability;
    private volatile String cachedCheckoutAvailabilityFingerprint;
    private volatile Instant cachedCheckoutAvailabilityAt;

    public SePaySettingsService(
        org.springframework.jdbc.core.JdbcTemplate jdbcTemplate,
        AuthorizationService authorizationService,
        RestClient.Builder restClientBuilder,
        @Value("${sepay.base-url:https://userapi.sepay.vn/v2}") String baseUrl,
        @Value("${sepay.api-token:${SEPAY_API_TOKEN:}}") String defaultApiToken,
        @Value("${sepay.webhook-api-key:${SEPAY_WEBHOOK_API_KEY:}}") String defaultWebhookApiKey,
        @Value("${sepay.bank:${SEPAY_BANK:}}") String defaultBankShortName,
        @Value("${sepay.account-number:${SEPAY_ACCOUNT_NUMBER:}}") String defaultAccountNumber
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.authorizationService = authorizationService;
        this.apiBaseUrl = trimTrailingSlash(baseUrl);
        this.restClient = restClientBuilder.baseUrl(apiBaseUrl).build();
        this.defaultApiToken = normalize(defaultApiToken);
        this.defaultWebhookApiKey = normalize(defaultWebhookApiKey);
        this.defaultBankShortName = normalize(defaultBankShortName);
        this.defaultAccountNumber = normalize(defaultAccountNumber);
    }

    public SePaySettings getSettings(User actor) {
        authorizationService.requireAdmin(actor);
        return resolveEffectiveSettings();
    }

    public SePaySettings resolveEffectiveSettings() {
        Map<String, String> values = readSettings(List.of(
            ENABLED_KEY,
            API_TOKEN_KEY,
            WEBHOOK_API_KEY_KEY,
            BANK_SHORT_NAME_KEY,
            ACCOUNT_NUMBER_KEY
        ));
        Optional<String> enabledValue = Optional.ofNullable(values.get(ENABLED_KEY));
        String apiToken = firstNonBlank(values.get(API_TOKEN_KEY), defaultApiToken);
        String webhookApiKey = firstNonBlank(values.get(WEBHOOK_API_KEY_KEY), defaultWebhookApiKey);
        String bankShortName = firstNonBlank(values.get(BANK_SHORT_NAME_KEY), defaultBankShortName);
        String accountNumber = firstNonBlank(values.get(ACCOUNT_NUMBER_KEY), defaultAccountNumber);
        boolean enabled = enabledValue
            .map(this::parseBoolean)
            .orElse(!apiToken.isBlank() && !bankShortName.isBlank() && !accountNumber.isBlank());
        return new SePaySettings(enabled, apiToken, webhookApiKey, bankShortName, accountNumber);
    }

    @Transactional
    public SePaySettings saveSettings(User actor, SePaySettings settings) {
        authorizationService.requireAdmin(actor);
        SePaySettings normalized = normalizeSettings(settings);
        if (normalized.enabled()) {
            requireComplete(normalized);
        }
        jdbcTemplate.update(
            "DELETE FROM system_settings WHERE setting_key IN (?, ?, ?, ?, ?)",
            ENABLED_KEY,
            API_TOKEN_KEY,
            WEBHOOK_API_KEY_KEY,
            BANK_SHORT_NAME_KEY,
            ACCOUNT_NUMBER_KEY
        );
        insertSetting(ENABLED_KEY, Boolean.toString(normalized.enabled()), "Enable automatic QR payment confirmation with SePay.");
        insertSetting(API_TOKEN_KEY, normalized.apiToken(), "SePay API token for transaction polling.");
        insertSetting(WEBHOOK_API_KEY_KEY, normalized.webhookApiKey(), "SePay webhook API key for inbound payment verification.");
        insertSetting(BANK_SHORT_NAME_KEY, normalized.bankShortName(), "SePay/VietQR bank short name.");
        insertSetting(ACCOUNT_NUMBER_KEY, normalized.accountNumber(), "Receiving bank account number for SePay QR payments.");
        clearCheckoutAvailabilityCache();
        return normalized;
    }

    public void testConnection(User actor, SePaySettings settings) {
        authorizationService.requireAdmin(actor);
        validateConnection(normalizeSettings(settings));
    }

    public CheckoutAvailability checkCheckoutAvailability() {
        SePaySettings settings = resolveEffectiveSettings();
        String fingerprint = checkoutAvailabilityFingerprint(settings);
        CheckoutAvailability cached = cachedCheckoutAvailability;
        Instant cachedAt = cachedCheckoutAvailabilityAt;
        if (cached != null
            && cachedAt != null
            && fingerprint.equals(cachedCheckoutAvailabilityFingerprint)
            && Duration.between(cachedAt, Instant.now()).compareTo(CHECKOUT_AVAILABILITY_CACHE_TTL) < 0) {
            return cached;
        }

        CheckoutAvailability availability = computeCheckoutAvailability(settings);
        cachedCheckoutAvailability = availability;
        cachedCheckoutAvailabilityFingerprint = fingerprint;
        cachedCheckoutAvailabilityAt = Instant.now();
        return availability;
    }

    private CheckoutAvailability computeCheckoutAvailability(SePaySettings settings) {
        SePaySettings normalized = normalizeSettings(settings);
        if (!normalized.enabled()) {
            return new CheckoutAvailability(false, "SePay QR payment is disabled in Settings.");
        }
        try {
            validateConnection(normalized);
            return new CheckoutAvailability(true, "SePay QR payment is ready.");
        } catch (RuntimeException ex) {
            String message = ex.getMessage() != null && !ex.getMessage().isBlank()
                ? ex.getMessage()
                : "SePay QR payment is not available.";
            return new CheckoutAvailability(false, message);
        }
    }

    private void validateConnection(SePaySettings settings) {
        SePaySettings normalized = normalizeSettings(settings);
        requireComplete(normalized);
        try {
            JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path(bankAccountsPath())
                    .queryParam(isV2Api() ? "bank_short_name" : "short_name", normalized.bankShortName())
                    .queryParam(isV2Api() ? "per_page" : "limit", 100)
                    .build())
                .header("Authorization", "Bearer " + normalized.apiToken())
                .retrieve()
                .body(JsonNode.class);
            if (looksLikeAuthFailure(response == null ? "" : response.toString())) {
                throw new QrPaymentException("SePay rejected the supplied API token");
            }
            if (!containsConfiguredAccount(response, normalized)) {
                throw new QrPaymentException("SePay token is valid, but the configured bank account was not found");
            }
        } catch (RestClientResponseException ex) {
            if (isAuthStatus(ex.getStatusCode()) || looksLikeAuthFailure(ex.getResponseBodyAsString())) {
                throw new QrPaymentException("SePay rejected the API Access token. Recheck the token and whether it is live or test mode.", ex);
            }
            throw new QrPaymentException("Could not validate SePay bank account: " + ex.getResponseBodyAsString(), ex);
        } catch (ResourceAccessException ex) {
            throw new QrPaymentException("Could not reach SePay. Check the network connection.", ex);
        }
    }

    private boolean containsConfiguredAccount(JsonNode response, SePaySettings settings) {
        JsonNode accounts = response == null ? null : firstArray(response, "data", "bankaccounts");
        if (accounts == null || !accounts.isArray()) {
            return false;
        }
        for (JsonNode account : accounts) {
            String accountNumber = text(account, "account_number");
            String bankShortName = text(account, "bank_short_name");
            if (settings.accountNumber().equalsIgnoreCase(accountNumber)
                && settings.bankShortName().equalsIgnoreCase(bankShortName)) {
                return true;
            }
        }
        return false;
    }

    private JsonNode firstArray(JsonNode response, String... fieldNames) {
        if (response == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode value = response.get(fieldName);
            if (value != null && value.isArray()) {
                return value;
            }
        }
        return null;
    }

    private String text(JsonNode node, String field) {
        return node != null && node.hasNonNull(field) ? normalize(node.get(field).asText()) : "";
    }

    private void insertSetting(String key, String value, String description) {
        jdbcTemplate.update(
            "INSERT INTO system_settings (setting_key, setting_value, description, updated_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
            key,
            value == null ? "" : value,
            description == null ? "" : description
        );
    }

    private Optional<String> readSetting(String key) {
        try {
            List<String> values = jdbcTemplate.query(
                "SELECT setting_value FROM system_settings WHERE setting_key = ?",
                (rs, rowNum) -> rs.getString("setting_value"),
                key
            );
            return values.stream().findFirst().map(this::normalize);
        } catch (DataAccessException ex) {
            return Optional.empty();
        }
    }

    private Map<String, String> readSettings(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Map.of();
        }
        String placeholders = String.join(", ", keys.stream().map(key -> "?").toList());
        try {
            return jdbcTemplate.query(
                "SELECT setting_key, setting_value FROM system_settings WHERE setting_key IN (" + placeholders + ")",
                rs -> {
                    Map<String, String> values = new LinkedHashMap<>();
                    while (rs.next()) {
                        values.put(rs.getString("setting_key"), normalize(rs.getString("setting_value")));
                    }
                    return values;
                },
                keys.toArray()
            );
        } catch (DataAccessException ex) {
            return Map.of();
        }
    }

    private SePaySettings normalizeSettings(SePaySettings settings) {
        if (settings == null) {
            return new SePaySettings(false, "", "", "", "");
        }
        return new SePaySettings(
            settings.enabled(),
            normalize(settings.apiToken()),
            normalize(settings.webhookApiKey()),
            normalize(settings.bankShortName()),
            normalize(settings.accountNumber())
        );
    }

    private void requireComplete(SePaySettings settings) {
        List<String> missingFields = new ArrayList<>();
        if (settings.apiToken().isBlank()) {
            missingFields.add("API token");
        }
        if (settings.bankShortName().isBlank()) {
            missingFields.add("bank");
        }
        if (settings.accountNumber().isBlank()) {
            missingFields.add("account number");
        }
        if (!missingFields.isEmpty()) {
            throw new ValidationException(String.join(", ", missingFields) + " required for SePay.");
        }
    }

    private void clearCheckoutAvailabilityCache() {
        cachedCheckoutAvailability = null;
        cachedCheckoutAvailabilityFingerprint = null;
        cachedCheckoutAvailabilityAt = null;
    }

    private String checkoutAvailabilityFingerprint(SePaySettings settings) {
        SePaySettings normalized = normalizeSettings(settings);
        return normalized.enabled() + "|"
            + normalized.apiToken() + "|"
            + normalized.bankShortName() + "|"
            + normalized.accountNumber();
    }

    private boolean isAuthStatus(HttpStatusCode statusCode) {
        return statusCode != null && (statusCode.value() == 401 || statusCode.value() == 403);
    }

    private boolean looksLikeAuthFailure(String body) {
        if (body == null || body.isBlank()) {
            return false;
        }
        String normalized = body.toLowerCase(Locale.ROOT);
        return normalized.contains("unauthorized")
            || normalized.contains("forbidden")
            || normalized.contains("invalid token")
            || normalized.contains("api token")
            || normalized.contains("bearer");
    }

    private String bankAccountsPath() {
        return isV2Api() ? "/bank-accounts" : "/userapi/bankaccounts/list";
    }

    private boolean isV2Api() {
        return apiBaseUrl.contains("userapi.sepay.vn/v2") || apiBaseUrl.endsWith("/v2");
    }

    private boolean parseBoolean(String value) {
        return value != null && ("true".equalsIgnoreCase(value.trim()) || "1".equals(value.trim()));
    }

    private String firstNonBlank(String first, String fallback) {
        String normalizedFirst = normalize(first);
        return normalizedFirst.isBlank() ? normalize(fallback) : normalizedFirst;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().replaceAll("/+$", "");
    }

    public record SePaySettings(
        boolean enabled,
        String apiToken,
        String webhookApiKey,
        String bankShortName,
        String accountNumber
    ) {
        public boolean configured() {
            return apiToken != null && !apiToken.isBlank()
                && bankShortName != null && !bankShortName.isBlank()
                && accountNumber != null && !accountNumber.isBlank();
        }
    }

    public record CheckoutAvailability(boolean available, String message) {
    }
}
