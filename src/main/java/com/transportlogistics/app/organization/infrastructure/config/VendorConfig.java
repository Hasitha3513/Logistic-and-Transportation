package com.transportlogistics.app.organization.infrastructure.config;

import com.transportlogistics.app.organization.VendorLookup;
import com.transportlogistics.app.organization.application.ports.in.VendorUseCase;
import com.transportlogistics.app.organization.application.ports.out.VendorRepository;
import com.transportlogistics.app.organization.application.service.VendorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class VendorConfig {
    @Bean VendorUseCase vendorUseCase(VendorRepository vendors) { return new VendorService(vendors); }

    @Bean
    VendorLookup vendorLookup(VendorUseCase vendors) {
        return id -> {
            try {
                var vendor = vendors.get(id);
                return java.util.Optional.of(new VendorLookup.VendorReference(vendor.id(), vendor.code(),
                        vendor.name(), vendor.active()));
            } catch (com.transportlogistics.app.shared.domain.NotFoundException ignored) {
                return java.util.Optional.empty();
            }
        };
    }
}
