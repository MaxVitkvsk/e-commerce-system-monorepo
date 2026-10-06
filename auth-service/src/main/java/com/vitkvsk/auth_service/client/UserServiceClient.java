package com.vitkvsk.auth_service.client;

import com.vitkvsk.auth_service.dto.RegisterRequest;
import com.vitkvsk.auth_service.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Service-Token";

    private final RestClient restClient;

    @Value("${app.user-service-url}") private String userServiceUrl;
    @Value("${app.internal-secret}")  private String internalSecret;

    @Retry(name = "remote")
    public void createProfile(String keycloakId, RegisterRequest req) {
        Map<String, Object> profile = Map.of(
                "name", req.name(),
                "surname", req.surname(),
                "birthDate", req.birthDate().toString(),
                "email", req.email());
        Map<String, Object> body = Map.of("id", keycloakId, "user", profile);

        try {
            restClient.post()
                    .uri(userServiceUrl + "/api/users/internal")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(INTERNAL_TOKEN_HEADER, internalSecret)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw AuthException.badRequest("Profile creation rejected: " + e.getStatusCode());
        }
    }

    @Retry(name = "remote")
    public void deleteProfile(UUID userId) {
        restClient.delete()
                .uri(userServiceUrl + "/api/users/internal/" + userId)
                .header(INTERNAL_TOKEN_HEADER, internalSecret)
                .retrieve()
                .toBodilessEntity();
    }
}