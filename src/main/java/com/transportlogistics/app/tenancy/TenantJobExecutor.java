package com.transportlogistics.app.tenancy;

import java.util.UUID;

/** Executes scheduled/background work once per active Tenant with explicit context. */
public class TenantJobExecutor {
    private static final UUID SYSTEM_ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final TenantDirectory tenants;
    private final TenantContextExecutor contexts;

    public TenantJobExecutor(TenantDirectory tenants, TenantContextExecutor contexts) {
        this.tenants = tenants;
        this.contexts = contexts;
    }

    public void forEachActiveTenant(String jobName, Runnable work) {
        tenants.findActiveTenants().forEach(tenant -> contexts.within(
                new TenantExecutionContext(tenant.tenantId(), SYSTEM_ACTOR, "system:" + jobName,
                        jobName + ":" + UUID.randomUUID()),
                work));
    }
}
