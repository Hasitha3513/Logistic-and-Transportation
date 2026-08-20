package com.transportlogistics.app.trip.application.ports.out;

import com.transportlogistics.app.trip.domain.model.TripOperationalEvent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TripOperationalEventRepository {

    TripOperationalEvent save(TripOperationalEvent event);

    Optional<TripOperationalEvent> findById(UUID id);

    List<TripOperationalEvent> findByTripIdOrderByOccurredAtAsc(UUID tripId);
}
