package com.transportlogistics.app.integration.ports.outbound;

import com.transportlogistics.app.integration.domain.model.IntegrationExchangeAttempt;

import java.util.List;
import java.util.UUID;

public interface IntegrationAttemptRepository {
    IntegrationExchangeAttempt save(IntegrationExchangeAttempt attempt);
    List<IntegrationExchangeAttempt> findByExchange(UUID tenantId, UUID exchangeId);
}
