package com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.NotificationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationDeliveryResponse(UUID notificationId, UUID ruleId, UUID eventId, String eventType,
                                           NotificationChannel channel, NotificationStatus status, long attemptCount,
                                           OffsetDateTime nextDeliveryAt, boolean terminalFailure,
                                           UUID parentNotificationId, int escalationLevel,
                                           OffsetDateTime createdAt, OffsetDateTime sentAt, String recipient) {}
