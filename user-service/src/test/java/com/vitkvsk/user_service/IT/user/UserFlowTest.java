package com.vitkvsk.user_service.IT.user;


import com.vitkvsk.user_service.IntegrationTest;
import com.vitkvsk.user_service.dto.user.UserCreateDto;
import com.vitkvsk.user_service.dto.user.UserUpdateDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
public class UserFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateAndGetUser() throws Exception {
        UserCreateDto createDto = new UserCreateDto(
                "Max", "Dod", LocalDate.of(1990, 1, 1), "max.dod@test.com"
        );

        String response = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Max"))
                .andReturn().getResponse().getContentAsString();

        Long userId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value("max.dod@test.com"));
    }

    @Test
    void shouldUpdateUser() throws Exception {
        UserCreateDto createDto = new UserCreateDto(
                "Alice", "Dod", LocalDate.of(1995, 5, 15), "alice.dod@test.com"
        );

        String response = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long userId = objectMapper.readTree(response).get("id").asLong();

        UserUpdateDto updateDto = new UserUpdateDto(
                "Alice", "Smith", LocalDate.of(1995, 5, 15), "alice.smith@test.com"
        );

        mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.surname").value("Smith"))
                .andExpect(jsonPath("$.email").value("alice.smith@test.com"));
    }

    @Test
    void shouldReturnNotFoundForNonExistentUser() throws Exception {
        mockMvc.perform(get("/api/users/{id}", 99999L))
                .andExpect(status().isNotFound());
    }
}