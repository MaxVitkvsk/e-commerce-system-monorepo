package com.vitkvsk.user_service.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record PaymentCardCreateDto(
        @NotNull(message = "User ID is mandatory")
        Long userId,

        @NotBlank(message = "Card number is mandatory")
        @Size(max = 32, message = "Card number must not exceed 32 characters")
        @Pattern(regexp = "^[0-9]+$", message = "Card number must contain only digits")
        String number,

        @NotBlank(message = "Cardholder name is mandatory")
        @Size(max = 100, message = "Cardholder name must not exceed 100 characters")
        String holder,

        @NotNull(message = "Expiration date is mandatory")
        @Future(message = "Expiration date must be in the future")
        LocalDate expirationDate
) {}
