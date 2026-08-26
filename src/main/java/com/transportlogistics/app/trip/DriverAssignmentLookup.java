package com.transportlogistics.app.trip;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Minimal trip-owned assignment view published to other modules.
 */
public interface DriverAssignmentLookup {
    boolean hasOverlap(UUID driverId, OffsetDateTime from, OffsetDateTime to, UUID excludeTripId);
}
