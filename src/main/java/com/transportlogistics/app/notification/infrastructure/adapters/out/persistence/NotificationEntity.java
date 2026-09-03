package com.transportlogistics.app.notification.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.notification.domain.model.Notification;
import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.NotificationSeverity;
import com.transportlogistics.app.notification.domain.model.NotificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification")
public class NotificationEntity extends com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity {

    @Id
    private UUID id;

    @Column(name = "rule_id")
    private UUID ruleId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationChannel channel;

    @Column(nullable = false, length = 320)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationSeverity severity;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "template_version")
    private Integer templateVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "next_delivery_at")
    private OffsetDateTime nextDeliveryAt;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "related_route", length = 255)
    private String relatedRoute;

    @Column(name = "parent_notification_id")
    private UUID parentNotificationId;

    @Column(name = "escalation_level", nullable = false)
    private int escalationLevel;

    public NotificationEntity() {}

    public NotificationEntity(
        UUID id,
        UUID ruleId,
        UUID eventId,
        String eventType,
        NotificationChannel channel,
        String recipient,
        NotificationSeverity severity,
        String title,
        String message,
        UUID templateId,
        Integer templateVersion,
        NotificationStatus status,
        OffsetDateTime nextDeliveryAt,
        OffsetDateTime createdAt,
        OffsetDateTime sentAt,
        OffsetDateTime readAt,
        String failureReason,
        String relatedRoute,
        UUID parentNotificationId,
        int escalationLevel
    ) {
        this.id = id;
        this.ruleId = ruleId;
        this.eventId = eventId;
        this.eventType = eventType;
        this.channel = channel;
        this.recipient = recipient;
        this.severity = severity;
        this.title = title;
        this.message = message;
        this.templateId = templateId;
        this.templateVersion = templateVersion;
        this.status = status;
        this.nextDeliveryAt = nextDeliveryAt;
        this.createdAt = createdAt;
        this.sentAt = sentAt;
        this.readAt = readAt;
        this.failureReason = failureReason;
        this.relatedRoute = relatedRoute;
        this.parentNotificationId = parentNotificationId;
        this.escalationLevel = escalationLevel;
    }

    public static NotificationEntity fromDomain(Notification notification) {
        return new NotificationEntity(
            notification.id(),
            notification.ruleId(),
            notification.eventId(),
            notification.eventType(),
            notification.channel(),
            notification.recipient(),
            notification.severity(),
            notification.title(),
            notification.message(),
            notification.templateId(),
            notification.templateVersion(),
            notification.status(),
            notification.nextDeliveryAt(),
            notification.createdAt(),
            notification.sentAt(),
            notification.readAt(),
            notification.failureReason(),
            notification.relatedRoute(),
            notification.parentNotificationId(),
            notification.escalationLevel()
        );
    }

    public Notification toDomain() {
        return new Notification(
            id,
            ruleId,
            eventId,
            eventType,
            channel,
            recipient,
            severity,
            title,
            message,
            templateId,
            templateVersion,
            status,
            nextDeliveryAt,
            createdAt,
            sentAt,
            readAt,
            failureReason,
            relatedRoute,
            parentNotificationId,
            escalationLevel
        );
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getRuleId() { return ruleId; }
    public void setRuleId(UUID ruleId) { this.ruleId = ruleId; }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public NotificationChannel getChannel() { return channel; }
    public void setChannel(NotificationChannel channel) { this.channel = channel; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public NotificationSeverity getSeverity() { return severity; }
    public void setSeverity(NotificationSeverity severity) { this.severity = severity; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public UUID getTemplateId() { return templateId; }
    public void setTemplateId(UUID templateId) { this.templateId = templateId; }

    public Integer getTemplateVersion() { return templateVersion; }
    public void setTemplateVersion(Integer templateVersion) { this.templateVersion = templateVersion; }

    public NotificationStatus getStatus() { return status; }
    public void setStatus(NotificationStatus status) { this.status = status; }

    public OffsetDateTime getNextDeliveryAt() { return nextDeliveryAt; }
    public void setNextDeliveryAt(OffsetDateTime nextDeliveryAt) { this.nextDeliveryAt = nextDeliveryAt; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getSentAt() { return sentAt; }
    public void setSentAt(OffsetDateTime sentAt) { this.sentAt = sentAt; }

    public OffsetDateTime getReadAt() { return readAt; }
    public void setReadAt(OffsetDateTime readAt) { this.readAt = readAt; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public String getRelatedRoute() { return relatedRoute; }
    public void setRelatedRoute(String relatedRoute) { this.relatedRoute = relatedRoute; }
    public UUID getParentNotificationId() { return parentNotificationId; }
    public void setParentNotificationId(UUID parentNotificationId) { this.parentNotificationId = parentNotificationId; }
    public int getEscalationLevel() { return escalationLevel; }
    public void setEscalationLevel(int escalationLevel) { this.escalationLevel = escalationLevel; }
}
