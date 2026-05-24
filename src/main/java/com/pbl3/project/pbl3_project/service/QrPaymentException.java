package com.pbl3.project.pbl3_project.service;

public class QrPaymentException extends RuntimeException {
    public QrPaymentException(String message) {
        super(message);
    }

    public QrPaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
