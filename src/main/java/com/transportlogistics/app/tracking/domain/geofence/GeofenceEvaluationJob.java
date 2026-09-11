package com.transportlogistics.app.tracking.domain.geofence;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record GeofenceEvaluationJob(UUID tenantId, UUID positionId, Status status,
                                    int attempt, String leaseOwner, Instant leaseUntil,
                                    Instant createdAt, Instant nextAttemptAt) {
    public GeofenceEvaluationJob {
        Objects.requireNonNull(tenantId, "Tenant ID is required");
        Objects.requireNonNull(positionId, "Position ID is required");
        Objects.requireNonNull(status, "Job status is required");
        Objects.requireNonNull(createdAt, "Created time is required");
        Objects.requireNonNull(nextAttemptAt, "Next attempt time is required");
        if (attempt < 0 || (leaseOwner == null) != (leaseUntil == null)) {
            throw new IllegalArgumentException("Evaluation job attempt or lease is invalid");
        }
    }

    public enum Status {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }
}
