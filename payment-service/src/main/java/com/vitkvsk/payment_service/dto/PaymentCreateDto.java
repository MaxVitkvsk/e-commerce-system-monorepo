package com.vitkvsk.payment_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaymentCreateDto(
        @NotNull(message = "orderId is required") Long orderId,
        @NotBlank(message = "userId is required") String userId,
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be > 0") BigDecimal paymentAmount
) {}
