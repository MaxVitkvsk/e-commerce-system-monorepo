package com.vitkvsk.user_service.IT.paymentcard;

import com.vitkvsk.user_service.IT.TestJwt;
import com.vitkvsk.user_service.IntegrationTest;
import com.vitkvsk.user_service.dto.paymentcard.PaymentCardCreateDto;
import com.vitkvsk.user_service.dto.paymentcard.PaymentCardUpdateDto;
import com.vitkvsk.user_service.entity.User;
import com.vitkvsk.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.Month;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
class PaymentCardFlowTest {

    @Autowired
    private MockMvc
            mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .name("John")
                .surname("Dod")
                .email("john@test.com")
                .birthDate(LocalDate.of(1990, Month.APRIL, 1))
                .build();
        userRepository.save(user);
    }

    @Test
    void shouldCreateAndGetCard() throws Exception {
        PaymentCardCreateDto createDto = new PaymentCardCreateDto(
                userId, "1234567890123456", "JOHN DOD", LocalDate.of(2030, Month.APRIL, 28));

        String response = mockMvc.perform(post("/api/cards")
                        .with(TestJwt.user(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value("1234567890123456"))
                .andReturn().getResponse().getContentAsString();

        Long cardId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/cards/{id}", cardId).with(TestJwt.user(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId))
                .andExpect(jsonPath("$.holder").value("JOHN DOD"));
    }

    @Test
    void shouldUpdateCard() throws Exception {
        PaymentCardCreateDto createDto = new PaymentCardCreateDto(
                userId, "9876543210987654", "JOHN DOD", LocalDate.of(2030, Month.APRIL, 28));

        String response = mockMvc.perform(post("/api/cards")
                        .with(TestJwt.user(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long cardId = objectMapper.readTree(response).get("id").asLong();

        PaymentCardUpdateDto updateDto = new PaymentCardUpdateDto(
                "JANE DOD", LocalDate.of(2031, Month.APRIL, 28), false);

        mockMvc.perform(put("/api/cards/{id}", cardId)
                        .with(TestJwt.admin(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holder").value("JANE DOD"))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void shouldDeleteCard() throws Exception {
        PaymentCardCreateDto createDto = new PaymentCardCreateDto(
                userId, "1111222233334444", "JOHN DOD", LocalDate.of(2030, Month.APRIL, 28));

        String response = mockMvc.perform(post("/api/cards")
                        .with(TestJwt.user(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long cardId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/api/cards/{id}", cardId)
                        .with(TestJwt.admin(userId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/cards/{id}", cardId)
                        .with(TestJwt.admin(userId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectCardCreationWhenLimitExceeded() throws Exception {
        for (int i = 0; i < 5; i++) {
            PaymentCardCreateDto createDto = new PaymentCardCreateDto(
                    userId, "123456789012345" + i, "JOHN DOD", LocalDate.of(2030, Month.APRIL, 28));
            mockMvc.perform(post("/api/cards")
                            .with(TestJwt.user(userId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDto)))
                    .andExpect(status().isCreated());
        }

        PaymentCardCreateDto exceedDto = new PaymentCardCreateDto(
                userId, "9999888877776666", "JOHN DOD", LocalDate.of(2030, Month.APRIL, 28));

        mockMvc.perform(post("/api/cards")
                        .with(TestJwt.user(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exceedDto)))
                .andExpect(status().isBadRequest());
    }
}