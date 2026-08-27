package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request;

import com.transportlogistics.app.fleet.domain.model.VehicleReadingType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record RecordManualVehicleReadingRequest(
        @NotNull(message = "Reading type is required")
        VehicleReadingType readingType,

        @NotNull(message = "Reading value is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Reading value cannot be negative")
        BigDecimal value,

        @NotNull(message = "Recorded time is required")
        OffsetDateTime recordedAt,

        @Size(max = 255, message = "Idempotency key cannot exceed 255 characters")
        String idempotencyKey,

        @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
        String notes
) {
}
