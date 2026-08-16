package com.transportlogistics.app.fleet.application.ports.in;

import com.transportlogistics.app.fleet.domain.model.VehicleReading;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingSourceType;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleReadingUseCase {
    VehicleReading record(RecordCommand command);

    VehicleReading get(UUID readingId);

    PageResult<VehicleReading> list(SearchQuery query);

    LatestReadings latest(UUID vehicleId);

    record RecordCommand(UUID vehicleId, VehicleReadingType readingType, BigDecimal value,
                         VehicleReadingSourceType sourceType, UUID sourceReferenceId, OffsetDateTime recordedAt,
                         UUID actorId, String idempotencyKey, String notes) {
    }

    record SearchQuery(UUID vehicleId, VehicleReadingType readingType, VehicleReadingSourceType sourceType,
                       OffsetDateTime from, OffsetDateTime to, int page, int limit) {
    }

    record PageResult<T>(List<T> content, int page, int limit, long totalElements, int totalPages) {
    }

    record LatestReadings(UUID vehicleId, Optional<VehicleReading> odometer,
                          Optional<VehicleReading> engineHours) {
    }
}
