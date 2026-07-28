package com.vitkvsk.user_service.dto.paymentcard;

import com.vitkvsk.user_service.validation.CardHolder;
import com.vitkvsk.user_service.validation.CardNumber;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.UUID;

public record PaymentCardCreateDto(
        @NotNull(message = "User ID is mandatory")
        UUID userId,

        @NotBlank(message = "Card number is mandatory")
        @CardNumber
        String number,

        @NotBlank(message = "Cardholder name is mandatory")
        @CardHolder
        String holder,

        @NotNull(message = "Expiration date is mandatory")
        @Future(message = "Expiration date must be in the future")
        LocalDate expirationDate
) {}
