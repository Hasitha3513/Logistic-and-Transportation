package com.transportlogistics.app.fleet.infrastructure.adapters.in.web;

import com.transportlogistics.app.fleet.CoverageStatus;
import com.transportlogistics.app.fleet.VehicleMileageSummary;
import com.transportlogistics.app.fleet.application.ports.in.VehicleReadingUseCase;
import com.transportlogistics.app.fleet.domain.model.VehicleMeterReset;
import com.transportlogistics.app.fleet.domain.model.VehicleReading;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingSourceType;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingType;
import com.transportlogistics.app.identity.AuthenticatedUserLookup;
import com.transportlogistics.app.shared.domain.NotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
public class VehicleReadingController {

    private final VehicleReadingUseCase readingUseCase;
    private final AuthenticatedUserLookup userLookup;

    public VehicleReadingController(VehicleReadingUseCase readingUseCase, AuthenticatedUserLookup userLookup) {
        this.readingUseCase = readingUseCase;
        this.userLookup = userLookup;
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
                result.content().stream().map(VehicleReadingResponse::from).toList(),
                result.page(),
                result.limit(),
                result.totalElements(),
                result.totalPages()
        );
    }

    @GetMapping("/vehicles/{vehicleId}/readings/latest")
    public LatestVehicleReadingsResponse getLatestReadings(@PathVariable UUID vehicleId) {
        var latest = readingUseCase.latest(vehicleId);
        return new LatestVehicleReadingsResponse(
                latest.vehicleId(),
                latest.odometer().map(LatestVehicleReadingsResponse.ReadingSnapshot::from).orElse(null),
                latest.engineHours().map(LatestVehicleReadingsResponse.ReadingSnapshot::from).orElse(null)
        );
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
        var reading = readingUseCase.record(command);
        return VehicleReadingResponse.from(reading);
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
        var corrected = readingUseCase.correct(command);
        return VehicleReadingResponse.from(corrected);
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
        var reset = readingUseCase.resetMeter(command);
        return VehicleMeterResetResponse.from(reset);
    }

    @GetMapping("/vehicles/{vehicleId}/meter-resets")
    public List<VehicleMeterResetResponse> listMeterResets(@PathVariable UUID vehicleId) {
        return readingUseCase.listMeterResets(vehicleId).stream()
                .map(VehicleMeterResetResponse::from)
                .toList();
    }

    @GetMapping("/vehicles/{vehicleId}/mileage")
    public VehicleMileageSummaryResponse getMileage(
            @PathVariable UUID vehicleId,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to) {
        var summary = readingUseCase.getMileage(vehicleId, from, to);
        return VehicleMileageSummaryResponse.from(summary);
    }

    private UUID resolveActorId(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new NotFoundException("USER_NOT_FOUND", "Authenticated actor is required");
        }
        return userLookup.findByUsername(principal.getName())
                .map(AuthenticatedUserLookup.AuthenticatedUser::id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Authenticated user not found: " + principal.getName()));
    }

    public record RecordManualVehicleReadingRequest(
            @NotNull(message = "Reading type is required")
            VehicleReadingType readingType,

            @NotNull(message = "Reading value is required")
            @DecimalMin(value = "0.000", message = "Reading value cannot be negative")
            BigDecimal value,

            @NotNull(message = "Recorded time is required")
            OffsetDateTime recordedAt,

            @Size(max = 160, message = "Idempotency key cannot exceed 160 characters")
            String idempotencyKey,

            @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
            String notes
    ) {
    }

    public record RecordVehicleReadingCorrectionRequest(
            @NotNull(message = "Corrected value is required")
            @DecimalMin(value = "0.000", message = "Corrected value cannot be negative")
            BigDecimal value,

            @NotBlank(message = "Correction reason is required")
            @Size(max = 1000, message = "Reason cannot exceed 1000 characters")
            String reason,

            OffsetDateTime recordedAt
    ) {
    }

    public record RecordVehicleMeterResetRequest(
            @NotNull(message = "Reading type is required")
            VehicleReadingType readingType,

            @NotNull(message = "New meter value is required")
            @DecimalMin(value = "0.000", message = "New meter value cannot be negative")
            BigDecimal newMeterValue,

            @NotNull(message = "Effective time is required")
            OffsetDateTime effectiveAt,

            @NotBlank(message = "Meter reset reason is required")
            @Size(max = 1000, message = "Reason cannot exceed 1000 characters")
            String reason
    ) {
    }

    public record VehicleReadingResponse(
            UUID id,
            UUID vehicleId,
            VehicleReadingType readingType,
            BigDecimal value,
            String unit,
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
            OffsetDateTime createdAt
    ) {
        public static VehicleReadingResponse from(VehicleReading r) {
            return new VehicleReadingResponse(
                    r.id(),
                    r.vehicleId(),
                    r.readingType(),
                    r.value(),
                    r.unit().name(),
                    r.meterEpoch(),
                    r.sourceType(),
                    r.sourceReferenceId(),
                    r.recordedAt(),
                    r.receivedAt(),
                    r.createdBy(),
                    r.correctionOfReadingId(),
                    r.correctionReason(),
                    r.idempotencyKey(),
                    r.notes(),
                    r.createdAt()
            );
        }
    }

    public record VehicleMeterResetResponse(
            UUID id,
            UUID vehicleId,
            VehicleReadingType readingType,
            int fromEpoch,
            int toEpoch,
            BigDecimal lastReadingValue,
            BigDecimal newMeterValue,
            OffsetDateTime effectiveAt,
            String reason,
            UUID createdBy,
            OffsetDateTime createdAt
    ) {
        public static VehicleMeterResetResponse from(VehicleMeterReset r) {
            return new VehicleMeterResetResponse(
                    r.id(),
                    r.vehicleId(),
                    r.readingType(),
                    r.fromEpoch(),
                    r.toEpoch(),
                    r.lastReadingValue(),
                    r.newMeterValue(),
                    r.effectiveAt(),
                    r.reason(),
                    r.createdBy(),
                    r.createdAt()
            );
        }
    }

    public record VehicleMileageSummaryResponse(
            UUID vehicleId,
            OffsetDateTime from,
            OffsetDateTime to,
            BigDecimal openingOdometer,
            BigDecimal closingOdometer,
            BigDecimal distanceTravelledKm,
            BigDecimal openingEngineHours,
            BigDecimal closingEngineHours,
            BigDecimal engineHoursUsed,
            int meterResetCount,
            CoverageStatus coverageStatus,
            boolean abnormalDetected
    ) {
        public static VehicleMileageSummaryResponse from(VehicleMileageSummary s) {
            return new VehicleMileageSummaryResponse(
                    s.vehicleId(),
                    s.from(),
                    s.to(),
                    s.openingOdometer(),
                    s.closingOdometer(),
                    s.distanceTravelledKm(),
                    s.openingEngineHours(),
                    s.closingEngineHours(),
                    s.engineHoursUsed(),
                    s.meterResetCount(),
                    s.coverageStatus(),
                    s.abnormalDetected()
            );
        }
    }

    public record LatestVehicleReadingsResponse(
            UUID vehicleId,
            ReadingSnapshot odometer,
            ReadingSnapshot engineHours
    ) {
        public record ReadingSnapshot(
                UUID readingId,
                BigDecimal value,
                String unit,
                int meterEpoch,
                VehicleReadingSourceType sourceType,
                UUID sourceReferenceId,
                OffsetDateTime recordedAt,
                OffsetDateTime receivedAt
        ) {
            public static ReadingSnapshot from(VehicleReading r) {
                if (r == null) return null;
                return new ReadingSnapshot(
                        r.id(),
                        r.value(),
                        r.unit().name(),
                        r.meterEpoch(),
                        r.sourceType(),
                        r.sourceReferenceId(),
                        r.recordedAt(),
                        r.receivedAt()
                );
            }
        }
    }

    public record PageResponse<T>(
            List<T> content,
            int page,
            int limit,
            long totalElements,
            int totalPages
    ) {
    }
}