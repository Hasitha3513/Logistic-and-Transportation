package com.transportlogistics.app.trip.infrastructure.adapters.out.events;

import com.transportlogistics.app.notification.OperationalNotificationEvent;
import com.transportlogistics.app.notification.OperationalNotificationPublisher;
import com.transportlogistics.app.trip.application.ports.out.TripOperationalNotificationPublisher;
import org.springframework.stereotype.Component;

@Component
public final class SpringTripOperationalNotificationPublisher implements TripOperationalNotificationPublisher {
    private final OperationalNotificationPublisher publisher;

    public SpringTripOperationalNotificationPublisher(OperationalNotificationPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(OperationalNotificationEvent event) {
        publisher.publish(event);
    }
}
