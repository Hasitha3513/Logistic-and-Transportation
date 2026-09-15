package com.transportlogistics.app.tracking.application;

import static com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeterministicJourneyReplayStopAnalyzerTest {
    private static final UUID TENANT = new UUID(1, 1);
    private static final UUID VEHICLE = new UUID(2, 2);
    private static final Instant SNAPSHOT = Instant.parse("2026-09-15T01:00:00Z");
    private static final Instant START = Instant.parse("2026-09-15T00:00:00Z");

    @Test void confirmsExactFiveMinuteDwellAtSpeedAndAccuracyBoundaries() {
        var analyzer = analyzer();
        analyzer.accept(point(0, "3.000", "100.000", "6.9", "79.8", Trust.TRUSTED));
        analyzer.accept(point(120, "3.000", "100.000", "6.9", "79.8", Trust.TRUSTED));
        analyzer.accept(point(240, "3.000", "100.000", "6.9", "79.8", Trust.TRUSTED));
        analyzer.accept(point(300, "3.000", "100.000", "6.9", "79.8", Trust.TRUSTED));
        assertThat(analyzer.finish()).singleElement().satisfies(stop -> {
            assertThat(stop.dwell()).isEqualTo(java.time.Duration.ofMinutes(5));
            assertThat(stop.evidenceCount()).isEqualTo(4);
        });
    }

    @Test void rejectsShortMovingUntrustedUnknownAndInaccurateEvidence() {
        for (JourneyPoint breaker : List.of(
                point(301, "3.001", "1", "6.9", "79.8", Trust.TRUSTED),
                point(301, "0", "100.001", "6.9", "79.8", Trust.TRUSTED),
                point(301, "0", null, "6.9", "79.8", Trust.TRUSTED),
                point(301, "0", "1", "6.9", "79.8", Trust.UNTRUSTED))) {
            var analyzer = analyzer();
            analyzer.accept(point(0, "0", "1", "6.9", "79.8", Trust.TRUSTED));
            analyzer.accept(point(299, "0", "1", "6.9", "79.8", Trust.TRUSTED));
            analyzer.accept(breaker);
            assertThat(analyzer.finish()).isEmpty();
        }
    }

    @Test void missingSpeedIsSpatialEvidenceAndIsNeverConvertedToZero() {
        var analyzer = analyzer();
        analyzer.accept(point(0, null, "5", "6.9", "79.8", Trust.TRUSTED));
        analyzer.accept(point(120, "0", "5", "6.9", "79.8", Trust.TRUSTED));
        analyzer.accept(point(240, null, "5", "6.9", "79.8", Trust.TRUSTED));
        analyzer.accept(point(300, null, "5", "6.9", "79.8", Trust.TRUSTED));
        assertThat(analyzer.finish()).singleElement().satisfies(stop ->
                assertThat(stop.quality()).isEqualTo(StopEvidenceQuality.SPEED_SPATIAL_ONLY));
    }

    @Test void exactTwoMinuteGapPassesAndGreaterGapSplits() {
        var exact = analyzer();
        for (int second : new int[]{0, 120, 240, 300}) exact.accept(point(second));
        assertThat(exact.finish()).hasSize(1);
        var exceeded = analyzer();
        for (int second : new int[]{0, 120, 241, 541}) exceeded.accept(point(second));
        assertThat(exceeded.finish()).isEmpty();
    }

    @Test void centroidUsesAccuracySquaredWeightsAndHandlesDateLine() {
        Coordinate weighted = DeterministicJourneyReplayStopAnalyzer.centroid(List.of(
                point(0, "0", "1", "0", "0", Trust.TRUSTED),
                point(1, "0", "10", "0", "1", Trust.TRUSTED)));
        assertThat(weighted.longitude().doubleValue()).isLessThan(0.02d);
        Coordinate wrapped = DeterministicJourneyReplayStopAnalyzer.centroid(List.of(
                point(0, "0", "1", "0", "179.9", Trust.TRUSTED),
                point(1, "0", "1", "0", "-179.9", Trust.TRUSTED)));
        assertThat(Math.abs(wrapped.longitude().doubleValue())).isGreaterThan(179.9d);
    }

    @Test void radiusViolationStartsNewCandidateAndRepeatedCoordinatesRemainEvidence() {
        var analyzer = analyzer();
        analyzer.accept(point(0));
        analyzer.accept(point(120));
        analyzer.accept(point(240));
        analyzer.accept(point(300));
        analyzer.accept(point(301, "0", "1", "7.9", "80.8", Trust.TRUSTED));
        assertThat(analyzer.finish()).singleElement().satisfies(stop ->
                assertThat(stop.evidenceCount()).isEqualTo(4));
    }

    @Test void deterministicIdentityChangesWithTenantVehicleSnapshotAndBoundaries() {
        List<JourneyPoint> evidence = List.of(point(0), point(120), point(240), point(300));
        String first = analyze(TENANT, VEHICLE, SNAPSHOT, evidence).getFirst().stopId();
        assertThat(analyze(TENANT, VEHICLE, SNAPSHOT, evidence).getFirst().stopId()).isEqualTo(first);
        assertThat(analyze(UUID.randomUUID(), VEHICLE, SNAPSHOT, evidence).getFirst().stopId()).isNotEqualTo(first);
        assertThat(analyze(TENANT, UUID.randomUUID(), SNAPSHOT, evidence).getFirst().stopId()).isNotEqualTo(first);
        assertThat(analyze(TENANT, VEHICLE, SNAPSHOT.plusSeconds(1), evidence).getFirst().stopId()).isNotEqualTo(first);
    }

    @Test void qualifyingBoundaryEvidenceMarksBothEndsTruncated() {
        JourneyPoint before = point(-60);
        JourneyPoint after = point(360);
        ReplayBoundaryEvidence boundaries = new ReplayBoundaryEvidence(
                new BoundaryObservation(BoundaryReason.ADJACENT_QUALIFYING, before),
                new BoundaryObservation(BoundaryReason.ADJACENT_QUALIFYING, after));
        var analyzer = new DeterministicJourneyReplayStopAnalyzer(TENANT, VEHICLE, SNAPSHOT,
                boundaries, Coverage.COMPLETE);
        for (int second : new int[]{0, 120, 240, 300}) analyzer.accept(point(second));
        assertThat(analyzer.finish()).singleElement().satisfies(stop -> {
            assertThat(stop.startTruncated()).isTrue();
            assertThat(stop.endTruncated()).isTrue();
        });
    }

    private static List<ConfirmedStop> analyze(UUID tenant, UUID vehicle, Instant snapshot,
            List<JourneyPoint> points) {
        var analyzer = new DeterministicJourneyReplayStopAnalyzer(tenant, vehicle, snapshot,
                ReplayBoundaryEvidence.unavailable(), Coverage.COMPLETE);
        points.forEach(analyzer::accept);
        return analyzer.finish();
    }

    private static DeterministicJourneyReplayStopAnalyzer analyzer() {
        return new DeterministicJourneyReplayStopAnalyzer(TENANT, VEHICLE, SNAPSHOT,
                ReplayBoundaryEvidence.unavailable(), Coverage.COMPLETE);
    }

    private static JourneyPoint point(int seconds) {
        return point(seconds, "0", "1", "6.9", "79.8", Trust.TRUSTED);
    }

    private static JourneyPoint point(int seconds, String speed, String accuracy,
            String latitude, String longitude, Trust trust) {
        return new JourneyPoint(new UUID(0, seconds + 1L), VEHICLE, START.plusSeconds(seconds),
                START.plusSeconds(seconds + 1L), new Coordinate(new BigDecimal(latitude),
                new BigDecimal(longitude)), decimal(speed), decimal(accuracy), trust, "GOOD",
                Ordering.IN_ORDER, Attribution.unavailable(AttributionStatus.UNATTRIBUTED), Set.of());
    }

    private static BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }
}
