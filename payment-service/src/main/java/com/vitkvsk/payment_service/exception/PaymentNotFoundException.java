package com.vitkvsk.payment_service.exception;

public class PaymentNotFoundException extends PaymentException {
    public PaymentNotFoundException(String paymentId) {
        super(ExceptionCode.PAYMENT_NOT_FOUND,
                "Payment not found: " + paymentId,
                paymentId);
    }

    public PaymentNotFoundException(Long orderId) {
        super(ExceptionCode.PAYMENT_NOT_FOUND,
                "Payment for order not found: " + orderId,
                orderId);
    }
}
