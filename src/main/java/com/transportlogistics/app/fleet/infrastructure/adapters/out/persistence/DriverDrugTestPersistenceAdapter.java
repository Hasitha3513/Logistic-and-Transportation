package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.application.ports.out.DriverDrugTestRepository;
import com.transportlogistics.app.fleet.domain.model.DriverDrugTest;
import com.transportlogistics.app.fleet.domain.model.DrugTestResult;
import com.transportlogistics.app.fleet.domain.model.DrugTestStatus;
import com.transportlogistics.app.fleet.domain.model.DrugTestType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class DriverDrugTestPersistenceAdapter implements DriverDrugTestRepository {

    private final DriverDrugTestJpaRepository repository;

    public DriverDrugTestPersistenceAdapter(DriverDrugTestJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public DriverDrugTest save(DriverDrugTest test) {
        var entity = toEntity(test);
        var saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<DriverDrugTest> findByDriverId(UUID driverId) {
        return repository.findByDriverIdOrderByScheduledDateDescCreatedAtDesc(driverId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<DriverDrugTest> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<DriverDrugTest> findActiveByDriverId(UUID driverId) {
        return repository.findByDriverIdAndActiveTrueOrderByScheduledDateDesc(driverId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<DriverDrugTest> findLatestByDriverId(UUID driverId) {
        return repository.findLatestByDriverId(driverId).map(this::toDomain);
    }

    private DriverDrugTestEntity toEntity(DriverDrugTest t) {
        return new DriverDrugTestEntity(
                t.id(),
                t.driverId(),
                t.testType().name(),
                t.scheduledDate(),
                t.sampleCollectedAt(),
                t.resultDate(),
                t.result().name(),
                t.status().name(),
                t.laboratoryOrProvider(),
                t.referenceNumber(),
                t.remarks(),
                t.returnToDutyRequired(),
                t.returnToDutyClearedAt(),
                t.active(),
                t.createdAt(),
                t.updatedAt(),
                t.createdBy(),
                t.updatedBy()
        );
    }

    private DriverDrugTest toDomain(DriverDrugTestEntity e) {
        return new DriverDrugTest(
                e.getId(),
                e.getDriverId(),
                DrugTestType.fromString(e.getTestType()),
                e.getScheduledDate(),
                e.getSampleCollectedAt(),
                e.getResultDate(),
                DrugTestResult.fromString(e.getResult()),
                DrugTestStatus.fromString(e.getStatus()),
                e.getLaboratoryOrProvider(),
                e.getReferenceNumber(),
                e.getRemarks(),
                e.isReturnToDutyRequired(),
                e.getReturnToDutyClearedAt(),
                e.isActive(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getCreatedBy(),
                e.getUpdatedBy()
        );
    }
}
