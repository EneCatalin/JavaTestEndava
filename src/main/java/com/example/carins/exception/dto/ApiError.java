package com.example.carins.exception.dto;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String exception,
        String path,
        List<FieldErrorDto> details
) {}