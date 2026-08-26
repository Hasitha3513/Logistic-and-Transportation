package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.application.ports.out.MaintenanceScheduleRepository;
import com.transportlogistics.app.fleet.domain.model.MaintenanceSchedule;
import com.transportlogistics.app.fleet.domain.model.MaintenanceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MaintenanceSchedulePersistenceAdapter implements MaintenanceScheduleRepository {

    private final MaintenanceScheduleJpaRepository repository;

    @Override
    public MaintenanceSchedule save(MaintenanceSchedule schedule) {
        var entity = toEntity(schedule);
        var saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<MaintenanceSchedule> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<MaintenanceSchedule> findByVehicleId(UUID vehicleId) {
        return repository.findByVehicleIdOrderByScheduledStartAsc(vehicleId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<MaintenanceSchedule> findScheduledStartingBetween(OffsetDateTime fromExclusive,
                                                                   OffsetDateTime toInclusive) {
        return repository.findScheduledStartingBetween(fromExclusive, toInclusive).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean hasOverlappingSchedule(UUID vehicleId, OffsetDateTime from, OffsetDateTime to, List<MaintenanceStatus> blockingStatuses) {
        var statusNames = blockingStatuses.stream().map(Enum::name).toList();
        return repository.hasOverlap(vehicleId, from, to, statusNames);
    }

    @Override
    public boolean hasOverlappingScheduleExcluding(UUID vehicleId, OffsetDateTime from, OffsetDateTime to, List<MaintenanceStatus> blockingStatuses, UUID excludeScheduleId) {
        var statusNames = blockingStatuses.stream().map(Enum::name).toList();
        return repository.hasOverlapExcluding(vehicleId, from, to, statusNames, excludeScheduleId);
    }

    private MaintenanceScheduleEntity toEntity(MaintenanceSchedule model) {
        return new MaintenanceScheduleEntity(
                model.id(),
                model.vehicleId(),
                model.maintenanceType(),
                model.scheduledStart(),
                model.scheduledEnd(),
                model.status().name(),
                model.description(),
                model.serviceProvider(),
                model.cost(),
                model.createdAt(),
                model.updatedAt(),
                model.createdBy(),
                model.updatedBy()
        );
    }

    private MaintenanceSchedule toDomain(MaintenanceScheduleEntity entity) {
        return new MaintenanceSchedule(
                entity.getId(),
                entity.getVehicleId(),
                entity.getMaintenanceType(),
                entity.getScheduledStart(),
                entity.getScheduledEnd(),
                MaintenanceStatus.valueOf(entity.getStatus()),
                entity.getDescription(),
                entity.getServiceProvider(),
                entity.getCost(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedBy()
        );
    }
}
