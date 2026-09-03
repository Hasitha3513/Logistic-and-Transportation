package com.transportlogistics.app.notification;

public interface OperationalNotificationPublisher {
    void publish(OperationalNotificationEvent event);
}
