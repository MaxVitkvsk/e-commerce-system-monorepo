package com.vitkvsk.payment_service.handler;

import com.vitkvsk.payment_service.dto.ExceptionResponse;
import com.vitkvsk.payment_service.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String SERVICE_UNAVAILABLE = "Service Unavailable";
    private static final String INTERNAL_SERVER_ERROR = "Internal Server Error";
    private static final String BAD_REQUEST = "Bad Request";
    private static final String UNEXPECTED_ERROR = "UNEXPECTED_ERROR";
    private static final String FIELD_ERRORS = "fieldErrors";

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ExceptionResponse> handlePaymentNotFound(
            PaymentNotFoundException ex, HttpServletRequest request) {
        log.warn("Payment not found: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ExceptionResponse.of(
                        HttpStatus.NOT_FOUND.value(),
                        "Not Found",
                        ex.getExceptionCode().name(),
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ExceptionResponse> handleExternalServiceError(
            ExternalServiceException ex, HttpServletRequest request) {
        log.error("External service error: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ExceptionResponse.of(
                        HttpStatus.SERVICE_UNAVAILABLE.value(),
                        SERVICE_UNAVAILABLE,
                        ex.getExceptionCode().name(),
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(InvalidPaymentDataException.class)
    public ResponseEntity<ExceptionResponse> handleInvalidPaymentData(
            InvalidPaymentDataException ex, HttpServletRequest request) {
        log.warn("Invalid payment data: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ExceptionResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        BAD_REQUEST,
                        ex.getExceptionCode().name(),
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(PaymentProcessingException.class)
    public ResponseEntity<ExceptionResponse> handlePaymentProcessing(
            PaymentProcessingException ex, HttpServletRequest request) {
        log.error("Payment processing error: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ExceptionResponse.of(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        INTERNAL_SERVER_ERROR,
                        ex.getExceptionCode().name(),
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

        log.warn("Validation failed: {}", fieldErrors);

        Map<String, Object> details = new HashMap<>();
        details.put(FIELD_ERRORS, fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ExceptionResponse.builder()
                        .timestamp(Instant.now())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error(BAD_REQUEST)
                        .code(ExceptionCode.VALIDATION_ERROR.name())
                        .message("Validation failed")
                        .path(request.getRequestURI())
                        .details(details)
                        .build());
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<ExceptionResponse> handleHttpServerError(
            HttpServerErrorException ex, HttpServletRequest request) {
        log.error("HTTP server error from external service: {} - {}",
                ex.getStatusCode(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ExceptionResponse.of(
                        HttpStatus.SERVICE_UNAVAILABLE.value(),
                        SERVICE_UNAVAILABLE,
                        ExceptionCode.EXTERNAL_SERVICE_UNAVAILABLE.name(),
                        "External service returned error: " + ex.getStatusCode(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ExceptionResponse> handleResourceAccess(
            ResourceAccessException ex, HttpServletRequest request) {
        log.error("Cannot access external resource: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ExceptionResponse.of(
                        HttpStatus.SERVICE_UNAVAILABLE.value(),
                        SERVICE_UNAVAILABLE,
                        ExceptionCode.EXTERNAL_SERVICE_UNAVAILABLE.name(),
                        "Cannot connect to external service",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ExceptionResponse.of(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        INTERNAL_SERVER_ERROR,
                        UNEXPECTED_ERROR,
                        "An unexpected error occurred",
                        request.getRequestURI()
                ));
    }
}