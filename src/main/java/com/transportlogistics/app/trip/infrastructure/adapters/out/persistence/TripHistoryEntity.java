package com.transportlogistics.app.trip.infrastructure.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "trip_status_history")
@Getter
@Setter
@NoArgsConstructor
class TripHistoryEntity extends com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity {
    @Id
    private UUID id;
    @Column(name = "trip_id", nullable = false)
    private UUID tripId;
    @Column(name = "from_status")
    private String fromStatus;
    @Column(name = "to_status", nullable = false)
    private String toStatus;
    @Column(name = "action", nullable = false)
    private String action;
    @Column(name = "vehicle_id")
    private UUID vehicleId;
    @Column(name = "driver_id")
    private UUID driverId;
    @Column(name = "license_class")
    private String licenseClass;
    @Column(name = "actor", nullable = false)
    private String actor;
    @Column(name = "details")
    private String details;
    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;
}
