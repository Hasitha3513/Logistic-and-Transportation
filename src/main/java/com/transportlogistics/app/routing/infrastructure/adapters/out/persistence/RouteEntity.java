package com.transportlogistics.app.routing.infrastructure.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "route")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
}
