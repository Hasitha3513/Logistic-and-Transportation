package com.transportlogistics.app.delivery.adapters.outbound.events;

import com.transportlogistics.app.delivery.ports.outbound.DeliveryBatchEventPublisherPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringDeliveryBatchEventPublisher implements DeliveryBatchEventPublisherPort {
    private final ApplicationEventPublisher publisher;

    public SpringDeliveryBatchEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(Object event) {
        if (event != null) {
            publisher.publishEvent(event);
        }
    }
}
