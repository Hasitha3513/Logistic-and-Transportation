package com.transportlogistics.app.fleet.application.ports.out;

import com.transportlogistics.app.fleet.domain.model.MaintenanceSchedule;
import com.transportlogistics.app.fleet.domain.model.MaintenanceStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MaintenanceScheduleRepository {

    MaintenanceSchedule save(MaintenanceSchedule schedule);

    Optional<MaintenanceSchedule> findById(UUID id);

    List<MaintenanceSchedule> findByVehicleId(UUID vehicleId);

    boolean hasOverlappingSchedule(UUID vehicleId, OffsetDateTime from, OffsetDateTime to, List<MaintenanceStatus> blockingStatuses);

    boolean hasOverlappingScheduleExcluding(UUID vehicleId, OffsetDateTime from, OffsetDateTime to, List<MaintenanceStatus> blockingStatuses, UUID excludeScheduleId);
}
