package com.transportlogistics.app.tracking.adapters.outbound.organization;

import com.transportlogistics.app.organization.LocationLookup;
import com.transportlogistics.app.tracking.ports.outbound.GeofenceLocationLookupPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class OrganizationGeofenceLocationAdapter implements GeofenceLocationLookupPort {
    private final LocationLookup locations;

    OrganizationGeofenceLocationAdapter(LocationLookup locations) {
        this.locations = locations;
    }

    @Override
    public Optional<LocationReference> findActive(UUID tenantId, UUID locationId) {
        return locations.find(tenantId, locationId)
                .filter(LocationLookup.LocationReference::active)
                .map(value -> new LocationReference(value.id(), value.active()));
    }
}
