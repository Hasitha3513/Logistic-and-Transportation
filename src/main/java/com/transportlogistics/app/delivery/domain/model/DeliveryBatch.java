package com.transportlogistics.app.delivery.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeliveryBatch(
        UUID id,
        UUID tenantId,
        DeliveryBatchCode batchCode,
        UUID deliveryZoneId,
        UUID deliverySlotId,
        UUID riderId,
        DeliveryBatchStatus status,
        int maxBatchSize,
        long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String createdBy,
        String updatedBy
) {
    public DeliveryBatch {
        if (id == null || tenantId == null || batchCode == null || deliveryZoneId == null
                || status == null || createdAt == null || updatedAt == null
                || createdBy == null || createdBy.isBlank() || updatedBy == null || updatedBy.isBlank()) {
            throw new BusinessRuleException("INVALID_DELIVERY_BATCH_DATA", "Required delivery batch data is missing");
        }
        if (maxBatchSize <= 0) {
            throw new BusinessRuleException("INVALID_MAX_BATCH_SIZE", "Max batch size must be positive");
        }
        createdBy = createdBy.trim();
        updatedBy = updatedBy.trim();
    }

    public static DeliveryBatch create(
            UUID id,
            UUID tenantId,
            DeliveryBatchCode batchCode,
            UUID deliveryZoneId,
            UUID deliverySlotId,
            int maxBatchSize,
            OffsetDateTime now,
            String actor
    ) {
        if (maxBatchSize <= 0) {
            throw new BusinessRuleException("INVALID_MAX_BATCH_SIZE", "Max batch size must be positive");
        }
        return new DeliveryBatch(
                id != null ? id : UUID.randomUUID(),
                tenantId,
                batchCode,
                deliveryZoneId,
                deliverySlotId,
                null,
                DeliveryBatchStatus.DRAFT,
                maxBatchSize,
                0L,
                now,
                now,
                actor,
                actor
        );
    }

    public DeliveryBatch updateMetadata(int newMaxBatchSize, OffsetDateTime now, String actor) {
        if (status != DeliveryBatchStatus.DRAFT && status != DeliveryBatchStatus.READY) {
            throw new BusinessRuleException("DELIVERY_BATCH_INVALID_STATE", "Batch metadata can only be updated in DRAFT or READY status");
        }
        return new DeliveryBatch(
                id,
                tenantId,
                batchCode,
                deliveryZoneId,
                deliverySlotId,
                riderId,
                status,
                newMaxBatchSize > 0 ? newMaxBatchSize : this.maxBatchSize,
                version,
                createdAt,
                now,
                createdBy,
                actor
        );
    }

    public DeliveryBatch markReady(int activeOrderCount, OffsetDateTime now, String actor) {
        if (status != DeliveryBatchStatus.DRAFT) {
            throw new BusinessRuleException("DELIVERY_BATCH_INVALID_STATE", "Batch must be in DRAFT status to mark as READY");
        }
        if (activeOrderCount <= 0) {
            throw new BusinessRuleException("DELIVERY_BATCH_EMPTY", "Cannot mark an empty batch as READY");
        }
        if (activeOrderCount > maxBatchSize) {
            throw new BusinessRuleException("DELIVERY_BATCH_CAPACITY_EXCEEDED", "Batch order count exceeds max batch size");
        }
        return new DeliveryBatch(
                id,
                tenantId,
                batchCode,
                deliveryZoneId,
                deliverySlotId,
                riderId,
                DeliveryBatchStatus.READY,
                maxBatchSize,
                version,
                createdAt,
                now,
                createdBy,
                actor
        );
    }

    public DeliveryBatch assignRider(UUID assignedRiderId, OffsetDateTime now, String actor) {
        if (assignedRiderId == null) {
            throw new BusinessRuleException("INVALID_RIDER_ID", "Rider ID is required for assignment");
        }
        if (status != DeliveryBatchStatus.DRAFT && status != DeliveryBatchStatus.READY && status != DeliveryBatchStatus.ASSIGNED) {
            throw new BusinessRuleException("DELIVERY_BATCH_INVALID_STATE", "Batch can only be assigned in DRAFT, READY, or ASSIGNED status");
        }
        return new DeliveryBatch(
                id,
                tenantId,
                batchCode,
                deliveryZoneId,
                deliverySlotId,
                assignedRiderId,
                DeliveryBatchStatus.ASSIGNED,
                maxBatchSize,
                version,
                createdAt,
                now,
                createdBy,
                actor
        );
    }

    public DeliveryBatch dispatch(OffsetDateTime now, String actor) {
        if (status != DeliveryBatchStatus.ASSIGNED) {
            throw new BusinessRuleException("DELIVERY_BATCH_INVALID_STATE", "Batch must be in ASSIGNED status to dispatch");
        }
        if (riderId == null) {
            throw new BusinessRuleException("DELIVERY_BATCH_NO_RIDER", "Cannot dispatch batch without an assigned rider");
        }
        return new DeliveryBatch(
                id,
                tenantId,
                batchCode,
                deliveryZoneId,
                deliverySlotId,
                riderId,
                DeliveryBatchStatus.DISPATCHED,
                maxBatchSize,
                version,
                createdAt,
                now,
                createdBy,
                actor
        );
    }

    public DeliveryBatch complete(OffsetDateTime now, String actor) {
        if (status != DeliveryBatchStatus.DISPATCHED) {
            throw new BusinessRuleException("DELIVERY_BATCH_INVALID_STATE", "Batch must be in DISPATCHED status to complete");
        }
        return new DeliveryBatch(
                id,
                tenantId,
                batchCode,
                deliveryZoneId,
                deliverySlotId,
                riderId,
                DeliveryBatchStatus.COMPLETED,
                maxBatchSize,
                version,
                createdAt,
                now,
                createdBy,
                actor
        );
    }

    public DeliveryBatch cancel(OffsetDateTime now, String actor) {
        if (status == DeliveryBatchStatus.COMPLETED || status == DeliveryBatchStatus.CANCELLED) {
            throw new BusinessRuleException("DELIVERY_BATCH_INVALID_STATE", "Completed or already cancelled batch cannot be cancelled");
        }
        return new DeliveryBatch(
                id,
                tenantId,
                batchCode,
                deliveryZoneId,
                deliverySlotId,
                riderId,
                DeliveryBatchStatus.CANCELLED,
                maxBatchSize,
                version,
                createdAt,
                now,
                createdBy,
                actor
        );
    }
}
