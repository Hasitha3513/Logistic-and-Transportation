package com.transportlogistics.app.integration.ports.outbound;

import com.transportlogistics.app.integration.domain.model.IntegrationExchange;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IntegrationExchangeRepository {
    IntegrationExchange save(IntegrationExchange exchange);
    IntegrationExchange saveIfAbsent(IntegrationExchange exchange);
    List<IntegrationExchange> claimDue(UUID tenantId, OffsetDateTime now, int batchSize);
    List<IntegrationExchange> list(UUID tenantId, UUID configurationId, int page, int size);
    long count(UUID tenantId, UUID configurationId);
    Optional<IntegrationExchange> findExchange(UUID tenantId, UUID id);
}
