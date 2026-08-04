package com.vitkvsk.user_service.dto.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InternalUserCreateRequest(
        @NotNull UUID id,
        @Valid @NotNull UserCreateDto user
) {}
