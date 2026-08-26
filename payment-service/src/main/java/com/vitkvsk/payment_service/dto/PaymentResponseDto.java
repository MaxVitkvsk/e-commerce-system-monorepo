package com.vitkvsk.payment_service.dto;

import com.vitkvsk.payment_service.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponseDto(
        String id,
        Long orderId,
        String userId,
        PaymentStatus status,
        Instant timestamp,
        BigDecimal paymentAmount
) {}
