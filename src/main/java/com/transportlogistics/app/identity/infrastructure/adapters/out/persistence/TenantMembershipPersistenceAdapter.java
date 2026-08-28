package com.transportlogistics.app.identity.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.identity.application.ports.out.TenantMembershipRepository;
import com.transportlogistics.app.identity.domain.model.TenantMembership;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class TenantMembershipPersistenceAdapter implements TenantMembershipRepository {
    private final TenantMembershipJpaRepository repository;

    TenantMembershipPersistenceAdapter(TenantMembershipJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<TenantMembership> findByUserId(UUID userId) {
        return repository.findByUserId(userId).map(this::toDomain);
    }

    @Override
    public TenantMembership save(TenantMembership membership) {
        var entity = new TenantMembershipEntity();
        entity.setMembershipId(membership.membershipId());
        entity.setTenantId(membership.tenantId());
        entity.setUserId(membership.userId());
        entity.setStatus(membership.status());
        entity.setCreatedAt(membership.createdAt());
        entity.setCreatedBy(membership.createdBy());
        entity.setUpdatedAt(membership.updatedAt());
        entity.setUpdatedBy(membership.updatedBy());
        entity.setVersion(membership.version());
        return toDomain(repository.save(entity));
    }

    private TenantMembership toDomain(TenantMembershipEntity entity) {
        return new TenantMembership(entity.getMembershipId(), entity.getTenantId(), entity.getUserId(),
                entity.getStatus(), entity.getCreatedAt(), entity.getCreatedBy(), entity.getUpdatedAt(),
                entity.getUpdatedBy(), entity.getVersion());
    }
}
