package com.transportlogistics.app.fleet;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface VehicleMileageQuery {
    VehicleMileageSummary getMileage(UUID vehicleId, OffsetDateTime from, OffsetDateTime to);
    TripDistanceSummary calculateTripDistance(UUID tripId);
}