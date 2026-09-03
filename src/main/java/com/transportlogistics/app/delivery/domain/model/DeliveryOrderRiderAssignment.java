package com.transportlogistics.app.delivery.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public class DeliveryOrderRiderAssignment {

    private final UUID id;
    private final UUID tenantId;
    private final UUID deliveryOrderId;
    private final UUID riderId;
    private DeliveryRiderAssignmentStatus status;
    private final OffsetDateTime assignedAt;
    private final String assignedBy;
    private OffsetDateTime unassignedAt;
    private String unassignedBy;
    private final boolean isOverride;
    private final String overrideReason;
    private final long version;

    public DeliveryOrderRiderAssignment(
            UUID id,
            UUID tenantId,
            UUID deliveryOrderId,
            UUID riderId,
            DeliveryRiderAssignmentStatus status,
            OffsetDateTime assignedAt,
            String assignedBy,
            OffsetDateTime unassignedAt,
            String unassignedBy,
            boolean isOverride,
            String overrideReason,
            long version
    ) {
        if (id == null) throw new BusinessRuleException("DELIVERY_RIDER_ASSIGNMENT_ID_REQUIRED", "Assignment ID is required");
        if (tenantId == null) throw new BusinessRuleException("DELIVERY_RIDER_TENANT_REQUIRED", "Tenant ID is required");
        if (deliveryOrderId == null) throw new BusinessRuleException("DELIVERY_ORDER_ID_REQUIRED", "Delivery order ID is required");
        if (riderId == null) throw new BusinessRuleException("DELIVERY_RIDER_ID_REQUIRED", "Rider ID is required");
        if (isOverride && (overrideReason == null || overrideReason.isBlank())) {
            throw new BusinessRuleException("DELIVERY_RIDER_OVERRIDE_REASON_REQUIRED", "Override reason is mandatory when override flag is set");
        }

        this.id = id;
        this.tenantId = tenantId;
        this.deliveryOrderId = deliveryOrderId;
        this.riderId = riderId;
        this.status = status != null ? status : DeliveryRiderAssignmentStatus.ACTIVE;
        this.assignedAt = assignedAt != null ? assignedAt : OffsetDateTime.now();
        this.assignedBy = assignedBy != null ? assignedBy : "system";
        this.unassignedAt = unassignedAt;
        this.unassignedBy = unassignedBy;
        this.isOverride = isOverride;
        this.overrideReason = overrideReason != null ? overrideReason.trim() : null;
        this.version = version;
    }

    public static DeliveryOrderRiderAssignment create(
            UUID id,
            UUID tenantId,
            UUID deliveryOrderId,
            UUID riderId,
            boolean isOverride,
            String overrideReason,
            String actor,
            OffsetDateTime now
    ) {
        return new DeliveryOrderRiderAssignment(
                id != null ? id : UUID.randomUUID(),
                tenantId,
                deliveryOrderId,
                riderId,
                DeliveryRiderAssignmentStatus.ACTIVE,
                now,
                actor,
                null,
                null,
                isOverride,
                overrideReason,
                0L
        );
    }

    public void complete(String actor, OffsetDateTime now) {
        if (this.status != DeliveryRiderAssignmentStatus.ACTIVE) {
            throw new BusinessRuleException("DELIVERY_RIDER_ASSIGNMENT_NOT_ACTIVE", "Only active assignments can be completed");
        }
        this.status = DeliveryRiderAssignmentStatus.COMPLETED;
        this.unassignedAt = now;
        this.unassignedBy = actor != null ? actor : "system";
    }

    public void reassign(String actor, OffsetDateTime now) {
        if (this.status != DeliveryRiderAssignmentStatus.ACTIVE) {
            throw new BusinessRuleException("DELIVERY_RIDER_ASSIGNMENT_NOT_ACTIVE", "Only active assignments can be reassigned");
        }
        this.status = DeliveryRiderAssignmentStatus.REASSIGNED;
        this.unassignedAt = now;
        this.unassignedBy = actor != null ? actor : "system";
    }

    public void unassign(String actor, OffsetDateTime now) {
        if (this.status != DeliveryRiderAssignmentStatus.ACTIVE) {
            throw new BusinessRuleException("DELIVERY_RIDER_ASSIGNMENT_NOT_ACTIVE", "Only active assignments can be unassigned");
        }
        this.status = DeliveryRiderAssignmentStatus.CANCELLED;
        this.unassignedAt = now;
        this.unassignedBy = actor != null ? actor : "system";
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeliveryOrderRiderAssignment that = (DeliveryOrderRiderAssignment) o;
        return Objects.equals(id, that.id) && Objects.equals(tenantId, that.tenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tenantId);
    }
}
