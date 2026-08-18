package com.transportlogistics.app.fuel.domain.model;

/**
 * Indicates the overall status of a fuel cost calculation for a trip.
 */
public enum TripFuelCostCalculationStatus {
    /**
     * All required data (fuel issues with unit price and trip distance) were available and the calculation is complete.
     */
    COMPLETE,
    /**
     * Some fuel issues lack a unit price or the distance is partially unavailable, resulting in a partial calculation.
     */
    PARTIAL,
    /**
     * Required data (e.g., distance) is unavailable, so the calculation cannot be performed.
     */
    UNAVAILABLE
}


