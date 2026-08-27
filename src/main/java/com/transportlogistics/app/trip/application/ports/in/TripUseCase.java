package com.transportlogistics.app.trip.application.ports.in;

import com.transportlogistics.app.trip.domain.model.Trip;
import com.transportlogistics.app.trip.domain.model.TripCommand;
import com.transportlogistics.app.trip.domain.model.TripHistoryEntry;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface TripUseCase {
    Trip create(CreateCommand command);

    Trip get(UUID id);

    List<Trip> list();

    Trip update(UUID id, Trip t);

    Trip transition(UUID id, TripCommand cmd, String actor);

    default Trip transition(UUID id, TripCommand cmd) {
        return transition(id, cmd, "system");
    }

    Trip dispatch(UUID id, String actor, String remarks);

    Trip assignVehicle(UUID id, UUID vehicleId, String actor);

    Trip assignDriver(UUID id, UUID driverId, String requiredLicenseClass, String actor);

    Trip assignRoute(UUID id, UUID routeId, String actor);

    Trip unassignVehicle(UUID id, String actor);

    default Trip unassignVehicle(UUID id) {
        return unassignVehicle(id, "system");
    }

    Trip unassignDriver(UUID id, String actor);

    default Trip unassignDriver(UUID id) {
        return unassignDriver(id, "system");
    }

    List<TripHistoryEntry> history(UUID id);

    record CreateCommand(UUID customerId, UUID departmentId, UUID projectId, UUID routeId, String priority,
                         UUID originLocationId, UUID destinationLocationId, OffsetDateTime requestedStartTime,
                         OffsetDateTime requestedEndTime, UUID requiredVehicleTypeId, Double requiredCapacityKg,
                         String cargoDescription, Integer passengerCount, String customerInstructions, String notes) {
    }
}
