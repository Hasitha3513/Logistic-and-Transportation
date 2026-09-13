package com.transportlogistics.app.tracking.domain.routedeviation;

import java.util.Objects;

public record RouteDeviationEvaluationResult(Outcome outcome, VehicleRouteDeviationState state,
        RouteDeviationEpisode episode, boolean detectionPublication, boolean escalationPublication) {
    public enum Outcome { NO_OP_INELIGIBLE, STATE_UNAVAILABLE, BASELINE_ON_ROUTE, CANDIDATE_STARTED,
        CANDIDATE_CLEARED, EPISODE_CONFIRMED, EPISODE_PROGRESSED, SEVERITY_ESCALATED, EPISODE_CLOSED }
    public RouteDeviationEvaluationResult {
        Objects.requireNonNull(outcome, "Outcome is required");
        Objects.requireNonNull(state, "State is required");
    }
}
