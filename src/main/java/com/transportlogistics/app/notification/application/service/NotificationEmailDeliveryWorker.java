package com.transportlogistics.app.notification.application.service;

import com.transportlogistics.app.notification.application.ports.out.EmailNotificationSenderPort;
import com.transportlogistics.app.notification.application.ports.out.SmsNotificationSenderPort;
import com.transportlogistics.app.notification.application.ports.out.NotificationRepository;
import com.transportlogistics.app.notification.application.ports.in.NotificationEmailDeliveryClaimUseCase;
import com.transportlogistics.app.notification.application.ports.in.NotificationEscalationUseCase;
import com.transportlogistics.app.notification.domain.model.EmailDeliveryErrorCategory;
import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class NotificationEmailDeliveryWorker {
    private static final Logger log = LoggerFactory.getLogger(NotificationEmailDeliveryWorker.class);
    private static final int BATCH_SIZE = 50;
    private final NotificationRepository notifications;
    private final NotificationEmailDeliveryClaimUseCase claims;
    private final NotificationEscalationUseCase escalations;
    private final EmailNotificationSenderPort sender;
    private final SmsNotificationSenderPort smsSender;
    private final Clock clock;
    private final String from;
    private final Duration timeout;

    @Autowired
    public NotificationEmailDeliveryWorker(NotificationRepository notifications,
                                           NotificationEmailDeliveryClaimUseCase claims,
                                           NotificationEscalationUseCase escalations,
                                           EmailNotificationSenderPort sender,
                                           SmsNotificationSenderPort smsSender,
                                           Clock clock,
                                           @Value("${app.notification.email.from}") String from,
                                           @Value("${app.notification.email.read-timeout}") Duration timeout) {
        this.notifications = notifications; this.claims = claims; this.escalations = escalations;
        this.sender = sender; this.smsSender = smsSender; this.clock = clock; this.from = from; this.timeout = timeout;
    }

    public NotificationEmailDeliveryWorker(NotificationRepository notifications,
                                    NotificationEmailDeliveryClaimUseCase claims,
                                    NotificationEscalationUseCase escalations,
                                    EmailNotificationSenderPort sender,
                                    Clock clock, String from, Duration timeout) {
        this.notifications = notifications;
        this.claims = claims;
        this.escalations = escalations;
        this.sender = sender;
        this.smsSender = null;
        this.clock = clock;
        this.from = from;
        this.timeout = timeout;
    }

    public void processDue() {
        OffsetDateTime now = now();
        for (var candidate : notifications.findDuePendingEmails(now, BATCH_SIZE)) {
            claims.claim(candidate.id(), now).ifPresent(this::send);
        }
        for (var failed : notifications.findFailedEmails(BATCH_SIZE)) {
            try {
                escalations.escalateIfDue(failed.id(), now());
            } catch (RuntimeException exception) {
                log.error("Escalation processing failed for notification {}", failed.id(), exception);
            }
        }
    }

    private void send(NotificationEmailDeliveryClaimUseCase.ClaimedDelivery claim) {
        var notification = claim.notification();
        var attempt = claim.attempt();
        try {
            if (notification.channel() == NotificationChannel.SMS) sendSms(notification, attempt);
            else sendEmail(notification, attempt);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            claims.completeFailure(notification.id(), attempt.id(), now(), EmailDeliveryErrorCategory.INTERRUPTION,
                "INTERRUPTED", "EMAIL delivery interrupted");
        } catch (RuntimeException exception) {
            claims.completeFailure(notification.id(), attempt.id(), now(), EmailDeliveryErrorCategory.CONFIGURATION,
                "SENDER_FAILURE", "EMAIL sender failed without an accepted result");
        }
    }

    private void sendEmail(com.transportlogistics.app.notification.domain.model.Notification notification,
                           com.transportlogistics.app.notification.domain.model.NotificationDeliveryAttempt attempt)
        throws InterruptedException {
        var result = sender.send(new EmailNotificationSenderPort.SendRequest(notification.id(),
            attempt.idempotencyKey(), from, notification.recipient(), notification.title(),
            notification.message(), timeout));
        complete(notification.id(), attempt.id(), result.accepted(), result.providerMessageId(),
            result.errorCategory(), result.errorCode(), result.errorMessage());
    }

    private void sendSms(com.transportlogistics.app.notification.domain.model.Notification notification,
                         com.transportlogistics.app.notification.domain.model.NotificationDeliveryAttempt attempt)
        throws InterruptedException {
        if (smsSender == null) {
            claims.completeFailure(notification.id(), attempt.id(), now(), EmailDeliveryErrorCategory.CONFIGURATION,
                "SMS_SENDER_UNAVAILABLE", "SMS sender is unavailable");
            return;
        }
        var result = smsSender.send(new SmsNotificationSenderPort.SendRequest(notification.id(),
            attempt.idempotencyKey(), notification.recipient(), notification.message(), timeout));
        complete(notification.id(), attempt.id(), result.accepted(), result.providerMessageId(),
            result.errorCategory(), result.errorCode(), result.errorMessage());
    }

    private void complete(java.util.UUID notificationId, java.util.UUID attemptId, boolean accepted,
                          String providerMessageId, EmailDeliveryErrorCategory category,
                          String errorCode, String errorMessage) {
        OffsetDateTime completedAt = now();
        if (accepted) claims.completeSuccess(notificationId, attemptId, completedAt, providerMessageId);
        else claims.completeFailure(notificationId, attemptId, completedAt, category, errorCode, errorMessage);
    }

    private OffsetDateTime now() { return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC); }
}
