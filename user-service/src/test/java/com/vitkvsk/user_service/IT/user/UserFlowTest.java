package com.vitkvsk.user_service.IT.user;

import com.vitkvsk.user_service.IT.TestJwt;
import com.vitkvsk.user_service.IntegrationTest;
import com.vitkvsk.user_service.dto.user.UserUpdateDto;
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
class UserFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;

    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        User user = User.builder()
                .id(testUserId)
                .name("John")
                .surname("Dod")
                .email("john@test.com")
                .birthDate(LocalDate.of(1990, Month.APRIL, 1))
                .active(true)
                .build();
        userRepository.save(user);
    }

    @Test
    void shouldGetUserById() throws Exception {
        mockMvc.perform(get("/api/users/{id}", testUserId).with(TestJwt.user(testUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUserId.toString()))
                .andExpect(jsonPath("$.name").value("John"))
                .andExpect(jsonPath("$.email").value("john@test.com"));
    }

    @Test
    void shouldUpdateUser() throws Exception {
        UserUpdateDto updateDto = new UserUpdateDto(
                "John", "Smith", LocalDate.of(1990, Month.APRIL, 1), "john.smith@test.com");

        mockMvc.perform(put("/api/users/{id}", testUserId)
                        .with(TestJwt.user(testUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.surname").value("Smith"))
                .andExpect(jsonPath("$.email").value("john.smith@test.com"));
    }

    @Test
    void shouldChangeUserStatus() throws Exception {
        mockMvc.perform(patch("/api/users/{id}/status", testUserId)
                        .with(TestJwt.admin())
                        .param("active", "false"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldDeleteUser() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", testUserId).with(TestJwt.admin()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/{id}", testUserId).with(TestJwt.admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnNotFoundForNonExistentUser() throws Exception {
        UUID fakeId = UUID.randomUUID();
        mockMvc.perform(get("/api/users/{id}", fakeId).with(TestJwt.admin()))
                .andExpect(status().isNotFound());
    }
}