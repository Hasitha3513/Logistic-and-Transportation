package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.domain.model.DeliveryBatch;
import com.transportlogistics.app.delivery.domain.model.DeliveryBatchCode;
import com.transportlogistics.app.delivery.domain.model.DeliveryBatchOrder;
import com.transportlogistics.app.delivery.domain.model.DeliveryBatchOrderStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryBatchStatus;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryBatchRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class DeliveryBatchPersistenceAdapter implements DeliveryBatchRepository {

    private final DeliveryBatchJpaRepository batchJpaRepository;
    private final DeliveryBatchOrderJpaRepository batchOrderJpaRepository;

    public DeliveryBatchPersistenceAdapter(
            DeliveryBatchJpaRepository batchJpaRepository,
            DeliveryBatchOrderJpaRepository batchOrderJpaRepository
    ) {
        this.batchJpaRepository = batchJpaRepository;
        this.batchOrderJpaRepository = batchOrderJpaRepository;
    }

    @Override
    public DeliveryBatch save(DeliveryBatch batch) {
        DeliveryBatchEntity entity = toEntity(batch);
        DeliveryBatchEntity saved = batchJpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<DeliveryBatch> findById(UUID tenantId, UUID batchId) {
        return batchJpaRepository.findByIdAndTenantId(batchId, tenantId).map(this::toDomain);
    }

    @Override
    public Optional<DeliveryBatch> findByIdForUpdate(UUID tenantId, UUID batchId) {
        return batchJpaRepository.findByIdAndTenantIdForUpdate(batchId, tenantId).map(this::toDomain);
    }

    @Override
    public List<DeliveryBatch> findBatches(UUID tenantId, UUID zoneId, UUID slotId, UUID riderId, DeliveryBatchStatus status, int limit, int offset) {
        int page = offset / Math.max(1, limit);
        PageRequest pageRequest = PageRequest.of(page, limit);
        String statusStr = status != null ? status.name() : null;
        return batchJpaRepository.findBatches(tenantId, zoneId, slotId, riderId, statusStr, pageRequest)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countBatches(UUID tenantId, UUID zoneId, UUID slotId, UUID riderId, DeliveryBatchStatus status) {
        String statusStr = status != null ? status.name() : null;
        return batchJpaRepository.countBatches(tenantId, zoneId, slotId, riderId, statusStr);
    }

    @Override
    public DeliveryBatchOrder saveOrderMembership(DeliveryBatchOrder orderMembership) {
        DeliveryBatchOrderEntity entity = toEntity(orderMembership);
        DeliveryBatchOrderEntity saved = batchOrderJpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<DeliveryBatchOrder> findActiveOrderMembershipsByBatchId(UUID tenantId, UUID batchId) {
        return batchOrderJpaRepository.findActiveByBatchId(tenantId, batchId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<DeliveryBatchOrder> findAllOrderMembershipsByBatchId(UUID tenantId, UUID batchId) {
        return batchOrderJpaRepository.findAllByBatchId(tenantId, batchId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<DeliveryBatchOrder> findActiveMembershipByDeliveryOrderId(UUID tenantId, UUID deliveryOrderId) {
        return batchOrderJpaRepository.findByTenantIdAndDeliveryOrderIdAndStatus(tenantId, deliveryOrderId, "ACTIVE")
                .map(this::toDomain);
    }

    @Override
    public List<UUID> findActiveBatchedDeliveryOrderIds(UUID tenantId, List<UUID> deliveryOrderIds) {
        if (deliveryOrderIds == null || deliveryOrderIds.isEmpty()) {
            return List.of();
        }
        return batchOrderJpaRepository.findActiveBatchedDeliveryOrderIds(tenantId, deliveryOrderIds);
    }

    @Override
    public int countActiveOrdersByBatchId(UUID tenantId, UUID batchId) {
        return batchOrderJpaRepository.countActiveByBatchId(tenantId, batchId);
    }

    private DeliveryBatchEntity toEntity(DeliveryBatch domain) {
        DeliveryBatchEntity entity = new DeliveryBatchEntity();
        entity.setId(domain.id());
        entity.setTenantId(domain.tenantId());
        entity.setBatchCode(domain.batchCode().value());
        entity.setDeliveryZoneId(domain.deliveryZoneId());
        entity.setDeliverySlotId(domain.deliverySlotId());
        entity.setRiderId(domain.riderId());
        entity.setStatus(domain.status().name());
        entity.setMaxBatchSize(domain.maxBatchSize());
        entity.setVersion(domain.version());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        entity.setCreatedBy(domain.createdBy());
        entity.setUpdatedBy(domain.updatedBy());
        return entity;
    }

    private DeliveryBatch toDomain(DeliveryBatchEntity entity) {
        return new DeliveryBatch(
                entity.getId(),
                entity.getTenantId(),
                new DeliveryBatchCode(entity.getBatchCode()),
                entity.getDeliveryZoneId(),
                entity.getDeliverySlotId(),
                entity.getRiderId(),
                DeliveryBatchStatus.valueOf(entity.getStatus()),
                entity.getMaxBatchSize(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedBy()
        );
    }

    private DeliveryBatchOrderEntity toEntity(DeliveryBatchOrder domain) {
        DeliveryBatchOrderEntity entity = new DeliveryBatchOrderEntity();
        entity.setId(domain.id());
        entity.setTenantId(domain.tenantId());
        entity.setBatchId(domain.batchId());
        entity.setDeliveryOrderId(domain.deliveryOrderId());
        entity.setSequenceHint(domain.sequenceHint());
        entity.setStatus(domain.status().name());
        entity.setAddedAt(domain.addedAt());
        entity.setAddedBy(domain.addedBy());
        entity.setRemovedAt(domain.removedAt());
        entity.setRemovedBy(domain.removedBy());
        entity.setVersion(domain.version());
        return entity;
    }

    private DeliveryBatchOrder toDomain(DeliveryBatchOrderEntity entity) {
        return new DeliveryBatchOrder(
                entity.getId(),
                entity.getTenantId(),
                entity.getBatchId(),
                entity.getDeliveryOrderId(),
                entity.getSequenceHint(),
                DeliveryBatchOrderStatus.valueOf(entity.getStatus()),
                entity.getAddedAt(),
                entity.getAddedBy(),
                entity.getRemovedAt(),
                entity.getRemovedBy(),
                entity.getVersion()
        );
    }
}
