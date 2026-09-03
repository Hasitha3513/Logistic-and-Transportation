package com.transportlogistics.app.delivery.adapters.outbound.organization;

import com.transportlogistics.app.delivery.ports.outbound.DeliveryLocationLookupPort;
import com.transportlogistics.app.organization.LocationLookup;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class OrganizationDeliveryLocationAdapter implements DeliveryLocationLookupPort {
    private final LocationLookup locations;
    OrganizationDeliveryLocationAdapter(LocationLookup locations) { this.locations = locations; }
    @Override public Optional<LocationReference> findLocation(UUID id) {
        return locations.find(id).map(value -> new LocationReference(value.id(), value.code(), value.name(), value.address(), value.latitude(), value.longitude(), value.active()));
    }
}
