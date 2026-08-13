package com.transportlogistics.app.trip.application.ports.out;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface VehicleEligibilityPort {
    void assertEligibleForAssignment(UUID vehicleId, OffsetDateTime from, OffsetDateTime to,
                                     UUID requiredVehicleTypeId, Double requiredCapacityKg);

    void assertEligibleForDispatch(UUID vehicleId, OffsetDateTime from, OffsetDateTime to,
                                   UUID requiredVehicleTypeId, Double requiredCapacityKg, UUID excludeTripId);
}
