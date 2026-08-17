package com.transportlogistics.app.fleet;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Fleet-side allocation lookup port implemented by cross-module composition.
 */
public interface VehicleAllocationAvailability {
    boolean hasOverlap(UUID vehicleId, OffsetDateTime from, OffsetDateTime to, UUID excludeTripId);
}
