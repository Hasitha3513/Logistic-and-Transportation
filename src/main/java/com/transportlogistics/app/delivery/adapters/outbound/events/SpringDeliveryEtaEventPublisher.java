package com.transportlogistics.app.delivery.adapters.outbound.events;

import com.transportlogistics.app.delivery.domain.events.DeliveryEtaCalculatedEvent;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryEtaEventPublisherPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringDeliveryEtaEventPublisher implements DeliveryEtaEventPublisherPort {

    private final ApplicationEventPublisher publisher;

    public SpringDeliveryEtaEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(DeliveryEtaCalculatedEvent event) {
        if (event != null) {
            publisher.publishEvent(event);
        }
    }
}
