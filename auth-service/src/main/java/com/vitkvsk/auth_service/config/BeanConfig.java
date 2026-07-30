package com.vitkvsk.auth_service.config;


import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class BeanConfig {

    @Bean
    public RestTemplate restTemplate() {return new RestTemplate(); }

    @Bean
    public Keycloak keycloak(KeycloakProperties p) {
        return KeycloakBuilder.builder()
                .serverUrl(p.getUrl())
                .realm(p.getRealm())
                .clientId(p.getClientId())
                .username(p.getAdminUser())
                .password(p.getAdminPassword())
                .build();
    }
}
