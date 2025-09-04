package com.example.carins.exception;

import com.example.carins.exception.dto.ApiError;
import com.example.carins.exception.dto.FieldErrorDto;
import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;




@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException ex, HttpServletRequest req) {
        log.warn("API error at {} {} -> {}: {}", req.getMethod(), req.getRequestURI(),
                ex.status().value(), rootCauseMessage(ex), ex);
        return build(ex.status(), "Request failed", ex, req, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        String target = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "value";
        String msg = ex.getName().equals("date")
                ? "Invalid date format, expected YYYY-MM-DD"
                : "Invalid value for parameter '" + ex.getName() + "' (expected " + target + ")";
        return build(HttpStatus.BAD_REQUEST, msg, ex, req, null);
    }
    // ---- ResponseStatusException (if any remain) ----
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String msg = StringUtils.hasText(ex.getReason()) ? ex.getReason() : "Request failed";
        log.warn("RSE at {} {} -> {}: {}", req.getMethod(), req.getRequestURI(),
                status.value(), rootCauseMessage(ex), ex);
        return build(status, msg, ex, req, null);
    }

    // ---- @Valid on @RequestBody ----
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                 HttpServletRequest req) {
        List<FieldErrorDto> details = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                details.add(new FieldErrorDto(fe.getField(), fe.getDefaultMessage())));
        log.debug("Validation error at {} {}: {}", req.getMethod(), req.getRequestURI(), details);
        return build(HttpStatus.BAD_REQUEST, "Validation failed", ex, req, details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex,
                                                              HttpServletRequest req) {
        List<FieldErrorDto> details = new ArrayList<>();
        for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
            details.add(new FieldErrorDto(v.getPropertyPath().toString(), v.getMessage()));
        }
        log.debug("Constraint violations at {} {}: {}", req.getMethod(), req.getRequestURI(), details);
        return build(HttpStatus.BAD_REQUEST, "Validation failed", ex, req, details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleNotReadable(HttpMessageNotReadableException ex,
                                                      HttpServletRequest req) {
        String msg = "Malformed JSON";
        Throwable cause = ex.getCause();
        if (cause instanceof JsonMappingException jme) {
            String pathRef = jme.getPath() != null && !jme.getPath().isEmpty()
                    ? " at " + jme.getPathReference()
                    : "";
            msg = "Malformed JSON" + pathRef;
        }
        log.warn("Bad JSON at {} {}: {}", req.getMethod(), req.getRequestURI(), rootCauseMessage(ex));
        return build(HttpStatus.BAD_REQUEST, msg, ex, req, null);
    }

    // ---- DB constraint errors ----
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex,
                                                        HttpServletRequest req) {
        log.error("Data integrity violation at {} {}: {}", req.getMethod(), req.getRequestURI(), rootCauseMessage(ex), ex);
        return build(HttpStatus.CONFLICT, "Data integrity violation", ex, req, null);
    }

    // ---- Wrong HTTP method ----
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                             HttpServletRequest req) {
        log.debug("Method not allowed at {} {}: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed", ex, req, null);
    }

    // ---- Common Spring Errors ----
    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        String msg = "Missing required parameter '" + ex.getParameterName() + "'";
        return build(HttpStatus.BAD_REQUEST, msg, ex, req, null);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiError> handleBind(BindException ex, HttpServletRequest req) {
        var details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorDto(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", ex, req, details);
    }

    //? this can be edited back in, just remember to edit the properties file after
//    @ExceptionHandler(org.springframework.web.servlet.NoHandlerFoundException.class)
//    public ResponseEntity<ApiError> handleNoHandler(NoHandlerFoundException ex, HttpServletRequest req) {
//        return build(HttpStatus.NOT_FOUND, "No handler for " + ex.getHttpMethod() + " " + ex.getRequestURL(), ex, req, null);
//    }

    // ---- Fallback ----
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception at {} {}", req.getMethod(), req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", ex, req, null);
    }

    private ResponseEntity<ApiError> build(HttpStatus status,
                                           String message,
                                           Exception ex,
                                           HttpServletRequest req,
                                           List<FieldErrorDto> details) {
        ApiError body = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                ex.getClass().getName(),
                req.getRequestURI(),
                details
        );
        return ResponseEntity.status(status).body(body);
    }

    private static String rootCauseMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null) cur = cur.getCause();
        String cls = cur.getClass().getSimpleName();
        String msg = cur.getMessage();
        return (msg == null || msg.isBlank()) ? cls : cls + ": " + msg;
    }
}