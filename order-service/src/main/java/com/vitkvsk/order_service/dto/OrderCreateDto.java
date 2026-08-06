package com.vitkvsk.order_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderCreateDto(
        @NotNull UUID userId,
        @NotNull @Positive BigDecimal totalPrice,
        @NotEmpty @Valid List<OrderItemCreateDto> items
) {}
