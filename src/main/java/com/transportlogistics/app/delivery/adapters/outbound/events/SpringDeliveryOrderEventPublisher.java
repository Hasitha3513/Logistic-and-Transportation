package com.transportlogistics.app.delivery.adapters.outbound.events;

import com.transportlogistics.app.delivery.ports.outbound.DeliveryOrderEventPublisherPort;
import com.transportlogistics.app.shared.AfterCommitEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringDeliveryOrderEventPublisher implements DeliveryOrderEventPublisherPort {
    private final AfterCommitEventPublisher publisher;

    public SpringDeliveryOrderEventPublisher(AfterCommitEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publishEvent(Object event) {
        publisher.publish(event);
    }
}
