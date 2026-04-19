package com.pbl3.project.pbl3_project.controller;

import com.pbl3.project.pbl3_project.service.AuthorizationException;
import com.pbl3.project.pbl3_project.service.ConcurrencyConflictException;
import com.pbl3.project.pbl3_project.service.StaleStocktakeSessionException;
import com.pbl3.project.pbl3_project.service.UnsafeLegacyOperationException;
import com.pbl3.project.pbl3_project.service.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<ApiError> handleAuthorization(AuthorizationException ex) {
        return build(HttpStatus.FORBIDDEN, "authorization_error", ex.getMessage());
    }

    @ExceptionHandler({
        ConcurrencyConflictException.class,
        StaleStocktakeSessionException.class,
        UnsafeLegacyOperationException.class
    })
    public ResponseEntity<ApiError> handleConflict(RuntimeException ex) {
        return build(HttpStatus.CONFLICT, "conflict_error", ex.getMessage());
    }

    @ExceptionHandler({
        ValidationException.class,
        IllegalArgumentException.class
    })
    public ResponseEntity<ApiError> handleValidation(RuntimeException ex) {
        return build(HttpStatus.BAD_REQUEST, "validation_error", ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleRuntime(RuntimeException ex) {
        return build(
            HttpStatus.BAD_REQUEST,
            "runtime_error",
            ex.getMessage() == null || ex.getMessage().isBlank() ? "Operation failed" : ex.getMessage()
        );
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiError(code, message));
    }

    public record ApiError(String code, String message) {
    }
}
