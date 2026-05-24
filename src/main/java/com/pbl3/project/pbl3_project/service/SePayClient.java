package com.pbl3.project.pbl3_project.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.pbl3.project.pbl3_project.dto.payment.SePayWebhookPayload;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

@Service
public class SePayClient implements QrPaymentGateway {

    private final RestClient restClient;
    private final SePaySettingsService settingsService;
    private final String apiBaseUrl;
    private final String qrBaseUrl;
    private final int paymentExpirySeconds;

    public SePayClient(
        RestClient.Builder restClientBuilder,
        SePaySettingsService settingsService,
        @Value("${sepay.base-url:https://userapi.sepay.vn/v2}") String baseUrl,
        @Value("${sepay.qr-base-url:https://qr.sepay.vn}") String qrBaseUrl,
        @Value("${sepay.qr.expire-seconds:300}") int paymentExpirySeconds
    ) {
        this.apiBaseUrl = trimTrailingSlash(baseUrl);
        this.restClient = restClientBuilder.baseUrl(apiBaseUrl).build();
        this.settingsService = settingsService;
        this.qrBaseUrl = trimTrailingSlash(qrBaseUrl == null || qrBaseUrl.isBlank() ? "https://qr.sepay.vn" : qrBaseUrl);
        this.paymentExpirySeconds = Math.max(60, paymentExpirySeconds);
    }

    @Override
    public QrPaymentLink createPaymentLink(QrPaymentRequest request) {
        SePaySettingsService.SePaySettings settings = activeSettings();
        requireConfigured(settings);
        long amount = toVndAmount(request.amount());
        String paymentCode = requireDescription(request.description());
        String qrUrl = qrBaseUrl + "/img?acc=" + encode(settings.accountNumber())
            + "&bank=" + encode(settings.bankShortName())
            + "&amount=" + amount
            + "&des=" + encode(paymentCode);
        return new QrPaymentLink(paymentCode, qrUrl, qrUrl, "PENDING");
    }

    @Override
    public QrPaymentProviderStatus getPaymentStatus(Long orderCode) {
        SePaySettingsService.SePaySettings settings = activeSettings();
        requireConfigured(settings);
        JsonNode response;
        try {
            response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path(transactionsPath())
                    .queryParam("account_number", settings.accountNumber())
                    .queryParam(isV2Api() ? "per_page" : "limit", 100)
                    .build())
                .header("Authorization", "Bearer " + settings.apiToken())
                .retrieve()
                .body(JsonNode.class);
        } catch (RestClientResponseException ex) {
            if (isAuthStatus(ex.getStatusCode())) {
                throw new QrPaymentException("SePay rejected the API Access token. Recheck Settings.", ex);
            }
            throw ex;
        }
        JsonNode transactions = response == null ? null : firstArray(response, "data", "transactions");
        if (transactions == null || !transactions.isArray()) {
            return new QrPaymentProviderStatus("PENDING", MoneySupport.ZERO);
        }
        String expectedCode = buildPaymentCode(orderCode);
        for (JsonNode transaction : transactions) {
            BigDecimal amountIn = decimal(transaction, "amount_in");
            if (amountIn.compareTo(MoneySupport.ZERO) <= 0) {
                continue;
            }
            if (matchesPaymentCode(transaction, expectedCode)) {
                return new QrPaymentProviderStatus("PAID", amountIn);
            }
        }
        return new QrPaymentProviderStatus("PENDING", MoneySupport.ZERO);
    }

    @Override
    public void cancelPayment(Long orderCode, String reason) {
        // SePay QR transfers are bank transfers generated from a QR image. There is no remote payment link to cancel.
    }

    @Override
    public boolean verifyWebhook(SePayWebhookPayload payload, String authorizationHeader, String rawBody) {
        if (payload == null || !payload.isIncoming()) {
            return false;
        }
        SePaySettingsService.SePaySettings settings = activeSettings();
        requireConfigured(settings);
        if (settings.webhookApiKey() == null || settings.webhookApiKey().isBlank()) {
            return false;
        }
        String expected = "Apikey " + settings.webhookApiKey();
        return authorizationHeader != null
            && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                authorizationHeader.trim().getBytes(StandardCharsets.UTF_8)
            );
    }

    @Override
    public int paymentExpirySeconds() {
        return paymentExpirySeconds;
    }

    private boolean matchesPaymentCode(JsonNode transaction, String expectedCode) {
        return containsCode(text(transaction, "code"), expectedCode)
            || containsCode(text(transaction, "transaction_content"), expectedCode)
            || containsCode(text(transaction, "content"), expectedCode)
            || containsCode(text(transaction, "description"), expectedCode);
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

    private boolean containsCode(String value, String expectedCode) {
        return value != null && expectedCode != null
            && value.toUpperCase(Locale.ROOT).contains(expectedCode.toUpperCase(Locale.ROOT));
    }

    private BigDecimal decimal(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) {
            return MoneySupport.ZERO;
        }
        try {
            return MoneySupport.normalize(node.get(field).decimalValue());
        } catch (RuntimeException ex) {
            return MoneySupport.ZERO;
        }
    }

    private String text(JsonNode node, String field) {
        return node != null && node.hasNonNull(field) ? node.get(field).asText() : "";
    }

    private SePaySettingsService.SePaySettings activeSettings() {
        return settingsService.resolveEffectiveSettings();
    }

    private void requireConfigured(SePaySettingsService.SePaySettings settings) {
        if (settings == null || !settings.enabled()) {
            throw new QrPaymentException("SePay QR payment is disabled in Settings");
        }
        if (!settings.configured()) {
            throw new QrPaymentException("SePay credentials are not configured");
        }
    }

    private long toVndAmount(BigDecimal amount) {
        BigDecimal normalized = MoneySupport.normalize(amount);
        if (normalized.compareTo(MoneySupport.ZERO) <= 0) {
            throw new QrPaymentException("QR payment amount must be greater than zero");
        }
        return normalized.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private String requireDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new QrPaymentException("QR payment code is required");
        }
        return description.trim();
    }

    public static String buildPaymentCode(Long orderCode) {
        if (orderCode == null) {
            return "";
        }
        return "PBL" + orderCode;
    }

    private String transactionsPath() {
        return isV2Api() ? "/transactions" : "/userapi/transactions/list";
    }

    private boolean isV2Api() {
        return apiBaseUrl.contains("userapi.sepay.vn/v2") || apiBaseUrl.endsWith("/v2");
    }

    private boolean isAuthStatus(HttpStatusCode statusCode) {
        return statusCode != null && (statusCode.value() == 401 || statusCode.value() == 403);
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().replaceAll("/+$", "");
    }
}
