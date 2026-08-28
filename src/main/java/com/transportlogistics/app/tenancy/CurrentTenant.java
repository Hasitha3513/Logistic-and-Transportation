package com.transportlogistics.app.tenancy;

import java.util.Optional;

public interface CurrentTenant {
    Optional<TenantExecutionContext> current();

    default TenantExecutionContext required() {
        return current().orElseThrow(() -> new IllegalStateException("Tenant context is not established"));
    }
}
