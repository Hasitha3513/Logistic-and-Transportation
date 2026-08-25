package com.transportlogistics.app.freight.order.ports.outbound;

import java.util.Optional;
import java.util.UUID;

public interface FreightCustomerPort {
    Optional<CustomerReference> find(UUID customerId);
    record CustomerReference(UUID id, String code, String name, boolean active) { }
}
