package com.vitkvsk.auth_service.config;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@EnableResilientMethods
public class BeanConfig {

    @Bean
    public RestTemplate restTemplate(
            @Value("${http.client.connect-timeout}") Duration connectTimeout,
            @Value("${http.client.read-timeout}") Duration readTimeout) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return new RestTemplate(factory);
    }

    @Bean
    public Keycloak keycloak(KeycloakProperties p) {
        return KeycloakBuilder.builder()
                .serverUrl(p.getUrl())
                .realm(p.getAdminRealm())
                .clientId(p.getAdminClientId())
                .username(p.getAdminUser())
                .password(p.getAdminPassword())
                .build();
    }
}