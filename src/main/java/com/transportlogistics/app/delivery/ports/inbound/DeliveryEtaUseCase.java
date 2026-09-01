package com.transportlogistics.app.delivery.ports.inbound;

import com.transportlogistics.app.delivery.domain.model.BatchEtaEstimate;
import com.transportlogistics.app.delivery.domain.model.SingleOrderEtaEstimate;

import java.util.UUID;

public interface DeliveryEtaUseCase {

    SingleOrderEtaEstimate getOrderEta(UUID orderId);

    SingleOrderEtaEstimate calculateOrderEta(UUID orderId, String actor);

    default SingleOrderEtaEstimate calculateOrderEta(UUID orderId) {
        return calculateOrderEta(orderId, "system");
    }

    BatchEtaEstimate getBatchEta(UUID batchId);

    BatchEtaEstimate calculateBatchEta(UUID batchId, String actor);

    default BatchEtaEstimate calculateBatchEta(UUID batchId) {
        return calculateBatchEta(batchId, "system");
    }
}
