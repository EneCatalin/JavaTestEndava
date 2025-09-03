package com.example.carins.exception.policy;

import com.example.carins.exception.ApiException;
import org.springframework.http.HttpStatus;

public class PolicyEndDateException extends ApiException {
    public PolicyEndDateException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, message); // 422 is great for validation
    }
}