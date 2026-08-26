package com.transportlogistics.app.notification.application.service;

import com.transportlogistics.app.notification.application.ports.out.NotificationDeliveryAttemptRepository;
import com.transportlogistics.app.notification.application.ports.out.NotificationRepository;
import com.transportlogistics.app.notification.application.ports.in.NotificationEmailDeliveryClaimUseCase;
import com.transportlogistics.app.notification.domain.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationEmailDeliveryClaimService implements NotificationEmailDeliveryClaimUseCase {
    static final Duration STALE_CLAIM_AFTER = Duration.ofMinutes(5);
    private final NotificationRepository notifications;
    private final NotificationDeliveryAttemptRepository attempts;

    public NotificationEmailDeliveryClaimService(NotificationRepository notifications,
                                                  NotificationDeliveryAttemptRepository attempts) {
        this.notifications = notifications;
        this.attempts = attempts;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<ClaimedDelivery> claim(UUID notificationId, OffsetDateTime now) {
        Notification notification = notifications.findByIdForUpdate(notificationId).orElse(null);
        if (notification == null || notification.channel() != NotificationChannel.EMAIL
            || notification.status() != NotificationStatus.PENDING
            || (notification.nextDeliveryAt() != null && notification.nextDeliveryAt().isAfter(now))) {
            return Optional.empty();
        }

        NotificationDeliveryAttempt latest = attempts.findLatest(notificationId).orElse(null);
        NotificationDeliveryAttempt claimed;
        if (latest == null) {
            OffsetDateTime dueAt = notification.nextDeliveryAt() == null ? notification.createdAt() : notification.nextDeliveryAt();
            claimed = NotificationDeliveryAttempt.start(notificationId, 1, dueAt, now);
        } else if (latest.state() == NotificationDeliveryAttemptState.IN_PROGRESS) {
            if (latest.startedAt() == null || latest.startedAt().isAfter(now.minus(STALE_CLAIM_AFTER))) {
                return Optional.empty();
            }
            claimed = latest.restart(now);
        } else if (latest.state() == NotificationDeliveryAttemptState.FAILED
            && latest.attemptNumber() < NotificationEmailRetryPolicy.MAX_ATTEMPTS) {
            claimed = NotificationDeliveryAttempt.start(notificationId, latest.attemptNumber() + 1,
                notification.nextDeliveryAt() == null ? now : notification.nextDeliveryAt(), now);
        } else {
            return Optional.empty();
        }
        return Optional.of(new ClaimedDelivery(notification, attempts.save(claimed)));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeSuccess(UUID notificationId, UUID attemptId, OffsetDateTime completedAt,
                                String providerMessageId) {
        Notification notification = notifications.findByIdForUpdate(notificationId).orElseThrow();
        NotificationDeliveryAttempt attempt = attempts.findById(attemptId).orElseThrow();
        if (notification.status() != NotificationStatus.PENDING
            || attempt.state() != NotificationDeliveryAttemptState.IN_PROGRESS) return;
        attempts.save(attempt.succeed(completedAt, providerMessageId));
        notifications.save(notification.markSent(completedAt));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean completeFailure(UUID notificationId, UUID attemptId, OffsetDateTime completedAt,
                                   EmailDeliveryErrorCategory category, String errorCode, String errorMessage) {
        Notification notification = notifications.findByIdForUpdate(notificationId).orElseThrow();
        NotificationDeliveryAttempt attempt = attempts.findById(attemptId).orElseThrow();
        if (notification.status() != NotificationStatus.PENDING
            || attempt.state() != NotificationDeliveryAttemptState.IN_PROGRESS) return false;
        NotificationDeliveryAttempt failed = attempts.save(attempt.fail(completedAt, category, errorCode, errorMessage));
        boolean terminal = !category.retryable()
            || attempt.attemptNumber() == NotificationEmailRetryPolicy.MAX_ATTEMPTS;
        if (terminal) {
            String reason = category.name() + (failed.errorCode() == null ? "" : ": " + failed.errorCode())
                + (failed.errorMessage() == null ? "" : " - " + failed.errorMessage());
            notifications.save(notification.markFailed(reason));
        } else {
            notifications.save(notification.scheduleRetry(completedAt.plus(
                NotificationEmailRetryPolicy.delayAfterFailure(attempt.attemptNumber()))));
        }
        return terminal;
    }

}
