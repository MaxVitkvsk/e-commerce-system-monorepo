package com.vitkvsk.user_service.dto.user;

import com.vitkvsk.user_service.validation.EmailAddress;
import com.vitkvsk.user_service.validation.PersonName;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record UserUpdateDto(

        @PersonName
        String name,

        @PersonName
        String surname,

        @Past(message = "Birth date must be in the past")
        LocalDate birthDate,

        @EmailAddress
        String email
) {}
