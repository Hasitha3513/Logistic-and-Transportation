package com.transportlogistics.app.trip.application.service;

import com.transportlogistics.app.trip.TripReplayQuery;
import com.transportlogistics.app.trip.TripReplayQueryException;
import com.transportlogistics.app.trip.application.ports.out.TripReplayRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class TripReplayQueryService implements TripReplayQuery {
    private static final Duration MAX_RANGE = Duration.ofDays(MAX_RANGE_DAYS);
    private final TripReplayRepository repository;

    public TripReplayQueryService(TripReplayRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public Optional<TripReplayQuery.TripReplayScope> findReplayScope(UUID tenantId, UUID tripId) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(tripId, "tripId");
        return repository.findReplayScope(tenantId, tripId);
    }

    @Override
    public List<TripReplayQuery.VehicleTripAssignmentInterval> findAssignmentsOverlapping(
            UUID tenantId, UUID vehicleId, Instant rangeStart, Instant rangeEnd) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(vehicleId, "vehicleId");
        Objects.requireNonNull(rangeStart, "rangeStart");
        Objects.requireNonNull(rangeEnd, "rangeEnd");
        if (!rangeStart.isBefore(rangeEnd)) {
            throw new IllegalArgumentException("Assignment range start must precede end");
        }
        if (Duration.between(rangeStart, rangeEnd).compareTo(MAX_RANGE) > 0) {
            throw new IllegalArgumentException("Assignment range must not exceed seven days");
        }
        List<TripReplayQuery.VehicleTripAssignmentInterval> intervals = repository.findAssignmentsOverlapping(
                tenantId, vehicleId, rangeStart, rangeEnd, MAX_ASSIGNMENT_INTERVALS + 1);
        if (intervals.size() > MAX_ASSIGNMENT_INTERVALS) {
            throw new TripReplayQueryException(TripReplayQueryException.RESULT_LIMIT_EXCEEDED,
                    "Assignment attribution cannot be completed safely; request a narrower time range");
        }
        return List.copyOf(intervals);
    }
}
