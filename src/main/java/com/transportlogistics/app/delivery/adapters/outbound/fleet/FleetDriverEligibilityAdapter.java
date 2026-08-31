package com.transportlogistics.app.delivery.adapters.outbound.fleet;

import com.transportlogistics.app.delivery.ports.outbound.DriverEligibilityPort;
import com.transportlogistics.app.fleet.DriverAssignmentEligibility;
import com.transportlogistics.app.fleet.DriverLookup;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
public class FleetDriverEligibilityAdapter implements DriverEligibilityPort {

    private final DriverLookup driverLookup;
    private final DriverAssignmentEligibility driverAssignmentEligibility;

    public FleetDriverEligibilityAdapter(DriverLookup driverLookup,
                                         DriverAssignmentEligibility driverAssignmentEligibility) {
        this.driverLookup = driverLookup;
        this.driverAssignmentEligibility = driverAssignmentEligibility;
    }

    @Override
    public Optional<DriverSummary> findDriver(UUID driverId) {
        if (driverId == null) {
            return Optional.empty();
        }
        return driverLookup.findDriver(driverId).map(driver -> new DriverSummary(
                driver.id(),
                driver.employeeNumber(),
                driver.firstName(),
                driver.lastName(),
                driver.status(),
                driver.active()
        ));
    }

    @Override
    public boolean isOperationallyEligible(UUID driverId, OffsetDateTime from, OffsetDateTime to) {
        if (driverId == null) {
            return false;
        }
        try {
            driverAssignmentEligibility.assertEligible(driverId, null, from, to);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
