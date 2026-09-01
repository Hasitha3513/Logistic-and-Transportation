package com.transportlogistics.app.delivery.ports.outbound;

import com.transportlogistics.app.delivery.domain.model.DeliveryBatch;
import com.transportlogistics.app.delivery.domain.model.DeliveryBatchOrder;
import com.transportlogistics.app.delivery.domain.model.DeliveryBatchStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryBatchRepository {
    DeliveryBatch save(DeliveryBatch batch);

    Optional<DeliveryBatch> findById(UUID tenantId, UUID batchId);

    Optional<DeliveryBatch> findByIdForUpdate(UUID tenantId, UUID batchId);

    List<DeliveryBatch> findBatches(UUID tenantId, UUID zoneId, UUID slotId, UUID riderId, DeliveryBatchStatus status, int limit, int offset);

    long countBatches(UUID tenantId, UUID zoneId, UUID slotId, UUID riderId, DeliveryBatchStatus status);

    DeliveryBatchOrder saveOrderMembership(DeliveryBatchOrder orderMembership);

    List<DeliveryBatchOrder> findActiveOrderMembershipsByBatchId(UUID tenantId, UUID batchId);

    List<DeliveryBatchOrder> findAllOrderMembershipsByBatchId(UUID tenantId, UUID batchId);

    Optional<DeliveryBatchOrder> findActiveMembershipByDeliveryOrderId(UUID tenantId, UUID deliveryOrderId);

    List<UUID> findActiveBatchedDeliveryOrderIds(UUID tenantId, List<UUID> deliveryOrderIds);

    int countActiveOrdersByBatchId(UUID tenantId, UUID batchId);
}
