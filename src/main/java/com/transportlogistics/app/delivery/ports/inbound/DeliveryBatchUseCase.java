package com.transportlogistics.app.delivery.ports.inbound;

import com.transportlogistics.app.delivery.domain.model.DeliveryBatch;
import com.transportlogistics.app.delivery.domain.model.DeliveryBatchOrder;
import com.transportlogistics.app.delivery.domain.model.DeliveryBatchStatus;

import java.util.List;
import java.util.UUID;

public interface DeliveryBatchUseCase {

    record CreateDeliveryBatchCommand(
            UUID deliveryZoneId,
            UUID deliverySlotId,
            Integer maxBatchSize,
            List<UUID> deliveryOrderIds,
            UUID riderId
    ) {}

    record AutoClusterBatchesCommand(
            UUID deliveryZoneId,
            UUID deliverySlotId,
            Integer maxBatchSize
    ) {}

    record UpdateDeliveryBatchCommand(
            Integer maxBatchSize
    ) {}

    record AddOrdersToBatchCommand(
            List<UUID> deliveryOrderIds
    ) {}

    record AssignRiderToBatchCommand(
            UUID riderId,
            boolean override,
            String overrideReason
    ) {}

    record DeliveryBatchSummary(
            UUID id,
            String batchCode,
            UUID deliveryZoneId,
            UUID deliverySlotId,
            UUID riderId,
            DeliveryBatchStatus status,
            int maxBatchSize,
            int activeOrderCount,
            long version
    ) {}

    DeliveryBatch createBatch(CreateDeliveryBatchCommand command);

    List<DeliveryBatch> autoClusterBatches(AutoClusterBatchesCommand command);

    DeliveryBatch getBatch(UUID batchId);

    List<DeliveryBatch> listBatches(UUID zoneId, UUID slotId, UUID riderId, DeliveryBatchStatus status, int limit, int offset);

    long countBatches(UUID zoneId, UUID slotId, UUID riderId, DeliveryBatchStatus status);

    DeliveryBatch updateBatch(UUID batchId, UpdateDeliveryBatchCommand command);

    DeliveryBatch markReady(UUID batchId);

    DeliveryBatch addOrdersToBatch(UUID batchId, AddOrdersToBatchCommand command);

    DeliveryBatch removeOrderFromBatch(UUID batchId, UUID deliveryOrderId);

    DeliveryBatch assignRider(UUID batchId, AssignRiderToBatchCommand command);

    DeliveryBatch dispatchBatch(UUID batchId);

    DeliveryBatch cancelBatch(UUID batchId);

    List<DeliveryBatchOrder> getBatchOrderMemberships(UUID batchId);
}
