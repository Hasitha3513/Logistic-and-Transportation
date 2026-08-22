package com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.notification.domain.model.EmailDeliveryErrorCategory;
import com.transportlogistics.app.notification.domain.model.NotificationDeliveryAttemptState;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationDeliveryAttemptResponse(UUID id, int attemptNumber,
                                                  NotificationDeliveryAttemptState state, OffsetDateTime dueAt,
                                                  OffsetDateTime startedAt, OffsetDateTime completedAt,
                                                  EmailDeliveryErrorCategory errorCategory, String errorCode,
                                                  String errorMessage, String providerMessageId) {}
