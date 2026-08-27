package com.transportlogistics.app.fuel.application.service;

import com.transportlogistics.app.fuel.application.ports.in.FuelStationUseCase;
import com.transportlogistics.app.fuel.application.ports.out.FuelStationRepository;
import com.transportlogistics.app.fuel.domain.model.FuelStation;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class FuelStationService implements FuelStationUseCase {
    private final FuelStationRepository stations;

    public FuelStationService(FuelStationRepository stations) {
        this.stations = stations;
    }

    @Override
    public FuelStation create(Command command) {
        validate(command);
        var code = command.code().trim().toUpperCase(Locale.ROOT);
        if (stations.existsByCode(code, null)) throw new ConflictException("FUEL_STATION_CODE_EXISTS", "Fuel station code already exists");
        return stations.save(new FuelStation(UUID.randomUUID(), code, command.name().trim(), command.stationType(),
                command.active() == null || command.active(), command.vendorId(), command.locationId()));
    }

    @Override
    public FuelStation update(UUID id, Command command) {
        get(id);
        validate(command);
        var code = command.code().trim().toUpperCase(Locale.ROOT);
        if (stations.existsByCode(code, id)) throw new ConflictException("FUEL_STATION_CODE_EXISTS", "Fuel station code already exists");
        return stations.save(new FuelStation(id, code, command.name().trim(), command.stationType(),
                command.active() == null || command.active(), command.vendorId(), command.locationId()));
    }

    @Override
    public FuelStation get(UUID id) {
        return stations.findById(id).orElseThrow(() -> new NotFoundException("FUEL_STATION_NOT_FOUND", "Fuel station not found: " + id));
    }

    @Override
    public List<FuelStation> list(Boolean active) {
        return stations.findAll(active);
    }

    private void validate(Command command) {
        if (command.code() == null || command.code().isBlank()) throw new BusinessRuleException("FUEL_STATION_CODE_REQUIRED", "Fuel station code is required");
        if (command.name() == null || command.name().isBlank()) throw new BusinessRuleException("FUEL_STATION_NAME_REQUIRED", "Fuel station name is required");
        if (command.stationType() == null) throw new BusinessRuleException("FUEL_STATION_TYPE_REQUIRED", "Fuel station type is required");
    }
}
