package com.transportlogistics.app.delivery.adapters.inbound.events;

import com.transportlogistics.app.delivery.domain.events.DeliveryBatchOrderMembershipEvent;
import com.transportlogistics.app.delivery.domain.events.DeliveryBatchRiderAssignedEvent;
import com.transportlogistics.app.delivery.domain.events.DeliveryBatchStatusChangedEvent;
import com.transportlogistics.app.delivery.domain.events.DeliveryOrderDestinationChangedEvent;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderEvents;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryBatchRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryRiderRepository;
import com.transportlogistics.app.delivery.ports.outbound.EtaCachePort;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DeliveryEtaInvalidationListener {

    private final EtaCachePort cache;
    private final DeliveryBatchRepository batches;
    private final DeliveryRiderRepository riders;

    public DeliveryEtaInvalidationListener(EtaCachePort cache, DeliveryBatchRepository batches,
                                           DeliveryRiderRepository riders) {
        this.cache = cache;
        this.batches = batches;
        this.riders = riders;
    }

    @EventListener
    public void onMembership(DeliveryBatchOrderMembershipEvent event) {
        cache.evictOrderEta(event.tenantId(), event.deliveryOrderId());
        cache.evictBatchEta(event.tenantId(), event.batchId());
    }

    @EventListener
    public void onRiderAssignment(DeliveryBatchRiderAssignedEvent event) {
        cache.evictBatchEta(event.tenantId(), event.batchId());
    }

    @EventListener
    public void onBatchStatus(DeliveryBatchStatusChangedEvent event) {
        cache.evictBatchEta(event.tenantId(), event.batchId());
    }

    @EventListener
    public void onOrderRiderAssigned(DeliveryRiderEvents.DeliveryRiderAssignedEvent event) {
        invalidateOrderAndBatch(event.tenantId(), event.deliveryOrderId());
    }

    @EventListener
    public void onOrderRiderReassigned(DeliveryRiderEvents.DeliveryRiderReassignedEvent event) {
        invalidateOrderAndBatch(event.tenantId(), event.deliveryOrderId());
    }

    @EventListener
    public void onOrderRiderUnassigned(DeliveryRiderEvents.DeliveryRiderUnassignedEvent event) {
        invalidateOrderAndBatch(event.tenantId(), event.deliveryOrderId());
    }

    @EventListener
    public void onModeChanged(DeliveryRiderEvents.DeliveryRiderTransportModeChangedEvent event) {
        riders.findActiveAssignmentsForRider(event.riderId(), event.tenantId())
                .forEach(assignment -> cache.evictOrderEta(event.tenantId(), assignment.getDeliveryOrderId()));
        batches.findBatches(event.tenantId(), null, null, event.riderId(), null, 500, 0)
                .forEach(batch -> cache.evictBatchEta(event.tenantId(), batch.id()));
    }

    @EventListener
    public void onDestinationChanged(DeliveryOrderDestinationChangedEvent event) {
        invalidateOrderAndBatch(event.tenantId(), event.deliveryOrderId());
    }

    private void invalidateOrderAndBatch(java.util.UUID tenantId, java.util.UUID orderId) {
        cache.evictOrderEta(tenantId, orderId);
        batches.findActiveMembershipByDeliveryOrderId(tenantId, orderId)
                .ifPresent(member -> cache.evictBatchEta(tenantId, member.batchId()));
    }
}
