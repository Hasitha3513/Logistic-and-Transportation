package com.transportlogistics.app.trip.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.trip.application.ports.out.TripDispatchRepository;
import com.transportlogistics.app.trip.domain.model.TripDispatchRecord;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class TripDispatchPersistenceAdapter implements TripDispatchRepository {
    private final TripDispatchJpaRepository repository;

    TripDispatchPersistenceAdapter(TripDispatchJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public TripDispatchRecord save(TripDispatchRecord record) {
        var entity = new TripDispatchEntity();
        entity.setTripId(record.tripId());
        entity.setDispatchedAt(record.dispatchedAt());
        entity.setDispatchedBy(record.dispatchedBy());
        entity.setRemarks(record.remarks());
        return map(repository.save(entity));
    }

    @Override
    public Optional<TripDispatchRecord> findByTripId(UUID tripId) {
        return repository.findById(tripId).map(this::map);
    }

    private TripDispatchRecord map(TripDispatchEntity entity) {
        return new TripDispatchRecord(entity.getTripId(), entity.getDispatchedAt(), entity.getDispatchedBy(),
                entity.getRemarks());
    }
}
