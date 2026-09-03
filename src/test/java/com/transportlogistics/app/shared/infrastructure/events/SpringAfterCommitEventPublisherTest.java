package com.transportlogistics.app.shared.infrastructure.events;

import com.transportlogistics.app.delivery.DeliveryCustomerNotificationEvent;
import com.transportlogistics.app.shared.DurableEventPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doAnswer;
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
    void nestedEventPublishesDuringAfterCommitInsteadOfBeingQueuedTooLate() {
        Object sourceEvent = new Object();
        Object nestedEvent = new Object();
        beginTransactionSynchronization();
        doAnswer(invocation -> {
            publisher.publish(nestedEvent);
            return null;
        }).when(delegate).publishEvent(sourceEvent);
        publisher.publish(sourceEvent);

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        verify(delegate).publishEvent(sourceEvent);
        verify(delegate).publishEvent(nestedEvent);
    }

    @Test
    void publishesImmediatelyWhenThereIsNoTransaction() {
        Object event = new Object();

        publisher.publish(event);

        verify(delegate).publishEvent(event);
    }

    @Test
    void durableEventsAreStoredInsideTheCallerTransactionInsteadOfPublishedInMemory() {
        DurableEventPublisher durable = mock(DurableEventPublisher.class);
        var transactionAware = new SpringAfterCommitEventPublisher(delegate, durable);
        var event = new DeliveryCustomerNotificationEvent(UUID.randomUUID(), "DELIVERY_COMPLETED",
            UUID.randomUUID(), OffsetDateTime.parse("2026-09-03T10:00:00Z"), 1, "DELIVERY_ORDER",
            UUID.randomUUID(), Map.of("customerId", UUID.randomUUID().toString(), "deliveryNumber", "DEL-1",
                "actor", "system", "status", "DELIVERED", "completedAt", "2026-09-03T10:00:00Z"));
        beginTransactionSynchronization();

        transactionAware.publish(event);

        verify(durable).publish(event);
        verify(delegate, never()).publishEvent(event);
        assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
    }

    private void beginTransactionSynchronization() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
    }
}
