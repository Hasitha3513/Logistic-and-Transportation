package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "delivery_attempt")
public class DeliveryAttemptEntity extends TenantScopedEntity {
    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "delivery_id", nullable = false)
    private UUID deliveryId;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "attempt_timestamp", nullable = false)
    private OffsetDateTime attemptTimestamp;

    @Column(name = "failure_reason", nullable = false, length = 50)
    private String failureReason;

    @Column(name = "notes")
    private String notes;

    @Column(nullable = false, length = 50)
    private String disposition;

    @Column(name = "recorded_by", nullable = false, length = 128)
    private String recordedBy;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getDeliveryId() { return deliveryId; }
    public void setDeliveryId(UUID deliveryId) { this.deliveryId = deliveryId; }

    public int getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(int attemptNumber) { this.attemptNumber = attemptNumber; }

    public OffsetDateTime getAttemptTimestamp() { return attemptTimestamp; }
    public void setAttemptTimestamp(OffsetDateTime attemptTimestamp) { this.attemptTimestamp = attemptTimestamp; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getDisposition() { return disposition; }
    public void setDisposition(String disposition) { this.disposition = disposition; }

    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }

    public OffsetDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(OffsetDateTime recordedAt) { this.recordedAt = recordedAt; }
}
