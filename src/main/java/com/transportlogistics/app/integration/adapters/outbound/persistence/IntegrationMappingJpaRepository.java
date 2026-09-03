package com.transportlogistics.app.integration.adapters.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

interface IntegrationMappingJpaRepository extends JpaRepository<IntegrationMappingEntity, UUID> {
    Optional<IntegrationMappingEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    @Query("select coalesce(max(m.mappingVersion), 0) from IntegrationMappingEntity m "
        + "where m.tenantId = :tenantId and m.configurationId = :configurationId and m.mappingKey = :mappingKey")
    int maxVersion(UUID tenantId, UUID configurationId, String mappingKey);
}
