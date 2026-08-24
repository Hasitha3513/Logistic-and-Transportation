package com.transportlogistics.app.fuel.application.ports.out;

import java.util.Optional;
import java.util.UUID;

public interface FuelVendorPort {
    Optional<Vendor> find(UUID vendorId);

    record Vendor(UUID id, String code, String name, boolean active) {
    }
}
