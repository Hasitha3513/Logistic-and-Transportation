package com.transportlogistics.app.notification.infrastructure.adapters.in.scheduling;

import com.transportlogistics.app.shared.DurableEventWorker;
import com.transportlogistics.app.tenancy.TenantJobExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.integration.outbox.enabled", havingValue = "true", matchIfMissing = true)
class IntegrationOutboxScheduler {
    private final DurableEventWorker worker;
    private final TenantJobExecutor tenants;

    IntegrationOutboxScheduler(DurableEventWorker worker, TenantJobExecutor tenants) {
        this.worker = worker;
        this.tenants = tenants;
    }

    @Scheduled(fixedDelayString = "${app.integration.outbox.worker-delay:PT5S}")
    void processDue() {
        tenants.forEachActiveTenant("integration-outbox", worker::processDue);
    }
}
