package com.transportlogistics.app.trip.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.trip.domain.model.TripCheckpointType;
import com.transportlogistics.app.trip.domain.model.TripIncidentSeverity;
import com.transportlogistics.app.trip.domain.model.TripOperationalEventType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "trip_operational_event")
@Getter
@Setter
public class TripOperationalEventEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "trip_id", nullable = false)
    private UUID tripId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private TripOperationalEventType eventType;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "location_id")
    private UUID locationId;

    @Column(name = "location_description", length = 255)
    private String locationDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "checkpoint_type", length = 32)
    private TripCheckpointType checkpointType;

    @Column(name = "delay_minutes")
    private Integer delayMinutes;

    @Column(name = "reason", length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "incident_severity", length = 32)
    private TripIncidentSeverity incidentSeverity;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "recorded_by", nullable = false, length = 128)
    private String recordedBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
