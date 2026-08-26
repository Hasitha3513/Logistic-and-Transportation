package com.transportlogistics.app.fleet.infrastructure.adapters.in.scheduling;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import static org.assertj.core.api.Assertions.assertThat;

class FleetNotificationSchedulerTest {
    @Test void usesFrozenHourlyAndDailySafeDefaults() throws Exception {
        var maintenance = FleetNotificationScheduler.class.getMethod("scanMaintenance").getAnnotation(Scheduled.class);
        var compliance = FleetNotificationScheduler.class.getMethod("scanCompliance").getAnnotation(Scheduled.class);
        assertThat(maintenance.fixedDelayString()).isEqualTo("${app.notification.producers.maintenance-delay:PT1H}");
        assertThat(compliance.fixedDelayString()).isEqualTo("${app.notification.producers.compliance-delay:PT24H}");
    }
}
