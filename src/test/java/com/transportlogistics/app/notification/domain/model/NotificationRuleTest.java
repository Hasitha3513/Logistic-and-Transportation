package com.transportlogistics.app.notification.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationRuleTest {

    @Test
    void create_validInAppRule_succeeds() {
        NotificationRule rule = NotificationRule.create(
            "Trip Delays Alert",
            "Notify dispatchers when a trip is delayed",
            "TRIP_DELAY_RECORDED",
            NotificationChannel.IN_APP,
            RecipientType.ROLE,
            "DISPATCHER",
            true,
            NotificationSeverity.WARNING
        );

        assertThat(rule.id()).isNotNull();
        assertThat(rule.name()).isEqualTo("Trip Delays Alert");
        assertThat(rule.eventType()).isEqualTo("TRIP_DELAY_RECORDED");
        assertThat(rule.channel()).isEqualTo(NotificationChannel.IN_APP);
        assertThat(rule.recipientType()).isEqualTo(RecipientType.ROLE);
        assertThat(rule.recipientValue()).isEqualTo("DISPATCHER");
        assertThat(rule.templateCode()).isEqualTo("TRIP_DELAY");
        assertThat(rule.enabled()).isTrue();
        assertThat(rule.severityThreshold()).isEqualTo(NotificationSeverity.WARNING);
    }

    @Test
    void create_validEmailRule_succeeds() {
        NotificationRule rule = NotificationRule.create(
            "Critical Maintenance Email",
            "Send email on critical maintenance",
            "VEHICLE_MAINTENANCE_DUE",
            NotificationChannel.EMAIL,
            RecipientType.EMAIL_ADDRESS,
            "fleet.manager@company.com",
            true,
            NotificationSeverity.CRITICAL
        );

        assertThat(rule.channel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(rule.recipientValue()).isEqualTo("fleet.manager@company.com");
    }

    @Test
    void create_invalidEmail_throwsException() {
        assertThatThrownBy(() -> NotificationRule.create(
            "Email Alert",
            "Desc",
            "TRIP_DELAY_RECORDED",
            NotificationChannel.EMAIL,
            RecipientType.EMAIL_ADDRESS,
            "not-an-email",
            true,
            NotificationSeverity.INFO
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Invalid email address");
    }

    @Test
    void create_blankNameOrEventType_throwsException() {
        assertThatThrownBy(() -> NotificationRule.create(
            "   ",
            "Desc",
            "TRIP_DELAY_RECORDED",
            NotificationChannel.IN_APP,
            RecipientType.USER,
            "user1",
            true,
            NotificationSeverity.INFO
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> NotificationRule.create(
            "Name",
            "Desc",
            "   ",
            NotificationChannel.IN_APP,
            RecipientType.USER,
            "user1",
            true,
            NotificationSeverity.INFO
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void matchesEvent_respectsEnabledAndSeverity() {
        NotificationRule rule = NotificationRule.create(
            "Incident Alert",
            "Desc",
            "TRIP_INCIDENT_RECORDED",
            NotificationChannel.IN_APP,
            RecipientType.ROLE,
            "DISPATCHER",
            true,
            NotificationSeverity.WARNING
        );

        // Matching event
        assertThat(rule.matchesEvent("TRIP_INCIDENT_RECORDED", NotificationSeverity.WARNING)).isTrue();
        assertThat(rule.matchesEvent("TRIP_INCIDENT_RECORDED", NotificationSeverity.CRITICAL)).isTrue();

        // Below threshold
        assertThat(rule.matchesEvent("TRIP_INCIDENT_RECORDED", NotificationSeverity.INFO)).isFalse();

        // Different event
        assertThat(rule.matchesEvent("TRIP_DELAY_RECORDED", NotificationSeverity.CRITICAL)).isFalse();

        // Disabled rule
        NotificationRule disabled = rule.withEnabled(false);
        assertThat(disabled.matchesEvent("TRIP_INCIDENT_RECORDED", NotificationSeverity.CRITICAL)).isFalse();
    }
}
