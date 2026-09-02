package com.transportlogistics.app.shared.infrastructure.events;

import com.transportlogistics.app.shared.AfterCommitEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final ApplicationEventPublisher publisher;

    public SpringAfterCommitEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = Objects.requireNonNull(publisher);
    }

    @Override
    public void publish(Object event) {
        Objects.requireNonNull(event, "Event cannot be null");
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
        try {
            publisher.publishEvent(event);
        } catch (RuntimeException exception) {
            log.error("After-commit event consumer failed for {} without affecting committed state",
                    event.getClass().getName(), exception);
        }
    }
}
