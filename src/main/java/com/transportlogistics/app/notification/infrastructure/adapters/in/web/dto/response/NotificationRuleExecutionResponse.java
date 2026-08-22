package com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.NotificationRuleExecutionOutcome;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationRuleExecutionResponse(
    UUID id, UUID eventId, String eventType, String aggregateType, UUID aggregateId, UUID ruleId,
    String resolvedRecipient, NotificationChannel channel, NotificationRuleExecutionOutcome outcome,
    UUID controllingNotificationId, String failureCode, String failureMessage,
    OffsetDateTime createdAt, OffsetDateTime completedAt
) {}
