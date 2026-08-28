package com.transportlogistics.app.routing.infrastructure.adapters.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "route_revision")
@Getter
@Setter
@NoArgsConstructor
class RouteRevisionEntity extends com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "route_id", nullable = false)
    private UUID routeId;

    @Column(name = "revision_number", nullable = false)
    private int revisionNumber;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "origin_location_id", nullable = false)
    private UUID originLocationId;

    @Column(name = "destination_location_id", nullable = false)
    private UUID destinationLocationId;

    @Column(name = "planned_distance_km", nullable = false)
    private Double plannedDistanceKm;

    @Column(name = "estimated_duration_minutes", nullable = false)
    private Integer estimatedDurationMinutes;

    @Column(name = "active", nullable = false)
    private boolean active;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "route_revision_stop", joinColumns = @JoinColumn(name = "route_revision_id"))
    @OrderColumn(name = "stop_order")
    @Column(name = "location_id", nullable = false)
    private List<UUID> stopLocationIds = new ArrayList<>();

    @Column(name = "changed_at", nullable = false)
    private OffsetDateTime changedAt;

    @Column(name = "changed_by", nullable = false)
    private String changedBy;

    public RouteRevisionEntity(UUID id, UUID routeId, int revisionNumber, String code, String name,
                               UUID originLocationId, UUID destinationLocationId, Double plannedDistanceKm,
                               Integer estimatedDurationMinutes, boolean active, List<UUID> stopLocationIds,
                               OffsetDateTime changedAt, String changedBy) {
        this.id = id;
        this.routeId = routeId;
        this.revisionNumber = revisionNumber;
        this.code = code;
        this.name = name;
        this.originLocationId = originLocationId;
        this.destinationLocationId = destinationLocationId;
        this.plannedDistanceKm = plannedDistanceKm;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.active = active;
        this.stopLocationIds = stopLocationIds != null ? new ArrayList<>(stopLocationIds) : new ArrayList<>();
        this.changedAt = changedAt;
        this.changedBy = changedBy;
    }
}
