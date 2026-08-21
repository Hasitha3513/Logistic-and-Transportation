package com.transportlogistics.app.notification.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.NotificationRule;
import com.transportlogistics.app.notification.domain.model.NotificationSeverity;
import com.transportlogistics.app.notification.domain.model.RecipientType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_rule")
public class NotificationRuleEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false, length = 32)
    private RecipientType recipientType;

    @Column(name = "recipient_value", nullable = false, length = 128)
    private String recipientValue;

    @Column(name = "template_code", length = 64)
    private String templateCode;

    @Column(nullable = false)
    private boolean enabled;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity_threshold", nullable = false, length = 32)
    private NotificationSeverity severityThreshold;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public NotificationRuleEntity() {}

    public NotificationRuleEntity(
        UUID id,
        String name,
        String description,
        String eventType,
        NotificationChannel channel,
        RecipientType recipientType,
        String recipientValue,
        String templateCode,
        boolean enabled,
        NotificationSeverity severityThreshold,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.eventType = eventType;
        this.channel = channel;
        this.recipientType = recipientType;
        this.recipientValue = recipientValue;
        this.templateCode = templateCode;
        this.enabled = enabled;
        this.severityThreshold = severityThreshold;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static NotificationRuleEntity fromDomain(NotificationRule rule) {
        return new NotificationRuleEntity(
            rule.id(),
            rule.name(),
            rule.description(),
            rule.eventType(),
            rule.channel(),
            rule.recipientType(),
            rule.recipientValue(),
            rule.templateCode(),
            rule.enabled(),
            rule.severityThreshold(),
            rule.createdAt(),
            rule.updatedAt()
        );
    }

    public NotificationRule toDomain() {
        return new NotificationRule(
            id,
            name,
            description,
            eventType,
            channel,
            recipientType,
            recipientValue,
            templateCode,
            enabled,
            severityThreshold,
            createdAt,
            updatedAt
        );
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public NotificationChannel getChannel() { return channel; }
    public void setChannel(NotificationChannel channel) { this.channel = channel; }

    public RecipientType getRecipientType() { return recipientType; }
    public void setRecipientType(RecipientType recipientType) { this.recipientType = recipientType; }

    public String getRecipientValue() { return recipientValue; }
    public void setRecipientValue(String recipientValue) { this.recipientValue = recipientValue; }

    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public NotificationSeverity getSeverityThreshold() { return severityThreshold; }
    public void setSeverityThreshold(NotificationSeverity severityThreshold) { this.severityThreshold = severityThreshold; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
