package com.transportlogistics.app.trip.application.ports.in;

import com.transportlogistics.app.trip.domain.model.TripCheckpointType;
import com.transportlogistics.app.trip.domain.model.TripIncidentSeverity;
import com.transportlogistics.app.trip.domain.model.TripOperationalEvent;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface TripOperationalEventUseCase {

    record RecordCheckpointCommand(
            TripCheckpointType checkpointType,
            OffsetDateTime occurredAt,
            UUID locationId,
            String locationDescription,
            String remarks
    ) {}

    record RecordDelayCommand(
            int delayMinutes,
            String reason,
            OffsetDateTime occurredAt,
            UUID locationId,
            String locationDescription,
            String remarks
    ) {}

    record RecordIncidentCommand(
            TripIncidentSeverity incidentSeverity,
            String description,
            OffsetDateTime occurredAt,
            UUID locationId,
            String locationDescription,
            String remarks
    ) {}

    TripOperationalEvent recordCheckpoint(UUID tripId, RecordCheckpointCommand command, String actor);

    TripOperationalEvent recordDelay(UUID tripId, RecordDelayCommand command, String actor);

    TripOperationalEvent recordIncident(UUID tripId, RecordIncidentCommand command, String actor);

    List<TripOperationalEvent> getTripEvents(UUID tripId);

    TripOperationalEvent getEvent(UUID tripId, UUID eventId);
}
