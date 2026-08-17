package com.transportlogistics.app.fleet.infrastructure.adapters.in.web;

import com.transportlogistics.app.fleet.application.ports.in.VehicleReadingUseCase;
import com.transportlogistics.app.fleet.application.ports.out.VehicleReadingRepository;
import com.transportlogistics.app.fleet.domain.model.*;
import com.transportlogistics.app.identity.AuthenticatedUserLookup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class VehicleReadingController {
    private final VehicleReadingUseCase readingUseCase;
    private final VehicleReadingRepository readingRepository;
    private final AuthenticatedUserLookup userLookup;

    @GetMapping("/vehicles/{vehicleId}/readings")
    public VehicleReadingUseCase.PageResult<VehicleReadingResponse> listReadings(
            @PathVariable UUID vehicleId,
            @RequestParam(required = false) VehicleReadingType readingType,
            @RequestParam(required = false) VehicleReadingSourceType sourceType,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit) {
        var pageResult = readingUseCase.list(new VehicleReadingUseCase.SearchQuery(
                vehicleId, readingType, sourceType, from, to, page, limit));
        var content = pageResult.content().stream().map(this::toResponse).toList();
        return new VehicleReadingUseCase.PageResult<>(content, pageResult.page(), pageResult.limit(),
                pageResult.totalElements(), pageResult.totalPages());
    }

    @GetMapping("/vehicles/{vehicleId}/readings/latest")
    public LatestReadingsResponse latestReadings(@PathVariable UUID vehicleId) {
        var latest = readingUseCase.latest(vehicleId);
        return new LatestReadingsResponse(
                latest.vehicleId(),
                latest.odometer().map(this::toResponse).orElse(null),
                latest.engineHours().map(this::toResponse).orElse(null)
        );
    }

    @GetMapping("/vehicles/{vehicleId}/mileage-summary")
    public VehicleMileageSummaryResponse getMileageSummary(
            @PathVariable UUID vehicleId,
            @RequestParam @NotNull @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @NotNull @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "true") boolean includeSourceBreakdown) {
        var summary = readingUseCase.mileageSummary(vehicleId, from, to, includeSourceBreakdown);
        return toMileageSummaryResponse(summary);
    }

    @PostMapping("/vehicles/{vehicleId}/readings")
    public ResponseEntity<VehicleReadingResponse> recordManualReading(
            @PathVariable UUID vehicleId,
            @Valid @RequestBody ManualReadingRequest request,
            Principal principal) {
        var actorId = resolveActorId(principal);
        var reading = readingUseCase.record(new VehicleReadingUseCase.RecordCommand(
                vehicleId, request.readingType(), request.value(), VehicleReadingSourceType.MANUAL,
                null, request.recordedAt(), actorId, request.idempotencyKey(), request.notes()
        ));
        return ResponseEntity.status(201).body(toResponse(reading));
    }

    @PostMapping("/vehicles/{vehicleId}/readings/{readingId}/correct")
    public ResponseEntity<VehicleReadingResponse> correctReading(
            @PathVariable UUID vehicleId,
            @PathVariable UUID readingId,
            @Valid @RequestBody CorrectionRequest request,
            Principal principal) {
        var actorId = resolveActorId(principal);
        var reading = readingUseCase.correct(new VehicleReadingUseCase.CorrectCommand(
                vehicleId, readingId, request.value(), request.reason(), actorId,
                request.idempotencyKey(), request.notes()
        ));
        return ResponseEntity.status(201).body(toResponse(reading));
    }

    @PostMapping("/vehicles/{vehicleId}/meter-resets")
    public ResponseEntity<MeterResetResponse> resetMeter(
            @PathVariable UUID vehicleId,
            @Valid @RequestBody MeterResetRequest request,
            Principal principal) {
        var actorId = resolveActorId(principal);
        var reset = readingUseCase.resetMeter(new VehicleReadingUseCase.ResetMeterCommand(
                vehicleId, request.readingType(), request.newMeterValue(), request.effectiveAt(),
                request.reason(), actorId, actorId, request.notes()
        ));
        return ResponseEntity.status(201).body(toResetResponse(reset));
    }

    @GetMapping("/vehicles/{vehicleId}/meter-resets")
    public List<MeterResetResponse> listResets(@PathVariable UUID vehicleId) {
        return readingUseCase.listResets(vehicleId).stream().map(this::toResetResponse).toList();
    }

    private UUID resolveActorId(Principal principal) {
        if (principal == null || principal.getName() == null) {
            return UUID.randomUUID();
        }
        if (userLookup == null) {
            return UUID.nameUUIDFromBytes(principal.getName().getBytes());
        }
        return userLookup.findByUsername(principal.getName())
                .map(AuthenticatedUserLookup.AuthenticatedUser::id)
                .orElseGet(() -> UUID.nameUUIDFromBytes(principal.getName().getBytes()));
    }

    private VehicleReadingResponse toResponse(VehicleReading reading) {
        var isSuperseded = readingRepository.isSuperseded(reading.id());
        String status;
        if (isSuperseded) {
            status = "CORRECTED";
        } else if (reading.correctionOfReadingId() != null) {
            status = "CORRECTION";
        } else {
            status = "ACTIVE";
        }

        return new VehicleReadingResponse(
                reading.id(),
                reading.vehicleId(),
                reading.readingType(),
                reading.value(),
                reading.unit(),
                reading.meterEpoch(),
                reading.sourceType(),
                reading.sourceReferenceId(),
                reading.recordedAt(),
                reading.receivedAt(),
                reading.createdBy(),
                reading.correctionOfReadingId(),
                reading.correctionReason(),
                reading.idempotencyKey(),
                reading.notes(),
                reading.createdAt(),
                status
        );
    }

    private MeterResetResponse toResetResponse(VehicleMeterReset reset) {
        return new MeterResetResponse(
                reset.id(),
                reset.vehicleId(),
                reset.readingType(),
                reset.previousReadingId(),
                reset.previousMeterValue(),
                reset.newReadingId(),
                reset.newMeterValue(),
                reset.effectiveAt(),
                reset.reason(),
                reset.createdBy(),
                reset.approvedBy(),
                reset.notes(),
                reset.createdAt()
        );
    }

    private VehicleMileageSummaryResponse toMileageSummaryResponse(com.transportlogistics.app.fleet.VehicleMileageSummary summary) {
        var sources = new java.util.HashMap<String, Integer>();
        summary.sourceCounts().forEach((k, v) -> sources.put(k.name(), v));
        return new VehicleMileageSummaryResponse(
                summary.vehicleId(), summary.from(), summary.to(),
                summary.openingOdometer(), summary.closingOdometer(), summary.distanceKm(),
                summary.openingEngineHours(), summary.closingEngineHours(), summary.engineHoursUsed(),
                summary.readingCount(), summary.correctionCount(), summary.meterResetCount(),
                summary.firstReadingAt(), summary.lastReadingAt(),
                summary.coverageStatus().name(), summary.coverageReason(), sources
        );
    }

    public record ManualReadingRequest(
            @NotNull VehicleReadingType readingType,
            @NotNull @DecimalMin("0.0") BigDecimal value,
            @NotNull OffsetDateTime recordedAt,
            String idempotencyKey,
            String notes
    ) {
    }

    public record CorrectionRequest(
            @NotNull @DecimalMin("0.0") BigDecimal value,
            @NotBlank String reason,
            String idempotencyKey,
            String notes
    ) {
    }

    public record MeterResetRequest(
            @NotNull VehicleReadingType readingType,
            @NotNull @DecimalMin("0.0") BigDecimal newMeterValue,
            @NotNull OffsetDateTime effectiveAt,
            @NotBlank String reason,
            String notes
    ) {
    }

    public record VehicleReadingResponse(
            UUID id,
            UUID vehicleId,
            VehicleReadingType readingType,
            BigDecimal value,
            VehicleReadingUnit unit,
            int meterEpoch,
            VehicleReadingSourceType sourceType,
            UUID sourceReferenceId,
            OffsetDateTime recordedAt,
            OffsetDateTime receivedAt,
            UUID createdBy,
            UUID correctionOfReadingId,
            String correctionReason,
            String idempotencyKey,
            String notes,
            OffsetDateTime createdAt,
            String status
    ) {
    }

    public record LatestReadingsResponse(
            UUID vehicleId,
            VehicleReadingResponse odometer,
            VehicleReadingResponse engineHours
    ) {
    }

    public record MeterResetResponse(
            UUID id,
            UUID vehicleId,
            VehicleReadingType readingType,
            UUID previousReadingId,
            BigDecimal previousMeterValue,
            UUID newReadingId,
            BigDecimal newMeterValue,
            OffsetDateTime effectiveAt,
            String reason,
            UUID createdBy,
            UUID approvedBy,
            String notes,
            OffsetDateTime createdAt
    ) {
    }

    public record VehicleMileageSummaryResponse(
            UUID vehicleId,
            OffsetDateTime from,
            OffsetDateTime to,
            BigDecimal openingOdometer,
            BigDecimal closingOdometer,
            BigDecimal distanceKm,
            BigDecimal openingEngineHours,
            BigDecimal closingEngineHours,
            BigDecimal engineHoursUsed,
            int readingCount,
            int correctionCount,
            int meterResetCount,
            OffsetDateTime firstReadingAt,
            OffsetDateTime lastReadingAt,
            String coverageStatus,
            String coverageReason,
            java.util.Map<String, Integer> sourceCounts
    ) {
    }
}
