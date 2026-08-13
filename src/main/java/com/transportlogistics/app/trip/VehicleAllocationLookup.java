package com.transportlogistics.app.trip;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Minimal trip-owned allocation view published to other modules. */
public interface VehicleAllocationLookup {
    boolean hasOverlap(UUID vehicleId, OffsetDateTime from, OffsetDateTime to, UUID excludeTripId);
}
