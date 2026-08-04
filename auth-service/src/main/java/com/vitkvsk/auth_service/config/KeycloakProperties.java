package com.vitkvsk.auth_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {
    private String
            url,
            realm,
            clientId,
            clientSecret,
            adminRealm,
            adminClientId,
            adminUser,
            adminPassword,
            appAdminUsername,
            appAdminPassword;
}