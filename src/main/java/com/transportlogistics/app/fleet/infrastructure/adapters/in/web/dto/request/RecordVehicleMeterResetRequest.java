package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request;

import com.transportlogistics.app.fleet.domain.model.VehicleReadingType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record RecordVehicleMeterResetRequest(
        @NotNull(message = "Reading type is required")
        VehicleReadingType readingType,

        @NotNull(message = "New meter value is required")
        @DecimalMin(value = "0.000", message = "New meter value cannot be negative")
        BigDecimal newMeterValue,

        @NotNull(message = "Effective time is required")
        OffsetDateTime effectiveAt,

        @NotBlank(message = "Meter reset reason is required")
        @Size(max = 1000, message = "Reason cannot exceed 1000 characters")
        String reason
) {
}
