package com.transportlogistics.app.notification.application.ports.in;

import com.transportlogistics.app.notification.domain.model.*;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface NotificationEmailDeliveryClaimUseCase {
    Optional<ClaimedDelivery> claim(UUID notificationId, OffsetDateTime now);
    void completeSuccess(UUID notificationId, UUID attemptId, OffsetDateTime completedAt, String providerMessageId);
    boolean completeFailure(UUID notificationId, UUID attemptId, OffsetDateTime completedAt,
                            EmailDeliveryErrorCategory category, String errorCode, String errorMessage);

    record ClaimedDelivery(Notification notification, NotificationDeliveryAttempt attempt) {}
}
