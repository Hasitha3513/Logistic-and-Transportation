package com.transportlogistics.app.notification;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record OperationalNotificationEvent(
    UUID eventId,
    String eventType,
    String aggregateType,
    UUID aggregateId,
    Severity severity,
    String title,
    String message,
    OffsetDateTime occurredAt,
    Map<String, String> metadata,
    UUID tenantId,
    int schemaVersion
) {
    public OperationalNotificationEvent(UUID eventId, String eventType, String aggregateType, UUID aggregateId,
                                        Severity severity, String title, String message,
                                        OffsetDateTime occurredAt, Map<String, String> metadata) {
        this(eventId, eventType, aggregateType, aggregateId, severity, title, message, occurredAt, metadata, null, 1);
    }

    public OperationalNotificationEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (eventType == null || eventType.trim().isBlank()) {
            throw new IllegalArgumentException("Event type must not be blank");
        }
        if (severity == null) {
            severity = Severity.INFO;
        }
        if (title == null || title.trim().isBlank()) {
            throw new IllegalArgumentException("Title must not be blank");
        }
        if (message == null || message.trim().isBlank()) {
            throw new IllegalArgumentException("Message must not be blank");
        }
        if (occurredAt == null) {
            occurredAt = OffsetDateTime.now();
        }
        eventType = eventType.trim().toUpperCase();
        title = title.trim();
        message = message.trim();
        metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("Schema version must be positive");
        }
    }

    public static OperationalNotificationEvent of(
        String eventType,
        String aggregateType,
        UUID aggregateId,
        Severity severity,
        String title,
        String message,
        Map<String, String> metadata
    ) {
        return new OperationalNotificationEvent(
            UUID.randomUUID(),
            eventType,
            aggregateType,
            aggregateId,
            severity,
            title,
            message,
            OffsetDateTime.now(),
            metadata
        );
    }

    public OperationalNotificationEvent withTenantId(UUID resolvedTenantId) {
        return new OperationalNotificationEvent(eventId, eventType, aggregateType, aggregateId, severity, title,
                message, occurredAt, metadata, java.util.Objects.requireNonNull(resolvedTenantId), schemaVersion);
    }

    public enum Severity {
        INFO,
        WARNING,
        CRITICAL
    }
}
