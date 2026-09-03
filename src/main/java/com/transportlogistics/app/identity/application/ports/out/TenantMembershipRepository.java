package com.transportlogistics.app.identity.application.ports.out;

import com.transportlogistics.app.identity.domain.model.TenantMembership;

import java.util.Optional;
import java.util.UUID;

public interface TenantMembershipRepository {
    Optional<TenantMembership> findByUserId(UUID userId);

    TenantMembership save(TenantMembership membership);
}
