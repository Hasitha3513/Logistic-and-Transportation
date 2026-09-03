package com.transportlogistics.app.tenancy;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface TenantDirectory {
    Optional<TenantView> findTenant(UUID tenantId);

    default List<TenantView> findActiveTenants() {
        return List.of();
    }

    record TenantView(UUID tenantId, String tenantCode, String tenantName, String defaultCurrency,
                      String defaultTimeZone, String status) {
        public boolean active() {
            return "ACTIVE".equals(status);
        }
    }
}
