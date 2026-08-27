package com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.NotificationSeverity;
import com.transportlogistics.app.notification.domain.model.RecipientType;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public record NotificationRuleResponse(
    UUID id,
    String name,
    String description,
    String eventType,
    NotificationChannel channel,
    RecipientType recipientType,
    String recipientValue,
    String templateCode,
    boolean quietHoursEnabled,
    LocalTime quietStartTime,
    LocalTime quietEndTime,
    Set<DayOfWeek> quietDays,
    int suppressionWindowMinutes,
    boolean escalationEnabled,
    Integer escalationDelayMinutes,
    RecipientType escalationRecipientType,
    String escalationRecipientValue,
    boolean enabled,
    NotificationSeverity severityThreshold,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
