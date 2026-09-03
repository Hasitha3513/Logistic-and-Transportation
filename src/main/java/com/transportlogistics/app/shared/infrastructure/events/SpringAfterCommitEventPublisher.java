package com.transportlogistics.app.shared.infrastructure.events;

import com.transportlogistics.app.shared.AfterCommitEventPublisher;
import com.transportlogistics.app.shared.DurableEventEnvelope;
import com.transportlogistics.app.shared.DurableEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Objects;

/**
 * Publishes local secondary-reaction events only after an active transaction commits.
 */
@Component
public final class SpringAfterCommitEventPublisher implements AfterCommitEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(SpringAfterCommitEventPublisher.class);
    private static final ThreadLocal<Boolean> PUBLISHING_AFTER_COMMIT =
        ThreadLocal.withInitial(() -> Boolean.FALSE);

    private final ApplicationEventPublisher publisher;
    private final DurableEventPublisher durablePublisher;

    @Autowired
    public SpringAfterCommitEventPublisher(ApplicationEventPublisher publisher,
                                           DurableEventPublisher durablePublisher) {
        this.publisher = Objects.requireNonNull(publisher);
        this.durablePublisher = Objects.requireNonNull(durablePublisher);
    }

    /** Test-only compatibility constructor for isolated local publication tests. */
    public SpringAfterCommitEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = Objects.requireNonNull(publisher);
        this.durablePublisher = null;
    }

    @Override
    public void publish(Object event) {
        Objects.requireNonNull(event, "Event cannot be null");
        if (event instanceof DurableEventEnvelope durableEvent && durablePublisher != null) {
            durablePublisher.publish(durableEvent);
            return;
        }
        if (Boolean.TRUE.equals(PUBLISHING_AFTER_COMMIT.get())) {
            publisher.publishEvent(event);
            return;
        }
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishWithoutAffectingCommittedState(event);
                }
            });
            return;
        }
        publisher.publishEvent(event);
    }

    private void publishWithoutAffectingCommittedState(Object event) {
        PUBLISHING_AFTER_COMMIT.set(Boolean.TRUE);
        try {
            publisher.publishEvent(event);
        } catch (RuntimeException exception) {
            log.error("After-commit event consumer failed for {} without affecting committed state",
                    event.getClass().getName(), exception);
        } finally {
            PUBLISHING_AFTER_COMMIT.remove();
        }
    }
}
