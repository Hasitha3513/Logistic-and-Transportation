package com.transportlogistics.app.fuel.infrastructure.adapters.out.fleet;

import com.transportlogistics.app.fleet.TripDistanceSummary;
import com.transportlogistics.app.fleet.VehicleMileageQuery;
import com.transportlogistics.app.fuel.application.ports.out.TripDistancePort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class FleetFuelTripDistanceAdapter implements TripDistancePort {

    private final VehicleMileageQuery mileageQuery;

    public FleetFuelTripDistanceAdapter(VehicleMileageQuery mileageQuery) {
        this.mileageQuery = mileageQuery;
    }

    @Override
    public TripDistanceSummary getTripDistance(UUID tripId) {
        return mileageQuery.calculateTripDistance(tripId);
    }
}