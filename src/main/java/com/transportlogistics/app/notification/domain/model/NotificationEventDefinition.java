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
    Set<String> optionalVariables
) {
    public NotificationEventDefinition {
        supportedChannels = Set.copyOf(supportedChannels);
        requiredVariables = Set.copyOf(requiredVariables);
        optionalVariables = Set.copyOf(optionalVariables);
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
}
