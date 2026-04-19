package com.pbl3.project.pbl3_project.controller;

import com.pbl3.project.pbl3_project.service.ConcurrencyConflictException;
import com.pbl3.project.pbl3_project.service.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void conflictExceptionsMapToConflictStatus() {
        var response = handler.handleConflict(new ConcurrencyConflictException("conflict"));
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("conflict_error", response.getBody().code());
    }

    @Test
    void validationExceptionsMapToBadRequest() {
        var response = handler.handleValidation(new ValidationException("invalid"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("validation_error", response.getBody().code());
    }
}
