package com.transportlogistics.app.trip.application.ports.out;

import com.transportlogistics.app.trip.domain.model.TripHistoryEntry;

import java.util.List;
import java.util.UUID;

public interface TripHistoryRepository {
    TripHistoryEntry save(TripHistoryEntry entry);

    List<TripHistoryEntry> findByTripId(UUID tripId);
}
