package com.transportlogistics.app.freight.order.adapters.outbound.events;

import com.transportlogistics.app.freight.order.ports.outbound.FreightOrderEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
class SpringFreightOrderEventPublisher implements FreightOrderEventPublisher {
    private final ApplicationEventPublisher publisher;
    SpringFreightOrderEventPublisher(ApplicationEventPublisher publisher) { this.publisher = publisher; }
    @Override public void publish(Object event) { publisher.publishEvent(event); }
}
