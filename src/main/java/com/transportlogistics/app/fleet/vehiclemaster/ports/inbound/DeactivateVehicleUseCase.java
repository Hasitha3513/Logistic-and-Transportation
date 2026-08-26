package com.transportlogistics.app.fleet.vehiclemaster.ports.inbound;

import java.util.UUID;

public interface DeactivateVehicleUseCase {
    void deactivate(UUID id);
}
