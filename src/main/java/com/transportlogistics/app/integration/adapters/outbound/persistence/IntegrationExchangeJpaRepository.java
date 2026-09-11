package com.transportlogistics.app.integration.adapters.outbound.persistence;

import com.transportlogistics.app.integration.domain.model.IntegrationConfiguration;
import com.transportlogistics.app.integration.domain.model.IntegrationExchange;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface IntegrationExchangeJpaRepository extends JpaRepository<IntegrationExchangeEntity, UUID> {
    Optional<IntegrationExchangeEntity> findByTenantIdAndId(UUID tenantId, UUID id);
    Optional<IntegrationExchangeEntity> findByTenantIdAndConfigurationIdAndSourceEventIdAndMappingVersionId(
        UUID tenantId, UUID configurationId, UUID sourceEventId, UUID mappingVersionId);
    List<IntegrationExchangeEntity> findByTenantIdAndConfigurationIdOrderByCreatedAtDesc(
        UUID tenantId, UUID configurationId, Pageable pageable);
    long countByTenantIdAndConfigurationId(UUID tenantId, UUID configurationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from IntegrationExchangeEntity e where e.tenantId = :tenantId "
        + "and e.attemptCount < :maximumAttempts and ((e.status in :readyStatuses and e.nextAttemptAt <= :now) "
        + "or (e.status = :processingStatus and e.lockedUntil <= :now)) "
        + "and e.configurationId in (select c.id from IntegrationConfigurationEntity c "
        + "where c.tenantId = :tenantId and c.lifecycle = :activeLifecycle) order by e.nextAttemptAt, e.createdAt")
    List<IntegrationExchangeEntity> findClaimable(UUID tenantId, OffsetDateTime now,
        Collection<IntegrationExchange.Status> readyStatuses, IntegrationExchange.Status processingStatus,
        IntegrationConfiguration.Lifecycle activeLifecycle, int maximumAttempts, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from IntegrationExchangeEntity e where e.tenantId = :tenantId "
        + "and e.status = :processingStatus and e.lockedUntil <= :now and e.attemptCount >= :maximumAttempts")
    List<IntegrationExchangeEntity> findExpiredExhausted(UUID tenantId, OffsetDateTime now,
        IntegrationExchange.Status processingStatus, int maximumAttempts, Pageable pageable);
}
