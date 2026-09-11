package com.transportlogistics.app.tracking.domain.speed;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SpeedEvaluationJob(UUID tenantId, UUID positionId, Status status,
                                 Instant nextAttemptAt, int attemptCount) {
    public enum Status { QUEUED, CLAIMED, COMPLETE, FAILED }

    public SpeedEvaluationJob {
        Objects.requireNonNull(tenantId, "Tenant ID is required");
        Objects.requireNonNull(positionId, "Position ID is required");
        Objects.requireNonNull(status, "Status is required");
        Objects.requireNonNull(nextAttemptAt, "Next attempt time is required");
        if (attemptCount < 0) throw new IllegalArgumentException("Attempt count cannot be negative");
    }
}
