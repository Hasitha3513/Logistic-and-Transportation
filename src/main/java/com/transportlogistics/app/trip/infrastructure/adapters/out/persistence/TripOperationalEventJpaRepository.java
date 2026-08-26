package com.transportlogistics.app.trip.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TripOperationalEventJpaRepository extends JpaRepository<TripOperationalEventEntity, UUID> {

    List<TripOperationalEventEntity> findByTripIdOrderByOccurredAtAscCreatedAtAsc(UUID tripId);

    List<TripOperationalEventEntity> findByTripIdIn(java.util.Collection<UUID> tripIds);
}
