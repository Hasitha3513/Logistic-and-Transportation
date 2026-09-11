package com.transportlogistics.app.tracking.domain.speed;

import java.util.Optional;

public record SpeedEvaluationResult(Outcome outcome, VehicleSpeedState state,
                                    Optional<SpeedingEpisode> episode) {
    public enum Outcome {
        NO_OP, STATE_INITIALIZED, CANDIDATE_UPDATED, EPISODE_CONFIRMED,
        EPISODE_PROGRESSED, EPISODE_CLOSED, CONFIGURATION_UNAVAILABLE
    }

    public SpeedEvaluationResult {
        episode = episode == null ? Optional.empty() : episode;
    }

    public static SpeedEvaluationResult of(Outcome outcome, VehicleSpeedState state) {
        return new SpeedEvaluationResult(outcome, state, Optional.empty());
    }

    public static SpeedEvaluationResult withEpisode(Outcome outcome, VehicleSpeedState state,
                                                     SpeedingEpisode episode) {
        return new SpeedEvaluationResult(outcome, state, Optional.of(episode));
    }
}
