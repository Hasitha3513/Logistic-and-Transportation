package com.transportlogistics.app.organization.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.organization.application.ports.out.CustomerRepository;
import com.transportlogistics.app.organization.domain.model.Customer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class CustomerPersistenceAdapter implements CustomerRepository {
    private final CustomerJpaRepository repo;

    public Customer save(Customer v) {
        var e = new CustomerEntity();
        e.setId(v.id());
        e.setCode(v.code());
        e.setName(v.name());
        e.setContactPerson(v.contactPerson());
        e.setPhone(v.phone());
        e.setEmail(v.email());
        e.setActive(v.active());
        return map(repo.save(e));
    }

    public Optional<Customer> findById(UUID id) {
        return repo.findById(id).map(this::map);
    }

    public List<Customer> findAll() {
        return repo.findAll().stream().map(this::map).toList();
    }

    private Customer map(CustomerEntity e) {
        return new Customer(e.getId(), e.getCode(), e.getName(), e.getContactPerson(), e.getPhone(), e.getEmail(), e.isActive());
    }
}
