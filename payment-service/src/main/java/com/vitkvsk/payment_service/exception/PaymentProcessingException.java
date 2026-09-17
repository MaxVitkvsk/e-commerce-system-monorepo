package com.vitkvsk.payment_service.exception;

public class PaymentProcessingException extends PaymentException {
    public PaymentProcessingException(String message) {
        super(ExceptionCode.PAYMENT_PROCESSING_ERROR, message);
    }

    public PaymentProcessingException(String message, Throwable cause) {
        super(ExceptionCode.PAYMENT_PROCESSING_ERROR, message, cause);
    }
}
