package com.example.carins.exception.policy;

public class PolicyOperationException extends RuntimeException {
    public PolicyOperationException(String message) {
        super(message);
    }

    public PolicyOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}