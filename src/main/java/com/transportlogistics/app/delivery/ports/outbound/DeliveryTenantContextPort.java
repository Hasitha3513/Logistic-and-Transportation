package com.transportlogistics.app.delivery.ports.outbound;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryTenantContextPort {
    Optional<TenantContext> currentTenant();

    default Optional<UUID> currentTenantId() {
        return currentTenant().map(TenantContext::tenantId);
    }

    record TenantContext(UUID tenantId, String timeZone) {}
}
