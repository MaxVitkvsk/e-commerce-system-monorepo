package com.vitkvsk.user_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class InternalServiceFilter extends OncePerRequestFilter {
    @Value("${app.internal-secret}") private String secret;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        return !req.getRequestURI().startsWith("/api/users/internal");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (!secret.equals(req.getHeader("X-Internal-Service-Token"))) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "invalid internal token");
            return;
        }
        chain.doFilter(req, res);
    }
}
