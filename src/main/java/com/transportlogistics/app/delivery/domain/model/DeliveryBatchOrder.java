package com.transportlogistics.app.delivery.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeliveryBatchOrder(
        UUID id,
        UUID tenantId,
        UUID batchId,
        UUID deliveryOrderId,
        Integer sequenceHint,
        DeliveryBatchOrderStatus status,
        OffsetDateTime addedAt,
        String addedBy,
        OffsetDateTime removedAt,
        String removedBy,
        long version
) {
    public DeliveryBatchOrder {
        if (id == null || tenantId == null || batchId == null || deliveryOrderId == null
                || status == null || addedAt == null || addedBy == null || addedBy.isBlank()) {
            throw new BusinessRuleException("INVALID_BATCH_ORDER_DATA", "Required batch order data is missing");
        }
        addedBy = addedBy.trim();
        removedBy = removedBy == null || removedBy.isBlank() ? null : removedBy.trim();
    }

    public static DeliveryBatchOrder create(
            UUID id,
            UUID tenantId,
            UUID batchId,
            UUID deliveryOrderId,
            Integer sequenceHint,
            OffsetDateTime now,
            String actor
    ) {
        return new DeliveryBatchOrder(
                id != null ? id : UUID.randomUUID(),
                tenantId,
                batchId,
                deliveryOrderId,
                sequenceHint,
                DeliveryBatchOrderStatus.ACTIVE,
                now,
                actor,
                null,
                null,
                0L
        );
    }

    public DeliveryBatchOrder markRemoved(OffsetDateTime now, String actor) {
        if (this.status != DeliveryBatchOrderStatus.ACTIVE) {
            throw new BusinessRuleException("BATCH_ORDER_NOT_ACTIVE", "Only active batch order memberships can be removed");
        }
        return new DeliveryBatchOrder(
                id,
                tenantId,
                batchId,
                deliveryOrderId,
                sequenceHint,
                DeliveryBatchOrderStatus.REMOVED,
                addedAt,
                addedBy,
                now,
                actor,
                version + 1
        );
    }

    public DeliveryBatchOrder markCompleted(OffsetDateTime now) {
        return new DeliveryBatchOrder(
                id,
                tenantId,
                batchId,
                deliveryOrderId,
                sequenceHint,
                DeliveryBatchOrderStatus.COMPLETED,
                addedAt,
                addedBy,
                now,
                addedBy,
                version + 1
        );
    }
}
