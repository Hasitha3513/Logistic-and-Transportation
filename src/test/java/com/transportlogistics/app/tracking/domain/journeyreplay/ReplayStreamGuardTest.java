package com.transportlogistics.app.tracking.domain.journeyreplay;

import static com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReplayStreamGuardTest {
    private static final Instant NOW = Instant.parse("2026-09-15T00:00:00Z");

    @Test void rejectsRepeatedCursorRegressingTupleAndSnapshotChange() {
        ReplayStreamGuard repeated = new ReplayStreamGuard();
        repeated.accept(null, page(List.of(point(NOW, new UUID(0, 2))), "same", NOW));
        assertThatThrownBy(() -> repeated.accept("same", page(List.of(), "same", NOW)))
                .isInstanceOf(JourneyReplayException.class);
        ReplayStreamGuard regressing = new ReplayStreamGuard();
        regressing.accept(null, page(List.of(point(NOW, new UUID(0, 2))), "next", NOW));
        assertThatThrownBy(() -> regressing.accept("next",
                page(List.of(point(NOW, new UUID(0, 1))), null, NOW)))
                .isInstanceOf(JourneyReplayException.class);
        ReplayStreamGuard changed = new ReplayStreamGuard();
        changed.accept(null, page(List.of(point(NOW, new UUID(0, 1))), "next", NOW));
        assertThatThrownBy(() -> changed.accept("next", page(List.of(), null, NOW.plusSeconds(1))))
                .isInstanceOf(JourneyReplayException.class);
    }

    private static ReplayPage page(List<JourneyPoint> points, String next, Instant snapshot) {
        TimeRange range = new TimeRange(NOW.minusSeconds(1), NOW.plusSeconds(2));
        return new ReplayPage(points, next, range, range, Coverage.COMPLETE, List.of(), false,
                false, Set.of(), snapshot, ReplayBoundaryEvidence.unavailable());
    }

    private static JourneyPoint point(Instant time, UUID id) {
        return new JourneyPoint(id, UUID.randomUUID(), time, time, new Coordinate(BigDecimal.ONE,
                BigDecimal.ONE), BigDecimal.ZERO, BigDecimal.ONE, Trust.TRUSTED, "GOOD",
                Ordering.IN_ORDER, Attribution.unavailable(AttributionStatus.UNATTRIBUTED), Set.of());
    }
}
