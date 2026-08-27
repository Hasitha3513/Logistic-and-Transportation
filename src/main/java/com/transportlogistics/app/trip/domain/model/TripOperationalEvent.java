package com.transportlogistics.app.trip.domain.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public record TripOperationalEvent(
        UUID id,
        UUID tripId,
        TripOperationalEventType eventType,
        OffsetDateTime occurredAt,
        UUID locationId,
        String locationDescription,
        TripCheckpointType checkpointType,
        Integer delayMinutes,
        String reason,
        TripIncidentSeverity incidentSeverity,
        String remarks,
        String recordedBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public TripOperationalEvent {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(tripId, "tripId cannot be null");
        Objects.requireNonNull(eventType, "eventType cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        Objects.requireNonNull(recordedBy, "recordedBy cannot be null");

        if (recordedBy.isBlank()) {
            throw new IllegalArgumentException("recordedBy cannot be blank");
        }

        switch (eventType) {
            case CHECKPOINT -> {
                if (checkpointType == null) {
                    throw new IllegalArgumentException("checkpointType is required for CHECKPOINT events");
                }
            }
            case DELAY -> {
                if (delayMinutes == null || delayMinutes <= 0) {
                    throw new IllegalArgumentException("delayMinutes must be strictly greater than zero for DELAY events");
                }
                if (reason == null || reason.isBlank()) {
                    throw new IllegalArgumentException("reason is required for DELAY events");
                }
            }
            case INCIDENT -> {
                if (incidentSeverity == null) {
                    throw new IllegalArgumentException("incidentSeverity is required for INCIDENT events");
                }
                if (reason == null || reason.isBlank()) {
                    throw new IllegalArgumentException("reason is required for INCIDENT events");
                }
            }
        }
    }

    public static TripOperationalEvent createCheckpoint(
            UUID id,
            UUID tripId,
            TripCheckpointType checkpointType,
            OffsetDateTime occurredAt,
            UUID locationId,
            String locationDescription,
            String remarks,
            String recordedBy,
            OffsetDateTime now
    ) {
        return new TripOperationalEvent(
                id,
                tripId,
                TripOperationalEventType.CHECKPOINT,
                occurredAt,
                locationId,
                locationDescription,
                checkpointType,
                null,
                null,
                null,
                remarks,
                recordedBy,
                now,
                now
        );
    }

    public static TripOperationalEvent createDelay(
            UUID id,
            UUID tripId,
            int delayMinutes,
            String reason,
            OffsetDateTime occurredAt,
            UUID locationId,
            String locationDescription,
            String remarks,
            String recordedBy,
            OffsetDateTime now
    ) {
        return new TripOperationalEvent(
                id,
                tripId,
                TripOperationalEventType.DELAY,
                occurredAt,
                locationId,
                locationDescription,
                null,
                delayMinutes,
                reason,
                null,
                remarks,
                recordedBy,
                now,
                now
        );
    }

    public static TripOperationalEvent createIncident(
            UUID id,
            UUID tripId,
            TripIncidentSeverity severity,
            String description,
            OffsetDateTime occurredAt,
            UUID locationId,
            String locationDescription,
            String remarks,
            String recordedBy,
            OffsetDateTime now
    ) {
        return new TripOperationalEvent(
                id,
                tripId,
                TripOperationalEventType.INCIDENT,
                occurredAt,
                locationId,
                locationDescription,
                null,
                null,
                description,
                severity,
                remarks,
                recordedBy,
                now,
                now
        );
    }
}
