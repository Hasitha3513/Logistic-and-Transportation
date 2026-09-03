package com.transportlogistics.app.fleet.infrastructure.adapters.in.scheduling;

import com.transportlogistics.app.fleet.application.service.ComplianceNotificationScanner;
import com.transportlogistics.app.fleet.application.service.MaintenanceDueNotificationScanner;
import com.transportlogistics.app.tenancy.TenantJobExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FleetNotificationScheduler {
    private final MaintenanceDueNotificationScanner maintenance;
    private final ComplianceNotificationScanner compliance;
    private final TenantJobExecutor tenants;

    public FleetNotificationScheduler(MaintenanceDueNotificationScanner maintenance,
                                      ComplianceNotificationScanner compliance, TenantJobExecutor tenants) {
        this.maintenance = maintenance;
        this.compliance = compliance;
        this.tenants = tenants;
    }

    @Scheduled(fixedDelayString = "${app.notification.producers.maintenance-delay:PT1H}")
    public void scanMaintenance() { tenants.forEachActiveTenant("maintenance-scan", maintenance::scan); }

    @Scheduled(fixedDelayString = "${app.notification.producers.compliance-delay:PT24H}")
    public void scanCompliance() { tenants.forEachActiveTenant("compliance-scan", compliance::scan); }
}
