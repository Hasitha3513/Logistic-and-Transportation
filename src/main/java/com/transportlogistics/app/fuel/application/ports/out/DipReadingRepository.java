package com.transportlogistics.app.fuel.application.ports.out;

import com.transportlogistics.app.fuel.domain.model.DipReading;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DipReadingRepository {

    DipReading save(DipReading reading);

    Optional<DipReading> findById(UUID id);

    List<DipReading> findByTankId(UUID tankId);
}
