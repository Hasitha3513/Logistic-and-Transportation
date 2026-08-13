package com.transportlogistics.app.trip.application.ports.in;

import com.transportlogistics.app.trip.domain.model.Trip;
import com.transportlogistics.app.trip.domain.model.TripCommand;
import com.transportlogistics.app.trip.domain.model.TripHistoryEntry;

import java.util.List;
import java.util.UUID;

public interface TripUseCase {
    Trip create(Trip t);

    Trip get(UUID id);

    List<Trip> list();

    Trip update(UUID id, Trip t);

    Trip transition(UUID id, TripCommand cmd);

    Trip assignVehicle(UUID id, UUID vehicleId, String actor);

    Trip assignDriver(UUID id, UUID driverId, String requiredLicenseClass, String actor);

    Trip unassignVehicle(UUID id);

    Trip unassignDriver(UUID id);

    List<TripHistoryEntry> history(UUID id);
}
