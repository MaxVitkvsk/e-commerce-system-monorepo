package com.vitkvsk.user_service.dto;


import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record UserCreateDto(
        @NotBlank(message = "Name is mandatory")
        @Size(max = 50, message = "Name must not exceed 50 characters")
        String name,

        @NotBlank(message = "Surname is mandatory")
        @Size(max = 50, message = "Surname must not exceed 50 characters")
        String surname,

        @NotNull(message = "Birth date is mandatory")
        @Past(message = "Birth date must be in the past")
        LocalDate birthDate,

        @NotBlank(message = "Email is mandatory")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        @Email(message = "Invalid email format")
        String email,

        Boolean active
) {
    public UserCreateDto {
        if (active == null) {
            active = true;
        }
    }
}
