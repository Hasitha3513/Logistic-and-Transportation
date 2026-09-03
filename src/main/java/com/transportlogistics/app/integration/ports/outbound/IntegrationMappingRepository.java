package com.transportlogistics.app.integration.ports.outbound;

import com.transportlogistics.app.integration.domain.model.IntegrationMapping;

import java.util.Optional;
import java.util.UUID;

public interface IntegrationMappingRepository {
    IntegrationMapping save(IntegrationMapping mapping);
    Optional<IntegrationMapping> findMapping(UUID tenantId, UUID id);
    int nextVersion(UUID tenantId, UUID configurationId, String mappingKey);
    void supersede(UUID tenantId, UUID mappingId);
}
