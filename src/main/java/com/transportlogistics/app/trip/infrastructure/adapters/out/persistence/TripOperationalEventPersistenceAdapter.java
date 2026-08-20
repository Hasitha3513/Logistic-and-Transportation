package com.transportlogistics.app.trip.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.trip.application.ports.out.TripOperationalEventRepository;
import com.transportlogistics.app.trip.domain.model.TripOperationalEvent;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class TripOperationalEventPersistenceAdapter implements TripOperationalEventRepository {

    private final TripOperationalEventJpaRepository repo;

    public TripOperationalEventPersistenceAdapter(TripOperationalEventJpaRepository repo) {
        this.repo = Objects.requireNonNull(repo, "TripOperationalEventJpaRepository cannot be null");
    }

    @Override
    public TripOperationalEvent save(TripOperationalEvent event) {
        var entity = toEntity(event);
        var saved = repo.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<TripOperationalEvent> findById(UUID id) {
        return repo.findById(id).map(this::toDomain);
    }

    @Override
    public List<TripOperationalEvent> findByTripIdOrderByOccurredAtAsc(UUID tripId) {
        return repo.findByTripIdOrderByOccurredAtAscCreatedAtAsc(tripId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private TripOperationalEventEntity toEntity(TripOperationalEvent d) {
        var e = new TripOperationalEventEntity();
        e.setId(d.id());
        e.setTripId(d.tripId());
        e.setEventType(d.eventType());
        e.setOccurredAt(d.occurredAt());
        e.setLocationId(d.locationId());
        e.setLocationDescription(d.locationDescription());
        e.setCheckpointType(d.checkpointType());
        e.setDelayMinutes(d.delayMinutes());
        e.setReason(d.reason());
        e.setIncidentSeverity(d.incidentSeverity());
        e.setRemarks(d.remarks());
        e.setRecordedBy(d.recordedBy());
        e.setCreatedAt(d.createdAt());
        e.setUpdatedAt(d.updatedAt());
        return e;
    }

    private TripOperationalEvent toDomain(TripOperationalEventEntity e) {
        return new TripOperationalEvent(
                e.getId(),
                e.getTripId(),
                e.getEventType(),
                e.getOccurredAt(),
                e.getLocationId(),
                e.getLocationDescription(),
                e.getCheckpointType(),
                e.getDelayMinutes(),
                e.getReason(),
                e.getIncidentSeverity(),
                e.getRemarks(),
                e.getRecordedBy(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
