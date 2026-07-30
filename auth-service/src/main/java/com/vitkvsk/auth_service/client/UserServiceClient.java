package com.vitkvsk.auth_service.client;

import com.vitkvsk.auth_service.dto.RegisterRequest;
import com.vitkvsk.auth_service.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestTemplate rest;

    @Value("${app.user-service-url}") private String userServiceUrl;
    @Value("${app.internal-secret}")  private String internalSecret;

    @Retryable(includes = {ResourceAccessException.class, HttpServerErrorException.class},
            maxRetries = 2, delay = 500, multiplier = 2.0, jitter = 250)
    public void createProfile(String keycloakId, RegisterRequest req) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("X-Internal-Service-Token", internalSecret);
        Map<String, Object> body = Map.of(
                "id", keycloakId, "username", req.username(), "email", req.email(),
                "firstName", req.firstName(), "lastName", req.lastName(), "active", true);
        try {
            rest.postForEntity(userServiceUrl + "/api/users/internal", new HttpEntity<>(body, h), Void.class);
        } catch (HttpClientErrorException e) {
            throw AuthException.badRequest("Profile creation rejected: " + e.getStatusCode());
        }
    }
}
