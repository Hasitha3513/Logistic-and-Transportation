package com.transportlogistics.app.tracking.adapters.inbound.web.controllers;

import com.transportlogistics.app.shared.domain.TooManyRequestsException;
import com.transportlogistics.app.shared.web.ApiError;
import com.transportlogistics.app.shared.web.CorrelationIdFilter;
import com.transportlogistics.app.tenancy.CurrentTenant;
import com.transportlogistics.app.tracking.adapters.inbound.web.dto.request.JourneyReplayRequests;
import com.transportlogistics.app.tracking.adapters.inbound.web.dto.response.JourneyReplayResponses;
import com.transportlogistics.app.tracking.adapters.inbound.web.mappers.JourneyReplayWebMapper;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.Coverage;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.OverlayType;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ReplayPage;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ReplayQuery;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ReplayRequest;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.StopPage;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.StopReplayQuery;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.TenantContext;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayException;
import com.transportlogistics.app.tracking.domain.journeyreplay.ReplayQueryPolicy;
import com.transportlogistics.app.tracking.ports.inbound.JourneyReplayQueryUseCase;
import com.transportlogistics.app.tracking.ports.outbound.JourneyReplayAuditPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/tracking/journey-replays")
@ConditionalOnBean(JourneyReplayQueryUseCase.class)
@ConditionalOnProperty(name = "app.tracking.journey-replay.enabled", havingValue = "true", matchIfMissing = true)
@Tag(name = "Journey Replay", description = "Bounded, Tenant-scoped historical telemetry replay. Coordinates are sensitive; responses are never cacheable.")
public class JourneyReplayController {
    private final JourneyReplayQueryUseCase queries;
    private final JourneyReplayAuditPort audit;
    private final JourneyReplayWebMapper mapper;
    private final CurrentTenant tenants;
    private final Clock clock;
    private final JourneyReplayAdmissionGuard admission;

    public JourneyReplayController(JourneyReplayQueryUseCase queries, JourneyReplayAuditPort audit,
            JourneyReplayWebMapper mapper, CurrentTenant tenants, Clock clock,
            JourneyReplayAdmissionGuard admission) {
        this.queries = queries;
        this.audit = audit;
        this.mapper = mapper;
        this.tenants = tenants;
        this.clock = clock;
        this.admission = admission;
    }

    @PostMapping("/points/query")
    @PreAuthorize("hasAuthority('JOURNEY_REPLAY_VIEW')")
    @Operation(summary = "Query a bounded chronological replay point page",
            description = "Uses an authenticated 15-minute opaque cursor. Partial retention, quality flags and the 20,000-point browser ceiling are explicit.")
    public ResponseEntity<JourneyReplayResponses.Points> points(
            @Valid @RequestBody JourneyReplayRequests.Query request) {
        ReplayQuery query = query(request.vehicleId(), request.tripId(), request.from(), request.to(),
                request.limit(), request.cursor(), Set.of());
        Timer.Sample sample = admit("points");
        ReplayPage result;
        try {
            result = queries.points(query);
        } catch (RuntimeException exception) {
            admission.rejected(sample, "points", reason(exception));
            throw exception;
        }
        admission.success(sample, "points", result.coverage().name(), result.items().size(), Set.of());
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
                request.limit() == null ? JourneyReplayModels.DEFAULT_STOP_LIMIT : request.limit(), request.cursor());
        Timer.Sample sample = admit("stops");
        StopPage result;
        try {
            result = queries.stops(query);
        } catch (RuntimeException exception) {
            admission.rejected(sample, "stops", reason(exception));
            throw exception;
        }
        admission.success(sample, "stops", result.coverage().name(), result.items().size(), Set.of());
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
        Timer.Sample sample = admit("incidents");
        JourneyReplayModels.IncidentPage result;
        try {
            result = queries.incidents(query);
        } catch (RuntimeException exception) {
            admission.rejected(sample, "incidents", reason(exception));
            throw exception;
        }
        admission.success(sample, "incidents", Coverage.COMPLETE.name(), result.items().size(),
                types.stream().map(Enum::name).collect(java.util.stream.Collectors.toUnmodifiableSet()));
        audit("JOURNEY_REPLAY_INCIDENTS_VIEWED", query, result.items().size(), Coverage.COMPLETE);
        return privateResponse(new JourneyReplayResponses.Incidents(mapper.incidents(result.items()), result.nextCursor(),
                result.snapshotRecordedAt(), new JourneyReplayResponses.Range(query.requestedRange().from(), query.requestedRange().to()),
                null, Coverage.COMPLETE, List.of(), types));
    }

    private ReplayQuery query(UUID vehicleId, UUID tripId, Instant from, Instant to,
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

    private Timer.Sample admit(String operation) {
        var context = tenants.required();
        return admission.admit(context.tenantId(), context.actorId(), operation);
    }

    private static String reason(RuntimeException exception) {
        return exception instanceof JourneyReplayException replay ? replay.error().name() : "FAILED";
    }

    @ExceptionHandler(TooManyRequestsException.class)
    ResponseEntity<ApiError> rateLimited(
            TooManyRequestsException exception, HttpServletRequest request) {
        var body = new ApiError(OffsetDateTime.now(), 429,
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(), exception.code(), exception.getMessage(),
                request.getRequestURI(), (String) request.getAttribute(CorrelationIdFilter.ATTRIBUTE), List.of());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "60").body(body);
    }
}
