package com.transportlogistics.app.fuel.application.ports.in;

import com.transportlogistics.app.fuel.TripFuelCost;

import java.util.UUID;

public interface TripFuelCostUseCase {
    TripFuelCost getTripFuelCost(UUID tripId);
}