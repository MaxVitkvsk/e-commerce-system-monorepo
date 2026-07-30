package com.vitkvsk.user_service.configuration;


import com.vitkvsk.user_service.dto.error.ErrorResponse;
import com.vitkvsk.user_service.security.InternalServiceFilter;
import com.vitkvsk.user_service.security.RealmRoleJwtConverter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;


@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final RealmRoleJwtConverter jwtConverter;
    private final InternalServiceFilter internalFilter;
    private final ObjectMapper mapper;

    @Bean
    public SecurityFilterChain chain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) ->
                                writeJson(res, 401, "Unauthorized", ex.getMessage()))
                        .accessDeniedHandler((req, res, ex) ->
                                writeJson(res, 403, "Forbidden", ex.getMessage())))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/users/internal").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/cards").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/users/*/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/cards/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/cards/*/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/cards/*").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtConverter)))
                .addFilterBefore(internalFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void writeJson(HttpServletResponse res, int code, String err, String msg) throws IOException {
        res.setStatus(code);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(res.getWriter(), new ErrorResponse(LocalDateTime.now(), code, err, msg));
    }
}
