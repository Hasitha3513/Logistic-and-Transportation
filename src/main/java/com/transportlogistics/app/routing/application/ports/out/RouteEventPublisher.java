package com.transportlogistics.app.routing.application.ports.out;

public interface RouteEventPublisher {
    void publish(Object event);
}
