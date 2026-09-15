package com.transportlogistics.app.trip;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Published, Tenant-qualified Trip facts required by bounded journey replay. */
public interface TripReplayQuery {
    int MAX_RANGE_DAYS = 7;
    int MAX_ASSIGNMENT_INTERVALS = 2_000;

    Optional<TripReplayScope> findReplayScope(UUID tenantId, UUID tripId);

    List<VehicleTripAssignmentInterval> findAssignmentsOverlapping(
            UUID tenantId, UUID vehicleId, Instant rangeStart, Instant rangeEnd);

    record TripReplayScope(UUID tripId, UUID vehicleId, Instant actualStartTime,
                           Instant actualEndTime, String tripStatus, UUID routeId,
                           String routeVersion) {
    }

    record VehicleTripAssignmentInterval(UUID tripId, UUID vehicleId, Instant effectiveStart,
                                         Instant effectiveEnd, UUID routeId, String routeVersion,
                                         String assignmentStatus) {
    }
}
