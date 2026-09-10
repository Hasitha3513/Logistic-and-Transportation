package com.transportlogistics.app.tracking.adapters.inbound.web.controllers;

import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.tenancy.CurrentTenant;
import com.transportlogistics.app.tracking.adapters.inbound.web.dto.request.GeofenceRequests;
import com.transportlogistics.app.tracking.adapters.inbound.web.dto.response.GeofenceResponses;
import com.transportlogistics.app.tracking.adapters.inbound.web.mappers.GeofenceWebMapper;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceLifecycle;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceType;
import com.transportlogistics.app.tracking.ports.inbound.GeofenceManagementUseCase;
import com.transportlogistics.app.tracking.ports.inbound.GeofenceQuery;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/v1/tracking/geofences")
public class GeofenceController {
    private final GeofenceManagementUseCase management;
    private final GeofenceQuery query;
    private final GeofenceWebMapper mapper;
    private final CurrentTenant currentTenant;

    public GeofenceController(GeofenceManagementUseCase management, GeofenceQuery query,
                              GeofenceWebMapper mapper, CurrentTenant currentTenant) {
        this.management = management;
        this.query = query;
        this.mapper = mapper;
        this.currentTenant = currentTenant;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('GEOFENCE_MANAGE')")
    public GeofenceResponses.Definition create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody GeofenceRequests.Create request) {
        return mapper.response(management.create(context(), mapper.command(request), idempotencyKey));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('GEOFENCE_VIEW')")
    public GeofenceResponses.DefinitionPage list(
            @RequestParam(required = false) GeofenceType type,
            @RequestParam(required = false) GeofenceLifecycle lifecycle,
            @RequestParam(required = false) UUID locationId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return mapper.response(query.list(tenant(), type, lifecycle, locationId, page, size));
    }

    @GetMapping("/{geofenceId}")
    @PreAuthorize("hasAuthority('GEOFENCE_VIEW')")
    public GeofenceResponses.Definition get(@PathVariable UUID geofenceId) {
        return mapper.response(query.get(tenant(), geofenceId).orElseThrow(GeofenceController::notFound));
    }

    @PutMapping("/{geofenceId}")
    @PreAuthorize("hasAuthority('GEOFENCE_MANAGE')")
    public GeofenceResponses.Definition update(
            @PathVariable UUID geofenceId, @Valid @RequestBody GeofenceRequests.Update request) {
        return mapper.response(management.update(
                context(), geofenceId, request.expectedVersion(), mapper.command(request)));
    }

    @PostMapping("/{geofenceId}/activate")
    @PreAuthorize("hasAuthority('GEOFENCE_MANAGE')")
    public GeofenceResponses.Definition activate(
            @PathVariable UUID geofenceId, @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody GeofenceRequests.Version request) {
        return mapper.response(management.activate(
                context(), geofenceId, request.expectedVersion(), idempotencyKey));
    }

    @PostMapping("/{geofenceId}/disable")
    @PreAuthorize("hasAuthority('GEOFENCE_MANAGE')")
    public GeofenceResponses.Definition disable(
            @PathVariable UUID geofenceId, @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody GeofenceRequests.Reason request) {
        return mapper.response(management.disable(context(), geofenceId, request.expectedVersion(),
                request.reason(), idempotencyKey));
    }

    @PostMapping("/{geofenceId}/retire")
    @PreAuthorize("hasAuthority('GEOFENCE_MANAGE')")
    public GeofenceResponses.Definition retire(
            @PathVariable UUID geofenceId, @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody GeofenceRequests.Reason request) {
        return mapper.response(management.retire(context(), geofenceId, request.expectedVersion(),
                request.reason(), idempotencyKey));
    }

    @GetMapping("/memberships")
    @PreAuthorize("hasAuthority('GEOFENCE_VIEW')")
    public GeofenceResponses.MembershipPage memberships(
            @RequestParam(required = false) UUID vehicleId,
            @RequestParam(required = false) UUID geofenceId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return mapper.membershipResponse(
                query.memberships(tenant(), vehicleId, geofenceId, page, size));
    }

    @GetMapping("/transitions")
    @PreAuthorize("hasAuthority('GEOFENCE_EVENT_VIEW')")
    public GeofenceResponses.TransitionPage transitions(
            @RequestParam(required = false) UUID geofenceId,
            @RequestParam(required = false) UUID vehicleId,
            @RequestParam(required = false) GeofenceType type,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "100") @Min(1) @Max(100) int limit) {
        return mapper.response(query.transitions(
                tenant(), geofenceId, vehicleId, type, from, to, cursor, limit));
    }

    @GetMapping("/unauthorized-transitions")
    @PreAuthorize("hasAuthority('GEOFENCE_EVENT_VIEW')")
    public GeofenceResponses.TransitionPage unauthorizedTransitions(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "100") @Min(1) @Max(100) int limit) {
        return mapper.response(query.unauthorizedTransitions(tenant(), from, to, cursor, limit));
    }

    private UUID tenant() {
        return currentTenant.required().tenantId();
    }

    private GeofenceManagementUseCase.Context context() {
        var current = currentTenant.required();
        return new GeofenceManagementUseCase.Context(current.tenantId(), current.actorId(),
                current.correlationId(), Instant.now());
    }

    private static NotFoundException notFound() {
        return new NotFoundException("GEOFENCE_NOT_FOUND", "Geofence not found");
    }
}
