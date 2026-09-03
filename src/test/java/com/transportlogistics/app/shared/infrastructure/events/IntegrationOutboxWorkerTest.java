package com.transportlogistics.app.shared.infrastructure.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.shared.DurableEventHandler;
import com.transportlogistics.app.shared.PermanentEventFailureException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IntegrationOutboxWorkerTest {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-03T10:00:00Z");
    private final IntegrationOutboxTransactions transactions = mock(IntegrationOutboxTransactions.class);
    private final DurableEventHandler handler = mock(DurableEventHandler.class);

    @Test
    void deliversAndMarksPublished() {
        when(handler.consumerName()).thenReturn("consumer");
        var event = claimed(1, 1, "{\"safe\":\"value\"}");
        when(transactions.claim(NOW, IntegrationOutboxWorker.BATCH_SIZE)).thenReturn(List.of(event));

        worker(handler).processDue();

        verify(handler).handle(any());
        verify(transactions).published(event.id(), NOW);
    }

    @Test
    void retriesTransientFailureWithBoundedBackoffThenTerminates() {
        when(handler.consumerName()).thenReturn("consumer");
        doThrow(new IllegalStateException("temporary database outage")).when(handler).handle(any());
        var retry = claimed(1, 1, "{}");
        when(transactions.claim(NOW, IntegrationOutboxWorker.BATCH_SIZE)).thenReturn(List.of(retry));

        worker(handler).processDue();

        verify(transactions).retry(eq(retry.id()), eq(NOW),
            eq(NOW.plus(IntegrationOutboxWorker.backoff(1))), eq("ILLEGALSTATEEXCEPTION"));

        var exhausted = claimed(5, 1, "{}");
        when(transactions.claim(NOW, IntegrationOutboxWorker.BATCH_SIZE)).thenReturn(List.of(exhausted));
        worker(handler).processDue();
        verify(transactions).failed(exhausted.id(), NOW, "RETRY_EXHAUSTED_ILLEGALSTATEEXCEPTION", false);
        assertThat(IntegrationOutboxWorker.backoff(99)).isEqualTo(java.time.Duration.ofMinutes(15));
    }

    @Test
    void parksUnsupportedVersionInvalidPayloadAndPermanentFailureWithoutRetry() {
        when(handler.consumerName()).thenReturn("consumer");
        var unsupported = claimed(1, 2, "{}");
        when(transactions.claim(NOW, IntegrationOutboxWorker.BATCH_SIZE)).thenReturn(List.of(unsupported));
        worker(handler).processDue();
        verify(transactions).failed(unsupported.id(), NOW, "UNSUPPORTED_VERSION", true);
        verify(handler, never()).handle(any());

        var invalid = claimed(1, 1, "not-json");
        when(transactions.claim(NOW, IntegrationOutboxWorker.BATCH_SIZE)).thenReturn(List.of(invalid));
        worker(handler).processDue();
        verify(transactions).failed(invalid.id(), NOW, "INVALID_PAYLOAD", false);

        var permanent = claimed(1, 1, "{}");
        doThrow(new PermanentEventFailureException("INVALID_EVENT", "invalid")).when(handler).handle(any());
        when(transactions.claim(NOW, IntegrationOutboxWorker.BATCH_SIZE)).thenReturn(List.of(permanent));
        worker(handler).processDue();
        verify(transactions).failed(permanent.id(), NOW, "INVALID_EVENT", false);
    }

    private IntegrationOutboxWorker worker(DurableEventHandler durableHandler) {
        return new IntegrationOutboxWorker(transactions, new ObjectMapper(), List.of(durableHandler),
            Clock.fixed(Instant.parse("2026-09-03T10:00:00Z"), ZoneOffset.UTC));
    }

    private ClaimedOutboxEvent claimed(int attempt, int version, String payload) {
        return new ClaimedOutboxEvent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "consumer",
            "EVENT", version, "AGGREGATE", UUID.randomUUID(), payload, NOW.minusMinutes(1), attempt);
    }
}
