package com.transportlogistics.app.fleet.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DriverException(
        UUID id,
        UUID driverId,
        DriverExceptionType exceptionType,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        DriverExceptionStatus status,
        String reason,
        String remarks,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String createdBy,
        String updatedBy
) {
    public DriverException {
        if (id == null) {
            throw new IllegalArgumentException("Driver exception ID cannot be null");
        }
        if (driverId == null) {
            throw new IllegalArgumentException("Driver ID cannot be null");
        }
        if (exceptionType == null) {
            throw new IllegalArgumentException("Driver exception type cannot be null");
        }
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start time and end time cannot be null");
        }
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("End time must be strictly after start time");
        }
        if (status == null) {
            throw new IllegalArgumentException("Driver exception status cannot be null");
        }
    }

    public boolean hasOverlap(OffsetDateTime from, OffsetDateTime to) {
        if (from == null || to == null || !from.isBefore(to)) {
            throw new IllegalArgumentException("Query interval must have from before to");
        }
        // Half-open interval semantics: [startTime, endTime) overlaps with [from, to)
        return this.startTime.isBefore(to) && this.endTime.isAfter(from);
    }
}
