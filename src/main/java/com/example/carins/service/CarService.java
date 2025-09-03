package com.example.carins.service;

import com.example.carins.exception.policy.PolicyEndDateException;
import com.example.carins.model.Car;
import com.example.carins.model.InsurancePolicy;
import com.example.carins.repo.CarRepository;
import com.example.carins.repo.InsurancePolicyRepository;
import com.example.carins.web.dto.CarDto;
import com.example.carins.web.dto.PolicyResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

    //My code

    public PolicyResponse createPolicy(Long carId, LocalDate startDate, LocalDate endDate, String provider) {
        validateDates(startDate, endDate);

        Car car = carRepository.findById(carId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "carId not found"));

        InsurancePolicy p = new InsurancePolicy();
        p.setCar(car);
        p.setProvider(provider);
        p.setStartDate(startDate);
        p.setEndDate(endDate);

        InsurancePolicy saved = policyRepository.save(p);
        return toPolicyResponse(saved);
    }

    public PolicyResponse updatePolicy(Long policyId, LocalDate startDate, LocalDate endDate, String provider) {
        validateDates(startDate, endDate);

        InsurancePolicy existing = policyRepository.findById(policyId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "policyId not found"));

        existing.setProvider(provider);
        existing.setStartDate(startDate);
        existing.setEndDate(endDate);

        InsurancePolicy saved = policyRepository.save(existing);
        return toPolicyResponse(saved);
    }


    private CarDto toDto(Car c) {
        var o = c.getOwner();
        return new CarDto(
                c.getId(),
                c.getVin(),
                c.getMake(),
                c.getModel(),
                c.getYearOfManufacture(),
                (o != null ? o.getId() : null),
                (o != null ? o.getName() : null),
                (o != null ? o.getEmail() : null)
        );
    }

    private PolicyResponse toPolicyResponse(InsurancePolicy p) {
        return new PolicyResponse(
                p.getId(),
                (p.getCar() != null ? p.getCar().getId() : null),
                p.getProvider(),
                p.getStartDate(),
                p.getEndDate()
        );
    }

    // ------ Validation helpers ------

    private static void validateDates(LocalDate start, LocalDate end) {
        if (start == null) throw new PolicyEndDateException("startDate is required");
        if (end == null) throw new PolicyEndDateException("endDate is required");
        if (end.isBefore(start)) {
            throw new PolicyEndDateException("endDate must be on or after startDate");
        }
    }
}