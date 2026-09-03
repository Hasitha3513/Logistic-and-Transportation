package com.transportlogistics.app.delivery.adapters.outbound.events;

import com.transportlogistics.app.delivery.ports.outbound.DeliveryRiderEventPublisherPort;
import com.transportlogistics.app.shared.AfterCommitEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringDeliveryRiderEventPublisher implements DeliveryRiderEventPublisherPort {
    private final AfterCommitEventPublisher applicationEventPublisher;

    public SpringDeliveryRiderEventPublisher(AfterCommitEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publishEvent(Object event) {
        if (event != null) {
            applicationEventPublisher.publish(event);
        }
    }
}
