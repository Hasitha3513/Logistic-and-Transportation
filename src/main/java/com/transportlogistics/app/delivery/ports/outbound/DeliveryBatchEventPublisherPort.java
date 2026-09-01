package com.transportlogistics.app.delivery.ports.outbound;

public interface DeliveryBatchEventPublisherPort {
    void publish(Object event);
}
