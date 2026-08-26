package com.transportlogistics.app.notification.application.service;

import com.transportlogistics.app.notification.application.ports.out.NotificationRuleExecutionRepository;
import com.transportlogistics.app.notification.domain.model.*;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class NotificationSuppressionEvaluatorTest {
    private final NotificationRuleExecutionRepository repository = mock(NotificationRuleExecutionRepository.class);
    private final NotificationSuppressionEvaluator evaluator = new NotificationSuppressionEvaluator(repository);
    private final OffsetDateTime now = OffsetDateTime.parse("2026-08-21T10:15:00Z");

    @Test void zeroWindowIsNeverTimeSuppressed() {
        assertThat(evaluator.evaluate(policy(0), NotificationSeverity.WARNING, "a".repeat(64), now).suppressed()).isFalse();
        verifyNoInteractions(repository);
    }

    @Test void acceptedExecutionInsideWindowControlsSuppression() {
        NotificationRuleExecution execution = execution(now.minusMinutes(10));
        when(repository.findLatestAccepted(eq("a".repeat(64)), eq(now.minusMinutes(15))))
            .thenReturn(Optional.of(execution));
        var result = evaluator.evaluate(policy(15), NotificationSeverity.WARNING, "a".repeat(64), now);
        assertThat(result.suppressed()).isTrue();
        assertThat(result.controllingExecution()).isSameAs(execution);
    }

    @Test void noAcceptedExecutionAfterBoundaryIsAccepted() {
        when(repository.findLatestAccepted(any(), any())).thenReturn(Optional.empty());
        assertThat(evaluator.evaluate(policy(15), NotificationSeverity.WARNING, "a".repeat(64), now).suppressed()).isFalse();
    }

    @Test void criticalBypassesTimeSuppressionButLeavesEventIdempotencyToEngine() {
        assertThat(evaluator.evaluate(policy(1440), NotificationSeverity.CRITICAL, "a".repeat(64), now).suppressed()).isFalse();
        verifyNoInteractions(repository);
    }

    private NotificationRulePolicy policy(int window) {
        return new NotificationRulePolicy(false, null, null, Set.of(), window);
    }

    private NotificationRuleExecution execution(OffsetDateTime time) {
        return NotificationRuleExecution.completed(UUID.randomUUID(), "TRIP_DELAY_RECORDED", "Trip",
            UUID.randomUUID(), UUID.randomUUID(), "user1", NotificationChannel.IN_APP,
            NotificationRuleExecutionOutcome.ACCEPTED, "a".repeat(64), UUID.randomUUID(), null, null, time);
    }
}
