package com.transportlogistics.app.fuel.application.ports.out;

import com.transportlogistics.app.fleet.TripDistanceSummary;

import java.util.UUID;

public interface TripDistancePort {
    TripDistanceSummary getTripDistance(UUID tripId);
}