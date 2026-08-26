package com.transportlogistics.app.notification.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.notification.application.ports.out.NotificationRepository;
import com.transportlogistics.app.notification.domain.model.Notification;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.transportlogistics.app.notification.domain.model.NotificationStatus;

@Component
public class NotificationPersistenceAdapter implements NotificationRepository {
    private final NotificationJpaRepository jpaRepository;

    public NotificationPersistenceAdapter(NotificationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Notification save(Notification notification) {
        NotificationEntity entity = NotificationEntity.fromDomain(notification);
        return jpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return jpaRepository.findById(id).map(NotificationEntity::toDomain);
    }

    @Override
    public boolean existsByEventIdAndRuleIdAndRecipient(UUID eventId, UUID ruleId, String recipient) {
        return jpaRepository.existsByEventIdAndRuleIdAndRecipient(eventId, ruleId, recipient);
    }

    @Override
    public List<Notification> findByRecipientsOrderByCreatedAtDesc(Collection<String> recipients, int limit) {
        if (recipients == null || recipients.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findByRecipients(recipients, PageRequest.of(0, limit)).stream()
            .map(NotificationEntity::toDomain)
            .toList();
    }

    @Override
    public long countUnreadByRecipients(Collection<String> recipients) {
        if (recipients == null || recipients.isEmpty()) {
            return 0;
        }
        return jpaRepository.countUnreadByRecipients(recipients);
    }

    @Override
    public int markAllAsReadForRecipients(Collection<String> recipients) {
        if (recipients == null || recipients.isEmpty()) {
            return 0;
        }
        return jpaRepository.markAllAsReadForRecipients(recipients, OffsetDateTime.now());
    }

    public Optional<Notification> findByIdForUpdate(UUID id) {
        return jpaRepository.findByIdForUpdate(id).map(NotificationEntity::toDomain);
    }

    public List<Notification> findDuePendingEmails(OffsetDateTime now, int limit) {
        return jpaRepository.findDuePendingEmails(now, PageRequest.of(0, limit)).stream()
            .map(NotificationEntity::toDomain).toList();
    }

    public List<Notification> findFailedEmails(int limit) {
        return jpaRepository.findFailedEmails(PageRequest.of(0, limit)).stream()
            .map(NotificationEntity::toDomain).toList();
    }

    public List<Notification> findDeliveries(NotificationStatus status, String eventType,
                                             OffsetDateTime from, OffsetDateTime to, int limit) {
        String normalized = eventType == null || eventType.isBlank() ? null : eventType.trim().toUpperCase();
        return jpaRepository.findDeliveries(status, normalized, from, to, PageRequest.of(0, limit)).stream()
            .map(NotificationEntity::toDomain).toList();
    }

    public boolean existsByParentNotificationIdAndRecipient(UUID parentNotificationId, String recipient) {
        return jpaRepository.existsByParentNotificationIdAndRecipient(parentNotificationId, recipient);
    }
}
