package com.transportlogistics.app.trip;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Public Trip application boundary for appending operational events without exposing Trip internals.
 */
public interface TripOperationalEventRecorder {

    enum CheckpointType {
        DEPARTURE, ARRIVAL, PICKUP, DELIVERY, REST_STOP, CUSTOM
    }

    enum IncidentSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    record CheckpointCommand(UUID tripId, CheckpointType checkpointType, OffsetDateTime occurredAt,
                             UUID locationId, String locationDescription, String remarks, String actor) {
    }

    record DelayCommand(UUID tripId, int delayMinutes, String reason, OffsetDateTime occurredAt,
                        UUID locationId, String locationDescription, String remarks, String actor) {
    }

    record IncidentCommand(UUID tripId, IncidentSeverity incidentSeverity, String description,
                           OffsetDateTime occurredAt, UUID locationId, String locationDescription,
                           String remarks, String actor) {
    }

    record Result(UUID eventId, UUID tripId, OffsetDateTime occurredAt) {
    }

    Result recordCheckpoint(CheckpointCommand command);

    Result recordDelay(DelayCommand command);

    Result recordIncident(IncidentCommand command);
}
