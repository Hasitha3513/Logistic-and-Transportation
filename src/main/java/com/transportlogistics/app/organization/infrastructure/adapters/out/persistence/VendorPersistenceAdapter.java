package com.transportlogistics.app.organization.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.organization.application.ports.out.VendorRepository;
import com.transportlogistics.app.organization.domain.model.Vendor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class VendorPersistenceAdapter implements VendorRepository {
    private final VendorJpaRepository repository;

    VendorPersistenceAdapter(VendorJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Vendor save(Vendor vendor) {
        var entity = new VendorEntity();
        entity.setId(vendor.id());
        entity.setCode(vendor.code());
        entity.setName(vendor.name());
        entity.setContactPerson(vendor.contactPerson());
        entity.setPhone(vendor.phone());
        entity.setEmail(vendor.email());
        entity.setActive(vendor.active());
        return map(repository.save(entity));
    }

    @Override public Optional<Vendor> findById(UUID id) { return repository.findById(id).map(this::map); }

    @Override
    public List<Vendor> findAll(Boolean active) {
        var entities = active == null ? repository.findAllByOrderByName() : repository.findAllByActiveOrderByName(active);
        return entities.stream().map(this::map).toList();
    }

    @Override public boolean existsByCode(String code, UUID excludingId) {
        return repository.existsByCodeAndIdNot(code, excludingId);
    }

    private Vendor map(VendorEntity entity) {
        return new Vendor(entity.getId(), entity.getCode(), entity.getName(), entity.getContactPerson(),
                entity.getPhone(), entity.getEmail(), entity.isActive());
    }
}
