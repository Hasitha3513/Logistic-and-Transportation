package com.transportlogistics.app.fuel.application.ports.in;

import com.transportlogistics.app.fuel.domain.model.FuelStation;
import com.transportlogistics.app.fuel.domain.model.FuelStationType;

import java.util.List;
import java.util.UUID;

public interface FuelStationUseCase {
    FuelStation create(Command command);

    FuelStation update(UUID id, Command command);

    FuelStation get(UUID id);

    List<FuelStation> list(Boolean active);

    record Command(String code, String name, FuelStationType stationType, Boolean active, UUID vendorId,
                   UUID locationId) {
    }
}
