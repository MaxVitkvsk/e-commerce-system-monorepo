package com.vitkvsk.user_service.IT;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TestJwt {
    private TestJwt() {}

    public static RequestPostProcessor user(UUID userId) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(j -> j.subject(userId.toString())
                        .claim("realm_access", Map.of("roles", List.of("user"))))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    public static RequestPostProcessor admin() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(j -> j.subject(UUID.randomUUID().toString())
                        .claim("realm_access", Map.of("roles", List.of("admin"))))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    public static RequestPostProcessor admin(UUID userId) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(j -> j.subject(userId.toString())
                        .claim("realm_access", Map.of("roles", List.of("admin"))))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
