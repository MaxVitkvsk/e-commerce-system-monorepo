package com.vitkvsk.payment_service.exception;

import lombok.Getter;

@Getter
public abstract class PaymentException extends RuntimeException {
    private final ExceptionCode exceptionCode;
    private final transient Object[] args;

    protected PaymentException(ExceptionCode exceptionCode, String message, Object... args) {
        super(message);
        this.exceptionCode = exceptionCode;
        this.args = args;
    }

    protected PaymentException(ExceptionCode exceptionCode, String message, Throwable cause, Object... args) {
        super(message, cause);
        this.exceptionCode = exceptionCode;
        this.args = args;
    }
}