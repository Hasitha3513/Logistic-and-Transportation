package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.application.ports.out.DriverViolationRepository;
import com.transportlogistics.app.fleet.domain.model.DriverViolation;
import com.transportlogistics.app.fleet.domain.model.DriverViolationType;
import com.transportlogistics.app.fleet.domain.model.FinePaymentStatus;
import com.transportlogistics.app.fleet.domain.model.ViolationSeverity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DriverViolationPersistenceAdapter implements DriverViolationRepository {

    private final DriverViolationJpaRepository repository;

    @Override
    public DriverViolation save(DriverViolation violation) {
        var entity = toEntity(violation);
        var saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<DriverViolation> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<DriverViolation> findByDriverId(UUID driverId) {
        return repository.findByDriverIdOrderByViolationDateDesc(driverId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<DriverViolation> findByDriverIdAndViolationDateBetween(UUID driverId, OffsetDateTime from, OffsetDateTime to) {
        return repository.findByDriverIdAndViolationDateBetweenOrderByViolationDateDesc(driverId, from, to).stream()
                .map(this::toDomain)
                .toList();
    }

    private DriverViolationEntity toEntity(DriverViolation model) {
        return new DriverViolationEntity(
                model.id(),
                model.driverId(),
                model.tripId(),
                model.violationType().name(),
                model.severity().name(),
                model.violationDate(),
                model.penaltyPoints(),
                model.fineAmount(),
                model.paymentStatus().name(),
                model.paidAt(),
                model.paymentReference(),
                model.location(),
                model.description(),
                model.createdAt(),
                model.updatedAt(),
                model.createdBy(),
                model.updatedBy()
        );
    }

    private DriverViolation toDomain(DriverViolationEntity entity) {
        return new DriverViolation(
                entity.getId(),
                entity.getDriverId(),
                entity.getTripId(),
                DriverViolationType.fromString(entity.getViolationType()),
                ViolationSeverity.valueOf(entity.getSeverity()),
                entity.getViolationDate(),
                entity.getPenaltyPoints(),
                entity.getFineAmount(),
                FinePaymentStatus.valueOf(entity.getPaymentStatus()),
                entity.getPaidAt(),
                entity.getPaymentReference(),
                entity.getLocation(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedBy()
        );
    }
}
