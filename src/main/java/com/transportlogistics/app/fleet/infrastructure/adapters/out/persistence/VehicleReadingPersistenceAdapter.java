package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.application.ports.in.VehicleReadingUseCase;
import com.transportlogistics.app.fleet.application.ports.out.VehicleReadingRepository;
import com.transportlogistics.app.fleet.domain.model.VehicleReading;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingSourceType;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class VehicleReadingPersistenceAdapter implements VehicleReadingRepository {
    private static final PageRequest ONE = PageRequest.of(0, 1);

    private final VehicleReadingJpaRepository repository;

    VehicleReadingPersistenceAdapter(VehicleReadingJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public VehicleReading save(VehicleReading reading) {
        return map(repository.saveAndFlush(entity(reading)));
    }

    @Override
    public Optional<VehicleReading> findById(UUID readingId) {
        return repository.findById(readingId).map(this::map);
    }

    @Override
    public Optional<VehicleReading> findPreviousEffective(UUID vehicleId, VehicleReadingType type, int meterEpoch,
                                                          OffsetDateTime recordedAt) {
        return repository.findPreviousEffective(vehicleId, type, meterEpoch, recordedAt, ONE).stream()
                .findFirst().map(this::map);
    }

    @Override
    public Optional<VehicleReading> findNextEffective(UUID vehicleId, VehicleReadingType type, int meterEpoch,
                                                      OffsetDateTime recordedAt) {
        return repository.findNextEffective(vehicleId, type, meterEpoch, recordedAt, ONE).stream()
                .findFirst().map(this::map);
    }

    @Override
    public List<VehicleReading> findEffectiveAt(UUID vehicleId, VehicleReadingType type, int meterEpoch,
                                                OffsetDateTime recordedAt) {
        return repository.findEffectiveAt(vehicleId, type, meterEpoch, recordedAt).stream().map(this::map).toList();
    }

    @Override
    public Optional<VehicleReading> findLatestEffective(UUID vehicleId, VehicleReadingType type, int meterEpoch) {
        return repository.findLatestEffective(vehicleId, type, meterEpoch, ONE).stream().findFirst().map(this::map);
    }

    @Override
    public int findCurrentMeterEpoch(UUID vehicleId, VehicleReadingType type) {
        return repository.findCurrentMeterEpoch(vehicleId, type);
    }

    @Override
    public Optional<VehicleReading> findOriginalBySource(UUID vehicleId, VehicleReadingType type,
                                                         VehicleReadingSourceType sourceType,
                                                         UUID sourceReferenceId) {
        return repository
                .findByVehicleIdAndReadingTypeAndSourceTypeAndSourceReferenceIdAndCorrectionOfReadingIdIsNull(
                        vehicleId, type, sourceType, sourceReferenceId)
                .map(this::map);
    }

    @Override
    public Optional<VehicleReading> findByIdempotencyKey(String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey).map(this::map);
    }

    @Override
    public Optional<VehicleReading> findCorrection(UUID originalReadingId) {
        return repository.findByCorrectionOfReadingId(originalReadingId).map(this::map);
    }

    @Override
    public VehicleReadingUseCase.PageResult<VehicleReading> search(VehicleReadingUseCase.SearchQuery query) {
        var pageable = PageRequest.of(query.page(), query.limit(), Sort.by(
                Sort.Order.desc("recordedAt"), Sort.Order.desc("receivedAt"), Sort.Order.desc("createdAt")));
        Specification<VehicleReadingEntity> specification = (root, ignored, builder) ->
                query.vehicleId() != null ? builder.equal(root.get("vehicleId"), query.vehicleId()) : builder.conjunction();
        if (query.readingType() != null) {
            specification = specification.and((root, ignored, builder) ->
                    builder.equal(root.get("readingType"), query.readingType()));
        }
        if (query.sourceType() != null) {
            specification = specification.and((root, ignored, builder) ->
                    builder.equal(root.get("sourceType"), query.sourceType()));
        }
        if (query.from() != null) {
            specification = specification.and((root, ignored, builder) ->
                    builder.greaterThanOrEqualTo(root.get("recordedAt"), query.from()));
        }
        if (query.to() != null) {
            specification = specification.and((root, ignored, builder) ->
                    builder.lessThanOrEqualTo(root.get("recordedAt"), query.to()));
        }
        var page = repository.findAll(specification, pageable);
        return new VehicleReadingUseCase.PageResult<>(page.stream().map(this::map).toList(), page.getNumber(),
                page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private VehicleReadingEntity entity(VehicleReading reading) {
        var entity = new VehicleReadingEntity();
        entity.setId(reading.id());
        entity.setVehicleId(reading.vehicleId());
        entity.setReadingType(reading.readingType());
        entity.setValue(reading.value());
        entity.setUnit(reading.unit());
        entity.setMeterEpoch(reading.meterEpoch());
        entity.setSourceType(reading.sourceType());
        entity.setSourceReferenceId(reading.sourceReferenceId());
        entity.setRecordedAt(reading.recordedAt());
        entity.setReceivedAt(reading.receivedAt());
        entity.setCreatedBy(reading.createdBy());
        entity.setCorrectionOfReadingId(reading.correctionOfReadingId());
        entity.setCorrectionReason(reading.correctionReason());
        entity.setIdempotencyKey(reading.idempotencyKey());
        entity.setNotes(reading.notes());
        entity.setCreatedAt(reading.createdAt());
        return entity;
    }

    private VehicleReading map(VehicleReadingEntity entity) {
        return new VehicleReading(entity.getId(), entity.getVehicleId(), entity.getReadingType(), entity.getValue(),
                entity.getUnit(), entity.getMeterEpoch(), entity.getSourceType(), entity.getSourceReferenceId(),
                entity.getRecordedAt(), entity.getReceivedAt(), entity.getCreatedBy(),
                entity.getCorrectionOfReadingId(), entity.getCorrectionReason(), entity.getIdempotencyKey(),
                entity.getNotes(), entity.getCreatedAt());
    }
}