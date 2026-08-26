package com.transportlogistics.app.notification.domain.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.Locale;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public record NotificationRuleExecution(
    UUID id,
    String executionKey,
    UUID eventId,
    String eventType,
    String aggregateType,
    UUID aggregateId,
    UUID ruleId,
    String resolvedRecipient,
    NotificationChannel channel,
    NotificationRuleExecutionOutcome outcome,
    String suppressionKey,
    UUID controllingNotificationId,
    String failureCode,
    String failureMessage,
    OffsetDateTime createdAt,
    OffsetDateTime completedAt
) {
    public NotificationRuleExecution {
        Objects.requireNonNull(id, "Execution ID must not be null");
        executionKey = required(executionKey, "Execution key", 64);
        Objects.requireNonNull(eventId, "Event ID must not be null");
        eventType = required(eventType, "Event type", 64).toUpperCase();
        aggregateType = required(aggregateType, "Aggregate type", 64);
        Objects.requireNonNull(aggregateId, "Aggregate ID must not be null");
        Objects.requireNonNull(ruleId, "Rule ID must not be null");
        resolvedRecipient = optional(resolvedRecipient, 320);
        Objects.requireNonNull(channel, "Channel must not be null");
        Objects.requireNonNull(outcome, "Outcome must not be null");
        suppressionKey = optional(suppressionKey, 64);
        failureCode = optional(failureCode, 64);
        failureMessage = sanitize(failureMessage);
        Objects.requireNonNull(createdAt, "Created time must not be null");
        Objects.requireNonNull(completedAt, "Completed time must not be null");
    }

    public static NotificationRuleExecution completed(UUID eventId,
                                                      String eventType,
                                                      String aggregateType,
                                                      UUID aggregateId,
                                                      UUID ruleId,
                                                      String recipient,
                                                      NotificationChannel channel,
                                                      NotificationRuleExecutionOutcome outcome,
                                                      String suppressionKey,
                                                      UUID controllingNotificationId,
                                                      String failureCode,
                                                      String failureMessage,
                                                      OffsetDateTime now) {
        return new NotificationRuleExecution(UUID.randomUUID(), executionKey(eventId, ruleId, channel, recipient),
            eventId, eventType, aggregateType, aggregateId, ruleId, recipient, channel, outcome,
            suppressionKey, controllingNotificationId, failureCode, failureMessage, now, now);
    }

    public static String executionKey(UUID eventId, UUID ruleId, NotificationChannel channel, String recipient) {
        String target = recipient == null || recipient.isBlank() ? "<none>" : recipient.trim().toLowerCase(Locale.ROOT);
        String canonical = eventId + "|" + ruleId + "|" + channel.name() + "|" + target;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String required(String value, String label, int maximum) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        String normalized = value.trim();
        if (normalized.length() > maximum) {
            throw new IllegalArgumentException(label + " must not exceed " + maximum + " characters");
        }
        return normalized;
    }

    private static String optional(String value, int maximum) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return normalized.substring(0, Math.min(maximum, normalized.length()));
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        StringBuilder sanitized = new StringBuilder();
        for (int index = 0; index < value.length() && sanitized.length() < 500; index++) {
            char character = value.charAt(index);
            if (!Character.isISOControl(character) || character == '\t') {
                sanitized.append(character);
            } else if (character == '\n' || character == '\r') {
                sanitized.append(' ');
            }
        }
        return sanitized.toString().trim();
    }
}
