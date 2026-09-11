package com.transportlogistics.app.tracking.domain.speed;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SpeedPosition(UUID tenantId, UUID vehicleId, UUID positionId, Instant sourceTimestamp,
                            SpeedKph speedKph, boolean duplicate, boolean trusted,
                            boolean vehicleAssociated, boolean inOrder) {
    private static final Duration MAXIMUM_AGE = Duration.ofMinutes(5);

    public SpeedPosition {
        Objects.requireNonNull(tenantId, "Tenant ID is required");
        Objects.requireNonNull(vehicleId, "Vehicle ID is required");
        Objects.requireNonNull(positionId, "Position ID is required");
        Objects.requireNonNull(sourceTimestamp, "Source timestamp is required");
    }

    public Eligibility eligibilityAt(Instant evaluatedAt) {
        Objects.requireNonNull(evaluatedAt, "Evaluation time is required");
        if (duplicate) return Eligibility.DUPLICATE;
        if (!trusted) return Eligibility.UNTRUSTED;
        if (!vehicleAssociated) return Eligibility.UNASSOCIATED;
        if (!inOrder) return Eligibility.OUT_OF_ORDER;
        if (speedKph == null) return Eligibility.SPEED_UNKNOWN;
        Duration age = Duration.between(sourceTimestamp, evaluatedAt);
        if (age.isNegative()) return Eligibility.FUTURE;
        if (age.compareTo(MAXIMUM_AGE) > 0) return Eligibility.STALE;
        return Eligibility.ELIGIBLE;
    }

    public enum Eligibility {
        ELIGIBLE, DUPLICATE, UNTRUSTED, UNASSOCIATED, OUT_OF_ORDER, SPEED_UNKNOWN, FUTURE, STALE
    }
}
