package com.example.carins.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PolicyUpsertRequest(
        String provider,                  // optional per current spec
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) { }