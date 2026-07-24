package com.vitkvsk.user_service.dto.user;

import com.vitkvsk.user_service.validation.EmailAddress;
import com.vitkvsk.user_service.validation.PersonName;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record UserCreateDto(
        @NotBlank(message = "Name is mandatory")
        @PersonName
        String name,

        @NotBlank(message = "Surname is mandatory")
        @PersonName
        String surname,

        @NotNull(message = "Birth date is mandatory")
        @Past(message = "Birth date must be in the past")
        LocalDate birthDate,

        @NotBlank(message = "Email is mandatory")
        @EmailAddress
        String email
) {}
