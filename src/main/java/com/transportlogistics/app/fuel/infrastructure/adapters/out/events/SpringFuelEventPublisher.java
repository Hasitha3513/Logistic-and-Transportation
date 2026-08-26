package com.transportlogistics.app.fuel.infrastructure.adapters.out.events;

import com.transportlogistics.app.fuel.application.ports.out.FuelEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
class SpringFuelEventPublisher implements FuelEventPublisher {
    private final ApplicationEventPublisher publisher;

    SpringFuelEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(Object event) {
        publisher.publishEvent(event);
    }
}
