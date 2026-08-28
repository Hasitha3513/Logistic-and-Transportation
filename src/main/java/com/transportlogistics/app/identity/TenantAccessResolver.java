package com.transportlogistics.app.identity;

import com.transportlogistics.app.tenancy.TenantDirectory;

import java.util.UUID;

public interface TenantAccessResolver {
    ResolvedTenant resolve(UUID userId);

    record ResolvedTenant(UUID membershipId, UUID tenantId, TenantDirectory.TenantView tenant) {
    }
}
