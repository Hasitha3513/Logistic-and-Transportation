package com.transportlogistics.app.tracking.ports.outbound;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RouteDeviationAssignmentLookupPort {
    Optional<Assignment> findAt(UUID tenantId, UUID vehicleId, Instant sourceTimestamp);

    record Assignment(UUID tripId, UUID driverId, UUID routeId, String routeVersion) {
    }
}
