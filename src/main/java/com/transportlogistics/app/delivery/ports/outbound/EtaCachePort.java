package com.transportlogistics.app.delivery.ports.outbound;

import com.transportlogistics.app.delivery.domain.model.BatchEtaEstimate;
import com.transportlogistics.app.delivery.domain.model.SingleOrderEtaEstimate;

import java.util.Optional;
import java.util.UUID;

public interface EtaCachePort {

    Optional<SingleOrderEtaEstimate> getOrderEta(UUID tenantId, UUID orderId, String inputFingerprint);

    long beginOrderCalculation(UUID tenantId, UUID orderId);

    boolean putOrderEtaIfCurrent(UUID tenantId, UUID orderId, long generation,
                                 String inputFingerprint, SingleOrderEtaEstimate estimate);

    void evictOrderEta(UUID tenantId, UUID orderId);

    Optional<BatchEtaEstimate> getBatchEta(UUID tenantId, UUID batchId, String inputFingerprint);

    long beginBatchCalculation(UUID tenantId, UUID batchId);

    boolean putBatchEtaIfCurrent(UUID tenantId, UUID batchId, long generation,
                                 String inputFingerprint, BatchEtaEstimate estimate);

    void evictBatchEta(UUID tenantId, UUID batchId);

    void clear();
}
