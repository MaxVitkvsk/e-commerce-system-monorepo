package com.vitkvsk.order_service.client;

import com.vitkvsk.order_service.dto.UserInfoDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private static final ParameterizedTypeReference<List<UserInfoDto>> USER_LIST =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserInfoFallback")
    @Retryable(includes = {ResourceAccessException.class, HttpServerErrorException.class},
            maxRetries = 2, delay = 500, multiplier = 2.0, jitter = 250)
    public UserInfoDto getUserInfo(UUID userId) {
        return restClient.get()
                .uri("/api/users/internal/{id}", userId)
                .retrieve()
                .body(UserInfoDto.class);
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getUsersFallback")
    @Retryable(includes = {ResourceAccessException.class, HttpServerErrorException.class},
            maxRetries = 2, delay = 500, multiplier = 2.0, jitter = 250)
    public Map<UUID, UserInfoDto> getUsersByIds(Collection<UUID> ids) {
        String joined = ids.stream().map(UUID::toString).collect(Collectors.joining(","));
        List<UserInfoDto> list = restClient.get()
                .uri("/api/users/internal/ids?ids={ids}", joined)
                .retrieve()
                .body(USER_LIST);

        return list == null ? Map.of()
                : list.stream().collect(Collectors.toMap(UserInfoDto::id, Function.identity()));
    }

    private UserInfoDto getUserInfoFallback(UUID userId, Throwable t) {
        log.warn("user-service unavailable for user {}: {}", userId, t.toString());
        return null;
    }

    private Map<UUID, UserInfoDto> getUsersFallback(Collection<UUID> ids, Throwable t) {
        log.warn("user-service batch unavailable: {}", t.toString());
        return Map.of();
    }
}