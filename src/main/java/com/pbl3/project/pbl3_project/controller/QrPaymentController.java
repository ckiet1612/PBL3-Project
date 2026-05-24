package com.pbl3.project.pbl3_project.controller;

import com.pbl3.project.pbl3_project.dto.payment.QrPaymentCreateRequest;
import com.pbl3.project.pbl3_project.dto.payment.SePayWebhookPayload;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.service.ApiSessionService;
import com.pbl3.project.pbl3_project.service.QrPaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class QrPaymentController {
    private final QrPaymentService qrPaymentService;
    private final ApiSessionService apiSessionService;

    public QrPaymentController(QrPaymentService qrPaymentService, ApiSessionService apiSessionService) {
        this.qrPaymentService = qrPaymentService;
        this.apiSessionService = apiSessionService;
    }

    @PostMapping("/qr-payments")
    public ResponseEntity<?> createPayment(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody QrPaymentCreateRequest request
    ) {
        User actor = apiSessionService.requireUser(authorizationHeader);
        request.setUserId(actor.getId());
        return ResponseEntity.ok(qrPaymentService.createPayment(request));
    }

    @GetMapping("/qr-payments/{id}")
    public ResponseEntity<?> getPaymentStatus(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long id
    ) {
        User actor = apiSessionService.requireUser(authorizationHeader);
        return ResponseEntity.ok(qrPaymentService.getPaymentStatus(actor, id));
    }

    @PostMapping("/qr-payments/{id}/refresh")
    public ResponseEntity<?> refreshPaymentStatus(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long id
    ) {
        User actor = apiSessionService.requireUser(authorizationHeader);
        return ResponseEntity.ok(qrPaymentService.refreshPaymentStatus(actor, id));
    }

    @PostMapping("/qr-payments/{id}/cancel")
    public ResponseEntity<?> cancelPayment(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long id
    ) {
        User actor = apiSessionService.requireUser(authorizationHeader);
        return ResponseEntity.ok(qrPaymentService.cancelPayment(actor, id));
    }

    @PostMapping("/payments/sepay/webhook")
    public ResponseEntity<java.util.Map<String, Boolean>> handleSePayWebhook(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody SePayWebhookPayload payload
    ) {
        qrPaymentService.handleSePayWebhook(payload, authorizationHeader, null);
        return ResponseEntity.ok(java.util.Map.of("success", true));
    }

    @GetMapping("/qr-payments/return")
    public ResponseEntity<String> paymentReturn() {
        return ResponseEntity.ok("Payment received. You can return to POS.");
    }

    @GetMapping("/qr-payments/cancelled")
    public ResponseEntity<String> paymentCancelled() {
        return ResponseEntity.ok("Payment cancelled. You can return to POS.");
    }
}
