package com.transportlogistics.app.tracking.domain.journeyreplay;

import static com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StopReplayContractTest {
    private static final Instant NOW = Instant.parse("2026-09-15T00:00:00Z");

    @Test void enforcesDefaultAndMaximumStopLimits() {
        assertThat(StopReplayQuery.defaults(replay()).stopLimit()).isEqualTo(100);
        assertThat(new StopReplayQuery(replay(), 500, null).stopLimit()).isEqualTo(500);
        assertThatThrownBy(() -> new StopReplayQuery(replay(), 501, null))
                .isInstanceOf(JourneyReplayException.class);
    }

    @Test void validatesTenantSelectorRangeRuleAndExpiryBindings() {
        StopReplayQuery query = StopReplayQuery.defaults(replay());
        StopCursorState valid = cursor(query, NOW.plusSeconds(900));
        ReplayQueryPolicy.validateStopCursor(valid, query, NOW);
        assertThatThrownBy(() -> ReplayQueryPolicy.validateStopCursor(
                new StopCursorState(UUID.randomUUID(), valid.selector(), valid.requestedRange(),
                        valid.effectiveRange(), valid.snapshotRecordedAt(),
                        valid.lastStartSourceTimestamp(), valid.lastStopId(), valid.ruleVersion(),
                        valid.expiresAt()), query, NOW)).isInstanceOf(JourneyReplayException.class);
        assertThatThrownBy(() -> ReplayQueryPolicy.validateStopCursor(cursor(query, NOW), query, NOW))
                .isInstanceOf(JourneyReplayException.class);
    }

    @Test void pointCeilingRejectsOnlyValuesAboveTwentyThousand() {
        StopPage page = new StopPage(java.util.List.of(), null, NOW, replay().requestedRange(),
                null, Coverage.NO_DATA, java.util.List.of(), 20_000, 20_000);
        assertThat(page.analyzedPointCount()).isEqualTo(20_000);
        assertThatThrownBy(() -> new StopPage(java.util.List.of(), null, NOW,
                replay().requestedRange(), null, Coverage.NO_DATA, java.util.List.of(), 20_001, 20_000))
                .isInstanceOf(JourneyReplayException.class);
    }

    private static ReplayQuery replay() {
        TimeRange range = new TimeRange(NOW.minusSeconds(600), NOW);
        return new ReplayQuery(new TenantContext(UUID.randomUUID(), UUID.randomUUID()),
                ReplaySelector.vehicle(UUID.randomUUID()), range, range, 2_000, null, Set.of(),
                Direction.CHRONOLOGICAL_ASCENDING, BROWSER_POINT_CEILING);
    }

    private static StopCursorState cursor(StopReplayQuery query, Instant expires) {
        ReplayQuery replay = query.replayQuery();
        return new StopCursorState(replay.tenant().tenantId(), replay.selector(), replay.requestedRange(),
                replay.effectiveRange(), NOW, NOW.minusSeconds(30), "stop", STOP_RULE_VERSION, expires);
    }
}
