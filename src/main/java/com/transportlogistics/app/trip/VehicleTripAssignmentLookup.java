package com.transportlogistics.app.trip;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Published source-time Trip attribution contract for Tracking consumers. */
public interface VehicleTripAssignmentLookup {
    Optional<VehicleTripAssignment> findAt(UUID tenantId, UUID vehicleId, Instant sourceTimestamp);

    record VehicleTripAssignment(UUID tripId, UUID driverId, UUID routeId, String routeVersion) {
    }
}
