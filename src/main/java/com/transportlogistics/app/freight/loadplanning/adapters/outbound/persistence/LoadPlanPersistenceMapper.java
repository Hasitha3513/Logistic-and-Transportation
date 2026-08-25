package com.transportlogistics.app.freight.loadplanning.adapters.outbound.persistence;

import com.transportlogistics.app.freight.loadplanning.domain.LoadPlan;
import com.transportlogistics.app.freight.loadplanning.domain.LoadPlanItemPlacement;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class LoadPlanPersistenceMapper {

    public LoadPlanEntity toEntity(LoadPlan domain) {
        LoadPlanEntity entity = new LoadPlanEntity();
        entity.setId(domain.getLoadPlanId());
        entity.setLoadPlanNumber(domain.getLoadPlanNumber());
        entity.setCargoManifestId(domain.getCargoManifestId());
        entity.setVehicleId(domain.getVehicleId());
        entity.setNotes(domain.getNotes());
        entity.setVersion(domain.getVersion());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setCreatedBy(domain.getCreatedBy());
        entity.setUpdatedBy(domain.getUpdatedBy());

        List<LoadPlanItemPlacementEntity> placementEntities = new ArrayList<>();
        if (domain.getPlacements() != null) {
            for (LoadPlanItemPlacement placement : domain.getPlacements()) {
                LoadPlanItemPlacementEntity pEntity = new LoadPlanItemPlacementEntity();
                pEntity.setId(placement.placementId());
                pEntity.setManifestItemId(placement.manifestItemId());
                pEntity.setPlacementOrder(placement.placementOrder());
                pEntity.setZoneReference(placement.zoneReference());
                pEntity.setStackGroup(placement.stackGroup());
                pEntity.setContainerReference(placement.containerReference());
                pEntity.setLoadingSequence(placement.loadingSequence());
                pEntity.setSpecialHandlingNotes(placement.specialHandlingNotes());
                placementEntities.add(pEntity);
            }
        }
        entity.replacePlacements(placementEntities);
        return entity;
    }

    public LoadPlan toDomain(LoadPlanEntity entity) {
        List<LoadPlanItemPlacement> placements = new ArrayList<>();
        if (entity.getPlacements() != null) {
            for (LoadPlanItemPlacementEntity pEntity : entity.getPlacements()) {
                placements.add(new LoadPlanItemPlacement(
                        pEntity.getId(),
                        pEntity.getManifestItemId(),
                        pEntity.getPlacementOrder(),
                        pEntity.getZoneReference(),
                        pEntity.getStackGroup(),
                        pEntity.getContainerReference(),
                        pEntity.getLoadingSequence(),
                        pEntity.getSpecialHandlingNotes()
                ));
            }
        }

        return new LoadPlan(
                entity.getId(),
                entity.getLoadPlanNumber(),
                entity.getCargoManifestId(),
                entity.getVehicleId(),
                placements,
                entity.getNotes(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedBy(),
                entity.getVersion()
        );
    }
}
