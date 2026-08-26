package com.transportlogistics.app.notification.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationDeliveryAttemptJpaRepository extends JpaRepository<NotificationDeliveryAttemptEntity, UUID> {
    Optional<NotificationDeliveryAttemptEntity> findFirstByNotificationIdOrderByAttemptNumberDesc(UUID notificationId);
    List<NotificationDeliveryAttemptEntity> findByNotificationIdOrderByAttemptNumberAsc(UUID notificationId);
    long countByNotificationId(UUID notificationId);
}
