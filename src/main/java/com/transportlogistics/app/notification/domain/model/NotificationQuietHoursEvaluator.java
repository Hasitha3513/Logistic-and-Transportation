package com.transportlogistics.app.notification.domain.model;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.zone.ZoneOffsetTransition;
import java.util.List;
import java.util.Objects;

public final class NotificationQuietHoursEvaluator {
    private final Clock clock;
    private final ZoneId businessZone;

    public NotificationQuietHoursEvaluator(Clock clock, ZoneId businessZone) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.businessZone = Objects.requireNonNull(businessZone, "businessZone must not be null");
    }

    public QuietHoursDecision evaluate(NotificationRulePolicy policy,
                                       NotificationChannel channel,
                                       NotificationSeverity severity) {
        Objects.requireNonNull(policy, "policy must not be null");
        if (!policy.quietHoursEnabled()
            || (channel != NotificationChannel.EMAIL && channel != NotificationChannel.SMS)
            || severity == NotificationSeverity.CRITICAL) {
            return QuietHoursDecision.immediate();
        }

        ZonedDateTime now = clock.instant().atZone(businessZone);
        LocalDate intervalEndDate = activeIntervalEndDate(policy, now.toLocalDate(), now.toLocalTime());
        if (intervalEndDate == null) {
            return QuietHoursDecision.immediate();
        }
        Instant nextDelivery = resolveLocal(intervalEndDate, policy.quietEndTime()).toInstant();
        return new QuietHoursDecision(true, OffsetDateTime.ofInstant(nextDelivery, ZoneOffset.UTC));
    }

    private LocalDate activeIntervalEndDate(NotificationRulePolicy policy, LocalDate date, LocalTime time) {
        LocalTime start = policy.quietStartTime();
        LocalTime end = policy.quietEndTime();
        if (start.isBefore(end)) {
            return policy.quietDays().contains(date.getDayOfWeek())
                && !time.isBefore(start) && time.isBefore(end) ? date : null;
        }

        if (!time.isBefore(start) && policy.quietDays().contains(date.getDayOfWeek())) {
            return date.plusDays(1);
        }
        DayOfWeek previousDay = date.minusDays(1).getDayOfWeek();
        if (time.isBefore(end) && policy.quietDays().contains(previousDay)) {
            return date;
        }
        return null;
    }

    private ZonedDateTime resolveLocal(LocalDate date, LocalTime time) {
        LocalDateTime local = LocalDateTime.of(date, time);
        List<ZoneOffset> offsets = businessZone.getRules().getValidOffsets(local);
        if (!offsets.isEmpty()) {
            return ZonedDateTime.ofLocal(local, businessZone, offsets.get(0));
        }
        ZoneOffsetTransition transition = businessZone.getRules().getTransition(local);
        return ZonedDateTime.ofLocal(transition.getDateTimeAfter(), businessZone, transition.getOffsetAfter());
    }

    public ZoneId businessZone() {
        return businessZone;
    }

    public record QuietHoursDecision(boolean queued, OffsetDateTime nextDeliveryAt) {
        public QuietHoursDecision {
            if (queued != (nextDeliveryAt != null)) {
                throw new IllegalArgumentException("Queued decision and next delivery time must be present together");
            }
        }

        public static QuietHoursDecision immediate() {
            return new QuietHoursDecision(false, null);
        }
    }
}
