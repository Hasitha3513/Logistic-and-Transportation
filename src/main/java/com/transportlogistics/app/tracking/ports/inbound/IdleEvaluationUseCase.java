package com.transportlogistics.app.tracking.ports.inbound;

import com.transportlogistics.app.tracking.application.telemetry.HistoricalTelemetry;
import java.time.Instant;

public interface IdleEvaluationUseCase {
    void evaluate(HistoricalTelemetry telemetry, Instant evaluatedAt);
}
