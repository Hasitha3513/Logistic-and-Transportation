package com.transportlogistics.app.fuel.application.ports.out;

import com.transportlogistics.app.fuel.domain.model.BunkerTank;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BunkerTankRepository {

    BunkerTank save(BunkerTank tank);

    Optional<BunkerTank> findById(UUID id);

    Optional<BunkerTank> findByIdForUpdate(UUID id);

    Optional<BunkerTank> findByTankCode(String tankCode);

    Optional<BunkerTank> findActiveByStationAndFuelType(UUID fuelStationId, String fuelType);

    Optional<BunkerTank> findActiveByStationAndFuelTypeForUpdate(UUID fuelStationId, String fuelType);

    List<BunkerTank> list(UUID fuelStationId, String fuelType, Boolean active);
}
