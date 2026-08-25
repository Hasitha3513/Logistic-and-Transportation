package com.transportlogistics.app.freight.insurance.adapters.inbound.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RecordSettlementRequest(
        @NotNull(message = "Settlement amount is required")
        @DecimalMin(value = "0.01", message = "Settlement amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        @Size(max = 10)
        String currency,

        @Size(max = 120)
        String settlementReference,

        @Size(max = 1000)
        String notes,

        @NotNull(message = "Version is required for optimistic concurrency")
        Long version
) {}
