package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.controllers;

import com.transportlogistics.app.fleet.application.ports.in.VehicleReadingUseCase;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingSourceType;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingType;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request.RecordManualVehicleReadingRequest;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request.RecordVehicleMeterResetRequest;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request.RecordVehicleReadingCorrectionRequest;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.response.*;
import com.transportlogistics.app.fleet.infrastructure.adapters.in.web.mappers.VehicleReadingWebMapper;
import com.transportlogistics.app.identity.AuthenticatedUserLookup;
import com.transportlogistics.app.shared.domain.NotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
public class VehicleReadingController {

    private final VehicleReadingUseCase readingUseCase;
    private final AuthenticatedUserLookup userLookup;
    private final VehicleReadingWebMapper mapper;

    public VehicleReadingController(VehicleReadingUseCase readingUseCase,
                                    AuthenticatedUserLookup userLookup,
                                    VehicleReadingWebMapper mapper) {
        this.readingUseCase = readingUseCase;
        this.userLookup = userLookup;
        this.mapper = mapper;
    }

    @GetMapping("/vehicles/{vehicleId}/readings")
    public PageResponse<VehicleReadingResponse> listReadings(
            @PathVariable UUID vehicleId,
            @RequestParam(required = false) VehicleReadingType readingType,
            @RequestParam(required = false) VehicleReadingSourceType sourceType,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit) {
        var query = new VehicleReadingUseCase.SearchQuery(vehicleId, readingType, sourceType, from, to, page, limit);
        var result = readingUseCase.list(query);
        return new PageResponse<>(
                mapper.toReadingResponseList(result.content()),
                result.page(),
                result.limit(),
                result.totalElements(),
                result.totalPages()
        );
    }

    @GetMapping("/vehicles/{vehicleId}/readings/latest")
    public LatestVehicleReadingsResponse getLatestReadings(@PathVariable UUID vehicleId) {
        return mapper.toResponse(readingUseCase.latest(vehicleId));
    }

    @PostMapping("/vehicles/{vehicleId}/readings")
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleReadingResponse recordManualReading(
            @PathVariable UUID vehicleId,
            @Valid @RequestBody RecordManualVehicleReadingRequest request,
            Principal principal) {
        var actorId = resolveActorId(principal);
        var idempotencyKey = request.idempotencyKey() != null && !request.idempotencyKey().isBlank()
                ? request.idempotencyKey().trim()
                : UUID.randomUUID().toString();

        var command = new VehicleReadingUseCase.RecordCommand(
                vehicleId,
                request.readingType(),
                request.value(),
                VehicleReadingSourceType.MANUAL,
                null,
                request.recordedAt(),
                actorId,
                idempotencyKey,
                request.notes()
        );
        return mapper.toResponse(readingUseCase.record(command));
    }

    @PostMapping("/vehicles/{vehicleId}/readings/{readingId}/correct")
    public VehicleReadingResponse correctReading(
            @PathVariable UUID vehicleId,
            @PathVariable UUID readingId,
            @Valid @RequestBody RecordVehicleReadingCorrectionRequest request,
            Principal principal) {
        var actorId = resolveActorId(principal);
        var command = new VehicleReadingUseCase.CorrectCommand(
                vehicleId,
                readingId,
                request.value(),
                request.reason(),
                request.recordedAt(),
                actorId
        );
        return mapper.toResponse(readingUseCase.correct(command));
    }

    @PostMapping("/vehicles/{vehicleId}/meter-resets")
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleMeterResetResponse resetMeter(
            @PathVariable UUID vehicleId,
            @Valid @RequestBody RecordVehicleMeterResetRequest request,
            Principal principal) {
        var actorId = resolveActorId(principal);
        var command = new VehicleReadingUseCase.ResetMeterCommand(
                vehicleId,
                request.readingType(),
                request.newMeterValue(),
                request.effectiveAt(),
                request.reason(),
                actorId
        );
        return mapper.toResponse(readingUseCase.resetMeter(command));
    }

    @GetMapping("/vehicles/{vehicleId}/meter-resets")
    public List<VehicleMeterResetResponse> listMeterResets(@PathVariable UUID vehicleId) {
        return readingUseCase.listMeterResets(vehicleId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @GetMapping("/vehicles/{vehicleId}/mileage")
    public VehicleMileageSummaryResponse getMileage(
            @PathVariable UUID vehicleId,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to) {
        return mapper.toResponse(readingUseCase.getMileage(vehicleId, from, to));
    }

    private UUID resolveActorId(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new NotFoundException("USER_NOT_FOUND", "Authenticated actor is required");
        }
        return userLookup.findByUsername(principal.getName())
                .map(AuthenticatedUserLookup.AuthenticatedUser::id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Authenticated user not found: " + principal.getName()));
    }
}
