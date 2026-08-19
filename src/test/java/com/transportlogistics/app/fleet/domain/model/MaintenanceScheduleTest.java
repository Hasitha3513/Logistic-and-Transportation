package com.transportlogistics.app.fleet.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MaintenanceScheduleTest {

    @Test
    void createsValidMaintenanceSchedule() {
        var start = OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC);
        var end = OffsetDateTime.of(2026, 9, 1, 16, 0, 0, 0, ZoneOffset.UTC);
        var id = UUID.randomUUID();
        var vehicleId = UUID.randomUUID();

        var schedule = new MaintenanceSchedule(
                id,
                vehicleId,
                "Routine 50k km Service",
                start,
                end,
                MaintenanceStatus.SCHEDULED,
                "Oil change and inspection",
                "Central Garage",
                new BigDecimal("250.00"),
                OffsetDateTime.now(ZoneOffset.UTC),
                OffsetDateTime.now(ZoneOffset.UTC),
                "admin",
                "admin"
        );

        assertThat(schedule.id()).isEqualTo(id);
        assertThat(schedule.vehicleId()).isEqualTo(vehicleId);
        assertThat(schedule.maintenanceType()).isEqualTo("Routine 50k km Service");
        assertThat(schedule.isBlocking()).isTrue();
    }

    @Test
    void rejectsInvalidDatesWhereEndIsBeforeOrEqualStart() {
        var start = OffsetDateTime.of(2026, 9, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        var endBefore = OffsetDateTime.of(2026, 9, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        var endEqual = OffsetDateTime.of(2026, 9, 1, 12, 0, 0, 0, ZoneOffset.UTC);

        assertThatThrownBy(() -> new MaintenanceSchedule(
                UUID.randomUUID(), UUID.randomUUID(), "Service", start, endBefore,
                MaintenanceStatus.SCHEDULED, null, null, null, null, null, null, null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Scheduled end must be strictly after scheduled start");

        assertThatThrownBy(() -> new MaintenanceSchedule(
                UUID.randomUUID(), UUID.randomUUID(), "Service", start, endEqual,
                MaintenanceStatus.SCHEDULED, null, null, null, null, null, null, null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Scheduled end must be strictly after scheduled start");
    }

    @Test
    void checksBlockingStatusCorrectly() {
        var start = OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC);
        var end = OffsetDateTime.of(2026, 9, 1, 16, 0, 0, 0, ZoneOffset.UTC);

        var scheduled = new MaintenanceSchedule(UUID.randomUUID(), UUID.randomUUID(), "Type", start, end,
                MaintenanceStatus.SCHEDULED, null, null, null, null, null, null, null);
        var inProgress = new MaintenanceSchedule(UUID.randomUUID(), UUID.randomUUID(), "Type", start, end,
                MaintenanceStatus.IN_PROGRESS, null, null, null, null, null, null, null);
        var completed = new MaintenanceSchedule(UUID.randomUUID(), UUID.randomUUID(), "Type", start, end,
                MaintenanceStatus.COMPLETED, null, null, null, null, null, null, null);
        var cancelled = new MaintenanceSchedule(UUID.randomUUID(), UUID.randomUUID(), "Type", start, end,
                MaintenanceStatus.CANCELLED, null, null, null, null, null, null, null);

        assertThat(scheduled.isBlocking()).isTrue();
        assertThat(inProgress.isBlocking()).isTrue();
        assertThat(completed.isBlocking()).isFalse();
        assertThat(cancelled.isBlocking()).isFalse();
    }

    @Test
    void evaluatesHalfOpenIntervalOverlapCorrectly() {
        var maintStart = OffsetDateTime.of(2026, 9, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        var maintEnd = OffsetDateTime.of(2026, 9, 1, 14, 0, 0, 0, ZoneOffset.UTC);
        var schedule = new MaintenanceSchedule(UUID.randomUUID(), UUID.randomUUID(), "Type", maintStart, maintEnd,
                MaintenanceStatus.SCHEDULED, null, null, null, null, null, null, null);

        // Trip ending exactly when maintenance starts -> NO overlap
        assertThat(schedule.hasOverlap(
                OffsetDateTime.of(2026, 9, 1, 6, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 1, 10, 0, 0, 0, ZoneOffset.UTC)
        )).isFalse();

        // Trip starting exactly when maintenance ends -> NO overlap
        assertThat(schedule.hasOverlap(
                OffsetDateTime.of(2026, 9, 1, 14, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 1, 18, 0, 0, 0, ZoneOffset.UTC)
        )).isFalse();

        // Trip overlapping start of maintenance
        assertThat(schedule.hasOverlap(
                OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 1, 11, 0, 0, 0, ZoneOffset.UTC)
        )).isTrue();

        // Trip overlapping end of maintenance
        assertThat(schedule.hasOverlap(
                OffsetDateTime.of(2026, 9, 1, 13, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 1, 16, 0, 0, 0, ZoneOffset.UTC)
        )).isTrue();

        // Trip entirely within maintenance
        assertThat(schedule.hasOverlap(
                OffsetDateTime.of(2026, 9, 1, 11, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 1, 13, 0, 0, 0, ZoneOffset.UTC)
        )).isTrue();

        // Trip containing maintenance interval
        assertThat(schedule.hasOverlap(
                OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 1, 16, 0, 0, 0, ZoneOffset.UTC)
        )).isTrue();
    }
}
