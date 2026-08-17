package com.transportlogistics.app.fleet;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Derived, read-side domain summary of the authoritative distance traveled during a Trip,
 * calculated directly from the Fleet-owned VehicleReading ledger.
 */
public record TripDistanceSummary(
        UUID tripId,
        UUID vehicleId,
        BigDecimal startOdometer,
        BigDecimal endOdometer,
        BigDecimal distanceKm,
        TripDistanceStatus status,
        boolean meterResetEncountered,
        String notes
) {
    public TripDistanceSummary {
        Objects.requireNonNull(tripId, "tripId must not be null");
        Objects.requireNonNull(vehicleId, "vehicleId must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }
}
