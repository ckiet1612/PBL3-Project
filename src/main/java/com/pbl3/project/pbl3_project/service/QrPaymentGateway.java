package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.dto.payment.SePayWebhookPayload;

import java.math.BigDecimal;

public interface QrPaymentGateway {
    QrPaymentLink createPaymentLink(QrPaymentRequest request);

    QrPaymentProviderStatus getPaymentStatus(Long orderCode);

    void cancelPayment(Long orderCode, String reason);

    boolean verifyWebhook(SePayWebhookPayload payload, String authorizationHeader, String rawBody);

    int paymentExpirySeconds();

    record QrPaymentRequest(
        Long orderCode,
        BigDecimal amount,
        String description,
        String cancelUrl,
        String returnUrl
    ) {
    }

    record QrPaymentLink(
        String paymentLinkId,
        String qrCode,
        String checkoutUrl,
        String status
    ) {
    }

    record QrPaymentProviderStatus(
        String status,
        BigDecimal amountPaid
    ) {
    }
}
