
package com.vitkvsk.gateway.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ReactiveRealmRoleConverter implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    @Override
    @SuppressWarnings("unchecked")
    public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
        var authorities = Optional.ofNullable(jwt.getClaimAsMap("realm_access"))
                .map(m -> (Collection<String>) m.getOrDefault("roles", List.of()))
                .stream()
                .flatMap(Collection::stream)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toSet());
        return Mono.just(new JwtAuthenticationToken(jwt, authorities));
    }
}
