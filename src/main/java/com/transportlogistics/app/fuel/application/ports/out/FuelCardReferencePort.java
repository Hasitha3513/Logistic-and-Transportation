package com.transportlogistics.app.fuel.application.ports.out;

import java.util.UUID;

public interface FuelCardReferencePort {
    boolean providerActive(UUID providerId);
    boolean vehicleActive(UUID vehicleId);
    boolean driverActive(UUID driverId);
    boolean tripExists(UUID tripId);
    boolean purchaseExists(UUID purchaseId);
}
