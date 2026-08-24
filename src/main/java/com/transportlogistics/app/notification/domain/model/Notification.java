package com.transportlogistics.app.notification.domain.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public record Notification(
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
    public Notification {
        Objects.requireNonNull(id, "Notification ID must not be null");
        Objects.requireNonNull(eventId, "Event ID must not be null");
        if (eventType == null || eventType.trim().isBlank()) {
            throw new IllegalArgumentException("Event type must not be blank");
        }
        Objects.requireNonNull(channel, "Channel must not be null");
        if (recipient == null || recipient.trim().isBlank()) {
            throw new IllegalArgumentException("Recipient must not be blank");
        }
        Objects.requireNonNull(severity, "Severity must not be null");
        if (title == null || title.trim().isBlank()) {
            throw new IllegalArgumentException("Title must not be blank");
        }
        if (message == null || message.trim().isBlank()) {
            throw new IllegalArgumentException("Message must not be blank");
        }
        if ((templateId == null) != (templateVersion == null)) {
            throw new IllegalArgumentException("Template ID and version must both be present or both be absent");
        }
        if (templateVersion != null && templateVersion <= 0) {
            throw new IllegalArgumentException("Template version must be positive");
        }
        if (status == null) {
            status = NotificationStatus.PENDING;
        }
        if (nextDeliveryAt != null && status != NotificationStatus.PENDING) {
            throw new IllegalArgumentException("Only PENDING notifications may have a next delivery time");
        }
        if (escalationLevel < 0 || escalationLevel > 1) {
            throw new IllegalArgumentException("Escalation level must be 0 or 1");
        }
        if ((parentNotificationId == null) != (escalationLevel == 0)) {
            throw new IllegalArgumentException("Escalation parent and level must be consistent");
        }
        eventType = eventType.trim().toUpperCase();
        recipient = recipient.trim();
        title = title.trim();
        message = message.trim();
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public Notification(UUID id, UUID ruleId, UUID eventId, String eventType, NotificationChannel channel,
                        String recipient, NotificationSeverity severity, String title, String message,
                        UUID templateId, Integer templateVersion, NotificationStatus status,
                        OffsetDateTime nextDeliveryAt, OffsetDateTime createdAt, OffsetDateTime sentAt,
                        OffsetDateTime readAt, String failureReason, String relatedRoute) {
        this(id, ruleId, eventId, eventType, channel, recipient, severity, title, message, templateId,
            templateVersion, status, nextDeliveryAt, createdAt, sentAt, readAt, failureReason, relatedRoute,
            null, 0);
    }

    public static Notification createPending(
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
        String relatedRoute,
        OffsetDateTime createdAt,
        OffsetDateTime nextDeliveryAt
    ) {
        OffsetDateTime effectiveCreatedAt = createdAt == null ? OffsetDateTime.now() : createdAt;
        if (nextDeliveryAt != null && !nextDeliveryAt.isAfter(effectiveCreatedAt)) {
            throw new IllegalArgumentException("Next delivery time must be after notification creation");
        }
        return new Notification(
            UUID.randomUUID(),
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
            NotificationStatus.PENDING,
            nextDeliveryAt,
            effectiveCreatedAt,
            null,
            null,
            null,
            relatedRoute,
            null,
            0
        );
    }

    public static Notification createPending(
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
        String relatedRoute
    ) {
        return createPending(ruleId, eventId, eventType, channel, recipient, severity,
            title, message, templateId, templateVersion, relatedRoute, null, null);
    }

    public static Notification createPending(
        UUID ruleId,
        UUID eventId,
        String eventType,
        NotificationChannel channel,
        String recipient,
        NotificationSeverity severity,
        String title,
        String message,
        String relatedRoute
    ) {
        return createPending(ruleId, eventId, eventType, channel, recipient, severity,
            title, message, null, null, relatedRoute, null, null);
    }

    public Notification markSent() {
        return markSent(OffsetDateTime.now());
    }

    public Notification markSent(OffsetDateTime sentAt) {
        if (!this.status.canTransitionTo(NotificationStatus.SENT)) {
            throw new IllegalStateException("Cannot transition notification from " + this.status + " to SENT");
        }
        return new Notification(
            this.id,
            this.ruleId,
            this.eventId,
            this.eventType,
            this.channel,
            this.recipient,
            this.severity,
            this.title,
            this.message,
            this.templateId,
            this.templateVersion,
            NotificationStatus.SENT,
            null,
            this.createdAt,
            Objects.requireNonNull(sentAt),
            this.readAt,
            null,
            this.relatedRoute,
            this.parentNotificationId,
            this.escalationLevel
        );
    }

    public Notification markFailed(String failureReason) {
        if (!this.status.canTransitionTo(NotificationStatus.FAILED)) {
            throw new IllegalStateException("Cannot transition notification from " + this.status + " to FAILED");
        }
        return new Notification(
            this.id,
            this.ruleId,
            this.eventId,
            this.eventType,
            this.channel,
            this.recipient,
            this.severity,
            this.title,
            this.message,
            this.templateId,
            this.templateVersion,
            NotificationStatus.FAILED,
            null,
            this.createdAt,
            this.sentAt,
            null,
            failureReason != null ? failureReason.substring(0, Math.min(failureReason.length(), 500)) : "Unknown delivery error",
            this.relatedRoute,
            this.parentNotificationId,
            this.escalationLevel
        );
    }

    public Notification markRead() {
        if (channel != NotificationChannel.IN_APP) {
            throw new IllegalStateException("Only IN_APP notifications may be marked read");
        }
        if (!this.status.canTransitionTo(NotificationStatus.READ)) {
            throw new IllegalStateException("Cannot transition notification from " + this.status + " to READ");
        }
        return new Notification(
            this.id,
            this.ruleId,
            this.eventId,
            this.eventType,
            this.channel,
            this.recipient,
            this.severity,
            this.title,
            this.message,
            this.templateId,
            this.templateVersion,
            NotificationStatus.READ,
            null,
            this.createdAt,
            this.sentAt,
            OffsetDateTime.now(),
            this.failureReason,
            this.relatedRoute,
            this.parentNotificationId,
            this.escalationLevel
        );
    }

    public Notification scheduleRetry(OffsetDateTime nextDeliveryAt) {
        if (status != NotificationStatus.PENDING || channel != NotificationChannel.EMAIL) {
            throw new IllegalStateException("Only pending EMAIL notifications may be retried");
        }
        return new Notification(id, ruleId, eventId, eventType, channel, recipient, severity, title, message,
            templateId, templateVersion, status, Objects.requireNonNull(nextDeliveryAt), createdAt, sentAt,
            readAt, null, relatedRoute, parentNotificationId, escalationLevel);
    }

    public Notification recordEscalationDiagnostic(String diagnostic) {
        if (status != NotificationStatus.FAILED || escalationLevel != 0) {
            throw new IllegalStateException("Escalation diagnostics require an original failed notification");
        }
        String combined = (failureReason == null ? "Terminal EMAIL failure" : failureReason)
            + " [" + diagnostic + "]";
        return new Notification(id, ruleId, eventId, eventType, channel, recipient, severity, title, message,
            templateId, templateVersion, status, null, createdAt, sentAt, readAt,
            combined.substring(0, Math.min(combined.length(), 500)), relatedRoute, null, 0);
    }

    public static Notification createEscalation(Notification failedEmail, String recipient,
                                                String sanitizedFailureReason, OffsetDateTime createdAt) {
        if (failedEmail.channel != NotificationChannel.EMAIL || failedEmail.status != NotificationStatus.FAILED
            || failedEmail.escalationLevel != 0) {
            throw new IllegalArgumentException("Escalation requires an original failed EMAIL notification");
        }
        String failure = sanitizedFailureReason == null ? "delivery failed" : sanitizedFailureReason;
        String childTitle = "EMAIL delivery failed: " + failedEmail.title;
        String childMessage = "Notification " + failedEmail.id + " failed terminally (" + failure + "). " + failedEmail.message;
        return new Notification(UUID.randomUUID(), failedEmail.ruleId, failedEmail.eventId, failedEmail.eventType,
            NotificationChannel.IN_APP, recipient, failedEmail.severity,
            childTitle.substring(0, Math.min(childTitle.length(), 255)),
            childMessage.substring(0, Math.min(childMessage.length(), 4000)),
            null, null, NotificationStatus.SENT, null,
            createdAt, createdAt, null, null, failedEmail.relatedRoute, failedEmail.id, 1);
    }
}
