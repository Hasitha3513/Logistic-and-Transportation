package com.transportlogistics.app.delivery.adapters.outbound.events;

import com.transportlogistics.app.delivery.domain.events.DeliveryEtaCalculatedEvent;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryEtaEventPublisherPort;
import com.transportlogistics.app.shared.AfterCommitEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringDeliveryEtaEventPublisher implements DeliveryEtaEventPublisherPort {

    private final AfterCommitEventPublisher publisher;

    public SpringDeliveryEtaEventPublisher(AfterCommitEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(DeliveryEtaCalculatedEvent event) {
        if (event != null) {
            publisher.publish(event);
        }
    }
}
