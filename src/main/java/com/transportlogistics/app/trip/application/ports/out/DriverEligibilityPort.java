package com.transportlogistics.app.trip.application.ports.out;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface DriverEligibilityPort {
    void assertEligible(UUID driverId, String requiredLicenseClass, OffsetDateTime from, OffsetDateTime to,
                        UUID excludeTripId);
}
