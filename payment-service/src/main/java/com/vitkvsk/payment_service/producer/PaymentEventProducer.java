package com.vitkvsk.payment_service.producer;

import com.vitkvsk.payment_service.dto.event.PaymentCreatedEvent;
import com.vitkvsk.payment_service.entity.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, PaymentCreatedEvent> kafkaTemplate;

    @Value("${app.kafka.topic:payment-events}")
    private String topic;

    public void sendPaymentCreated(Payment payment) {
        PaymentCreatedEvent event = new PaymentCreatedEvent(
                UUID.randomUUID().toString(),
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getStatus().name(),
                payment.getPaymentAmount(),
                payment.getTimestamp()
        );

        kafkaTemplate.send(topic, String.valueOf(payment.getOrderId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send CREATE_PAYMENT event: orderId={}, eventId={}",
                                payment.getOrderId(), event.eventId(), ex);
                    } else {
                        log.info("CREATE_PAYMENT sent: orderId={}, status={}, partition={}, offset={}, eventId={}",
                                payment.getOrderId(), event.status(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset(),
                                event.eventId());
                    }
                });
    }
}
