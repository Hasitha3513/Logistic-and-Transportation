package com.transportlogistics.app.tracking.adapters.inbound.flespi;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("trackingFlespi")
final class FlespiAdapterHealthIndicator implements HealthIndicator {
    private final FlespiAdapterState state;

    FlespiAdapterHealthIndicator(FlespiAdapterState state) {
        this.state = state;
    }

    @Override
    public Health health() {
        var snapshot = state.snapshot();
        var builder = snapshot.reachable() ? Health.up() : Health.unknown();
        return builder.withDetail("adapterRegistered", true)
                .withDetail("connectionCount", state.connectionCount())
                .withDetail("reachable", snapshot.reachable())
                .withDetail("lastSuccessfulPoll", snapshot.lastSuccessfulPoll())
                .withDetail("lastProviderMessage", snapshot.lastProviderMessage())
                .withDetail("lastFailureCategory", snapshot.lastFailureCategory().name())
                .build();
    }
}
