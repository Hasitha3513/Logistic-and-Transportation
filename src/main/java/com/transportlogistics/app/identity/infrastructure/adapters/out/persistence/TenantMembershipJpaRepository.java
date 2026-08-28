package com.transportlogistics.app.identity.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface TenantMembershipJpaRepository extends JpaRepository<TenantMembershipEntity, UUID> {
    Optional<TenantMembershipEntity> findByUserId(UUID userId);
}
