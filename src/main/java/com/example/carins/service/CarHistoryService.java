package com.example.carins.service;

import com.example.carins.constants.HistoryEventType;
import com.example.carins.model.Claim;
import com.example.carins.model.InsurancePolicy;
import com.example.carins.repo.CarRepository;
import com.example.carins.repo.ClaimRepository;
import com.example.carins.repo.InsurancePolicyRepository;
import com.example.carins.web.dto.HistoryEventDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CarHistoryService {

    private static final Map<HistoryEventType, Integer> SAME_DAY_ORDER;
    static {
        Map<HistoryEventType, Integer> order = new HashMap<>();
        order.put(HistoryEventType.POLICY_ENDED, 0);
        order.put(HistoryEventType.POLICY_STARTED, 1);
        order.put(HistoryEventType.CLAIM_REGISTERED, 2);
        SAME_DAY_ORDER = Map.copyOf(order);
    }

    private final CarRepository carRepository;
    private final InsurancePolicyRepository policyRepository;
    private final ClaimRepository claimRepository;

    public CarHistoryService(CarRepository carRepository,
                             InsurancePolicyRepository policyRepository,
                             ClaimRepository claimRepository) {
        this.carRepository = carRepository;
        this.policyRepository = policyRepository;
        this.claimRepository = claimRepository;
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
}