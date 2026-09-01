package com.transportlogistics.app.delivery.adapters.outbound.cache;

import com.transportlogistics.app.delivery.domain.model.BatchEtaEstimate;
import com.transportlogistics.app.delivery.domain.model.SingleOrderEtaEstimate;
import com.transportlogistics.app.delivery.ports.outbound.EtaCachePort;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class InMemoryEtaCacheAdapter implements EtaCachePort {

    private final Map<OrderCacheKey, SingleOrderEtaEstimate> orderCache = new ConcurrentHashMap<>();
    private final Map<BatchCacheKey, BatchEtaEstimate> batchCache = new ConcurrentHashMap<>();
    private static final int MAX_GENERATION_ENTRIES = 100_000;
    private final Map<SubjectKey, Long> generations = new ConcurrentHashMap<>();
    private final AtomicLong generationSequence = new AtomicLong();

    private record SubjectKey(UUID tenantId, String type, UUID subjectId) {}

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
    public long beginOrderCalculation(UUID tenantId, UUID orderId) {
        return nextGeneration(new SubjectKey(tenantId, "ORDER", orderId));
    }

    @Override
    public boolean putOrderEtaIfCurrent(UUID tenantId, UUID orderId, long generation,
                                        String inputFingerprint, SingleOrderEtaEstimate estimate) {
        if (!isCurrent(new SubjectKey(tenantId, "ORDER", orderId), generation)) return false;
        orderCache.put(new OrderCacheKey(tenantId, orderId, inputFingerprint), estimate);
        return true;
    }

    @Override
    public void evictOrderEta(UUID tenantId, UUID orderId) {
        if (tenantId == null || orderId == null) return;
        orderCache.keySet().removeIf(k -> k.tenantId().equals(tenantId) && k.orderId().equals(orderId));
        nextGeneration(new SubjectKey(tenantId, "ORDER", orderId));
    }

    @Override
    public Optional<BatchEtaEstimate> getBatchEta(UUID tenantId, UUID batchId, String inputFingerprint) {
        if (tenantId == null || batchId == null || inputFingerprint == null) return Optional.empty();
        return Optional.ofNullable(batchCache.get(new BatchCacheKey(tenantId, batchId, inputFingerprint)));
    }

    @Override
    public long beginBatchCalculation(UUID tenantId, UUID batchId) {
        return nextGeneration(new SubjectKey(tenantId, "BATCH", batchId));
    }

    @Override
    public boolean putBatchEtaIfCurrent(UUID tenantId, UUID batchId, long generation,
                                        String inputFingerprint, BatchEtaEstimate estimate) {
        if (!isCurrent(new SubjectKey(tenantId, "BATCH", batchId), generation)) return false;
        batchCache.put(new BatchCacheKey(tenantId, batchId, inputFingerprint), estimate);
        return true;
    }

    @Override
    public void evictBatchEta(UUID tenantId, UUID batchId) {
        if (tenantId == null || batchId == null) return;
        batchCache.keySet().removeIf(k -> k.tenantId().equals(tenantId) && k.batchId().equals(batchId));
        nextGeneration(new SubjectKey(tenantId, "BATCH", batchId));
    }

    @Override
    public void clear() {
        orderCache.clear();
        batchCache.clear();
        generations.clear();
    }

    private long nextGeneration(SubjectKey key) {
        long generation = generationSequence.incrementAndGet();
        generations.put(key, generation);
        if (generations.size() > MAX_GENERATION_ENTRIES) {
            pruneUnusedGenerations();
        }
        return generation;
    }

    private boolean isCurrent(SubjectKey key, long generation) {
        return generations.getOrDefault(key, Long.MIN_VALUE) == generation;
    }

    private void pruneUnusedGenerations() {
        generations.keySet().removeIf(key -> !hasCachedValue(key));
    }

    private boolean hasCachedValue(SubjectKey key) {
        if ("ORDER".equals(key.type())) {
            return orderCache.keySet().stream().anyMatch(cacheKey ->
                    cacheKey.tenantId().equals(key.tenantId()) && cacheKey.orderId().equals(key.subjectId()));
        }
        return batchCache.keySet().stream().anyMatch(cacheKey ->
                cacheKey.tenantId().equals(key.tenantId()) && cacheKey.batchId().equals(key.subjectId()));
    }
}
