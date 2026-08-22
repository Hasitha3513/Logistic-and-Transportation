package com.transportlogistics.app.notification.domain.model;

import java.time.Duration;

public final class NotificationEmailRetryPolicy {
    public static final int MAX_ATTEMPTS = 3;

    private NotificationEmailRetryPolicy() {}

    public static Duration delayAfterFailure(int failedAttemptNumber) {
        return switch (failedAttemptNumber) {
            case 1 -> Duration.ofMinutes(1);
            case 2 -> Duration.ofMinutes(2);
            default -> throw new IllegalArgumentException("No retry follows attempt " + failedAttemptNumber);
        };
    }
}
