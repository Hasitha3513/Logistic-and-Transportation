package com.transportlogistics.app.system.infrastructure.adapters.out;

import com.transportlogistics.app.fleet.DriverPayrollIntegrationPort;
import com.transportlogistics.app.integration.PayrollIntegrationConfigurationLookup;
import com.transportlogistics.app.shared.DurableEventEnvelope;
import com.transportlogistics.app.shared.DurableEventPublisher;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class DriverPayrollIntegrationAdapter implements DriverPayrollIntegrationPort {

    private final PayrollIntegrationConfigurationLookup lookup;
    private final DurableEventPublisher events;

    DriverPayrollIntegrationAdapter(
            PayrollIntegrationConfigurationLookup lookup,
            DurableEventPublisher events) {
        this.lookup = lookup;
        this.events = events;
    }

    @Override
    public Optional<UUID> activePayrollConfiguration(UUID tenantId) {
        return lookup.activePayrollConfiguration(tenantId);
    }

    @Override
    public void publish(DurableEventEnvelope event) {
        events.publish(event);
    }
}
