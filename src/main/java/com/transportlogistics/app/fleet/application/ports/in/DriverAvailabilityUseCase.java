package com.transportlogistics.app.fleet.application.ports.in;

import com.transportlogistics.app.fleet.domain.model.DriverAvailability;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface DriverAvailabilityUseCase {
    DriverAvailability evaluate(Query query);

    record Query(UUID driverId, OffsetDateTime from, OffsetDateTime to, String requiredLicenseClass,
                 UUID excludeTripId, boolean checkAssignmentConflicts, boolean lockDriver) {
        public Query(UUID driverId, OffsetDateTime from, OffsetDateTime to, String requiredLicenseClass,
                     UUID excludeTripId) {
            this(driverId, from, to, requiredLicenseClass, excludeTripId, true, false);
        }
    }
}
