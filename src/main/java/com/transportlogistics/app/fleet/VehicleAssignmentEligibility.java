package com.transportlogistics.app.fleet;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Public fleet contract for intrinsic vehicle eligibility; trip owns allocation conflicts. */
public interface VehicleAssignmentEligibility {
    void assertEligible(UUID vehicleId, OffsetDateTime from, OffsetDateTime to, UUID requiredVehicleTypeId,
                        Double requiredCapacityKg);
}
