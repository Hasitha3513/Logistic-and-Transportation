package com.transportlogistics.app.notification.domain.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public record NotificationRulePolicy(
    boolean quietHoursEnabled,
    LocalTime quietStartTime,
    LocalTime quietEndTime,
    Set<DayOfWeek> quietDays,
    int suppressionWindowMinutes,
    boolean escalationEnabled,
    Integer escalationDelayMinutes,
    RecipientType escalationRecipientType,
    String escalationRecipientValue
) {
    public NotificationRulePolicy {
        quietDays = quietDays == null ? Set.of() : Set.copyOf(quietDays);
        if (suppressionWindowMinutes < 0 || suppressionWindowMinutes > 1440) {
            throw new IllegalArgumentException("Suppression window must be between 0 and 1440 minutes");
        }
        if (quietHoursEnabled) {
            if (quietStartTime == null || quietEndTime == null || quietDays.isEmpty()) {
                throw new IllegalArgumentException("Enabled quiet hours require start time, end time, and at least one day");
            }
            if (quietStartTime.equals(quietEndTime)) {
                throw new IllegalArgumentException("Quiet-hours start and end must be different");
            }
        } else {
            quietStartTime = null;
            quietEndTime = null;
            quietDays = Set.of();
        }
        if (escalationEnabled) {
            if (escalationDelayMinutes == null || escalationDelayMinutes < 0 || escalationDelayMinutes > 60) {
                throw new IllegalArgumentException("Escalation delay must be between 0 and 60 minutes");
            }
            if (escalationRecipientType != RecipientType.USER && escalationRecipientType != RecipientType.ROLE) {
                throw new IllegalArgumentException("Escalation recipient type must be USER or ROLE");
            }
            if (escalationRecipientValue == null || escalationRecipientValue.isBlank()) {
                throw new IllegalArgumentException("Escalation recipient value is required");
            }
            escalationRecipientValue = escalationRecipientValue.trim();
        } else {
            escalationDelayMinutes = null;
            escalationRecipientType = null;
            escalationRecipientValue = null;
        }
    }

    public NotificationRulePolicy(boolean quietHoursEnabled, LocalTime quietStartTime, LocalTime quietEndTime,
                                  Set<DayOfWeek> quietDays, int suppressionWindowMinutes) {
        this(quietHoursEnabled, quietStartTime, quietEndTime, quietDays, suppressionWindowMinutes,
            false, null, null, null);
    }

    public static NotificationRulePolicy defaults(String eventType) {
        return new NotificationRulePolicy(false, null, null, Set.of(),
            NotificationEventCatalogue.require(eventType).defaultSuppressionWindowMinutes(),
            false, null, null, null);
    }

    public void validateForChannel(NotificationChannel channel) {
        if (quietHoursEnabled && channel != NotificationChannel.EMAIL) {
            throw new IllegalArgumentException("Quiet hours may be enabled only for EMAIL rules");
        }
        if (escalationEnabled && channel != NotificationChannel.EMAIL) {
            throw new IllegalArgumentException("Escalation may be enabled only for EMAIL rules");
        }
    }
}
