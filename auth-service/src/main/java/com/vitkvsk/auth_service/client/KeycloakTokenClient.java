package com.vitkvsk.auth_service.client;

import com.vitkvsk.auth_service.config.KeycloakProperties;
import com.vitkvsk.auth_service.exception.AuthException;
import lombok.RequiredArgsConstructor;
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

@Component
@RequiredArgsConstructor
public class KeycloakTokenClient {

    private final RestTemplate rest;
    private final KeycloakProperties kc;

    private Map<String, Object> call(MultiValueMap<String, String> form) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        try {
            return rest.postForObject(tokenUrl(), new HttpEntity<>(form, h), Map.class);
        } catch (HttpClientErrorException e) {
            throw AuthException.unauthorized("Invalid credentials or token");
        }
    }

    private MultiValueMap<String, String> base() {
        MultiValueMap<String, String> f = new LinkedMultiValueMap<>();
        f.add("client_id", kc.getClientId());
        f.add("client_secret", kc.getClientSecret());
        return f;
    }

    private String tokenUrl()      { return kc.getUrl() + "/realms/" + kc.getRealm() + "/protocol/openid-connect/token"; }
    private String introspectUrl() { return kc.getUrl() + "/realms/" + kc.getRealm() + "/protocol/openid-connect/token/introspect"; }

    @Retryable(includes = {ResourceAccessException.class, HttpServerErrorException.class},
            maxRetries = 2, delay = 500, multiplier = 2.0, jitter = 250) // 1+2 = 3 попытки
    public Map<String, Object> passwordGrant(String username, String password) {
        MultiValueMap<String, String> form = base();
        form.add("grant_type", "password");
        form.add("username", username);
        form.add("password", password);
        return call(form);
    }

    @Retryable(includes = {ResourceAccessException.class, HttpServerErrorException.class},
            maxRetries = 2, delay = 500, multiplier = 2.0, jitter = 250)
    public Map<String, Object> refreshGrant(String refreshToken) {
        MultiValueMap<String, String> form = base();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);
        return call(form);
    }

    @Retryable(includes = {ResourceAccessException.class, HttpServerErrorException.class},
            maxRetries = 2, delay = 500, multiplier = 2.0, jitter = 250)
    public Map<?, ?> introspect(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        h.setBasicAuth(kc.getClientId(), kc.getClientSecret());
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("token", token);
        return rest.postForObject(introspectUrl(), new HttpEntity<>(form, h), Map.class);
    }
}
