package com.transportlogistics.app.organization.application.ports.out;

import com.transportlogistics.app.organization.domain.model.Vendor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendorRepository {
    Vendor save(Vendor vendor);

    Optional<Vendor> findById(UUID id);

    List<Vendor> findAll(Boolean active);

    boolean existsByCode(String code, UUID excludingId);
}
