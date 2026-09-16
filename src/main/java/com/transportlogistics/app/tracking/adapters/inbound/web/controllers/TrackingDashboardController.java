package com.transportlogistics.app.tracking.adapters.inbound.web.controllers;

import com.transportlogistics.app.shared.domain.TooManyRequestsException;
import com.transportlogistics.app.shared.web.ApiError;
import com.transportlogistics.app.shared.web.CorrelationIdFilter;
import com.transportlogistics.app.tenancy.CurrentTenant;
import com.transportlogistics.app.tracking.adapters.inbound.web.dto.request.TrackingDashboardRequests;
import com.transportlogistics.app.tracking.adapters.inbound.web.dto.response.TrackingDashboardResponses;
import com.transportlogistics.app.tracking.adapters.inbound.web.mappers.TrackingDashboardWebMapper;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardException;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.DashboardFilter;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.DashboardQuery;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Disclosure;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardPolicy;
import com.transportlogistics.app.tracking.ports.inbound.TrackingDashboardQueryUseCase;
import com.transportlogistics.app.tracking.ports.outbound.TrackingDashboardAuditPort;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/tracking/dashboard")
@ConditionalOnBean(TrackingDashboardQueryUseCase.class)
@ConditionalOnProperty(name = "app.tracking.dashboard.enabled", havingValue = "true", matchIfMissing = true)
@Tag(name = "Tracking Dashboard", description = "Bounded same-Tenant operational tracking summary. Precise coordinates and incident sections require additional permissions; responses are never cacheable.")
public class TrackingDashboardController {
    private final TrackingDashboardQueryUseCase query;
    private final TrackingDashboardAuditPort audit;
    private final TrackingDashboardWebMapper mapper;
    private final TrackingDashboardAdmissionGuard admission;
    private final CurrentTenant tenants;
    private final Clock clock;

    public TrackingDashboardController(TrackingDashboardQueryUseCase query,
            TrackingDashboardAuditPort audit, TrackingDashboardWebMapper mapper,
            TrackingDashboardAdmissionGuard admission, CurrentTenant tenants, Clock clock) {
        this.query = query;
        this.audit = audit;
        this.mapper = mapper;
        this.admission = admission;
        this.tenants = tenants;
        this.clock = clock;
    }

    @PostMapping("/query")
    @PreAuthorize("hasAuthority('TRACKING_DASHBOARD_VIEW')")
    @Operation(summary = "Query the operational Tracking dashboard",
            description = "Maximum 100 Vehicles; authenticated five-minute Tenant/filter-bound cursor; exact optional sections are permission-filtered; degraded sources remain explicit.")
    @ApiResponse(responseCode = "200", description = "Bounded result, possibly with explicit degraded sources")
    @ApiResponse(responseCode = "400", description = "Malformed filter, enum, bound or cursor")
    @ApiResponse(responseCode = "403", description = "TRACKING_DASHBOARD_VIEW is required")
    @ApiResponse(responseCode = "429", description = "10 requests per actor and 40 per Tenant per minute")
    @ApiResponse(responseCode = "503", description = "No truthful Tracking live-state result is available")
    public ResponseEntity<TrackingDashboardResponses.Dashboard> query(
            @Valid @RequestBody TrackingDashboardRequests.Query request, Authentication authentication) {
        var context = tenants.required();
        DashboardFilter filter = filter(request);
        int pageSize = request.pageSize() == null ? TrackingDashboardPolicy.DEFAULT_PAGE_SIZE : request.pageSize();
        DashboardQuery command = new DashboardQuery(context.tenantId(), filter, request.cursor(), pageSize);
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(value -> value.getAuthority()).collect(java.util.stream.Collectors.toUnmodifiableSet());
        Disclosure disclosure = new Disclosure(authorities.contains("TRACKING_VIEW"),
                authorities.contains("GEOFENCE_EVENT_VIEW"), authorities.contains("SPEED_EVENT_VIEW"),
                authorities.contains("ROUTE_DEVIATION_EVENT_VIEW"), authorities.contains("JOURNEY_REPLAY_VIEW"));
        Timer.Sample sample;
        try {
            sample = admission.admit(context.tenantId(), context.actorId());
        } catch (TooManyRequestsException exception) {
            audit(context, "TRACKING_DASHBOARD_RATE_LIMITED", filter, pageSize, 0, Set.of(), Set.of("RATE_LIMITED"));
            throw exception;
        }
        var result = query.query(command, disclosure);
        Set<String> included = included(disclosure);
        admission.complete(sample, "SUCCESS", result.vehicles().size(), result.sourceStatus().name(), included);
        if (request.cursor() == null) {
            audit(context, "TRACKING_DASHBOARD_VIEWED", filter, pageSize, result.vehicles().size(),
                    included, statuses(result));
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header("Referrer-Policy", "no-referrer").body(mapper.response(result));
    }

    private static DashboardFilter filter(TrackingDashboardRequests.Query request) {
        return new DashboardFilter(request.vehicleIds(), request.freshness(), request.connectivity(),
                request.motion(), request.incidentTypes(), Boolean.TRUE.equals(request.includeHeatMap()),
                request.includeIncidents() == null || request.includeIncidents());
    }

    private void audit(com.transportlogistics.app.tenancy.TenantExecutionContext context, String action,
            DashboardFilter filter, int pageSize, int resultCount, Set<String> included, Set<String> statuses) {
        audit.record(context.tenantId(), context.actorId(), context.correlationId(), action,
                categories(filter), pageSize, resultCount, included, statuses, clock.instant());
    }

    private static Set<String> categories(DashboardFilter filter) {
        var result = new HashSet<String>();
        if (!filter.vehicleIds().isEmpty()) result.add("VEHICLE");
        if (!filter.freshness().isEmpty()) result.add("FRESHNESS");
        if (!filter.connectivity().isEmpty()) result.add("CONNECTIVITY");
        if (!filter.motion().isEmpty()) result.add("MOTION");
        if (!filter.incidentTypes().isEmpty()) result.add("INCIDENT_TYPE");
        if (filter.includeHeatMap()) result.add("HEAT_MAP");
        if (filter.includeIncidents()) result.add("INCIDENTS");
        return Set.copyOf(result);
    }

    private static Set<String> included(Disclosure disclosure) {
        var result = new HashSet<String>();
        if (disclosure.coordinates()) result.add("COORDINATES");
        if (disclosure.geofenceIncidents()) result.add("GEOFENCE");
        if (disclosure.speedIncidents()) result.add("SPEED");
        if (disclosure.routeDeviationIncidents()) result.add("ROUTE_DEVIATION");
        if (disclosure.journeyReplay()) result.add("JOURNEY_REPLAY");
        return Set.copyOf(result);
    }

    private static Set<String> statuses(com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.DashboardPage result) {
        var statuses = new HashSet<String>();
        statuses.add("LIVE=" + result.sourceStatus());
        result.incidentSourceStatuses().forEach((type, status) -> statuses.add(type + "=" + status));
        return Set.copyOf(statuses);
    }

    @ExceptionHandler(TrackingDashboardException.class)
    ResponseEntity<ApiError> invalid(TrackingDashboardException exception, HttpServletRequest request) {
        var body = new ApiError(OffsetDateTime.now(), 400, HttpStatus.BAD_REQUEST.getReasonPhrase(),
                exception.code(), exception.getMessage(), request.getRequestURI(),
                (String) request.getAttribute(CorrelationIdFilter.ATTRIBUTE), List.of());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    ResponseEntity<ApiError> rateLimited(TooManyRequestsException exception, HttpServletRequest request) {
        var body = new ApiError(OffsetDateTime.now(), 429, HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                exception.code(), exception.getMessage(), request.getRequestURI(),
                (String) request.getAttribute(CorrelationIdFilter.ATTRIBUTE), List.of());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "60").body(body);
    }
}
