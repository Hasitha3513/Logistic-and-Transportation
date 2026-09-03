package com.transportlogistics.app.delivery.ports.outbound;

import java.util.Map;
import java.util.UUID;

public interface DeliveryNotificationPort {
    void notifyInternalDeliveryEvent(UUID deliveryId, String eventType, Map<String, String> metadata);
}
