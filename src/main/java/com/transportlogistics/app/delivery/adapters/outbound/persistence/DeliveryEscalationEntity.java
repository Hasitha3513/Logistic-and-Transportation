package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "delivery_escalation")
public class DeliveryEscalationEntity extends TenantScopedEntity {
    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "delivery_id", nullable = false)
    private UUID deliveryId;

    @Column(name = "delivery_attempt_id")
    private UUID deliveryAttemptId;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "resolution_notes")
    private String resolutionNotes;

    @Column(name = "escalated_by", nullable = false, length = 128)
    private String escalatedBy;

    @Column(name = "escalated_at", nullable = false)
    private OffsetDateTime escalatedAt;

    @Column(name = "resolved_by", length = 128)
    private String resolvedBy;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getDeliveryId() { return deliveryId; }
    public void setDeliveryId(UUID deliveryId) { this.deliveryId = deliveryId; }

    public UUID getDeliveryAttemptId() { return deliveryAttemptId; }
    public void setDeliveryAttemptId(UUID deliveryAttemptId) { this.deliveryAttemptId = deliveryAttemptId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public String getEscalatedBy() { return escalatedBy; }
    public void setEscalatedBy(String escalatedBy) { this.escalatedBy = escalatedBy; }

    public OffsetDateTime getEscalatedAt() { return escalatedAt; }
    public void setEscalatedAt(OffsetDateTime escalatedAt) { this.escalatedAt = escalatedAt; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }

    public OffsetDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(OffsetDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
