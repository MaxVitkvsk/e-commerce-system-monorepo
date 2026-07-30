package com.vitkvsk.auth_service.dto;

import jakarta.validation.constraints.NotBlank;

public record ValidateRequest(@NotBlank String token) {}
