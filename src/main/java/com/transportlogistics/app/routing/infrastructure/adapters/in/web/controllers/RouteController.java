package com.transportlogistics.app.routing.infrastructure.adapters.in.web.controllers;

import com.transportlogistics.app.routing.application.ports.in.RouteUseCase;
import com.transportlogistics.app.routing.domain.model.Route;
import com.transportlogistics.app.routing.infrastructure.adapters.in.web.dto.request.RouteRequest;
import com.transportlogistics.app.routing.infrastructure.adapters.in.web.dto.response.MessageResponse;
import com.transportlogistics.app.routing.infrastructure.adapters.in.web.dto.response.RouteResponse;
import com.transportlogistics.app.routing.infrastructure.adapters.in.web.mappers.RouteWebMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class RouteController {

    private final RouteUseCase routes;
    private final RouteWebMapper mapper;

    @PostMapping("/routes")
    ResponseEntity<RouteResponse> create(@Valid @RequestBody RouteRequest r) {
        var created = routes.create(map(UUID.randomUUID(), r));
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
    RouteResponse update(@PathVariable UUID id, @Valid @RequestBody RouteRequest r) {
        var updated = routes.update(id, map(id, r));
        return mapper.toResponse(updated);
    }

    @DeleteMapping("/routes/{id}")
    MessageResponse deactivate(@PathVariable UUID id) {
        routes.deactivate(id);
        return new MessageResponse("Route deactivated");
    }

    private Route map(UUID id, RouteRequest r) {
        return new Route(id, r.code(), r.name(), r.originLocationId(), r.destinationLocationId(),
                r.plannedDistanceKm(), r.estimatedDurationMinutes(), r.active() == null || r.active(), r.stops());
    }
}
