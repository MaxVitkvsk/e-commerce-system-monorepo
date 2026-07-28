package com.vitkvsk.user_service.dto.paymentcard;

import java.time.LocalDate;
import java.util.UUID;

public record PaymentCardResponseDto(
        Long id,
        UUID userId,
        String number,
        String holder,
        LocalDate expirationDate,
        boolean active
) {}
