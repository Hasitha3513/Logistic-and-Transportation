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
    LocationLookup locationLookup(LocationUseCase locations) {
        return id -> {
            try {
                var loc = locations.get(id);
                return Optional.of(new LocationLookup.LocationReference(
                        loc.id(), loc.code(), loc.name(), loc.address(), loc.latitude(), loc.longitude(), loc.active()
                ));
            } catch (com.transportlogistics.app.shared.domain.NotFoundException ignored) {
                return Optional.empty();
            }
        };
    }
}
