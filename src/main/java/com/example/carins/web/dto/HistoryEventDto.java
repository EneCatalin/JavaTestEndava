package com.example.carins.web.dto;

import com.example.carins.constants.HistoryEventType;

import java.time.LocalDate;

public record HistoryEventDto(
        LocalDate date,
        HistoryEventType type,
        String description,
        Long refId

) {}