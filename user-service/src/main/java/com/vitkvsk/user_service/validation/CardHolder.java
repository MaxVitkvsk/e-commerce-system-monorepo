package com.vitkvsk.user_service.validation;

import jakarta.validation.constraints.Size;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Size(max = 100, message = "must not exceed 100 characters")
public @interface CardHolder {
}
