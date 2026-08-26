package com.transportlogistics.app.trip.application.ports.out;

import com.transportlogistics.app.trip.domain.model.TripDispatchRecord;

import java.util.Optional;
import java.util.UUID;

public interface TripDispatchRepository {
    TripDispatchRecord save(TripDispatchRecord record);

    Optional<TripDispatchRecord> findByTripId(UUID tripId);
}
