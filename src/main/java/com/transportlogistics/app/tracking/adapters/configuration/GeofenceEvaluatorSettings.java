package com.transportlogistics.app.tracking.adapters.configuration;

import java.time.Duration;

public record GeofenceEvaluatorSettings(int claimSize, int workerCount, int queueCapacity,
                                        Duration leaseDuration, Duration retryBackoff,
                                        Duration shutdownGrace) {
    public GeofenceEvaluatorSettings {
        if (claimSize < 1 || claimSize > 100 || workerCount < 1 || workerCount > 32
                || queueCapacity < 0 || queueCapacity > 10_000 || leaseDuration.isNegative()
                || leaseDuration.isZero() || retryBackoff.isNegative() || shutdownGrace.isNegative()) {
            throw new IllegalArgumentException("Geofence evaluator settings are invalid");
        }
    }
}
