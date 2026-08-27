package com.transportlogistics.app.fuel.application.ports.out;

import com.transportlogistics.app.fuel.domain.model.FuelStation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FuelStationRepository {
    FuelStation save(FuelStation station);

    Optional<FuelStation> findById(UUID id);

    List<FuelStation> findAll(Boolean active);

    boolean existsByCode(String code, UUID excludeId);
}
