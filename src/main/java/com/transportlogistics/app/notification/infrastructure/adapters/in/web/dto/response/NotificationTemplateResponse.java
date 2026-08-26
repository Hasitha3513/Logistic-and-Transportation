package com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.notification.domain.model.NotificationChannel;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationTemplateResponse(
    UUID id,
    String code,
    String name,
    String eventType,
    NotificationChannel channel,
    String subject,
    String body,
    int version,
    boolean active,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
