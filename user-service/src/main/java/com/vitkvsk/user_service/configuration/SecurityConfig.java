package com.vitkvsk.user_service.configuration;

import com.vitkvsk.user_service.security.InternalServiceFilter;
import com.vitkvsk.user_service.security.RealmRoleJwtConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String ROLE_ADMIN = "ADMIN";

    private final RealmRoleJwtConverter realmRoleJwtConverter;
    private final InternalServiceFilter internalServiceFilter;

    @SuppressWarnings("java:S4502")
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/users/internal/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users", "/api/cards").hasRole(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/api/users/*/status").hasRole(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/users/*", "/api/cards/*").hasRole(ROLE_ADMIN)
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(realmRoleJwtConverter))
                )

                .addFilterBefore(internalServiceFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Unauthorized"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                response.sendError(HttpStatus.FORBIDDEN.value(), "Forbidden"))
                )
                .build();
    }

    @Bean
    public FilterRegistrationBean<InternalServiceFilter> internalServiceFilterRegistration(
            InternalServiceFilter filter) {
        FilterRegistrationBean<InternalServiceFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }
}