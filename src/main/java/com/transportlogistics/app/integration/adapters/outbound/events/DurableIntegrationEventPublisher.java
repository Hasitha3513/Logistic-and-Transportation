package com.transportlogistics.app.integration.adapters.outbound.events;

import com.transportlogistics.app.integration.IntegrationPlatformProbeEvent;
import com.transportlogistics.app.integration.ports.outbound.IntegrationEventPublisher;
import com.transportlogistics.app.shared.DurableEventPublisher;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
class DurableIntegrationEventPublisher implements IntegrationEventPublisher {
    private final DurableEventPublisher events;

    DurableIntegrationEventPublisher(DurableEventPublisher events) { this.events = events; }

    @Override
    public void publishProbe(UUID tenantId, UUID configurationId, UUID probeId, long sequence,
                             OffsetDateTime occurredAt) {
        events.publish(new IntegrationPlatformProbeEvent(UUID.randomUUID(), tenantId, configurationId, probeId,
            sequence, occurredAt));
    }
}
