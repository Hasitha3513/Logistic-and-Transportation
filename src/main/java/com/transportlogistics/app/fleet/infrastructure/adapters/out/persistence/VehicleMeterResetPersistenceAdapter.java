package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.application.ports.out.VehicleMeterResetRepository;
import com.transportlogistics.app.fleet.domain.model.VehicleMeterReset;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class VehicleMeterResetPersistenceAdapter implements VehicleMeterResetRepository {
    private final VehicleMeterResetJpaRepository repo;

    VehicleMeterResetPersistenceAdapter(VehicleMeterResetJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    public VehicleMeterReset save(VehicleMeterReset reset) {
        var entity = new VehicleMeterResetEntity(
                reset.id(),
                reset.vehicleId(),
                reset.readingType().name(),
                reset.fromEpoch(),
                reset.toEpoch(),
                reset.lastReadingValue(),
                reset.newMeterValue(),
                reset.effectiveAt(),
                reset.reason(),
                reset.createdBy(),
                reset.createdAt()
        );
        return toDomain(repo.save(entity));
    }

    @Override
    public List<VehicleMeterReset> findByVehicleId(UUID vehicleId) {
        return repo.findByVehicleIdOrderByEffectiveAtDesc(vehicleId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<VehicleMeterReset> findByVehicleIdAndType(UUID vehicleId, VehicleReadingType readingType) {
        return repo.findByVehicleIdAndReadingTypeOrderByEffectiveAtDesc(vehicleId, readingType.name())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<VehicleMeterReset> findLatestByVehicleIdAndType(UUID vehicleId, VehicleReadingType readingType) {
        return repo.findLatest(vehicleId, readingType.name()).map(this::toDomain);
    }

    private VehicleMeterReset toDomain(VehicleMeterResetEntity e) {
        return new VehicleMeterReset(
                e.getId(),
                e.getVehicleId(),
                VehicleReadingType.valueOf(e.getReadingType()),
                e.getFromEpoch(),
                e.getToEpoch(),
                e.getLastReadingValue(),
                e.getNewMeterValue(),
                e.getEffectiveAt(),
                e.getReason(),
                e.getCreatedBy(),
                e.getCreatedAt()
        );
    }
}