package com.vitkvsk.auth_service.config;

import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.*;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakRealmInitializer {

    private final Keycloak keycloak;
    private final KeycloakProperties p;

    private void ensureRealm() {
        try { keycloak.realm(p.getRealm()).toRepresentation(); }
        catch (NotFoundException e) {
            RealmRepresentation r = new RealmRepresentation();
            r.setRealm(p.getRealm());
            r.setEnabled(true);
            r.setRegistrationAllowed(false);
            r.setAccessTokenLifespan(900);
            r.setPasswordPolicy("hashAlgorithm(pbkdf2-sha512) and hashIterations(600000)");
            keycloak.realms().create(r);
        }
    }

    private void ensureRole(RealmResource realm, String name) {
        try { realm.roles().get(name).toRepresentation(); }
        catch (NotFoundException e) {
            RoleRepresentation role = new RoleRepresentation();
            role.setName(name);
            realm.roles().create(role);
        }
    }

    private void ensureClient(RealmResource realm) {
        List<ClientRepresentation> found = realm.clients().findByClientId(p.getClientId());
        if (!found.isEmpty()) {
            ClientRepresentation existing = found.get(0);
            boolean changed = false;
            if (p.getClientSecret() != null && !p.getClientSecret().equals(existing.getSecret())) {
                existing.setSecret(p.getClientSecret());
                changed = true;
            }
            if (!Boolean.TRUE.equals(existing.isDirectAccessGrantsEnabled())) {
                existing.setDirectAccessGrantsEnabled(true);
                changed = true;
            }
            if (changed) {
                realm.clients().get(existing.getId()).update(existing);
                log.info("Client '{}' secret/grants synchronized", p.getClientId());
            }
            return;
        }
        ClientRepresentation c = new ClientRepresentation();
        c.setClientId(p.getClientId());
        c.setEnabled(true);
        c.setPublicClient(false);
        c.setSecret(p.getClientSecret());
        c.setDirectAccessGrantsEnabled(true);
        c.setStandardFlowEnabled(true);
        c.setServiceAccountsEnabled(true);
        c.setProtocol("openid-connect");
        c.setRedirectUris(List.of("*")); 
        realm.clients().create(c);
    }

    private void ensureAdminUser(RealmResource realm) {
        String username = p.getAppAdminUsername();
        List<UserRepresentation> found = realm.users().search(username, true);
        String id;
        if (!found.isEmpty()) {
            id = found.get(0).getId();
            UserRepresentation existing = realm.users().get(id).toRepresentation();
            existing.setEnabled(true);
            existing.setEmailVerified(true);
            existing.setFirstName("System");
            existing.setLastName("Administrator");
            existing.setRequiredActions(Collections.emptyList());
            realm.users().get(id).update(existing);
            realm.users().get(id).resetPassword(passwordCredential(p.getAppAdminPassword()));
            log.info("Admin user '{}' synchronized (profile + password)", username);
        } else {
            id = createAdmin(realm, username);
        }
        boolean hasAdmin = realm.users().get(id).roles().realmLevel().listAll().stream()
                .anyMatch(r -> Roles.ADMIN.equals(r.getName()));
        if (!hasAdmin) {
            realm.users().get(id).roles().realmLevel()
                    .add(List.of(realm.roles().get(Roles.ADMIN).toRepresentation()));
        }
    }

    private String createAdmin(RealmResource realm, String username) {
        UserRepresentation u = new UserRepresentation();
        u.setUsername(username);
        u.setEmail(username + "@local.dev");
        u.setEmailVerified(true);
        u.setEnabled(true);
        u.setFirstName("System");
        u.setLastName("Administrator");
        u.setRequiredActions(Collections.emptyList());
        u.setCredentials(Collections.singletonList(passwordCredential(p.getAppAdminPassword())));
        realm.users().create(u);
        log.info("Admin user '{}' created", username);
        return realm.users().search(username, true).get(0).getId();
    }

    private CredentialRepresentation passwordCredential(String value) {
        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(value);
        cred.setTemporary(false);
        return cred;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        ensureRealm();
        RealmResource realm = keycloak.realm(p.getRealm());
        ensureRole(realm, Roles.ADMIN);
        ensureRole(realm, Roles.USER);
        ensureClient(realm);
        ensureAdminUser(realm);
        log.info("Keycloak realm '{}' initialized", p.getRealm());
    }
}