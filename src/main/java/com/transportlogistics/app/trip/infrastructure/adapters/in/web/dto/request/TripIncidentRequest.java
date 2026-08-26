package com.transportlogistics.app.trip.infrastructure.adapters.in.web.dto.request;

import com.transportlogistics.app.trip.domain.model.TripIncidentSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TripIncidentRequest(
        @NotNull(message = "incidentSeverity is required")
        TripIncidentSeverity incidentSeverity,

        @NotBlank(message = "description is required")
        @Size(max = 500, message = "description cannot exceed 500 characters")
        String description,

        OffsetDateTime occurredAt,

        UUID locationId,

        @Size(max = 255, message = "locationDescription cannot exceed 255 characters")
        String locationDescription,

        @Size(max = 2000, message = "remarks cannot exceed 2000 characters")
        String remarks
) {}
