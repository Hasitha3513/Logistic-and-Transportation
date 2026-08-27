package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CorrectVehicleReadingRequest(
        @NotNull(message = "Corrected reading value is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Reading value cannot be negative")
        BigDecimal correctedValue,

        OffsetDateTime recordedAt,

        @NotBlank(message = "Correction reason is required")
        @Size(max = 1000, message = "Correction reason cannot exceed 1000 characters")
        String reason,

        @Size(max = 255, message = "Idempotency key cannot exceed 255 characters")
        String idempotencyKey
) {
}
