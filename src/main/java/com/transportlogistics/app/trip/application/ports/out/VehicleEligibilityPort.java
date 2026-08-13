package com.transportlogistics.app.trip.application.ports.out;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface VehicleEligibilityPort {
    void assertEligible(UUID vehicleId, OffsetDateTime from, OffsetDateTime to, UUID requiredVehicleTypeId,
                        Double requiredCapacityKg, UUID excludeTripId);
}
