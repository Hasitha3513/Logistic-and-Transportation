package com.transportlogistics.app.notification.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.NotificationRuleExecution;
import com.transportlogistics.app.notification.domain.model.NotificationRuleExecutionOutcome;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_rule_execution")
public class NotificationRuleExecutionEntity {
    @Id private UUID id;
    @Column(name = "execution_key", nullable = false, unique = true, length = 64) private String executionKey;
    @Column(name = "event_id", nullable = false) private UUID eventId;
    @Column(name = "event_type", nullable = false, length = 64) private String eventType;
    @Column(name = "aggregate_type", nullable = false, length = 64) private String aggregateType;
    @Column(name = "aggregate_id", nullable = false) private UUID aggregateId;
    @Column(name = "rule_id", nullable = false) private UUID ruleId;
    @Column(name = "resolved_recipient", length = 320) private String resolvedRecipient;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private NotificationChannel channel;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private NotificationRuleExecutionOutcome outcome;
    @Column(name = "suppression_key", length = 64) private String suppressionKey;
    @Column(name = "controlling_notification_id") private UUID controllingNotificationId;
    @Column(name = "failure_code", length = 64) private String failureCode;
    @Column(name = "failure_message", length = 500) private String failureMessage;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "completed_at", nullable = false) private OffsetDateTime completedAt;

    protected NotificationRuleExecutionEntity() {}

    static NotificationRuleExecutionEntity fromDomain(NotificationRuleExecution value) {
        NotificationRuleExecutionEntity entity = new NotificationRuleExecutionEntity();
        entity.id = value.id(); entity.executionKey = value.executionKey(); entity.eventId = value.eventId();
        entity.eventType = value.eventType(); entity.aggregateType = value.aggregateType(); entity.aggregateId = value.aggregateId();
        entity.ruleId = value.ruleId(); entity.resolvedRecipient = value.resolvedRecipient(); entity.channel = value.channel();
        entity.outcome = value.outcome(); entity.suppressionKey = value.suppressionKey();
        entity.controllingNotificationId = value.controllingNotificationId(); entity.failureCode = value.failureCode();
        entity.failureMessage = value.failureMessage(); entity.createdAt = value.createdAt(); entity.completedAt = value.completedAt();
        return entity;
    }

    NotificationRuleExecution toDomain() {
        return new NotificationRuleExecution(id, executionKey, eventId, eventType, aggregateType, aggregateId,
            ruleId, resolvedRecipient, channel, outcome, suppressionKey, controllingNotificationId,
            failureCode, failureMessage, createdAt, completedAt);
    }
}
