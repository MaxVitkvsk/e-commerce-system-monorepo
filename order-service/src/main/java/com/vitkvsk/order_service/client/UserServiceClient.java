package com.vitkvsk.order_service.client;

import com.vitkvsk.order_service.dto.UserInfoDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Service-Token";

    private final RestTemplate rest;

    @Value("${app.user-service-url}") private String userServiceUrl;
    @Value("${app.internal-secret}")  private String internalSecret;

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserInfoFallback")
    @Retryable(includes = {ResourceAccessException.class, HttpServerErrorException.class},
            maxRetries = 2, delay = 500, multiplier = 2.0, jitter = 250)
    public UserInfoDto getUserInfo(UUID userId) {
        return rest.exchange(userServiceUrl + "/api/users/internal/" + userId,
                        HttpMethod.GET, new HttpEntity<>(internalHeaders()), UserInfoDto.class)
                .getBody();
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getUsersFallback")
    @Retryable(includes = {ResourceAccessException.class, HttpServerErrorException.class},
            maxRetries = 2, delay = 500, multiplier = 2.0, jitter = 250)
    public Map<UUID, UserInfoDto> getUsersByIds(Collection<UUID> ids) {
        String joined = ids.stream().map(UUID::toString).collect(Collectors.joining(","));
        UserInfoDto[] arr = rest.exchange(
                        userServiceUrl + "/api/users/internal/ids?ids=" + joined,
                        HttpMethod.GET, new HttpEntity<>(internalHeaders()), UserInfoDto[].class)
                .getBody();
        return arr == null ? Map.of()
                : Arrays.stream(arr).collect(Collectors.toMap(UserInfoDto::id, Function.identity()));
    }

    private UserInfoDto getUserInfoFallback(UUID userId, Throwable t) {
        log.warn("user-service unavailable for user {}: {}", userId, t.toString());
        return null;
    }

    private Map<UUID, UserInfoDto> getUsersFallback(Collection<UUID> ids, Throwable t) {
        log.warn("user-service batch unavailable: {}", t.toString());
        return Map.of();
    }

    private HttpHeaders internalHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.set(INTERNAL_TOKEN_HEADER, internalSecret);
        return h;
    }
}
