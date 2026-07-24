package com.vitkvsk.user_service.dto.paymentcard;

import com.vitkvsk.user_service.validation.CardHolder;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record PaymentCardUpdateDto(

        @CardHolder
        String holder,

        @Future(message = "Expiration date must be in the future")
        LocalDate expirationDate,

        Boolean active
) {}
