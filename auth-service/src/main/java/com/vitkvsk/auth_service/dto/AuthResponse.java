package com.vitkvsk.auth_service.dto;

public record AuthResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {}
