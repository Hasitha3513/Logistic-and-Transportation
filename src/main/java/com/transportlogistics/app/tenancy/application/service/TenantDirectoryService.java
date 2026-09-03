package com.transportlogistics.app.tenancy.application.service;

import com.transportlogistics.app.tenancy.TenantDirectory;
import com.transportlogistics.app.tenancy.application.ports.out.TenantRepository;

public final class TenantDirectoryService implements TenantDirectory {
    private final TenantRepository tenants;

    public TenantDirectoryService(TenantRepository tenants) {
        this.tenants = tenants;
    }

    @Override
    public java.util.Optional<TenantView> findTenant(java.util.UUID tenantId) {
        return tenants.findById(tenantId).map(this::view);
    }

    @Override
    public java.util.List<TenantView> findActiveTenants() {
        return tenants.findActive().stream().map(this::view).toList();
    }

    private TenantView view(com.transportlogistics.app.tenancy.domain.model.Tenant tenant) {
        return new TenantView(tenant.tenantId(), tenant.tenantCode(),
                tenant.tenantName(), tenant.defaultCurrency().getCurrencyCode(), tenant.defaultTimeZone(),
                tenant.status().name());
    }
}
