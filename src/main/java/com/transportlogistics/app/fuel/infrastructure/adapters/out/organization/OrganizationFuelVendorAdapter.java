package com.transportlogistics.app.fuel.infrastructure.adapters.out.organization;

import com.transportlogistics.app.fuel.application.ports.out.FuelVendorPort;
import com.transportlogistics.app.organization.VendorLookup;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class OrganizationFuelVendorAdapter implements FuelVendorPort {
    private final VendorLookup vendors;

    OrganizationFuelVendorAdapter(VendorLookup vendors) { this.vendors = vendors; }

    @Override
    public Optional<Vendor> find(UUID vendorId) {
        return vendors.find(vendorId).map(v -> new Vendor(v.id(), v.code(), v.name(), v.active()));
    }
}
