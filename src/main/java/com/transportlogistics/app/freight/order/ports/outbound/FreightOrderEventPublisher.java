package com.transportlogistics.app.freight.order.ports.outbound;

public interface FreightOrderEventPublisher {
    void publish(Object event);
}
