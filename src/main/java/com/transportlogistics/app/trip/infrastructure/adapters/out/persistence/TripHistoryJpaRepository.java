package com.transportlogistics.app.trip.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface TripHistoryJpaRepository extends JpaRepository<TripHistoryEntity, UUID> {
    List<TripHistoryEntity> findByTripIdOrderByOccurredAtAsc(UUID tripId);

    Optional<TripHistoryEntity>
    findFirstByTripIdAndDriverIdAndLicenseClassIsNotNullAndActionInOrderByOccurredAtDesc(
            UUID tripId, UUID driverId, List<String> actions);
}
