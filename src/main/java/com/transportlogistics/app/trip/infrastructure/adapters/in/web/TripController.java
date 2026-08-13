package com.transportlogistics.app.trip.infrastructure.adapters.in.web;

import com.transportlogistics.app.trip.application.ports.in.TripUseCase;
import com.transportlogistics.app.trip.domain.model.Trip;
import com.transportlogistics.app.trip.domain.model.TripCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
class TripController {
    private final TripUseCase trips;

    TripController(TripUseCase t) {
        trips = t;
    }

    @PostMapping("/trips")
    ResponseEntity<Trip> create(@Valid @RequestBody TripRequest r) {
        var now = OffsetDateTime.now();
        var t = new Trip(UUID.randomUUID(), "TRIP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(), r.customerId(), r.departmentId(), r.projectId(), r.routeId(), r.priority() == null ? "NORMAL" : r.priority(), "DRAFT", r.originLocationId(), r.destinationLocationId(), r.requestedStartTime(), r.requestedEndTime(), r.requiredVehicleTypeId(), r.requiredCapacityKg(), r.cargoDescription(), r.passengerCount(), r.customerInstructions(), r.notes(), null, null, null, null, null, null, null, now, now);
        return ResponseEntity.status(201).body(trips.create(t));
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
        return trips.update(id, new Trip(id, old.tripNumber(), r.customerId(), r.departmentId(), r.projectId(), r.routeId(), r.priority() == null ? old.priority() : r.priority(), old.status(), r.originLocationId(), r.destinationLocationId(), r.requestedStartTime(), r.requestedEndTime(), r.requiredVehicleTypeId(), r.requiredCapacityKg(), r.cargoDescription(), r.passengerCount(), r.customerInstructions(), r.notes(), old.vehicleId(), old.driverId(), old.actualStartTime(), old.actualEndTime(), old.startOdometerKm(), old.endOdometerKm(), old.completionRemarks(), old.createdAt(), OffsetDateTime.now()));
    }

    @PostMapping("/trips/{id}/submit")
    Trip submit(@PathVariable UUID id) {
        return trips.transition(id, new TripCommand.Submit());
    }

    @PostMapping("/trips/{id}/approve")
    Trip approve(@PathVariable UUID id, @RequestBody(required = false) Map<String, Object> body) {
        return trips.transition(id, new TripCommand.Approve());
    }

    @PostMapping("/trips/{id}/reject")
    Trip reject(@PathVariable UUID id, @RequestBody(required = false) ReasonRequest r) {
        return trips.transition(id, new TripCommand.Reject(r == null ? null : r.reason()));
    }

    @PostMapping("/trips/{id}/dispatch")
    Trip dispatch(@PathVariable UUID id, @RequestBody(required = false) Map<String, Object> body) {
        return trips.transition(id, new TripCommand.Dispatch());
    }

    @PostMapping("/trips/{id}/start")
    Trip start(@PathVariable UUID id, @RequestBody(required = false) StartRequest r) {
        return trips.transition(id, new TripCommand.Start(r == null ? null : r.startOdometerKm()));
    }

    @PostMapping("/trips/{id}/complete")
    Trip complete(@PathVariable UUID id, @RequestBody(required = false) CompleteRequest r) {
        return trips.transition(id, new TripCommand.Complete(r == null ? null : r.endOdometerKm(), r == null ? null : r.completionRemarks()));
    }

    @PostMapping("/trips/{id}/close")
    Trip close(@PathVariable UUID id) {
        return trips.transition(id, new TripCommand.Close());
    }

    @PostMapping("/trips/{id}/cancel")
    Trip cancel(@PathVariable UUID id, @RequestBody(required = false) ReasonRequest r) {
        return trips.transition(id, new TripCommand.Cancel(r == null ? null : r.reason()));
    }

    @PostMapping("/trips/{id}/assign-vehicle")
    Trip assignVehicle(@PathVariable UUID id, @RequestBody Map<String, UUID> r) {
        return trips.assignVehicle(id, r.values().stream().findFirst().orElseThrow());
    }

    @PostMapping("/trips/{id}/unassign-vehicle")
    Trip unassignVehicle(@PathVariable UUID id) {
        return trips.unassignVehicle(id);
    }

    @PostMapping("/trips/{id}/assign-driver")
    Trip assignDriver(@PathVariable UUID id, @Valid @RequestBody DriverAssignmentRequest request) {
        return trips.assignDriver(id, request.driverId(), request.requiredLicenseClass());
    }

    @PostMapping("/trips/{id}/unassign-driver")
    Trip unassignDriver(@PathVariable UUID id) {
        return trips.unassignDriver(id);
    }

    @GetMapping("/trips/{id}/status-history")
    List<Map<String, Object>> history(@PathVariable UUID id) {
        var t = trips.get(id);
        return List.of(Map.of("tripId", id, "status", t.status(), "at", t.updatedAt()));
    }

    record TripRequest(UUID customerId, UUID departmentId, UUID projectId, UUID routeId, String priority,
                       @NotNull UUID originLocationId, @NotNull UUID destinationLocationId,
                       @NotNull OffsetDateTime requestedStartTime, @NotNull OffsetDateTime requestedEndTime,
                       UUID requiredVehicleTypeId, Double requiredCapacityKg, String cargoDescription,
                       Integer passengerCount, String customerInstructions, String notes) {
    }

    record AssignmentRequest(@NotNull UUID id) {
    }

    record DriverAssignmentRequest(@NotNull UUID driverId, String requiredLicenseClass) {
    }

    record ReasonRequest(String reason) {
    }

    record StartRequest(Double startOdometerKm) {
    }

    record CompleteRequest(Double endOdometerKm, String completionRemarks) {
    }
}
