package com.vitkvsk.user_service.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record PaymentCardUpdateDto(
        @Size(max = 100, message = "Cardholder name must not exceed 100 characters")
        String holder,

        @Future(message = "Expiration date must be in the future")
        LocalDate expirationDate,

        Boolean active
) {}
