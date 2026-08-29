package com.transportlogistics.app.delivery.ports.outbound;

import java.util.UUID;

public interface DeliveryOfflineSyncPort {
    OfflineAcknowledgement acknowledge(UUID operationId, UUID deliveryId, String result);

    record OfflineAcknowledgement(UUID operationId, UUID deliveryId, String result) {}
}
