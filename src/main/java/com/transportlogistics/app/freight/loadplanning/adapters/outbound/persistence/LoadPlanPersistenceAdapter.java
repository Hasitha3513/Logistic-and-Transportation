package com.transportlogistics.app.freight.loadplanning.adapters.outbound.persistence;

import com.transportlogistics.app.freight.loadplanning.domain.LoadPlan;
import com.transportlogistics.app.freight.loadplanning.domain.LoadPlanItemPlacement;
import com.transportlogistics.app.freight.loadplanning.ports.outbound.LoadPlanRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class LoadPlanPersistenceAdapter implements LoadPlanRepository {

    private final LoadPlanJpaRepository repository;
    private final LoadPlanPersistenceMapper mapper;

    public LoadPlanPersistenceAdapter(LoadPlanJpaRepository repository,
                                      LoadPlanPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public LoadPlan save(LoadPlan loadPlan) {
        Optional<LoadPlanEntity> existingOpt = repository.findById(loadPlan.getLoadPlanId());
        LoadPlanEntity entity;
        if (existingOpt.isPresent()) {
            entity = existingOpt.get();
            entity.setVehicleId(loadPlan.getVehicleId());
            entity.setNotes(loadPlan.getNotes());
            entity.setUpdatedAt(loadPlan.getUpdatedAt());
            entity.setUpdatedBy(loadPlan.getUpdatedBy());
            entity.setVersion(loadPlan.getVersion());

            Map<UUID, LoadPlanItemPlacementEntity> existingMap = entity.getPlacements().stream()
                    .collect(Collectors.toMap(LoadPlanItemPlacementEntity::getManifestItemId, p -> p));

            List<LoadPlanItemPlacementEntity> updatedPlacements = new ArrayList<>();
            if (loadPlan.getPlacements() != null) {
                for (LoadPlanItemPlacement placement : loadPlan.getPlacements()) {
                    LoadPlanItemPlacementEntity pEntity = existingMap.remove(placement.manifestItemId());
                    if (pEntity == null) {
                        pEntity = new LoadPlanItemPlacementEntity();
                        pEntity.setId(placement.placementId());
                        pEntity.setManifestItemId(placement.manifestItemId());
                    }
                    pEntity.setPlacementOrder(placement.placementOrder());
                    pEntity.setZoneReference(placement.zoneReference());
                    pEntity.setStackGroup(placement.stackGroup());
                    pEntity.setContainerReference(placement.containerReference());
                    pEntity.setLoadingSequence(placement.loadingSequence());
                    pEntity.setSpecialHandlingNotes(placement.specialHandlingNotes());
                    pEntity.setLoadPlan(entity);
                    updatedPlacements.add(pEntity);
                }
            }
            entity.getPlacements().clear();
            entity.getPlacements().addAll(updatedPlacements);
        } else {
            entity = mapper.toEntity(loadPlan);
        }
        LoadPlanEntity saved = repository.saveAndFlush(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<LoadPlan> findById(UUID loadPlanId) {
        return repository.findById(loadPlanId).map(mapper::toDomain);
    }

    @Override
    public Optional<LoadPlan> findByCargoManifestId(UUID cargoManifestId) {
        return repository.findByCargoManifestId(cargoManifestId).map(mapper::toDomain);
    }

    @Override
    public List<LoadPlan> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }
}
