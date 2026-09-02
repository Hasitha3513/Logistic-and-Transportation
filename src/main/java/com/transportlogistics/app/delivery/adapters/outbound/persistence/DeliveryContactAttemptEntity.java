package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "delivery_contact_attempt")
public class DeliveryContactAttemptEntity extends TenantScopedEntity {
    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "delivery_attempt_id", nullable = false)
    private UUID deliveryAttemptId;

    @Column(nullable = false, length = 50)
    private String channel;

    @Column(name = "contact_timestamp", nullable = false)
    private OffsetDateTime contactTimestamp;

    @Column(nullable = false, length = 50)
    private String outcome;

    @Column(length = 500)
    private String notes;

    @Column(name = "recorded_by", nullable = false, length = 128)
    private String recordedBy;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getDeliveryAttemptId() { return deliveryAttemptId; }
    public void setDeliveryAttemptId(UUID deliveryAttemptId) { this.deliveryAttemptId = deliveryAttemptId; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public OffsetDateTime getContactTimestamp() { return contactTimestamp; }
    public void setContactTimestamp(OffsetDateTime contactTimestamp) { this.contactTimestamp = contactTimestamp; }

    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }

    public OffsetDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(OffsetDateTime recordedAt) { this.recordedAt = recordedAt; }
}
