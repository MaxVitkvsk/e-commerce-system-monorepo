package com.vitkvsk.auth_service.client;

import com.vitkvsk.auth_service.config.RetryConfig;
import com.vitkvsk.auth_service.dto.RegisterRequest;
import com.vitkvsk.auth_service.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Service-Token";

    private final RestTemplate rest;

    @Value("${app.user-service-url}") private String userServiceUrl;
    @Value("${app.internal-secret}")  private String internalSecret;

    @Retryable(includes = {ResourceAccessException.class, HttpServerErrorException.class},
            maxRetries = RetryConfig.MAX_RETRIES, delay = RetryConfig.DELAY_MS,
            multiplier = RetryConfig.MULTIPLIER, jitter = RetryConfig.JITTER_MS)
    public void createProfile(String keycloakId, RegisterRequest req) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set(INTERNAL_TOKEN_HEADER, internalSecret);

        Map<String, Object> profile = Map.of(
                "name", req.name(),
                "surname", req.surname(),
                "birthDate", req.birthDate().toString(),
                "email", req.email());
        Map<String, Object> body = Map.of("id", keycloakId, "user", profile);

        try {
            rest.postForEntity(userServiceUrl + "/api/users/internal", new HttpEntity<>(body, h), Void.class);
        } catch (HttpClientErrorException e) {
            throw AuthException.badRequest("Profile creation rejected: " + e.getStatusCode());
        }
    }

    @Retryable(includes = {ResourceAccessException.class, HttpServerErrorException.class},
            maxRetries = RetryConfig.MAX_RETRIES, delay = RetryConfig.DELAY_MS,
            multiplier = RetryConfig.MULTIPLIER, jitter = RetryConfig.JITTER_MS)
    public void deleteProfile(UUID userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(INTERNAL_TOKEN_HEADER, internalSecret);
        rest.exchange(userServiceUrl + "/api/users/internal/" + userId,
                HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
    }
}