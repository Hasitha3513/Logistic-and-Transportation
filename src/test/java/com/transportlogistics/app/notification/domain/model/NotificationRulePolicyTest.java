package com.transportlogistics.app.notification.domain.model;

import org.junit.jupiter.api.Test;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationRulePolicyTest {
    @Test void validatesSuppressionBounds() {
        assertThatThrownBy(() -> new NotificationRulePolicy(false, null, null, Set.of(), -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NotificationRulePolicy(false, null, null, Set.of(), 1441)).isInstanceOf(IllegalArgumentException.class);
        assertThat(new NotificationRulePolicy(false, null, null, Set.of(), 1440).suppressionWindowMinutes()).isEqualTo(1440);
    }

    @Test void enabledQuietHoursRequireCoherentIntervalAndDay() {
        assertThatThrownBy(() -> new NotificationRulePolicy(true, LocalTime.NOON, LocalTime.NOON,
            Set.of(DayOfWeek.MONDAY), 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NotificationRulePolicy(true, LocalTime.NOON, LocalTime.MIDNIGHT,
            Set.of(), 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void quietHoursAreEmailOnly() {
        var policy = new NotificationRulePolicy(true, LocalTime.of(22, 0), LocalTime.of(6, 0),
            Set.of(DayOfWeek.MONDAY), 0);
        assertThatThrownBy(() -> policy.validateForChannel(NotificationChannel.IN_APP)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void validatesFrozenEscalationPolicy() {
        assertThatThrownBy(() -> new NotificationRulePolicy(false, null, null, Set.of(), 0,
            true, -1, RecipientType.USER, "user1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NotificationRulePolicy(false, null, null, Set.of(), 0,
            true, 61, RecipientType.USER, "user1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NotificationRulePolicy(false, null, null, Set.of(), 0,
            true, 0, RecipientType.EMAIL_ADDRESS, "ops@example.test")).isInstanceOf(IllegalArgumentException.class);
        assertThat(new NotificationRulePolicy(false, null, null, Set.of(), 0,
            true, 60, RecipientType.ROLE, "OPERATIONS").escalationDelayMinutes()).isEqualTo(60);
    }
}
