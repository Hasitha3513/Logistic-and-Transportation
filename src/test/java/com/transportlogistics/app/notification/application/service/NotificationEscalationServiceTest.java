package com.transportlogistics.app.notification.application.service;

import com.transportlogistics.app.notification.application.ports.out.*;
import com.transportlogistics.app.notification.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationEscalationServiceTest {
    private NotificationRepository notifications;
    private NotificationDeliveryAttemptRepository attempts;
    private NotificationRulePolicyRepository policies;
    private NotificationRecipientDirectoryPort directory;
    private NotificationEscalationService service;
    private final OffsetDateTime now = OffsetDateTime.parse("2026-08-21T10:00:00Z");

    @BeforeEach void setUp() {
        notifications = mock(NotificationRepository.class); attempts = mock(NotificationDeliveryAttemptRepository.class);
        policies = mock(NotificationRulePolicyRepository.class); directory = mock(NotificationRecipientDirectoryPort.class);
        service = new NotificationEscalationService(notifications, attempts, policies,
            new NotificationRecipientResolver(directory));
        when(notifications.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test void roleFallbackCreatesOneLinkedInAppChildPerDistinctActiveUserWhenDue() {
        UUID ruleId = UUID.randomUUID(); Notification failed = failed(ruleId);
        when(notifications.findByIdForUpdate(failed.id())).thenReturn(Optional.of(failed));
        when(policies.findByRuleId(ruleId)).thenReturn(Optional.of(policy(5, RecipientType.ROLE, "OPERATIONS")));
        when(attempts.findLatest(failed.id())).thenReturn(Optional.of(failedAttempt(failed.id(), now.minusMinutes(5))));
        when(directory.activeRoleExists("OPERATIONS")).thenReturn(true);
        when(directory.findActiveRoleMembers("OPERATIONS")).thenReturn(List.of(
            new NotificationRecipientDirectoryPort.RecipientUser("alice", "a@example.test"),
            new NotificationRecipientDirectoryPort.RecipientUser("alice", "a@example.test"),
            new NotificationRecipientDirectoryPort.RecipientUser("bob", "b@example.test")));

        service.escalateIfDue(failed.id(), now);

        var captor = org.mockito.ArgumentCaptor.forClass(Notification.class);
        verify(notifications, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Notification::recipient).containsExactly("alice", "bob");
        assertThat(captor.getAllValues()).allSatisfy(child -> {
            assertThat(child.channel()).isEqualTo(NotificationChannel.IN_APP);
            assertThat(child.parentNotificationId()).isEqualTo(failed.id());
            assertThat(child.escalationLevel()).isEqualTo(1);
            assertThat(child.message()).doesNotContain("password", "credential");
        });
    }

    @Test void escalationDoesNotRunBeforeDelayOrWithoutTerminalFailure() {
        UUID ruleId = UUID.randomUUID(); Notification failed = failed(ruleId);
        when(notifications.findByIdForUpdate(failed.id())).thenReturn(Optional.of(failed));
        when(policies.findByRuleId(ruleId)).thenReturn(Optional.of(policy(10, RecipientType.USER, "alice")));
        when(attempts.findLatest(failed.id())).thenReturn(Optional.of(failedAttempt(failed.id(), now.minusMinutes(9))));
        service.escalateIfDue(failed.id(), now);
        verify(notifications, never()).save(any());
    }

    @Test void zeroRoleRecipientsRecordsDurableSanitizedDiagnostic() {
        UUID ruleId = UUID.randomUUID(); Notification failed = failed(ruleId);
        when(notifications.findByIdForUpdate(failed.id())).thenReturn(Optional.of(failed));
        when(policies.findByRuleId(ruleId)).thenReturn(Optional.of(policy(0, RecipientType.ROLE, "EMPTY")));
        when(attempts.findLatest(failed.id())).thenReturn(Optional.of(failedAttempt(failed.id(), now)));
        when(directory.activeRoleExists("EMPTY")).thenReturn(true);
        when(directory.findActiveRoleMembers("EMPTY")).thenReturn(List.of());
        service.escalateIfDue(failed.id(), now);
        verify(notifications).save(argThat(n -> n.failureReason().contains("ESCALATION_NO_RECIPIENT")));
    }

    @Test void userFallbackAtDelayZeroCreatesExactlyOneChild() {
        UUID ruleId = UUID.randomUUID(); Notification failed = failed(ruleId);
        when(notifications.findByIdForUpdate(failed.id())).thenReturn(Optional.of(failed));
        when(policies.findByRuleId(ruleId)).thenReturn(Optional.of(policy(0, RecipientType.USER, "alice")));
        when(attempts.findLatest(failed.id())).thenReturn(Optional.of(failedAttempt(failed.id(), now)));
        when(directory.findActiveUser("alice")).thenReturn(Optional.of(
            new NotificationRecipientDirectoryPort.RecipientUser("alice", "a@example.test")));
        service.escalateIfDue(failed.id(), now);
        verify(notifications).save(argThat(n -> "alice".equals(n.recipient()) && n.escalationLevel() == 1));

        reset(notifications);
        when(notifications.findByIdForUpdate(failed.id())).thenReturn(Optional.of(failed));
        when(notifications.existsByParentNotificationIdAndRecipient(failed.id(), "alice")).thenReturn(true);
        service.escalateIfDue(failed.id(), now);
        verify(notifications, never()).save(any());
    }

    @Test void sentNotificationIsNeverEscalated() {
        Notification sent = Notification.createPending(null, UUID.randomUUID(), "TRIP_DELAY_RECORDED",
            NotificationChannel.EMAIL, "ops@example.test", NotificationSeverity.WARNING, "Subject", "Body",
            null, null, null, now.minusMinutes(1), null).markSent(now);
        when(notifications.findByIdForUpdate(sent.id())).thenReturn(Optional.of(sent));
        service.escalateIfDue(sent.id(), now);
        verifyNoInteractions(policies, attempts, directory);
        verify(notifications, never()).save(any());
    }

    private NotificationRulePolicy policy(int delay, RecipientType type, String value) {
        return new NotificationRulePolicy(false, null, null, Set.of(), 0, true, delay, type, value);
    }

    private Notification failed(UUID ruleId) {
        return Notification.createPending(ruleId, UUID.randomUUID(), "TRIP_DELAY_RECORDED", NotificationChannel.EMAIL,
            "ops@example.test", NotificationSeverity.CRITICAL, "Subject", "Body", null, null, null,
            now.minusMinutes(10), null).markFailed("TIMEOUT: sanitized");
    }

    private NotificationDeliveryAttempt failedAttempt(UUID notificationId, OffsetDateTime completedAt) {
        return NotificationDeliveryAttempt.start(notificationId, 3, completedAt.minusSeconds(1), completedAt.minusSeconds(1))
            .fail(completedAt, EmailDeliveryErrorCategory.TIMEOUT, "TIMEOUT", "sanitized");
    }
}
