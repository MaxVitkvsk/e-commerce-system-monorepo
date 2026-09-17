package com.vitkvsk.payment_service.unit;

import com.vitkvsk.payment_service.client.RandomNumberClient;
import com.vitkvsk.payment_service.dto.PaymentCreateDto;
import com.vitkvsk.payment_service.dto.PaymentResponseDto;
import com.vitkvsk.payment_service.dto.PaymentTotalDto;
import com.vitkvsk.payment_service.entity.Payment;
import com.vitkvsk.payment_service.entity.PaymentStatus;
import com.vitkvsk.payment_service.mapper.PaymentMapper;
import com.vitkvsk.payment_service.producer.PaymentEventProducer;
import com.vitkvsk.payment_service.repository.PaymentRepository;
import com.vitkvsk.payment_service.service.PaymentService;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentMapper paymentMapper;
    @Mock private RandomNumberClient randomNumberClient;
    @Mock private PaymentEventProducer paymentEventProducer;
    @Mock private MongoTemplate mongoTemplate;

    private PaymentService paymentService;

    private static final Instant FROM = Instant.parse("2020-01-01T00:00:00Z");
    private static final Instant TO   = Instant.parse("2030-01-01T00:00:00Z");

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentRepository,
                paymentMapper,
                randomNumberClient,
                paymentEventProducer,
                mongoTemplate
        );
    }

    private PaymentCreateDto dto() {
        return new PaymentCreateDto(42L, "user-1", new BigDecimal("100.00"));
    }

    private Payment buildEntity() {
        return Payment.builder()
                .id("id-1")
                .orderId(42L)
                .userId("user-1")
                .paymentAmount(new BigDecimal("100.00"))
                .build();
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("even random - SUCCESS, timestamp set, event sent")
        void evenRandom_success() {
            Payment entity = buildEntity();
            PaymentResponseDto response = new PaymentResponseDto(
                    "id-1", 42L, "user-1", PaymentStatus.SUCCESS, Instant.now(), new BigDecimal("100.00"));

            when(paymentMapper.toEntity(any(PaymentCreateDto.class))).thenReturn(entity);
            when(randomNumberClient.getRandomNumber()).thenReturn(42);
            when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
            when(paymentMapper.toResponseDto(any(Payment.class))).thenReturn(response);

            PaymentResponseDto result = paymentService.create(dto());

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            Payment saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(saved.getTimestamp()).isNotNull();

            verify(paymentEventProducer).sendPaymentCreated(saved);
            assertThat(result.status()).isEqualTo(PaymentStatus.SUCCESS);
        }

        @Test
        @DisplayName("odd random - FAILED, event still sent")
        void oddRandom_failed() {
            Payment entity = buildEntity();
            PaymentResponseDto response = new PaymentResponseDto(
                    "id-2", 42L, "user-1", PaymentStatus.FAILED, Instant.now(), new BigDecimal("100.00"));

            when(paymentMapper.toEntity(any())).thenReturn(entity);
            when(randomNumberClient.getRandomNumber()).thenReturn(43);
            when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(paymentMapper.toResponseDto(any())).thenReturn(response);

            PaymentResponseDto result = paymentService.create(dto());

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);

            verify(paymentEventProducer).sendPaymentCreated(any(Payment.class));
        }

        @Test
        @DisplayName("zero random - SUCCESS (0 % 2 == 0)")
        void zeroRandom_success() {
            Payment entity = buildEntity();
            when(paymentMapper.toEntity(any())).thenReturn(entity);
            when(randomNumberClient.getRandomNumber()).thenReturn(0);
            when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(paymentMapper.toResponseDto(any())).thenReturn(
                    new PaymentResponseDto("id-3", 42L, "user-1",
                            PaymentStatus.SUCCESS, Instant.now(), new BigDecimal("100.00")));

            PaymentResponseDto result = paymentService.create(dto());

            assertThat(result.status()).isEqualTo(PaymentStatus.SUCCESS);
        }

        @Test
        @DisplayName("negative odd random - FAILED")
        void negativeOddRandom_failed() {
            Payment entity = buildEntity();
            when(paymentMapper.toEntity(any())).thenReturn(entity);
            when(randomNumberClient.getRandomNumber()).thenReturn(-7);
            when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(paymentMapper.toResponseDto(any())).thenReturn(
                    new PaymentResponseDto("id-4", 42L, "user-1",
                            PaymentStatus.FAILED, Instant.now(), new BigDecimal("100.00")));

            PaymentResponseDto result = paymentService.create(dto());

            assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);
        }
    }

    @Nested
    @DisplayName("search()")
    class Search {

        @Test
        @DisplayName("all three filters provided - query built and mapper called")
        void allFilters() {
            Payment p = buildEntity();
            when(mongoTemplate.find(any(Query.class), eq(Payment.class))).thenReturn(List.of(p));
            when(paymentMapper.toResponseDtoList(List.of(p))).thenReturn(List.of(
                    new PaymentResponseDto("id-1", 42L, "user-1",
                            PaymentStatus.SUCCESS, Instant.now(), new BigDecimal("100.00"))));

            List<PaymentResponseDto> result =
                    paymentService.search("user-1", 42L, PaymentStatus.SUCCESS);

            assertThat(result).hasSize(1);
            verify(mongoTemplate).find(any(Query.class), eq(Payment.class));
        }

        @Test
        @DisplayName("all filters null - empty orOperator")
        void noFilters() {
            when(mongoTemplate.find(any(Query.class), eq(Payment.class))).thenReturn(Collections.emptyList());
            when(paymentMapper.toResponseDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

            List<PaymentResponseDto> result = paymentService.search(null, null, null);

            assertThat(result).isEmpty();
            verify(mongoTemplate).find(any(Query.class), eq(Payment.class));
        }

        @Test
        @DisplayName("only userId - query contains user_id")
        void onlyUserId() {
            when(mongoTemplate.find(any(Query.class), eq(Payment.class))).thenReturn(Collections.emptyList());
            when(paymentMapper.toResponseDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

            paymentService.search("user-1", null, null);

            ArgumentCaptor<Query> q = ArgumentCaptor.forClass(Query.class);
            verify(mongoTemplate).find(q.capture(), eq(Payment.class));
            assertThat(q.getValue().toString()).contains("user_id");
        }

        @Test
        @DisplayName("only orderId - query contains order_id")
        void onlyOrderId() {
            when(mongoTemplate.find(any(Query.class), eq(Payment.class))).thenReturn(Collections.emptyList());
            when(paymentMapper.toResponseDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

            paymentService.search(null, 99L, null);

            ArgumentCaptor<Query> q = ArgumentCaptor.forClass(Query.class);
            verify(mongoTemplate).find(q.capture(), eq(Payment.class));
            assertThat(q.getValue().toString()).contains("order_id");
        }
    }

    @Nested
    @DisplayName("totalForUser()")
    class TotalForUser {

        @Test
        @DisplayName("Decimal128 in Mongo response - correct BigDecimal")
        void decimal128Result() {
            Document doc = new Document("total", Decimal128.parse("250.50"));
            when(mongoTemplate.aggregate(any(Aggregation.class), eq("payments"), eq(Document.class)))
                    .thenReturn(new AggregationResults<>(List.of(doc), new Document()));

            PaymentTotalDto total = paymentService.totalForUser("user-1", FROM, TO);

            assertThat(total.total()).isEqualByComparingTo(new BigDecimal("250.50"));
        }

        @Test
        @DisplayName("Number (long) in response - converted")
        void numberResult() {
            Document doc = new Document("total", 1234L);
            when(mongoTemplate.aggregate(any(Aggregation.class), eq("payments"), eq(Document.class)))
                    .thenReturn(new AggregationResults<>(List.of(doc), new Document()));

            PaymentTotalDto total = paymentService.totalForUser("user-1", FROM, TO);

            assertThat(total.total()).isEqualByComparingTo(new BigDecimal("1234"));
        }

        @Test
        @DisplayName("no documents - ZERO")
        void noData_returnsZero() {
            when(mongoTemplate.aggregate(any(Aggregation.class), eq("payments"), eq(Document.class)))
                    .thenReturn(new AggregationResults<>(Collections.emptyList(), new Document()));

            PaymentTotalDto total = paymentService.totalForUser("user-1", FROM, TO);

            assertThat(total.total()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("total = null in document - ZERO")
        void nullTotal_returnsZero() {
            Document doc = new Document("total", null);
            when(mongoTemplate.aggregate(any(Aggregation.class), eq("payments"), eq(Document.class)))
                    .thenReturn(new AggregationResults<>(List.of(doc), new Document()));

            PaymentTotalDto total = paymentService.totalForUser("user-1", FROM, TO);

            assertThat(total.total()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("totalForAll()")
    class TotalForAll {

        @Test
        @DisplayName("total sum returned correctly")
        void sum() {
            Document doc = new Document("total", Decimal128.parse("1000.00"));
            when(mongoTemplate.aggregate(any(Aggregation.class), eq("payments"), eq(Document.class)))
                    .thenReturn(new AggregationResults<>(List.of(doc), new Document()));

            PaymentTotalDto total = paymentService.totalForAll(FROM, TO);

            assertThat(total.total()).isEqualByComparingTo(new BigDecimal("1000.00"));
        }

        @Test
        @DisplayName("empty collection - ZERO")
        void empty_returnsZero() {
            when(mongoTemplate.aggregate(any(Aggregation.class), eq("payments"), eq(Document.class)))
                    .thenReturn(new AggregationResults<>(Collections.emptyList(), new Document()));

            PaymentTotalDto total = paymentService.totalForAll(FROM, TO);

            assertThat(total.total()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}