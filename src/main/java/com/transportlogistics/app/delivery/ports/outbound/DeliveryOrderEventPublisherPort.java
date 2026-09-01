package com.transportlogistics.app.delivery.ports.outbound;

public interface DeliveryOrderEventPublisherPort {
    void publishEvent(Object event);
}
