package com.transportlogistics.app.notification.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationQuietHoursEvaluatorTest {
    private static final ZoneId COLOMBO = ZoneId.of("Asia/Colombo");

    @Test void queuesEmailAtInclusiveStartAndUsesExclusiveEnd() {
        var policy = policy(LocalTime.of(22, 0), LocalTime.of(6, 0), Set.of(DayOfWeek.FRIDAY));
        assertThat(evaluate("2026-08-21T16:30:00Z", policy, NotificationChannel.EMAIL, NotificationSeverity.WARNING).queued()).isTrue();
        assertThat(evaluate("2026-08-22T00:30:00Z", policy, NotificationChannel.EMAIL, NotificationSeverity.WARNING).queued()).isFalse();
    }

    @Test void overnightIntervalBelongsToItsStartDay() {
        var policy = policy(LocalTime.of(22, 0), LocalTime.of(6, 0), Set.of(DayOfWeek.FRIDAY));
        var decision = evaluate("2026-08-21T20:00:00Z", policy, NotificationChannel.EMAIL, NotificationSeverity.WARNING);
        assertThat(decision.queued()).isTrue();
        assertThat(decision.nextDeliveryAt()).isEqualTo("2026-08-22T00:30Z");
    }

    @Test void nonQuietDayIsImmediate() {
        var policy = policy(LocalTime.of(9, 0), LocalTime.of(17, 0), Set.of(DayOfWeek.MONDAY));
        assertThat(evaluate("2026-08-21T06:00:00Z", policy, NotificationChannel.EMAIL, NotificationSeverity.WARNING).queued()).isFalse();
    }

    @Test void criticalAndInAppNotificationsBypassQuietHours() {
        var policy = policy(LocalTime.of(0, 1), LocalTime.of(23, 59), Set.of(DayOfWeek.FRIDAY));
        assertThat(evaluate("2026-08-21T06:00:00Z", policy, NotificationChannel.EMAIL, NotificationSeverity.CRITICAL).queued()).isFalse();
        assertThat(evaluate("2026-08-21T06:00:00Z", policy, NotificationChannel.IN_APP, NotificationSeverity.WARNING).queued()).isFalse();
    }

    @Test void disabledQuietHoursAreImmediate() {
        var policy = new NotificationRulePolicy(false, null, null, Set.of(), 0);
        assertThat(evaluate("2026-08-21T06:00:00Z", policy, NotificationChannel.EMAIL, NotificationSeverity.WARNING).queued()).isFalse();
    }

    @Test void dstGapUsesNextValidInstant() {
        ZoneId newYork = ZoneId.of("America/New_York");
        var policy = policy(LocalTime.MIDNIGHT, LocalTime.of(2, 30), Set.of(DayOfWeek.SUNDAY));
        Clock clock = Clock.fixed(Instant.parse("2026-03-08T06:00:00Z"), newYork);
        var result = new NotificationQuietHoursEvaluator(clock, newYork)
            .evaluate(policy, NotificationChannel.EMAIL, NotificationSeverity.WARNING);
        assertThat(result.nextDeliveryAt()).isEqualTo("2026-03-08T07:00Z");
    }

    @Test void dstOverlapUsesEarlierOffset() {
        ZoneId newYork = ZoneId.of("America/New_York");
        var policy = policy(LocalTime.MIDNIGHT, LocalTime.of(1, 30), Set.of(DayOfWeek.SUNDAY));
        Clock clock = Clock.fixed(Instant.parse("2026-11-01T04:30:00Z"), newYork);
        var result = new NotificationQuietHoursEvaluator(clock, newYork)
            .evaluate(policy, NotificationChannel.EMAIL, NotificationSeverity.WARNING);
        assertThat(result.nextDeliveryAt()).isEqualTo("2026-11-01T05:30Z");
    }

    @Test void invalidIanaTimezoneIsRejected() {
        assertThatThrownBy(() -> ZoneId.of("Not/A_Zone")).isInstanceOf(java.time.DateTimeException.class);
    }

    private NotificationQuietHoursEvaluator.QuietHoursDecision evaluate(String instant, NotificationRulePolicy policy,
                                                                         NotificationChannel channel,
                                                                         NotificationSeverity severity) {
        Clock clock = Clock.fixed(Instant.parse(instant), COLOMBO);
        return new NotificationQuietHoursEvaluator(clock, COLOMBO).evaluate(policy, channel, severity);
    }

    private NotificationRulePolicy policy(LocalTime start, LocalTime end, Set<DayOfWeek> days) {
        return new NotificationRulePolicy(true, start, end, days, 15);
    }
}
