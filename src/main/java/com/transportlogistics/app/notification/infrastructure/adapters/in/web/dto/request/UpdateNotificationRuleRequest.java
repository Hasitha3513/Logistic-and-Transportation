package com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.request;

import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.NotificationSeverity;
import com.transportlogistics.app.notification.domain.model.RecipientType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public record UpdateNotificationRuleRequest(
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

    Boolean quietHoursEnabled,
    LocalTime quietStartTime,
    LocalTime quietEndTime,
    Set<DayOfWeek> quietDays,
    @Min(value = 0, message = "Suppression window must be at least 0 minutes")
    @Max(value = 1440, message = "Suppression window must not exceed 1440 minutes")
    Integer suppressionWindowMinutes,
    Boolean escalationEnabled,
    @Min(value = 0, message = "Escalation delay must be at least 0 minutes")
    @Max(value = 60, message = "Escalation delay must not exceed 60 minutes")
    Integer escalationDelayMinutes,
    RecipientType escalationRecipientType,
    @Size(max = 128, message = "Escalation recipient value must not exceed 128 characters")
    String escalationRecipientValue,

    Boolean enabled,

    NotificationSeverity severityThreshold
) {
    public UpdateNotificationRuleRequest(String name, String description, String eventType,
                                         NotificationChannel channel, RecipientType recipientType,
                                         String recipientValue, String templateCode, Boolean enabled,
                                         NotificationSeverity severityThreshold) {
        this(name, description, eventType, channel, recipientType, recipientValue, templateCode,
            null, null, null, null, null, null, null, null, null, enabled, severityThreshold);
    }
}
