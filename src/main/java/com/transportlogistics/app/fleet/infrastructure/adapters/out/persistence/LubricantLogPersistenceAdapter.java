package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.application.ports.out.LubricantLogRepository;
import com.transportlogistics.app.fleet.domain.model.FluidType;
import com.transportlogistics.app.fleet.domain.model.LubricantLog;
import com.transportlogistics.app.fleet.domain.model.MeasurementUnit;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class LubricantLogPersistenceAdapter implements LubricantLogRepository {

    private final LubricantLogJpaRepository repository;

    public LubricantLogPersistenceAdapter(LubricantLogJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public LubricantLog save(LubricantLog log) {
        var entity = toEntity(log);
        var saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<LubricantLog> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<LubricantLog> findByVehicleId(UUID vehicleId) {
        return repository.findByVehicleIdOrderByRecordedAtDesc(vehicleId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<LubricantLog> findByVehicleIdWithFilters(
            UUID vehicleId,
            FluidType fluidType,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        var fluidTypeStr = fluidType != null ? fluidType.name() : null;
        return repository.findByVehicleIdWithFilters(vehicleId, fluidTypeStr, from, to)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private LubricantLogEntity toEntity(LubricantLog l) {
        return new LubricantLogEntity(
                l.id(),
                l.vehicleId(),
                l.fluidType().name(),
                l.quantity(),
                l.unit().name(),
                l.recordedAt(),
                l.odometerKm(),
                l.engineHours(),
                l.vendorId(),
                l.supplierName(),
                l.referenceNumber(),
                l.remarks(),
                l.active(),
                l.createdAt(),
                l.updatedAt(),
                l.createdBy(),
                l.updatedBy()
        );
    }

    private LubricantLog toDomain(LubricantLogEntity e) {
        return new LubricantLog(
                e.getId(),
                e.getVehicleId(),
                FluidType.fromString(e.getFluidType()),
                e.getQuantity(),
                MeasurementUnit.fromString(e.getUnit()),
                e.getRecordedAt(),
                e.getOdometerKm(),
                e.getEngineHours(),
                e.getVendorId(),
                e.getSupplierName(),
                e.getReferenceNumber(),
                e.getRemarks(),
                e.isActive(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getCreatedBy(),
                e.getUpdatedBy()
        );
    }
}
