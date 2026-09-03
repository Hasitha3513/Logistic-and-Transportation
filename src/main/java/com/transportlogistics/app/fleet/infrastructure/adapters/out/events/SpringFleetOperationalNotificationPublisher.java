package com.transportlogistics.app.fleet.infrastructure.adapters.out.events;

import com.transportlogistics.app.fleet.application.ports.out.FleetOperationalNotificationPublisher;
import com.transportlogistics.app.notification.OperationalNotificationEvent;
import com.transportlogistics.app.notification.OperationalNotificationPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringFleetOperationalNotificationPublisher implements FleetOperationalNotificationPublisher {
    private final OperationalNotificationPublisher publisher;

    public SpringFleetOperationalNotificationPublisher(OperationalNotificationPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(OperationalNotificationEvent event) {
        publisher.publish(event);
    }
}
