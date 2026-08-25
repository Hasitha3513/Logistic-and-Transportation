package com.transportlogistics.app.freight.order.adapters.outbound.organization;

import com.transportlogistics.app.freight.order.ports.outbound.FreightCustomerPort;
import com.transportlogistics.app.organization.CustomerLookup;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class OrganizationFreightCustomerAdapter implements FreightCustomerPort {
    private final CustomerLookup customers;
    OrganizationFreightCustomerAdapter(CustomerLookup customers) { this.customers = customers; }
    @Override public Optional<CustomerReference> find(UUID customerId) {
        return customers.find(customerId).map(value -> new CustomerReference(value.id(), value.code(), value.name(), value.active()));
    }
}
