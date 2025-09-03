package com.example.carins.service;

import com.example.carins.model.Car;
import com.example.carins.repo.CarRepository;
import com.example.carins.repo.InsurancePolicyRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class CarService {

    private final CarRepository carRepository;
    private final InsurancePolicyRepository policyRepository;

    public CarService(CarRepository carRepository, InsurancePolicyRepository policyRepository) {
        this.carRepository = carRepository;
        this.policyRepository = policyRepository;
    }

    public List<Car> listCars() {
        return carRepository.findAll();
    }

    public boolean isInsuranceValid(Long carId, LocalDate date) {
        if (carId == null || date == null) return false;
        // TODO: optionally throw NotFound if car does not exist
        return policyRepository.existsActiveOnDate(carId, date);
    }
}
