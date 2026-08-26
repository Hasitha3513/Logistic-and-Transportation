package com.transportlogistics.app.fleet.application.ports.in;

import com.transportlogistics.app.fleet.domain.model.FluidType;
import com.transportlogistics.app.fleet.domain.model.LubricantLog;
import com.transportlogistics.app.fleet.domain.model.MeasurementUnit;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface LubricantLogUseCase {

    LubricantLog create(UUID vehicleId, CreateCommand command, String actor);

    List<LubricantLog> list(UUID vehicleId, FluidType fluidType, OffsetDateTime from, OffsetDateTime to);

    LubricantLog get(UUID vehicleId, UUID logId);

    record CreateCommand(
            FluidType fluidType,
            BigDecimal quantity,
            MeasurementUnit unit,
            OffsetDateTime recordedAt,
            Double odometerKm,
            Double engineHours,
            UUID vendorId,
            String supplierName,
            String referenceNumber,
            String remarks
    ) {}
}
