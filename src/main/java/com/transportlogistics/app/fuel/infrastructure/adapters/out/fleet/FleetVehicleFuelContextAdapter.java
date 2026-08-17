package com.transportlogistics.app.fuel.infrastructure.adapters.out.fleet;

import com.transportlogistics.app.fleet.VehicleFuelContextLookup;
import com.transportlogistics.app.fuel.application.ports.out.VehicleFuelContextPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class FleetVehicleFuelContextAdapter implements VehicleFuelContextPort {
    private final VehicleFuelContextLookup vehicles;

    @Override
    public Optional<VehicleContext> find(UUID vehicleId) {
        return vehicles.find(vehicleId).map(vehicle -> new VehicleContext(vehicle.vehicleId(),
                vehicle.registrationNumber(), vehicle.active(), vehicle.operationalStatus(),
                vehicle.currentOdometerKm(), vehicle.engineHours()));
    }
}
