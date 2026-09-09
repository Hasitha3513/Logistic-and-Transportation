package com.transportlogistics.app.tracking.adapters.inbound.flespi;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("trackingFlespi")
final class FlespiAdapterHealthIndicator implements HealthIndicator {
    private final FlespiAdapterProperties properties;
    private final FlespiAdapterState state;

    FlespiAdapterHealthIndicator(FlespiAdapterProperties properties, FlespiAdapterState state) {
        this.properties = properties;
        this.state = state;
    }

    @Override
    public Health health() {
        var snapshot = state.snapshot();
        if (!properties.isEnabled()) {
            return Health.up().withDetail("enabled", false).withDetail("configured", false).build();
        }
        var builder = snapshot.configured() && snapshot.reachable() ? Health.up() : Health.unknown();
        return builder.withDetail("enabled", true)
                .withDetail("configured", snapshot.configured())
                .withDetail("reachable", snapshot.reachable())
                .withDetail("lastSuccessfulPoll", snapshot.lastSuccessfulPoll())
                .withDetail("lastProviderMessage", snapshot.lastProviderMessage())
                .withDetail("lastFailureCategory", snapshot.lastFailureCategory().name())
                .build();
    }
}
