package com.transportlogistics.app.tracking.domain.speed;

import java.util.Objects;

public record ResolvedSpeedThreshold(SpeedRule rule, ThresholdSource source) {
    public enum ThresholdSource { ROUTE_CONFIG, TENANT_CONFIG }

    public ResolvedSpeedThreshold {
        Objects.requireNonNull(rule, "Rule is required");
        Objects.requireNonNull(source, "Threshold source is required");
        if (rule.lifecycle() != SpeedRule.Lifecycle.ACTIVE) {
            throw new SpeedMonitoringException("SPEED_CONFIGURATION_UNAVAILABLE", "Resolved rule must be active");
        }
    }
}
