package com.transportlogistics.app.delivery.ports.outbound;

import com.transportlogistics.app.delivery.domain.events.DeliveryEtaCalculatedEvent;

public interface DeliveryEtaEventPublisherPort {
    void publish(DeliveryEtaCalculatedEvent event);
}
