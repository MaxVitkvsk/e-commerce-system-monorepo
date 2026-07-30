package com.vitkvsk.auth_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AuthException extends RuntimeException {
    private final HttpStatus status;
    public AuthException(HttpStatus status, String message) { super(message); this.status = status; }
    public static AuthException unauthorized(String m) { return new AuthException(HttpStatus.UNAUTHORIZED, m); }
    public static AuthException conflict(String m)     { return new AuthException(HttpStatus.CONFLICT, m); }
    public static AuthException badRequest(String m)   { return new AuthException(HttpStatus.BAD_REQUEST, m); }
}
