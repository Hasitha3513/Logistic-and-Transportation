package com.transportlogistics.app.organization.application.ports.out;

import com.transportlogistics.app.organization.domain.model.Customer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository {
    Customer save(Customer value);

    Optional<Customer> findById(UUID id);

    List<Customer> findAll();
}
