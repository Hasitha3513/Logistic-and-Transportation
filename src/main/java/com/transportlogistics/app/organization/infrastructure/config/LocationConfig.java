package com.transportlogistics.app.organization.infrastructure.config;

import com.transportlogistics.app.organization.LocationLookup;
import com.transportlogistics.app.organization.application.ports.in.LocationUseCase;
import com.transportlogistics.app.organization.application.ports.out.LocationRepository;
import com.transportlogistics.app.organization.application.service.LocationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
class LocationConfig {
    @Bean
    LocationUseCase locationUseCase(LocationRepository repo) {
        return new LocationService(repo);
    }

    @Bean
    LocationLookup locationLookup(LocationUseCase locationUseCase, LocationRepository locations) {
        return new LocationLookup() {
            @Override
            public Optional<LocationReference> find(java.util.UUID id) {
                try {
                    var loc = locationUseCase.get(id);
                    return Optional.of(reference(loc));
                } catch (com.transportlogistics.app.shared.domain.NotFoundException ignored) {
                    return Optional.empty();
                }
            }

            @Override
            public Optional<LocationReference> find(java.util.UUID tenantId, java.util.UUID id) {
                return locations.findById(tenantId, id).map(this::reference);
            }

            private LocationReference reference(com.transportlogistics.app.organization.domain.model.Location location) {
                return new LocationReference(location.id(), location.code(), location.name(), location.address(),
                        location.latitude(), location.longitude(), location.active());
            }
        };
    }
}
