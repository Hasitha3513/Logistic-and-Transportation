package com.transportlogistics.app.tenancy.application.ports.out;

import com.transportlogistics.app.tenancy.domain.model.Tenant;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface TenantRepository {
    Optional<Tenant> findById(UUID tenantId);

    default List<Tenant> findActive() {
        return List.of();
    }
}
