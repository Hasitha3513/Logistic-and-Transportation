package com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.request;

import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.NotificationSeverity;
import com.transportlogistics.app.notification.domain.model.RecipientType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateNotificationRuleRequest(
    @NotBlank(message = "Rule name is required")
    @Size(max = 128, message = "Rule name must not exceed 128 characters")
    String name,

    @Size(max = 255, message = "Description must not exceed 255 characters")
    String description,

    @NotBlank(message = "Event type is required")
    @Size(max = 64, message = "Event type must not exceed 64 characters")
    String eventType,

    @NotNull(message = "Notification channel is required")
    NotificationChannel channel,

    @NotNull(message = "Recipient type is required")
    RecipientType recipientType,

    @NotBlank(message = "Recipient value is required")
    @Size(max = 128, message = "Recipient value must not exceed 128 characters")
    String recipientValue,

    @Size(max = 64, message = "Template code must not exceed 64 characters")
    String templateCode,

    Boolean enabled,

    NotificationSeverity severityThreshold
) {}
