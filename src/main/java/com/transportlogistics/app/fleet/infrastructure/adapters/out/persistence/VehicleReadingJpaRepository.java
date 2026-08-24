package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.domain.model.VehicleReadingSourceType;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface VehicleReadingJpaRepository extends JpaRepository<VehicleReadingEntity, UUID>,
        JpaSpecificationExecutor<VehicleReadingEntity> {
    @Query("""
            select reading from VehicleReadingEntity reading
            where reading.vehicleId = :vehicleId
              and reading.readingType = :readingType
              and reading.meterEpoch = :meterEpoch
              and reading.recordedAt < :recordedAt
              and not exists (select correction.id from VehicleReadingEntity correction
                              where correction.correctionOfReadingId = reading.id)
            order by reading.recordedAt desc, reading.receivedAt desc, reading.createdAt desc
            """)
    List<VehicleReadingEntity> findPreviousEffective(@Param("vehicleId") UUID vehicleId,
                                                      @Param("readingType") VehicleReadingType readingType,
                                                      @Param("meterEpoch") int meterEpoch,
                                                      @Param("recordedAt") OffsetDateTime recordedAt,
                                                      Pageable pageable);

    @Query("""
            select reading from VehicleReadingEntity reading
            where reading.vehicleId = :vehicleId
              and reading.readingType = :readingType
              and reading.meterEpoch = :meterEpoch
              and reading.recordedAt > :recordedAt
              and not exists (select correction.id from VehicleReadingEntity correction
                              where correction.correctionOfReadingId = reading.id)
            order by reading.recordedAt asc, reading.receivedAt asc, reading.createdAt asc
            """)
    List<VehicleReadingEntity> findNextEffective(@Param("vehicleId") UUID vehicleId,
                                                  @Param("readingType") VehicleReadingType readingType,
                                                  @Param("meterEpoch") int meterEpoch,
                                                  @Param("recordedAt") OffsetDateTime recordedAt,
                                                  Pageable pageable);

    @Query("""
            select reading from VehicleReadingEntity reading
            where reading.vehicleId = :vehicleId
              and reading.readingType = :readingType
              and reading.meterEpoch = :meterEpoch
              and reading.recordedAt = :recordedAt
              and not exists (select correction.id from VehicleReadingEntity correction
                              where correction.correctionOfReadingId = reading.id)
            order by reading.receivedAt asc, reading.createdAt asc
            """)
    List<VehicleReadingEntity> findEffectiveAt(@Param("vehicleId") UUID vehicleId,
                                                @Param("readingType") VehicleReadingType readingType,
                                                @Param("meterEpoch") int meterEpoch,
                                                @Param("recordedAt") OffsetDateTime recordedAt);

    @Query("""
            select reading from VehicleReadingEntity reading
            where reading.vehicleId = :vehicleId
              and reading.readingType = :readingType
              and reading.meterEpoch = :meterEpoch
              and not exists (select correction.id from VehicleReadingEntity correction
                              where correction.correctionOfReadingId = reading.id)
            order by reading.recordedAt desc, reading.receivedAt desc, reading.createdAt desc
            """)
    List<VehicleReadingEntity> findLatestEffective(@Param("vehicleId") UUID vehicleId,
                                                    @Param("readingType") VehicleReadingType readingType,
                                                    @Param("meterEpoch") int meterEpoch,
                                                    Pageable pageable);

    @Query("select coalesce(max(reading.meterEpoch), 0) from VehicleReadingEntity reading where reading.vehicleId = :vehicleId and reading.readingType = :readingType")
    int findCurrentMeterEpoch(@Param("vehicleId") UUID vehicleId,
                              @Param("readingType") VehicleReadingType readingType);

    Optional<VehicleReadingEntity> findByVehicleIdAndReadingTypeAndSourceTypeAndSourceReferenceIdAndCorrectionOfReadingIdIsNull(
            UUID vehicleId, VehicleReadingType readingType, VehicleReadingSourceType sourceType,
            UUID sourceReferenceId);

    Optional<VehicleReadingEntity> findByIdempotencyKey(String idempotencyKey);

    Optional<VehicleReadingEntity> findByCorrectionOfReadingId(UUID originalReadingId);
}