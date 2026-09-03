package com.transportlogistics.app.notification.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.notification.application.ports.out.NotificationRepository;
import com.transportlogistics.app.notification.domain.model.Notification;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
                                             OffsetDateTime from, OffsetDateTime to, String aggregateType,
                                             UUID aggregateId, int limit) {
        String normalized = eventType == null || eventType.isBlank() ? null : eventType.trim().toUpperCase();
        String normalizedAggregate = aggregateType == null || aggregateType.isBlank()
            ? null : aggregateType.trim().toUpperCase();
        Specification<NotificationEntity> filters = Specification.where(null);
        if (status != null) {
            filters = filters.and((root, query, builder) -> builder.equal(root.get("status"), status));
        }
        if (normalized != null) {
            filters = filters.and((root, query, builder) -> builder.equal(root.get("eventType"), normalized));
        }
        if (from != null) {
            filters = filters.and((root, query, builder) -> builder.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            filters = filters.and((root, query, builder) -> builder.lessThanOrEqualTo(root.get("createdAt"), to));
        }
        if (normalizedAggregate != null) {
            filters = filters.and((root, query, builder) -> {
                var execution = query.subquery(UUID.class);
                var executionRoot = execution.from(NotificationRuleExecutionEntity.class);
                execution.select(executionRoot.get("id"));
                var predicate = builder.and(
                    builder.equal(executionRoot.get("controllingNotificationId"), root.get("id")),
                    builder.equal(executionRoot.get("aggregateType"), normalizedAggregate));
                if (aggregateId != null) {
                    predicate = builder.and(predicate,
                        builder.equal(executionRoot.get("aggregateId"), aggregateId));
                }
                execution.where(predicate);
                return builder.exists(execution);
            });
        }
        return jpaRepository.findAll(filters,
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent().stream()
            .map(NotificationEntity::toDomain).toList();
    }

    public List<Notification> findDeliveries(NotificationStatus status, String eventType,
                                             OffsetDateTime from, OffsetDateTime to, int limit) {
        return findDeliveries(status, eventType, from, to, null, null, limit);
    }

    public boolean existsByParentNotificationIdAndRecipient(UUID parentNotificationId, String recipient) {
        return jpaRepository.existsByParentNotificationIdAndRecipient(parentNotificationId, recipient);
    }
}
