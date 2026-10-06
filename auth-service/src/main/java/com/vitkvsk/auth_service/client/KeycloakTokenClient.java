package com.vitkvsk.auth_service.client;

import com.vitkvsk.auth_service.config.KeycloakProperties;
import com.vitkvsk.auth_service.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakTokenClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final KeycloakProperties keycloakProperties;

    private Map<String, Object> postForm(String url, MultiValueMap<String, String> form) {
        return postForm(url, form, null);
    }

    private Map<String, Object> postForm(String url, MultiValueMap<String, String> form, String basicAuth) {
        try {
            RestClient.RequestBodySpec spec = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED);
            if (basicAuth != null) {
                spec = spec.header(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth);
            }
            return spec.body(form).retrieve().body(MAP_TYPE);
        } catch (RestClientResponseException e) {
            log.warn("Keycloak token endpoint rejected request: {} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw AuthException.unauthorized("Invalid credentials or token");
        }
    }

    private MultiValueMap<String, String> clientForm() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", keycloakProperties.getClientId());
        form.add("client_secret", keycloakProperties.getClientSecret());
        return form;
    }

    private String oidc(String path) {
        return keycloakProperties.getUrl() + "/realms/" + keycloakProperties.getRealm()
                + "/protocol/openid-connect/" + path;
    }

    @Retry(name = "remote")
    public Map<String, Object> passwordGrant(String username, String password) {
        MultiValueMap<String, String> form = clientForm();
        form.add("grant_type", "password");
        form.add("username", username);
        form.add("password", password);
        return postForm(oidc("token"), form);
    }

    @Retry(name = "remote")
    public Map<String, Object> refreshGrant(String refreshToken) {
        MultiValueMap<String, String> form = clientForm();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);
        return postForm(oidc("token"), form);
    }

    @Retry(name = "remote")
    public Map<?, ?> introspect(String token) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("token", token);
        try {
            return restClient.post()
                    .uri(oidc("token/introspect"))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .headers(h -> h.setBasicAuth(
                            keycloakProperties.getClientId(), keycloakProperties.getClientSecret()))
                    .body(form)
                    .retrieve()
                    .body(MAP_TYPE);
        } catch (HttpClientErrorException e) {
            log.warn("Introspect rejected: {} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }
}