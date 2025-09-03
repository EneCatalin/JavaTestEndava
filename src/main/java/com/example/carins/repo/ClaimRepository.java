package com.example.carins.repo;

import com.example.carins.model.Claim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
    List<Claim> findByCarIdOrderByClaimDateAsc(Long carId);
}