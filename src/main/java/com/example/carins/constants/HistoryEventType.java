package com.example.carins.constants;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum HistoryEventType {
    @JsonProperty("policyStarted")
    POLICY_STARTED,

    @JsonProperty("policyEnded")
    POLICY_ENDED,

    @JsonProperty("claimRegistered")
    CLAIM_REGISTERED
}