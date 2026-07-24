package com.vitkvsk.user_service.validation;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Size(max = 32, message = "must not exceed 32 characters")
@Pattern(regexp = "^[0-9]+$", message = "must contain only digits")
public @interface CardNumber {
}
