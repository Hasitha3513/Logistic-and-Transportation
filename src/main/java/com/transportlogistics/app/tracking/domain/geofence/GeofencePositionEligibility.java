package com.transportlogistics.app.tracking.domain.geofence;

import com.transportlogistics.app.tracking.domain.TrackingModels.Ordering;
import com.transportlogistics.app.tracking.domain.TrackingModels.Trust;
import java.time.Duration;
import java.time.Instant;

public final class GeofencePositionEligibility {
    private static final Duration MAXIMUM_AGE = Duration.ofMinutes(5);

    private GeofencePositionEligibility() {
    }

    public static boolean isEligible(GeofencePosition position, Instant evaluatedAt) {
        if (position == null || evaluatedAt == null || position.tenantId() == null
                || position.positionId() == null || position.vehicleId() == null
                || position.sourceTimestamp() == null || position.coordinate() == null
                || position.duplicate() || position.trust() != Trust.TRUSTED
                || position.ordering() != Ordering.IN_ORDER) {
            return false;
        }
        Duration age = Duration.between(position.sourceTimestamp(), evaluatedAt);
        return !age.isNegative() && age.compareTo(MAXIMUM_AGE) <= 0;
    }
}
