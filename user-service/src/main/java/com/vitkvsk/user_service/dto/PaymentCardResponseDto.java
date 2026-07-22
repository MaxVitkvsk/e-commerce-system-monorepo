package com.vitkvsk.user_service.dto;

import java.time.LocalDate;

public record PaymentCardResponseDto(
        Long id,
        Long userId,
        String number,
        String holder,
        LocalDate expirationDate,
        boolean active
) {}
