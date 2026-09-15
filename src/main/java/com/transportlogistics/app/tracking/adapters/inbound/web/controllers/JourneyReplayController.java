package com.transportlogistics.app.tracking.adapters.inbound.web.controllers;

import com.transportlogistics.app.tenancy.CurrentTenant;
import com.transportlogistics.app.tracking.adapters.inbound.web.dto.request.JourneyReplayRequests;
import com.transportlogistics.app.tracking.adapters.inbound.web.dto.response.JourneyReplayResponses;
import com.transportlogistics.app.tracking.adapters.inbound.web.mappers.JourneyReplayWebMapper;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.Coverage;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.OverlayType;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ReplayPage;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ReplayQuery;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ReplayRequest;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.StopPage;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.StopReplayQuery;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.TenantContext;
import com.transportlogistics.app.tracking.domain.journeyreplay.ReplayQueryPolicy;
import com.transportlogistics.app.tracking.ports.inbound.JourneyReplayQueryUseCase;
import com.transportlogistics.app.tracking.ports.outbound.JourneyReplayAuditPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/tracking/journey-replays")
@ConditionalOnBean(JourneyReplayQueryUseCase.class)
@Tag(name = "Journey Replay", description = "Bounded, Tenant-scoped historical telemetry replay. Coordinates are sensitive; responses are never cacheable.")
public class JourneyReplayController {
    private final JourneyReplayQueryUseCase queries;
    private final JourneyReplayAuditPort audit;
    private final JourneyReplayWebMapper mapper;
    private final CurrentTenant tenants;
    private final Clock clock;

    public JourneyReplayController(JourneyReplayQueryUseCase queries, JourneyReplayAuditPort audit,
            JourneyReplayWebMapper mapper, CurrentTenant tenants, Clock clock) {
        this.queries = queries; this.audit = audit; this.mapper = mapper; this.tenants = tenants; this.clock = clock;
    }

    @PostMapping("/points/query")
    @PreAuthorize("hasAuthority('JOURNEY_REPLAY_VIEW')")
    @Operation(summary = "Query a bounded chronological replay point page",
            description = "Uses an authenticated 15-minute opaque cursor. Partial retention, quality flags and the 20,000-point browser ceiling are explicit.")
    public ResponseEntity<JourneyReplayResponses.Points> points(
            @Valid @RequestBody JourneyReplayRequests.Query request) {
        ReplayQuery query = query(request.vehicleId(), request.tripId(), request.from(), request.to(),
                request.limit(), request.cursor(), Set.of());
        ReplayPage result = queries.points(query);
        audit("JOURNEY_REPLAY_POINTS_VIEWED", query, result.items().size(), result.coverage());
        return privateResponse(mapper.response(result));
    }

    @PostMapping("/stops/query")
    @PreAuthorize("hasAuthority('JOURNEY_REPLAY_VIEW')")
    @Operation(summary = "Query deterministic replay stops",
            description = "Stop evidence is bounded by the replay point ceiling and may report truncated boundaries or partial retention.")
    public ResponseEntity<JourneyReplayResponses.Stops> stops(
            @Valid @RequestBody JourneyReplayRequests.StopQuery request) {
        ReplayQuery replay = query(request.vehicleId(), request.tripId(), request.from(), request.to(),
                null, null, Set.of());
        StopReplayQuery query = new StopReplayQuery(replay,
                request.limit() == null ? com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.DEFAULT_STOP_LIMIT : request.limit(), request.cursor());
        StopPage result = queries.stops(query);
        audit("JOURNEY_REPLAY_STOPS_VIEWED", replay, result.items().size(), result.coverage());
        return privateResponse(mapper.response(result));
    }

    @PostMapping("/incidents/query")
    @PreAuthorize("hasAuthority('JOURNEY_REPLAY_VIEW') and hasAuthority('JOURNEY_REPLAY_INCIDENT_VIEW')")
    @Operation(summary = "Query producer-labelled incident overlays",
            description = "Overlay availability and physical-field acceptance remain explicit; unavailable producers are never inferred.")
    public ResponseEntity<JourneyReplayResponses.Incidents> incidents(
            @Valid @RequestBody JourneyReplayRequests.IncidentQuery request) {
        Set<OverlayType> types = request.types() == null || request.types().isEmpty()
                ? Set.of(OverlayType.GEOFENCE) : Set.copyOf(request.types());
        ReplayQuery query = query(request.vehicleId(), request.tripId(), request.from(), request.to(),
                request.limit(), request.cursor(), types);
        var result = queries.incidents(query);
        audit("JOURNEY_REPLAY_INCIDENTS_VIEWED", query, result.items().size(), Coverage.COMPLETE);
        return privateResponse(new JourneyReplayResponses.Incidents(mapper.incidents(result.items()), result.nextCursor(),
                result.snapshotRecordedAt(), new JourneyReplayResponses.Range(query.requestedRange().from(), query.requestedRange().to()),
                null, Coverage.COMPLETE, java.util.List.of(), types));
    }

    private ReplayQuery query(java.util.UUID vehicleId, java.util.UUID tripId, Instant from, Instant to,
            Integer limit, String cursor, Set<OverlayType> overlays) {
        var context = tenants.required();
        return ReplayQueryPolicy.validate(new TenantContext(context.tenantId(), context.actorId()),
                new ReplayRequest(vehicleId, tripId, from, to, limit, cursor, overlays, false), clock.instant());
    }

    private void audit(String action, ReplayQuery query, int count, Coverage coverage) {
        if (query.cursor() != null) return;
        var context = tenants.required();
        audit.record(context.tenantId(), context.actorId(), context.correlationId(), action,
                query.selector().type(), query.selector().id(), query.requestedRange().duration(),
                count, coverage, query.overlays(), clock.instant());
    }

    private static <T> ResponseEntity<T> privateResponse(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header("Referrer-Policy", "no-referrer").body(body);
    }
}
