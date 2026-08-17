package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.application.ports.out.VehicleMeterResetRepository;
import com.transportlogistics.app.fleet.domain.model.VehicleMeterReset;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class VehicleMeterResetPersistenceAdapter implements VehicleMeterResetRepository {
    private static final PageRequest ONE = PageRequest.of(0, 1);

    private final VehicleMeterResetJpaRepository repository;

    @Override
    public VehicleMeterReset save(VehicleMeterReset reset) {
        return map(repository.saveAndFlush(entity(reset)));
    }

    @Override
    public Optional<VehicleMeterReset> findById(UUID resetId) {
        return repository.findById(resetId).map(this::map);
    }

    @Override
    public List<VehicleMeterReset> findByVehicleId(UUID vehicleId) {
        return repository.findByVehicleIdOrderByEffectiveAtDescCreatedAtDesc(vehicleId).stream()
                .map(this::map).toList();
    }

    @Override
    public Optional<VehicleMeterReset> findLatestByVehicleIdAndReadingType(UUID vehicleId, VehicleReadingType readingType) {
        return repository.findLatestByVehicleIdAndReadingType(vehicleId, readingType, ONE).stream()
                .findFirst().map(this::map);
    }

    private VehicleMeterResetEntity entity(VehicleMeterReset reset) {
        var entity = new VehicleMeterResetEntity();
        entity.setId(reset.id());
        entity.setVehicleId(reset.vehicleId());
        entity.setReadingType(reset.readingType());
        entity.setPreviousReadingId(reset.previousReadingId());
        entity.setPreviousMeterValue(reset.previousMeterValue());
        entity.setNewReadingId(reset.newReadingId());
        entity.setNewMeterValue(reset.newMeterValue());
        entity.setEffectiveAt(reset.effectiveAt());
        entity.setReason(reset.reason());
        entity.setCreatedBy(reset.createdBy());
        entity.setApprovedBy(reset.approvedBy());
        entity.setNotes(reset.notes());
        entity.setCreatedAt(reset.createdAt());
        return entity;
    }

    private VehicleMeterReset map(VehicleMeterResetEntity entity) {
        return new VehicleMeterReset(entity.getId(), entity.getVehicleId(), entity.getReadingType(),
                entity.getPreviousReadingId(), entity.getPreviousMeterValue(), entity.getNewReadingId(),
                entity.getNewMeterValue(), entity.getEffectiveAt(), entity.getReason(), entity.getCreatedBy(),
                entity.getApprovedBy(), entity.getNotes(), entity.getCreatedAt());
    }
}
