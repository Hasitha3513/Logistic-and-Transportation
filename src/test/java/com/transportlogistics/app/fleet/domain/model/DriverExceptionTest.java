package com.transportlogistics.app.fleet.domain.model;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DriverExceptionTest {

    private final UUID id = UUID.randomUUID();
    private final UUID driverId = UUID.randomUUID();
    private final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

    @Test
    void createsValidDriverException() {
        var start = now.plusDays(1);
        var end = now.plusDays(3);
        var exception = new DriverException(
                id, driverId, DriverExceptionType.LEAVE, start, end,
                DriverExceptionStatus.SCHEDULED, "Annual leave", "Approved by manager",
                now, now, "admin", "admin"
        );

        assertThat(exception.id()).isEqualTo(id);
        assertThat(exception.driverId()).isEqualTo(driverId);
        assertThat(exception.exceptionType()).isEqualTo(DriverExceptionType.LEAVE);
        assertThat(exception.status()).isEqualTo(DriverExceptionStatus.SCHEDULED);
        assertThat(exception.status().isBlocking()).isTrue();
    }

    @Test
    void rejectsWhenEndTimeNotAfterStartTime() {
        var start = now.plusDays(3);
        var end = now.plusDays(1);

        assertThatThrownBy(() -> new DriverException(
                id, driverId, DriverExceptionType.LEAVE, start, end,
                DriverExceptionStatus.SCHEDULED, null, null,
                now, now, "admin", "admin"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End time must be strictly after start time");
    }

    @Test
    void detectsHalfOpenIntervalOverlapsCorrectly() {
        // Exception: [10:00, 12:00)
        var start = OffsetDateTime.of(2026, 9, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        var end = OffsetDateTime.of(2026, 9, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        var exception = new DriverException(
                id, driverId, DriverExceptionType.DISCIPLINARY_SUSPENSION, start, end,
                DriverExceptionStatus.SCHEDULED, null, null,
                now, now, "admin", "admin"
        );

        // 1. Boundary adjacent before: [08:00, 10:00) -> NO OVERLAP
        assertThat(exception.hasOverlap(
                OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 1, 10, 0, 0, 0, ZoneOffset.UTC)
        )).isFalse();

        // 2. Boundary adjacent after: [12:00, 14:00) -> NO OVERLAP
        assertThat(exception.hasOverlap(
                OffsetDateTime.of(2026, 9, 1, 12, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 1, 14, 0, 0, 0, ZoneOffset.UTC)
        )).isFalse();

        // 3. Partial overlap left: [09:00, 11:00) -> OVERLAP
        assertThat(exception.hasOverlap(
                OffsetDateTime.of(2026, 9, 1, 9, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 1, 11, 0, 0, 0, ZoneOffset.UTC)
        )).isTrue();

        // 4. Partial overlap right: [11:00, 13:00) -> OVERLAP
        assertThat(exception.hasOverlap(
                OffsetDateTime.of(2026, 9, 1, 11, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 1, 13, 0, 0, 0, ZoneOffset.UTC)
        )).isTrue();

        // 5. Enclosing: [09:00, 13:00) -> OVERLAP
        assertThat(exception.hasOverlap(
                OffsetDateTime.of(2026, 9, 1, 9, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 1, 13, 0, 0, 0, ZoneOffset.UTC)
        )).isTrue();

        // 6. Enclosed: [10:30, 11:30) -> OVERLAP
        assertThat(exception.hasOverlap(
                OffsetDateTime.of(2026, 9, 1, 10, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 1, 11, 30, 0, 0, ZoneOffset.UTC)
        )).isTrue();
    }
}
