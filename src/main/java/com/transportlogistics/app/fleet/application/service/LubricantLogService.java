package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.application.ports.in.LubricantLogUseCase;
import com.transportlogistics.app.fleet.application.ports.out.LubricantLogRepository;
import com.transportlogistics.app.fleet.vehiclemaster.ports.outbound.VehicleRepository;
import com.transportlogistics.app.fleet.domain.model.FluidType;
import com.transportlogistics.app.fleet.domain.model.LubricantLog;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Transactional
public class LubricantLogService implements LubricantLogUseCase {

    private final VehicleRepository vehicles;
    private final LubricantLogRepository lubricantLogs;

    public LubricantLogService(VehicleRepository vehicles, LubricantLogRepository lubricantLogs) {
        this.vehicles = Objects.requireNonNull(vehicles, "VehicleRepository cannot be null");
        this.lubricantLogs = Objects.requireNonNull(lubricantLogs, "LubricantLogRepository cannot be null");
    }

    @Override
    public LubricantLog create(UUID vehicleId, CreateCommand command, String actor) {
        var vehicle = vehicles.findByIdForUpdate(vehicleId)
                .orElseThrow(() -> new NotFoundException("Vehicle not found: " + vehicleId));

        var now = OffsetDateTime.now();
        var log = new LubricantLog(
                UUID.randomUUID(),
                vehicle.id(),
                command.fluidType(),
                command.quantity(),
                command.unit(),
                command.recordedAt() != null ? command.recordedAt() : now,
                command.odometerKm(),
                command.engineHours(),
                command.vendorId(),
                command.supplierName(),
                command.referenceNumber(),
                command.remarks(),
                true,
                now,
                now,
                actor != null ? actor : "system",
                actor != null ? actor : "system"
        );
        return lubricantLogs.save(log);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LubricantLog> list(UUID vehicleId, FluidType fluidType, OffsetDateTime from, OffsetDateTime to) {
        vehicles.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException("Vehicle not found: " + vehicleId));

        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("From date cannot be after to date");
        }

        if (fluidType == null && from == null && to == null) {
            return lubricantLogs.findByVehicleId(vehicleId);
        }
        return lubricantLogs.findByVehicleIdWithFilters(vehicleId, fluidType, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public LubricantLog get(UUID vehicleId, UUID logId) {
        return lubricantLogs.findById(logId)
                .filter(log -> log.vehicleId().equals(vehicleId))
                .orElseThrow(() -> new NotFoundException("Lubricant log not found: " + logId));
    }
}
