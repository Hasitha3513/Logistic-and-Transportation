package com.transportlogistics.app.freight.order.adapters.outbound.organization;

import com.transportlogistics.app.freight.order.ports.outbound.FreightLocationPort;
import com.transportlogistics.app.organization.LocationLookup;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class OrganizationFreightLocationAdapter implements FreightLocationPort {
    private final LocationLookup locations;
    OrganizationFreightLocationAdapter(LocationLookup locations) { this.locations = locations; }
    @Override public Optional<LocationReference> find(UUID locationId) {
        return locations.find(locationId).map(value -> new LocationReference(value.id(), value.code(), value.name(), value.active()));
    }
}
