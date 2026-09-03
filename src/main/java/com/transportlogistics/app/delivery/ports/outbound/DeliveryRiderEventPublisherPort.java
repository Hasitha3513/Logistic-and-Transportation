package com.transportlogistics.app.delivery.ports.outbound;

public interface DeliveryRiderEventPublisherPort {
    void publishEvent(Object event);
}
