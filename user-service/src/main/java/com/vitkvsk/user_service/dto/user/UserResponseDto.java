package com.vitkvsk.user_service.dto.user;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record UserResponseDto(
        UUID id,
        String name,
        String surname,
        LocalDate birthDate,
        String email,
        boolean active,
        Instant createdAt,
        Instant  updatedAt
) {}
