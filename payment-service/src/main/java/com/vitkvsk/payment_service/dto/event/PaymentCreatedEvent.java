package com.vitkvsk.payment_service.dto.event;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentCreatedEvent(
        String eventId,
        String paymentId,
        Long orderId,
        String userId,
        String status,
        BigDecimal paymentAmount,
        Instant timestamp
) {}
