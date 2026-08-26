package com.transportlogistics.app.fleet.application.ports.in;

import com.transportlogistics.app.fleet.domain.model.VehicleAvailability;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface VehicleAvailabilityUseCase {
    VehicleAvailability evaluate(Query query);

    record Query(UUID vehicleId, OffsetDateTime from, OffsetDateTime to, UUID requiredVehicleTypeId,
                 Double requiredCapacityKg, UUID excludeTripId, boolean checkAllocationConflicts,
                 boolean lockVehicle) {
        public Query(UUID vehicleId, OffsetDateTime from, OffsetDateTime to, UUID requiredVehicleTypeId,
                     Double requiredCapacityKg, UUID excludeTripId) {
            this(vehicleId, from, to, requiredVehicleTypeId, requiredCapacityKg, excludeTripId, true, false);
        }
    }
}
