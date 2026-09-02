package com.transportlogistics.app.delivery.ports.outbound;

import com.transportlogistics.app.delivery.domain.events.DeliveryEtaCalculatedEvent;
import com.transportlogistics.app.delivery.DeliveryCustomerNotificationEvent;

public interface DeliveryEtaEventPublisherPort {
    void publish(DeliveryEtaCalculatedEvent event);

    default void publishCustomerNotification(DeliveryCustomerNotificationEvent event) {
        // Optional compatibility hook for adapters predating US-69.
    }
}
