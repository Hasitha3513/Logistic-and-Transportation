package com.transportlogistics.app.billing;

import com.transportlogistics.app.shared.DurableEventEnvelope;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record TransportBillingExportRequestedV1(UUID eventId, UUID tenantId, UUID aggregateId,
        OffsetDateTime occurredAt, Map<String, ?> payload) implements DurableEventEnvelope {
    public static final String EVENT_TYPE = "TRANSPORT_BILLING_V1";
    public static final String CONSUMER = "integration-outbound-exchange";
    @Override public String eventType() { return EVENT_TYPE; }
    @Override public int version() { return 1; }
    @Override public String aggregateType() { return "TRANSPORT_BILLING_RECORD"; }
    @Override public String durableConsumer() { return CONSUMER; }
}
