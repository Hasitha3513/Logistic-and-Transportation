package com.transportlogistics.app.fuel.domain.model;

import java.util.UUID;

public record FuelStation(UUID id, String code, String name, FuelStationType stationType, boolean active,
                          UUID vendorId, UUID locationId) {
    public boolean isInternal() {
        return stationType == FuelStationType.INTERNAL;
    }

    public boolean isExternal() {
        return stationType == FuelStationType.EXTERNAL;
    }
}
