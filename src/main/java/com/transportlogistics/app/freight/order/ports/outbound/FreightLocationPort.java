package com.transportlogistics.app.freight.order.ports.outbound;

import java.util.Optional;
import java.util.UUID;

public interface FreightLocationPort {
    Optional<LocationReference> find(UUID locationId);
    record LocationReference(UUID id, String code, String name, boolean active) { }
}
