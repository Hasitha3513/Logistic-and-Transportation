package com.transportlogistics.app.notification.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.notification.application.ports.out.NotificationDeliveryAttemptRepository;
import com.transportlogistics.app.notification.domain.model.NotificationDeliveryAttempt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class NotificationDeliveryAttemptPersistenceAdapter implements NotificationDeliveryAttemptRepository {
    private final NotificationDeliveryAttemptJpaRepository repository;

    public NotificationDeliveryAttemptPersistenceAdapter(NotificationDeliveryAttemptJpaRepository repository) {
        this.repository = repository;
    }

    public NotificationDeliveryAttempt save(NotificationDeliveryAttempt attempt) {
        return repository.saveAndFlush(NotificationDeliveryAttemptEntity.fromDomain(attempt)).toDomain();
    }

    public Optional<NotificationDeliveryAttempt> findLatest(UUID notificationId) {
        return repository.findFirstByNotificationIdOrderByAttemptNumberDesc(notificationId)
            .map(NotificationDeliveryAttemptEntity::toDomain);
    }

    public Optional<NotificationDeliveryAttempt> findById(UUID id) {
        return repository.findById(id).map(NotificationDeliveryAttemptEntity::toDomain);
    }

    public List<NotificationDeliveryAttempt> findByNotificationId(UUID notificationId) {
        return repository.findByNotificationIdOrderByAttemptNumberAsc(notificationId).stream()
            .map(NotificationDeliveryAttemptEntity::toDomain).toList();
    }

    public long countByNotificationId(UUID notificationId) { return repository.countByNotificationId(notificationId); }
}
