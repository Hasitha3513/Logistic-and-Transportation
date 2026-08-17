package com.transportlogistics.app.trip.application.ports.out;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Output port for recording authoritative operational vehicle readings for Trip lifecycle events.
 */
public interface TripVehicleReadingPort {
    void recordStart(UUID vehicleId, UUID tripId, Double odometerKm, Double engineHours,
                     OffsetDateTime actualStartTime, UUID actorId);

    void recordComplete(UUID vehicleId, UUID tripId, Double odometerKm, Double engineHours,
                        OffsetDateTime actualEndTime, UUID actorId);
}
