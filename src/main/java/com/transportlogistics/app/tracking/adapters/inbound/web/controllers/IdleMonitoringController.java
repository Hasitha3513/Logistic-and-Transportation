package com.transportlogistics.app.tracking.adapters.inbound.web.controllers;

import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.tenancy.CurrentTenant;
import com.transportlogistics.app.tracking.adapters.inbound.web.dto.response.IdleMonitoringResponses;
import com.transportlogistics.app.tracking.adapters.inbound.web.mappers.IdleMonitoringWebMapper;
import com.transportlogistics.app.tracking.ports.inbound.IdleMonitoringQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated @RestController @RequestMapping("/v1/tracking/idle-monitoring")
@Tag(name="Idle Monitoring",description="Read-only same-Tenant idle state, confirmed episode and minimized evidence views")
public class IdleMonitoringController {
    private final IdleMonitoringQuery query; private final IdleMonitoringWebMapper mapper;
    private final CurrentTenant tenants; private final Clock clock;
    public IdleMonitoringController(IdleMonitoringQuery query,IdleMonitoringWebMapper mapper,CurrentTenant tenants,Clock clock){this.query=query;this.mapper=mapper;this.tenants=tenants;this.clock=clock;}
    @GetMapping("/states") @PreAuthorize("hasAuthority('IDLE_MONITOR_VIEW')")
    @Operation(summary="List current same-Tenant idle monitoring states")
    public ResponseEntity<IdleMonitoringResponses.StatePage> states(@RequestParam(required=false) UUID vehicleId,
            @RequestParam(required=false) String state,@RequestParam(required=false) String cursor,
            @RequestParam(defaultValue="50") @Min(1) @Max(100) int limit){return noStore(mapper.states(query.states(context(),new IdleMonitoringQuery.StateFilter(vehicleId,state),cursor,limit)));}
    @GetMapping("/episodes") @PreAuthorize("hasAuthority('IDLE_EVENT_VIEW')")
    @Operation(summary="List confirmed or closed same-Tenant idle episodes")
    public ResponseEntity<IdleMonitoringResponses.EpisodePage> episodes(@RequestParam(required=false) UUID vehicleId,
            @RequestParam @NotNull Instant from,@RequestParam @NotNull Instant to,
            @RequestParam(required=false) String endReason,@RequestParam(required=false) String cursor,
            @RequestParam(defaultValue="50") @Min(1) @Max(100) int limit){return noStore(mapper.episodes(query.episodes(context(),new IdleMonitoringQuery.EpisodeFilter(vehicleId,from,to,endReason),cursor,limit)));}
    @GetMapping("/episodes/{episodeId}") @PreAuthorize("hasAuthority('IDLE_EVENT_VIEW')")
    @Operation(summary="Get a confirmed or closed same-Tenant idle episode")
    public ResponseEntity<IdleMonitoringResponses.Episode> episode(@PathVariable UUID episodeId){return noStore(mapper.episode(query.episode(context(),episodeId).orElseThrow(IdleMonitoringController::notFound)));}
    @GetMapping("/episodes/{episodeId}/evidence") @PreAuthorize("hasAuthority('IDLE_EVENT_VIEW')")
    @Operation(summary="List chronological minimized evidence for a same-Tenant idle episode")
    public ResponseEntity<IdleMonitoringResponses.EvidencePage> evidence(@PathVariable UUID episodeId,
            @RequestParam(required=false) String cursor,@RequestParam(defaultValue="50") @Min(1) @Max(100) int limit){return noStore(mapper.evidence(query.evidence(context(),episodeId,cursor,limit)));}
    private IdleMonitoringQuery.Context context(){var c=tenants.required();return new IdleMonitoringQuery.Context(c.tenantId(),c.actorId(),c.correlationId(),clock.instant());}
    private static <T>ResponseEntity<T> noStore(T body){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("Referrer-Policy","no-referrer").body(body);}
    private static NotFoundException notFound(){return new NotFoundException("IDLE_EPISODE_NOT_FOUND","Idle episode not found");}
}
