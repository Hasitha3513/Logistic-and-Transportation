package com.transportlogistics.app.integration.ports.outbound;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface IntegrationEventPublisher {
    void publishProbe(UUID tenantId, UUID configurationId, UUID probeId, long sequence, OffsetDateTime occurredAt);
}
