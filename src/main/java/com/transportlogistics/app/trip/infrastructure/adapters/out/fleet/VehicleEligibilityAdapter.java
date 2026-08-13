package com.transportlogistics.app.trip.infrastructure.adapters.out.fleet;

import com.transportlogistics.app.fleet.VehicleDispatchEligibility;
import com.transportlogistics.app.trip.application.ports.out.VehicleEligibilityPort;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
class VehicleEligibilityAdapter implements VehicleEligibilityPort {
    private final VehicleDispatchEligibility fleet;

    VehicleEligibilityAdapter(VehicleDispatchEligibility fleet) {
        this.fleet = fleet;
    }

    @Override
    public void assertEligible(UUID vehicleId, OffsetDateTime from, OffsetDateTime to, UUID requiredVehicleTypeId,
                               Double requiredCapacityKg, UUID excludeTripId) {
        fleet.assertEligible(vehicleId, from, to, requiredVehicleTypeId, requiredCapacityKg, excludeTripId);
    }
}
