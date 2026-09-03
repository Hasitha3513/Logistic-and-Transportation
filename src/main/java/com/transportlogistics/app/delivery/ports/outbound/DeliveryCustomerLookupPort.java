package com.transportlogistics.app.delivery.ports.outbound;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryCustomerLookupPort {
    Optional<CustomerReference> findCustomer(UUID customerId);

    record CustomerReference(UUID customerId, String code, String name, boolean active) {}
}
