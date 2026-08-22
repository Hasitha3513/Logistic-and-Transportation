package com.transportlogistics.app.notification.domain.model;

import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class NotificationRuleExecutionTest {
    @Test void sanitizesFailureMessagesAndRecordsTimestamps() {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-21T10:00:00Z");
        var execution = NotificationRuleExecution.completed(UUID.randomUUID(), "TRIP_DELAY_RECORDED", "Trip",
            UUID.randomUUID(), UUID.randomUUID(), null, NotificationChannel.IN_APP,
            NotificationRuleExecutionOutcome.FAILED, null, null, "RENDER_FAILED",
            "unsafe\nmessage\u0000", now);
        assertThat(execution.failureMessage()).isEqualTo("unsafe message");
        assertThat(execution.createdAt()).isEqualTo(now);
        assertThat(execution.completedAt()).isEqualTo(now);
    }
}
