package com.vitkvsk.user_service.exception;

public class CardLimitExceededException extends BusinessException{
    public CardLimitExceededException(int maxCards) {
        super("User cannot have more than " + maxCards + " payment cards");
    }
}
