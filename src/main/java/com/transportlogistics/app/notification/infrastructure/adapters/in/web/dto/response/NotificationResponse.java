package com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.NotificationSeverity;
import com.transportlogistics.app.notification.domain.model.NotificationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    UUID ruleId,
    UUID eventId,
    String eventType,
    NotificationChannel channel,
    String recipient,
    NotificationSeverity severity,
    String title,
    String message,
    UUID templateId,
    Integer templateVersion,
    NotificationStatus status,
    OffsetDateTime nextDeliveryAt,
    OffsetDateTime createdAt,
    OffsetDateTime sentAt,
    OffsetDateTime readAt,
    String failureReason,
    String relatedRoute,
    Long attemptCount,
    boolean terminalFailure,
    UUID parentNotificationId,
    int escalationLevel
) {}
