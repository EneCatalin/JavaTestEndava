package com.example.carins.web;

import com.example.carins.service.CarService;
import com.example.carins.service.PolicyService;
import com.example.carins.web.dto.PolicyResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PolicyController {

    private final PolicyService service;

    public PolicyController(PolicyService service) {
        this.service = service;
    }

    @GetMapping("/policies")
    public List<PolicyResponse> getAllPolicies() {
        return service.listAll();
    }
}