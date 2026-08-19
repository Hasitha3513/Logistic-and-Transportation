package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LubricantLogRequest(
        @NotBlank(message = "Fluid type is required")
        String fluidType,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.01", message = "Quantity must be greater than zero")
        BigDecimal quantity,

        @NotBlank(message = "Measurement unit is required")
        String unit,

        @NotNull(message = "Recorded date/time is required")
        OffsetDateTime recordedAt,

        @DecimalMin(value = "0.0", message = "Odometer reading cannot be negative")
        Double odometerKm,

        @DecimalMin(value = "0.0", message = "Engine hours cannot be negative")
        Double engineHours,

        UUID vendorId,
        String supplierName,
        String referenceNumber,
        String remarks
) {}
