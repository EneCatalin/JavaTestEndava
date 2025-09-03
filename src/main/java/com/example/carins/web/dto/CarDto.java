package com.example.carins.web.dto;

public record CarDto(Long id, String vin, String make, String model, int year, Long ownerId, String ownerName, String ownerEmail) {}
