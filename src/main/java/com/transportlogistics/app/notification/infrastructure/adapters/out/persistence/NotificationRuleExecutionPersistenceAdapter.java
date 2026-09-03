package com.transportlogistics.app.notification.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.notification.application.ports.out.NotificationRuleExecutionRepository;
import com.transportlogistics.app.notification.domain.model.NotificationRuleExecution;
import com.transportlogistics.app.notification.domain.model.NotificationRuleExecutionOutcome;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class NotificationRuleExecutionPersistenceAdapter implements NotificationRuleExecutionRepository {
    private final NotificationRuleExecutionJpaRepository repository;

    public NotificationRuleExecutionPersistenceAdapter(NotificationRuleExecutionJpaRepository repository) {
        this.repository = repository;
    }

    @Override public NotificationRuleExecution save(NotificationRuleExecution execution) {
        return repository.save(NotificationRuleExecutionEntity.fromDomain(execution)).toDomain();
    }
    @Override public boolean existsByExecutionKey(String executionKey) { return repository.existsByExecutionKey(executionKey); }
    @Override public Optional<NotificationRuleExecution> findLatestAccepted(String suppressionKey, OffsetDateTime after) {
        return repository.findFirstBySuppressionKeyAndOutcomeAndCompletedAtAfterOrderByCompletedAtDesc(
            suppressionKey, NotificationRuleExecutionOutcome.ACCEPTED, after).map(NotificationRuleExecutionEntity::toDomain);
    }
    @Override public List<NotificationRuleExecution> findRecent(UUID ruleId, UUID eventId, int limit) {
        return repository.findRecent(ruleId, eventId, PageRequest.of(0, Math.max(1, Math.min(limit, 200))))
            .stream().map(NotificationRuleExecutionEntity::toDomain).toList();
    }
    @Override public Optional<NotificationRuleExecution> findByControllingNotificationId(UUID notificationId) {
        return repository.findByControllingNotificationId(notificationId).map(NotificationRuleExecutionEntity::toDomain);
    }
}
