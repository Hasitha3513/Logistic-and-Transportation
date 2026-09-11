package com.transportlogistics.app.operations.adapters.inbound.scheduling;

import com.transportlogistics.app.operations.ports.inbound.OperationalExceptionUseCase;
import com.transportlogistics.app.tenancy.CurrentTenant;
import com.transportlogistics.app.tenancy.TenantJobExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.operations.sla.enabled", havingValue = "true", matchIfMissing = true)
class OperationalExceptionSlaScheduler {
    private final OperationalExceptionUseCase operations;
    private final TenantJobExecutor tenants;
    private final CurrentTenant currentTenant;

    OperationalExceptionSlaScheduler(OperationalExceptionUseCase operations, TenantJobExecutor tenants,
                                     CurrentTenant currentTenant) {
        this.operations = operations;
        this.tenants = tenants;
        this.currentTenant = currentTenant;
    }

    @Scheduled(fixedDelayString = "${app.operations.sla.worker-delay:PT1M}")
    void scan() {
        tenants.forEachActiveTenant("operational-exception-sla",
            () -> operations.scanDue(currentTenant.required().tenantId()));
    }
}
