package com.transportlogistics.app.trip.application.ports.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface TripDistancePort {
    DistanceResult getTripDistance(UUID tripId, UUID vehicleId);

    record DistanceResult(
            UUID tripId,
            UUID vehicleId,
            BigDecimal startOdometer,
            BigDecimal endOdometer,
            BigDecimal distanceKm,
            String status,
            boolean meterResetEncountered,
            String notes
    ) {
    }
}
