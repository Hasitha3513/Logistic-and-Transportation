package com.transportlogistics.app.routing.infrastructure.adapters.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "route")
@Getter
@Setter
@NoArgsConstructor
class RouteEntity {
    @Id
    @Column(name = "id")
    private UUID id;
    @Column(name = "code")
    private String code;
    @Column(name = "name")
    private String name;
    @Column(name = "origin_location_id")
    private UUID originLocationId;
    @Column(name = "destination_location_id")
    private UUID destinationLocationId;
    @Column(name = "planned_distance_km")
    private Double plannedDistanceKm;
    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;
    @Column(name = "active")
    private boolean active;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "route_stop", joinColumns = @JoinColumn(name = "route_id"))
    @OrderColumn(name = "stop_order")
    @Column(name = "location_id", nullable = false)
    private List<UUID> stopLocationIds = new ArrayList<>();

    public RouteEntity(UUID id, String code, String name, UUID originLocationId, UUID destinationLocationId,
                       Double plannedDistanceKm, Integer estimatedDurationMinutes, boolean active,
                       List<UUID> stopLocationIds) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.originLocationId = originLocationId;
        this.destinationLocationId = destinationLocationId;
        this.plannedDistanceKm = plannedDistanceKm;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.active = active;
        this.stopLocationIds = stopLocationIds != null ? stopLocationIds : new ArrayList<>();
    }
}
