package com.transportlogistics.app.identity.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByUsername(String username);

    @Query("select user from UserEntity user where user.id = :id and exists " +
            "(select membership.membershipId from TenantMembershipEntity membership where membership.userId = user.id " +
            "and membership.tenantId = :tenantId and membership.status = com.transportlogistics.app.identity.domain.model.TenantMembershipStatus.ACTIVE)")
    Optional<UserEntity> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("select user from UserEntity user where exists " +
            "(select membership.membershipId from TenantMembershipEntity membership where membership.userId = user.id " +
            "and membership.tenantId = :tenantId and membership.status = com.transportlogistics.app.identity.domain.model.TenantMembershipStatus.ACTIVE)")
    List<UserEntity> findAllByTenantId(@Param("tenantId") UUID tenantId);
}
