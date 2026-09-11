package com.transportlogistics.app.fuel.infrastructure.adapters.out;

import com.transportlogistics.app.fleet.FleetFuelPerformanceLookup;
import com.transportlogistics.app.fuel.application.ports.out.FuelPerformanceContextPort;
import com.transportlogistics.app.trip.TripFuelPerformanceLookup;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class FuelPerformanceContextAdapter implements FuelPerformanceContextPort {
    private final FleetFuelPerformanceLookup fleet;
    private final TripFuelPerformanceLookup trips;

    public FuelPerformanceContextAdapter(FleetFuelPerformanceLookup fleet, TripFuelPerformanceLookup trips) {
        this.fleet = fleet;
        this.trips = trips;
    }

    @Override
    public Map<UUID, VehicleContext> vehicles(Set<UUID> ids) {
        return fleet.vehicles(ids).values().stream().map(value -> new VehicleContext(value.vehicleId(),
                        value.registrationNumber(), value.vehicleTypeId(), value.active()))
                .collect(Collectors.toMap(VehicleContext::id, Function.identity()));
    }

    @Override
    public Map<UUID, DriverContext> drivers(Set<UUID> ids) {
        return fleet.drivers(ids).values().stream().map(value ->
                        new DriverContext(value.driverId(), value.displayLabel(), value.active()))
                .collect(Collectors.toMap(DriverContext::id, Function.identity()));
    }

    @Override
    public Map<UUID, TripContext> trips(Set<UUID> ids) {
        return trips.findAll(ids).values().stream().map(value ->
                        new TripContext(value.tripId(), value.vehicleId(), value.driverId()))
                .collect(Collectors.toMap(TripContext::id, Function.identity()));
    }
}
