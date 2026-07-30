package com.vitkvsk.auth_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {
    private String url, realm, clientId, clientSecret, adminUser, adminPassword, appAdminUsername, appAdminPassword;

}
