package com.transportlogistics.app.freight.insurance.adapters.inbound.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateClaimRequest(
        @NotNull(message = "Policy ID is required")
        UUID policyId,

        @Size(max = 120)
        String incidentReference,

        @NotBlank(message = "Damage description is required")
        @Size(max = 2000)
        String damageDescription,

        @NotNull(message = "Claimed amount is required")
        @DecimalMin(value = "0.01", message = "Claimed amount must be greater than zero")
        BigDecimal claimedAmount
) {}
