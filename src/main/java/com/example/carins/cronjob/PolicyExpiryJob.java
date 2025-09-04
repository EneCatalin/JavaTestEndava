package com.example.carins.cronjob;

import com.example.carins.model.InsurancePolicy;
import com.example.carins.repo.InsurancePolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class PolicyExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(PolicyExpiryJob.class);
    private final InsurancePolicyRepository policyRepository;

    public PolicyExpiryJob(InsurancePolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }


    //saves every hour
    @Scheduled(fixedRate = 60 * 60 * 1000) // every hour
    public void checkExpiredPolicies() {
        LocalDate today = LocalDate.now();
        List<InsurancePolicy> expired = policyRepository
                .findByEndDateBeforeAndExpiryLoggedFalse(today.plusDays(1)); // midnight inclusive

        for (InsurancePolicy policy : expired) {
            log.info("Policy {} for car {} expired on {}",
                    policy.getId(),
                    policy.getCar().getId(),
                    policy.getEndDate());
            policy.setExpiryLogged(true);
        }

        if (!expired.isEmpty()) {
            policyRepository.saveAll(expired);
        }
    }
}