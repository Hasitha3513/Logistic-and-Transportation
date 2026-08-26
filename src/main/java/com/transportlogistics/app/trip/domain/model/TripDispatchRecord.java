package com.transportlogistics.app.trip.domain.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public record TripDispatchRecord(UUID tripId, OffsetDateTime dispatchedAt, String dispatchedBy, String remarks) {
    public TripDispatchRecord {
        Objects.requireNonNull(tripId, "Dispatched trip id is required");
        Objects.requireNonNull(dispatchedAt, "Dispatch timestamp is required");
        if (dispatchedBy == null || dispatchedBy.isBlank()) {
            throw new IllegalArgumentException("Dispatch actor is required");
        }
        dispatchedBy = dispatchedBy.trim();
        remarks = remarks == null || remarks.isBlank() ? null : remarks.trim();
    }
}
