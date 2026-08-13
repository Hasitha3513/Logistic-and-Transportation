package com.transportlogistics.app.organization.application.ports.in;

import com.transportlogistics.app.organization.domain.model.Customer;

import java.util.List;
import java.util.UUID;

public interface CustomerUseCase {
    Customer create(Customer value);

    Customer get(UUID id);

    List<Customer> list();

    Customer update(UUID id, Customer value);

    void deactivate(UUID id);
}
