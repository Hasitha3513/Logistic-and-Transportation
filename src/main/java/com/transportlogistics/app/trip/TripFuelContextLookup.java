package com.transportlogistics.app.trip;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Minimal trip-owned operational view used by fuel operations.
 */
public interface TripFuelContextLookup {
    Optional<TripFuelContext> find(UUID tripId);

    record TripFuelContext(UUID tripId, String tripNumber, String status, UUID vehicleId, UUID driverId,
                           OffsetDateTime requestedStartTime, OffsetDateTime requestedEndTime) {
    }
}
