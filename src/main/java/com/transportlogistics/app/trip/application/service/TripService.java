package com.transportlogistics.app.trip.application.service;

import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.trip.application.ports.in.TripUseCase;
import com.transportlogistics.app.trip.application.ports.out.TripRepository;
import com.transportlogistics.app.trip.domain.model.Trip;
import com.transportlogistics.app.trip.domain.model.TripCommand;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class TripService implements TripUseCase {
    private final TripRepository repo;

    public TripService(TripRepository r) {
        repo = r;
    }

    public Trip create(Trip t) {
        return repo.save(t);
    }

    public Trip get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Trip not found: " + id));
    }

    public List<Trip> list() {
        return repo.findAll();
    }

    public Trip update(UUID id, Trip t) {
        get(id);
        return repo.save(t);
    }

    public Trip assignVehicle(UUID id, UUID v) {
        var t = get(id);
        return repo.save(copy(t, t.status(), v, t.driverId(), t.actualStartTime(), t.actualEndTime(), t.startOdometerKm(), t.endOdometerKm(), t.completionRemarks()));
    }

    public Trip assignDriver(UUID id, UUID d) {
        var t = get(id);
        return repo.save(copy(t, t.status(), t.vehicleId(), d, t.actualStartTime(), t.actualEndTime(), t.startOdometerKm(), t.endOdometerKm(), t.completionRemarks()));
    }

    public Trip unassignVehicle(UUID id) {
        return assignVehicle(id, null);
    }

    public Trip unassignDriver(UUID id) {
        return assignDriver(id, null);
    }

    public Trip transition(UUID id, TripCommand c) {
        var t = get(id);
        var now = OffsetDateTime.now();
        return switch (c) {
            case TripCommand.Submit x ->
                    repo.save(copy(t, "SUBMITTED", t.vehicleId(), t.driverId(), t.actualStartTime(), t.actualEndTime(), t.startOdometerKm(), t.endOdometerKm(), t.completionRemarks()));
            case TripCommand.Approve x ->
                    repo.save(copy(t, "APPROVED", t.vehicleId(), t.driverId(), t.actualStartTime(), t.actualEndTime(), t.startOdometerKm(), t.endOdometerKm(), t.completionRemarks()));
            case TripCommand.Reject x ->
                    repo.save(copy(t, "REJECTED", t.vehicleId(), t.driverId(), t.actualStartTime(), t.actualEndTime(), t.startOdometerKm(), t.endOdometerKm(), x.reason()));
            case TripCommand.Dispatch x ->
                    repo.save(copy(t, "DISPATCHED", t.vehicleId(), t.driverId(), t.actualStartTime(), t.actualEndTime(), t.startOdometerKm(), t.endOdometerKm(), t.completionRemarks()));
            case TripCommand.Start x ->
                    repo.save(copy(t, "IN_PROGRESS", t.vehicleId(), t.driverId(), now, t.actualEndTime(), x.odometerKm(), t.endOdometerKm(), t.completionRemarks()));
            case TripCommand.Complete x ->
                    repo.save(copy(t, "COMPLETED", t.vehicleId(), t.driverId(), t.actualStartTime(), now, t.startOdometerKm(), x.odometerKm(), x.remarks()));
            case TripCommand.Close x ->
                    repo.save(copy(t, "CLOSED", t.vehicleId(), t.driverId(), t.actualStartTime(), t.actualEndTime(), t.startOdometerKm(), t.endOdometerKm(), t.completionRemarks()));
            case TripCommand.Cancel x ->
                    repo.save(copy(t, "CANCELLED", t.vehicleId(), t.driverId(), t.actualStartTime(), t.actualEndTime(), t.startOdometerKm(), t.endOdometerKm(), x.reason()));
        };
    }

    private Trip copy(Trip t, String s, UUID v, UUID d, OffsetDateTime as, OffsetDateTime ae, Double so, Double eo, String remarks) {
        return new Trip(t.id(), t.tripNumber(), t.customerId(), t.departmentId(), t.projectId(), t.routeId(), t.priority(), s, t.originLocationId(), t.destinationLocationId(), t.requestedStartTime(), t.requestedEndTime(), t.requiredVehicleTypeId(), t.requiredCapacityKg(), t.cargoDescription(), t.passengerCount(), t.customerInstructions(), t.notes(), v, d, as, ae, so, eo, remarks, t.createdAt(), OffsetDateTime.now());
    }
}