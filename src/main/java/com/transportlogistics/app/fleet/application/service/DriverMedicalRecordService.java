package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.application.ports.in.DriverMedicalRecordUseCase;
import com.transportlogistics.app.fleet.application.ports.out.DriverMedicalRecordRepository;
import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.domain.model.DriverMedicalRecord;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Transactional
public class DriverMedicalRecordService implements DriverMedicalRecordUseCase {

    private final DriverRepository drivers;
    private final DriverMedicalRecordRepository medicalRecords;

    public DriverMedicalRecordService(DriverRepository drivers, DriverMedicalRecordRepository medicalRecords) {
        this.drivers = Objects.requireNonNull(drivers, "DriverRepository cannot be null");
        this.medicalRecords = Objects.requireNonNull(medicalRecords, "DriverMedicalRecordRepository cannot be null");
    }

    @Override
    public DriverMedicalRecord create(UUID driverId, CreateCommand command, String actor) {
        var driver = drivers.findByIdForUpdate(driverId)
                .orElseThrow(() -> new NotFoundException("Driver not found: " + driverId));

        var now = OffsetDateTime.now();
        var record = new DriverMedicalRecord(
                UUID.randomUUID(),
                driver.id(),
                command.assessmentDate(),
                command.validFrom(),
                command.validUntil(),
                command.fitnessStatus(),
                command.visionTestStatus(),
                command.restrictions(),
                command.examinerOrProvider(),
                command.certificateReference(),
                command.remarks(),
                true,
                now,
                now,
                actor,
                actor
        );
        return medicalRecords.save(record);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverMedicalRecord> list(UUID driverId) {
        drivers.findById(driverId)
                .orElseThrow(() -> new NotFoundException("Driver not found: " + driverId));
        return medicalRecords.findByDriverId(driverId);
    }

    @Override
    @Transactional(readOnly = true)
    public DriverMedicalRecord get(UUID driverId, UUID recordId) {
        return medicalRecords.findById(recordId)
                .filter(r -> r.driverId().equals(driverId))
                .orElseThrow(() -> new NotFoundException("Medical record not found: " + recordId));
    }

    @Override
    public DriverMedicalRecord update(UUID driverId, UUID recordId, UpdateCommand command, String actor) {
        drivers.findByIdForUpdate(driverId)
                .orElseThrow(() -> new NotFoundException("Driver not found: " + driverId));
        var existing = get(driverId, recordId);

        var updated = new DriverMedicalRecord(
                existing.id(),
                existing.driverId(),
                command.assessmentDate() != null ? command.assessmentDate() : existing.assessmentDate(),
                command.validFrom() != null ? command.validFrom() : existing.validFrom(),
                command.validUntil() != null ? command.validUntil() : existing.validUntil(),
                command.fitnessStatus() != null ? command.fitnessStatus() : existing.fitnessStatus(),
                command.visionTestStatus() != null ? command.visionTestStatus() : existing.visionTestStatus(),
                command.restrictions() != null ? command.restrictions() : existing.restrictions(),
                command.examinerOrProvider() != null ? command.examinerOrProvider() : existing.examinerOrProvider(),
                command.certificateReference() != null ? command.certificateReference() : existing.certificateReference(),
                command.remarks() != null ? command.remarks() : existing.remarks(),
                command.active() != null ? command.active() : existing.active(),
                existing.createdAt(),
                OffsetDateTime.now(),
                existing.createdBy(),
                actor
        );
        return medicalRecords.save(updated);
    }
}
