package com.transportlogistics.app.tenancy;

import java.util.UUID;

public record TenantExecutionContext(UUID tenantId, UUID actorId, String username, String correlationId) {
    public TenantExecutionContext {
        if (tenantId == null || actorId == null || username == null || username.isBlank()) {
            throw new IllegalArgumentException("Tenant, actor, and username are required");
        }
    }
}
