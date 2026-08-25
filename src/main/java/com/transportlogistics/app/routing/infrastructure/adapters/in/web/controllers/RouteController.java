package com.transportlogistics.app.routing.infrastructure.adapters.in.web.controllers;

import com.transportlogistics.app.routing.application.ports.in.RouteUseCase;
import com.transportlogistics.app.routing.domain.model.Route;
import com.transportlogistics.app.routing.infrastructure.adapters.in.web.dto.request.ApplyOptimizationRequest;
import com.transportlogistics.app.routing.infrastructure.adapters.in.web.dto.request.RouteDisruptionRequest;
import com.transportlogistics.app.routing.infrastructure.adapters.in.web.dto.request.RouteRequest;
import com.transportlogistics.app.routing.infrastructure.adapters.in.web.dto.response.*;
import com.transportlogistics.app.routing.infrastructure.adapters.in.web.mappers.RouteWebMapper;
import com.transportlogistics.app.shared.utils.PrincipalUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class RouteController {

    private final RouteUseCase routes;
    private final RouteWebMapper mapper;

    @PostMapping("/routes")
    ResponseEntity<RouteResponse> create(@Valid @RequestBody RouteRequest r, Principal principal) {
        var created = routes.create(map(UUID.randomUUID(), r), PrincipalUtils.resolveActorName(principal));
        return ResponseEntity.status(201).body(mapper.toResponse(created));
    }

    @GetMapping("/routes")
    List<RouteResponse> list(@RequestParam(required = false) String query,
                             @RequestParam(required = false) UUID originLocationId,
                             @RequestParam(required = false) UUID destinationLocationId,
                             @RequestParam(required = false) Boolean active) {
        return mapper.toResponseList(routes.search(query, originLocationId, destinationLocationId, active));
    }

    @GetMapping("/routes/{id}")
    RouteResponse get(@PathVariable UUID id) {
        return mapper.toResponse(routes.get(id));
    }

    @PutMapping("/routes/{id}")
    RouteResponse update(@PathVariable UUID id, @Valid @RequestBody RouteRequest r, Principal principal) {
        var updated = routes.update(id, map(id, r), PrincipalUtils.resolveActorName(principal));
        return mapper.toResponse(updated);
    }

    @DeleteMapping("/routes/{id}")
    MessageResponse deactivate(@PathVariable UUID id, Principal principal) {
        routes.deactivate(id, PrincipalUtils.resolveActorName(principal));
        return new MessageResponse("Route deactivated");
    }

    // US-21: Route Revisions
    @GetMapping("/routes/{id}/revisions")
    List<RouteRevisionResponse> listRevisions(@PathVariable UUID id) {
        return mapper.toRevisionResponseList(routes.getRevisions(id));
    }

    @GetMapping("/routes/{id}/revisions/{revisionNumber}")
    RouteRevisionResponse getRevision(@PathVariable UUID id, @PathVariable int revisionNumber) {
        return mapper.toRevisionResponse(routes.getRevision(id, revisionNumber));
    }

    // US-23: Route Disruptions
    @PostMapping("/routes/{id}/disruptions")
    ResponseEntity<RouteDisruptionResponse> createDisruption(@PathVariable UUID id,
                                                            @Valid @RequestBody RouteDisruptionRequest r,
                                                            Principal principal) {
        var disruption = routes.createDisruption(
                id,
                r.disruptionType(),
                r.severity(),
                r.description(),
                r.effectiveFrom(),
                r.effectiveUntil(),
                r.detourRouteId(),
                PrincipalUtils.resolveActorName(principal)
        );
        return ResponseEntity.status(201).body(mapper.toDisruptionResponse(disruption));
    }

    @GetMapping("/routes/{id}/disruptions")
    List<RouteDisruptionResponse> listDisruptions(@PathVariable UUID id) {
        return mapper.toDisruptionResponseList(routes.getDisruptions(id));
    }

    @PostMapping("/routes/{id}/disruptions/{disruptionId}/resolve")
    RouteDisruptionResponse resolveDisruption(@PathVariable UUID id,
                                             @PathVariable UUID disruptionId,
                                             Principal principal) {
        var resolved = routes.resolveDisruption(id, disruptionId, PrincipalUtils.resolveActorName(principal));
        return mapper.toDisruptionResponse(resolved);
    }

    @GetMapping("/routes/disruptions/active")
    List<RouteDisruptionResponse> listActiveDisruptions() {
        return mapper.toDisruptionResponseList(routes.getActiveDisruptions());
    }

    // US-20: Route Optimization Preview and Apply
    @PostMapping("/routes/{id}/optimize")
    ResponseEntity<RouteOptimizationResponse> optimize(@PathVariable UUID id) {
        var result = routes.optimizeRoute(id);
        return ResponseEntity.ok(mapper.toOptimizationResponse(result));
    }

    @PostMapping("/routes/{id}/apply-optimization")
    ResponseEntity<RouteResponse> applyOptimization(@PathVariable UUID id,
                                                    @Valid @RequestBody ApplyOptimizationRequest r,
                                                    Principal principal) {
        var updated = routes.applyOptimization(id, r.optimizedStopLocationIds(), PrincipalUtils.resolveActorName(principal));
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    // US-22: Route Performance Analytics
    @GetMapping("/routes/{id}/performance")
    ResponseEntity<RoutePerformanceResponse> getPerformance(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    ) {
        var analytics = routes.getRoutePerformance(id, from, to);
        return ResponseEntity.ok(mapper.toPerformanceResponse(analytics));
    }

    private Route map(UUID id, RouteRequest r) {
        return new Route(id, r.code(), r.name(), r.originLocationId(), r.destinationLocationId(),
                r.plannedDistanceKm(), r.estimatedDurationMinutes(), r.active() == null || r.active(), r.stops());
    }
}
