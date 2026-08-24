package com.transportlogistics.app.notification.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationTest {

    @Test
    void createPending_andTransitionToSentAndRead_succeeds() {
        UUID ruleId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        Notification pending = Notification.createPending(
            ruleId,
            eventId,
            "TRIP_DELAY_RECORDED",
            NotificationChannel.IN_APP,
            "dispatcher1",
            NotificationSeverity.WARNING,
            "Trip Delay: TRP-001",
            "Trip delayed by 30 mins",
            "/trips/123"
        );

        assertThat(pending.status()).isEqualTo(NotificationStatus.PENDING);
        assertThat(pending.sentAt()).isNull();
        assertThat(pending.readAt()).isNull();

        Notification sent = pending.markSent();
        assertThat(sent.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(sent.sentAt()).isNotNull();
        assertThat(sent.readAt()).isNull();

        Notification read = sent.markRead();
        assertThat(read.status()).isEqualTo(NotificationStatus.READ);
        assertThat(read.readAt()).isNotNull();
    }

    @Test
    void createPending_andTransitionToFailed_succeeds() {
        Notification pending = Notification.createPending(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "EVENT",
            NotificationChannel.EMAIL,
            "admin@example.com",
            NotificationSeverity.CRITICAL,
            "Title",
            "Message",
            null
        );

        Notification failed = pending.markFailed("SMTP server unreachable");
        assertThat(failed.status()).isEqualTo(NotificationStatus.FAILED);
        assertThat(failed.failureReason()).isEqualTo("SMTP server unreachable");
    }

    @Test
    void invalidTransitions_throwIllegalStateException() {
        Notification pending = Notification.createPending(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "EVENT",
            NotificationChannel.IN_APP,
            "user",
            NotificationSeverity.INFO,
            "Title",
            "Message",
            null
        );

        // Cannot transition PENDING directly to READ
        assertThatThrownBy(pending::markRead)
            .isInstanceOf(IllegalStateException.class);

        Notification sent = pending.markSent();
        // Cannot transition SENT to FAILED
        assertThatThrownBy(() -> sent.markFailed("Error"))
            .isInstanceOf(IllegalStateException.class);
    }
}
