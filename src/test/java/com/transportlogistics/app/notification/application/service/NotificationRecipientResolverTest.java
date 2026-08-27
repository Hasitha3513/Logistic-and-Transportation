package com.transportlogistics.app.notification.application.service;

import com.transportlogistics.app.notification.application.ports.out.NotificationRecipientDirectoryPort;
import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.RecipientType;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationRecipientResolverTest {
    private NotificationRecipientDirectoryPort directory;
    private NotificationRecipientResolver resolver;

    @BeforeEach
    void setUp() {
        directory = mock(NotificationRecipientDirectoryPort.class);
        resolver = new NotificationRecipientResolver(directory);
    }

    @Test
    void resolvesActiveUserForBothChannels() {
        when(directory.findActiveUser("operator")).thenReturn(Optional.of(user("Operator", "Ops@Example.Test")));
        assertThat(resolver.resolve(RecipientType.USER, NotificationChannel.IN_APP, "operator"))
            .containsExactly("Operator");
        assertThat(resolver.resolve(RecipientType.USER, NotificationChannel.EMAIL, "operator"))
            .containsExactly("ops@example.test");
    }

    @Test
    void rejectsUnknownOrEmaillessUser() {
        when(directory.findActiveUser("unknown")).thenReturn(Optional.empty());
        assertCode(() -> resolver.validate(RecipientType.USER, NotificationChannel.IN_APP, "unknown"),
            "NOTIFICATION_RECIPIENT_NOT_FOUND");
        when(directory.findActiveUser("operator")).thenReturn(Optional.of(user("operator", null)));
        assertCode(() -> resolver.validate(RecipientType.USER, NotificationChannel.EMAIL, "operator"),
            "NOTIFICATION_RECIPIENT_INVALID");
    }

    @Test
    void validatesRoleAndReturnsDeterministicZeroRecipients() {
        when(directory.activeRoleExists("DISPATCHER")).thenReturn(true);
        when(directory.findActiveRoleMembers("DISPATCHER")).thenReturn(List.of());
        assertThat(resolver.resolve(RecipientType.ROLE, NotificationChannel.IN_APP, "DISPATCHER")).isEmpty();

        when(directory.activeRoleExists("UNKNOWN")).thenReturn(false);
        assertCode(() -> resolver.validate(RecipientType.ROLE, NotificationChannel.IN_APP, "UNKNOWN"),
            "NOTIFICATION_RECIPIENT_NOT_FOUND");
    }

    @Test
    void deduplicatesRoleMembersAndSkipsInvalidEmailMembers() {
        when(directory.activeRoleExists("OPS")).thenReturn(true);
        when(directory.findActiveRoleMembers("OPS")).thenReturn(List.of(
            user("alice", "OPS@example.test"), user("bob", "ops@example.test"), user("charlie", null)));
        assertThat(resolver.resolve(RecipientType.ROLE, NotificationChannel.EMAIL, "OPS"))
            .containsExactly("ops@example.test");
    }

    @Test
    void validatesDirectEmailAndRejectsInAppCombination() {
        assertThat(resolver.resolve(RecipientType.EMAIL_ADDRESS, NotificationChannel.EMAIL, "Ops@Example.Test"))
            .containsExactly("ops@example.test");
        assertCode(() -> resolver.validate(RecipientType.EMAIL_ADDRESS, NotificationChannel.EMAIL, "bad"),
            "NOTIFICATION_RECIPIENT_INVALID");
        assertCode(() -> resolver.validate(RecipientType.EMAIL_ADDRESS, NotificationChannel.IN_APP, "ops@example.test"),
            "NOTIFICATION_CHANNEL_RECIPIENT_INCOMPATIBLE");
    }

    private NotificationRecipientDirectoryPort.RecipientUser user(String username, String email) {
        return new NotificationRecipientDirectoryPort.RecipientUser(username, email);
    }

    private void assertCode(Runnable action, String code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessRuleException.class,
            error -> assertThat(error.code()).isEqualTo(code));
    }
}
