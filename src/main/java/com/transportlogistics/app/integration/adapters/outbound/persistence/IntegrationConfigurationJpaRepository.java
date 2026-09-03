package com.transportlogistics.app.integration.adapters.outbound.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface IntegrationConfigurationJpaRepository extends JpaRepository<IntegrationConfigurationEntity, UUID> {
    Optional<IntegrationConfigurationEntity> findByTenantIdAndId(UUID tenantId, UUID id);
    boolean existsByTenantIdAndNormalizedName(UUID tenantId, String normalizedName);
    boolean existsByTenantIdAndNormalizedNameAndIdNot(UUID tenantId, String normalizedName, UUID id);
    List<IntegrationConfigurationEntity> findByTenantIdOrderByNormalizedName(UUID tenantId, Pageable pageable);
    long countByTenantId(UUID tenantId);
}
