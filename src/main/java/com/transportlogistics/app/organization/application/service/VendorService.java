package com.transportlogistics.app.organization.application.service;

import com.transportlogistics.app.organization.application.ports.in.VendorUseCase;
import com.transportlogistics.app.organization.application.ports.out.VendorRepository;
import com.transportlogistics.app.organization.domain.model.Vendor;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class VendorService implements VendorUseCase {
    private final VendorRepository vendors;

    public VendorService(VendorRepository vendors) {
        this.vendors = vendors;
    }

    @Override
    public Vendor create(Vendor vendor) {
        return save(vendor.id() == null ? UUID.randomUUID() : vendor.id(), vendor);
    }

    @Override
    public Vendor update(UUID id, Vendor vendor) {
        get(id);
        return save(id, vendor);
    }

    private Vendor save(UUID id, Vendor vendor) {
        String code = vendor.code().trim().toUpperCase(Locale.ROOT);
        if (vendors.existsByCode(code, id)) {
            throw new ConflictException("FUEL_VENDOR_CODE_EXISTS", "Vendor code already exists");
        }
        return vendors.save(new Vendor(id, code, vendor.name().trim(), trim(vendor.contactPerson()),
                trim(vendor.phone()), trim(vendor.email()), vendor.active()));
    }

    @Override
    public Vendor get(UUID id) {
        return vendors.findById(id).orElseThrow(() ->
                new NotFoundException("FUEL_VENDOR_NOT_FOUND", "Vendor not found: " + id));
    }

    @Override
    public List<Vendor> list(Boolean active) {
        return vendors.findAll(active);
    }

    @Override
    public void deactivate(UUID id) {
        Vendor vendor = get(id);
        vendors.save(new Vendor(vendor.id(), vendor.code(), vendor.name(), vendor.contactPerson(), vendor.phone(),
                vendor.email(), false));
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
