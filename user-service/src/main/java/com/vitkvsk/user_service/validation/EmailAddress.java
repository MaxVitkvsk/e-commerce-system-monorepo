package com.vitkvsk.user_service.validation;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Email(message = "invalid email format")
@Size(max = 255, message = "must not exceed 255 characters")
public @interface EmailAddress {
}
