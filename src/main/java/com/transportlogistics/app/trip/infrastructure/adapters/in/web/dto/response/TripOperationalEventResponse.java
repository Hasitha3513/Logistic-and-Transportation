package com.transportlogistics.app.trip.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.trip.domain.model.TripCheckpointType;
import com.transportlogistics.app.trip.domain.model.TripIncidentSeverity;
import com.transportlogistics.app.trip.domain.model.TripOperationalEventType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TripOperationalEventResponse(
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
) {}
