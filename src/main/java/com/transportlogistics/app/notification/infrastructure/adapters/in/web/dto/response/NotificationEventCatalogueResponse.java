package com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.NotificationSeverity;

import java.util.List;
import java.util.Set;

public record NotificationEventCatalogueResponse(
    String eventType,
    String owningModule,
    NotificationSeverity defaultSeverity,
    Set<NotificationChannel> supportedChannels,
    List<String> templateCodes,
    Set<String> requiredVariables,
    Set<String> optionalVariables
) {
}
