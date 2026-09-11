package com.transportlogistics.app.trip.application.ports.out;

import com.transportlogistics.app.trip.domain.model.Trip;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;

public interface TripRepository {
    Trip save(Trip t);

    Optional<Trip> findById(UUID id);

    Optional<Trip> findByIdForUpdate(UUID id);

    List<Trip> findAll();

    default List<Trip> findAllByIds(Set<UUID> ids) {
        return findAll().stream().filter(trip -> ids.contains(trip.id())).toList();
    }

    boolean hasOverlappingVehicleAllocation(UUID vehicleId, OffsetDateTime from, OffsetDateTime to,
                                            UUID excludeTripId);

    boolean hasOverlappingDriverAssignment(UUID driverId, OffsetDateTime from, OffsetDateTime to,
                                           UUID excludeTripId);
}
