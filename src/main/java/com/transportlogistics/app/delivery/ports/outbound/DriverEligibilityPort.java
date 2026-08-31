package com.transportlogistics.app.delivery.ports.outbound;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface DriverEligibilityPort {

    Optional<DriverSummary> findDriver(UUID driverId);

    boolean isOperationallyEligible(UUID driverId, OffsetDateTime from, OffsetDateTime to);

    record DriverSummary(
            UUID id,
            String employeeNumber,
            String firstName,
            String lastName,
            String status,
            boolean active
    ) {
    }
}
