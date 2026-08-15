package com.transportlogistics.app.fuel.application.ports.out;

public interface FuelEventPublisher {
    void publish(Object event);
}
