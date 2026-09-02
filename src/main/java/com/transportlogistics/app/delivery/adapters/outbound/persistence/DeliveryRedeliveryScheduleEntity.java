package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.domain.model.DeliveryId;
import com.transportlogistics.app.delivery.domain.model.DeliveryRedeliverySchedule;
import com.transportlogistics.app.delivery.domain.model.RedeliveryScheduleStatus;
import com.transportlogistics.app.delivery.domain.model.RedeliverySchedulingMethod;
import com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "delivery_redelivery_schedule")
public class DeliveryRedeliveryScheduleEntity extends TenantScopedEntity {

    @Id
    private UUID id;

    @Column(name = "delivery_order_id", nullable = false)
    private UUID deliveryOrderId;

    @Column(name = "delivery_attempt_id", nullable = false)
    private UUID deliveryAttemptId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scheduling_method", nullable = false)
    private RedeliverySchedulingMethod schedulingMethod;

    @Column(name = "preferred_start_time")
    private OffsetDateTime preferredStartTime;

    @Column(name = "preferred_end_time")
    private OffsetDateTime preferredEndTime;

    @Column(name = "customer_preference_notes", length = 500)
    private String customerPreferenceNotes;

    @Column(name = "scheduled_start_time", nullable = false)
    private OffsetDateTime scheduledStartTime;

    @Column(name = "scheduled_end_time", nullable = false)
    private OffsetDateTime scheduledEndTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RedeliveryScheduleStatus status;

    @Column(name = "scheduled_by", nullable = false, length = 100)
    private String scheduledBy;

    @Column(name = "scheduled_at", nullable = false)
    private OffsetDateTime scheduledAt;

    @Column(name = "superseded_at")
    private OffsetDateTime supersededAt;

    @Column(name = "superseded_by", length = 100)
    private String supersededBy;

    @Column(name = "supersede_reason", length = 500)
    private String supersedeReason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public DeliveryRedeliveryScheduleEntity() {
    }

    public static DeliveryRedeliveryScheduleEntity fromDomain(DeliveryRedeliverySchedule domain) {
        DeliveryRedeliveryScheduleEntity entity = new DeliveryRedeliveryScheduleEntity();
        entity.id = domain.id();
        entity.tenantId = domain.tenantId();
        entity.deliveryOrderId = domain.deliveryOrderId().value();
        entity.deliveryAttemptId = domain.deliveryAttemptId();
        entity.schedulingMethod = domain.schedulingMethod();
        entity.preferredStartTime = domain.preferredStartTime();
        entity.preferredEndTime = domain.preferredEndTime();
        entity.customerPreferenceNotes = domain.customerPreferenceNotes();
        entity.scheduledStartTime = domain.scheduledStartTime();
        entity.scheduledEndTime = domain.scheduledEndTime();
        entity.status = domain.status();
        entity.scheduledBy = domain.scheduledBy();
        entity.scheduledAt = domain.scheduledAt();
        entity.supersededAt = domain.supersededAt();
        entity.supersededBy = domain.supersededBy();
        entity.supersedeReason = domain.supersedeReason();
        entity.createdAt = domain.createdAt();
        entity.updatedAt = domain.updatedAt();
        return entity;
    }

    public DeliveryRedeliverySchedule toDomain() {
        return new DeliveryRedeliverySchedule(
                this.id,
                this.tenantId,
                new DeliveryId(this.deliveryOrderId),
                this.deliveryAttemptId,
                this.schedulingMethod,
                this.preferredStartTime,
                this.preferredEndTime,
                this.customerPreferenceNotes,
                this.scheduledStartTime,
                this.scheduledEndTime,
                this.status,
                this.scheduledBy,
                this.scheduledAt,
                this.supersededAt,
                this.supersededBy,
                this.supersedeReason,
                this.createdAt,
                this.updatedAt
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getDeliveryOrderId() {
        return deliveryOrderId;
    }

    public UUID getDeliveryAttemptId() {
        return deliveryAttemptId;
    }

    public RedeliverySchedulingMethod getSchedulingMethod() {
        return schedulingMethod;
    }

    public OffsetDateTime getPreferredStartTime() {
        return preferredStartTime;
    }

    public OffsetDateTime getPreferredEndTime() {
        return preferredEndTime;
    }

    public String getCustomerPreferenceNotes() {
        return customerPreferenceNotes;
    }

    public OffsetDateTime getScheduledStartTime() {
        return scheduledStartTime;
    }

    public OffsetDateTime getScheduledEndTime() {
        return scheduledEndTime;
    }

    public RedeliveryScheduleStatus getStatus() {
        return status;
    }

    public String getScheduledBy() {
        return scheduledBy;
    }

    public OffsetDateTime getScheduledAt() {
        return scheduledAt;
    }

    public OffsetDateTime getSupersededAt() {
        return supersededAt;
    }

    public String getSupersededBy() {
        return supersededBy;
    }

    public String getSupersedeReason() {
        return supersedeReason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
