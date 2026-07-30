package com.vitkvsk.auth_service.dto;

public record ValidateResponse(boolean valid, String userId, String role, String message) {}
