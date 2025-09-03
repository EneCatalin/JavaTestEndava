package com.example.carins.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "car")
public class Car {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Size(min = 5, max = 32)
    private String vin;

    private String make;
    private String model;
    private int yearOfManufacture;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Owner owner;

    public Car() {}
    public Car(String vin, String make, String model, int yearOfManufacture, Owner owner) {
        this.vin = vin; this.make = make; this.model = model; this.yearOfManufacture = yearOfManufacture; this.owner = owner;
    }

    public Long getId() { return id; }
    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }
    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getYearOfManufacture() { return yearOfManufacture; }
    public void setYearOfManufacture(int y) { this.yearOfManufacture = y; }
    public Owner getOwner() { return owner; }
    public void setOwner(Owner owner) { this.owner = owner; }
}
