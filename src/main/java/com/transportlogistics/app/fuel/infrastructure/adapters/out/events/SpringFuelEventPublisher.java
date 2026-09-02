package com.transportlogistics.app.fuel.infrastructure.adapters.out.events;

import com.transportlogistics.app.fuel.application.ports.out.FuelEventPublisher;
import com.transportlogistics.app.shared.AfterCommitEventPublisher;
import org.springframework.stereotype.Component;

@Component
class SpringFuelEventPublisher implements FuelEventPublisher {
    private final AfterCommitEventPublisher publisher;

    SpringFuelEventPublisher(AfterCommitEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(Object event) {
        publisher.publish(event);
    }
}
