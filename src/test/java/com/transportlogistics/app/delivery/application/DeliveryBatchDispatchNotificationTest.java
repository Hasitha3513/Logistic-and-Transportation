package com.transportlogistics.app.delivery.application;

import com.transportlogistics.app.delivery.DeliveryCustomerNotificationEvent;
import com.transportlogistics.app.delivery.adapters.outbound.events.SpringDeliveryBatchEventPublisher;
import com.transportlogistics.app.delivery.domain.model.DeliveryBatch;
import com.transportlogistics.app.delivery.domain.model.DeliveryBatchCode;
import com.transportlogistics.app.delivery.domain.model.DeliveryBatchOrder;
import com.transportlogistics.app.delivery.domain.model.DeliveryBatchOrderStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryBatchStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryId;
import com.transportlogistics.app.delivery.domain.model.DeliveryNumber;
import com.transportlogistics.app.delivery.domain.model.DeliveryOrder;
import com.transportlogistics.app.delivery.domain.model.DeliveryPriority;
import com.transportlogistics.app.delivery.domain.model.DeliveryServiceType;
import com.transportlogistics.app.delivery.domain.model.DeliveryStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryWindow;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryZoneLookupPort;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryBatchCodeGenerator;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryBatchRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryOrderRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryOrderTransaction;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryRiderRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryTenantContextPort;
import com.transportlogistics.app.delivery.ports.outbound.DriverEligibilityPort;
import com.transportlogistics.app.shared.infrastructure.events.SpringAfterCommitEventPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeliveryBatchDispatchNotificationTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-03T01:00:00Z"), ZoneOffset.UTC);
    private static final OffsetDateTime NOW = OffsetDateTime.now(CLOCK);

    private final UUID tenantId = UUID.randomUUID();
    private final UUID zoneId = UUID.randomUUID();
    private final UUID riderId = UUID.randomUUID();
    private final DeliveryBatchRepository batches = mock(DeliveryBatchRepository.class);
    private final DeliveryOrderRepository orders = mock(DeliveryOrderRepository.class);
    private final DeliveryRiderRepository riders = mock(DeliveryRiderRepository.class);
    private final DriverEligibilityPort drivers = mock(DriverEligibilityPort.class);
    private final DeliveryZoneLookupPort zones = mock(DeliveryZoneLookupPort.class);
    private final DeliveryTenantContextPort tenants = mock(DeliveryTenantContextPort.class);
    private final DeliveryBatchCodeGenerator codes = mock(DeliveryBatchCodeGenerator.class);

    @AfterEach
    void clearTransactionState() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void readyCommitEmitsNoOutForDeliveryCustomerEvent() {
        DeliveryBatch draft = draftBatch();
        List<Object> observed = new ArrayList<>();
        prepareTenantAndSave(draft);
        when(batches.countActiveOrdersByBatchId(tenantId, draft.id())).thenReturn(1);

        DeliveryBatch result = service(committingTransaction(), observed).markReady(draft.id());

        assertThat(result.status()).isEqualTo(DeliveryBatchStatus.READY);
        assertThat(customerEvents(observed)).isEmpty();
        verify(batches, never()).findActiveOrderMembershipsByBatchId(tenantId, draft.id());
    }

    @Test
    void dispatchCommitEmitsExactlyOneCustomerEventPerActiveMemberAndExcludesRemovedMember() {
        DeliveryBatch assigned = assignedBatch();
        UUID firstOrderId = UUID.randomUUID();
        UUID secondOrderId = UUID.randomUUID();
        UUID removedOrderId = UUID.randomUUID();
        List<DeliveryBatchOrder> memberships = List.of(
            activeMember(assigned.id(), firstOrderId, 1),
            activeMember(assigned.id(), secondOrderId, 2),
            activeMember(assigned.id(), removedOrderId, 3).markRemoved(NOW, "system")
        );
        List<Object> observed = new ArrayList<>();
        prepareTenantAndSave(assigned);
        when(batches.findActiveOrderMembershipsByBatchId(tenantId, assigned.id())).thenAnswer(invocation ->
            memberships.stream().filter(member -> member.status() == DeliveryBatchOrderStatus.ACTIVE).toList());
        when(orders.findById(firstOrderId)).thenReturn(Optional.of(order(firstOrderId, "DEL-2026-910001")));
        when(orders.findById(secondOrderId)).thenReturn(Optional.of(order(secondOrderId, "DEL-2026-910002")));

        DeliveryBatch result = service(committingTransaction(), observed).dispatchBatch(assigned.id());

        assertThat(result.status()).isEqualTo(DeliveryBatchStatus.DISPATCHED);
        assertThat(customerEvents(observed))
            .hasSize(2)
            .extracting(DeliveryCustomerNotificationEvent::aggregateId)
            .containsExactlyInAnyOrder(firstOrderId, secondOrderId);
        assertThat(customerEvents(observed))
            .allSatisfy(event -> assertThat(event.eventType()).isEqualTo("DELIVERY_OUT_FOR_DELIVERY"));
        verify(orders, never()).findById(removedOrderId);
    }

    @Test
    void dispatchRollbackPublishesNoAfterCommitCustomerEvent() {
        DeliveryBatch assigned = assignedBatch();
        UUID orderId = UUID.randomUUID();
        List<Object> observed = new ArrayList<>();
        prepareTenantAndSave(assigned);
        when(batches.findActiveOrderMembershipsByBatchId(tenantId, assigned.id()))
            .thenReturn(List.of(activeMember(assigned.id(), orderId, 1)));
        when(orders.findById(orderId)).thenReturn(Optional.of(order(orderId, "DEL-2026-910003")));

        assertThatThrownBy(() -> service(rollingBackTransaction(), observed).dispatchBatch(assigned.id()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("controlled dispatch rollback");

        assertThat(observed).isEmpty();
    }

    private DeliveryBatchService service(DeliveryOrderTransaction transaction, List<Object> observed) {
        ApplicationEventPublisher applicationEvents = observed::add;
        var afterCommit = new SpringAfterCommitEventPublisher(applicationEvents);
        var eventPublisher = new SpringDeliveryBatchEventPublisher(afterCommit);
        return new DeliveryBatchService(batches, orders, riders, drivers, zones, tenants, transaction, codes,
            eventPublisher, CLOCK);
    }

    private void prepareTenantAndSave(DeliveryBatch batch) {
        when(tenants.currentTenantId()).thenReturn(Optional.of(tenantId));
        when(batches.findByIdForUpdate(tenantId, batch.id())).thenReturn(Optional.of(batch));
        when(batches.save(org.mockito.ArgumentMatchers.any(DeliveryBatch.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private DeliveryBatch draftBatch() {
        return DeliveryBatch.create(UUID.randomUUID(), tenantId, new DeliveryBatchCode("BAT-2026-910001"),
            zoneId, null, 5, NOW, "system");
    }

    private DeliveryBatch assignedBatch() {
        return draftBatch().markReady(3, NOW, "system").assignRider(riderId, NOW, "system");
    }

    private DeliveryBatchOrder activeMember(UUID batchId, UUID orderId, int sequence) {
        return DeliveryBatchOrder.create(UUID.randomUUID(), tenantId, batchId, orderId, sequence, NOW, "system");
    }

    private DeliveryOrder order(UUID orderId, String deliveryNumber) {
        return new DeliveryOrder(new DeliveryId(orderId), new DeliveryNumber(deliveryNumber), UUID.randomUUID(),
            UUID.randomUUID(), UUID.randomUUID(), DeliveryPriority.NORMAL, DeliveryServiceType.STANDARD,
            new DeliveryWindow(NOW, NOW.plusHours(2)), "Fixture", DeliveryStatus.READY_FOR_ASSIGNMENT, 0L,
            NOW, NOW, "system", "system");
    }

    private List<DeliveryCustomerNotificationEvent> customerEvents(List<Object> observed) {
        return observed.stream().filter(DeliveryCustomerNotificationEvent.class::isInstance)
            .map(DeliveryCustomerNotificationEvent.class::cast).toList();
    }

    private DeliveryOrderTransaction committingTransaction() {
        return transaction(true);
    }

    private DeliveryOrderTransaction rollingBackTransaction() {
        return transaction(false);
    }

    private DeliveryOrderTransaction transaction(boolean commit) {
        return new DeliveryOrderTransaction() {
            @Override
            public <T> T execute(Supplier<T> operation) {
                TransactionSynchronizationManager.setActualTransactionActive(true);
                TransactionSynchronizationManager.initSynchronization();
                try {
                    T result = operation.get();
                    List<TransactionSynchronization> synchronizations =
                        TransactionSynchronizationManager.getSynchronizations();
                    if (!commit) {
                        synchronizations.forEach(synchronization ->
                            synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
                        throw new IllegalStateException("controlled dispatch rollback");
                    }
                    synchronizations.forEach(TransactionSynchronization::afterCommit);
                    synchronizations.forEach(synchronization ->
                        synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
                    return result;
                } finally {
                    TransactionSynchronizationManager.clearSynchronization();
                    TransactionSynchronizationManager.setActualTransactionActive(false);
                }
            }
        };
    }
}
