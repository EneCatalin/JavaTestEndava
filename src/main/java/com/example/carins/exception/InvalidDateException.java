package com.example.carins.exception;

import org.springframework.http.HttpStatus;

public class InvalidDateException extends ApiException {
    public InvalidDateException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
    public InvalidDateException(String message, Throwable cause) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}