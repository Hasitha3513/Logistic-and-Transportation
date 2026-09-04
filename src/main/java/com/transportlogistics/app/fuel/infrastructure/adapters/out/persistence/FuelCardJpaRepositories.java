package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface FuelCardJpaRepository extends JpaRepository<FuelCardJpaEntity, UUID>, JpaSpecificationExecutor<FuelCardJpaEntity> {
    Optional<FuelCardJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);
    List<FuelCardJpaEntity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    boolean existsByTenantIdAndProviderIdAndProviderCardReference(UUID tenantId, UUID providerId, String reference);
    Optional<FuelCardJpaEntity> findByTenantIdAndProviderIdAndProviderCardReference(UUID tenantId, UUID providerId, String reference);
}
interface FuelCardBindingJpaRepository extends JpaRepository<FuelCardBindingJpaEntity, UUID> {
    Optional<FuelCardBindingJpaEntity> findByTenantIdAndCardIdAndEffectiveToIsNull(UUID tenantId, UUID cardId);
    List<FuelCardBindingJpaEntity> findByTenantIdAndCardIdOrderByEffectiveFromDesc(UUID tenantId, UUID cardId);
}
interface FuelCardRestrictionJpaRepository extends JpaRepository<FuelCardRestrictionJpaEntity, UUID> {
    Optional<FuelCardRestrictionJpaEntity> findByTenantIdAndCardId(UUID tenantId, UUID cardId);
    boolean existsByTenantIdAndCardId(UUID tenantId, UUID cardId);
}
interface FuelCardAuditJpaRepository extends JpaRepository<FuelCardAuditJpaEntity, UUID> {
    List<FuelCardAuditJpaEntity> findByTenantIdAndCardIdOrderByCreatedAtDesc(UUID tenantId, UUID cardId, Pageable pageable);
}
