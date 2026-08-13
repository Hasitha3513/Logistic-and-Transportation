package com.transportlogistics.app.trip.application.ports.out;

import com.transportlogistics.app.trip.domain.model.Trip;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TripRepository {
    Trip save(Trip t);

    Optional<Trip> findById(UUID id);

    List<Trip> findAll();

    boolean hasOverlappingVehicleAllocation(UUID vehicleId, OffsetDateTime from, OffsetDateTime to,
                                            UUID excludeTripId);
}
