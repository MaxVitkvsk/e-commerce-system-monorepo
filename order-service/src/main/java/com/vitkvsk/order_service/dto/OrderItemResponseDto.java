package com.vitkvsk.order_service.dto;

import java.math.BigDecimal;

public record OrderItemResponseDto(
        Long id,
        Long itemId,
        String itemName,
        BigDecimal price,
        Integer quantity
) {}