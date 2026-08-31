package com.transportlogistics.app.delivery.adapters.outbound.events;

import com.transportlogistics.app.delivery.ports.outbound.DeliveryRiderEventPublisherPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringDeliveryRiderEventPublisher implements DeliveryRiderEventPublisherPort {
    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringDeliveryRiderEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publishEvent(Object event) {
        if (event != null) {
            applicationEventPublisher.publishEvent(event);
        }
    }
}
