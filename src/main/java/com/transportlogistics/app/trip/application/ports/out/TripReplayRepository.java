package com.transportlogistics.app.trip.application.ports.out;

import com.transportlogistics.app.trip.TripReplayQuery.TripReplayScope;
import com.transportlogistics.app.trip.TripReplayQuery.VehicleTripAssignmentInterval;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TripReplayRepository {
    Optional<TripReplayScope> findReplayScope(UUID tenantId, UUID tripId);

    List<VehicleTripAssignmentInterval> findAssignmentsOverlapping(
            UUID tenantId, UUID vehicleId, Instant rangeStart, Instant rangeEnd, int limit);
}
