package com.transportlogistics.app.fuel.infrastructure.adapters.out.events;

import com.transportlogistics.app.fuel.application.ports.out.FuelEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class SpringFuelEventPublisher implements FuelEventPublisher {
    private final ApplicationEventPublisher publisher;

    @Override
    public void publish(Object event) {
        publisher.publishEvent(event);
    }
}
