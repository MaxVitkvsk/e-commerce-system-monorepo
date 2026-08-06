package com.vitkvsk.order_service.dto;

import com.vitkvsk.order_service.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponseDto(
        Long id,
        UUID userId,
        OrderStatus status,
        BigDecimal totalPrice,
        Instant createdAt,
        Instant updatedAt,
        List<OrderItemResponseDto> items,
        UserInfoDto user
) {}