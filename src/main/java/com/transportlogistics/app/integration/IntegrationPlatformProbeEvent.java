package com.transportlogistics.app.integration;

import com.transportlogistics.app.shared.DurableEventEnvelope;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/** The only US-73 durable contract; contains controlled-sandbox data, never business data. */
public record IntegrationPlatformProbeEvent(
        UUID eventId,
        UUID tenantId,
        UUID configurationId,
        UUID probeId,
        long sequence,
        OffsetDateTime occurredAt
) implements DurableEventEnvelope {
    public static final String EVENT_TYPE = "US73_PLATFORM_PROBE_V1";
    public static final String CONSUMER = "integration-outbound-exchange";

    @Override public String eventType() { return EVENT_TYPE; }
    @Override public int version() { return 1; }
    @Override public String aggregateType() { return "INTEGRATION_CONFIGURATION"; }
    @Override public UUID aggregateId() { return configurationId; }
    @Override public String durableConsumer() { return CONSUMER; }

    @Override
    public Map<String, ?> payload() {
        return Map.of("probeId", probeId.toString(), "probeType", "CONTROLLED_SANDBOX", "sequence", sequence);
    }
}
