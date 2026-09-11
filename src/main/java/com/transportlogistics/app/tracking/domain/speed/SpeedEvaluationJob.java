package com.transportlogistics.app.tracking.domain.speed;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SpeedEvaluationJob(UUID tenantId, UUID positionId, UUID vehicleId,
                                 Instant sourceTimestamp, Status status, int attemptCount,
                                 Instant nextAttemptAt, String leaseOwner, Instant leaseUntil,
                                 String lastErrorCode, Instant createdAt) {
    public enum Status { PENDING, PROCESSING, COMPLETED, FAILED }

    public SpeedEvaluationJob {
        Objects.requireNonNull(tenantId, "Tenant ID is required");
        Objects.requireNonNull(positionId, "Position ID is required");
        Objects.requireNonNull(vehicleId, "Vehicle ID is required");
        Objects.requireNonNull(sourceTimestamp, "Source timestamp is required");
        Objects.requireNonNull(status, "Status is required");
        Objects.requireNonNull(nextAttemptAt, "Next attempt time is required");
        Objects.requireNonNull(createdAt, "Created time is required");
        if (attemptCount < 0 || (leaseOwner == null) != (leaseUntil == null)) {
            throw new IllegalArgumentException("Attempt count or lease is invalid");
        }
    }
}
