package com.vitkvsk.order_service.IT;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.vitkvsk.order_service.IntegrationTest;
import com.vitkvsk.order_service.dto.*;
import com.vitkvsk.order_service.entity.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
@WireMockTest(httpPort = 8089)
@Sql(scripts = "/test-data/insert-test-item.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class OrderFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper json;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        WireMock.reset();

        UserInfoDto user = new UserInfoDto(userId, "John", "Doe", "john@test.com");
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/api/users/internal/[^/]+"))
                .willReturn(WireMock.okJson(json.writeValueAsString(user))));
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/api/users/internal/ids"))
                .willReturn(WireMock.okJson(json.writeValueAsString(List.of(user)))));
    }

    private Long createOrder() throws Exception {
        String response = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new OrderCreateDto(
                                userId, new BigDecimal("25.50"),
                                List.of(new OrderItemCreateDto(1L, 2))))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }

    @Test
    void shouldCreateOrderAndReturnUserInfo() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new OrderCreateDto(
                                userId, new BigDecimal("25.50"),
                                List.of(new OrderItemCreateDto(1L, 2))))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.user.email").value("john@test.com"));
    }

    @Test
    void shouldGetByIdAndReturn404WhenNotFound() throws Exception {
        Long orderId = createOrder();

        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId));

        mockMvc.perform(get("/api/orders/{id}", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldSearchWithPaginationAndStatusFilter() throws Exception {
        for (int i = 0; i < 3; i++) createOrder();

        mockMvc.perform(get("/api/orders").param("statuses", "NEW")
                        .param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(3)));
    }

    @Test
    void shouldUpdateStatusAndSoftDelete() throws Exception {
        Long orderId = createOrder();

        mockMvc.perform(put("/api/orders/{id}", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new OrderUpdateDto(OrderStatus.CONFIRMED, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(delete("/api/orders/{id}", orderId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400ForInvalidDto() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"totalPrice\": 10.00, \"items\": []}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnOrderWithoutUserWhenUserServiceDown() throws Exception {
        WireMock.reset();  // user-service «мёртв» — стабов нет
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/api/users/internal/.*"))
                .willReturn(WireMock.aResponse().withStatus(500)));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new OrderCreateDto(
                                userId, new BigDecimal("22.00"),
                                List.of(new OrderItemCreateDto(1L, 1))))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user").isEmpty());
    }
}