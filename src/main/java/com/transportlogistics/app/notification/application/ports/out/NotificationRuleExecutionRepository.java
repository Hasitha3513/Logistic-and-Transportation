package com.transportlogistics.app.notification.application.ports.out;

import com.transportlogistics.app.notification.domain.model.NotificationRuleExecution;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRuleExecutionRepository {
    NotificationRuleExecution save(NotificationRuleExecution execution);
    boolean existsByExecutionKey(String executionKey);
    Optional<NotificationRuleExecution> findLatestAccepted(String suppressionKey, OffsetDateTime after);
    List<NotificationRuleExecution> findRecent(UUID ruleId, UUID eventId, int limit);
    Optional<NotificationRuleExecution> findByControllingNotificationId(UUID notificationId);
}
