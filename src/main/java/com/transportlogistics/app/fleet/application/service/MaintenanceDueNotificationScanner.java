package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.application.ports.out.FleetOperationalNotificationPublisher;
import com.transportlogistics.app.fleet.application.ports.out.MaintenanceScheduleRepository;
import com.transportlogistics.app.fleet.vehiclemaster.ports.outbound.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.OffsetDateTime;

public class MaintenanceDueNotificationScanner {
    private static final Logger log = LoggerFactory.getLogger(MaintenanceDueNotificationScanner.class);
    private final MaintenanceScheduleRepository schedules;
    private final VehicleRepository vehicles;
    private final FleetOperationalNotificationPublisher publisher;
    private final Clock clock;

    public MaintenanceDueNotificationScanner(MaintenanceScheduleRepository schedules, VehicleRepository vehicles,
                                             FleetOperationalNotificationPublisher publisher, Clock clock) {
        this.schedules = schedules;
        this.vehicles = vehicles;
        this.publisher = publisher;
        this.clock = clock;
    }

    public void scan() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        for (var schedule : schedules.findScheduledStartingBetween(now, now.plusHours(24))) {
            if (schedule.status() != com.transportlogistics.app.fleet.domain.model.MaintenanceStatus.SCHEDULED
                || !schedule.scheduledStart().isAfter(now) || schedule.scheduledStart().isAfter(now.plusHours(24))) {
                continue;
            }
            try {
                vehicles.findById(schedule.vehicleId()).ifPresent(vehicle -> publisher.publish(
                    FleetOperationalNotificationEvents.maintenanceDue(schedule, vehicle, now)));
            } catch (RuntimeException exception) {
                log.error("Maintenance due notification publication failed for schedule {}", schedule.id(), exception);
            }
        }
    }
}
