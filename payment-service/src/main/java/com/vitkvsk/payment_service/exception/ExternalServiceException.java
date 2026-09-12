package com.vitkvsk.payment_service.exception;

public class ExternalServiceException extends PaymentException {
    public ExternalServiceException(String serviceName, String reason) {
        super(ExceptionCode.EXTERNAL_SERVICE_UNAVAILABLE,
                "External service '" + serviceName + "' failed: " + reason,
                serviceName, reason);
    }

    public ExternalServiceException(String serviceName, Throwable cause) {
        super(ExceptionCode.EXTERNAL_SERVICE_UNAVAILABLE,
                "External service '" + serviceName + "' unavailable: " + cause.getMessage(),
                cause,
                serviceName);
    }
}
