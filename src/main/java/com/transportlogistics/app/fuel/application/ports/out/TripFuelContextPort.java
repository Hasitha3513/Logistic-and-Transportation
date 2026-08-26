package com.transportlogistics.app.fuel.application.ports.out;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface TripFuelContextPort {
    Optional<TripContext> find(UUID tripId);

    record TripContext(UUID id, String tripNumber, String status, UUID vehicleId, UUID driverId,
                       OffsetDateTime requestedStartTime, OffsetDateTime requestedEndTime) {
    }
}
