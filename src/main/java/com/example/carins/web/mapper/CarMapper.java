package com.example.carins.web.mapper;


import com.example.carins.model.Car;
import com.example.carins.model.Owner;
import com.example.carins.web.dto.CarDto;
import org.springframework.stereotype.Component;

@Component
public class CarMapper {

    public CarDto toDto(Car c) {
        // use explicit type to avoid 'var'
        Owner o = c.getOwner();
        Long ownerId = (o != null) ? o.getId() : null;
        String ownerName = (o != null) ? o.getName() : null;
        String ownerEmail = (o != null) ? o.getEmail() : null;

        return new CarDto(
                c.getId(),
                c.getVin(),
                c.getMake(),
                c.getModel(),
                c.getYearOfManufacture(),
                ownerId,
                ownerName,
                ownerEmail
        );
    }
}