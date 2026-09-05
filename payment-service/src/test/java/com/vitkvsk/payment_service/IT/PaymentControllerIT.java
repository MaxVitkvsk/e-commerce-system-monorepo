package com.vitkvsk.payment_service.IT;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.vitkvsk.payment_service.dto.PaymentCreateDto;
import com.vitkvsk.payment_service.dto.PaymentResponseDto;
import com.vitkvsk.payment_service.dto.PaymentTotalDto;
import com.vitkvsk.payment_service.entity.Payment;
import com.vitkvsk.payment_service.entity.PaymentStatus;
import com.vitkvsk.payment_service.producer.PaymentEventProducer;
import com.vitkvsk.payment_service.repository.PaymentRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Testcontainers
class PaymentControllerIT {

    @Container
    @ServiceConnection
    static final MongoDBContainer mongo = new MongoDBContainer("mongo:8.0");

    static final WireMockServer wireMock =
            new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

    static {
        wireMock.start();
    }

    @MockitoBean
    private PaymentEventProducer paymentEventProducer;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RestTestClient restTestClient;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("app.random-api.url", wireMock::baseUrl);
        registry.add("app.internal-secret", () -> "test-secret");
        registry.add("spring.kafka.bootstrap-servers", () -> "dummy:9092");
        registry.add("mongock.enabled", () -> "false");
    }

    @AfterAll
    static void tearDown() {
        wireMock.stop();
    }

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        wireMock.resetAll();
        wireMock.stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse().withStatus(200).withBody("42")));
    }

    @Test
    @DisplayName("POST - 201: even random - SUCCESS, document in Mongo, event sent")
    void createPayment_success() {
        restTestClient.post()
                .uri("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PaymentCreateDto(1L, "user-1", new BigDecimal("100.00")))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(PaymentResponseDto.class)
                .value(body -> {
                    assertThat(body.status()).isEqualTo(PaymentStatus.SUCCESS);
                    assertThat(body.id()).isNotBlank();
                });

        List<Payment> all = paymentRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getStatus()).isEqualTo(PaymentStatus.SUCCESS);

        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/")));
        verify(paymentEventProducer).sendPaymentCreated(any(Payment.class));
    }

    @Test
    @DisplayName("POST: odd random - FAILED")
    void createPayment_failed() {
        wireMock.stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse().withStatus(200).withBody("43")));

        restTestClient.post()
                .uri("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PaymentCreateDto(2L, "user-1", new BigDecimal("10.00")))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(PaymentResponseDto.class)
                .value(body -> assertThat(body.status()).isEqualTo(PaymentStatus.FAILED));
    }

    @Test
    @DisplayName("POST: 500 - @Retryable - success on 2nd attempt")
    void createPayment_retriesOn500() {
        wireMock.stubFor(get(urlPathEqualTo("/"))
                .inScenario("retry")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("second"));
        wireMock.stubFor(get(urlPathEqualTo("/"))
                .inScenario("retry")
                .whenScenarioStateIs("second")
                .willReturn(aResponse().withStatus(200).withBody("7")));

        restTestClient.post()
                .uri("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PaymentCreateDto(3L, "user-1", new BigDecimal("10.00")))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(PaymentResponseDto.class)
                .value(body -> assertThat(body.status()).isEqualTo(PaymentStatus.FAILED));

        wireMock.verify(2, getRequestedFor(urlPathEqualTo("/")));
    }

    @Test
    @DisplayName("POST: invalid body - 400")
    void createPayment_validation400() {
        restTestClient.post()
                .uri("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("orderId", 1, "userId", "", "paymentAmount", -5))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("GET /api/payments?userId: search via real MongoTemplate")
    void searchByUserId() {
        paymentRepository.save(Payment.builder()
                .orderId(1L).userId("user-1")
                .paymentAmount(new BigDecimal("10.00"))
                .status(PaymentStatus.SUCCESS).timestamp(Instant.now()).build());

        restTestClient.get()
                .uri("/api/payments?userId=user-1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].userId").isEqualTo("user-1");
    }

    @Test
    @DisplayName("GET /total: real sum aggregation in Mongo")
    void totalForUser() {
        paymentRepository.saveAll(List.of(
                Payment.builder().orderId(1L).userId("user-1")
                        .paymentAmount(new BigDecimal("100.00"))
                        .status(PaymentStatus.SUCCESS).timestamp(Instant.now()).build(),
                Payment.builder().orderId(2L).userId("user-1")
                        .paymentAmount(new BigDecimal("50.00"))
                        .status(PaymentStatus.SUCCESS).timestamp(Instant.now()).build()));

        restTestClient.get()
                .uri("/api/payments/total?userId=user-1&from=2020-01-01T00:00:00Z&to=2030-01-01T00:00:00Z")
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentTotalDto.class)
                .value(dto -> assertThat(dto.total()).isEqualByComparingTo(new BigDecimal("150.00")));
    }

    @Test
    @DisplayName("GET /total: no data - ZERO")
    void totalForUser_noData() {
        restTestClient.get()
                .uri("/api/payments/total?userId=ghost&from=2020-01-01T00:00:00Z&to=2030-01-01T00:00:00Z")
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentTotalDto.class)
                .value(dto -> assertThat(dto.total()).isEqualByComparingTo(BigDecimal.ZERO));
    }
}