package com.transportlogistics.app.tracking.ports.inbound;

import com.transportlogistics.app.tracking.domain.speed.SpeedEvaluationJob;
import com.transportlogistics.app.tracking.domain.speed.SpeedEvaluationResult;
import java.time.Instant;
import java.util.Optional;

public interface SpeedEvaluationJobUseCase {
    Optional<SpeedEvaluationResult> process(SpeedEvaluationJob job, String leaseOwner, Instant evaluatedAt);
}
