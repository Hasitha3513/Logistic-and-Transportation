package com.transportlogistics.app.fleet.infrastructure.config;

import com.transportlogistics.app.fleet.application.ports.out.*;
import com.transportlogistics.app.fleet.application.service.ComplianceNotificationScanner;
import com.transportlogistics.app.fleet.application.service.MaintenanceDueNotificationScanner;
import com.transportlogistics.app.fleet.vehiclemaster.ports.outbound.VehicleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableScheduling
class FleetNotificationConfig {
    @Bean
    MaintenanceDueNotificationScanner maintenanceDueNotificationScanner(
        MaintenanceScheduleRepository schedules, VehicleRepository vehicles,
        FleetOperationalNotificationPublisher publisher, Clock clock) {
        return new MaintenanceDueNotificationScanner(schedules, vehicles, publisher, clock);
    }

    @Bean
    ComplianceNotificationScanner complianceNotificationScanner(
        VehicleDocumentRepository documents, VehicleRepository vehicles,
        DriverMedicalRecordRepository medicalRecords, DriverLicenseRepository licenses,
        DriverRepository drivers, FleetOperationalNotificationPublisher publisher, Clock clock,
        @Value("${app.notification.time-zone:UTC}") String timeZone) {
        return new ComplianceNotificationScanner(documents, vehicles, medicalRecords, licenses, drivers,
            publisher, clock, ZoneId.of(timeZone));
    }
}
