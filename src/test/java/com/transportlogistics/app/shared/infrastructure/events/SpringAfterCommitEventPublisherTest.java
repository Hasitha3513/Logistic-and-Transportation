package com.transportlogistics.app.shared.infrastructure.events;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SpringAfterCommitEventPublisherTest {
    private final ApplicationEventPublisher delegate = mock(ApplicationEventPublisher.class);
    private final SpringAfterCommitEventPublisher publisher = new SpringAfterCommitEventPublisher(delegate);

    @AfterEach
    void clearTransactionState() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void publishesOnlyAfterActiveTransactionCommits() {
        Object event = new Object();
        beginTransactionSynchronization();

        publisher.publish(event);

        verify(delegate, never()).publishEvent(event);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(delegate).publishEvent(event);
    }

    @Test
    void doesNotPublishWhenActiveTransactionRollsBack() {
        Object event = new Object();
        beginTransactionSynchronization();

        publisher.publish(event);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(delegate, never()).publishEvent(event);
    }

    @Test
    void consumerFailureCannotChangeAlreadyCommittedOutcome() {
        Object event = new Object();
        beginTransactionSynchronization();
        doThrow(new IllegalStateException("consumer failed")).when(delegate).publishEvent(event);
        publisher.publish(event);

        assertDoesNotThrow(() -> TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit));
        verify(delegate).publishEvent(event);
    }

    @Test
    void publishesImmediatelyWhenThereIsNoTransaction() {
        Object event = new Object();

        publisher.publish(event);

        verify(delegate).publishEvent(event);
    }

    private void beginTransactionSynchronization() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
    }
}
