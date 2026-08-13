package com.transportlogistics.app.trip.infrastructure.adapters.out.fleet;

import com.transportlogistics.app.fleet.VehicleDispatchEligibility;
import com.transportlogistics.app.trip.application.ports.out.VehicleEligibilityPort;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
class VehicleEligibilityAdapter implements VehicleEligibilityPort {
    private final VehicleDispatchEligibility fleet;

    VehicleEligibilityAdapter(VehicleDispatchEligibility fleet) {
        this.fleet = fleet;
    }

    @Override
    public void assertEligible(UUID vehicleId, LocalDate onDate) {
        fleet.assertEligible(vehicleId, onDate);
    }
}
