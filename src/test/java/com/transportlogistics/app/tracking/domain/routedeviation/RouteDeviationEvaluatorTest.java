package com.transportlogistics.app.tracking.domain.routedeviation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RouteDeviationEvaluatorTest {
    private final UUID tenantId = UUID.randomUUID();
    private final UUID vehicleId = UUID.randomUUID();
    private final UUID tripId = UUID.randomUUID();
    private final UUID routeId = UUID.randomUUID();
    private final RouteVersion routeVersion = RouteVersion.ofRevision(1);
    private final Instant now = Instant.parse("2026-01-01T00:05:00Z");
    private final RouteDeviationRule rule = new RouteDeviationRule(UUID.randomUUID(), tenantId,
            routeId, routeVersion, DistanceMeters.of(100), RouteDeviationRule.Lifecycle.ACTIVE,
            1, 1, now.minusSeconds(60));

    @Test
    void confirmsOnSecondOutsideAndClearsOnFirstInside() {
        VehicleRouteDeviationState state = VehicleRouteDeviationState.unknown(tenantId, vehicleId);
        RouteDeviationEvaluationResult first = evaluate(state, null, position(now.minusSeconds(2)), 110);
        assertEquals(RouteDeviationEvaluationResult.Outcome.CANDIDATE_STARTED, first.outcome());
        assertFalse(first.detectionPublication());

        RouteDeviationEvaluationResult second = evaluate(first.state(), null,
                position(now.minusSeconds(1)), 120);
        assertEquals(RouteDeviationEvaluationResult.Outcome.EPISODE_CONFIRMED, second.outcome());
        assertEquals(VehicleRouteDeviationState.State.DEVIATING, second.state().state());
        assertTrue(second.detectionPublication());

        RouteDeviationEvaluationResult cleared = evaluate(second.state(), second.episode(),
                position(now), 100);
        assertEquals(RouteDeviationEvaluationResult.Outcome.EPISODE_CLOSED, cleared.outcome());
        assertEquals(RouteDeviationEpisode.TerminalOutcome.RETURNED_TO_ROUTE,
                cleared.episode().terminalOutcome());
        assertEquals(VehicleRouteDeviationState.State.ON_ROUTE, cleared.state().state());
    }

    @Test
    void contextChangeRestartsCandidateAndExactTwoTimesIsWarning() {
        RouteDeviationEvaluationResult first = evaluate(
                VehicleRouteDeviationState.unknown(tenantId, vehicleId), null,
                position(now.minusSeconds(2)), 120);
        RouteDeviationRule changed = new RouteDeviationRule(UUID.randomUUID(), tenantId, routeId,
                routeVersion, DistanceMeters.of(100), RouteDeviationRule.Lifecycle.ACTIVE,
                2, 1, now.minusSeconds(1));
        RouteDeviationEvaluationResult restarted = RouteDeviationEvaluator.evaluate(first.state(), null,
                position(now.minusSeconds(1)), now, tripId, null, routeId, routeVersion,
                changed, DistanceMeters.of(200));
        assertEquals(RouteDeviationEvaluationResult.Outcome.CANDIDATE_STARTED, restarted.outcome());

        RouteDeviationEvaluationResult confirmed = RouteDeviationEvaluator.evaluate(restarted.state(),
                null, position(now), now, tripId, null, routeId, routeVersion,
                changed, DistanceMeters.of(200));
        assertEquals(RouteDeviationEpisode.Severity.WARNING, confirmed.episode().severity());
        assertFalse(confirmed.escalationPublication());
    }

    @Test
    void staleAndUntrustedPositionsCannotMutateState() {
        RouteDeviationEvaluationResult baseline = evaluate(
                VehicleRouteDeviationState.unknown(tenantId, vehicleId), null,
                position(now.minusSeconds(1)), 100);
        RouteDeviationPosition stale = new RouteDeviationPosition(UUID.randomUUID(), tenantId,
                vehicleId, now.minusSeconds(400), point(), DistanceMeters.of(0), true, false,
                RouteDeviationPosition.Trust.TRUSTED, true, RouteDeviationPosition.Ordering.IN_ORDER);
        RouteDeviationEvaluationResult ignored = evaluate(baseline.state(), null, stale, 500);
        assertEquals(RouteDeviationEvaluationResult.Outcome.NO_OP_INELIGIBLE, ignored.outcome());
        assertSame(baseline.state(), ignored.state());
    }

    private RouteDeviationEvaluationResult evaluate(VehicleRouteDeviationState state,
            RouteDeviationEpisode episode, RouteDeviationPosition position, double distance) {
        return RouteDeviationEvaluator.evaluate(state, episode, position, now, tripId, null,
                routeId, routeVersion, rule, DistanceMeters.of(distance));
    }

    private RouteDeviationPosition position(Instant sourceTimestamp) {
        return new RouteDeviationPosition(UUID.randomUUID(), tenantId, vehicleId, sourceTimestamp,
                point(), DistanceMeters.of(0), true, false, RouteDeviationPosition.Trust.TRUSTED,
                true, RouteDeviationPosition.Ordering.IN_ORDER);
    }

    private static RoutePoint point() {
        return new RoutePoint(BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
