package com.vitkvsk.user_service.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record UserResponseDto(
        Long id,
        String name,
        String surname,
        LocalDate birthDate,
        String email,
        boolean active,
        Instant createdAt,
        Instant  updatedAt
) {}
