package com.vitkvsk.auth_service.service;

import com.vitkvsk.auth_service.client.KeycloakTokenClient;
import com.vitkvsk.auth_service.client.UserServiceClient;
import com.vitkvsk.auth_service.config.KeycloakProperties;
import com.vitkvsk.auth_service.dto.AuthRequest;
import com.vitkvsk.auth_service.dto.AuthResponse;
import com.vitkvsk.auth_service.dto.RefreshRequest;
import com.vitkvsk.auth_service.dto.RegisterRequest;
import com.vitkvsk.auth_service.dto.ValidateRequest;
import com.vitkvsk.auth_service.dto.ValidateResponse;
import com.vitkvsk.auth_service.exception.AuthException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final Keycloak keycloak;
    private final KeycloakProperties kc;
    private final KeycloakTokenClient tokenClient;
    private final UserServiceClient userServiceClient;

    private AuthResponse build(Map<String, Object> token) {
        return new AuthResponse((String) token.get("access_token"), (String) token.get("refresh_token"),
                "Bearer", ((Number) token.getOrDefault("expires_in", 0)).longValue());
    }

    private String primaryRole(String userId) {
        try {
            return keycloak.realm(kc.getRealm()).users().get(userId).roles().realmLevel().listAll().stream()
                    .map(RoleRepresentation::getName)
                    .filter(n -> "admin".equals(n) || "user".equals(n))
                    .findFirst().orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    public AuthResponse register(RegisterRequest req) {
        RealmResource realm = keycloak.realm(kc.getRealm());
        if (!realm.users().search(req.username(), true).isEmpty())
            throw AuthException.conflict("Username already exists");

        UserRepresentation u = new UserRepresentation();
        u.setUsername(req.username());
        u.setEmail(req.email());
        u.setFirstName(req.firstName());
        u.setLastName(req.lastName());
        u.setEnabled(true);
        u.setEmailVerified(true);
        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(req.password());
        cred.setTemporary(false);
        u.setCredentials(Collections.singletonList(cred));

        String keycloakId;
        try (Response resp = realm.users().create(u)) {
            if (resp.getStatus() != 201) throw AuthException.badRequest("Keycloak user creation failed");
            keycloakId = realm.users().search(req.username(), true).get(0).getId();
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            throw AuthException.badRequest("Keycloak error: " + e.getMessage());
        }

        realm.users().get(keycloakId).roles().realmLevel()
                .add(List.of(realm.roles().get("user").toRepresentation()));

        try {
            userServiceClient.createProfile(keycloakId, req);
        } catch (Exception e) {
            log.error("profile creation failed after retries, compensating keycloak user {}", keycloakId, e);
            try { realm.users().get(keycloakId).remove(); } catch (Exception ignored) {}
            throw AuthException.badRequest("Profile creation failed: " + e.getMessage());
        }
        return login(new AuthRequest(req.username(), req.password()));
    }

    public AuthResponse login(AuthRequest req) {
        return build(tokenClient.passwordGrant(req.username(), req.password()));
    }

    public AuthResponse refresh(RefreshRequest req) {
        return build(tokenClient.refreshGrant(req.refreshToken()));
    }

    public ValidateResponse validate(ValidateRequest req) {
        try {
            Map<?, ?> body = tokenClient.introspect(req.token());
            if (body == null || !Boolean.TRUE.equals(body.get("active")))
                return new ValidateResponse(false, null, null, "Token is not active");
            String sub = (String) body.get("sub");
            return new ValidateResponse(true, sub, primaryRole(sub), "valid");
        } catch (Exception e) {
            return new ValidateResponse(false, null, null, "Validation error: " + e.getMessage());
        }
    }

}
