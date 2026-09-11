package com.transportlogistics.app.integration.adapters.inbound.scheduling;

import com.transportlogistics.app.integration.ports.inbound.IntegrationExchangeUseCase;
import com.transportlogistics.app.tenancy.CurrentTenant;
import com.transportlogistics.app.tenancy.TenantJobExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.integration.exchange.enabled", havingValue = "true", matchIfMissing = true)
class IntegrationExchangeScheduler {
    private final IntegrationExchangeUseCase exchanges;
    private final TenantJobExecutor tenants;
    private final CurrentTenant currentTenant;

    IntegrationExchangeScheduler(IntegrationExchangeUseCase exchanges, TenantJobExecutor tenants,
                                 CurrentTenant currentTenant) {
        this.exchanges = exchanges;
        this.tenants = tenants;
        this.currentTenant = currentTenant;
    }

    @Scheduled(fixedDelayString = "${app.integration.exchange.worker-delay:PT5S}")
    void processDue() {
        tenants.forEachActiveTenant("integration-exchange",
            () -> exchanges.processDue(currentTenant.required().tenantId()));
    }
}
