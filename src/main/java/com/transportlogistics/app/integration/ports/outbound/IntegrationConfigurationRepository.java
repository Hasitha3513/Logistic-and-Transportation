package com.transportlogistics.app.integration.ports.outbound;

import com.transportlogistics.app.integration.domain.model.IntegrationConfiguration;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IntegrationConfigurationRepository {
    IntegrationConfiguration save(IntegrationConfiguration configuration);
    Optional<IntegrationConfiguration> findConfiguration(UUID tenantId, UUID id);
    boolean existsByNormalizedName(UUID tenantId, String normalizedName, UUID excludingId);
    List<IntegrationConfiguration> list(UUID tenantId, int page, int size);
    long count(UUID tenantId);
}
