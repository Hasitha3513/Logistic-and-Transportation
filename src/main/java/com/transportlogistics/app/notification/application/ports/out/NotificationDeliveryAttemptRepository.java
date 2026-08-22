package com.transportlogistics.app.notification.application.ports.out;

import com.transportlogistics.app.notification.domain.model.NotificationDeliveryAttempt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationDeliveryAttemptRepository {
    NotificationDeliveryAttempt save(NotificationDeliveryAttempt attempt);
    Optional<NotificationDeliveryAttempt> findLatest(UUID notificationId);
    Optional<NotificationDeliveryAttempt> findById(UUID id);
    List<NotificationDeliveryAttempt> findByNotificationId(UUID notificationId);
    long countByNotificationId(UUID notificationId);
}
