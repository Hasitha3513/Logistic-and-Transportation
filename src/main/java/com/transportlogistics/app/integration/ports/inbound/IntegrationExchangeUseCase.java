package com.transportlogistics.app.integration.ports.inbound;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public interface IntegrationExchangeUseCase {
    void acceptProbe(ProbeFact fact);
    void acceptPayroll(ExchangeFact fact);
    void acceptBilling(ExchangeFact fact);
    void processDue(UUID tenantId);

    record ProbeFact(UUID eventId, UUID tenantId, UUID configurationId, String eventType, int version,
                     String aggregateType, OffsetDateTime occurredAt, Map<String, ?> payload) {}
    record ExchangeFact(UUID eventId, UUID tenantId, String eventType, int version, String aggregateType,
                        OffsetDateTime occurredAt, Map<String, ?> payload) {}
}
