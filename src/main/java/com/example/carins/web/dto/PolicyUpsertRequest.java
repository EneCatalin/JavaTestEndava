package com.example.carins.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PolicyUpsertRequest(
        String provider,
        @NotNull(message="startDate is required") LocalDate startDate,
        @NotNull(message="endDate is required")  LocalDate endDate
) { }