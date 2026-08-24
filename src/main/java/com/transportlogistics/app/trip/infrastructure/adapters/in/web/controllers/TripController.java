package com.transportlogistics.app.trip.infrastructure.adapters.in.web.controllers;

import com.transportlogistics.app.shared.utils.PrincipalUtils;
import com.transportlogistics.app.trip.application.ports.in.TripOperationalEventUseCase;
import com.transportlogistics.app.trip.application.ports.in.TripUseCase;
import com.transportlogistics.app.trip.domain.model.Trip;
import com.transportlogistics.app.trip.domain.model.TripCommand;
import com.transportlogistics.app.trip.infrastructure.adapters.in.web.dto.request.*;
import com.transportlogistics.app.trip.infrastructure.adapters.in.web.dto.response.TripHistoryResponse;
import com.transportlogistics.app.trip.infrastructure.adapters.in.web.dto.response.TripOperationalEventResponse;
import com.transportlogistics.app.trip.infrastructure.adapters.in.web.dto.response.TripResponse;
import com.transportlogistics.app.trip.infrastructure.adapters.in.web.mappers.TripWebMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class TripController {

    private final TripUseCase trips;
    private final TripOperationalEventUseCase operationalEvents;
    private final TripWebMapper mapper;

    @Autowired
    public TripController(TripUseCase trips, TripOperationalEventUseCase operationalEvents, TripWebMapper mapper) {
        this.trips = trips;
        this.operationalEvents = operationalEvents;
        this.mapper = mapper;
    }

    public TripController(TripUseCase trips, TripWebMapper mapper) {
        this(trips, null, mapper);
    }

    @PostMapping("/trips")
    ResponseEntity<TripResponse> create(@Valid @RequestBody TripRequest r) {
        var command = new TripUseCase.CreateCommand(r.customerId(), r.departmentId(), r.projectId(), r.routeId(),
                r.priority(), r.originLocationId(), r.destinationLocationId(), r.requestedStartTime(),
                r.requestedEndTime(), r.requiredVehicleTypeId(), r.requiredCapacityKg(), r.cargoDescription(),
                r.passengerCount(), r.customerInstructions(), r.notes());
        return ResponseEntity.status(201).body(mapper.toResponse(trips.create(command)));
    }

    @GetMapping("/trips")
    List<TripResponse> list() {
        return mapper.toTripResponseList(trips.list());
    }

    @GetMapping("/trips/{id}")
    TripResponse get(@PathVariable UUID id) {
        return mapper.toResponse(trips.get(id));
    }

    @PutMapping("/trips/{id}")
    TripResponse update(@PathVariable UUID id, @Valid @RequestBody TripRequest r) {
        var old = trips.get(id);
        var updated = trips.update(id, new Trip(id, old.tripNumber(), r.customerId(), r.departmentId(), r.projectId(),
                r.routeId(), r.priority() == null ? old.priority() : r.priority(), old.status(), r.originLocationId(),
                r.destinationLocationId(), r.requestedStartTime(), r.requestedEndTime(), r.requiredVehicleTypeId(),
                r.requiredCapacityKg(), r.cargoDescription(), r.passengerCount(), r.customerInstructions(), r.notes(),
                old.vehicleId(), old.driverId(), old.actualStartTime(), old.actualEndTime(), old.startOdometerKm(),
                old.endOdometerKm(), old.completionRemarks(), old.createdAt(), old.updatedAt()));
        return mapper.toResponse(updated);
    }

    @PostMapping("/trips/{id}/submit")
    TripResponse submit(@PathVariable UUID id, Principal principal) {
        return mapper.toResponse(trips.transition(id, new TripCommand.Submit(), actor(principal)));
    }

    @PostMapping("/trips/{id}/approve")
    TripResponse approve(@PathVariable UUID id, @RequestBody(required = false) Map<String, Object> body,
                         Principal principal) {
        return mapper.toResponse(trips.transition(id, new TripCommand.Approve(), actor(principal)));
    }

    @PostMapping("/trips/{id}/reject")
    TripResponse reject(@PathVariable UUID id, @RequestBody(required = false) ReasonRequest r, Principal principal) {
        return mapper.toResponse(trips.transition(id, new TripCommand.Reject(r == null ? null : r.reason()), actor(principal)));
    }

    @PostMapping("/trips/{id}/dispatch")
    TripResponse dispatch(@PathVariable UUID id, @RequestBody(required = false) DispatchRequest request,
                          Principal principal) {
        return mapper.toResponse(trips.dispatch(id, actor(principal),
                request == null ? null : request.remarks()));
    }

    @PostMapping("/trips/{id}/start")
    TripResponse start(@PathVariable UUID id, @RequestBody(required = false) StartRequest r, Principal principal) {
        return mapper.toResponse(trips.transition(id, new TripCommand.Start(r == null ? null : r.startOdometerKm()), actor(principal)));
    }

    @PostMapping("/trips/{id}/complete")
    TripResponse complete(@PathVariable UUID id, @RequestBody(required = false) CompleteRequest r, Principal principal) {
        return mapper.toResponse(trips.transition(id, new TripCommand.Complete(r == null ? null : r.endOdometerKm(),
                r == null ? null : r.completionRemarks()), actor(principal)));
    }

    @PostMapping("/trips/{id}/close")
    TripResponse close(@PathVariable UUID id, Principal principal) {
        return mapper.toResponse(trips.transition(id, new TripCommand.Close(), actor(principal)));
    }

    @PostMapping("/trips/{id}/cancel")
    TripResponse cancel(@PathVariable UUID id, @RequestBody(required = false) ReasonRequest r, Principal principal) {
        return mapper.toResponse(trips.transition(id, new TripCommand.Cancel(r == null ? null : r.reason()), actor(principal)));
    }

    @PostMapping("/trips/{id}/assign-vehicle")
    TripResponse assignVehicle(@PathVariable UUID id, @Valid @RequestBody VehicleAssignmentRequest request,
                               Principal principal) {
        return mapper.toResponse(trips.assignVehicle(id, request.vehicleId(), actor(principal)));
    }

    @PostMapping("/trips/{id}/unassign-vehicle")
    TripResponse unassignVehicle(@PathVariable UUID id, Principal principal) {
        return mapper.toResponse(trips.unassignVehicle(id, actor(principal)));
    }

    @PostMapping("/trips/{id}/assign-driver")
    TripResponse assignDriver(@PathVariable UUID id, @Valid @RequestBody DriverAssignmentRequest request,
                              Principal principal) {
        return mapper.toResponse(trips.assignDriver(id, request.driverId(), request.requiredLicenseClass(),
                actor(principal)));
    }

    @PostMapping("/trips/{id}/assign-route")
    TripResponse assignRoute(@PathVariable UUID id, @Valid @RequestBody RouteAssignmentRequest request,
                             Principal principal) {
        return mapper.toResponse(trips.assignRoute(id, request.routeId(), actor(principal)));
    }

    @PostMapping("/trips/{id}/unassign-driver")
    TripResponse unassignDriver(@PathVariable UUID id, Principal principal) {
        return mapper.toResponse(trips.unassignDriver(id, actor(principal)));
    }

    @GetMapping("/trips/{id}/status-history")
    List<TripHistoryResponse> history(@PathVariable UUID id) {
        return mapper.toTripHistoryResponseList(trips.history(id));
    }

    @GetMapping("/trips/{id}/operational-events")
    List<TripOperationalEventResponse> listOperationalEvents(@PathVariable UUID id) {
        return mapper.toTripOperationalEventResponseList(operationalEvents.getTripEvents(id));
    }

    @GetMapping("/trips/{id}/operational-events/{eventId}")
    TripOperationalEventResponse getOperationalEvent(@PathVariable UUID id, @PathVariable UUID eventId) {
        return mapper.toResponse(operationalEvents.getEvent(id, eventId));
    }

    @PostMapping("/trips/{id}/checkpoints")
    ResponseEntity<TripOperationalEventResponse> recordCheckpoint(
            @PathVariable UUID id,
            @Valid @RequestBody TripCheckpointRequest request,
            Principal principal
    ) {
        var command = new TripOperationalEventUseCase.RecordCheckpointCommand(
                request.checkpointType(),
                request.occurredAt(),
                request.locationId(),
                request.locationDescription(),
                request.remarks()
        );
        var created = operationalEvents.recordCheckpoint(id, command, actor(principal));
        return ResponseEntity.status(201).body(mapper.toResponse(created));
    }

    @PostMapping("/trips/{id}/delays")
    ResponseEntity<TripOperationalEventResponse> recordDelay(
            @PathVariable UUID id,
            @Valid @RequestBody TripDelayRequest request,
            Principal principal
    ) {
        var command = new TripOperationalEventUseCase.RecordDelayCommand(
                request.delayMinutes(),
                request.reason(),
                request.occurredAt(),
                request.locationId(),
                request.locationDescription(),
                request.remarks()
        );
        var created = operationalEvents.recordDelay(id, command, actor(principal));
        return ResponseEntity.status(201).body(mapper.toResponse(created));
    }

    @PostMapping("/trips/{id}/incidents")
    ResponseEntity<TripOperationalEventResponse> recordIncident(
            @PathVariable UUID id,
            @Valid @RequestBody TripIncidentRequest request,
            Principal principal
    ) {
        var command = new TripOperationalEventUseCase.RecordIncidentCommand(
                request.incidentSeverity(),
                request.description(),
                request.occurredAt(),
                request.locationId(),
                request.locationDescription(),
                request.remarks()
        );
        var created = operationalEvents.recordIncident(id, command, actor(principal));
        return ResponseEntity.status(201).body(mapper.toResponse(created));
    }

    private String actor(Principal principal) {
        return PrincipalUtils.resolveActorName(principal, "system");
    }
}
