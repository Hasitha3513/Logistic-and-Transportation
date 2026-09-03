package com.transportlogistics.app.notification.infrastructure.adapters.out.organization;

import com.transportlogistics.app.notification.application.ports.out.CustomerNotificationContactPort;
import com.transportlogistics.app.organization.CustomerNotificationContactLookup;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class OrganizationCustomerNotificationContactAdapter implements CustomerNotificationContactPort {
    private final CustomerNotificationContactLookup customers;

    public OrganizationCustomerNotificationContactAdapter(CustomerNotificationContactLookup customers) {
        this.customers = customers;
    }

    @Override
    public Optional<CustomerContact> find(UUID customerId) {
        return customers.find(customerId).map(value -> new CustomerContact(
                value.customerId(), value.active(), value.displayName(), value.phone(), value.email()));
    }
}
