package com.vitkvsk.auth_service.dto;

public record ErrorResponse(int status, String error, String message) {}
