package com.transportlogistics.app.tenancy.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface TenantJpaRepository extends JpaRepository<TenantEntity, UUID> {
    java.util.List<TenantEntity> findByStatusOrderByTenantId(
            com.transportlogistics.app.tenancy.domain.model.TenantStatus status);
}
