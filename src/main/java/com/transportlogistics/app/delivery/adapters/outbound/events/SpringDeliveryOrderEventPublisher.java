package com.transportlogistics.app.delivery.adapters.outbound.events;

import com.transportlogistics.app.delivery.ports.outbound.DeliveryOrderEventPublisherPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringDeliveryOrderEventPublisher implements DeliveryOrderEventPublisherPort {
    private final ApplicationEventPublisher publisher;

    public SpringDeliveryOrderEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publishEvent(Object event) {
        publisher.publishEvent(event);
    }
}
