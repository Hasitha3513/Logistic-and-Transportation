package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.application.ports.out.DriverExceptionRepository;
import com.transportlogistics.app.fleet.domain.model.DriverException;
import com.transportlogistics.app.fleet.domain.model.DriverExceptionStatus;
import com.transportlogistics.app.fleet.domain.model.DriverExceptionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DriverExceptionPersistenceAdapter implements DriverExceptionRepository {

    private final DriverExceptionJpaRepository repository;

    @Override
    public DriverException save(DriverException exception) {
        var entity = toEntity(exception);
        var saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<DriverException> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<DriverException> findByDriverId(UUID driverId) {
        return repository.findByDriverIdOrderByStartTimeAsc(driverId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean hasOverlappingException(UUID driverId, OffsetDateTime from, OffsetDateTime to, List<DriverExceptionStatus> blockingStatuses) {
        var statusNames = blockingStatuses.stream().map(Enum::name).toList();
        return repository.hasOverlap(driverId, from, to, statusNames);
    }

    @Override
    public boolean hasOverlappingExceptionExcluding(UUID driverId, OffsetDateTime from, OffsetDateTime to, List<DriverExceptionStatus> blockingStatuses, UUID excludeExceptionId) {
        var statusNames = blockingStatuses.stream().map(Enum::name).toList();
        return repository.hasOverlapExcluding(driverId, from, to, statusNames, excludeExceptionId);
    }

    private DriverExceptionEntity toEntity(DriverException model) {
        return new DriverExceptionEntity(
                model.id(),
                model.driverId(),
                model.exceptionType().name(),
                model.startTime(),
                model.endTime(),
                model.status().name(),
                model.reason(),
                model.remarks(),
                model.createdAt(),
                model.updatedAt(),
                model.createdBy(),
                model.updatedBy()
        );
    }

    private DriverException toDomain(DriverExceptionEntity entity) {
        return new DriverException(
                entity.getId(),
                entity.getDriverId(),
                DriverExceptionType.fromString(entity.getExceptionType()),
                entity.getStartTime(),
                entity.getEndTime(),
                DriverExceptionStatus.valueOf(entity.getStatus()),
                entity.getReason(),
                entity.getRemarks(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedBy()
        );
    }
}
