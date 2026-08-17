package com.transportlogistics.app.fleet.application.ports.out;

import com.transportlogistics.app.fleet.application.ports.in.VehicleReadingUseCase;
import com.transportlogistics.app.fleet.domain.model.VehicleReading;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingSourceType;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleReadingRepository {
    VehicleReading save(VehicleReading reading);

    Optional<VehicleReading> findById(UUID readingId);

    Optional<VehicleReading> findPreviousEffective(UUID vehicleId, VehicleReadingType type, int meterEpoch,
                                                   OffsetDateTime recordedAt);

    Optional<VehicleReading> findNextEffective(UUID vehicleId, VehicleReadingType type, int meterEpoch,
                                               OffsetDateTime recordedAt);

    List<VehicleReading> findEffectiveAt(UUID vehicleId, VehicleReadingType type, int meterEpoch,
                                         OffsetDateTime recordedAt);

    Optional<VehicleReading> findLatestEffective(UUID vehicleId, VehicleReadingType type, int meterEpoch);

    int findCurrentMeterEpoch(UUID vehicleId, VehicleReadingType type);

    Optional<VehicleReading> findOriginalBySource(UUID vehicleId, VehicleReadingType type,
                                                  VehicleReadingSourceType sourceType, UUID sourceReferenceId);

    Optional<VehicleReading> findByIdempotencyKey(String idempotencyKey);

    Optional<VehicleReading> findCorrection(UUID originalReadingId);

    VehicleReadingUseCase.PageResult<VehicleReading> search(VehicleReadingUseCase.SearchQuery query);
}