package com.transportlogistics.app.tracking.adapters.inbound.web.controllers;

import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.tenancy.CurrentTenant;
import com.transportlogistics.app.tracking.adapters.inbound.web.dto.request.GpsExceptionRequests;
import com.transportlogistics.app.tracking.adapters.inbound.web.dto.response.GpsExceptionResponses;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsExceptionEpisode;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsExceptionEvidence;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.EpisodeStatus;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.ExceptionType;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Severity;
import com.transportlogistics.app.tracking.ports.inbound.GpsExceptionUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated @RestController @RequestMapping("/v1/tracking/gps-exceptions") @Tag(name="GPS Exceptions")
public class GpsExceptionController {
    private final GpsExceptionUseCase use; private final CurrentTenant tenants;
    public GpsExceptionController(GpsExceptionUseCase use,CurrentTenant tenants){this.use=use;this.tenants=tenants;}
    @GetMapping @PreAuthorize("hasAuthority('GPS_EXCEPTION_VIEW')")
    @Operation(summary="List same-Tenant GPS exception episodes",description="Uses [from,to), maximum seven days, mutable operational pagination ordered by last observation and ID; refresh after changes.")
    public ResponseEntity<GpsExceptionResponses.EpisodePage> list(@RequestParam @NotNull Instant from,@RequestParam @NotNull Instant to,
            @RequestParam(required=false) EpisodeStatus status,@RequestParam(required=false) ExceptionType type,
            @RequestParam(required=false) Severity severity,@RequestParam(required=false) UUID vehicleId,@RequestParam(required=false) UUID deviceId,
            @RequestParam(required=false) String cursor,@RequestParam(defaultValue="100") @Min(1) @Max(500) int limit){
        var page=use.episodes(context(),new GpsExceptionUseCase.Filter(from,to,status,type,severity,vehicleId,deviceId),cursor,limit);
        return noStore(new GpsExceptionResponses.EpisodePage(page.items().stream().map(GpsExceptionController::episode).toList(),page.nextCursor()));
    }
    @GetMapping("/{episodeId}") @PreAuthorize("hasAuthority('GPS_EXCEPTION_VIEW')") @Operation(summary="Get a same-Tenant GPS exception episode")
    public ResponseEntity<GpsExceptionResponses.Episode> detail(@PathVariable UUID episodeId){return noStore(episode(use.episode(context(),episodeId).orElseThrow(GpsExceptionController::notFound)));}
    @GetMapping("/{episodeId}/evidence") @PreAuthorize("hasAuthority('GPS_EXCEPTION_VIEW')") @Operation(summary="List immutable minimized GPS exception evidence")
    public ResponseEntity<GpsExceptionResponses.EvidencePage> evidence(@PathVariable UUID episodeId,@RequestParam(required=false) String cursor,@RequestParam(defaultValue="100") @Min(1) @Max(500) int limit){
        var page=use.evidence(context(),episodeId,cursor,limit);return noStore(new GpsExceptionResponses.EvidencePage(page.items().stream().map(GpsExceptionController::evidence).toList(),page.nextCursor()));}
    @PostMapping("/{episodeId}/acknowledge") @PreAuthorize("hasAuthority('GPS_EXCEPTION_REVIEW')") @Operation(summary="Acknowledge a same-Tenant GPS exception episode")
    public ResponseEntity<GpsExceptionResponses.Acknowledgement> acknowledge(@PathVariable UUID episodeId,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody GpsExceptionRequests.Acknowledge request){
        var a=use.acknowledge(context(),episodeId,request.expectedVersion(),request.reason(),key);return noStore(new GpsExceptionResponses.Acknowledgement(a.episodeId(),a.status().name(),a.severity().name(),a.version(),a.acknowledgedAt()));}
    private GpsExceptionUseCase.Context context(){var c=tenants.required();return new GpsExceptionUseCase.Context(c.tenantId(),c.actorId(),c.correlationId(),Instant.now());}
    private static GpsExceptionResponses.Episode episode(GpsExceptionEpisode e){return new GpsExceptionResponses.Episode(e.id(),e.deviceId(),e.vehicleId(),e.type().name(),e.severity().name(),e.status().name(),e.openedAt(),e.lastObservedAt(),e.resolvedAt(),e.evidenceCount(),e.consecutiveRecoveryPoints(),e.version());}
    private static GpsExceptionResponses.Evidence evidence(GpsExceptionEvidence e){return new GpsExceptionResponses.Evidence(e.id(),e.telemetrySourceTimestamp(),e.assessedAt(),e.trust().name(),e.ordering().name(),e.reliabilityState().name(),Arrays.stream(e.qualityCodes().split(",")).toList(),e.transition().name());}
    private static <T>ResponseEntity<T> noStore(T body){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);} private static NotFoundException notFound(){return new NotFoundException("GPS_EXCEPTION_NOT_FOUND","GPS exception not found");}
}
