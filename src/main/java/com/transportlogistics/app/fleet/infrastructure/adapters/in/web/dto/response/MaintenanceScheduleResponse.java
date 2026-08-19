package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.fleet.domain.model.MaintenanceStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MaintenanceScheduleResponse(
        UUID id,
        UUID vehicleId,
        String maintenanceType,
        OffsetDateTime scheduledStart,
        OffsetDateTime scheduledEnd,
        MaintenanceStatus status,
        String description,
        String serviceProvider,
        BigDecimal cost,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String createdBy,
        String updatedBy
) {
}
