package com.vitkvsk.user_service.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record UserUpdateDto(
        @NotBlank
        @Size(max = 50)
        String name,

        @NotBlank
        @Size(max = 50)
        String surname,

        @NotNull
        @Past
        LocalDate birthDate,

        @NotBlank
        @Size(max = 255)
        @Email
        String email
) {}
