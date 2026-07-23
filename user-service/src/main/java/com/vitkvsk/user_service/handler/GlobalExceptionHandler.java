package com.vitkvsk.user_service.handler;

import com.vitkvsk.user_service.dto.ErrorResponse;
import com.vitkvsk.user_service.exception.CardLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException responseStatusException){
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                responseStatusException.getStatusCode().value(),
                responseStatusException.getMessage(),
                responseStatusException.getReason()

        );

        return new ResponseEntity<>(errorResponse, responseStatusException.getStatusCode());

    }

    @ExceptionHandler(CardLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleCardLimit(CardLimitExceededException cardLimitExceededException) {
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                cardLimitExceededException.getMessage()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception exception) {
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred"
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
