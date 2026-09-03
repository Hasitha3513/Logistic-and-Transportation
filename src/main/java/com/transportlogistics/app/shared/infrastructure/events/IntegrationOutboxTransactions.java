package com.transportlogistics.app.shared.infrastructure.events;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Component
public class IntegrationOutboxTransactions {
    private static final Duration CLAIM_LEASE = Duration.ofMinutes(5);
    private final IntegrationOutboxJpaRepository events;

    public IntegrationOutboxTransactions(IntegrationOutboxJpaRepository events) {
        this.events = events;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<ClaimedOutboxEvent> claim(OffsetDateTime now, int batchSize) {
        events.findExpiredExhaustedClaims(now, IntegrationOutboxStatus.PROCESSING,
            IntegrationOutboxWorker.MAX_ATTEMPTS, PageRequest.of(0, batchSize))
            .forEach(event -> event.fail(now, "RETRY_EXHAUSTED_STALE_CLAIM", false));
        var claimable = events.findClaimable(now,
            EnumSet.of(IntegrationOutboxStatus.PENDING, IntegrationOutboxStatus.RETRY),
            IntegrationOutboxStatus.PROCESSING, IntegrationOutboxWorker.MAX_ATTEMPTS,
            PageRequest.of(0, batchSize));
        claimable.forEach(event -> event.claim(now, now.plus(CLAIM_LEASE)));
        return claimable.stream().map(this::snapshot).toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void published(UUID id, OffsetDateTime now) {
        events.findById(id).ifPresent(event -> event.publish(now));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void retry(UUID id, OffsetDateTime now, OffsetDateTime nextAttemptAt, String errorCode) {
        events.findById(id).ifPresent(event -> event.retry(now, nextAttemptAt, errorCode));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failed(UUID id, OffsetDateTime now, String errorCode, boolean unsupported) {
        events.findById(id).ifPresent(event -> event.fail(now, errorCode, unsupported));
    }

    private ClaimedOutboxEvent snapshot(IntegrationOutboxEventEntity event) {
        return new ClaimedOutboxEvent(event.id(), event.eventId(), event.tenantId(), event.consumerName(),
            event.eventType(), event.eventVersion(), event.aggregateType(), event.aggregateId(), event.payload(),
            event.occurredAt(), event.attemptCount());
    }
}
