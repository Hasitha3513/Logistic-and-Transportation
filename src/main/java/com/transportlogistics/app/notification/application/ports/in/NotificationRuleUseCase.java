package com.transportlogistics.app.notification.application.ports.in;

import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.NotificationRule;
import com.transportlogistics.app.notification.domain.model.NotificationSeverity;
import com.transportlogistics.app.notification.domain.model.RecipientType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public interface NotificationRuleUseCase {
    NotificationRule createRule(CreateRuleCommand command);
    NotificationRule updateRule(UUID id, UpdateRuleCommand command);
    NotificationRule enableRule(UUID id);
    NotificationRule disableRule(UUID id);
    void deleteRule(UUID id);
    Optional<NotificationRule> getRule(UUID id);
    List<NotificationRule> listRules();

    record CreateRuleCommand(
        String name,
        String description,
        String eventType,
        NotificationChannel channel,
        RecipientType recipientType,
        String recipientValue,
        String templateCode,
        Boolean quietHoursEnabled,
        LocalTime quietStartTime,
        LocalTime quietEndTime,
        Set<DayOfWeek> quietDays,
        Integer suppressionWindowMinutes,
        Boolean escalationEnabled,
        Integer escalationDelayMinutes,
        RecipientType escalationRecipientType,
        String escalationRecipientValue,
        boolean enabled,
        NotificationSeverity severityThreshold
    ) {
        public CreateRuleCommand(String name, String description, String eventType, NotificationChannel channel,
                                 RecipientType recipientType, String recipientValue, String templateCode,
                                 Boolean quietHoursEnabled, LocalTime quietStartTime, LocalTime quietEndTime,
                                 Set<DayOfWeek> quietDays, Integer suppressionWindowMinutes,
                                 boolean enabled, NotificationSeverity severityThreshold) {
            this(name, description, eventType, channel, recipientType, recipientValue, templateCode,
                quietHoursEnabled, quietStartTime, quietEndTime, quietDays, suppressionWindowMinutes,
                null, null, null, null, enabled, severityThreshold);
        }

        public CreateRuleCommand(String name, String description, String eventType, NotificationChannel channel,
                                 RecipientType recipientType, String recipientValue, String templateCode,
                                 boolean enabled, NotificationSeverity severityThreshold) {
            this(name, description, eventType, channel, recipientType, recipientValue, templateCode,
                null, null, null, null, null, null, null, null, null, enabled, severityThreshold);
        }
    }

    record UpdateRuleCommand(
        String name,
        String description,
        String eventType,
        NotificationChannel channel,
        RecipientType recipientType,
        String recipientValue,
        String templateCode,
        Boolean quietHoursEnabled,
        LocalTime quietStartTime,
        LocalTime quietEndTime,
        Set<DayOfWeek> quietDays,
        Integer suppressionWindowMinutes,
        Boolean escalationEnabled,
        Integer escalationDelayMinutes,
        RecipientType escalationRecipientType,
        String escalationRecipientValue,
        boolean enabled,
        NotificationSeverity severityThreshold
    ) {
        public UpdateRuleCommand(String name, String description, String eventType, NotificationChannel channel,
                                 RecipientType recipientType, String recipientValue, String templateCode,
                                 Boolean quietHoursEnabled, LocalTime quietStartTime, LocalTime quietEndTime,
                                 Set<DayOfWeek> quietDays, Integer suppressionWindowMinutes,
                                 boolean enabled, NotificationSeverity severityThreshold) {
            this(name, description, eventType, channel, recipientType, recipientValue, templateCode,
                quietHoursEnabled, quietStartTime, quietEndTime, quietDays, suppressionWindowMinutes,
                null, null, null, null, enabled, severityThreshold);
        }

        public UpdateRuleCommand(String name, String description, String eventType, NotificationChannel channel,
                                 RecipientType recipientType, String recipientValue, String templateCode,
                                 boolean enabled, NotificationSeverity severityThreshold) {
            this(name, description, eventType, channel, recipientType, recipientValue, templateCode,
                null, null, null, null, null, null, null, null, null, enabled, severityThreshold);
        }
    }
}
