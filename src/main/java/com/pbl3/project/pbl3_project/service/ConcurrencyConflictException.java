package com.pbl3.project.pbl3_project.service;

public class ConcurrencyConflictException extends RuntimeException {
    public ConcurrencyConflictException(String message) {
        super(message);
    }
}
