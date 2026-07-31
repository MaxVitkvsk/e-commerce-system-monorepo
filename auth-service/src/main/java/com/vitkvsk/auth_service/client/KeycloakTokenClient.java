package com.vitkvsk.auth_service.client;

import com.vitkvsk.auth_service.config.KeycloakProperties;
import com.vitkvsk.auth_service.config.RetryConfig;
import com.vitkvsk.auth_service.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakTokenClient {

    private final RestTemplate rest;
    private final KeycloakProperties kc;

    @Retryable(includes = {ResourceAccessException.class, HttpServerErrorException.class},
            maxRetries = RetryConfig.MAX_RETRIES, delay = RetryConfig.DELAY_MS,
            multiplier = RetryConfig.MULTIPLIER, jitter = RetryConfig.JITTER_MS)
    public Map<String, Object> passwordGrant(String username, String password) {
        MultiValueMap<String, String> form = clientForm();
        form.add("grant_type", "password");
        form.add("username", username);
        form.add("password", password);
        return postForm(oidc("token"), form);
    }

    @Retryable(includes = {ResourceAccessException.class, HttpServerErrorException.class},
            maxRetries = RetryConfig.MAX_RETRIES, delay = RetryConfig.DELAY_MS,
            multiplier = RetryConfig.MULTIPLIER, jitter = RetryConfig.JITTER_MS)
    public Map<String, Object> refreshGrant(String refreshToken) {
        MultiValueMap<String, String> form = clientForm();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);
        return postForm(oidc("token"), form);
    }

    @Retryable(includes = {ResourceAccessException.class, HttpServerErrorException.class},
            maxRetries = RetryConfig.MAX_RETRIES, delay = RetryConfig.DELAY_MS,
            multiplier = RetryConfig.MULTIPLIER, jitter = RetryConfig.JITTER_MS)
    public Map<?, ?> introspect(String token) {
        HttpHeaders h = formHeaders();
        h.setBasicAuth(kc.getClientId(), kc.getClientSecret());
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("token", token);
        try {
            return rest.postForObject(oidc("token/introspect"), new HttpEntity<>(form, h), Map.class);
        } catch (HttpClientErrorException e) {
            log.warn("Introspect rejected: {} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }

    private Map<String, Object> postForm(String url, MultiValueMap<String, String> form) {
        try {
            return rest.postForObject(url, new HttpEntity<>(form, formHeaders()), Map.class);
        } catch (HttpClientErrorException e) {
            log.warn("Keycloak token endpoint rejected request: {} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw AuthException.unauthorized("Invalid credentials or token");
        }
    }

    private MultiValueMap<String, String> clientForm() {
        MultiValueMap<String, String> f = new LinkedMultiValueMap<>();
        f.add("client_id", kc.getClientId());
        f.add("client_secret", kc.getClientSecret());
        return f;
    }

    private HttpHeaders formHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return h;
    }

    private String oidc(String path) {
        return kc.getUrl() + "/realms/" + kc.getRealm() + "/protocol/openid-connect/" + path;
    }
}