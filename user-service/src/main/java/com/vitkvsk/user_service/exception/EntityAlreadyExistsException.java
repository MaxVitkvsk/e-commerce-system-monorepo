package com.vitkvsk.user_service.exception;

public class EntityAlreadyExistsException extends BusinessException{
    public EntityAlreadyExistsException(String message) {
        super(message);
    }
}
