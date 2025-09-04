package com.example.carins.service;

import com.example.carins.constants.HistoryEventType;
import com.example.carins.exception.InvalidDateException;
import com.example.carins.exception.ResourceNotFoundException;
import com.example.carins.exception.policy.PolicyEndDateException;
import com.example.carins.model.Car;
import com.example.carins.model.Claim;
import com.example.carins.model.InsurancePolicy;
import com.example.carins.repo.CarRepository;
import com.example.carins.repo.ClaimRepository;
import com.example.carins.repo.InsurancePolicyRepository;
import com.example.carins.web.dto.ClaimDto;
import com.example.carins.web.dto.CreateClaimRequest;
import com.example.carins.web.dto.HistoryEventDto;
import com.example.carins.web.dto.PolicyResponse;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class CarService {

    private final CarRepository carRepository;
    private final InsurancePolicyRepository policyRepository;
    private final ClaimRepository claimRepository;

    public CarService(CarRepository carRepository, InsurancePolicyRepository policyRepository, ClaimRepository claimRepository) {
        this.carRepository = carRepository;
        this.policyRepository = policyRepository;
        this.claimRepository = claimRepository;

    }

    public List<Car> listCars() {
        return carRepository.findAll();
    }

    public boolean isInsuranceValid(Long carId, LocalDate date) {
        int y = date.getYear();
        if (y < 1900 || y > 2100) {
            throw new InvalidDateException("Date out of supported range (1900–2100): " + date);
        }
        if (!carRepository.existsById(carId)) {
            throw new ResourceNotFoundException("Car " + carId + " not found");
        }
        return policyRepository.existsActiveOnDate(carId, date);
    }

    public LocalDate parseDate(String input) {
        try {
            return LocalDate.parse(input);
        } catch (DateTimeParseException ex) {
            throw new InvalidDateException("Invalid date format, expected YYYY-MM-DD: " + input, ex);
        }
    }

    public PolicyResponse createPolicy(Long carId, LocalDate startDate, LocalDate endDate, String provider) {
        validateDates(startDate, endDate);

        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new ResourceNotFoundException("Car not found"));

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

        InsurancePolicy existing = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found"));

        existing.setProvider(provider);
        existing.setStartDate(startDate);
        existing.setEndDate(endDate);

        InsurancePolicy saved = policyRepository.save(existing);
        return toPolicyResponse(saved);
    }

    @Transactional
    public ClaimDto registerClaim(Long carId, CreateClaimRequest req) {
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new ResourceNotFoundException("Car not found"));

        Claim claim = new Claim(car, req.claimDate(), req.description(), req.amount());
        Claim saved = claimRepository.save(claim);

        return new ClaimDto(
                saved.getId(),
                saved.getCar().getId(),
                saved.getClaimDate(),
                saved.getDescription(),
                saved.getAmount()
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

    // ------ Car history service merge ------

    private static final Map<HistoryEventType, Integer> SAME_DAY_ORDER;
    static {
        Map<HistoryEventType, Integer> order = new HashMap<>();
        order.put(HistoryEventType.POLICY_ENDED, 0);
        order.put(HistoryEventType.POLICY_STARTED, 1);
        order.put(HistoryEventType.CLAIM_REGISTERED, 2);
        SAME_DAY_ORDER = Map.copyOf(order);
    }

    public List<HistoryEventDto> getHistory(Long carId) {
        if (!carRepository.existsById(carId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Car " + carId + " not found");
        }

        List<HistoryEventDto> events = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ISO_DATE;

        // Policies → start/end (refId = policy id)
        List<InsurancePolicy> policies = policyRepository.findByCarId(carId);
        for (InsurancePolicy p : policies) {
            events.add(new HistoryEventDto(
                    p.getStartDate(),
                    HistoryEventType.POLICY_STARTED,
                    "Policy #" + p.getId() + " (" + safe(p.getProvider()) + ") started on " + fmt.format(p.getStartDate()),
                    p.getId()
            ));
            if (p.getEndDate() != null) {
                events.add(new HistoryEventDto(
                        p.getEndDate(),
                        HistoryEventType.POLICY_ENDED,
                        "Policy #" + p.getId() + " (" + safe(p.getProvider()) + ") ended on " + fmt.format(p.getEndDate()),
                        p.getId()
                ));
            }
        }

        // Claims (refId = claim id)
        List<Claim> claims = claimRepository.findByCarIdOrderByClaimDateAsc(carId);
        for (Claim c : claims) {
            events.add(new HistoryEventDto(
                    c.getClaimDate(),
                    HistoryEventType.CLAIM_REGISTERED,
                    "Claim #" + c.getId() + " on " + fmt.format(c.getClaimDate()) +
                            " for amount " + c.getAmount() + " — " + c.getDescription(),
                    c.getId()
            ));
        }

        // Chronological order, then domain order for same-day ties, then a stable tiebreaker
        events.sort(Comparator
                .comparing(HistoryEventDto::date)
                .thenComparing(e -> SAME_DAY_ORDER.getOrDefault(e.type(), Integer.MAX_VALUE))
                .thenComparing(HistoryEventDto::type)   // stable fallback
                .thenComparing(e -> e.refId() == null ? Long.MAX_VALUE : e.refId())); // final stability

        return events;
    }

    private static String safe(String s) {
        return s == null ? "Unknown" : s;
    }
    // ------ Validation helpers ------

    private static void validateDates(LocalDate start, LocalDate end) {
        if (start == null) throw new PolicyEndDateException("startDate is required");
        if (end == null) throw new PolicyEndDateException("endDate is required");
        if (end.isBefore(start)) throw new PolicyEndDateException("endDate must be on or after startDate");
    }
}