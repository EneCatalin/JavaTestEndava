package com.example.carins.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Entity
@Table(name = "insurancepolicy")
public class InsurancePolicy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Car car;

    private String provider;
    private LocalDate startDate;

    @NotNull                           // <-- API/JPA validation
    @Column(name = "end_date", nullable = false) // <-- DB column NOT NULL via Hibernate DDL
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate; // nullable == open-ended

    public InsurancePolicy() {}
    public InsurancePolicy(Car car, String provider, LocalDate startDate, LocalDate endDate) {
        this.car = car; this.provider = provider; this.startDate = startDate; this.endDate = endDate;
    }

    @AssertTrue(message = "endDate must be on or after startDate")
    private boolean isDateRangeValid() {
        return startDate != null && endDate != null && !endDate.isBefore(startDate);
    }

    public Long getId() { return id; }
    public Car getCar() { return car; }
    public void setCar(Car car) { this.car = car; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}
