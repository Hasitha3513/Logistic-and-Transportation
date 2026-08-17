package com.transportlogistics.app.trip.infrastructure.adapters.in.web;

import com.transportlogistics.app.trip.application.ports.in.TripUseCase;
import com.transportlogistics.app.trip.domain.model.Trip;
import com.transportlogistics.app.trip.domain.model.TripCommand;
import com.transportlogistics.app.trip.domain.model.TripHistoryEntry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.security.Principal;

@RestController
class TripController {
    private final TripUseCase trips;

    TripController(TripUseCase t) {
        trips = t;
    }

    @PostMapping("/trips")
    ResponseEntity<Trip> create(@Valid @RequestBody TripRequest r) {
        var command = new TripUseCase.CreateCommand(r.customerId(), r.departmentId(), r.projectId(), r.routeId(),
                r.priority(), r.originLocationId(), r.destinationLocationId(), r.requestedStartTime(),
                r.requestedEndTime(), r.requiredVehicleTypeId(), r.requiredCapacityKg(), r.cargoDescription(),
                r.passengerCount(), r.customerInstructions(), r.notes());
        return ResponseEntity.status(201).body(trips.create(command));
    }

    @GetMapping("/trips")
    List<Trip> list() {
        return trips.list();
    }

    @GetMapping("/trips/{id}")
    Trip get(@PathVariable UUID id) {
        return trips.get(id);
    }

    @PutMapping("/trips/{id}")
    Trip update(@PathVariable UUID id, @Valid @RequestBody TripRequest r) {
        var old = trips.get(id);
        return trips.update(id, new Trip(id, old.tripNumber(), r.customerId(), r.departmentId(), r.projectId(), r.routeId(), r.priority() == null ? old.priority() : r.priority(), old.status(), r.originLocationId(), r.destinationLocationId(), r.requestedStartTime(), r.requestedEndTime(), r.requiredVehicleTypeId(), r.requiredCapacityKg(), r.cargoDescription(), r.passengerCount(), r.customerInstructions(), r.notes(), old.vehicleId(), old.driverId(), old.actualStartTime(), old.actualEndTime(), old.startOdometerKm(), old.endOdometerKm(), old.completionRemarks(), old.createdAt(), old.updatedAt()));
    }

    @PostMapping("/trips/{id}/submit")
    Trip submit(@PathVariable UUID id, Principal principal) {
        return trips.transition(id, new TripCommand.Submit(), actor(principal));
    }

    @PostMapping("/trips/{id}/approve")
    Trip approve(@PathVariable UUID id, @RequestBody(required = false) Map<String, Object> body,
                 Principal principal) {
        return trips.transition(id, new TripCommand.Approve(), actor(principal));
    }

    @PostMapping("/trips/{id}/reject")
    Trip reject(@PathVariable UUID id, @RequestBody(required = false) ReasonRequest r, Principal principal) {
        return trips.transition(id, new TripCommand.Reject(r == null ? null : r.reason()), actor(principal));
    }

    @PostMapping("/trips/{id}/dispatch")
    Trip dispatch(@PathVariable UUID id, @RequestBody(required = false) DispatchRequest request,
                  Principal principal) {
        return trips.dispatch(id, principal == null ? "system" : principal.getName(),
                request == null ? null : request.remarks());
    }

    @PostMapping("/trips/{id}/start")
    Trip start(@PathVariable UUID id, @RequestBody(required = false) StartRequest r, Principal principal) {
        return trips.transition(id, new TripCommand.Start(r == null ? null : r.startOdometerKm()), actor(principal));
    }

    @PostMapping("/trips/{id}/complete")
    Trip complete(@PathVariable UUID id, @RequestBody(required = false) CompleteRequest r, Principal principal) {
        return trips.transition(id, new TripCommand.Complete(r == null ? null : r.endOdometerKm(),
                r == null ? null : r.completionRemarks()), actor(principal));
    }

    @PostMapping("/trips/{id}/close")
    Trip close(@PathVariable UUID id, Principal principal) {
        return trips.transition(id, new TripCommand.Close(), actor(principal));
    }

    @PostMapping("/trips/{id}/cancel")
    Trip cancel(@PathVariable UUID id, @RequestBody(required = false) ReasonRequest r, Principal principal) {
        return trips.transition(id, new TripCommand.Cancel(r == null ? null : r.reason()), actor(principal));
    }

    @PostMapping("/trips/{id}/assign-vehicle")
    Trip assignVehicle(@PathVariable UUID id, @Valid @RequestBody VehicleAssignmentRequest request,
                       Principal principal) {
        return trips.assignVehicle(id, request.vehicleId(), principal == null ? "system" : principal.getName());
    }

    @PostMapping("/trips/{id}/unassign-vehicle")
    Trip unassignVehicle(@PathVariable UUID id, Principal principal) {
        return trips.unassignVehicle(id, actor(principal));
    }

    @PostMapping("/trips/{id}/assign-driver")
    Trip assignDriver(@PathVariable UUID id, @Valid @RequestBody DriverAssignmentRequest request,
                      Principal principal) {
        return trips.assignDriver(id, request.driverId(), request.requiredLicenseClass(),
                principal == null ? "system" : principal.getName());
    }

    @PostMapping("/trips/{id}/assign-route")
    Trip assignRoute(@PathVariable UUID id, @Valid @RequestBody RouteAssignmentRequest request,
                     Principal principal) {
        return trips.assignRoute(id, request.routeId(), actor(principal));
    }

    @PostMapping("/trips/{id}/unassign-driver")
    Trip unassignDriver(@PathVariable UUID id, Principal principal) {
        return trips.unassignDriver(id, actor(principal));
    }

    @GetMapping("/trips/{id}/status-history")
    List<TripHistoryEntry> history(@PathVariable UUID id) {
        return trips.history(id);
    }

    record TripRequest(UUID customerId, UUID departmentId, UUID projectId, UUID routeId, String priority,
                       @NotNull UUID originLocationId, @NotNull UUID destinationLocationId,
                       @NotNull OffsetDateTime requestedStartTime, @NotNull OffsetDateTime requestedEndTime,
                       UUID requiredVehicleTypeId, Double requiredCapacityKg, String cargoDescription,
                       Integer passengerCount, String customerInstructions, String notes) {
    }

    record VehicleAssignmentRequest(@NotNull UUID vehicleId) {
    }

    record DriverAssignmentRequest(@NotNull UUID driverId, @NotBlank String requiredLicenseClass) {
    }

    record RouteAssignmentRequest(@NotNull UUID routeId) {
    }

    record ReasonRequest(String reason) {
    }

    record StartRequest(Double startOdometerKm) {
    }

    record CompleteRequest(Double endOdometerKm, String completionRemarks) {
    }

    record DispatchRequest(String remarks) {
    }

    private String actor(Principal principal) {
        return principal == null ? null : principal.getName();
    }
}
