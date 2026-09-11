package com.transportlogistics.app.tracking.domain.speed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VehicleSpeedStateTest {
    private static final UUID TENANT = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID VEHICLE = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID RULE_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final Instant BASE = Instant.parse("2026-09-11T10:00:00Z");
    private static final ResolvedSpeedThreshold THRESHOLD = threshold(1, "60");

    @Test
    void missingOrIneligibleSamplesAreNoOps() {
        VehicleSpeedState state = VehicleSpeedState.unknown(TENANT, VEHICLE);
        assertEquals(SpeedEvaluationResult.Outcome.NO_OP,
                state.evaluate(position(1, BASE, null), Optional.of(THRESHOLD),
                        SpeedAttribution.unknown(), BASE, null, null).outcome());
        SpeedPosition stale = position(2, BASE.minusSeconds(301), "70");
        assertEquals(SpeedEvaluationResult.Outcome.NO_OP,
                state.evaluate(stale, Optional.of(THRESHOLD), SpeedAttribution.unknown(), BASE,
                        null, null).outcome());
    }

    @Test
    void belowAndEqualInitializeNormalWhileAboveStartsSilentCandidate() {
        assertEquals(VehicleSpeedState.MonitoringState.NORMAL, evaluateFirst("59").state().monitoringState());
        assertEquals(VehicleSpeedState.MonitoringState.NORMAL, evaluateFirst("60").state().monitoringState());
        SpeedEvaluationResult above = evaluateFirst("60.01");
        assertEquals(SpeedEvaluationResult.Outcome.CANDIDATE_UPDATED, above.outcome());
        assertEquals(VehicleSpeedState.MonitoringState.UNKNOWN, above.state().monitoringState());
        assertTrue(above.episode().isEmpty());
    }

    @Test
    void confirmsOnSecondDistinctConsecutiveAboveSampleAndReplayDoesNothing() {
        SpeedEvaluationResult first = evaluateFirst("70");
        SpeedEvaluationResult replay = first.state().evaluate(position(1, BASE, "70"), Optional.of(THRESHOLD),
                SpeedAttribution.unknown(), BASE.plusSeconds(1), null, null);
        assertEquals(SpeedEvaluationResult.Outcome.NO_OP, replay.outcome());
        SpeedEvaluationResult confirmed = first.state().evaluate(position(2, BASE.plusSeconds(1), "75"),
                Optional.of(THRESHOLD), SpeedAttribution.unknown(), BASE.plusSeconds(2), null, null);
        assertEquals(SpeedEvaluationResult.Outcome.EPISODE_CONFIRMED, confirmed.outcome());
        assertEquals(VehicleSpeedState.MonitoringState.SPEEDING, confirmed.state().monitoringState());
        assertEquals(2, confirmed.episode().orElseThrow().eligibleAboveThresholdSampleCount());
    }

    @Test
    void normalSampleBetweenCandidatesResetsCandidate() {
        VehicleSpeedState candidate = evaluateFirst("70").state();
        SpeedEvaluationResult normal = candidate.evaluate(position(2, BASE.plusSeconds(1), "60"),
                Optional.of(THRESHOLD), SpeedAttribution.unknown(), BASE.plusSeconds(2), null, null);
        assertEquals(VehicleSpeedState.MonitoringState.NORMAL, normal.state().monitoringState());
        assertEquals(0, normal.state().candidateSampleCount());
    }

    @Test
    void changedRuleDoesNotCarryCandidate() {
        VehicleSpeedState candidate = evaluateFirst("70").state();
        ResolvedSpeedThreshold changed = threshold(2, "65");
        SpeedEvaluationResult result = candidate.evaluate(position(2, BASE.plusSeconds(1), "70"),
                Optional.of(changed), SpeedAttribution.unknown(), BASE.plusSeconds(2), null, null);
        assertEquals(SpeedEvaluationResult.Outcome.CANDIDATE_UPDATED, result.outcome());
        assertEquals(positionId(2), result.state().candidateFirstPositionId());
    }

    @Test
    void speedingProgressesThenClearsAtEquality() {
        SpeedEvaluationResult confirmed = confirmed();
        SpeedingEpisode episode = confirmed.episode().orElseThrow();
        SpeedEvaluationResult progressed = confirmed.state().evaluate(position(3, BASE.plusSeconds(2), "80"),
                Optional.of(THRESHOLD), SpeedAttribution.unknown(), BASE.plusSeconds(3), episode, null);
        assertEquals(SpeedEvaluationResult.Outcome.EPISODE_PROGRESSED, progressed.outcome());
        assertEquals(new BigDecimal("8E+1"), progressed.episode().orElseThrow().maxObservedSpeedKph().value());
        SpeedEvaluationResult closed = progressed.state().evaluate(position(4, BASE.plusSeconds(3), "60"),
                Optional.of(THRESHOLD), SpeedAttribution.unknown(), BASE.plusSeconds(4),
                progressed.episode().orElseThrow(), null);
        assertEquals(SpeedEvaluationResult.Outcome.EPISODE_CLOSED, closed.outcome());
        assertFalse(closed.episode().orElseThrow().open());
        assertEquals(VehicleSpeedState.MonitoringState.NORMAL, closed.state().monitoringState());
    }

    @Test
    void configurationUnavailableIsExplicitAndOrderedPositionsCannotRewind() {
        VehicleSpeedState state = VehicleSpeedState.unknown(TENANT, VEHICLE);
        SpeedEvaluationResult unavailable = state.evaluate(position(1, BASE, "70"), Optional.empty(),
                SpeedAttribution.unknown(), BASE, null, null);
        assertEquals(SpeedEvaluationResult.Outcome.CONFIGURATION_UNAVAILABLE, unavailable.outcome());
        assertEquals(VehicleSpeedState.Availability.CONFIGURATION_UNAVAILABLE,
                unavailable.state().availability());
        SpeedEvaluationResult older = unavailable.state().evaluate(position(2, BASE.minusSeconds(1), "70"),
                Optional.of(THRESHOLD), SpeedAttribution.unknown(), BASE, null, null);
        assertEquals(SpeedEvaluationResult.Outcome.NO_OP, older.outcome());
    }

    @Test
    void equalTimestampUsesUuidTieBreak() {
        VehicleSpeedState first = evaluateFirst("70").state();
        SpeedPosition lowerId = new SpeedPosition(TENANT, VEHICLE,
                UUID.fromString("00000000-0000-0000-0000-000000000000"), BASE,
                speed("75"), false, true, true, true);
        assertEquals(SpeedEvaluationResult.Outcome.NO_OP,
                first.evaluate(lowerId, Optional.of(THRESHOLD), SpeedAttribution.unknown(),
                        BASE.plusSeconds(1), null, null).outcome());
    }

    private static SpeedEvaluationResult confirmed() {
        SpeedEvaluationResult first = evaluateFirst("70");
        return first.state().evaluate(position(2, BASE.plusSeconds(1), "75"), Optional.of(THRESHOLD),
                SpeedAttribution.unknown(), BASE.plusSeconds(2), null, null);
    }

    private static SpeedEvaluationResult evaluateFirst(String speed) {
        return VehicleSpeedState.unknown(TENANT, VEHICLE).evaluate(position(1, BASE, speed),
                Optional.of(THRESHOLD), SpeedAttribution.unknown(), BASE, null, null);
    }

    private static SpeedPosition position(int suffix, Instant time, String value) {
        return new SpeedPosition(TENANT, VEHICLE, positionId(suffix), time,
                value == null ? null : speed(value), false, true, true, true);
    }

    private static UUID positionId(int suffix) {
        return UUID.fromString("40000000-0000-0000-0000-" + String.format("%012d", suffix));
    }

    private static ResolvedSpeedThreshold threshold(long version, String value) {
        SpeedRule rule = new SpeedRule(RULE_ID, TENANT, "Tenant", SpeedRule.Scope.TENANT,
                null, null, speed(value), SpeedRule.Lifecycle.ACTIVE, version, BASE);
        return new ResolvedSpeedThreshold(rule, ResolvedSpeedThreshold.ThresholdSource.TENANT_CONFIG);
    }

    private static SpeedKph speed(String value) {
        return new SpeedKph(new BigDecimal(value));
    }
}
