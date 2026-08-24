package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.application.ports.out.DriverMedicalRecordRepository;
import com.transportlogistics.app.fleet.domain.model.DriverMedicalRecord;
import com.transportlogistics.app.fleet.domain.model.DriverMedicalStatus;
import com.transportlogistics.app.fleet.domain.model.VisionTestStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;

@Component
class DriverMedicalRecordPersistenceAdapter implements DriverMedicalRecordRepository {

    private final DriverMedicalRecordJpaRepository repository;

    public DriverMedicalRecordPersistenceAdapter(DriverMedicalRecordJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public DriverMedicalRecord save(DriverMedicalRecord record) {
        var entity = toEntity(record);
        var saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<DriverMedicalRecord> findByDriverId(UUID driverId) {
        return repository.findByDriverIdOrderByValidUntilDesc(driverId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<DriverMedicalRecord> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<DriverMedicalRecord> findLatestByDriverId(UUID driverId) {
        return repository.findLatestApplicableByDriverId(driverId).map(this::toDomain);
    }

    @Override
    public List<DriverMedicalRecord> findActiveFitExpiringBy(LocalDate cutoffInclusive) {
        return repository.findActiveFitExpiringBy(cutoffInclusive).stream().map(this::toDomain).toList();
    }

    private DriverMedicalRecordEntity toEntity(DriverMedicalRecord r) {
        return new DriverMedicalRecordEntity(
                r.id(),
                r.driverId(),
                r.assessmentDate(),
                r.validFrom(),
                r.validUntil(),
                r.fitnessStatus().name(),
                r.visionTestStatus() != null ? r.visionTestStatus().name() : null,
                r.restrictions(),
                r.examinerOrProvider(),
                r.certificateReference(),
                r.remarks(),
                r.active(),
                r.createdAt(),
                r.updatedAt(),
                r.createdBy(),
                r.updatedBy()
        );
    }

    private DriverMedicalRecord toDomain(DriverMedicalRecordEntity e) {
        return new DriverMedicalRecord(
                e.getId(),
                e.getDriverId(),
                e.getAssessmentDate(),
                e.getValidFrom(),
                e.getValidUntil(),
                DriverMedicalStatus.fromString(e.getFitnessStatus()),
                e.getVisionTestStatus() != null ? VisionTestStatus.fromString(e.getVisionTestStatus()) : null,
                e.getRestrictions(),
                e.getExaminerOrProvider(),
                e.getCertificateReference(),
                e.getRemarks(),
                e.isActive(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getCreatedBy(),
                e.getUpdatedBy()
        );
    }
}
