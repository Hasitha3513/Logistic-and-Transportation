package com.transportlogistics.app.integration.adapters.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface IntegrationExchangeAttemptJpaRepository extends JpaRepository<IntegrationExchangeAttemptEntity, UUID> {
    List<IntegrationExchangeAttemptEntity> findByTenantIdAndExchangeIdOrderByAttemptNumber(UUID tenantId,
                                                                                           UUID exchangeId);
}
