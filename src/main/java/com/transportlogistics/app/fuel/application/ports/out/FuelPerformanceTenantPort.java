package com.transportlogistics.app.fuel.application.ports.out;

import java.util.UUID;

public interface FuelPerformanceTenantPort {
    TenantContext required();
    record TenantContext(UUID tenantId, String timeZone, String currency) {}
}
