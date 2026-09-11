package com.transportlogistics.app.tracking.adapters.configuration;

import com.transportlogistics.app.tracking.adapters.inbound.provider.ProviderCoordinatorState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("trackingProviderCoordinator")
final class ProviderCoordinatorHealthIndicator implements HealthIndicator {
    private final ProviderCoordinatorState state;
    private final boolean enabled;

    ProviderCoordinatorHealthIndicator(
            ProviderCoordinatorState state,
            @Value("${app.tracking.provider-coordinator.enabled:false}") boolean enabled) {
        this.state = state;
        this.enabled = enabled;
    }

    @Override
    public Health health() {
        return Health.up()
                .withDetail("enabled", enabled)
                .withDetail("activeJobs", state.activeJobs())
                .withDetail("lastSuccessEpoch", state.lastSuccessEpoch())
                .withDetail("lastFailureEpoch", state.lastFailureEpoch())
                .build();
    }
}
