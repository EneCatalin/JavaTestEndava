package com.example.carins.web;

import com.example.carins.model.Car;
import com.example.carins.service.CarService;
import com.example.carins.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CarController {

    private final CarService service;

    public CarController(CarService service) {
        this.service = service;
    }

    @GetMapping("/cars")
    public List<CarDto> getCars() {
        return service.listCars().stream().map(this::toDto).toList();
    }

    @GetMapping("/cars/{carId}/insurance-valid")
    public ResponseEntity<?> isInsuranceValid(@PathVariable Long carId, @RequestParam String date) {
        // TODO: validate date format and handle errors consistently
        LocalDate d = LocalDate.parse(date);
        boolean valid = service.isInsuranceValid(carId, d);
        return ResponseEntity.ok(new InsuranceValidityResponse(carId, d.toString(), valid));
    }

    private CarDto toDto(Car c) {
        var o = c.getOwner();
        return new CarDto(c.getId(), c.getVin(), c.getMake(), c.getModel(), c.getYearOfManufacture(),
                o != null ? o.getId() : null,
                o != null ? o.getName() : null,
                o != null ? o.getEmail() : null);
    }

    public record InsuranceValidityResponse(Long carId, String date, boolean valid) {}

    //My code

    @PostMapping("/cars/{carId}/policies")
    public ResponseEntity<PolicyResponse> createPolicy(@PathVariable Long carId,
                                                       @Valid @RequestBody PolicyUpsertRequest req) {
        PolicyResponse saved = service.createPolicy(carId, req.startDate(), req.endDate(), req.provider());
        return ResponseEntity.created(URI.create("/api/policies/" + saved.id())).body(saved);
    }

    @PutMapping("/policies/{policyId}")
    public PolicyResponse updatePolicy(@PathVariable Long policyId,
                                       @Valid @RequestBody PolicyUpsertRequest req) {
        return service.updatePolicy(policyId, req.startDate(), req.endDate(), req.provider());
    }

    @PostMapping("/cars/{carId}/claims")
    public ResponseEntity<ClaimDto> createClaim(
            @PathVariable Long carId,
            @Valid @RequestBody CreateClaimRequest body,
            UriComponentsBuilder uri) {

        ClaimDto dto = service.registerClaim(carId, body);

        java.net.URI location = uri
                .path("/api/cars/{carId}/claims/{claimId}")
                .buildAndExpand(carId, dto.id())
                .toUri();

        return ResponseEntity.created(location).body(dto);
    }
}
