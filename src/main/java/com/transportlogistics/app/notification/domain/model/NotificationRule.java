package com.transportlogistics.app.notification.domain.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public record NotificationRule(
    UUID id,
    String name,
    String description,
    String eventType,
    NotificationChannel channel,
    RecipientType recipientType,
    String recipientValue,
    boolean enabled,
    NotificationSeverity severityThreshold,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public NotificationRule {
        Objects.requireNonNull(id, "Rule ID must not be null");
        if (name == null || name.trim().isBlank()) {
            throw new IllegalArgumentException("Rule name must not be blank");
        }
        if (eventType == null || eventType.trim().isBlank()) {
            throw new IllegalArgumentException("Event type must not be blank");
        }
        Objects.requireNonNull(channel, "Notification channel must not be null");
        Objects.requireNonNull(recipientType, "Recipient type must not be null");
        if (recipientValue == null || recipientValue.trim().isBlank()) {
            throw new IllegalArgumentException("Recipient value must not be blank");
        }
        if (recipientType == RecipientType.EMAIL_ADDRESS && !EMAIL_PATTERN.matcher(recipientValue.trim()).matches()) {
            throw new IllegalArgumentException("Invalid email address for EMAIL_ADDRESS recipient type: " + recipientValue);
        }
        if (severityThreshold == null) {
            severityThreshold = NotificationSeverity.INFO;
        }
        name = name.trim();
        eventType = eventType.trim().toUpperCase();
        recipientValue = recipientValue.trim();
        description = description != null ? description.trim() : null;
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    public static NotificationRule create(
        String name,
        String description,
        String eventType,
        NotificationChannel channel,
        RecipientType recipientType,
        String recipientValue,
        boolean enabled,
        NotificationSeverity severityThreshold
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        return new NotificationRule(
            UUID.randomUUID(),
            name,
            description,
            eventType,
            channel,
            recipientType,
            recipientValue,
            enabled,
            severityThreshold != null ? severityThreshold : NotificationSeverity.INFO,
            now,
            now
        );
    }

    public NotificationRule update(
        String name,
        String description,
        String eventType,
        NotificationChannel channel,
        RecipientType recipientType,
        String recipientValue,
        boolean enabled,
        NotificationSeverity severityThreshold
    ) {
        return new NotificationRule(
            this.id,
            name,
            description,
            eventType,
            channel,
            recipientType,
            recipientValue,
            enabled,
            severityThreshold != null ? severityThreshold : NotificationSeverity.INFO,
            this.createdAt,
            OffsetDateTime.now()
        );
    }

    public NotificationRule withEnabled(boolean enabled) {
        return new NotificationRule(
            this.id,
            this.name,
            this.description,
            this.eventType,
            this.channel,
            this.recipientType,
            this.recipientValue,
            enabled,
            this.severityThreshold,
            this.createdAt,
            OffsetDateTime.now()
        );
    }

    public boolean matchesEvent(String eventType, NotificationSeverity eventSeverity) {
        if (!this.enabled) {
            return false;
        }
        if (eventType == null || !this.eventType.equalsIgnoreCase(eventType.trim())) {
            return false;
        }
        if (eventSeverity != null && !eventSeverity.meetsThreshold(this.severityThreshold)) {
            return false;
        }
        return true;
    }
}
