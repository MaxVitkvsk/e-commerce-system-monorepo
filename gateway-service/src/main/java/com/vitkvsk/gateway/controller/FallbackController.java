package com.vitkvsk.gateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RestController
public class FallbackController {

    private static final Logger log = LoggerFactory.getLogger(FallbackController.class);

    public record ErrorResponse(String error, String message, Instant timestamp) {
        public ErrorResponse(String error, String message) {
            this(error, message, Instant.now());
        }
    }

    @GetMapping("/fallback/order")
    public Mono<ResponseEntity<ErrorResponse>> orderServiceFallback() {
        log.warn("Circuit breaker triggered for [order-service]. Returning fallback response.");

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(
                        "SERVICE_UNAVAILABLE",
                        "High load on Order Service. Please, try again later."
                )));
    }

    @GetMapping("/fallback/user")
    public Mono<ResponseEntity<ErrorResponse>> userServiceFallback() {
        log.warn("Circuit breaker triggered for [user-service]. Returning fallback response.");

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(
                        "SERVICE_UNAVAILABLE",
                        "User Service is currently unavailable or timed out."
                )));
    }
}