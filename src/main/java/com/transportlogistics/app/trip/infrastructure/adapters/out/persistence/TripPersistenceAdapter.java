package com.transportlogistics.app.trip.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.trip.application.ports.out.TripRepository;
import com.transportlogistics.app.trip.domain.model.Trip;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class TripPersistenceAdapter implements TripRepository {
    private final TripJpaRepository repo;
    private final TripMapper mapper;

    TripPersistenceAdapter(TripJpaRepository r, TripMapper m) {
        repo = r;
        mapper = m;
    }

    public Trip save(Trip t) {
        return mapper.toDomain(repo.save(mapper.toEntity(t)));
    }

    public Optional<Trip> findById(UUID id) {
        return repo.findById(id).map(mapper::toDomain);
    }

    public List<Trip> findAll() {
        return repo.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean hasOverlappingVehicleAllocation(UUID vehicleId, OffsetDateTime from, OffsetDateTime to,
                                                   UUID excludeTripId) {
        var count = excludeTripId == null
                ? repo.countOverlaps(vehicleId, from, to)
                : repo.countOverlapsExcluding(vehicleId, from, to, excludeTripId);
        return count > 0;
    }

    @Override
    public boolean hasOverlappingDriverAssignment(UUID driverId, OffsetDateTime from, OffsetDateTime to,
                                                  UUID excludeTripId) {
        var count = excludeTripId == null
                ? repo.countDriverOverlaps(driverId, from, to)
                : repo.countDriverOverlapsExcluding(driverId, from, to, excludeTripId);
        return count > 0;
    }
}
