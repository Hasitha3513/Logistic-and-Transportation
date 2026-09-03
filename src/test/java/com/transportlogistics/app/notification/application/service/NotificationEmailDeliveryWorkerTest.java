package com.transportlogistics.app.notification.application.service;

import com.transportlogistics.app.notification.application.ports.out.NotificationRepository;
import com.transportlogistics.app.notification.application.ports.out.EmailNotificationSenderPort;
import com.transportlogistics.app.notification.application.ports.out.NotificationRuleExecutionRepository;
import com.transportlogistics.app.notification.FinalSendCustomerLinkIssuer;
import com.transportlogistics.app.notification.application.ports.in.NotificationEmailDeliveryClaimUseCase;
import com.transportlogistics.app.notification.application.ports.in.NotificationEscalationUseCase;
import com.transportlogistics.app.notification.domain.model.*;
import com.transportlogistics.app.notification.support.DeterministicEmailNotificationSender;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationEmailDeliveryWorkerTest {
    private final OffsetDateTime now = OffsetDateTime.parse("2026-08-21T10:00:00Z");

    @Test void acceptedSenderResultCompletesAttemptWithProviderEvidence() {
        NotificationRepository notifications = mock(NotificationRepository.class);
        NotificationEmailDeliveryClaimUseCase claims = mock(NotificationEmailDeliveryClaimUseCase.class);
        NotificationEscalationUseCase escalations = mock(NotificationEscalationUseCase.class);
        var sender = new DeterministicEmailNotificationSender(
            DeterministicEmailNotificationSender.Scenario.SUCCESS);
        Notification notification = pending();
        var attempt = NotificationDeliveryAttempt.start(notification.id(), 1, now, now);
        when(notifications.findDuePendingEmails(any(), anyInt())).thenReturn(List.of(notification));
        when(notifications.findFailedEmails(anyInt())).thenReturn(List.of());
        when(claims.claim(eq(notification.id()), any())).thenReturn(Optional.of(
            new NotificationEmailDeliveryClaimUseCase.ClaimedDelivery(notification, attempt)));
        var worker = worker(notifications, claims, escalations, sender);

        worker.processDue();

        verify(claims).completeSuccess(eq(notification.id()), eq(attempt.id()), any(), eq("provider-1"));
        org.assertj.core.api.Assertions.assertThat(sender.requests()).singleElement().satisfies(request ->
            org.assertj.core.api.Assertions.assertThat(request.idempotencyKey()).isEqualTo(notification.id() + ":1"));
    }

    @Test void typedRetryableAndNonRetryableResultsArePassedToDurableCompletion() {
        verifyFailure(DeterministicEmailNotificationSender.Scenario.RETRYABLE_FAILURE,
            EmailDeliveryErrorCategory.TIMEOUT);
        verifyFailure(DeterministicEmailNotificationSender.Scenario.NON_RETRYABLE_FAILURE,
            EmailDeliveryErrorCategory.INVALID_RECIPIENT);
    }

    @Test void interruptionIsRetryableAndPreservesThreadInterruptStatus() {
        NotificationRepository notifications = mock(NotificationRepository.class);
        NotificationEmailDeliveryClaimUseCase claims = mock(NotificationEmailDeliveryClaimUseCase.class);
        NotificationEscalationUseCase escalations = mock(NotificationEscalationUseCase.class);
        Notification notification = pending();
        var attempt = NotificationDeliveryAttempt.start(notification.id(), 1, now, now);
        when(notifications.findDuePendingEmails(any(), anyInt())).thenReturn(List.of(notification));
        when(notifications.findFailedEmails(anyInt())).thenReturn(List.of());
        when(claims.claim(eq(notification.id()), any())).thenReturn(Optional.of(
            new NotificationEmailDeliveryClaimUseCase.ClaimedDelivery(notification, attempt)));
        EmailNotificationSenderPort interruptedSender = request -> { throw new InterruptedException("stop"); };
        var worker = new NotificationEmailDeliveryWorker(notifications, claims, escalations, interruptedSender,
            Clock.fixed(now.toInstant(), ZoneOffset.UTC), "noreply@example.test", Duration.ofSeconds(30));

        try {
            worker.processDue();
            verify(claims).completeFailure(eq(notification.id()), eq(attempt.id()), any(),
                eq(EmailDeliveryErrorCategory.INTERRUPTION), eq("INTERRUPTED"), any());
            org.assertj.core.api.Assertions.assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @Test void selfServiceLinkExistsOnlyInTransientProviderRequest() {
        NotificationRepository notifications = mock(NotificationRepository.class);
        NotificationEmailDeliveryClaimUseCase claims = mock(NotificationEmailDeliveryClaimUseCase.class);
        NotificationEscalationUseCase escalations = mock(NotificationEscalationUseCase.class);
        NotificationRuleExecutionRepository executions = mock(NotificationRuleExecutionRepository.class);
        FinalSendCustomerLinkIssuer links = mock(FinalSendCustomerLinkIssuer.class);
        Notification persisted = Notification.createPending(null, UUID.randomUUID(), "DELIVERY_OUT_FOR_DELIVERY",
                NotificationChannel.EMAIL, "customer@example.test", NotificationSeverity.INFO, "Delivery update",
                "Track here: [[SELF_SERVICE_LINK]]", null, null, null, now.minusMinutes(1), null);
        var attempt = NotificationDeliveryAttempt.start(persisted.id(), 1, now, now);
        var execution = NotificationRuleExecution.completed(persisted.eventId(), persisted.eventType(),
                "DELIVERY_ORDER", UUID.randomUUID(), UUID.randomUUID(), persisted.recipient(), persisted.channel(),
                NotificationRuleExecutionOutcome.ACCEPTED, null, persisted.id(), null, null, now);
        when(notifications.findDuePendingEmails(any(), anyInt())).thenReturn(List.of(persisted));
        when(notifications.findFailedEmails(anyInt())).thenReturn(List.of());
        when(claims.claim(eq(persisted.id()), any())).thenReturn(Optional.of(
                new NotificationEmailDeliveryClaimUseCase.ClaimedDelivery(persisted, attempt)));
        when(executions.findByControllingNotificationId(persisted.id())).thenReturn(Optional.of(execution));
        when(links.issue(any())).thenReturn(new FinalSendCustomerLinkIssuer.IssuedLink(
                "https://track.example.test/track#access_token=transient-token"));
        java.util.concurrent.atomic.AtomicReference<EmailNotificationSenderPort.SendRequest> sent = new java.util.concurrent.atomic.AtomicReference<>();
        EmailNotificationSenderPort sender = request -> { sent.set(request); return EmailNotificationSenderPort.SendResult.accepted("provider-1"); };
        var worker = new NotificationEmailDeliveryWorker(notifications, claims, escalations, sender, null,
                executions, links, Clock.fixed(now.toInstant(), ZoneOffset.UTC), "noreply@example.test", Duration.ofSeconds(30));

        worker.processDue();

        org.assertj.core.api.Assertions.assertThat(persisted.message()).doesNotContain("transient-token");
        org.assertj.core.api.Assertions.assertThat(sent.get().plainTextBody()).contains("transient-token")
                .doesNotContain("[[SELF_SERVICE_LINK]]");
        verify(links).issue(argThat(request -> request.issuanceIdempotencyKey().equals(attempt.idempotencyKey())));
    }

    private void verifyFailure(DeterministicEmailNotificationSender.Scenario scenario,
                               EmailDeliveryErrorCategory expected) {
        NotificationRepository notifications = mock(NotificationRepository.class);
        NotificationEmailDeliveryClaimUseCase claims = mock(NotificationEmailDeliveryClaimUseCase.class);
        NotificationEscalationUseCase escalations = mock(NotificationEscalationUseCase.class);
        Notification notification = pending();
        var attempt = NotificationDeliveryAttempt.start(notification.id(), 1, now, now);
        when(notifications.findDuePendingEmails(any(), anyInt())).thenReturn(List.of(notification));
        when(notifications.findFailedEmails(anyInt())).thenReturn(List.of());
        when(claims.claim(eq(notification.id()), any())).thenReturn(Optional.of(
            new NotificationEmailDeliveryClaimUseCase.ClaimedDelivery(notification, attempt)));
        worker(notifications, claims, escalations, new DeterministicEmailNotificationSender(scenario)).processDue();
        verify(claims).completeFailure(eq(notification.id()), eq(attempt.id()), any(), eq(expected), any(), any());
    }

    private NotificationEmailDeliveryWorker worker(NotificationRepository notifications,
                                                    NotificationEmailDeliveryClaimUseCase claims,
                                                    NotificationEscalationUseCase escalations,
                                                    DeterministicEmailNotificationSender sender) {
        return new NotificationEmailDeliveryWorker(notifications, claims, escalations, sender,
            Clock.fixed(now.toInstant(), ZoneOffset.UTC), "noreply@example.test", Duration.ofSeconds(30));
    }

    private Notification pending() {
        return Notification.createPending(null, UUID.randomUUID(), "TRIP_DELAY_RECORDED", NotificationChannel.EMAIL,
            "ops@example.test", NotificationSeverity.WARNING, "Subject", "Body", null, null, null,
            now.minusMinutes(1), null);
    }
}
