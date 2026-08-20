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
    NotificationStatus status,
    OffsetDateTime createdAt,
    OffsetDateTime sentAt,
    OffsetDateTime readAt,
    String failureReason,
    String relatedRoute
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
        if (status == null) {
            status = NotificationStatus.PENDING;
        }
        eventType = eventType.trim().toUpperCase();
        recipient = recipient.trim();
        title = title.trim();
        message = message.trim();
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
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
            NotificationStatus.PENDING,
            OffsetDateTime.now(),
            null,
            null,
            null,
            relatedRoute
        );
    }

    public Notification markSent() {
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
            NotificationStatus.SENT,
            this.createdAt,
            OffsetDateTime.now(),
            this.readAt,
            null,
            this.relatedRoute
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
            NotificationStatus.FAILED,
            this.createdAt,
            this.sentAt,
            null,
            failureReason != null ? failureReason.substring(0, Math.min(failureReason.length(), 500)) : "Unknown delivery error",
            this.relatedRoute
        );
    }

    public Notification markRead() {
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
            NotificationStatus.READ,
            this.createdAt,
            this.sentAt,
            OffsetDateTime.now(),
            this.failureReason,
            this.relatedRoute
        );
    }
}
