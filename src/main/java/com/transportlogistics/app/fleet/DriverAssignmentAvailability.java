package com.transportlogistics.app.fleet;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Fleet-side assignment lookup port implemented by cross-module composition. */
public interface DriverAssignmentAvailability {
    boolean hasOverlap(UUID driverId, OffsetDateTime from, OffsetDateTime to, UUID excludeTripId);
}
