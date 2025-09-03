package com.example.carins.exception;

import com.example.carins.exception.dto.FieldErrorDto;
import com.example.carins.exception.dto.ValidationErrorResponse;
import com.example.carins.exception.policy.PolicyEndDateException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest req) {

        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorDto(fe.getField(), fe.getDefaultMessage()))
                .toList();

        var status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(
                new ValidationErrorResponse(
                        Instant.now(),
                        status.value(),
                        "Validation failed",
                        "One or more fields are invalid.",
                        req.getRequestURI(),
                        errors
                )
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(
                new ErrorResponse(Instant.now(), status.value(),
                        "Malformed JSON or type mismatch", ex.getMostSpecificCause().getMessage(), req.getRequestURI())
        );
    }

    @ExceptionHandler(PolicyEndDateException.class)
    public ResponseEntity<ErrorResponse> handlePolicyEndDate(
            PolicyEndDateException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY; // 422 for business rule failures
        return ResponseEntity.status(status).body(
                new ErrorResponse(Instant.now(), status.value(),
                        "Policy validation failed", ex.getMessage(), req.getRequestURI())
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(
                new ErrorResponse(Instant.now(), status.value(),
                        "Internal Server Error", "Unexpected error", req.getRequestURI())
        );
    }
}