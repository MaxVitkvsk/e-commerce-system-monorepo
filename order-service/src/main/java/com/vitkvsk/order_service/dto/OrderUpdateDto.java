package com.vitkvsk.order_service.dto;

import com.vitkvsk.order_service.entity.OrderStatus;
import jakarta.validation.Valid;

import java.util.List;

public record OrderUpdateDto(
        OrderStatus status,
         List<@Valid OrderItemCreateDto> items ) {}
