package com.transportlogistics.app.notification.domain.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public record NotificationTemplate(
    UUID id,
    String code,
    String name,
    String eventType,
    NotificationChannel channel,
    String subject,
    String body,
    int version,
    boolean active,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    public NotificationTemplate {
        Objects.requireNonNull(id, "Template ID must not be null");
        code = required(code, "Template code", 64).toUpperCase();
        name = required(name, "Template name", 128);
        eventType = required(eventType, "Template event type", 64).toUpperCase();
        Objects.requireNonNull(channel, "Template channel must not be null");
        subject = normalize(required(subject, "Template subject", 255));
        body = normalize(required(body, "Template body", 4000));
        if (version <= 0) {
            throw new IllegalArgumentException("Template version must be positive");
        }
        createdAt = Objects.requireNonNull(createdAt, "Template createdAt must not be null");
        updatedAt = Objects.requireNonNull(updatedAt, "Template updatedAt must not be null");

        var event = NotificationEventCatalogue.require(eventType);
        if (!event.templateCode().equals(code) || !event.supports(channel)) {
            throw new IllegalArgumentException("Template code, event, and channel are incompatible");
        }
        NotificationTemplateRenderer.validateTokens(event, subject);
        NotificationTemplateRenderer.validateTokens(event, body);
    }

    private static String required(String value, String label, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        rejectUnsafeControls(value, label);
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(label + " must not exceed " + maxLength + " characters");
        }
        return trimmed;
    }

    static String normalize(String value) {
        return value.replace("\r\n", "\n").replace('\r', '\n');
    }

    static void rejectUnsafeControls(String value, String label) {
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (Character.isISOControl(character) && character != '\n' && character != '\t' && character != '\r') {
                throw new IllegalArgumentException(label + " contains an unsafe control character");
            }
        }
    }
}
