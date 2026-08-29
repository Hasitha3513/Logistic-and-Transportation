package com.transportlogistics.app.delivery.adapters.outbound.organization;

import com.transportlogistics.app.delivery.ports.outbound.DeliveryCustomerLookupPort;
import com.transportlogistics.app.organization.CustomerLookup;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class OrganizationDeliveryCustomerAdapter implements DeliveryCustomerLookupPort {
    private final CustomerLookup customers;
    OrganizationDeliveryCustomerAdapter(CustomerLookup customers) { this.customers = customers; }
    @Override public Optional<CustomerReference> findCustomer(UUID id) {
        return customers.find(id).map(value -> new CustomerReference(value.id(), value.code(), value.name(), value.active()));
    }
}
