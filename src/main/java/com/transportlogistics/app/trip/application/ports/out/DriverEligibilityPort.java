package com.transportlogistics.app.trip.application.ports.out;

import java.time.LocalDate;
import java.util.UUID;

public interface DriverEligibilityPort {
    void assertEligible(UUID driverId, String requiredLicenseClass, LocalDate onDate);
}
