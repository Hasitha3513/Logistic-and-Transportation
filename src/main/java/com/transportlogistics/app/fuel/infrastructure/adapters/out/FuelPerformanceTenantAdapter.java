package com.transportlogistics.app.fuel.infrastructure.adapters.out;

import com.transportlogistics.app.fuel.application.ports.out.FuelPerformanceTenantPort;
import com.transportlogistics.app.tenancy.CurrentTenant;
import com.transportlogistics.app.tenancy.TenantDirectory;
import org.springframework.stereotype.Component;

@Component
public class FuelPerformanceTenantAdapter implements FuelPerformanceTenantPort {
    private final CurrentTenant currentTenant;
    private final TenantDirectory tenants;

    public FuelPerformanceTenantAdapter(CurrentTenant currentTenant, TenantDirectory tenants) {
        this.currentTenant = currentTenant;
        this.tenants = tenants;
    }

    @Override
    public TenantContext required() {
        var context = currentTenant.required();
        var tenant = tenants.findTenant(context.tenantId())
                .orElseThrow(() -> new IllegalStateException("Active Tenant is unavailable"));
        return new TenantContext(context.tenantId(), tenant.defaultTimeZone(), tenant.defaultCurrency());
    }
}
