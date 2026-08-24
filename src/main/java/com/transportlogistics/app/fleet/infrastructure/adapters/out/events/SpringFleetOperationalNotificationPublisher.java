package com.transportlogistics.app.fleet.infrastructure.adapters.out.events;

import com.transportlogistics.app.fleet.application.ports.out.FleetOperationalNotificationPublisher;
import com.transportlogistics.app.notification.OperationalNotificationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringFleetOperationalNotificationPublisher implements FleetOperationalNotificationPublisher {
    private final ApplicationEventPublisher publisher;

    public SpringFleetOperationalNotificationPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(OperationalNotificationEvent event) {
        publisher.publishEvent(event);
    }
}
