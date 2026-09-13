package com.transportlogistics.app.tracking.domain.routedeviation;

import java.time.Instant;
import java.util.UUID;

public final class RouteDeviationEvaluator {
    private RouteDeviationEvaluator() { }

    public static RouteDeviationEvaluationResult evaluate(VehicleRouteDeviationState state,
            RouteDeviationEpisode activeEpisode, RouteDeviationPosition position, Instant evaluatedAt,
            UUID tripId, UUID driverId, UUID routeId, RouteVersion routeVersion,
            RouteDeviationRule rule, DistanceMeters distance) {
        requireSameScope(state, position, rule);
        if (!position.eligible(evaluatedAt) || !state.newer(position)) {
            return result(RouteDeviationEvaluationResult.Outcome.NO_OP_INELIGIBLE,
                    state, activeEpisode, false, false);
        }
        DistanceMeters effective = position.effectiveTolerance(rule);
        boolean inside = distance.compareTo(effective) <= 0;
        if (state.state() == VehicleRouteDeviationState.State.DEVIATING) {
            if (inside) {
                RouteDeviationEpisode closed = activeEpisode.close(position.sourceTimestamp(),
                        RouteDeviationEpisode.TerminalOutcome.RETURNED_TO_ROUTE);
                return result(RouteDeviationEvaluationResult.Outcome.EPISODE_CLOSED,
                        state.inside(position), closed, false, false);
            }
            RouteDeviationEpisode progressed = activeEpisode.progress(distance);
            boolean escalated = activeEpisode.severity() == RouteDeviationEpisode.Severity.WARNING
                    && progressed.severity() == RouteDeviationEpisode.Severity.HIGH;
            return result(escalated ? RouteDeviationEvaluationResult.Outcome.SEVERITY_ESCALATED
                            : RouteDeviationEvaluationResult.Outcome.EPISODE_PROGRESSED,
                    state.reset(position), progressed, false, escalated);
        }
        if (inside) {
            return result(state.candidate() == null
                            ? RouteDeviationEvaluationResult.Outcome.BASELINE_ON_ROUTE
                            : RouteDeviationEvaluationResult.Outcome.CANDIDATE_CLEARED,
                    state.baselineInside(position, tripId, routeId, routeVersion, rule, effective),
                    null, false, false);
        }
        if (!state.confirms(position, tripId, routeId, routeVersion, rule)) {
            return result(RouteDeviationEvaluationResult.Outcome.CANDIDATE_STARTED,
                    state.candidate(position, tripId, driverId, routeId, routeVersion, rule,
                            effective, distance), null, false, false);
        }
        VehicleRouteDeviationState.Candidate first = state.candidate();
        RouteDeviationPosition firstPosition = new RouteDeviationPosition(first.positionId(),
                state.tenantId(), state.vehicleId(), first.sourceTimestamp(), position.point(),
                position.accuracy(), true, false, RouteDeviationPosition.Trust.TRUSTED, true,
                RouteDeviationPosition.Ordering.IN_ORDER);
        RouteDeviationEpisode episode = RouteDeviationEpisode.confirm(state.tenantId(),
                state.vehicleId(), tripId, driverId, routeId, routeVersion, rule, firstPosition,
                position, first.distance(), distance, first.effectiveTolerance());
        return result(RouteDeviationEvaluationResult.Outcome.EPISODE_CONFIRMED,
                state.confirmed(position, episode.id()), episode, true,
                episode.severity() == RouteDeviationEpisode.Severity.HIGH);
    }

    private static void requireSameScope(VehicleRouteDeviationState state,
            RouteDeviationPosition position, RouteDeviationRule rule) {
        if (!state.tenantId().equals(position.tenantId())
                || !state.vehicleId().equals(position.vehicleId())
                || !state.tenantId().equals(rule.tenantId())) {
            throw new RouteDeviationException("TENANT_SCOPE_VIOLATION",
                    "State, position, and rule must share Tenant and Vehicle scope");
        }
    }

    private static RouteDeviationEvaluationResult result(RouteDeviationEvaluationResult.Outcome outcome,
            VehicleRouteDeviationState state, RouteDeviationEpisode episode,
            boolean detection, boolean escalation) {
        return new RouteDeviationEvaluationResult(outcome, state, episode, detection, escalation);
    }
}
