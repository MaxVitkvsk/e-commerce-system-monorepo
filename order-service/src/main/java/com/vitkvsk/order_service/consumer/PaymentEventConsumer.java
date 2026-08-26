package com.vitkvsk.order_service.consumer;

import com.vitkvsk.order_service.dto.event.PaymentCreatedEvent;
import com.vitkvsk.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final OrderService orderService;

    @KafkaListener(
            topics = "${app.kafka.topic:payment-events}",
            groupId = "order-service"
    )
    public void onPaymentCreated(PaymentCreatedEvent event) {
        log.info("CREATE_PAYMENT received: orderId={}, status={}, eventId={}",
                event.orderId(), event.status(), event.eventId());

        try {
            orderService.applyPaymentResult(event);
            log.info("Order {} updated based on payment event", event.orderId());
        } catch (Exception e) {
            log.error("Failed to process payment event: orderId={}, eventId={}",
                    event.orderId(), event.eventId(), e);
            throw e;
        }
    }
}
