package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record RecordVehicleReadingCorrectionRequest(
        @NotNull(message = "Corrected value is required")
        @DecimalMin(value = "0.000", message = "Corrected value cannot be negative")
        BigDecimal value,

        @NotBlank(message = "Correction reason is required")
        @Size(max = 1000, message = "Reason cannot exceed 1000 characters")
        String reason,

        OffsetDateTime recordedAt
) {
}
