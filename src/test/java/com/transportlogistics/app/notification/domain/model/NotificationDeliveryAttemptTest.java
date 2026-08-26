package com.transportlogistics.app.notification.domain.model;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class NotificationDeliveryAttemptTest {
    private final OffsetDateTime now = OffsetDateTime.parse("2026-08-21T10:00:00Z");

    @Test
    void attemptLifecycleAndStableIdempotencyKey() {
        UUID notificationId = UUID.randomUUID();
        var attempt = NotificationDeliveryAttempt.start(notificationId, 1, now, now);
        assertThat(attempt.state()).isEqualTo(NotificationDeliveryAttemptState.IN_PROGRESS);
        assertThat(attempt.idempotencyKey()).isEqualTo(notificationId + ":1");
        assertThat(attempt.restart(now.plusMinutes(6)).idempotencyKey()).isEqualTo(attempt.idempotencyKey());
        assertThat(attempt.succeed(now.plusSeconds(1), "provider-1").providerMessageId()).isEqualTo("provider-1");
    }

    @Test
    void failureDiagnosticsAreSanitizedAndBounded() {
        var failed = NotificationDeliveryAttempt.start(UUID.randomUUID(), 1, now, now)
            .fail(now.plusSeconds(1), EmailDeliveryErrorCategory.HTTP_429, "RATE\r\nLIMIT", "x\n".repeat(400));
        assertThat(failed.errorCode()).doesNotContain("\r", "\n");
        assertThat(failed.errorMessage()).hasSize(500).doesNotContain("\n");
    }

    @Test
    void frozenRetryPolicyAndTypedClassification() {
        assertThat(NotificationEmailRetryPolicy.MAX_ATTEMPTS).isEqualTo(3);
        assertThat(NotificationEmailRetryPolicy.delayAfterFailure(1)).isEqualTo(java.time.Duration.ofMinutes(1));
        assertThat(NotificationEmailRetryPolicy.delayAfterFailure(2)).isEqualTo(java.time.Duration.ofMinutes(2));
        assertThat(EmailDeliveryErrorCategory.HTTP_408.retryable()).isTrue();
        assertThat(EmailDeliveryErrorCategory.HTTP_429.retryable()).isTrue();
        assertThat(EmailDeliveryErrorCategory.PROVIDER_5XX.retryable()).isTrue();
        assertThat(EmailDeliveryErrorCategory.INTERRUPTION.retryable()).isTrue();
        assertThat(EmailDeliveryErrorCategory.PROVIDER_4XX.retryable()).isFalse();
        assertThat(EmailDeliveryErrorCategory.AUTHENTICATION.retryable()).isFalse();
        assertThat(EmailDeliveryErrorCategory.CONFIGURATION.retryable()).isFalse();
        assertThat(EmailDeliveryErrorCategory.INVALID_RECIPIENT.retryable()).isFalse();
    }
}
