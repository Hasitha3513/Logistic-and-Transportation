package com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.NotificationSeverity;
import com.transportlogistics.app.notification.domain.model.RecipientType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationRuleResponse(
    UUID id,
    String name,
    String description,
    String eventType,
    NotificationChannel channel,
    RecipientType recipientType,
    String recipientValue,
    boolean enabled,
    NotificationSeverity severityThreshold,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
