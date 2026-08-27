package com.transportlogistics.app.notification.domain.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public record NotificationDeliveryAttempt(
    UUID id,
    UUID notificationId,
    int attemptNumber,
    NotificationDeliveryAttemptState state,
    OffsetDateTime dueAt,
    OffsetDateTime startedAt,
    OffsetDateTime completedAt,
    EmailDeliveryErrorCategory errorCategory,
    String errorCode,
    String errorMessage,
    String providerMessageId,
    OffsetDateTime createdAt
) {
    public NotificationDeliveryAttempt {
        Objects.requireNonNull(id, "Attempt ID must not be null");
        Objects.requireNonNull(notificationId, "Notification ID must not be null");
        if (attemptNumber < 1 || attemptNumber > 3) {
            throw new IllegalArgumentException("Attempt number must be between 1 and 3");
        }
        Objects.requireNonNull(state, "Attempt state must not be null");
        Objects.requireNonNull(dueAt, "Attempt due time must not be null");
        Objects.requireNonNull(createdAt, "Attempt creation time must not be null");
        errorCode = sanitize(errorCode, 64);
        errorMessage = sanitize(errorMessage, 500);
        providerMessageId = sanitize(providerMessageId, 255);
    }

    public static NotificationDeliveryAttempt start(UUID notificationId, int attemptNumber,
                                                     OffsetDateTime dueAt, OffsetDateTime startedAt) {
        return new NotificationDeliveryAttempt(UUID.randomUUID(), notificationId, attemptNumber,
            NotificationDeliveryAttemptState.IN_PROGRESS, dueAt, startedAt, null,
            null, null, null, null, startedAt);
    }

    public NotificationDeliveryAttempt restart(OffsetDateTime startedAt) {
        return new NotificationDeliveryAttempt(id, notificationId, attemptNumber,
            NotificationDeliveryAttemptState.IN_PROGRESS, dueAt, startedAt, null,
            null, null, null, null, createdAt);
    }

    public NotificationDeliveryAttempt succeed(OffsetDateTime completedAt, String providerMessageId) {
        return new NotificationDeliveryAttempt(id, notificationId, attemptNumber,
            NotificationDeliveryAttemptState.SUCCEEDED, dueAt, startedAt, completedAt,
            null, null, null, providerMessageId, createdAt);
    }

    public NotificationDeliveryAttempt fail(OffsetDateTime completedAt, EmailDeliveryErrorCategory category,
                                             String code, String message) {
        return new NotificationDeliveryAttempt(id, notificationId, attemptNumber,
            NotificationDeliveryAttemptState.FAILED, dueAt, startedAt, completedAt,
            Objects.requireNonNull(category), code, message, null, createdAt);
    }

    public String idempotencyKey() {
        return notificationId + ":" + attemptNumber;
    }

    private static String sanitize(String value, int maxLength) {
        if (value == null) return null;
        String sanitized = value.replaceAll("[\\r\\n\\p{Cntrl}]", " ").trim();
        return sanitized.substring(0, Math.min(sanitized.length(), maxLength));
    }
}
