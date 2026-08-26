package com.transportlogistics.app.notification.domain.model;

import java.util.List;
import java.util.Set;

public record NotificationEventDefinition(
    String eventType,
    String owningModule,
    NotificationSeverity defaultSeverity,
    Set<NotificationChannel> supportedChannels,
    String templateCode,
    Set<String> requiredVariables,
    Set<String> optionalVariables,
    int defaultSuppressionWindowMinutes,
    String milestoneMetadataKey
) {
    public NotificationEventDefinition {
        supportedChannels = Set.copyOf(supportedChannels);
        requiredVariables = Set.copyOf(requiredVariables);
        optionalVariables = Set.copyOf(optionalVariables);
        if (defaultSuppressionWindowMinutes < 0 || defaultSuppressionWindowMinutes > 1440) {
            throw new IllegalArgumentException("Default suppression window must be between 0 and 1440 minutes");
        }
    }

    public List<String> templateCodes() {
        return List.of(templateCode);
    }

    public boolean supports(NotificationChannel channel) {
        return supportedChannels.contains(channel);
    }

    public boolean allowsVariable(String variable) {
        return requiredVariables.contains(variable) || optionalVariables.contains(variable);
    }

    public String milestone(java.util.Map<String, String> metadata) {
        return milestoneMetadataKey == null || metadata == null ? null : metadata.get(milestoneMetadataKey);
    }
}
