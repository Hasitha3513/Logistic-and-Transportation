package com.transportlogistics.app.freight.insurance.adapters.inbound.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AssessClaimRequest(
        @NotNull(message = "Assessed amount is required")
        @DecimalMin(value = "0.00", message = "Assessed amount must be non-negative")
        BigDecimal assessedAmount,

        @Size(max = 2000)
        String assessmentNotes,

        @NotNull(message = "Version is required for optimistic concurrency")
        Long version
) {}
