package com.transportlogistics.app.fleet.application.ports.in;

import com.transportlogistics.app.fleet.domain.model.MaintenanceSchedule;
import com.transportlogistics.app.fleet.domain.model.MaintenanceStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface MaintenanceScheduleUseCase {

    MaintenanceSchedule create(UUID vehicleId, CreateCommand command, String actor);

    MaintenanceSchedule get(UUID vehicleId, UUID scheduleId);

    List<MaintenanceSchedule> list(UUID vehicleId);

    MaintenanceSchedule update(UUID vehicleId, UUID scheduleId, UpdateCommand command, String actor);

    MaintenanceSchedule cancel(UUID vehicleId, UUID scheduleId, String reason, String actor);

    MaintenanceSchedule complete(UUID vehicleId, UUID scheduleId, String remarks, String actor);

    boolean hasOverlappingSchedule(UUID vehicleId, OffsetDateTime from, OffsetDateTime to);

    record CreateCommand(
            String maintenanceType,
            OffsetDateTime scheduledStart,
            OffsetDateTime scheduledEnd,
            String description,
            String serviceProvider,
            BigDecimal cost
    ) {
    }

    record UpdateCommand(
            String maintenanceType,
            OffsetDateTime scheduledStart,
            OffsetDateTime scheduledEnd,
            MaintenanceStatus status,
            String description,
            String serviceProvider,
            BigDecimal cost
    ) {
    }
}
