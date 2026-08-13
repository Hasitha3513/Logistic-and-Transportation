package com.transportlogistics.app.trip.infrastructure.adapters.out.fleet;

import com.transportlogistics.app.fleet.DriverAssignmentEligibility;
import com.transportlogistics.app.trip.application.ports.out.DriverEligibilityPort;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
class DriverEligibilityAdapter implements DriverEligibilityPort {
    private final DriverAssignmentEligibility fleet;

    DriverEligibilityAdapter(DriverAssignmentEligibility fleet) {
        this.fleet = fleet;
    }

    @Override
    public void assertEligible(UUID driverId, String requiredLicenseClass, OffsetDateTime from, OffsetDateTime to) {
        fleet.assertEligible(driverId, requiredLicenseClass, from, to);
    }
}
