package com.transportlogistics.app.trip.infrastructure.adapters.out.fleet;

import com.transportlogistics.app.fleet.VehicleDispatchEligibility;
import com.transportlogistics.app.fleet.VehicleAssignmentEligibility;
import com.transportlogistics.app.trip.application.ports.out.VehicleEligibilityPort;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
class VehicleEligibilityAdapter implements VehicleEligibilityPort {
    private final VehicleAssignmentEligibility assignmentEligibility;
    private final VehicleDispatchEligibility dispatchEligibility;

    VehicleEligibilityAdapter(VehicleAssignmentEligibility assignmentEligibility,
                              VehicleDispatchEligibility dispatchEligibility) {
        this.assignmentEligibility = assignmentEligibility;
        this.dispatchEligibility = dispatchEligibility;
    }

    @Override
    public void assertEligibleForAssignment(UUID vehicleId, OffsetDateTime from, OffsetDateTime to,
                                            UUID requiredVehicleTypeId, Double requiredCapacityKg) {
        assignmentEligibility.assertEligible(vehicleId, from, to, requiredVehicleTypeId, requiredCapacityKg);
    }

    @Override
    public void assertEligibleForDispatch(UUID vehicleId, OffsetDateTime from, OffsetDateTime to,
                                          UUID requiredVehicleTypeId, Double requiredCapacityKg,
                                          UUID excludeTripId) {
        dispatchEligibility.assertEligible(vehicleId, from, to, requiredVehicleTypeId, requiredCapacityKg,
                excludeTripId);
    }
}
