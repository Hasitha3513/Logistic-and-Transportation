package com.transportlogistics.app.fleet;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Public fleet contract used by trip allocation and dispatch.
 */
public interface VehicleDispatchEligibility {
    void assertEligible(UUID vehicleId, OffsetDateTime from, OffsetDateTime to, UUID requiredVehicleTypeId,
                        Double requiredCapacityKg, UUID excludeTripId);
}
