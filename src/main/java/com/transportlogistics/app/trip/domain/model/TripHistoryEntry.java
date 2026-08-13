package com.transportlogistics.app.trip.domain.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public record TripHistoryEntry(UUID id, UUID tripId, String fromStatus, String toStatus, String action,
                               UUID vehicleId, UUID driverId, String actor, String details,
                               OffsetDateTime occurredAt) {
    public TripHistoryEntry {
        Objects.requireNonNull(id, "History id is required");
        Objects.requireNonNull(tripId, "History trip id is required");
        Objects.requireNonNull(action, "History action is required");
        Objects.requireNonNull(actor, "History actor is required");
        Objects.requireNonNull(occurredAt, "History timestamp is required");
    }
}
