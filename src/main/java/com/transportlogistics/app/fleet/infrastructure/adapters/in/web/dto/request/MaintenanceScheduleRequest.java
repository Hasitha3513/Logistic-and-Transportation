package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MaintenanceScheduleRequest(
        @NotBlank(message = "Maintenance type is required")
        String maintenanceType,

        @NotNull(message = "Scheduled start is required")
        OffsetDateTime scheduledStart,

        @NotNull(message = "Scheduled end is required")
        OffsetDateTime scheduledEnd,

        String description,
        String serviceProvider,
        BigDecimal cost
) {
}
