package com.vitkvsk.payment_service.exception;

public class InvalidPaymentDataException extends PaymentException {
    public InvalidPaymentDataException(String field, String reason) {
        super(ExceptionCode.INVALID_PAYMENT_DATA,
                "Invalid payment data: field '" + field + "' - " + reason,
                field, reason);
    }

    public InvalidPaymentDataException(String message) {
        super(ExceptionCode.INVALID_PAYMENT_DATA, message);
    }
}
