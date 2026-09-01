package com.transportlogistics.app.delivery.adapters.outbound.cache;

import com.transportlogistics.app.delivery.domain.model.BatchEtaEstimate;
import com.transportlogistics.app.delivery.domain.model.SingleOrderEtaEstimate;
import com.transportlogistics.app.delivery.ports.outbound.EtaCachePort;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryEtaCacheAdapter implements EtaCachePort {

    private final Map<OrderCacheKey, SingleOrderEtaEstimate> orderCache = new ConcurrentHashMap<>();
    private final Map<BatchCacheKey, BatchEtaEstimate> batchCache = new ConcurrentHashMap<>();

    private record OrderCacheKey(UUID tenantId, UUID orderId, String inputFingerprint) {
    }

    private record BatchCacheKey(UUID tenantId, UUID batchId, String inputFingerprint) {
    }

    @Override
    public Optional<SingleOrderEtaEstimate> getOrderEta(UUID tenantId, UUID orderId, String inputFingerprint) {
        if (tenantId == null || orderId == null || inputFingerprint == null) return Optional.empty();
        return Optional.ofNullable(orderCache.get(new OrderCacheKey(tenantId, orderId, inputFingerprint)));
    }

    @Override
    public void putOrderEta(UUID tenantId, UUID orderId, String inputFingerprint, SingleOrderEtaEstimate estimate) {
        if (tenantId == null || orderId == null || inputFingerprint == null || estimate == null) return;
        orderCache.put(new OrderCacheKey(tenantId, orderId, inputFingerprint), estimate);
    }

    @Override
    public void evictOrderEta(UUID tenantId, UUID orderId) {
        if (tenantId == null || orderId == null) return;
        orderCache.keySet().removeIf(k -> k.tenantId().equals(tenantId) && k.orderId().equals(orderId));
    }

    @Override
    public Optional<BatchEtaEstimate> getBatchEta(UUID tenantId, UUID batchId, String inputFingerprint) {
        if (tenantId == null || batchId == null || inputFingerprint == null) return Optional.empty();
        return Optional.ofNullable(batchCache.get(new BatchCacheKey(tenantId, batchId, inputFingerprint)));
    }

    @Override
    public void putBatchEta(UUID tenantId, UUID batchId, String inputFingerprint, BatchEtaEstimate estimate) {
        if (tenantId == null || batchId == null || inputFingerprint == null || estimate == null) return;
        batchCache.put(new BatchCacheKey(tenantId, batchId, inputFingerprint), estimate);
    }

    @Override
    public void evictBatchEta(UUID tenantId, UUID batchId) {
        if (tenantId == null || batchId == null) return;
        batchCache.keySet().removeIf(k -> k.tenantId().equals(tenantId) && k.batchId().equals(batchId));
    }

    @Override
    public void clear() {
        orderCache.clear();
        batchCache.clear();
    }
}
