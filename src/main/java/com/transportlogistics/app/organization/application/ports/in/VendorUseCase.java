package com.transportlogistics.app.organization.application.ports.in;

import com.transportlogistics.app.organization.domain.model.Vendor;

import java.util.List;
import java.util.UUID;

public interface VendorUseCase {
    Vendor create(Vendor vendor);

    Vendor update(UUID id, Vendor vendor);

    Vendor get(UUID id);

    List<Vendor> list(Boolean active);

    void deactivate(UUID id);
}
