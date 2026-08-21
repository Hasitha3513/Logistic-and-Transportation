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
    String templateCode,
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
        templateCode = templateCode == null || templateCode.isBlank() ? null : templateCode.trim().toUpperCase();
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
        String templateCode,
        boolean enabled,
        NotificationSeverity severityThreshold
    ) {
        validateCatalogueConfiguration(eventType, channel, templateCode);
        OffsetDateTime now = OffsetDateTime.now();
        return new NotificationRule(
            UUID.randomUUID(),
            name,
            description,
            eventType,
            channel,
            recipientType,
            recipientValue,
            templateCode,
            enabled,
            severityThreshold != null ? severityThreshold : NotificationSeverity.INFO,
            now,
            now
        );
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
        String defaultTemplate = NotificationEventCatalogue.require(eventType).templateCode();
        return create(name, description, eventType, channel, recipientType, recipientValue,
            defaultTemplate, enabled, severityThreshold);
    }

    public NotificationRule update(
        String name,
        String description,
        String eventType,
        NotificationChannel channel,
        RecipientType recipientType,
        String recipientValue,
        String templateCode,
        boolean enabled,
        NotificationSeverity severityThreshold
    ) {
        validateCatalogueConfiguration(eventType, channel, templateCode);
        return new NotificationRule(
            this.id,
            name,
            description,
            eventType,
            channel,
            recipientType,
            recipientValue,
            templateCode,
            enabled,
            severityThreshold != null ? severityThreshold : NotificationSeverity.INFO,
            this.createdAt,
            OffsetDateTime.now()
        );
    }

    public NotificationRule withEnabled(boolean enabled) {
        if (enabled) {
            validateCatalogueConfiguration(this.eventType, this.channel, this.templateCode);
        }
        return new NotificationRule(
            this.id,
            this.name,
            this.description,
            this.eventType,
            this.channel,
            this.recipientType,
            this.recipientValue,
            this.templateCode,
            enabled,
            this.severityThreshold,
            this.createdAt,
            OffsetDateTime.now()
        );
    }

    private static void validateCatalogueConfiguration(String eventType, NotificationChannel channel, String templateCode) {
        NotificationEventDefinition definition = NotificationEventCatalogue.require(eventType);
        if (!definition.supports(channel)) {
            throw new IllegalArgumentException("Notification event does not support channel " + channel);
        }
        if (templateCode == null || !definition.templateCode().equalsIgnoreCase(templateCode.trim())) {
            throw new IllegalArgumentException("Template is incompatible with notification event and channel");
        }
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
