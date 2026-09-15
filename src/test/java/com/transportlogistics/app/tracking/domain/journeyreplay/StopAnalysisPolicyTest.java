package com.transportlogistics.app.tracking.domain.journeyreplay;

import static com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class StopAnalysisPolicyTest {
    private static final Instant START = Instant.parse("2026-09-15T00:00:00Z");
    private static final UUID VEHICLE = UUID.randomUUID();

    @Test void appliesTrustAccuracySpeedAndMissingSpeedBoundaries() {
        assertThat(StopAnalysisPolicy.eligible(point(START, "3.000", "100.000", Trust.TRUSTED))).isTrue();
        assertThat(StopAnalysisPolicy.eligible(point(START, null, "100.000", Trust.TRUSTED))).isTrue();
        assertThat(StopAnalysisPolicy.eligible(point(START, "3.001", "100.000", Trust.TRUSTED))).isFalse();
        assertThat(StopAnalysisPolicy.eligible(point(START, "0", "100.001", Trust.TRUSTED))).isFalse();
        assertThat(StopAnalysisPolicy.eligible(point(START, "0", null, Trust.TRUSTED))).isFalse();
        assertThat(StopAnalysisPolicy.eligible(point(START, "0", "1", Trust.UNTRUSTED))).isFalse();
    }

    @Test void appliesFiveMinuteDwellAndTwoMinuteGapBoundaries() {
        JourneyPoint first = point(START, "0", "1", Trust.TRUSTED);
        JourneyPoint exactGap = point(START.plus(Duration.ofMinutes(2)), "0", "1", Trust.TRUSTED);
        JourneyPoint excessiveGap = point(START.plus(Duration.ofMinutes(2)).plusNanos(1), "0", "1", Trust.TRUSTED);
        assertThat(StopAnalysisPolicy.gapAllowed(first, exactGap)).isTrue();
        assertThat(StopAnalysisPolicy.gapAllowed(first, excessiveGap)).isFalse();

        StopCandidate exact = candidate(Duration.ofMinutes(5), true, false);
        StopCandidate shortCandidate = candidate(Duration.ofMinutes(5).minusNanos(1), false, false);
        assertThat(StopAnalysisPolicy.confirmable(exact)).isTrue();
        assertThat(StopAnalysisPolicy.confirmable(shortCandidate)).isFalse();
        assertThat(exact.startTruncated()).isTrue();
    }

    @Test void mergesOnlyAtBothFrozenBoundaries() {
        ConfirmedStop first = stop(START, START.plusSeconds(300));
        ConfirmedStop atBoundary = stop(first.end().plusSeconds(120), first.end().plusSeconds(420));
        assertThat(StopAnalysisPolicy.mergeable(first, atBoundary, new BigDecimal("50.000"))).isTrue();
        assertThat(StopAnalysisPolicy.mergeable(first, atBoundary, new BigDecimal("50.001"))).isFalse();
        ConfirmedStop afterGap = stop(first.end().plusSeconds(121), first.end().plusSeconds(421));
        assertThat(StopAnalysisPolicy.mergeable(first, afterGap, BigDecimal.ONE)).isFalse();
    }

    private static StopCandidate candidate(Duration dwell, boolean startTruncated, boolean endTruncated) {
        JourneyPoint first = point(START, "0", "1", Trust.TRUSTED);
        JourneyPoint last = point(START.plus(dwell), "0", "1", Trust.TRUSTED);
        return new StopCandidate(List.of(first, last), first.coordinate(), first.sourceTimestamp(),
                last.sourceTimestamp(), startTruncated, endTruncated,
                startTruncated || endTruncated ? StopEvidenceQuality.RANGE_TRUNCATED : StopEvidenceQuality.QUALIFIED);
    }
    private static ConfirmedStop stop(Instant from, Instant to) {
        return new ConfirmedStop(UUID.randomUUID().toString(), new Coordinate(BigDecimal.ONE, BigDecimal.ONE),
                from, to, Duration.between(from, to), 2, BigDecimal.ONE, false, false,
                StopEvidenceQuality.QUALIFIED, Set.of());
    }
    private static JourneyPoint point(Instant time, String speed, String accuracy, Trust trust) {
        return new JourneyPoint(UUID.randomUUID(), VEHICLE, time, time.plusSeconds(1),
                new Coordinate(BigDecimal.ONE, BigDecimal.ONE), decimal(speed), decimal(accuracy), trust,
                "QUALITY", Ordering.IN_ORDER, Attribution.unavailable(AttributionStatus.UNATTRIBUTED), Set.of());
    }
    private static BigDecimal decimal(String value) { return value == null ? null : new BigDecimal(value); }
}
