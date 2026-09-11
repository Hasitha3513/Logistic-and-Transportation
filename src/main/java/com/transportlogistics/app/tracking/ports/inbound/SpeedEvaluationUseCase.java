package com.transportlogistics.app.tracking.ports.inbound;

import com.transportlogistics.app.tracking.domain.speed.SpeedEvaluationResult;
import com.transportlogistics.app.tracking.domain.speed.SpeedPosition;
import java.time.Instant;

public interface SpeedEvaluationUseCase {
    SpeedEvaluationResult evaluate(SpeedPosition position, Instant evaluatedAt);
}
