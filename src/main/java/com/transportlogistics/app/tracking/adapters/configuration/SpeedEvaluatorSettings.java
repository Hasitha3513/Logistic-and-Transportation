package com.transportlogistics.app.tracking.adapters.configuration;

import java.time.Duration;

public record SpeedEvaluatorSettings(int claimSize, int workerCount, int queueCapacity,
                                     int maximumAttempts, Duration leaseDuration,
                                     Duration retryBackoff, Duration shutdownGrace) {
    public SpeedEvaluatorSettings {
        if (claimSize < 1 || claimSize > 100 || workerCount < 1 || workerCount > 32
                || queueCapacity < 0 || queueCapacity > 10_000
                || maximumAttempts < 1 || maximumAttempts > 100
                || leaseDuration.isNegative() || leaseDuration.isZero()
                || retryBackoff.isNegative() || shutdownGrace.isNegative()) {
            throw new IllegalArgumentException("Speed evaluator settings are invalid");
        }
    }
}
