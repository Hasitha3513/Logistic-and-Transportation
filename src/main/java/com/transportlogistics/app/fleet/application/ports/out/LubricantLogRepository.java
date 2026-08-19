package com.transportlogistics.app.fleet.application.ports.out;

import com.transportlogistics.app.fleet.domain.model.FluidType;
import com.transportlogistics.app.fleet.domain.model.LubricantLog;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LubricantLogRepository {

    LubricantLog save(LubricantLog log);

    Optional<LubricantLog> findById(UUID id);

    List<LubricantLog> findByVehicleId(UUID vehicleId);

    List<LubricantLog> findByVehicleIdWithFilters(
            UUID vehicleId,
            FluidType fluidType,
            OffsetDateTime from,
            OffsetDateTime to
    );
}
