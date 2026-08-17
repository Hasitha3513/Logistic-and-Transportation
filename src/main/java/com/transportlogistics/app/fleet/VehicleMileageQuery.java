package com.transportlogistics.app.fleet;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Public Fleet module query boundary for vehicle mileage, engine hours, and trip distances.
 * Used by external modules (such as future US-34 Fuel Cost Per Trip and Reporting) without direct
 * access to Fleet persistence entities.
 */
public interface VehicleMileageQuery {

    /**
     * Calculates the derived mileage and engine hours summary for a vehicle over a period.
     */
    VehicleMileageSummary getVehicleMileageSummary(UUID vehicleId, OffsetDateTime from, OffsetDateTime to, boolean includeSourceBreakdown);

    /**
     * Calculates the authoritative distance traveled during a trip from the VehicleReading ledger.
     */
    TripDistanceSummary getTripDistance(UUID tripId, UUID vehicleId);

    /**
     * Queries the latest effective readings for a vehicle.
     */
    LatestReadings getLatestReadings(UUID vehicleId);

    record LatestReadings(UUID vehicleId, ReadingSnapshot odometer, ReadingSnapshot engineHours) {
    }

    record ReadingSnapshot(UUID readingId, java.math.BigDecimal value, String unit, int meterEpoch,
                           String sourceType, OffsetDateTime recordedAt) {
    }
}
