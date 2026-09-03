package com.transportlogistics.app.delivery.adapters.outbound.events;

import com.transportlogistics.app.delivery.ports.outbound.DeliveryBatchEventPublisherPort;
import com.transportlogistics.app.shared.AfterCommitEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringDeliveryBatchEventPublisher implements DeliveryBatchEventPublisherPort {
    private final AfterCommitEventPublisher publisher;

    public SpringDeliveryBatchEventPublisher(AfterCommitEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(Object event) {
        if (event != null) {
            publisher.publish(event);
        }
    }
}
