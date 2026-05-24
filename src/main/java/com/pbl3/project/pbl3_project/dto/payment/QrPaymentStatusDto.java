package com.pbl3.project.pbl3_project.dto.payment;

import com.pbl3.project.pbl3_project.entity.QrPayment;
import com.pbl3.project.pbl3_project.entity.QrPaymentStatus;
import com.pbl3.project.pbl3_project.service.MoneySupport;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record QrPaymentStatusDto(
    Long id,
    Long orderCode,
    String paymentLinkId,
    QrPaymentStatus status,
    BigDecimal amount,
    String qrCode,
    String checkoutUrl,
    LocalDateTime expiresAt,
    LocalDateTime paidAt,
    Long createdOrderId,
    String failureReason
) {
    public static QrPaymentStatusDto from(QrPayment payment) {
        return new QrPaymentStatusDto(
            payment.getId(),
            payment.getOrderCode(),
            payment.getPaymentLinkId(),
            payment.getStatus(),
            MoneySupport.normalize(payment.getAmount()),
            payment.getQrCode(),
            payment.getCheckoutUrl(),
            payment.getExpiresAt(),
            payment.getPaidAt(),
            payment.getCreatedOrder() != null ? payment.getCreatedOrder().getId() : null,
            payment.getFailureReason()
        );
    }
}
