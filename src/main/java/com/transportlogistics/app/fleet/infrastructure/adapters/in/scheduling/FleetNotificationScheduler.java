package com.transportlogistics.app.fleet.infrastructure.adapters.in.scheduling;

import com.transportlogistics.app.fleet.application.service.ComplianceNotificationScanner;
import com.transportlogistics.app.fleet.application.service.MaintenanceDueNotificationScanner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FleetNotificationScheduler {
    private final MaintenanceDueNotificationScanner maintenance;
    private final ComplianceNotificationScanner compliance;

    public FleetNotificationScheduler(MaintenanceDueNotificationScanner maintenance,
                                      ComplianceNotificationScanner compliance) {
        this.maintenance = maintenance;
        this.compliance = compliance;
    }

    @Scheduled(fixedDelayString = "${app.notification.producers.maintenance-delay:PT1H}")
    public void scanMaintenance() { maintenance.scan(); }

    @Scheduled(fixedDelayString = "${app.notification.producers.compliance-delay:PT24H}")
    public void scanCompliance() { compliance.scan(); }
}
