package com.example.carins.exception.policy;

public class PolicyEndDateException extends PolicyOperationException{
    public PolicyEndDateException(String message) {
        super(message);
    }

    public PolicyEndDateException(String message, Throwable cause) {
        super(message, cause);
    }
}
