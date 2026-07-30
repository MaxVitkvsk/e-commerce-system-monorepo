package com.vitkvsk.auth_service.config;

import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.*;
import org.springframework.context.ApplicationEvent;
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
        if (!realm.clients().findByClientId(p.getClientId()).isEmpty()) return;
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
        if (!realm.users().search(username, true).isEmpty()) return;

        UserRepresentation u = new UserRepresentation();
        u.setUsername(username);
        u.setEmail(username + "@local.dev");
        u.setEmailVerified(true);
        u.setEnabled(true);
        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(p.getAppAdminPassword());
        cred.setTemporary(false);
        u.setCredentials(Collections.singletonList(cred));
        realm.users().create(u);

        String id = realm.users().search(username, true).get(0).getId();
        realm.users().get(id).roles().realmLevel().add(List.of(realm.roles().get("admin").toRepresentation()));
    }

    @EventListener(ApplicationEvent.class)
    public void init() {
        ensureRealm();
        RealmResource realm = keycloak.realm(p.getRealm());
        ensureRole(realm, "admin");
        ensureRole(realm, "user");
        ensureClient(realm);
        ensureAdminUser(realm);
        log.info("Keycloak realm '{}' initialized", p.getRealm());
    }
}
