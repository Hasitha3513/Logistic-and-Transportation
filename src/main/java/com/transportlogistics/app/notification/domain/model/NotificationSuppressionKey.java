package com.transportlogistics.app.notification.domain.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record NotificationSuppressionKey(String value) {
    private static final String NO_MILESTONE = "<no-milestone>";

    public NotificationSuppressionKey {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Suppression key must be a SHA-256 value");
        }
    }

    public static NotificationSuppressionKey of(UUID ruleId,
                                                String eventType,
                                                String aggregateType,
                                                UUID aggregateId,
                                                String recipient,
                                                NotificationChannel channel,
                                                String milestone) {
        String canonical = String.join("|",
            Objects.requireNonNull(ruleId, "ruleId must not be null").toString(),
            normalize(eventType),
            normalize(aggregateType),
            Objects.requireNonNull(aggregateId, "aggregateId must not be null").toString(),
            normalize(recipient),
            Objects.requireNonNull(channel, "channel must not be null").name(),
            milestone == null || milestone.isBlank() ? NO_MILESTONE : normalize(milestone));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return new NotificationSuppressionKey(HexFormat.of().formatHex(digest));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "<empty>";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
