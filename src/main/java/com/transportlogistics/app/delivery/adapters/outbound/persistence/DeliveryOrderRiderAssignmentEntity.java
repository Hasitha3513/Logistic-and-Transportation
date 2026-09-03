package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.domain.model.DeliveryOrderRiderAssignment;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderAssignmentStatus;
import com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "delivery_order_rider_assignment")
public class DeliveryOrderRiderAssignmentEntity extends TenantScopedEntity {

    @Id
    private UUID id;

    @Column(name = "delivery_order_id", nullable = false)
    private UUID deliveryOrderId;

    @Column(name = "rider_id", nullable = false)
    private UUID riderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DeliveryRiderAssignmentStatus status;

    @Column(name = "assigned_at", nullable = false)
    private OffsetDateTime assignedAt;

    @Column(name = "assigned_by", nullable = false)
    private String assignedBy;

    @Column(name = "unassigned_at")
    private OffsetDateTime unassignedAt;

    @Column(name = "unassigned_by")
    private String unassignedBy;

    @Column(name = "is_override", nullable = false)
    private boolean isOverride;

    @Column(name = "override_reason")
    private String overrideReason;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public DeliveryOrderRiderAssignmentEntity() {
    }

    public static DeliveryOrderRiderAssignmentEntity fromDomain(DeliveryOrderRiderAssignment domain) {
        DeliveryOrderRiderAssignmentEntity entity = new DeliveryOrderRiderAssignmentEntity();
        entity.id = domain.getId();
        entity.tenantId = domain.getTenantId();
        entity.deliveryOrderId = domain.getDeliveryOrderId();
        entity.riderId = domain.getRiderId();
        entity.status = domain.getStatus();
        entity.assignedAt = domain.getAssignedAt();
        entity.assignedBy = domain.getAssignedBy();
        entity.unassignedAt = domain.getUnassignedAt();
        entity.unassignedBy = domain.getUnassignedBy();
        entity.isOverride = domain.isOverride();
        entity.overrideReason = domain.getOverrideReason();
        entity.version = domain.getVersion();
        return entity;
    }

    public DeliveryOrderRiderAssignment toDomain() {
        return new DeliveryOrderRiderAssignment(
                id,
                tenantId,
                deliveryOrderId,
                riderId,
                status,
                assignedAt,
                assignedBy,
                unassignedAt,
                unassignedBy,
                isOverride,
                overrideReason,
                version
        );
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getDeliveryOrderId() { return deliveryOrderId; }
    public UUID getRiderId() { return riderId; }
    public DeliveryRiderAssignmentStatus getStatus() { return status; }
    public OffsetDateTime getAssignedAt() { return assignedAt; }
    public String getAssignedBy() { return assignedBy; }
    public OffsetDateTime getUnassignedAt() { return unassignedAt; }
    public String getUnassignedBy() { return unassignedBy; }
    public boolean isOverride() { return isOverride; }
    public String getOverrideReason() { return overrideReason; }
    public long getVersion() { return version; }
}
