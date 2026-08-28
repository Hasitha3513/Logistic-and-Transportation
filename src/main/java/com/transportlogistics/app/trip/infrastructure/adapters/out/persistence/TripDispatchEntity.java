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
@Table(name = "trip_dispatch")
@Getter
@Setter
@NoArgsConstructor
class TripDispatchEntity extends com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity {
    @Id
    @Column(name = "trip_id")
    private UUID tripId;
    @Column(name = "dispatched_at", nullable = false)
    private OffsetDateTime dispatchedAt;
    @Column(name = "dispatched_by", nullable = false)
    private String dispatchedBy;
    @Column(name = "remarks")
    private String remarks;
}
