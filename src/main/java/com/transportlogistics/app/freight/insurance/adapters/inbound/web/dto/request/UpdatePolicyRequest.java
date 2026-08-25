package com.transportlogistics.app.freight.insurance.adapters.inbound.web.dto.request;

import com.transportlogistics.app.freight.insurance.domain.PolicyStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record UpdatePolicyRequest(
        @Size(max = 200)
        String insuranceProvider,

        @Size(max = 60)
        String policyType,

        @DecimalMin(value = "0.01", message = "Coverage amount must be greater than zero")
        BigDecimal coverageAmount,

        @DecimalMin(value = "0.00", message = "Premium amount must be non-negative")
        BigDecimal premiumAmount,

        OffsetDateTime validFrom,

        OffsetDateTime validUntil,

        PolicyStatus status,

        @NotNull(message = "Version is required for optimistic concurrency")
        Long version
) {}
