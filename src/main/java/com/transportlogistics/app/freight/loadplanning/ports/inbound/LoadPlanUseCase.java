package com.transportlogistics.app.freight.loadplanning.ports.inbound;

import com.transportlogistics.app.freight.loadplanning.domain.LoadPlan;
import com.transportlogistics.app.freight.loadplanning.domain.LoadPlanViolation;
import com.transportlogistics.app.freight.loadplanning.domain.LoadValidationResult;

import java.util.List;
import java.util.UUID;

/**
 * Inbound port / use case for managing Load Plans and performing validations.
 */
public interface LoadPlanUseCase {

    record ItemPlacementCommand(
            UUID manifestItemId,
            int placementOrder,
            String zoneReference,
            String stackGroup,
            String containerReference,
            int loadingSequence,
            String specialHandlingNotes
    ) {}

    record CreateCommand(
            UUID cargoManifestId,
            UUID vehicleId,
            List<ItemPlacementCommand> placements,
            String notes
    ) {}

    record UpdateCommand(
            UUID vehicleId,
            List<ItemPlacementCommand> placements,
            String notes,
            long version
    ) {}

    LoadPlan create(CreateCommand command, String actor);

    LoadPlan get(UUID id);

    List<LoadPlan> list();

    LoadPlan update(UUID id, UpdateCommand command, String actor);

    List<LoadPlanViolation> validateLayout(UUID id);

    LoadValidationResult validateWeightAndVolume(UUID id, String actor);
}
