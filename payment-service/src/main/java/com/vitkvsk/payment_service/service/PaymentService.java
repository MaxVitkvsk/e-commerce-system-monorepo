package com.vitkvsk.payment_service.service;

import com.vitkvsk.payment_service.client.RandomNumberClient;
import com.vitkvsk.payment_service.dto.PaymentCreateDto;
import com.vitkvsk.payment_service.dto.PaymentResponseDto;
import com.vitkvsk.payment_service.dto.PaymentTotalDto;
import com.vitkvsk.payment_service.entity.Payment;
import com.vitkvsk.payment_service.entity.PaymentStatus;
import com.vitkvsk.payment_service.exception.ExternalServiceException;
import com.vitkvsk.payment_service.exception.InvalidPaymentDataException;
import com.vitkvsk.payment_service.exception.PaymentNotFoundException;
import com.vitkvsk.payment_service.mapper.PaymentMapper;
import com.vitkvsk.payment_service.producer.PaymentEventProducer;
import com.vitkvsk.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String PAYMENTS_COLLECTION = "payments";
    private static final String PAYMENT_AMOUNT_FIELD = "payment_amount";
    private static final String TOTAL_FIELD = "total";
    private static final String USER_ID_FIELD = "user_id";
    private static final String ORDER_ID_FIELD = "order_id";
    private static final String TIMESTAMP_FIELD = "timestamp";
    private static final String STATUS_FIELD = "status";

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final RandomNumberClient randomNumberClient;
    private final PaymentEventProducer paymentEventProducer;
    private final MongoTemplate mongoTemplate;

    private void validatePaymentData(PaymentCreateDto dto) {
        if (dto.orderId() == null || dto.orderId() <= 0) {
            throw new InvalidPaymentDataException("orderId", "must be positive");
        }
        if (dto.userId() == null || dto.userId().isBlank()) {
            throw new InvalidPaymentDataException("userId", "cannot be empty");
        }
        if (dto.paymentAmount() == null || dto.paymentAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentDataException("paymentAmount", "must be positive");
        }
    }

    public PaymentResponseDto create(PaymentCreateDto dto) {
        validatePaymentData(dto);

        log.info("Creating payment: orderId={}, userId={}, amount={}",
                dto.orderId(), dto.userId(), dto.paymentAmount());

        Payment payment = paymentMapper.toEntity(dto);
        payment.setTimestamp(Instant.now());

        int random;
        try {
            random = randomNumberClient.getRandomNumber();
        } catch (HttpServerErrorException | ResourceAccessException ex) {
            throw new ExternalServiceException("random-api", ex);
        } catch (Exception ex) {
            throw new ExternalServiceException("random-api",
                    "Unexpected error: " + ex.getMessage());
        }

        PaymentStatus status = random % 2 == 0 ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
        log.debug("External random={} -> status={}", random, status);

        payment.setStatus(status);
        Payment saved = paymentRepository.save(payment);

        paymentEventProducer.sendPaymentCreated(saved);

        log.info("Payment created: id={}, status={}", saved.getId(), saved.getStatus());
        return paymentMapper.toResponseDto(saved);
    }

    public PaymentResponseDto getById(String id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
        return paymentMapper.toResponseDto(payment);
    }

    public List<PaymentResponseDto> search(String userId, Long orderId, PaymentStatus status) {
        log.debug("Search payments: userId={}, orderId={}, status={}", userId, orderId, status);
        List<Payment> found = findDynamic(userId, orderId, status);
        log.debug("Search returned {} payments", found.size());
        return paymentMapper.toResponseDtoList(found);
    }

    public PaymentTotalDto totalForUser(String userId, Instant from, Instant to) {
        if (userId == null || userId.isBlank()) {
            throw new InvalidPaymentDataException("userId", "cannot be empty");
        }
        log.debug("Total for user {}: [{} .. {}]", userId, from, to);
        return new PaymentTotalDto(sum(Criteria.where(USER_ID_FIELD).is(userId)
                .and(TIMESTAMP_FIELD).gte(from).lte(to)));
    }

    public PaymentTotalDto totalForAll(Instant from, Instant to) {
        log.info("Admin total requested: [{} .. {}]", from, to);
        return new PaymentTotalDto(sum(Criteria.where(TIMESTAMP_FIELD).gte(from).lte(to)));
    }

    private List<Payment> findDynamic(String userId, Long orderId, PaymentStatus status) {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put(USER_ID_FIELD, userId);
        filters.put(ORDER_ID_FIELD, orderId);
        filters.put(STATUS_FIELD, status);

        List<Criteria> or = filters.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .map(e -> Criteria.where(e.getKey()).is(e.getValue()))
                .toList();

        Criteria criteria = or.isEmpty()
                ? new Criteria()
                : new Criteria().orOperator(or.toArray(new Criteria[0]));
        return mongoTemplate.find(new Query(criteria), Payment.class);
    }

    private BigDecimal sum(Criteria criteria) {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.group().sum(PAYMENT_AMOUNT_FIELD).as(TOTAL_FIELD));
        Document r = mongoTemplate.aggregate(agg, PAYMENTS_COLLECTION, Document.class)
                .getUniqueMappedResult();

        if (r == null || r.get(TOTAL_FIELD) == null) return BigDecimal.ZERO;
        Object total = r.get(TOTAL_FIELD);
        if (total instanceof Decimal128 d128) return d128.bigDecimalValue();
        if (total instanceof Number num) return BigDecimal.valueOf(num.doubleValue());
        return BigDecimal.ZERO;
    }
}