package com.example.carins.exception.policy;

import com.example.carins.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {
    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
    public ResourceNotFoundException(String message, Throwable cause) {
        super(HttpStatus.NOT_FOUND, message);
    }
}