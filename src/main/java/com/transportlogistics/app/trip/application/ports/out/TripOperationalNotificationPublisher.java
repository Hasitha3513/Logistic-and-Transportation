package com.transportlogistics.app.trip.application.ports.out;

import com.transportlogistics.app.notification.OperationalNotificationEvent;

public interface TripOperationalNotificationPublisher {
    void publish(OperationalNotificationEvent event);
}
