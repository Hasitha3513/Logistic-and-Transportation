package com.transportlogistics.app.routing.infrastructure.adapters.in.web;

import com.transportlogistics.app.routing.application.ports.in.RouteUseCase;
import com.transportlogistics.app.routing.domain.model.Route;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
class RouteController {
    private final RouteUseCase routes;

    @PostMapping("/routes")
    ResponseEntity<Route> create(@Valid @RequestBody RouteRequest r) {
        return ResponseEntity.status(201).body(routes.create(map(UUID.randomUUID(), r)));
    }

    @GetMapping("/routes")
    List<Route> list(@RequestParam(required = false) String query,
                     @RequestParam(required = false) UUID originLocationId,
                     @RequestParam(required = false) UUID destinationLocationId,
                     @RequestParam(required = false) Boolean active) {
        return routes.search(query, originLocationId, destinationLocationId, active);
    }

    @GetMapping("/routes/{id}")
    Route get(@PathVariable UUID id) {
        return routes.get(id);
    }

    @PutMapping("/routes/{id}")
    Route update(@PathVariable UUID id, @Valid @RequestBody RouteRequest r) {
        return routes.update(id, map(id, r));
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

    record RouteRequest(@NotBlank @Size(max = 40) String code, @NotBlank @Size(max = 160) String name,
                        @NotNull UUID originLocationId,
                        @NotNull UUID destinationLocationId, @NotNull @Positive Double plannedDistanceKm,
                        @NotNull @Positive Integer estimatedDurationMinutes, @Size(max = 50) List<@NotNull UUID> stops,
                        Boolean active) {
    }

    record MessageResponse(String message) {
    }
}
