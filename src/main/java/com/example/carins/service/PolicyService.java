package com.example.carins.service;

import com.example.carins.model.InsurancePolicy;
import com.example.carins.repo.InsurancePolicyRepository;
import com.example.carins.web.dto.PolicyResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PolicyService {

    private final InsurancePolicyRepository policyRepo;

    public PolicyService(InsurancePolicyRepository policyRepo) {
        this.policyRepo = policyRepo;
    }

    public List<PolicyResponse> listAll() {
        return policyRepo.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private PolicyResponse toResponse(InsurancePolicy p) {
        return new PolicyResponse(
                p.getId(),
                (p.getCar() != null ? p.getCar().getId() : null),
                p.getProvider(),
                p.getStartDate(),
                p.getEndDate()
        );
    }
}