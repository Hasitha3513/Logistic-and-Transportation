package com.transportlogistics.app.notification.application.service;

import com.transportlogistics.app.notification.application.ports.out.NotificationDeliveryAttemptRepository;
import com.transportlogistics.app.notification.application.ports.out.NotificationRepository;
import com.transportlogistics.app.notification.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationEmailDeliveryClaimServiceTest {
    private NotificationRepository notifications;
    private NotificationDeliveryAttemptRepository attempts;
    private NotificationEmailDeliveryClaimService service;
    private final OffsetDateTime now = OffsetDateTime.parse("2026-08-21T10:00:00Z");

    @BeforeEach void setUp() {
        notifications = mock(NotificationRepository.class); attempts = mock(NotificationDeliveryAttemptRepository.class);
        service = new NotificationEmailDeliveryClaimService(notifications, attempts);
        when(attempts.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(notifications.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test void firstClaimCreatesAttemptBeforeReturningWork() {
        Notification pending = pending(null);
        when(notifications.findByIdForUpdate(pending.id())).thenReturn(Optional.of(pending));
        when(attempts.findLatest(pending.id())).thenReturn(Optional.empty());
        var claim = service.claim(pending.id(), now).orElseThrow();
        assertThat(claim.attempt().attemptNumber()).isEqualTo(1);
        assertThat(claim.attempt().state()).isEqualTo(NotificationDeliveryAttemptState.IN_PROGRESS);
        verify(attempts).save(claim.attempt());
    }

    @Test void activeClaimCannotBeClaimedTwiceButStaleClaimReusesAttemptAndKey() {
        Notification pending = pending(null);
        var active = NotificationDeliveryAttempt.start(pending.id(), 1, now.minusMinutes(1), now.minusMinutes(1));
        when(notifications.findByIdForUpdate(pending.id())).thenReturn(Optional.of(pending));
        when(attempts.findLatest(pending.id())).thenReturn(Optional.of(active));
        assertThat(service.claim(pending.id(), now)).isEmpty();
        var recovered = service.claim(pending.id(), now.plusMinutes(6)).orElseThrow().attempt();
        assertThat(recovered.id()).isEqualTo(active.id());
        assertThat(recovered.idempotencyKey()).isEqualTo(active.idempotencyKey());
    }

    @Test void retryableFailuresUseExactScheduleAndThirdFailureIsTerminal() {
        Notification pending = pending(null);
        var first = NotificationDeliveryAttempt.start(pending.id(), 1, now, now);
        when(notifications.findByIdForUpdate(pending.id())).thenReturn(Optional.of(pending));
        when(attempts.findById(first.id())).thenReturn(Optional.of(first));
        assertThat(service.completeFailure(pending.id(), first.id(), now, EmailDeliveryErrorCategory.TIMEOUT,
            "TIMEOUT", "timed out")).isFalse();
        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).save(saved.capture());
        assertThat(saved.getValue().nextDeliveryAt()).isEqualTo(now.plusMinutes(1));

        reset(notifications); when(notifications.findByIdForUpdate(pending.id())).thenReturn(Optional.of(pending));
        var third = NotificationDeliveryAttempt.start(pending.id(), 3, now, now);
        when(attempts.findById(third.id())).thenReturn(Optional.of(third));
        assertThat(service.completeFailure(pending.id(), third.id(), now, EmailDeliveryErrorCategory.TIMEOUT,
            "TIMEOUT", "timed out")).isTrue();
        verify(notifications).save(argThat(n -> n.status() == NotificationStatus.FAILED && n.nextDeliveryAt() == null));
    }

    @Test void acceptedResultMarksSentAndClearsSchedule() {
        Notification pending = pending(now.plusMinutes(1));
        var attempt = NotificationDeliveryAttempt.start(pending.id(), 1, now, now);
        when(notifications.findByIdForUpdate(pending.id())).thenReturn(Optional.of(pending));
        when(attempts.findById(attempt.id())).thenReturn(Optional.of(attempt));
        service.completeSuccess(pending.id(), attempt.id(), now, "provider-id");
        verify(attempts).save(argThat(a -> a.state() == NotificationDeliveryAttemptState.SUCCEEDED
            && "provider-id".equals(a.providerMessageId())));
        verify(notifications).save(argThat(n -> n.status() == NotificationStatus.SENT && n.nextDeliveryAt() == null));
    }

    @Test void secondRetryableFailureSchedulesAttemptThreeAfterExactlyTwoMinutes() {
        Notification pending = pending(null);
        var second = NotificationDeliveryAttempt.start(pending.id(), 2, now, now);
        when(notifications.findByIdForUpdate(pending.id())).thenReturn(Optional.of(pending));
        when(attempts.findById(second.id())).thenReturn(Optional.of(second));
        service.completeFailure(pending.id(), second.id(), now, EmailDeliveryErrorCategory.HTTP_429,
            "HTTP_429", "throttled");
        verify(notifications).save(argThat(n -> now.plusMinutes(2).equals(n.nextDeliveryAt())
            && n.status() == NotificationStatus.PENDING));
    }

    @Test void nonRetryableFailureTerminatesOnFirstAttempt() {
        Notification pending = pending(null);
        var first = NotificationDeliveryAttempt.start(pending.id(), 1, now, now);
        when(notifications.findByIdForUpdate(pending.id())).thenReturn(Optional.of(pending));
        when(attempts.findById(first.id())).thenReturn(Optional.of(first));
        assertThat(service.completeFailure(pending.id(), first.id(), now,
            EmailDeliveryErrorCategory.AUTHENTICATION, "AUTH", "authentication rejected")).isTrue();
        verify(notifications).save(argThat(n -> n.status() == NotificationStatus.FAILED
            && n.nextDeliveryAt() == null));
    }

    @Test void failedThirdAttemptCanNeverCreateAttemptFour() {
        Notification pending = pending(null);
        var third = NotificationDeliveryAttempt.start(pending.id(), 3, now.minusMinutes(1), now.minusMinutes(1))
            .fail(now, EmailDeliveryErrorCategory.TIMEOUT, "TIMEOUT", "timed out");
        when(notifications.findByIdForUpdate(pending.id())).thenReturn(Optional.of(pending));
        when(attempts.findLatest(pending.id())).thenReturn(Optional.of(third));

        assertThat(service.claim(pending.id(), now.plusMinutes(10))).isEmpty();
        verify(attempts, never()).save(any());
    }

    private Notification pending(OffsetDateTime due) {
        OffsetDateTime created = now.minusMinutes(10);
        return Notification.createPending(null, UUID.randomUUID(), "TRIP_DELAY_RECORDED", NotificationChannel.EMAIL,
            "ops@example.test", NotificationSeverity.WARNING, "Subject", "Body", null, null, null, created, due);
    }
}
