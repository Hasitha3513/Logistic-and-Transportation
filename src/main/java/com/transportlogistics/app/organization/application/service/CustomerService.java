package com.transportlogistics.app.organization.application.service;

import com.transportlogistics.app.organization.application.ports.in.CustomerUseCase;
import com.transportlogistics.app.organization.application.ports.out.CustomerRepository;
import com.transportlogistics.app.organization.domain.model.Customer;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.util.List;
import java.util.UUID;

public final class CustomerService implements CustomerUseCase {
    private final CustomerRepository repo;

    public CustomerService(CustomerRepository repo) {
        this.repo = repo;
    }

    public Customer create(Customer value) {
        return repo.save(value);
    }

    public Customer get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Customer not found: " + id));
    }

    public List<Customer> list() {
        return repo.findAll();
    }

    public Customer update(UUID id, Customer value) {
        return repo.save(value);
    }

    public void deactivate(UUID id) {
        var v = get(id);
        repo.save(new Customer(v.id(), v.code(), v.name(), v.contactPerson(), v.phone(), v.email(), false));
    }
}
