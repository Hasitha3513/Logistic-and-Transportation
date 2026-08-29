package com.transportlogistics.app.delivery.adapters.outbound.tenancy;

import com.transportlogistics.app.delivery.ports.outbound.DeliveryTenantContextPort;
import com.transportlogistics.app.tenancy.CurrentTenant;
import com.transportlogistics.app.tenancy.TenantDirectory;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class CurrentTenantDeliveryContextAdapter implements DeliveryTenantContextPort {
    private final CurrentTenant currentTenant;
    private final TenantDirectory tenants;

    CurrentTenantDeliveryContextAdapter(CurrentTenant currentTenant, TenantDirectory tenants) {
        this.currentTenant = currentTenant;
        this.tenants = tenants;
    }

    @Override
    public Optional<TenantContext> currentTenant() {
        return currentTenant.current().flatMap(context -> tenants.findTenant(context.tenantId())
                .filter(TenantDirectory.TenantView::active)
                .map(tenant -> new TenantContext(context.tenantId(), tenant.defaultTimeZone())));
    }
}
