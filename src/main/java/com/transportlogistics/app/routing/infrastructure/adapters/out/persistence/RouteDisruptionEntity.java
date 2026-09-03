package com.transportlogistics.app.routing.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.routing.domain.model.DisruptionSeverity;
import com.transportlogistics.app.routing.domain.model.DisruptionStatus;
import com.transportlogistics.app.routing.domain.model.RouteDisruptionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "route_disruption")
@Getter
@Setter
@NoArgsConstructor
class RouteDisruptionEntity extends com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "route_id", nullable = false)
    private UUID routeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "disruption_type", nullable = false)
    private RouteDisruptionType disruptionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private DisruptionSeverity severity;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "effective_from", nullable = false)
    private OffsetDateTime effectiveFrom;

    @Column(name = "effective_until")
    private OffsetDateTime effectiveUntil;

    @Column(name = "detour_route_id")
    private UUID detourRouteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DisruptionStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "resolved_by")
    private String resolvedBy;

    public RouteDisruptionEntity(UUID id, UUID routeId, RouteDisruptionType disruptionType,
                                 DisruptionSeverity severity, String description,
                                 OffsetDateTime effectiveFrom, OffsetDateTime effectiveUntil,
                                 UUID detourRouteId, DisruptionStatus status,
                                 OffsetDateTime createdAt, String createdBy,
                                 OffsetDateTime resolvedAt, String resolvedBy) {
        this.id = id;
        this.routeId = routeId;
        this.disruptionType = disruptionType;
        this.severity = severity;
        this.description = description;
        this.effectiveFrom = effectiveFrom;
        this.effectiveUntil = effectiveUntil;
        this.detourRouteId = detourRouteId;
        this.status = status;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.resolvedAt = resolvedAt;
        this.resolvedBy = resolvedBy;
    }
}
