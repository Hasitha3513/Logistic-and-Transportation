package com.transportlogistics.app.tracking.adapters.configuration;

import java.time.Duration;

public record ProviderCoordinatorSettings(
        int maxConnectionsPerTick,
        int maxWorkers,
        int queueCapacity,
        int maxDevicesPerFetch,
        int maxBatchesPerJob,
        int providerConcurrency,
        int responseByteLimit,
        Duration leaseDuration,
        Duration fetchDeadline,
        Duration failureBackoff,
        Duration shutdownGrace) {

    public ProviderCoordinatorSettings {
        if (maxConnectionsPerTick < 1 || maxConnectionsPerTick > 100
                || maxWorkers < 1 || maxWorkers > 64
                || queueCapacity < 0 || queueCapacity > 1_000
                || maxDevicesPerFetch < 1 || maxDevicesPerFetch > 500
                || maxBatchesPerJob < 1 || maxBatchesPerJob > 100
                || providerConcurrency < 1 || providerConcurrency > maxWorkers
                || responseByteLimit < 1 || responseByteLimit > 1_048_576) {
            throw new IllegalArgumentException("Provider coordinator bounds are invalid");
        }
        if (leaseDuration == null || fetchDeadline == null
                || leaseDuration.compareTo(fetchDeadline) <= 0
                || leaseDuration.compareTo(Duration.ofMinutes(10)) > 0
                || fetchDeadline.isNegative() || fetchDeadline.isZero()
                || failureBackoff == null || failureBackoff.isNegative() || failureBackoff.isZero()
                || shutdownGrace == null || shutdownGrace.isNegative()) {
            throw new IllegalArgumentException("Provider coordinator durations are invalid");
        }
    }
}
