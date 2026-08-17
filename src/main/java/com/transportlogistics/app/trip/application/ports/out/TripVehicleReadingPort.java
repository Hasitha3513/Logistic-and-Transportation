package com.transportlogistics.app.trip.application.ports.out;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface TripVehicleReadingPort {
    void recordTripStart(UUID vehicleId, UUID tripId, Double odometerKm, OffsetDateTime occurredAt, UUID actorId);
    void recordTripEnd(UUID vehicleId, UUID tripId, Double odometerKm, OffsetDateTime occurredAt, UUID actorId);
}