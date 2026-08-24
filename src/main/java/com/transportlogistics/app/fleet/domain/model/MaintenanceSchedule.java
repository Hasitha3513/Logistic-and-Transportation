package com.transportlogistics.app.fleet.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MaintenanceSchedule(
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
    public MaintenanceSchedule {
        if (id == null) {
            throw new IllegalArgumentException("Schedule ID is required");
        }
        if (vehicleId == null) {
            throw new IllegalArgumentException("Vehicle ID is required");
        }
        if (maintenanceType == null || maintenanceType.trim().isEmpty()) {
            throw new IllegalArgumentException("Maintenance type is required");
        }
        if (scheduledStart == null || scheduledEnd == null) {
            throw new IllegalArgumentException("Scheduled start and end are required");
        }
        if (!scheduledStart.isBefore(scheduledEnd)) {
            throw new IllegalArgumentException("Scheduled end must be strictly after scheduled start");
        }
        if (status == null) {
            status = MaintenanceStatus.SCHEDULED;
        }
    }

    public boolean isBlocking() {
        return status != null && status.isBlocking();
    }

    public boolean hasOverlap(OffsetDateTime from, OffsetDateTime to) {
        if (!isBlocking()) {
            return false;
        }
        return scheduledStart.isBefore(to) && scheduledEnd.isAfter(from);
    }
}
