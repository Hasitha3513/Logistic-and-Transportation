package com.transportlogistics.app.freight.loadplanning.adapters.inbound.web.mappers;

import com.transportlogistics.app.freight.loadplanning.adapters.inbound.web.dto.request.CreateLoadPlanRequest;
import com.transportlogistics.app.freight.loadplanning.adapters.inbound.web.dto.request.LoadPlanItemPlacementRequest;
import com.transportlogistics.app.freight.loadplanning.adapters.inbound.web.dto.request.UpdateLoadPlanRequest;
import com.transportlogistics.app.freight.loadplanning.adapters.inbound.web.dto.response.LoadPlanItemPlacementResponse;
import com.transportlogistics.app.freight.loadplanning.adapters.inbound.web.dto.response.LoadPlanResponse;
import com.transportlogistics.app.freight.loadplanning.adapters.inbound.web.dto.response.LoadPlanValidationResponse;
import com.transportlogistics.app.freight.loadplanning.adapters.inbound.web.dto.response.LoadValidationResultResponse;
import com.transportlogistics.app.freight.loadplanning.domain.LoadPlan;
import com.transportlogistics.app.freight.loadplanning.domain.LoadPlanItemPlacement;
import com.transportlogistics.app.freight.loadplanning.domain.LoadPlanViolation;
import com.transportlogistics.app.freight.loadplanning.domain.LoadValidationResult;
import com.transportlogistics.app.freight.loadplanning.ports.inbound.LoadPlanUseCase;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LoadPlanWebMapper {

    public LoadPlanUseCase.CreateCommand toCreateCommand(CreateLoadPlanRequest request) {
        List<LoadPlanUseCase.ItemPlacementCommand> placements = request.placements() == null ? List.of() :
                request.placements().stream()
                        .map(this::toPlacementCommand)
                        .toList();

        return new LoadPlanUseCase.CreateCommand(
                request.cargoManifestId(),
                request.vehicleId(),
                placements,
                request.notes()
        );
    }

    public LoadPlanUseCase.UpdateCommand toUpdateCommand(UpdateLoadPlanRequest request) {
        List<LoadPlanUseCase.ItemPlacementCommand> placements = request.placements() == null ? List.of() :
                request.placements().stream()
                        .map(this::toPlacementCommand)
                        .toList();

        return new LoadPlanUseCase.UpdateCommand(
                request.vehicleId(),
                placements,
                request.notes(),
                request.version()
        );
    }

    public LoadPlanUseCase.ItemPlacementCommand toPlacementCommand(LoadPlanItemPlacementRequest r) {
        return new LoadPlanUseCase.ItemPlacementCommand(
                r.manifestItemId(),
                r.placementOrder() != null ? r.placementOrder() : 0,
                r.zoneReference(),
                r.stackGroup(),
                r.containerReference(),
                r.loadingSequence() != null ? r.loadingSequence() : 0,
                r.specialHandlingNotes()
        );
    }

    public LoadPlanResponse toResponse(LoadPlan loadPlan) {
        List<LoadPlanItemPlacementResponse> placements = loadPlan.getPlacements() == null ? List.of() :
                loadPlan.getPlacements().stream()
                        .map(this::toPlacementResponse)
                        .toList();

        return new LoadPlanResponse(
                loadPlan.getLoadPlanId(),
                loadPlan.getLoadPlanNumber(),
                loadPlan.getCargoManifestId(),
                loadPlan.getVehicleId(),
                placements,
                loadPlan.getNotes(),
                loadPlan.getVersion(),
                loadPlan.getCreatedAt(),
                loadPlan.getUpdatedAt(),
                loadPlan.getCreatedBy(),
                loadPlan.getUpdatedBy()
        );
    }

    public LoadPlanItemPlacementResponse toPlacementResponse(LoadPlanItemPlacement placement) {
        return new LoadPlanItemPlacementResponse(
                placement.placementId(),
                placement.manifestItemId(),
                placement.placementOrder(),
                placement.zoneReference(),
                placement.stackGroup(),
                placement.containerReference(),
                placement.loadingSequence(),
                placement.specialHandlingNotes()
        );
    }

    public LoadPlanValidationResponse toValidationResponse(List<LoadPlanViolation> violations) {
        boolean valid = violations == null || violations.isEmpty();
        List<LoadPlanValidationResponse.ViolationDetail> details = violations == null ? List.of() :
                violations.stream()
                        .map(v -> new LoadPlanValidationResponse.ViolationDetail(v.code().name(), v.message()))
                        .toList();

        return new LoadPlanValidationResponse(valid, details);
    }

    public LoadValidationResultResponse toValidationResultResponse(LoadValidationResult result) {
        List<LoadValidationResultResponse.ViolationDetail> details = result.violations() == null ? List.of() :
                result.violations().stream()
                        .map(v -> new LoadValidationResultResponse.ViolationDetail(v.code(), v.message()))
                        .toList();

        return new LoadValidationResultResponse(
                result.loadPlanId(),
                result.validatedAt(),
                result.validatedBy(),
                result.overallOutcome().name(),
                result.grossWeightKg(),
                result.netWeightKg(),
                result.cubicVolumeM3(),
                result.payloadResult() != null ? result.payloadResult().name() : null,
                result.volumeResult() != null ? result.volumeResult().name() : null,
                result.axleResult() != null ? result.axleResult().name() : null,
                details,
                result.missingData()
        );
    }
}
