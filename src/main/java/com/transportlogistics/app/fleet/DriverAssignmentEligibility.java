package com.transportlogistics.app.fleet;

import java.time.LocalDate;
import java.util.UUID;

/** Public fleet contract used when a trip assigns a driver. */
public interface DriverAssignmentEligibility {
    void assertEligible(UUID driverId, String requiredLicenseClass, LocalDate onDate);
}
