package com.transportlogistics.app.freight.insurance.adapters.inbound.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AssociatePolicyRequest(
        @NotNull(message = "Freight order ID is required")
        UUID freightOrderId,

        UUID cargoManifestId,

        @NotBlank(message = "Insurance provider is required")
        @Size(max = 200)
        String insuranceProvider,

        @NotBlank(message = "Policy type is required")
        @Size(max = 60)
        String policyType,

        @NotNull(message = "Coverage amount is required")
        @DecimalMin(value = "0.01", message = "Coverage amount must be greater than zero")
        BigDecimal coverageAmount,

        @NotNull(message = "Premium amount is required")
        @DecimalMin(value = "0.00", message = "Premium amount must be non-negative")
        BigDecimal premiumAmount,

        @NotBlank(message = "Currency is required")
        @Size(max = 10)
        String currency,

        @NotNull(message = "Valid from timestamp is required")
        OffsetDateTime validFrom,

        @NotNull(message = "Valid until timestamp is required")
        OffsetDateTime validUntil
) {}
