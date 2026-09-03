package com.transportlogistics.app.delivery.adapters.outbound.tenancy;

import com.transportlogistics.app.tenancy.CurrentTenant;
import com.transportlogistics.app.tenancy.TenantExecutionContext;
import com.transportlogistics.app.tenancy.TenantDirectory;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentTenantDeliveryContextAdapterTest {
    @Test
    void delegatesTenantAuthorityToCurrentTenant() {
        UUID tenantId = UUID.randomUUID();
        CurrentTenant currentTenant = () -> Optional.of(
                new TenantExecutionContext(tenantId, UUID.randomUUID(), "delivery.architect", "corr-1"));

        TenantDirectory tenants = id -> Optional.of(new TenantDirectory.TenantView(id, "CLTS-LK", "Tenant", "LKR", "Asia/Colombo", "ACTIVE"));
        var adapter = new CurrentTenantDeliveryContextAdapter(currentTenant, tenants);

        assertThat(adapter.currentTenant()).contains(new com.transportlogistics.app.delivery.ports.outbound.DeliveryTenantContextPort.TenantContext(tenantId, "Asia/Colombo"));
    }

    @Test
    void doesNotInventTenantWhenExecutionContextIsAbsent() {
        CurrentTenant currentTenant = Optional::empty;

        TenantDirectory tenants = id -> Optional.empty();
        var adapter = new CurrentTenantDeliveryContextAdapter(currentTenant, tenants);

        assertThat(adapter.currentTenantId()).isEmpty();
    }
}
